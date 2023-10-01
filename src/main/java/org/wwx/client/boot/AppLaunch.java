package org.wwx.client.boot;

import com.alibaba.fastjson.JSON;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wwx.client.Client.TaskMachineProducer;
import org.wwx.client.Client.TaskMachineProducerImpl;
import org.wwx.client.constant.TaskConstant;
import org.wwx.client.constant.UserConfig;
import org.wwx.client.core.ObserverManager;
import org.wwx.client.core.observers.TimeObserver;
import org.wwx.client.data.*;
import org.wwx.client.enums.TaskStatus;
import org.wwx.client.lock.RedissonLock;
import org.wwx.client.task.TaskBuilder;
import org.wwx.client.task.TaskRet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AppLaunch implements Launch{
    final TaskMachineProducer taskMachineProducer;//用于发送请求


    public String packageName; //要执行的类的包名

    // 拉取哪几类任务
    static List<Class> taskTypes;

    // 拉取哪个任务的指针
    AtomicInteger offset;

    // 观察者模式的观察管理者
    ObserverManager observerManager;
    private Long intervalTime;//请求间隔时间，读取用户配置
    private int scheduleLimit; //一次拉取多少个任务，用户配置
    public Long cycleScheduleConfigTime = 10000L;// 多长时间拉取一次任务配置信息
    public static int MaxConcurrentRunTimes = 5; // 线程池最大数量
    public static int concurrentRunTimes = MaxConcurrentRunTimes; // 线程并发数
    private static String LOCK_KEY = "lock"; // 分布式锁的键
    Map<String, ScheduleConfig> scheduleCfgDic; // 存储任务配置信息
    Logger logger = LoggerFactory.getLogger(AppLaunch.class); //打印日志
    ThreadPoolExecutor threadPoolExecutor; // 拉取任务的线程池
    ScheduledExecutorService loadPool;


    public AppLaunch() {
        this(0, new ArrayList<>());
    }
    public AppLaunch(int scheduleLimit, ArrayList<Class> taskTypesList) {
        scheduleCfgDic = new ConcurrentHashMap<>();

        loadPool = Executors.newScheduledThreadPool(1);
        taskMachineProducer = new TaskMachineProducerImpl();
        taskTypes = taskTypesList;
        this.packageName = taskTypes.get(0).getPackage().getName();
        this.scheduleLimit = scheduleLimit;
        observerManager = new ObserverManager();
        // 向观察管理者注册观察者
        observerManager.registerEventObserver(new TimeObserver());
        offset = new AtomicInteger(0);
        // 初始化，拉取任务配置信息
        init();

    }

    // 启动：拉取任务
    @Override
    public int start() {
        // 根据支持列表中的任务类型，逐一拉取
        int i = offset.incrementAndGet();
        Class taskType = taskTypes.get(i % taskTypes.size());
        // 读取对应任务配置信息
        ScheduleConfig scheduleConfig = scheduleCfgDic.get(taskType.getSimpleName());
        // 如果用户没有配置时间间隔就使用默认时间间隔
        intervalTime = scheduleConfig.getSchedule_interval() == 0 ? TaskConstant.DEFAULT_TIME_INTERVAL * 1000L : scheduleConfig.getSchedule_interval() * 1000L;
        this.threadPoolExecutor = new ThreadPoolExecutor(concurrentRunTimes, MaxConcurrentRunTimes, intervalTime + 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(UserConfig.QUEUE_SIZE));
        for(;;) {
            // consumer堆积任务，停止拉取，只消费
            i = offset.getAndIncrement() % taskTypes.size();
            taskType = taskTypes.get(i);
            if (UserConfig.QUEUE_SIZE - threadPoolExecutor.getQueue().size() >= scheduleLimit) {
                execute(taskType);
            }
            // 随机等待
            try {
                Thread.sleep(intervalTime + (int)(Math.random() * 500));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scheduleConfig = scheduleCfgDic.get(taskType.getSimpleName());
        }
    }

    public void execute(Class<?> taskType) {
        List<TaskMachineTaskBase> taskMachineTaskBaseList = scheduleTask(taskType);
        if (taskMachineTaskBaseList == null) {
            return;
        }
        int size = taskMachineTaskBaseList.size();
        for (int i = 0; i < size; i++) {
            int finalI = i;
            threadPoolExecutor.execute(() -> executeTask(taskMachineTaskBaseList, finalI));
        }
    }

    // 拉取任务
    private List<TaskMachineTaskBase> scheduleTask(Class<?> taskType) {
        try {
            observerManager.wakeupObserver(ObserverType.onBoot);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // 调用拉取任务接口拉取任务
        List<TaskMachineTaskBase> taskMachineTaskBaseList = getAsyncTaskBases(observerManager, taskType);
        if (taskMachineTaskBaseList == null || taskMachineTaskBaseList.size() == 0) {
            return null;
        }
        return taskMachineTaskBaseList;
    }

    // 执行任务
    private void executeTask(List<TaskMachineTaskBase> taskMachineTaskBaseList, int i) {
        TaskMachineTaskBase v = taskMachineTaskBaseList.get(i);
        try {
            observerManager.wakeupObserver(ObserverType.onExecute, v);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        TaskMachineTaskSetStage taskMachineTaskSetStage = null;
        try {
            // 利用Java反射生成，并执行本地方法
            Class<?> aClass = Class.forName(packageName + "." + v.getTask_type());
            Method method = TaskBuilder.getMethod(aClass, v.getTask_stage(), v.getTask_context().getParams(), v.getTask_context().getClazz());
            System.out.println(method.getName());
            TaskRet returnVal = (TaskRet) method.invoke(aClass.newInstance(), v.getTask_context().getParams());
            if (returnVal != null) {
                taskMachineTaskSetStage = returnVal.getTaskMachineTaskSetStage();
                Object result = returnVal.getResult();
                System.out.println("执行结果为：" + result);
            }
        } catch (Exception e) {
            try {
                // 执行失败或出错，更改任务状态为PENDING，重试次数+1，超过重试次数设置为FAIL
                observerManager.wakeupObserver(ObserverType.onError, v, scheduleCfgDic.get(v.getTask_type()), taskMachineTaskBaseList, e);
                return;
            } catch (InvocationTargetException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        try {
            observerManager.wakeupObserver(ObserverType.onFinish, v, taskMachineTaskSetStage);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private List<TaskMachineTaskBase> getAsyncTaskBases(ObserverManager observerManager, Class<?> taskType) {
        String lockName = taskType.getSimpleName();
        RLock lock = RedissonLock.getRedissonClient().getLock(lockName);

        List<TaskMachineTaskReturn> taskList = null;
        try {
            // 上锁
            boolean lockRes = lock.tryLock();
            if (lockRes) {
            // 调用http请求接口
                taskList = taskMachineProducer.getTaskList(taskType, TaskStatus.PENDING.getStatus(), scheduleCfgDic.get(taskType.getSimpleName()).getSchedule_limit());
                if (taskList == null || taskList.size() == 0) {
                    logger.warn("no task to deal!");
                    return null;
                }
                // 转换
                List<TaskMachineTaskBase> taskMachineTaskBaseList = convertModel(taskList);
                try {

                    observerManager.wakeupObserver(ObserverType.onObtain, taskMachineTaskBaseList);
                    return taskMachineTaskBaseList;
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // 释放锁
            lock.unlock();
        }

        return null;
    }

    // 拉取任务配置信息
    private void loadCfg() {
        List<ScheduleConfig> taskTypeCfgList = taskMachineProducer.getTaskTypeCfgList();
        for (ScheduleConfig scheduleConfig : taskTypeCfgList) {
            scheduleCfgDic.put(scheduleConfig.getTask_type(), scheduleConfig);
        }
    }

    @Override
    public int init() {
        loadCfg();
        if (scheduleLimit != 0) {
            logger.debug("init ScheduleLimit : %d", scheduleLimit);
            concurrentRunTimes = scheduleLimit;
            MaxConcurrentRunTimes = scheduleLimit;
        } else {
            this.scheduleLimit = this.scheduleCfgDic.get(taskTypes.get(0).getSimpleName()).getSchedule_limit();
        }
        // 定期更新任务配置信息
        loadPool.scheduleAtFixedRate(this::loadCfg, cycleScheduleConfigTime, cycleScheduleConfigTime, TimeUnit.MILLISECONDS);
        return 0;
    }


    public List<TaskMachineTaskBase> convertModel(List<TaskMachineTaskReturn> taskMachineTaskReturnList) {
        List<TaskMachineTaskBase> taskMachineTaskBaseList = new ArrayList<>();
        for (TaskMachineTaskReturn taskMachineTaskReturn : taskMachineTaskReturnList) {
            TaskMachineTaskBase taskMachineTaskBase = new TaskMachineTaskBase();
            taskMachineTaskBase.setUser_id(taskMachineTaskReturn.getUser_id());
            taskMachineTaskBase.setTask_id(taskMachineTaskReturn.getTask_id());
            taskMachineTaskBase.setTask_type(taskMachineTaskReturn.getTask_type());
            taskMachineTaskBase.setTask_stage(taskMachineTaskReturn.getTask_stage());
            taskMachineTaskBase.setCrt_retry_num(taskMachineTaskReturn.getCrt_retry_num());
            taskMachineTaskBase.setMax_retry_num(taskMachineTaskReturn.getMax_retry_num());
            taskMachineTaskBase.setMax_retry_interval(taskMachineTaskReturn.getMax_retry_interval());
            taskMachineTaskBase.setCreate_time(taskMachineTaskReturn.getCreate_time());
            taskMachineTaskBase.setModify_time(taskMachineTaskReturn.getModify_time());
            taskMachineTaskBase.setSchedule_log(JSON.parseObject(String.valueOf(JSON.parse(taskMachineTaskReturn.getSchedule_log())), ScheduleLog.class));
            taskMachineTaskBase.setTask_context(JSON.parseObject(taskMachineTaskReturn.getTask_context(), NftTaskContext.class));
            taskMachineTaskBase.setTask_id(taskMachineTaskReturn.getTask_id());
            taskMachineTaskBase.setStatus(taskMachineTaskReturn.getStatus());
            taskMachineTaskBaseList.add(taskMachineTaskBase);
        }

        return taskMachineTaskBaseList;
    }

    @Override
    public int destroy() {
        return 0;
    }
    // 枚举
    public enum ObserverType {
        onBoot(0),
        onError(1),
        onExecute(2),
        onFinish(3),
        onStop(4), onObtain(5);
        private int code;

        private ObserverType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}

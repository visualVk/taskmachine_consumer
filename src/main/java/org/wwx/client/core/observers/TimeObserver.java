package org.wwx.client.core.observers;

import com.alibaba.fastjson.JSON;
import org.wwx.client.Client.TaskMachineProducer;
import org.wwx.client.Client.TaskMachineProducerImpl;
import org.wwx.client.boot.AppLaunch;
import org.wwx.client.constant.UserConfig;
import org.wwx.client.core.AnnType;
import org.wwx.client.core.ObserverFunction;
import org.wwx.client.data.*;
import org.wwx.client.enums.TaskStatus;

import java.util.List;
import java.util.UUID;

/**
 * 观察者
 */
public class TimeObserver implements ObserverFunction {
    private Long beginTime;
    TaskMachineProducer taskMachineProducer = new TaskMachineProducerImpl();

    // 获取任务时改变任务状态
    @Override
    @AnnType(observerType = AppLaunch.ObserverType.onObtain)
    public void onObtain(List<TaskMachineTaskBase> taskMachineTaskBaseList) {
        System.out.println("锁定任务啦");
//        for (AsyncTaskBase asyncTaskBase : asyncTaskBaseList) {
//            setTaskNow(modifyStatus(asyncTaskBase, TaskStatus.EXECUTING));
//        }
    }
    // 执行任务前做的动作，目前是简单打印
    @Override
    @AnnType(observerType = AppLaunch.ObserverType.onExecute)
    public void onExecute(TaskMachineTaskBase asyncTaskReturn) {
        this.beginTime = System.currentTimeMillis();
        System.out.println(asyncTaskReturn.getTask_type() + "开始执行。");
    }

    // 启动动作
    @Override
    @AnnType(observerType = AppLaunch.ObserverType.onBoot)
    public void onBoot() {
        System.out.println(UserConfig.USERID + "的线程" + Thread.currentThread().getName() + "取任务");
    }
    // 执行任务失败时的动作，目前是本地重试
    @Override
    @AnnType(observerType = AppLaunch.ObserverType.onError)
    public void onError(TaskMachineTaskBase asyncTaskReturn, ScheduleConfig scheduleConfig, List<TaskMachineTaskBase> taskMachineTaskBaseList, Exception e) {
//        if (asyncTaskReturn.getCrt_retry_num() < 60) {
//            if (asyncTaskReturn.getCrt_retry_num() != 0) {
//                asyncTaskReturn.setMax_retry_num(asyncTaskReturn.getCrt_retry_num() << 1);
//            }
//        } else {
//            asyncTaskReturn.setMax_retry_interval(scheduleConfig.getRetry_interval());
//        }
//        if (asyncTaskReturn.getMax_retry_interval() > scheduleConfig.getRetry_interval()) {
//            asyncTaskReturn.setMax_retry_interval(scheduleConfig.getRetry_interval());
//        }
//        asyncTaskReturn.getSchedule_log().getLastData().setErrMsg(e.getMessage());
//        if (asyncTaskReturn.getMax_retry_num() == 0 || asyncTaskReturn.getCrt_retry_num() >= asyncTaskReturn.getMax_retry_num()) {
//            AsyncTaskSetRequest asyncTaskSetRequest = modifyStatus(asyncTaskReturn, TaskStatus.FAIL);
//            asyncTaskSetRequest.setCrt_retry_num(asyncTaskReturn.getCrt_retry_num());
//            asyncTaskSetRequest.setMax_retry_interval(asyncTaskReturn.getMax_retry_interval());
//            asyncTaskSetRequest.setMax_retry_num(asyncTaskReturn.getMax_retry_num());
//            setTaskNow(asyncTaskSetRequest);
//            return;
//        }
        System.out.println(asyncTaskReturn.getTask_type() + "任务执行出错！");
        e.printStackTrace();
        TaskMachineTaskSetRequest taskMachineTaskSetRequest;
        if (asyncTaskReturn.getMax_retry_num() == 0
                || asyncTaskReturn.getCrt_retry_num() >= asyncTaskReturn.getMax_retry_num()) {
            taskMachineTaskSetRequest = modifyTaskInfo(asyncTaskReturn, TaskStatus.FAIL, null);
            taskMachineTaskSetRequest.setSchedule_log(JSON.toJSONString(asyncTaskReturn.getSchedule_log()));
            taskMachineTaskSetRequest.setCrt_retry_num(asyncTaskReturn.getMax_retry_num());
        } else {
            taskMachineTaskSetRequest = modifyTaskInfo(asyncTaskReturn, TaskStatus.PENDING, null);
            taskMachineTaskSetRequest.setSchedule_log(JSON.toJSONString(asyncTaskReturn.getSchedule_log()));
            taskMachineTaskSetRequest.setCrt_retry_num(asyncTaskReturn.getCrt_retry_num() + 1);

        }
        taskMachineTaskSetRequest.setOrder_time(System.currentTimeMillis() + (scheduleConfig.getRetry_interval() << asyncTaskReturn.getCrt_retry_num()));
        System.out.println(System.currentTimeMillis() + scheduleConfig.getRetry_interval());
        taskMachineTaskSetRequest.setMax_retry_interval(asyncTaskReturn.getMax_retry_interval());
        taskMachineTaskSetRequest.setMax_retry_num(asyncTaskReturn.getMax_retry_num());
        taskMachineTaskSetRequest.setSchedule_log(getScheduleLog(asyncTaskReturn, System.currentTimeMillis() - beginTime, e.getMessage()));
        setTaskNow(taskMachineTaskSetRequest);
    }
    // 任务执行完成做的动作
    @Override
    @AnnType(observerType = AppLaunch.ObserverType.onFinish)
    public void onFinish(TaskMachineTaskBase asyncTaskReturn, TaskMachineTaskSetStage taskMachineTaskSetStage){
        TaskMachineTaskSetRequest taskMachineTaskSetRequest = modifyTaskInfo(asyncTaskReturn, TaskStatus.SUCCESS, taskMachineTaskSetStage);
        long cost = System.currentTimeMillis() - beginTime;
        taskMachineTaskSetRequest.setSchedule_log(JSON.toJSONString(getScheduleLog(asyncTaskReturn, cost, "")));
        setTaskNow(taskMachineTaskSetRequest);
        System.out.println(asyncTaskReturn.getTask_type() + "执行完毕！");
    }


    // 获取待定使用
    @Override
    @AnnType(observerType = AppLaunch.ObserverType.onStop)
    public void onStop(TaskMachineTaskBase asyncTaskReturn){
    }

    // 修改任务状态
    public TaskMachineTaskSetRequest modifyTaskInfo(TaskMachineTaskBase taskMachineTaskBase, TaskStatus taskStatus, TaskMachineTaskSetStage taskMachineTaskSetStage) {
        TaskMachineTaskSetRequest taskMachineTaskSetRequest = TaskMachineTaskSetRequest.builder().
                task_id(taskMachineTaskBase.getTask_id())
                        .task_context(taskMachineTaskSetStage != null ? JSON.toJSONString(taskMachineTaskSetStage.getTask_context()) : JSON.toJSONString(taskMachineTaskBase.getTask_context()))
                                .priority(taskMachineTaskBase.getPriority())
                                        .task_stage(taskMachineTaskSetStage != null ? taskMachineTaskSetStage.getTask_stage() : taskMachineTaskBase.getTask_stage()).status(taskStatus.getStatus())
                .crt_retry_num(taskMachineTaskBase.getCrt_retry_num())
                .max_retry_interval(taskMachineTaskBase.getMax_retry_interval())
                .order_time(taskMachineTaskSetStage != null ? System.currentTimeMillis() - taskMachineTaskBase.getPriority() : taskMachineTaskBase.getOrder_time() - taskMachineTaskBase.getPriority())
                .build();
        taskMachineTaskSetRequest.setStatus(taskMachineTaskSetStage != null ? TaskStatus.PENDING.getStatus() : taskStatus.getStatus());
        return taskMachineTaskSetRequest;
    }

    public String getScheduleLog(TaskMachineTaskBase asyncTaskReturn, long costTime, String errMsg) {
        // 记录调度信息
        ScheduleLog scheduleLog = asyncTaskReturn.getSchedule_log();
        ScheduleData lastData = scheduleLog.getLastData();
        List<ScheduleData> historyDatas = scheduleLog.getHistoryDatas();
        historyDatas.add(lastData);
        if (historyDatas.size() > 3) {
            historyDatas.remove(0);
        }
        ScheduleData scheduleData = new ScheduleData(UUID.randomUUID() + "", errMsg, costTime + "");
        scheduleLog.setLastData(scheduleData);
        return JSON.toJSONString(scheduleLog);
    }

    // 修改任务信息
    public void setTaskNow(TaskMachineTaskSetRequest taskMachineTaskSetRequest) {
        taskMachineProducer.setTask(taskMachineTaskSetRequest);
    }
}

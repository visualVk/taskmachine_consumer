package org.wwx.client.Client;

import com.alibaba.fastjson.JSON;
import org.wwx.client.constant.UserConfig;
import org.wwx.client.data.*;
import org.wwx.client.enums.ErrorStatus;
import org.wwx.client.enums.TaskStatus;
import org.wwx.client.http.TaskMachineServer;
import org.wwx.client.http.TaskMachineServerImpl;

import java.util.List;

// 对http请求进行封装
public class TaskMachineProducerImpl implements TaskMachineProducer {
    TaskMachineServer taskMachineServer = new TaskMachineServerImpl();
    @Override
    public String createTask(TaskMachineTaskRequest taskMachineTaskRequest) {
        Object o = judgeReturnStatus(taskMachineServer.createTask(taskMachineTaskRequest));
        String taskId = JSON.parseObject(JSON.toJSONString(o), String.class);
        return taskId;
    }

    @Override
    public void setTask(TaskMachineTaskSetRequest taskMachineTaskSetRequest) {
        judgeReturnStatus(taskMachineServer.setTask(taskMachineTaskSetRequest));
    }

    @Override
    public TaskMachineTaskReturn getTask(String taskId) {
        Object o = judgeReturnStatus(taskMachineServer.getTask(taskId));
        String s = JSON.toJSONString(o);
        TaskByTaskIdReturn<TaskMachineTaskReturn> asyncFlowTask = JSON.parseObject(s, TaskByTaskIdReturn.class);
        TaskMachineTaskReturn taskMachineTaskReturn = JSON.parseObject(JSON.toJSONString(asyncFlowTask.getTaskData()), TaskMachineTaskReturn.class);
        return taskMachineTaskReturn;
    }

    @Override
    public List<TaskMachineTaskReturn> getTaskList(Class<?> taskType, int status, int limit) {
        Object o = judgeReturnStatus(taskMachineServer.getTaskList(taskType.getSimpleName(), status, limit));
        List<TaskMachineTaskReturn> taskMachineTaskReturns = getAsyncTaskReturns(o);
        return taskMachineTaskReturns;
    }

    @Override
    public List<ScheduleConfig> getTaskTypeCfgList() {
        Object o = judgeReturnStatus(taskMachineServer.getTaskTypeCfgList());
        ConfigReturn configReturn = JSON.parseObject(JSON.toJSONString(o), ConfigReturn.class);
        List<ScheduleConfig> scheduleConfigs = JSON.parseArray(JSON.toJSONString(configReturn.getScheduleCfgList()), ScheduleConfig.class);
        return scheduleConfigs;
    }


    public List<TaskMachineTaskReturn> doGetUserTaskList(String user_id, int status) {
        Object o = judgeReturnStatus(taskMachineServer.getUserTaskList(user_id, status));
        List<TaskMachineTaskReturn> taskMachineTaskReturns = getAsyncTaskReturns(o);
        return taskMachineTaskReturns;
    }

    @Override
    public List<TaskMachineTaskReturn> getUserTaskList(List<TaskStatus> taskStatuses) {
        String user_id = UserConfig.USERID;
        int statusList = 0;
        for (TaskStatus status : taskStatuses) {
            statusList |= status.getStatus();
        }
        return doGetUserTaskList(user_id, statusList);
    }

    private List<TaskMachineTaskReturn> getAsyncTaskReturns(Object o) {
        TaskList taskList = JSON.parseObject(JSON.toJSONString(o), TaskList.class);
        List<TaskMachineTaskReturn> taskMachineTaskReturns = JSON.parseArray(JSON.toJSONString(taskList.getTaskList()), TaskMachineTaskReturn.class);
        return taskMachineTaskReturns;
    }

    @Override
    public void createTaskCFG(ScheduleConfig scheduleConfig) {
        judgeReturnStatus(taskMachineServer.createTaskCFG(scheduleConfig));
    }

    public <E> E judgeReturnStatus(ReturnStatus<E> returnStatus) {
        if (returnStatus.getCode() != ErrorStatus.SUCCESS.getErrCode()) {
            throw new RuntimeException(returnStatus.getMsg());
        }
        return returnStatus.getResult();
    }
}

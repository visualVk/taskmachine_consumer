package org.wwx.client.http;


import org.wwx.client.data.TaskMachineTaskRequest;
import org.wwx.client.data.TaskMachineTaskSetRequest;
import org.wwx.client.data.ReturnStatus;
import org.wwx.client.data.ScheduleConfig;

public interface TaskMachineServer {
    ReturnStatus getTaskList(String taskType, int status, int limit);
    ReturnStatus createTask(TaskMachineTaskRequest taskMachineTaskRequest);
    ReturnStatus setTask(TaskMachineTaskSetRequest taskMachineTaskSetRequest);
    ReturnStatus getTask(String taskId);

    ReturnStatus getTaskTypeCfgList();
    ReturnStatus getUserTaskList(String user_id, int statusList);
    ReturnStatus createTaskCFG(ScheduleConfig scheduleConfig);

}

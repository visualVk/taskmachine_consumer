package org.wwx.client.Client;


import org.wwx.client.data.TaskMachineTaskRequest;
import org.wwx.client.data.TaskMachineTaskReturn;
import org.wwx.client.data.TaskMachineTaskSetRequest;
import org.wwx.client.data.ScheduleConfig;
import org.wwx.client.enums.TaskStatus;

import java.util.List;

public interface TaskMachineProducer {
    public String createTask(TaskMachineTaskRequest taskMachineTaskRequest);
    public void setTask(TaskMachineTaskSetRequest taskMachineTaskSetRequest);
    public TaskMachineTaskReturn getTask(String taskId);
    public List<TaskMachineTaskReturn> getTaskList(Class<?> clazz, int status, int limit);
    public List<ScheduleConfig> getTaskTypeCfgList();
    public List<TaskMachineTaskReturn> getUserTaskList(List<TaskStatus> taskStatuses);
    public void createTaskCFG(ScheduleConfig scheduleConfig);


}

package org.wwx.client.core;


import org.wwx.client.data.TaskMachineTaskBase;
import org.wwx.client.data.TaskMachineTaskSetStage;
import org.wwx.client.data.ScheduleConfig;

import java.util.List;

public interface ObserverFunction {
    void onBoot();
    void onObtain(List<TaskMachineTaskBase> taskMachineTaskBaseList);
    void onExecute(TaskMachineTaskBase asyncTaskReturn);
    void onFinish(TaskMachineTaskBase asyncTaskReturn, TaskMachineTaskSetStage taskMachineTaskSetStage);
    void onStop(TaskMachineTaskBase asyncTaskReturn);
    void onError(TaskMachineTaskBase asyncTaskReturn, ScheduleConfig scheduleConfig, List<TaskMachineTaskBase> taskMachineTaskBaseList, Exception e);

}

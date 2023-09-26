package org.wwx.client.task;


import lombok.Data;
import org.wwx.client.data.TaskMachineTaskSetStage;

@Data
public class TaskRet<T> {
    T result;
    TaskMachineTaskSetStage taskMachineTaskSetStage;
    public TaskRet(T result) {
        this(result, null);
    }
    public TaskRet(T result, TaskMachineTaskSetStage taskMachineTaskSetStage) {
        this.result = result;
        this.taskMachineTaskSetStage = taskMachineTaskSetStage;
    }

}

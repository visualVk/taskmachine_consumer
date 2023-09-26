package org.wwx.client.test;


import org.wwx.client.Client.TaskMachineProducer;
import org.wwx.client.Client.TaskMachineProducerImpl;
import org.wwx.client.data.TaskMachineClientData;
import org.wwx.client.data.TaskMachineTaskRequest;
import org.wwx.client.task.TaskBuilder;
import org.wwx.client.task.TestTask;

public class Test {
    static TaskMachineProducer taskMachineProducer = new TaskMachineProducerImpl();
    public static void main(String[] args) {
        testCreateTask();

    }

    private static void testCreateTask() {
        TaskMachineClientData taskMachineClientData = null;
        try {
            taskMachineClientData = TaskBuilder.build(new TestTask());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        String task = taskMachineProducer.createTask(new TaskMachineTaskRequest(taskMachineClientData));
    }

}

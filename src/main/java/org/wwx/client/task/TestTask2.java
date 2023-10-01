package org.wwx.client.task;

import org.wwx.client.data.TaskMachineTaskSetStage;

import java.lang.reflect.Method;

public class TestTask2 implements AsyncExecutable{

    public TaskRet stageOne(String stageOneParam){
    TaskMachineTaskSetStage nextStage = null;
        try {
            Method method = this.getClass().getMethod("stageTwo", String.class);
            System.out.println(Thread.currentThread().getId() + ": " + stageOneParam);
            nextStage = setStage(this.getClass(), method.getName(), new Object[]{"2stageTwoParam"}, method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return new TaskRet(null ,nextStage);
    }

    public TaskRet stageTwo(String stageTwoParam){
        System.out.println(Thread.currentThread().getId() + ": " + stageTwoParam);
        return new TaskRet(1);
    }

    @Override
    public TaskRet handleProcess() {
        return stageOne("2stageOneParam");
    }
}

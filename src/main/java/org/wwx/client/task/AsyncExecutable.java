package org.wwx.client.task;

import org.wwx.client.data.TaskMachineTaskSetStage;
import org.wwx.client.data.NftTaskContext;
import org.wwx.client.enums.TaskStatus;

import java.lang.reflect.Method;

@FunctionalInterface
public interface AsyncExecutable<T> {
    TaskRet<T> handleProcess();

    default TaskMachineTaskSetStage setStage(Class<?> clazz, String methodName, Object[] params, Class<?>[] parameterTypes, Object... envs) {
        return build(clazz, methodName, params, parameterTypes, envs);
    }


    // 利用类信息创建任务
    default TaskMachineTaskSetStage build(Class<?> clazz, String methodName, Object[] params, Class<?>[] parameterTypes, Object... envs) {
        TaskBuilder.checkParamsNum(params, parameterTypes);
        Method method = TaskBuilder.getMethod(clazz, methodName, params, parameterTypes);

        // get 方法名
        String taskStage = method.getName();

        // 上下文信息
        NftTaskContext nftTaskContext = new NftTaskContext(params, envs, parameterTypes);
        return TaskMachineTaskSetStage.builder()
                .status(TaskStatus.PENDING.getStatus())
                .task_context(nftTaskContext)
                .task_stage(taskStage)
                .build();
    }

    default boolean judgeParamsTypes(Method clazzMethod, Class<?>[] parameterTypes) {
        Class<?>[] types = clazzMethod.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i] != parameterTypes[i]) {
                return false;
            }
        }
        return true;
    }
}

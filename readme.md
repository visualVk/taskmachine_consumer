# TaskMachine Consumer的一些指南

## 调用接口

**可以在constant/TaskUrl中配置请求IP**

## 预备工作

1. 部署TaskMachine Server（跳过，已部署在服务器上）

2. 自定义任务，如task/TestTask

3. 提交任务，可以使用postman或者test/Test的testCreateTask方法创建

|请求URL|含义|要求参数|
|------|---|-------|
|/task/create_task|根据已有模板创建任务|{"taskData":{"schedule_log":"{"historyDatas":[],"lastData":{}}","task_context":"{"clazz":[],"envs":[],"params":[]}","task_stage":"handleProcess","task_type":"","user_id":""}}|

5. 将TaskMachine Consumer进行打包

```shell
mvn package
```

6. 运行

```shell
java -jar taskmachine_consumer-1.0-SNAPSHOT.jar
```


package org.wwx.client;

import org.wwx.client.boot.AppLaunch;
import org.wwx.client.boot.Launch;
import org.wwx.client.task.TestTask;

import java.util.ArrayList;


public class Main {

    public static void main(String[] args) {

        Launch l = new AppLaunch(0, new ArrayList<Class>(){{add(TestTask.class);}});
        // 启动worker
        l.start();
    }
}

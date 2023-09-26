package org.wwx.client;

import org.wwx.client.boot.AppLaunch;
import org.wwx.client.boot.Launch;


public class Main {

    public static void main(String[] args) {

        Launch l = new AppLaunch();
        // 启动worker
        l.start();
    }
}

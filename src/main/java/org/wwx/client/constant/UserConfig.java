package org.wwx.client.constant;

// 用户配置信息
public class UserConfig {
    public final static String USERID;
    static {
        USERID = "wwx";
    }
    public final static int QUEUE_SIZE = 10000; // 线程池等待队列大小

}

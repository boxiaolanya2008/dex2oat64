package com.vivo.applyindepthoptimization.server;

import java.io.File;

import yangFenTuoZi.server.ServerTemplate;

public class Server extends ServerTemplate {

    /**
     * 构造函数，初始化服务
     * 包括设置主线程、权限检查、日志记录器初始化、异常处理等
     *
     * @param args
     */
    public Server(Args args) {
        super(args);
    }

    // 入口
    public static void main(String[] args) {
        Args.Builder builder = new Args.Builder();
        builder.enableLogger = true;
        builder.logDir = new File("/storage/emulated/0/Android/data/com.vivo.applyindepthoptimization/files/logs");
        builder.serverName = "viqitos_server";
        builder.uids = new int[]{0, 2000};
        new Server(builder.build());
    }

    // 创建时
    @Override
    public void onCreate() {
        super.onCreate();
    }

    // 启动时
    @Override
    public void onStart() {
        super.onStart();
    }

    // 停止时
    @Override
    public void onStop() {
        super.onStop();
    }

    // 崩溃时
    @Override
    public void onCrash(Thread t, Throwable e) {
        super.onCrash(t, e);
    }
}

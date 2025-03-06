package com.vivo.applyindepthoptimization.server;

import android.os.Build;
import android.system.Os;

import com.vivo.applyindepthoptimization.BuildConfig;

import java.io.File;
import java.util.Arrays;

import yangFenTuoZi.server.Logger;
import yangFenTuoZi.server.ServerTemplate;

public class Server extends ServerTemplate {

    private boolean isCrashed = false;
    private Logger mLogger;

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
    }

    // 启动时
    @Override
    public void onStart() {
        mLogger = getLogger();
        mLogger.i("启动");
    }

    // 停止时
    @Override
    public void onStop() {
        mLogger.i("停止");
        mLogger.close();
    }

    // 崩溃时
    @Override
    public void onCrash(Thread t, Throwable e) {
        if (isCrashed) System.exit(255);
        isCrashed = true;
        new Thread(() -> {
            if (mLogger != null)
                mLogger.e("""
                        ** 程序崩溃! **
                        线程: %s
                        用户ID: %d, 进程ID: %d
                        
                        App版本: %s (%d)
                        机型: %s
                        厂商: %s
                        CPU架构: %s
                        SDK版本: %d
                        
                        %s
                        """, t.getName(),
                        Os.getuid(), Os.getpid(),
                        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                        Build.MANUFACTURER, Build.MODEL,
                        Arrays.toString(Build.SUPPORTED_ABIS),
                        Build.VERSION.SDK_INT,
                        Logger.getStackTraceString(e));
            finish(255);
        }).start();
    }
}

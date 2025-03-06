package com.vivo.applyindepthoptimization.server;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.system.Os;

import com.vivo.applyindepthoptimization.BuildConfig;
import com.vivo.applyindepthoptimization.IService;

import java.io.File;
import java.util.Arrays;

import yangFenTuoZi.server.Logger;
import yangFenTuoZi.server.ServerTemplate;

public class Server extends ServerTemplate {
    public static final String dataDir = "/storage/emulated/0/Android/data/" + BuildConfig.APPLICATION_ID;
    public static final String ACTION_SERVER_RUNNING = "dex2oat64_opt.intent.action.SERVER_RUNNING";
    public static final String ACTION_SERVER_STOPPED = "dex2oat64_opt.intent.action.SERVER_STOPPED";
    private boolean isCrashed = false;
    private Logger mLogger;
    private Thread appListener;

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
        builder.logDir = new File(dataDir + "/files/logs");
        builder.serverName = "viqitos_server";
        builder.uids = new int[]{0, 2000};
        new Server(builder.build());
    }

    // 启动时
    @Override
    public void onStart() {
        mLogger = getLogger();
        mLogger.i("启动");

        appListener = new Thread(() -> {
            File send_binder = new File(dataDir + "/cache/send_binder");
            while (!isCrashed) {
                try {
                    if (send_binder.exists()) {
                        sendBinder();
                        send_binder.delete();
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        appListener.start();
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
        if (appListener!= null) appListener.interrupt();
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

    public void sendBinder() {
        Intent intent = new Intent(Server.ACTION_SERVER_RUNNING)
                .setPackage(BuildConfig.APPLICATION_ID)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("binder", new BinderContainer(getBinder()));

        sendBroadcast(intent);
    }

    public IBinder getBinder() {
        return new Service() {
            @Override
            public boolean isShizuku() {
                return false;
            }
        };
    }
}

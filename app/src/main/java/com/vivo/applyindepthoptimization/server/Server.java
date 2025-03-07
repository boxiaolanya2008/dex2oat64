package com.vivo.applyindepthoptimization.server;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import com.vivo.applyindepthoptimization.BuildConfig;
import com.vivo.applyindepthoptimization.IService;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import yangFenTuoZi.server.Logger;
import yangFenTuoZi.server.ServerTemplate;

public class Server extends ServerTemplate {
    public static final String TAG = "viqitos_server";
    public static final String dataDir = "/storage/emulated/0/Android/data/" + BuildConfig.APPLICATION_ID;
    public static final String ACTION_SERVER_RUNNING = "dex2oat64_opt.intent.action.SERVER_RUNNING";
    public static final String ACTION_SERVER_STOPPED = "dex2oat64_opt.intent.action.SERVER_STOPPED";
    private boolean isCrashed = false;
    private Logger mLogger;
    private Thread appListener;

    public Server(Args args) {
        super(args);
    }

    // 入口
    public static void main(String[] args) {
        Args.Builder builder = new Args.Builder();
        builder.enableLogger = true;
        builder.logDir = new File(dataDir + "/files/logs");
        builder.serverName = TAG;
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
        mLogger.i("发送Binder给App");
        Intent intent = new Intent(Server.ACTION_SERVER_RUNNING)
                .setPackage(BuildConfig.APPLICATION_ID)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("binder", new BinderContainer(getBinder()));

        sendBroadcast(intent);
    }

    public IBinder getBinder() {
        return new IService.Stub() {

            @Override
            public void exit() {
                finish(0);
            }

            @Override
            public int getUid() {
                return Os.getuid();
            }

            @Override
            public String runCommand(String command) throws RemoteException {
                try {
                    StringBuilder output = new StringBuilder();

                    // 执行命令
                    Process process = Runtime.getRuntime().exec(command.split(" "));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    process.waitFor();
                    return output.toString();
                } catch (Throwable e) {
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }
        };
    }
}

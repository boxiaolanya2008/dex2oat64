package com.vivo.applyindepthoptimization.server;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
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
        if (appListener != null) appListener.interrupt();
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
                runOnMainThread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    finish(0);
                });
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

            // settings system
            // put
            @Override
            public boolean settingsSystemPutString(String name, String value) {
                mLogger.i("settingsSystemPutString called: name=%s, value=%s", name, value);
                return Settings.System.putString(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsSystemPutFloat(String name, float value) {
                mLogger.i("settingsSystemPutFloat called: name=%s, value=%f", name, value);
                return Settings.System.putFloat(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsSystemPutInt(String name, int value) {
                mLogger.i("settingsSystemPutInt called: name=%s, value=%d", name, value);
                return Settings.System.putInt(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsSystemPutLong(String name, long value) {
                mLogger.i("settingsSystemPutLong called: name=%s, value=%d", name, value);
                return Settings.System.putLong(getContentResolver(), name, value);
            }

            // get
            @Override
            public String settingsSystemGetString(String name) {
                mLogger.i("settingsSystemGetString called: name=%s", name);
                return Settings.System.getString(getContentResolver(), name);
            }

            @Override
            public float settingsSystemGetFloatWithDef(String name, float def) {
                mLogger.i("settingsSystemGetFloatWithDef called: name=%s, def=%f", name, def);
                return Settings.System.getFloat(getContentResolver(), name, def);
            }

            @Override
            public float settingsSystemGetFloat(String name) throws RemoteException {
                mLogger.i("settingsSystemGetFloat called: name=%s", name);
                try {
                    return Settings.System.getFloat(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsSystemGetFloat failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            @Override
            public int settingsSystemGetIntWithDef(String name, int def) {
                mLogger.i("settingsSystemGetIntWithDef called: name=%s, def=%d", name, def);
                return Settings.System.getInt(getContentResolver(), name, def);
            }

            @Override
            public int settingsSystemGetInt(String name) throws RemoteException {
                mLogger.i("settingsSystemGetInt called: name=%s", name);
                try {
                    return Settings.System.getInt(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsSystemGetInt failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            @Override
            public long settingsSystemGetLongWithDef(String name, long def) {
                mLogger.i("settingsSystemGetLongWithDef called: name=%s, def=%d", name, def);
                return Settings.System.getLong(getContentResolver(), name, def);
            }

            @Override
            public long settingsSystemGetLong(String name) throws RemoteException {
                mLogger.i("settingsSystemGetLong called: name=%s", name);
                try {
                    return Settings.System.getLong(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsSystemGetLong failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            // settings secure
            // put
            @Override
            public boolean settingsSecurePutString(String name, String value) {
                mLogger.i("settingsSecurePutString called: name=%s, value=%s", name, value);
                return Settings.Secure.putString(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsSecurePutFloat(String name, float value) {
                mLogger.i("settingsSecurePutFloat called: name=%s, value=%f", name, value);
                return Settings.Secure.putFloat(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsSecurePutInt(String name, int value) {
                mLogger.i("settingsSecurePutInt called: name=%s, value=%d", name, value);
                return Settings.Secure.putInt(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsSecurePutLong(String name, long value) {
                mLogger.i("settingsSecurePutLong called: name=%s, value=%d", name, value);
                return Settings.Secure.putLong(getContentResolver(), name, value);
            }

            // get
            @Override
            public String settingsSecureGetString(String name) {
                mLogger.i("settingsSecureGetString called: name=%s", name);
                return Settings.Secure.getString(getContentResolver(), name);
            }

            @Override
            public float settingsSecureGetFloatWithDef(String name, float def) {
                mLogger.i("settingsSecureGetFloatWithDef called: name=%s, def=%f", name, def);
                return Settings.Secure.getFloat(getContentResolver(), name, def);
            }

            @Override
            public float settingsSecureGetFloat(String name) throws RemoteException {
                mLogger.i("settingsSecureGetFloat called: name=%s", name);
                try {
                    return Settings.Secure.getFloat(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsSecureGetFloat failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            @Override
            public int settingsSecureGetIntWithDef(String name, int def) {
                mLogger.i("settingsSecureGetIntWithDef called: name=%s, def=%d", name, def);
                return Settings.Secure.getInt(getContentResolver(), name, def);
            }

            @Override
            public int settingsSecureGetInt(String name) throws RemoteException {
                mLogger.i("settingsSecureGetInt called: name=%s", name);
                try {
                    return Settings.Secure.getInt(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsSecureGetInt failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            @Override
            public long settingsSecureGetLongWithDef(String name, long def) {
                mLogger.i("settingsSecureGetLongWithDef called: name=%s, def=%d", name, def);
                return Settings.Secure.getLong(getContentResolver(), name, def);
            }

            @Override
            public long settingsSecureGetLong(String name) throws RemoteException {
                mLogger.i("settingsSecureGetLong called: name=%s", name);
                try {
                    return Settings.Secure.getLong(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsSecureGetLong failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            // settings global
            // put
            @Override
            public boolean settingsGlobalPutString(String name, String value) {
                mLogger.i("settingsGlobalPutString called: name=%s, value=%s", name, value);
                return Settings.Global.putString(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsGlobalPutFloat(String name, float value) {
                mLogger.i("settingsGlobalPutFloat called: name=%s, value=%f", name, value);
                return Settings.Global.putFloat(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsGlobalPutInt(String name, int value) {
                mLogger.i("settingsGlobalPutInt called: name=%s, value=%d", name, value);
                return Settings.Global.putInt(getContentResolver(), name, value);
            }

            @Override
            public boolean settingsGlobalPutLong(String name, long value) {
                mLogger.i("settingsGlobalPutLong called: name=%s, value=%d", name, value);
                return Settings.Global.putLong(getContentResolver(), name, value);
            }

            // get
            @Override
            public String settingsGlobalGetString(String name) {
                mLogger.i("settingsGlobalGetString called: name=%s", name);
                return Settings.Global.getString(getContentResolver(), name);
            }

            @Override
            public float settingsGlobalGetFloatWithDef(String name, float def) {
                mLogger.i("settingsGlobalGetFloatWithDef called: name=%s, def=%f", name, def);
                return Settings.Global.getFloat(getContentResolver(), name, def);
            }

            @Override
            public float settingsGlobalGetFloat(String name) throws RemoteException {
                mLogger.i("settingsGlobalGetFloat called: name=%s", name);
                try {
                    return Settings.Global.getFloat(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsGlobalGetFloat failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            @Override
            public int settingsGlobalGetIntWithDef(String name, int def) {
                mLogger.i("settingsGlobalGetIntWithDef called: name=%s, def=%d", name, def);
                return Settings.Global.getInt(getContentResolver(), name, def);
            }

            @Override
            public int settingsGlobalGetInt(String name) throws RemoteException {
                mLogger.i("settingsGlobalGetInt called: name=%s", name);
                try {
                    return Settings.Global.getInt(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsGlobalGetInt failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

            @Override
            public long settingsGlobalGetLongWithDef(String name, long def) {
                mLogger.i("settingsGlobalGetLongWithDef called: name=%s, def=%d", name, def);
                return Settings.Global.getLong(getContentResolver(), name, def);
            }

            @Override
            public long settingsGlobalGetLong(String name) throws RemoteException {
                mLogger.i("settingsGlobalGetLong called: name=%s", name);
                try {
                    return Settings.Global.getLong(getContentResolver(), name);
                } catch (Settings.SettingNotFoundException e) {
                    mLogger.e("settingsGlobalGetLong failed: %s", Log.getStackTraceString(e));
                    throw new RemoteException(Log.getStackTraceString(e));
                }
            }

        };
    }

}

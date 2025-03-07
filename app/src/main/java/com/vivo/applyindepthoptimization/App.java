package com.vivo.applyindepthoptimization;

import android.app.Application;
import android.os.IBinder;
import android.util.Log;

import com.google.android.material.color.DynamicColors;
import com.vivo.applyindepthoptimization.server.Server;
import com.vivo.applyindepthoptimization.ui.dialog.BaseDialogBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class App extends Application {
    private static File sendBinderFile;

    // 服务实例
    public static IService iService;

    // 监听器
    private static final LinkedList<ServiceListener> mListeners = new LinkedList<>();

    public static void onReceive(IBinder binder) {
        iService = IService.Stub.asInterface(binder);
        if (iService != null) {
            sendBinderFile.delete();
            for (ServiceListener listener : mListeners)
                listener.onStart();
        } else {
            try {
                sendBinderFile.createNewFile();
            } catch (IOException ignored) {
            }
            for (ServiceListener listener : mListeners)
                listener.onStop();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        sendBinderFile = new File(getExternalCacheDir(), "send_binder");

        try {
            sendBinderFile.createNewFile();

            FileWriter fw = new FileWriter(new File(getExternalFilesDir(""), "server_starter.sh"));
            fw.write(String.format("""
                    #!/system/bin/sh
                    exitValue=10
                    while [ $exitValue -eq 10 ]; do
                        pkill -f %1$s 2>&1 >/dev/null
                        app_process -Djava.class.path="$(pm path %2$s | sed 's/package://')" /system/bin --nice-name=%1$s %3$s
                        exitValue=$?
                    done
                    exit $exitValue
                    """, Server.TAG, BuildConfig.APPLICATION_ID, Server.class.getName()));
            fw.close();
        } catch (Exception e) {
            Log.e("App", "onCreate: ", e);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static void addListener(ServiceListener listener) {
        mListeners.add(listener);
    }

    public static void removeListener(ServiceListener listener) {
        mListeners.remove(listener);
    }

    public interface ServiceListener {
        void onStart();

        void onStop();
    }

    public static void exToDialog(MainActivity activity, Throwable e) {
        activity.runOnUiThread(() -> {
            try {
                new BaseDialogBuilder(activity)
                        .setTitle("发生异常")
                        .setMessage(Log.getStackTraceString(e))
                        .setPositiveButton("确定", (dialog, which) -> dialog.cancel())
                        .show();
            } catch (BaseDialogBuilder.DialogException ignored) {
            }
        });
    }
}

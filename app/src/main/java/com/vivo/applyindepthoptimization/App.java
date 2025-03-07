package com.vivo.applyindepthoptimization;

import android.app.Application;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.LinkedList;

public class App extends Application {
    // 服务实例
    public static IService iService;

    // 监听器
    private static final LinkedList<ServiceListener> mListeners = new LinkedList<>();

    public static void onReceive(IBinder binder) {
        iService = IService.Stub.asInterface(binder);
        if (iService != null) for (ServiceListener listener : mListeners)
            listener.onStart();
        else for (ServiceListener listener : mListeners)
            listener.onStop();
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
}

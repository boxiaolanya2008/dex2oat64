package com.vivo.applyindepthoptimization;

import android.app.Application;
import android.os.IBinder;

public class App extends Application {
    public static IService iService;

    public static void onServerReceive(IBinder binder) {
        iService = IService.Stub.asInterface(binder);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}

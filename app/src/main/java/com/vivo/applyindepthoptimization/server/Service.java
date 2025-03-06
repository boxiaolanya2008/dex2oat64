package com.vivo.applyindepthoptimization.server;

import android.os.RemoteException;
import android.system.Os;

import com.vivo.applyindepthoptimization.IService;

public class Service extends IService.Stub {
    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public int getUid() {
        return Os.getuid();
    }

    @Override
    public String runCommand(String command) {
        return "";
    }

    @Override
    public boolean isShizuku() {
        return true;
    }
}

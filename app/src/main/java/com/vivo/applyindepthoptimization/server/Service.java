package com.vivo.applyindepthoptimization.server;

import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import com.vivo.applyindepthoptimization.IService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Service extends IService.Stub {
    // Shizuku必须实现方法
    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    // 自定义方法
    @Override
    public int getUid() {
        return Os.getuid();
    }

    @Override
    public String runCommand(String command) throws RemoteException{
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

    @Override
    public boolean isShizuku() {
        return true;
    }
}

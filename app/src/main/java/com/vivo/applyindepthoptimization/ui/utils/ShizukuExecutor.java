package com.vivo.applyindepthoptimization.ui.utils;

import android.os.RemoteException;

import com.vivo.applyindepthoptimization.App;

public class ShizukuExecutor {

/*
    // 使用 Shizuku 执行命令
    public static String runCommand(String command) throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();

        // 使用 Shizuku 执行命令，提供命令、环境变量和工作目录
        Process process = Shizuku.newProcess(command.split(" "), null, null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        process.waitFor();
        return output.toString();
    }
*/

    // 使用 Shizuku 执行命令
    public static String runCommand(String command) throws RemoteException {
        return App.iService.runCommand(command);
    }
}

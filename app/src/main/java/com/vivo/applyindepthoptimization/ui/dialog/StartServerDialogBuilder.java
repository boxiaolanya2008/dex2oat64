package com.vivo.applyindepthoptimization.ui.dialog;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.vivo.applyindepthoptimization.App;
import com.vivo.applyindepthoptimization.MainActivity;
import com.vivo.applyindepthoptimization.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import rikka.shizuku.Shizuku;

public class StartServerDialogBuilder extends BaseDialogBuilder {

    boolean byShizuku;
    boolean br = false;
    TextView t1;
    TextView t2;
    MainActivity mContext;
    Thread h1;
    App.ServiceListener listener = new App.ServiceListener() {
        @Override
        public void onStart() {
            runOnUiThread(() -> {
                getAlertDialog().setCancelable(true);
                getAlertDialog().setTitle("结束");
                Toast.makeText(mContext, "服务启动成功, 窗口将会在5秒后关闭.", Toast.LENGTH_LONG).show();
            });
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            runOnUiThread(getAlertDialog()::cancel);
        }

        @Override
        public void onStop() {
        }
    };

    public StartServerDialogBuilder(@NonNull MainActivity context, boolean shizuku) throws DialogException {
        super(context);
        mContext = context;
        byShizuku = shizuku;
        setView(R.layout.dialog_exec);
        setTitle("执行中...");
        setOnDismissListener(dialog -> onDestroy());
    }

    @Override
    public AlertDialog show() {
        AlertDialog alertDialog = super.show();
        getAlertDialog().setCancelable(false);
        t1 = getAlertDialog().findViewById(R.id.exec_title);
        if (t1 != null) {
            t1.setVisibility(View.GONE);
        }
        t2 = getAlertDialog().findViewById(R.id.exec_msg);
        App.addListener(listener);
        h1 = new Thread(() -> {
            br = false;
            int exitValue;

            try {
                Process p = getProcess();
                OutputStream out = p.getOutputStream();
                out.write(("sh \"" + mContext.getExternalFilesDir("") + "/server_starter.sh\" 2>&1\n").getBytes());
                out.flush();
                out.close();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String inline;
                    while ((inline = reader.readLine()) != null && !br) {
                        String finalInline = inline;
                        runOnUiThread(() -> t2.append(finalInline + "\n"));
                    }
                    reader.close();
                } catch (Exception ignored) {
                }
                p.waitFor();
                exitValue = p.exitValue();
            } catch (Exception e) {
                exitValue = -1;
                runOnUiThread(() -> t2.append(e.getMessage()));
            }
            if (t1 != null) {
                runOnUiThread(() -> t1.setVisibility(View.VISIBLE));
            }
            int finalExitValue = exitValue;
            runOnUiThread(() -> {
                runOnUiThread(() -> t1.append(String.format("返回值: %d (%s)", finalExitValue, finalExitValue == 0 ? "成功" : "失败")));
                getAlertDialog().setTitle("结束");
            });
            br = true;
            getAlertDialog().setCancelable(true);
        });
        h1.start();
        return alertDialog;
    }

    private Process getProcess() throws IOException {
        return byShizuku ? Shizuku.newProcess(new String[]{"sh"}, null, null)
                : Runtime.getRuntime().exec("su");
    }

    public void onDestroy() {
        br = true;
        h1.interrupt();
        App.removeListener(listener);
    }
}
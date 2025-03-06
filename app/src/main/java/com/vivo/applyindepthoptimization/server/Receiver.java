package com.vivo.applyindepthoptimization.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vivo.applyindepthoptimization.App;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Server.ACTION_SERVER_RUNNING.equals(intent.getAction())) {
            BinderContainer binderContainer = intent.getParcelableExtra("binder");
            if (binderContainer != null) {
                App.onServerReceive(binderContainer.binder);
            }
        } else if (Server.ACTION_SERVER_STOPPED.equals(intent.getAction())) {
            App.onServerReceive(null);
        }
    }
}

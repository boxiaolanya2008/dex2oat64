package com.vivo.applyindepthoptimization.ui.dialog;


import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vivo.applyindepthoptimization.MainActivity;


public class BaseDialogBuilder extends MaterialAlertDialogBuilder {
    private final MainActivity mMainActivity;
    private AlertDialog mAlertDialog;
    private DialogInterface.OnDismissListener mOnDismissListener;

    public BaseDialogBuilder(@NonNull MainActivity context) throws DialogException {
        super(context);
        mMainActivity = context;
        if (mMainActivity.isDialogShowing) throw new DialogException();
        mMainActivity.isDialogShowing = true;
        super.setOnDismissListener(dialogInterface -> {
            mMainActivity.isDialogShowing = false;
            if (mOnDismissListener != null)
                mOnDismissListener.onDismiss(dialogInterface);
        });
    }

    public AlertDialog getAlertDialog() {
        return mAlertDialog;
    }

    @NonNull
    @Override
    public AlertDialog create() {
        return mAlertDialog = super.create();
    }

    @NonNull
    @Override
    public BaseDialogBuilder setOnDismissListener(@Nullable DialogInterface.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
        return this;
    }

    public void runOnUiThread(Runnable action) {
        mMainActivity.runOnUiThread(action);
    }

    public static class DialogException extends Exception {
        private DialogException() {
            super("有一个正在显示的弹窗");
        }
    }
}

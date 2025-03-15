package com.vivo.applyindepthoptimization.ui.settings;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.vivo.applyindepthoptimization.MainActivity;
import com.vivo.applyindepthoptimization.R;
import com.vivo.applyindepthoptimization.ui.dialog.BaseDialogBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import rikka.material.preference.MaterialSwitchPreference;
import rikka.preference.SimpleMenuPreference;
import rikka.shizuku.Shizuku;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "ShizukuExecutor";
    private MainActivity mContext;
    private MaterialSwitchPreference monster, turbo, game_resolution;
    private SimpleMenuPreference compile_ways;
    private Preference choose_apps;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        mContext = (MainActivity) requireContext();
        addPreferencesFromResource(R.xml.preference_settings);
        getPreferenceManager().setPreferenceDataStore(new EmptyPreferenceDataStore());
        compile_ways = findPreference("compile_ways");
        choose_apps = findPreference("choose_apps");
        monster = findPreference("monster");
        turbo = findPreference("turbo");
        game_resolution = findPreference("game_resolution");

        monster.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enable = (Boolean) newValue;
            String[] commands = enable ? new String[]{
                    "settings put system power_save_type 7",
                    "settings put secure power_save_type 7",
                    "settings put system game_plus_mode_key 1",
                    "settings put system power_save_type 5",
                    "settings put secure power_save_type 5"
            } : new String[]{
                    "settings put system power_save_type 0",
                    "settings put secure power_save_type 0",
                    "settings put system game_plus_mode_key 0"
            };
            return executeCommandsWithShizuku("Monster 模式", commands);
        });

        turbo.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enable = (Boolean) newValue;
            String[] commands = enable ? new String[]{
                    "pm uninstall --user 0 com.android.virtualmachine.res",
                    "pm uninstall --user 0 com.android.microdroid.empty_payload"
            } : new String[]{
                    "cmd package install-existing com.android.microdroid.empty_payload",
                    "cmd package install-existing com.android.virtualmachine.res",
                    "settings delete global sys_uidcpupower"
            };
            return executeCommandsWithShizuku("优化系统调度", commands);
        });

        game_resolution.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enable = (Boolean) newValue;
            String[] commands = enable ? new String[]{
                    "settings put global vsr_value_from_gamecube 1",
                    "settings put system vivo_sr_status 2"
            } : new String[]{
                    "settings put global vsr_value_from_gamecube 0",
                    "settings put system vivo_sr_status 0"
            };
            return executeCommandsWithShizuku("游戏超分", commands);
        });

        choose_apps.setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog();
            return true;
        });
    }

    private boolean executeCommandsWithShizuku(String actionName, String[] commands) {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0);
            showPermissionDialog();
            return false;
        }

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        int resultCode = 0;

        for (String command : commands) {
            try {
                Process process = Shizuku.newProcess(new String[]{"/system/bin/sh", "-c", command}, null, null);
                BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                String line;
                while ((line = stdout.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = stderr.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }

                resultCode = process.waitFor();
                stdout.close();
                stderr.close();

                if (resultCode != 0) {
                    showErrorDialog(actionName + " 执行失败", "错误码: " + resultCode + "\n错误信息:\n" + errorOutput);
                    Log.e(TAG, "执行失败: " + command + "\n错误码: " + resultCode + "\n" + errorOutput);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorDialog(actionName + " 失败", "异常: " + e.getMessage());
                return false;
            }
        }

        showDialog(actionName + " 成功", "执行结果:\n" + output);
        Log.d(TAG, actionName + " 执行成功: \n" + output);
        return true;
    }

    private void showAppSelectionDialog() {
        PackageManager packageManager = requireContext().getPackageManager();
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppInfo> apps = new ArrayList<>();
        for (ApplicationInfo app : appList) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(app.packageName, 0);
                apps.add(new AppInfo(app.loadLabel(packageManager).toString(), app.packageName, app.loadIcon(packageManager), packageInfo.versionName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        AppListAdapter adapter = new AppListAdapter(mContext, apps);
        try {
            new BaseDialogBuilder(mContext)
                    .setTitle("选择应用")
                    .setAdapter(adapter, (dialog, which) -> {
                        dialog.dismiss();
                        AppInfo selectedApp = apps.get(which);
                        executeCommandsWithShizuku("编译应用 " + selectedApp.packageName, new String[]{"cmd package compile -m speed " + selectedApp.packageName});
                    })
                    .show();
        } catch (BaseDialogBuilder.DialogException e) {
            e.printStackTrace();
        }
    }

    private void showDialog(String title, String message) {
        try {
            new BaseDialogBuilder(mContext)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("确定", null)
                    .show();
        } catch (BaseDialogBuilder.DialogException e) {
            e.printStackTrace();
        }
    }

    private void showErrorDialog(String title, String message) {
        Log.e(TAG, title + ": " + message);
        showDialog(title, message);
    }

    private void showPermissionDialog() {
        showDialog("权限不足", "请授予 Shizuku 权限后重试。");
    }

    private static class AppListAdapter extends ArrayAdapter<AppInfo> {
        public AppListAdapter(@NonNull android.content.Context context, List<AppInfo> apps) {
            super(context, 0, apps);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_app, parent, false);
            }
            AppInfo app = getItem(position);
            ((ImageView) convertView.findViewById(R.id.appIcon)).setImageDrawable(app.icon);
            ((TextView) convertView.findViewById(R.id.appName)).setText(app.name);
            ((TextView) convertView.findViewById(R.id.appVersion)).setText(app.version);
            return convertView;
        }
    }

    private static class AppInfo {
        String name, packageName, version;
        Drawable icon;

        public AppInfo(String name, String packageName, Drawable icon, String version) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
            this.version = version;
        }
    }
}

package com.vivo.applyindepthoptimization.ui.settings;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

import rikka.material.preference.MaterialSwitchPreference;
import rikka.preference.SimpleMenuPreference;

public class SettingsFragment extends PreferenceFragmentCompat {
    private MainActivity mContext;
    private MaterialSwitchPreference monster, turbo, game_resolution;
    private SimpleMenuPreference compile_ways;
    private Preference choose_apps;

    public void refreshStatus() {
        monster.setChecked(getMonster());
        turbo.setChecked(getTurbo());
        game_resolution.setChecked(getGameResolution());
    }

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

        refreshStatus();

        choose_apps.setOnPreferenceClickListener(preference -> {
            showAppSelectionDialog();
            return true;
        });
        
        monster.setOnPreferenceChangeListener((preference, newValue) -> setMonster((Boolean) newValue));
        turbo.setOnPreferenceChangeListener((preference, newValue) -> setTurbo((Boolean) newValue));
        game_resolution.setOnPreferenceChangeListener((preference, newValue) -> setGameResolution((Boolean) newValue));

    }

    // 显示应用选择对话框 (仅用于普通模式)
    private void showAppSelectionDialog() {
        PackageManager packageManager = requireContext().getPackageManager();
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppInfo> apps = new ArrayList<>();

        for (ApplicationInfo app : appList) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(app.packageName, 0);
                String appName = app.loadLabel(packageManager).toString();
                Drawable appIcon = app.loadIcon(packageManager);
                String version = packageInfo.versionName;
                apps.add(new AppInfo(appName, app.packageName, appIcon, version));
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
                        mContext.isDialogShowing = false;
                        var selectedApp = apps.get(which);
                        showConfirmationDialog(selectedApp);
                    })
                    .show();
        } catch (BaseDialogBuilder.DialogException ignored) {
        }
    }

    // 显示确认对话框 (仅用于普通模式)
    private void showConfirmationDialog(AppInfo selectedApp) {
        try {
            new BaseDialogBuilder(mContext)
                    .setTitle("确认选择")
                    .setMessage("你选择了应用：\n" + selectedApp.name + "\n包名：" + selectedApp.packageName + "\n版本：" + selectedApp.version + "\n模式：" + compile_ways.getValue())
                    .setPositiveButton("确定", (dialog, which) -> compile(selectedApp.packageName))
                    .setNegativeButton("取消", null)
                    .show();
        } catch (BaseDialogBuilder.DialogException ignored) {
        }
    }
    
    // 自定义 Adapter
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
            ImageView appIcon = convertView.findViewById(R.id.appIcon);
            TextView appName = convertView.findViewById(R.id.appName);
            TextView appVersion = convertView.findViewById(R.id.appVersion);

            appIcon.setImageDrawable(app.icon);
            appName.setText(app.name);
            appVersion.setText(app.version);
            return convertView;
        }
    }

    // 应用信息类
    private record AppInfo(String name, String packageName, Drawable icon, String version) {}
    
    // TODO
    public void compile(String packageName) {
        var mode = compile_ways.getValue();

    }

    // TODO
    public boolean getMonster() {
        return false;
    }

    // TODO
    public boolean setMonster(boolean enable) {
        return false;
    }

    // TODO
    public boolean getTurbo() {
        return false;
    }

    // TODO
    public boolean setTurbo(boolean enable) {
        return false;
    }

    // TODO
    public boolean getGameResolution() {
        return false;
    }

    // TODO
    public boolean setGameResolution(boolean enable) {
        return false;
    }
}
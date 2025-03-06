package com.vivo.applyindepthoptimization.ui.dashboard;

import  android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vivo.applyindepthoptimization.R;
import com.vivo.applyindepthoptimization.databinding.FragmentDashboardBinding;
import com.vivo.applyindepthoptimization.ui.utils.ShizukuExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private String selectedMode; // 用于普通模式 (mode1 - mode8)
    private AppInfo selectedApp;
    private boolean isMonsterMode = false; // 标记是否为Monster模式

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 设置普通模式点击监听器
        binding.btnSelectMode.setOnClickListener(v -> showModeSelectionDialog());
        binding.textClickHere.setOnClickListener(v -> showModeSelectionDialog());

        // 设置Monster模式点击监听器
        binding.btnMonsterMode.setOnClickListener(v -> {
            isMonsterMode = true;
            binding.textMonsterStatus.setText("Monster模式已激活");
            executeMonsterCommand(); // 直接执行Monster模式的ADB命令
        });
        binding.textMonsterStatus.setOnClickListener(v -> {
            isMonsterMode = true;
            binding.textMonsterStatus.setText("Monster模式已激活");
            executeMonsterCommand(); // 直接执行Monster模式的ADB命令
        });

        // 设置根视图的摇杆事件监听器
        root.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (event.getSource() == InputDevice.SOURCE_JOYSTICK &&
                        event.getAction() == MotionEvent.ACTION_MOVE) {
                    float xAxis = event.getAxisValue(MotionEvent.AXIS_X);
                    // 摇杆向右推到最大（值接近1.0）时触发Monster模式
                    if (xAxis > 0.9f) {
                        isMonsterMode = true;
                        binding.textMonsterStatus.setText("Monster模式已激活 (摇杆触发)");
                        binding.textClickHere.setText("点击选择模式"); // 重置普通模式状态
                        executeMonsterCommand(); // 直接执行Monster模式的ADB命令
                        return true;
                    }
                }
                return false; // 未处理时返回false
            }
        });

        // 使根视图可聚焦以接收输入事件
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        root.requestFocus();

        return root;
    }

    // 显示普通模式选择对话框 (不包含Monster模式)
    private void showModeSelectionDialog() {
        String[] modes = {
                "模式1: verify", "模式2: quicken", "模式3: speed-profile",
                "模式4: space-profile", "模式5: everything", "模式6: speed",
                "模式7: space", "模式8: balanced"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择模式")
                .setItems(modes, (dialog, which) -> {
                    selectedMode = "mode" + (which + 1);
                    isMonsterMode = false; // 确保不是Monster模式
                    binding.textClickHere.setText("已选择模式: " + modes[which]);
                    binding.textMonsterStatus.setText("使用摇杆或点击激活"); // 重置Monster状态
                    showAppSelectionDialog();
                })
                .show();
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

        AppListAdapter adapter = new AppListAdapter(requireContext(), apps);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择应用")
                .setAdapter(adapter, (dialog, which) -> {
                    selectedApp = apps.get(which);
                    showConfirmationDialog(selectedApp);
                })
                .show();
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

    // 显示确认对话框 (仅用于普通模式)
    private void showConfirmationDialog(AppInfo selectedApp) {
        String modeText = "模式 " + selectedMode;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认选择")
                .setMessage("你选择了应用：\n" + selectedApp.name + "\n包名：" + selectedApp.packageName + "\n版本：" + selectedApp.version + "\n模式：" + modeText)
                .setPositiveButton("确定", (dialog, which) -> executeCommand(selectedApp.packageName))
                .setNegativeButton("取消", null)
                .show();
    }

    // 执行普通模式的编译命令
    private void executeCommand(String packageName) {
        String command = "";
        switch (selectedMode) {
            case "mode1": command = "cmd package compile -m verify -f " + packageName; break;
            case "mode2": command = "cmd package compile -m quicken -f " + packageName; break;
            case "mode3": command = "cmd package compile -m speed-profile -f " + packageName; break;
            case "mode4": command = "cmd package compile -m space-profile -f " + packageName; break;
            case "mode5": command = "cmd package compile -m everything -f " + packageName; break;
            case "mode6": command = "cmd package compile -m speed -f " + packageName; break;
            case "mode7": command = "cmd package compile -m space -f " + packageName; break;
            case "mode8": command = "cmd package compile -m balanced -f " + packageName; break;
        }

        try {
            String output = ShizukuExecutor.runCommand(command);
            binding.tvSelectedApp.setText("已编译应用: " + selectedApp.name);
            showCompletionDialog(output);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            showErrorDialog(e.getMessage());
        }
    }

    // 执行Monster模式的ADB命令
    private void executeMonsterCommand() {
        String[] commands = {
            "settings put system power_save_type 7",
            "settings put secure power_save_type 7",
            "settings put system game_plus_mode_key 1",
            "settings put system power_save_type 5",
            "settings put secure power_save_type 5"
        };

        StringBuilder outputBuilder = new StringBuilder();
        boolean success = true;

        for (String command : commands) {
            try {
                String output = ShizukuExecutor.runCommand(command);
                if (!output.isEmpty()) {
                    outputBuilder.append(command).append(" 输出: ").append(output).append("\n");
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                success = false;
                outputBuilder.append(command).append(" 失败: ").append(e.getMessage()).append("\n");
            }
        }

        if (success) {
            binding.tvSelectedApp.setText("Monster+模式已执行");
            showCompletionDialog(outputBuilder.length() == 0 ? "Monster模式命令执行成功，无输出" : outputBuilder.toString());
        } else {
            showErrorDialog(outputBuilder.toString());
        }
    }

    // 显示命令完成对话框
    private void showCompletionDialog(String output) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("运行完成")
                .setMessage("命令执行完成！\n\n" + output)
                .setPositiveButton("确定", null)
                .show();
    }

    // 显示错误对话框
    private void showErrorDialog(String errorMessage) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("错误")
                .setMessage("命令执行失败！\n\n" + errorMessage)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // 应用信息类
    private static class AppInfo {
        String name;
        String packageName;
        Drawable icon;
        String version;

        AppInfo(String name, String packageName, Drawable icon, String version) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
            this.version = version;
        }
    }
}
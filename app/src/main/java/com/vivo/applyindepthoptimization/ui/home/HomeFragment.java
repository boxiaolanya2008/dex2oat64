package com.vivo.applyindepthoptimization.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vivo.applyindepthoptimization.databinding.FragmentHomeBinding;
import com.vivo.applyindepthoptimization.ui.utils.ShizukuExecutor;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog currentAlertDialog;  // 用于跟踪当前对话框

    private long startTime;
    private int totalApps;
    private int processedApps = 0;
    private volatile boolean isCancelled = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setUpButtonListeners();
        return root;
    }

    private void setUpButtonListeners() {
        binding.verifyButton.setOnClickListener(v -> executeForAllApps("verify"));
        binding.quickenButton.setOnClickListener(v -> executeForAllApps("quicken"));
        binding.speedProfileButton.setOnClickListener(v -> executeForAllApps("speed-profile"));
        binding.spaceProfileButton.setOnClickListener(v -> executeForAllApps("space-profile"));
        binding.balancedButton.setOnClickListener(v -> executeForAllApps("balanced"));
        binding.speedButton.setOnClickListener(v -> executeForAllApps("speed"));
        binding.spaceButton.setOnClickListener(v -> executeForAllApps("space"));
        binding.resetButton.setOnClickListener(v -> showResetDialog());
    }

    private void showResetDialog() {
        Context context = requireContext();

        new MaterialAlertDialogBuilder(context)
                .setTitle("选择重置方式")
                .setMessage("请选择要执行的重置命令：")
                .setPositiveButton("重置1", (dialog, which) -> executeResetCommand(1))
                .setNegativeButton("重置2", (dialog, which) -> executeResetCommand(2))
                .setCancelable(false)
                .show();
    }

    private void executeResetCommand(int resetType) {
        Context context = requireContext();
        
        // 创建新的输出视图
        final TextView outputTextView = new TextView(context);
        outputTextView.setText("命令执行中...\n");
        outputTextView.setPadding(20, 20, 20, 20);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(outputTextView);

        // 关闭之前的对话框（如果存在）
        if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
            currentAlertDialog.dismiss();
        }

        // 创建新对话框
        currentAlertDialog = new MaterialAlertDialogBuilder(context)
                .setTitle("命令执行")
                .setView(scrollView)
                .setCancelable(false)
                .setNegativeButton("取消", (dialog, which) -> {
                    isCancelled = true;
                    currentAlertDialog = null;
                })
                .show();

        // 重置状态
        resetTaskState();
        
        new Thread(() -> executeResetCommandForAllApps(resetType, outputTextView)).start();
    }

    private void executeResetCommandForAllApps(int resetType, TextView outputTextView) {
        try {
            List<String> packageNames = getAllInstalledPackages();
            totalApps = packageNames.size();
            startTime = System.currentTimeMillis();

            handler.post(() -> outputTextView.setText("开始执行重置命令，请稍候...\n\n"));

            for (String packageName : packageNames) {
                if (isCancelled) {
                    handler.post(() -> outputTextView.append("任务已取消\n"));
                    break;
                }

                String fullCommand = resetType == 1 
                        ? "cmd package compile --reset " + packageName 
                        : "pm compile --reset " + packageName;

                Log.d("HomeFragment", "Executing command: " + fullCommand);
                String result = ShizukuExecutor.runCommand(fullCommand);

                processedApps++;
                updateProgress(outputTextView, packageName, result);
            }

            handler.post(() -> outputTextView.append("所有命令执行完成！"));
        } catch (Exception e) {
            Log.e("HomeFragment", "Error executing reset command", e);
            handler.post(() -> outputTextView.append("\n命令执行失败: " + e.getMessage()));
        } finally {
            currentAlertDialog = null;
        }
    }

    private void executeForAllApps(String mode) {
        Context context = requireContext();
        
        // 创建新的输出视图
        final TextView outputTextView = new TextView(context);
        outputTextView.setText("命令执行中...\n");
        outputTextView.setPadding(20, 20, 20, 20);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(outputTextView);

        // 关闭之前的对话框（如果存在）
        if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
            currentAlertDialog.dismiss();
        }

        // 创建新对话框
        currentAlertDialog = new MaterialAlertDialogBuilder(context)
                .setTitle("命令执行")
                .setView(scrollView)
                .setCancelable(false)
                .setNegativeButton("取消", (dialog, which) -> {
                    isCancelled = true;
                    currentAlertDialog = null;
                })
                .show();

        // 重置状态
        resetTaskState();
        
        new Thread(() -> executeCommandForAllApps(mode, outputTextView)).start();
    }

    private void executeCommandForAllApps(String mode, TextView outputTextView) {
        try {
            List<String> packageNames = getAllInstalledPackages();
            totalApps = packageNames.size();
            startTime = System.currentTimeMillis();

            handler.post(() -> outputTextView.setText("开始执行命令，请稍候...\n\n"));

            for (String packageName : packageNames) {
                if (isCancelled) {
                    handler.post(() -> outputTextView.append("任务已取消\n"));
                    break;
                }

                String fullCommand = buildCommandForMode(mode, packageName);
                Log.d("HomeFragment", "Executing command: " + fullCommand);
                String result = ShizukuExecutor.runCommand(fullCommand);

                processedApps++;
                updateProgress(outputTextView, packageName, result);
            }

            handler.post(() -> outputTextView.append("所有命令执行完成！"));
        } catch (Exception e) {
            Log.e("HomeFragment", "Error executing command", e);
            handler.post(() -> outputTextView.append("\n命令执行失败: " + e.getMessage()));
        } finally {
            currentAlertDialog = null;
        }
    }

    private void resetTaskState() {
        isCancelled = false;
        processedApps = 0;
        totalApps = 0;
        startTime = System.currentTimeMillis();
    }

    private void updateProgress(TextView outputTextView, String packageName, String result) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long estimatedTimeRemaining = (elapsedTime * (totalApps - processedApps)) / processedApps;

        String updateText = String.format("正在处理: %s\n结果: %s\n进度: %d/%d, 预计剩余时间: %s\n\n",
                packageName, result, processedApps, totalApps, formatTime(estimatedTimeRemaining));
        handler.post(() -> outputTextView.append(updateText));
    }

    private String buildCommandForMode(String mode, String packageName) {
        switch (mode) {
            case "verify":
                return "cmd package compile -m verify -f " + packageName;
            case "quicken":
                return "cmd package compile -m quicken -f " + packageName;
            case "speed-profile":
                return "cmd package compile -m speed-profile -f " + packageName;
            case "space-profile":
                return "cmd package compile -m space-profile -f " + packageName;
            case "balanced":
                return "cmd package compile -m balanced -f " + packageName;
            case "speed":
                return "cmd package compile -m speed -f " + packageName;
            case "space":
                return "cmd package compile -m space -f " + packageName;
            default:
                return "";
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private List<String> getAllInstalledPackages() {
        List<String> packageNames = new ArrayList<>();
        for (android.content.pm.PackageInfo packageInfo : 
                requireContext().getPackageManager().getInstalledPackages(0)) {
            packageNames.add(packageInfo.packageName);
        }
        return packageNames;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理对话框引用
        if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
            currentAlertDialog.dismiss();
            currentAlertDialog = null;
        }
        binding = null;
    }
}
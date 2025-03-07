package com.vivo.applyindepthoptimization;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vivo.applyindepthoptimization.databinding.ActivityMainBinding;
import com.vivo.applyindepthoptimization.ui.dialog.BaseDialogBuilder;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 100;
    private static final int QUERY_ALL_PACKAGES_PERMISSION_REQUEST_CODE = 101;
    private ActivityMainBinding binding;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    public boolean isDialogShowing = false;
    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener = (requestCode, grantResult) -> {
        Log.d(TAG, "Shizuku 权限请求结果: requestCode=" + requestCode + ", grantResult=" + grantResult);
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                onShizukuPermissionGranted();
            } else {
                onShizukuPermissionDenied();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DynamicColors.applyToActivityIfAvailable(this);

        EdgeToEdge.enable(this, SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));

        if (!checkDeviceBrand()) {
            showBrandCheckDialog();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.inflateMenu(R.menu.main_menu);
        if (this instanceof MenuProvider self) {
            self.onPrepareMenu(binding.toolbar.getMenu());
        }

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.home_start) {

            } else if (item.getItemId() == R.id.home_stop) {
                if (App.iService != null) {
                    try {
                        App.iService.exit();
                    } catch (RemoteException e) {
                        App.exToDialog(this, e);
                    }
                } else {
                    Toast.makeText(this, "服务未启动", Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });

        initNavigation();

        executorService.execute(() -> {
            if (!checkDependencies()) {
                Log.e(TAG, "依赖检查失败，停止后续操作");
                return;
            }
            runOnUiThread(() -> {
                Log.d(TAG, "注册 Shizuku 权限监听");
                Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
                checkAndRequestShizukuPermission();
            });
        });
        checkAndRequestQueryAllPackagesPermission();
    }

    private boolean checkDeviceBrand() {
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        boolean isSupported = manufacturer.contains("vivo") || manufacturer.contains("oppo");
        Log.d(TAG, "设备品牌检查: " + manufacturer + ", 是否支持: " + isSupported);
        isSupported = true;
        return isSupported;
    }

    private void showBrandCheckDialog() {
        try {
            new BaseDialogBuilder(this)
                    .setTitle("设备不兼容")
                    .setMessage("本应用仅支持vivo和OPPO品牌设备")
                    .setCancelable(false)
                    .setPositiveButton("退出", (dialog, which) -> finish())
                    .setOnDismissListener(dialog -> finish())
                    .show();
        } catch (BaseDialogBuilder.DialogException ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
        executorService.shutdownNow();
        super.onDestroy();
    }

    private void initNavigation() {
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(
                binding.toolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private boolean checkDependencies() {
        if (!Shizuku.pingBinder()) {
            Log.w(TAG, "Shizuku 服务未启动或不可用");
            runOnUiThread(() -> showAlertDialog(
                    "Shizuku 不可用",
                    "Shizuku 服务未启动或设备不支持，请确保已安装并启动 Shizuku 服务后重试。",
                    "了解更多",
                    () -> openUrl("https://github.com/RikkaApps/Shizuku/"),
                    true
            ));
            return false;
        }
        Log.d(TAG, "Shizuku 服务已就绪");
        return true;
    }

    private void checkAndRequestShizukuPermission() {
        // 临时强制触发权限请求，调试弹窗问题
        Log.d(TAG, "检查 Shizuku 权限状态: " + Shizuku.checkSelfPermission());
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Shizuku 权限已授予");
            onShizukuPermissionGranted();
        } else {
            Log.d(TAG, "Shizuku 权限未授予，开始请求");
            requestShizukuPermission();
        }
        // 如果仍不弹出，可以临时注释掉上面 if-else，直接调用 requestShizukuPermission();
    }

    private void requestShizukuPermission() {
        showAlertDialog(
                "需要 Shizuku 权限",
                "应用需要 Shizuku 权限以执行高级操作，请授权。",
                "授权",
                () -> {
                    Log.d(TAG, "用户点击授权，开始请求 Shizuku 权限");
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
                },
                false
        );
    }

    private void checkAndRequestQueryAllPackagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.QUERY_ALL_PACKAGES}, QUERY_ALL_PACKAGES_PERMISSION_REQUEST_CODE);

            } else {
                onQueryAllPackagesPermissionGranted();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == QUERY_ALL_PACKAGES_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onQueryAllPackagesPermissionGranted();
            } else {
                onQueryAllPackagesPermissionDenied();
            }
        }
    }

    private void onQueryAllPackagesPermissionGranted() {
        // 修改：授权成功后不再显示弹窗，仅记录日志
        Log.d(TAG, "读取已安装应用列表权限已授予");
    }

    private void onQueryAllPackagesPermissionDenied() {
        showAlertDialog("权限被拒绝", "未授予 读取已安装应用列表 权限，应用将无法查询应用列表。", "重试", this::checkAndRequestQueryAllPackagesPermission, false);
    }

    private void showAlertDialog(String title, String message, String positive, Runnable onPositive, boolean finishOnCancel) {
        runOnUiThread(() -> {
            try {
                MaterialAlertDialogBuilder builder = new BaseDialogBuilder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(positive, (dialog, which) -> {
                            if (onPositive != null) onPositive.run();
                        });

                if (finishOnCancel) {
                    builder.setNegativeButton("退出", (dialog, which) -> finish());
                }

                builder.show();
                Log.d(TAG, "显示对话框: " + title);
            } catch (BaseDialogBuilder.DialogException ignored) {
            }
        });
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void onShizukuPermissionGranted() {
        showAlertDialog("权限已授予", "Shizuku 权限已成功授予，应用现在可以正常运行。", "好的", null, false);
    }

    private void onShizukuPermissionDenied() {
        showAlertDialog("权限被拒绝", "未授予 Shizuku 权限，应用将无法执行某些操作。", "重试", this::checkAndRequestShizukuPermission, false);
    }
}
package pub.carzy.auto_script.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogView;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.vdurmont.semver4j.Semver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ComAboutUpdateVersionBinding;
import pub.carzy.auto_script.databinding.DownloadProgressBinding;
import pub.carzy.auto_script.databinding.ViewAboutBinding;
import pub.carzy.auto_script.entity.CheckVersionResponse;
import pub.carzy.auto_script.model.AboutModel;
import pub.carzy.auto_script.model.AboutUpdateVersionModel;
import pub.carzy.auto_script.model.DownloadProgressModel;
import pub.carzy.auto_script.ui.BottomCustomSheetBuilder;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.ui.entity.PageMappingInflater;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.BeanHandler;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class AboutActivity extends BaseActivity {
    private ViewAboutBinding binding;
    private AboutModel model;
    private QMUITipDialog dialog;

    private Call call;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_about);
        model = new AboutModel();
        binding.setModel(model);
        initTopBar();
        init();
        initGroupListView();
    }

    private void init() {
        binding.versionLayout.setOnClickListener(e -> {
            Runnable runnable = () -> {
                //弹出更新中
                dialog = new QMUITipDialog.Builder(AboutActivity.this)
                        .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                        .setTipWord(getString(R.string.checking_update))
                        .create();
                dialog.setOnDismissListener(i -> {
                    //如果请求未完成就取消
                    if (call != null && !call.isExecuted() && !call.isCanceled()) {
                        call.cancel();
                    }
                });
                dialog.show();
            };
            //检查版本
            if (!model.isChecking()) {
                model.setChecking(true);
                //检查版本
                checkVersion();
                runnable.run();
            } else {
                runnable.run();
            }
        });
    }

    private void checkVersion() {
        ThreadUtil.runOnCpu(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.github.com/repos/Dylan-lijl/auto-script/releases/latest")
                    .build();
            call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    //失败
                    ThreadUtil.runOnUi(() -> {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        //弹出错误
                        QMUITipDialog tipDialog = new QMUITipDialog.Builder(AboutActivity.this)
                                .setIconType(QMUITipDialog.Builder.ICON_TYPE_FAIL)
                                .setTipWord(getString(R.string.checking_update_failed))
                                .create();
                        tipDialog.show();
                        //延迟关闭
                        ThreadUtil.runOnUi(tipDialog::dismiss, 3000);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    ThreadUtil.runOnUi(() -> {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    });
                    ResponseBody body = response.body();
                    if (body == null) {
                        return;
                    }
                    Gson gson = new Gson();
                    CheckVersionResponse res = gson.fromJson(body.string(), CheckVersionResponse.class);
                    if (res.getMessage() != null) {
                        ThreadUtil.runOnUi(() -> {
                            QMUITipDialog tipDialog = new QMUITipDialog.Builder(AboutActivity.this)
                                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_FAIL)
                                    .setTipWord(getString(R.string.checking_update_failed) + ":" + res.getMessage())
                                    .create();
                            tipDialog.show();
                            //延迟关闭
                            ThreadUtil.runOnUi(tipDialog::dismiss, 3000);
                        });
                        return;
                    }
                    model.setResponse(res);
                    //版本小于tag才显示
                    if (new Semver(ActivityUtils.getVersionName(AboutActivity.this)).isLowerThan(res.getTagName())) {
                        showUpdateDialog();
                    } else {
                        ThreadUtil.runOnUi(() -> Toast.makeText(AboutActivity.this, R.string.no_new_version, Toast.LENGTH_LONG).show());
                    }
                }
            });
        });
    }

    private void showUpdateDialog() {
        CheckVersionResponse response = model.getResponse();
        if (response == null) {
            return;
        }
        //这里需要使用布局
        AboutUpdateVersionModel copy = BeanHandler.copy(response, AboutUpdateVersionModel.class);
        if (response.getAssets() != null) {
            for (CheckVersionResponse.Asset item : response.getAssets()) {
                if (!("application/octet-stream".equals(item.getContentType()) && item.getName().endsWith(".apk"))) {
                    continue;
                }
                copy.setAppFileName(item.getName());
                copy.setBrowserDownloadUrl(item.getBrowserDownloadUrl());
                copy.setContentType(item.getContentType());
                copy.setSize(item.getSize());
                copy.setUrl(item.getUrl());
                copy.setDownloadCount(item.getDownloadCount());
                if (item.getUploader() != null) {
                    copy.setLogin(item.getUploader().getLogin());
                    copy.setHtmlUrl(item.getUploader().getHtmlUrl());
                }
            }
        }
        AtomicReference<QMUIBottomSheet> build = new AtomicReference<>(null);
        ComAboutUpdateVersionBinding inflate = ComAboutUpdateVersionBinding.inflate(LayoutInflater.from(this));
        inflate.setModel(copy);
        inflate.fileBtn.setOnClickListener(e -> {
            //复制下载地址
            ActivityUtils.copyToClipboard(AboutActivity.this, "link", copy.getBrowserDownloadUrl());
            Toast.makeText(AboutActivity.this, R.string.copied, Toast.LENGTH_SHORT).show();
        });
        inflate.publisherBtn.setOnClickListener(e -> {
            //跳转到对应界面
            ActivityUtils.openToBrowser(AboutActivity.this, copy.getHtmlUrl());
        });
        inflate.releasePageBtn.setOnClickListener(e -> {
            //跳转到对应界面
            ActivityUtils.openToBrowser(AboutActivity.this, copy.getUrl());
        });
        inflate.updateVersionBtn.setOnClickListener(e -> {
            //下载文件
            updateSoftware(copy.getBrowserDownloadUrl(), copy.getSize(), (file) -> ThreadUtil.runOnUi(() -> {
                if (build.get() != null) {
                    build.get().dismiss();
                }
                //安装
                installApk(file);
            }));
        });
        BottomCustomSheetBuilder builder = new BottomCustomSheetBuilder(this);
        builder.addView(inflate.getRoot())
                .setContentPaddingDp(20, 20)
                .setTitle(getString(R.string.new_version));
        build.set(builder.build());
        build.get().show();
    }

    ExecutorService executor = Executors.newSingleThreadExecutor();

    private void updateSoftware(String url, Integer size, Consumer<File> success) {
        if (url == null) {
            return;
        }
        //下载文件并显示进度
        //创建进度条
        DownloadProgressModel m = new DownloadProgressModel();
        DownloadProgressBinding inflate = DownloadProgressBinding.inflate(LayoutInflater.from(this));
        inflate.setModel(m);
        AtomicReference<Future<?>> future = new AtomicReference<>();
        QMUIDialog qmuiDialog = new QMUIDialog.CustomDialogBuilder(this) {
            @Nullable
            @Override
            protected View onCreateContent(QMUIDialog dialog, QMUIDialogView parent, Context context) {
                return inflate.getRoot();
            }
        }
                .setTitle(getString(R.string.download_progress))
                .addAction(new QMUIDialogAction(this, R.string.cancel).onClick((dialog, index) -> {
                    dialog.dismiss();
                    //取消下载任务
                    if (future.get() != null && !future.get().isDone() && !future.get().isCancelled()) {
                        future.get().cancel(true);
                    }
                }))
                .create();
        qmuiDialog.show();
        //启动任务
        future.set(executor.submit(() -> downloadFile(url, size, m, success)));
    }

    private void installApk(File file) {
        Context context = this;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Android 7.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file
            );
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }

        context.startActivity(intent);
    }

    private void downloadFile(String url, int totalSize, DownloadProgressModel m, Consumer<File> success) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return;
            }
            File out = new File(getExternalFilesDir(null), UUID.randomUUID().toString() + ".apk");
            if (out.exists()) {
                if (!out.delete()) {
                    Log.d(this.getClass().getName(), "delete file failed");
                    return;
                }
            }
            if (!out.createNewFile()) {
                Log.d(this.getClass().getName(), "create file failed");
                return;
            }
            try (InputStream is = body.byteStream(); FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buffer = new byte[4096];
                long downloaded = 0;
                int read;

                while ((read = is.read(buffer)) != -1 && !Thread.currentThread().isInterrupted()) {
                    fos.write(buffer, 0, read);
                    downloaded += read;

                    int progress = (int) (downloaded * 100L / totalSize);
                    ThreadUtil.runOnUi(() -> m.setProgress(progress));
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            ThreadUtil.runOnUi(() -> success.accept(out));
        } catch (Exception ignored) {
        }
    }


    private void initGroupListView() {
        QMUIGroupListView listView = binding.groupListView;
        //使用xml进行加载
        List<PageMappingInflater.PageItem> list = PageMappingInflater.inflate(this, R.xml.about_page_mapping);
        //构建列表项
        for (PageMappingInflater.PageItem item : list) {
            if (!item.isEnabled()) {
                continue;
            }
            QMUICommonListItemView itemView = listView.createItemView(item.getTitle() == null ? "" : item.getTitle());
            itemView.setOrientation(QMUICommonListItemView.HORIZONTAL);
            //设置右箭头
            itemView.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
            if (item.getIcon() != null) {
                itemView.setImageDrawable(item.getIcon());
            }
            QMUIGroupListView.Section section = QMUIGroupListView.newSection(this);
            section.addItemView(itemView, v -> clickView(item, v));
            section.addTo(listView);
        }
    }

    /**
     * 点击列表项跳转到对应activity
     *
     * @param item item
     * @param v    v
     */
    private void clickView(PageMappingInflater.PageItem item, View v) {
        if (item.getActivity() == null) {
            return;
        }
        Intent intent = new Intent(this, item.getActivity());
        intent.putExtra("id", item.getId());
        startActivity(intent);
    }

    @Override
    protected void openBottomSheet() {
        QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                .setGravityCenter(false)
                .setAddCancelBtn(false)
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    if (tag == null) {
                        return;
                    }
                    int id = ActionInflater.ActionItem.stringToId(tag);
                    if (defaultProcessMenu(id)) {
                        return;
                    }
                });
        addActionByXml(builder, this, R.xml.actions_common,
                (b, m, item) -> {
                    if (item.getId() == R.id.menu_about) {
                        return;
                    }
                    b.addItem(m);
                });
        QMUIBottomSheet build = builder.build();
        build.show();
    }

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }
}

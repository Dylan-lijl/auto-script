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
import com.qmuiteam.qmui.widget.QMUIProgressBar;
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
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.noties.markwon.Markwon;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.config.BeanFactory;
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
import pub.carzy.auto_script.utils.StringUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * 关于
 * @author admin
 */
public class AboutActivity extends BaseActivity {
    private ViewAboutBinding binding;
    private AboutModel model;
    private QMUITipDialog dialog;
    private Markwon markwon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_about);
        markwon = BeanFactory.getInstance().get(Markwon.class);
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
                dialog.setOnDismissListener(i -> cancelCalls());
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

    private void cancelCalls() {
        cancelCalls(null);
    }

    private void cancelCalls(Call c) {
        //如果请求未完成就取消
        for (Call call : calls) {
            if (call != null && !call.isCanceled() && c != call) {
                try {
                    call.cancel();
                } catch (Exception exception) {
                    Log.w("pub.carzy.auto_script.activities.AboutActivity.init", "取消任务失败!", exception);
                }
            }
        }
    }

    private static final String[] UPDATE_URLS = {"https://api.github.com/repos/Dylan-lijl/auto-script/releases/latest", "https://gitee.com/api/v5/repos/Dylan-lijl/auto-script/releases/latest"};
    private final Call[] calls = new Call[UPDATE_URLS.length];

    /**
     * 检查版本
     */
    private void checkVersion() {
        ThreadUtil.runOnCpu(() -> {
            //60秒
            OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(60 * 1000, TimeUnit.MILLISECONDS).build();
            //是否已经被处理
            AtomicReference<Boolean> finished = new AtomicReference<>(false);
            //遍历创建请求
            for (int i = 0; i < UPDATE_URLS.length; i++) {
                String url = UPDATE_URLS[i];
                calls[i] = client.newCall(new Request.Builder()
                        .url(url)
                        .build());
            }
            //回调
            Callback callback = new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    //已经被别人处理就直接返回
                    if (finished.get()) {
                        return;
                    }
                    //设置当前是自己在处理
                    finished.set(true);
                    //取消其他任务
                    cancelCalls(call);
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
                    //已经被别人处理就直接返回
                    if (finished.get()) {
                        return;
                    }
                    //设置当前是自己在处理
                    finished.set(true);
                    //取消其他任务
                    cancelCalls(call);
                    ThreadUtil.runOnUi(() -> {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    });
                    ResponseBody body = response.body();
                    if (body == null) {
                        return;
                    }
                    String content = body.string();
                    Gson gson = new Gson();
                    //解析json
                    CheckVersionResponse res = gson.fromJson(content, CheckVersionResponse.class);
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
                    //获取当前版本
                    String versionName = ActivityUtils.getVersionName(AboutActivity.this);
//                    String versionName = "0.0.9";
                    //版本小于tag才显示
                    if (new Semver(versionName).isLowerThan(res.getTagName().replace("v", ""))) {
                        //展示更新弹窗
                        showUpdateDialog();
                    } else {
                        ThreadUtil.runOnUi(() -> Toast.makeText(AboutActivity.this, R.string.no_new_version, Toast.LENGTH_LONG).show());
                    }
                }
            };
            //循环调用
            for (Call c : calls) {
                c.enqueue(callback);
            }
        });
    }

    /**
     * 展示更新弹窗
     */
    private void showUpdateDialog() {
        CheckVersionResponse response = model.getResponse();
        if (response == null) {
            return;
        }
        //这里需要使用布局
        AboutUpdateVersionModel copy = BeanHandler.copy(response, AboutUpdateVersionModel.class);
        //获取资源信息
        if (response.getAssets() != null) {
            for (CheckVersionResponse.Asset item : response.getAssets()) {
                //跳过debug安装包和不是安装包的资源
                if (!item.getName().endsWith(".apk") || item.getName().contains("debug.")) {
                    continue;
                }
                copy.setAppFileName(item.getName());
                copy.setBrowserDownloadUrl(item.getBrowserDownloadUrl());
                copy.setContentType(item.getContentType());
                copy.setSize(item.getSize());
                copy.setUrl(item.getUrl());
                copy.setLabel(item.getLabel());
                copy.setDigest(item.getDigest());
                copy.setDownloadCount(item.getDownloadCount());
                if (item.getUploader() != null) {
                    copy.setLogin(item.getUploader().getLogin());
                    copy.setHtmlUrl(item.getUploader().getHtmlUrl());
                }
            }
        }
        ThreadUtil.runOnUi(() -> {
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
                //下载文件进行更新
                updateSoftware(copy.getBrowserDownloadUrl(), (file) -> ThreadUtil.runOnUi(() -> {
                    if (build.get() != null) {
                        build.get().dismiss();
                    }
                    //安装apk
                    installApk(file);
                }));
            });
            //更新内容
            if (!StringUtils.isEmpty(copy.getBody())) {
                markwon.setMarkdown(inflate.body, copy.getBody());
            }
            BottomCustomSheetBuilder builder = new BottomCustomSheetBuilder(this);
            builder.addView(inflate.getRoot())
                    .setContentPaddingDp(20, 20)
                    .setTitle(getString(R.string.new_version));
            build.set(builder.build());
            build.get().show();
        });
    }

    ExecutorService executor = Executors.newSingleThreadExecutor();

    private void updateSoftware(String url, Consumer<File> success) {
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
                .setCanceledOnTouchOutside(false)
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
        future.set(executor.submit(() -> downloadFile(url,
                (v) -> ThreadUtil.runOnUi(() -> m.setMax(v.intValue())),
                (v) -> ThreadUtil.runOnUi(() -> m.setProgress(v.intValue())), success)));
    }

    private void installApk(File file) {
        Context context = this;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Android 7.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileProvider",
                    file
            );
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }

        context.startActivity(intent);
    }

    /**
     * 下载文件
     * @param url 路径
     * @param setMaxValue 设置文件最大值回调
     * @param updateProcess 更新进度回调
     * @param success 成功回调
     */
    private void downloadFile(String url, Consumer<Long> setMaxValue, Consumer<Long> updateProcess, Consumer<File> success) {
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
            if (setMaxValue != null) {
                //更新文件大小
                long length = body.contentLength();
                setMaxValue.accept(length);
            }
            //创建文件,这里需要跟配置路径一致
            File out = new File(getExternalFilesDir("AppInstaller"), UUID.randomUUID().toString() + ".apk");
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
            //读取文件流并写入目标文件中
            try (InputStream is = body.byteStream(); FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buffer = new byte[4096];
                long downloaded = 0;
                int read;
                while ((read = is.read(buffer)) != -1 && !Thread.currentThread().isInterrupted()) {
                    fos.write(buffer, 0, read);
                    downloaded += read;
                    final long d = downloaded;
                    if (updateProcess != null) {
                        //更新进度
                        updateProcess.accept(d);
                    }
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
                    if (id == R.id.check_version) {
                        binding.versionLayout.callOnClick();
                    }
                });
        addActionByXml(builder, this, R.xml.actions_about);
        //移除关于
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

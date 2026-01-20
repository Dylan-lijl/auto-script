package pub.carzy.auto_script.activities;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ViewAboutBinding;
import pub.carzy.auto_script.entity.CheckVersionResponse;
import pub.carzy.auto_script.model.AboutModel;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.ui.entity.PageMappingInflater;
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
        initTop();
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
                    showUpdateDialog();
                }
            });
        });
    }

    private void showUpdateDialog() {
        //todo
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

    private void initTop() {
        binding.topBarLayout.actionBar.setTitle(getActionBarTitle());
        QMUIAlphaImageButton manyBtn = binding.topBarLayout.actionBar.addRightImageButton(R.drawable.many_horizontal, QMUIViewHelper.generateViewId());
        manyBtn.setOnClickListener(e -> openBottomSheet());
    }

    private void openBottomSheet() {
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
        addDefaultMenu(builder);
        QMUIBottomSheet build = builder.build();
        build.show();
    }
}

package pub.carzy.auto_script.activities.about.child;

import static java.security.AccessController.getContext;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.popup.QMUIPopup;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutAcknowledgmentsBinding;
import pub.carzy.auto_script.entity.AcknowledgementEntity;
import pub.carzy.auto_script.entity.BasicFileImport;
import pub.carzy.auto_script.entity.DevelopmentProcessItem;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.model.AboutAcknowledgmentModel;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class AboutAcknowledgmentsActivity extends BaseActivity {
    private ComAboutAcknowledgmentsBinding binding;
    private AboutAcknowledgmentModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_acknowledgments);
        model = new AboutAcknowledgmentModel();
        binding.setModel(model);
        initTopBar();
        loadData();
    }
    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.acknowledgments);
    }

    private void loadData() {
        ThreadUtil.runOnCpu(() -> {
            WrapperEntity<AcknowledgementEntity> data = MixedUtil.loadFileData(
                    this,
                    R.raw.acknowledgements,
                    new TypeToken<>() {
                    }
            );
            if (data != null) {
                model.setData(data.getData());
                updateList();
            }
        });
    }

    private void updateList() {
        ThreadUtil.runOnUi(() -> {
            //移除旧数据
            binding.acknowledgmentList.removeAllViews();
            QMUIGroupListView.Section section = QMUIGroupListView.newSection(this);
            //从小新建
            for (AcknowledgementEntity item : model.getData()) {
                ImageView imageView = new ImageView(this);
                if (item.getType() == AcknowledgementEntity.PEOPLE) {
                    imageView.setImageDrawable(ActivityUtils.getDrawable(this, R.drawable.user, R.color.black));
                } else if (item.getType() == AcknowledgementEntity.LIBRARY) {
                    imageView.setImageDrawable(ActivityUtils.getDrawable(this, R.drawable.library, R.color.warning));
                } else if (item.getType() == AcknowledgementEntity.LINK) {
                    imageView.setImageDrawable(ActivityUtils.getDrawable(this, R.drawable.link, R.color.link));
                } else if (item.getType() == AcknowledgementEntity.ORGANIZATION) {
                    imageView.setImageDrawable(ActivityUtils.getDrawable(this, R.drawable.group, R.color.purple_200));
                } else {
                    continue;
                }
                QMUICommonListItemView view = binding.acknowledgmentList.createItemView(item.getTitle());
                view.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CUSTOM);
                view.addAccessoryCustomView(imageView);
                if (item.getContent() != null && !item.getContent().isEmpty()) {
                    view.setPadding(30, 30, 30, 30);
                    view.setOrientation(QMUICommonListItemView.VERTICAL);
                    view.setDetailText(item.getContent());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.topMargin = 20;
                    view.setLayoutParams(params);
                }
                section.addItemView(view, e -> clickView(item, e));
            }
            section.addTo(binding.acknowledgmentList);
        });
    }

    private void clickView(AcknowledgementEntity item, View e) {
        if (item.getHref() != null && !item.getHref().isEmpty()) {
            //打开浏览器
            if (item.getHref().size() == 1) {
                ActivityUtils.openToBrowser(this, item.getHref().get(0));
            } else {
                //列出弹窗
                AtomicReference<QMUIPopup> popup = new AtomicReference<>(null);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item, item.getHref());
                AdapterView.OnItemClickListener onItemClickListener = (adapterView, view, i, l) -> {
                    if (popup.get() != null) {
                        popup.get().dismiss();
                    }
                    ActivityUtils.openToBrowser(AboutAcknowledgmentsActivity.this, item.getHref().get(i));
                };
                popup.set(QMUIPopups.listPopup(this,
                                QMUIDisplayHelper.dp2px(this, 250),
                                QMUIDisplayHelper.dp2px(this, 300),
                                adapter,
                                onItemClickListener)
                        .animStyle(QMUIPopup.ANIM_GROW_FROM_CENTER)
                        .preferredDirection(QMUIPopup.DIRECTION_TOP)
                        .shadow(true)
                        .offsetYIfTop(QMUIDisplayHelper.dp2px(this, 5))
                        .skinManager(QMUISkinManager.defaultInstance(this))
                        .show(e));
            }
            return;
        }
        ActivityUtils.copyToClipboard(this, "text", item.getTitle());
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
    }
}

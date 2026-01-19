package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.qmuiteam.qmui.alpha.QMUIAlphaImageButton;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ViewAboutBinding;
import pub.carzy.auto_script.ui.entity.ActionInflater;
import pub.carzy.auto_script.ui.entity.PageMappingInflater;

/**
 * @author admin
 */
public class AboutActivity extends BaseActivity {
    private ViewAboutBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_about);
        initTop();
        initGroupListView();
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

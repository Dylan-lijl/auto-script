package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutDevelopmentProcessBinding;
import pub.carzy.auto_script.entity.DevelopmentProcessItem;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class AboutDevelopmentProcessActivity extends BaseActivity {
    private ComAboutDevelopmentProcessBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_development_process);
        loadData();
    }

    private void loadData() {
        ThreadUtil.runOnCpu(() -> {
            InputStream is = getResources().openRawResource(R.raw.development_process);
            try {
                try (InputStreamReader reader = new InputStreamReader(is)) {
                    Gson gson = new Gson();
                    WrapperEntity<DevelopmentProcessItem> wrapper = gson.fromJson(reader, new TypeToken<>() {
                    });
                    wrapper.getData().sort(Comparator.comparing(DevelopmentProcessItem::getOrder));
                    updateList(wrapper.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void updateList(List<DevelopmentProcessItem> data) {
        ThreadUtil.runOnUi(() -> {
            //移除旧数据
            binding.listView.removeAllViews();
            QMUIGroupListView.Section section = QMUIGroupListView.newSection(this);
            //从小新建
            for (DevelopmentProcessItem item : data) {
                QMUICommonListItemView view = binding.listView.createItemView(item.getTitle());
                section.addItemView(view, e -> clickView(item, e));
            }
            section.addTo(binding.listView);
        });
    }

    private void clickView(DevelopmentProcessItem item, View e) {
        if (item.getHref() != null) {
            ActivityUtils.openToBrowser(this, item.getHref());
        }
    }
}

package pub.carzy.auto_script.activities.about.child;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Objects;

import io.noties.markwon.Markwon;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ComAboutTroubleshootingBinding;
import pub.carzy.auto_script.entity.BasicFileImport;
import pub.carzy.auto_script.entity.FAQEntity;
import pub.carzy.auto_script.entity.TroubleshootingEntity;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.model.AboutTroubleshootingModel;
import pub.carzy.auto_script.ui_components.components.CollapseView;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.StringUtils;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class AboutTroubleshootingActivity extends BaseActivity {
    private ComAboutTroubleshootingBinding binding;
    private Markwon markwon;
    private AboutTroubleshootingModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_troubleshooting);
        markwon = BeanFactory.getInstance().get(Markwon.class);
        model = new AboutTroubleshootingModel();
        binding.setModel(model);
        initTopBar(binding.topBarLayout.actionBar);
        loadData();
    }

    private void loadData() {
        ThreadUtil.runOnCpu(() -> {
            InputStream is = getResources().openRawResource(R.raw.troubleshooting);
            try {
                try (InputStreamReader reader = new InputStreamReader(is)) {
                    Gson gson = new Gson();
                    WrapperEntity<TroubleshootingEntity> data = gson.fromJson(reader, new TypeToken<>() {
                    });
                    data.getData().sort(Comparator.comparing(BasicFileImport::getOrder));
                    model.setData(data.getData());
                    updateList();
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

    @SuppressWarnings("unchecked")
    private void updateList() {
        CollapseView<TroubleshootingEntity, TextView, LinearLayout, TextView> collapseView = binding.collapseView;
        collapseView.setTitleFactory(item -> {
            TextView textView = new TextView(this);
            textView.setText(item.getData().getQuestion());
            return textView;
        });
        collapseView.setRightFactory(item -> {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.HORIZONTAL);
            //添加跳转按钮
            if (!StringUtils.isEmpty(item.getData().getUrl())) {
                ImageButton network = new ImageButton(this);
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.network);
                if (drawable != null) {
                    drawable.setTint(ContextCompat.getColor(this, R.color.link));
                    network.setImageDrawable(drawable);
                }
                network.setOnClickListener(e -> {
                    ActivityUtils.openToBrowser(AboutTroubleshootingActivity.this, item.getData().getUrl());
                    //阻止事件向上传递
                    e.setClickable(true);
                });
                //设置图标为link颜色
                root.addView(network);
            }
            ImageView imageView = new ImageView(this);
            imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.arrow_left));
            root.addView(imageView);
            return root;
        });
        collapseView.setContentFactory(item -> {
            TextView textView = new TextView(this);
            markwon.setMarkdown(textView, item.getData().getAnswer());
            return textView;
        });
        //修改状态图标
        collapseView.setOnRenderListener((item) -> {
            LinearLayout root = item.getRightView();
            //获取图标view
            View lastChild = root.getChildAt(root.getChildCount() - 1);
            if (lastChild instanceof ImageView) {
                ImageView arrow = (ImageView) lastChild;
                arrow.setImageDrawable(ContextCompat.getDrawable(
                        this,
                        item.getData().isExpanded() ? R.drawable.arrow_down : R.drawable.arrow_left
                ));
            }
        });
    }
}

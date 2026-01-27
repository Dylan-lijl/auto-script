package pub.carzy.auto_script.activities.about.child;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;

import io.noties.markwon.Markwon;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.databinding.ComAboutFAQBinding;
import pub.carzy.auto_script.entity.AcknowledgementEntity;
import pub.carzy.auto_script.entity.BasicFileImport;
import pub.carzy.auto_script.entity.FAQEntity;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.model.AboutFAQModel;
import pub.carzy.auto_script.ui_components.components.CollapseView;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class AboutFAQActivity extends BaseActivity {
    private ComAboutFAQBinding binding;
    private AboutFAQModel model;
    private Markwon markwon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_f_a_q);
        markwon = BeanFactory.getInstance().get(Markwon.class);
        model = new AboutFAQModel();
        binding.setModel(model);
        initTopBar();
        loadData();
    }

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    private void loadData() {
        ThreadUtil.runOnCpu(() -> {
            WrapperEntity<FAQEntity> data = MixedUtil.loadFileData(
                    this,
                    R.raw.faq,
                    new TypeToken<>() {
                    }
            );
            if (data != null) {
                model.setData(data.getData());
                updateList();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void updateList() {
        CollapseView<FAQEntity, TextView, ImageView, TextView> collapseView = binding.collapseView;
        collapseView.setTitleFactory(item -> {
            TextView textView = new TextView(this);
            textView.setText(item.getData().getQuestion());
            return textView;
        });
        collapseView.setRightFactory(item -> {
            ImageView imageView = new ImageView(this);
            imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.arrow_left));
            return imageView;
        });
        collapseView.setContentFactory(item -> {
            TextView textView = new TextView(this);
            markwon.setMarkdown(textView, item.getData().getAnswer());
            return textView;
        });
        collapseView.setOnRenderListener((item) -> {
            ImageView view = item.getRightView();
            view.setImageDrawable(ContextCompat.getDrawable(this, item.getData().isExpanded() ? R.drawable.arrow_down : R.drawable.arrow_left));
        });
    }

}

package pub.carzy.auto_script.activities.about.child;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.viewpager.widget.PagerAdapter;

import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.tab.QMUITabBuilder;
import com.qmuiteam.qmui.widget.tab.QMUITabSegment;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ComAboutDevOtherBinding;
import pub.carzy.auto_script.databinding.ComAboutDevelopmentProcessBinding;
import pub.carzy.auto_script.databinding.ComAboutDialogueProcessBinding;
import pub.carzy.auto_script.databinding.ComAboutFuturePlansBinding;
import pub.carzy.auto_script.entity.DevelopmentProcessItem;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.utils.ActivityUtils;
import pub.carzy.auto_script.utils.MixedUtil;
import pub.carzy.auto_script.utils.ThreadUtil;

import com.google.gson.reflect.TypeToken;

/**
 * @author admin
 */
public class AboutDevelopmentProcessActivity extends BaseActivity {
    private ComAboutDevelopmentProcessBinding binding;
    private static final List<TabCallback<?>> TAB_LIST = new ArrayList<>();

    static {
        TAB_LIST.add(new DialogueProcessCallback());
        TAB_LIST.add(new FuturePlansCallback());
        TAB_LIST.add(new OtherCallback());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.com_about_development_process);
        initTopBar();
        initTab();
    }

    private void initTab() {
        QMUITabBuilder builder = binding.tabSegment.tabBuilder();
        for (TabCallback<?> tabCallback : TAB_LIST) {
            tabCallback.setContext(this);
            String tabName = tabCallback.getTabName();
            binding.tabSegment.addTab(builder.setText(tabName).build(this));
        }
        binding.tabSegment.setupWithViewPager(binding.contentViewPager, false);
        binding.tabSegment.setMode(QMUITabSegment.MODE_FIXED);
        PagerAdapter adapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return TAB_LIST.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull final ViewGroup container, int position) {
                View view = TAB_LIST.get(position).getView();
                if (view != null) {
                    container.addView(view);
                    return view;
                }
                return new TextView(AboutDevelopmentProcessActivity.this);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        };
        binding.contentViewPager.setAdapter(adapter);
        binding.contentViewPager.setCurrentItem(0);
        TAB_LIST.get(0).selected(true);
    }

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.development_process);
    }


    abstract static class TabCallback<T extends ViewDataBinding> {
        protected Context context;
        protected T binding;

        public void setContext(Context context) {
            this.context = context;
        }

        public abstract String getTabName();

        @LayoutRes
        public Integer getLayoutId() {
            return null;
        }

        protected void selected(boolean first) {

        }

        public void unselected() {

        }

        public View getView() {
            if (binding != null) {
                selected(false);
                return binding.getRoot();
            }
            if (getLayoutId() != null) {
                binding = DataBindingUtil.inflate(LayoutInflater.from(context), getLayoutId(), null, false);
                selected(true);
                return binding.getRoot();
            }
            return null;
        }
    }

    static class DialogueProcessCallback extends TabCallback<ComAboutDialogueProcessBinding> {
        @Override
        public String getTabName() {
            return context.getString(R.string.dialogue_process);
        }

        @Override
        public Integer getLayoutId() {
            return R.layout.com_about_dialogue_process;
        }

        @Override
        protected void selected(boolean first) {
            if (first) {
                loadData();
            }
        }

        private void loadData() {
            ThreadUtil.runOnCpu(() -> {
                WrapperEntity<DevelopmentProcessItem> data = MixedUtil.loadFileData(
                        context,
                        R.raw.development_process,
                        new TypeToken<>() {
                        }
                );
                if (data != null) {
                    updateList(data.getData());
                }
            });
        }

        private void updateList(List<DevelopmentProcessItem> data) {
            ThreadUtil.runOnUi(() -> {
                //移除旧数据
                binding.listView.removeAllViews();
                QMUIGroupListView.Section section = QMUIGroupListView.newSection(context);
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
                ActivityUtils.openToBrowser(context, item.getHref());
            }
        }
    }

    static class FuturePlansCallback extends TabCallback<ComAboutFuturePlansBinding> {

        @Override
        public String getTabName() {
            return context.getString(R.string.future_plans);
        }

        @Override
        public Integer getLayoutId() {
            return R.layout.com_about_future_plans;
        }
    }

    static class OtherCallback extends TabCallback<ComAboutDevOtherBinding> {

        @Override
        public String getTabName() {
            return context.getString(R.string.other);
        }

        @Override
        public Integer getLayoutId() {
            return R.layout.com_about_dev_other;
        }
    }
}

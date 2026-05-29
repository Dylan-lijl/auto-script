package pub.carzy.auto_script.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonField;
import com.openai.errors.OpenAIIoException;
import com.openai.models.models.Model;
import com.openai.models.models.ModelListPage;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.tab.QMUIBasicTabSegment;
import com.qmuiteam.qmui.widget.tab.QMUITabBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.ObjUtil;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.DataSearchModelBinding;
import pub.carzy.auto_script.databinding.ItemModelBinding;
import pub.carzy.auto_script.databinding.PageAiChatApiConfigBinding;
import pub.carzy.auto_script.databinding.ViewAiChatBinding;
import pub.carzy.auto_script.entity.AiChatApiModel;
import pub.carzy.auto_script.entity.AiChatConfig;
import pub.carzy.auto_script.entity.AiChatTabEntity;
import pub.carzy.auto_script.model.AiChatModel;
import pub.carzy.auto_script.ui.QMUIBottomSheetCustomBuilder;
import pub.carzy.auto_script.utils.ThreadUtil;

public class AiChatActivity extends BaseActivity {
    private ViewAiChatBinding binding;
    private PageAdapter pageAdapter;
    private AiChatModel model;
    private final List<AiChatTabEntity> tabs = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_ai_chat);
        model = new AiChatModel();
        model.setAiChatConfig(new AiChatConfig());
        //这里model还得进行初始化
        binding.setModel(model);
        initIntent();
        initTopBar();
        initListener();
    }

    private void initListener() {
        tabs.add(new AiChatTabEntity("AI配置", R.layout.page_ai_chat_api_config, this::initApiConfig));
        tabs.add(new AiChatTabEntity("应用配置", R.layout.page_ai_chat_app, this::initApp));
        tabs.add(new AiChatTabEntity("应用数据", R.layout.page_ai_chat_app_data, this::initAppData));
        tabs.add(new AiChatTabEntity("任务管理", R.layout.page_ai_chat_app_controller, this::initController));
        QMUITabBuilder builder = binding.tabSegment.tabBuilder();
        for (AiChatTabEntity tab : tabs) {
            binding.tabSegment.addTab(builder.setText(tab.getTitle()).build(this));
        }
        binding.tabSegment.notifyDataChanged();
        binding.tabSegment.addOnTabSelectedListener(new QMUIBasicTabSegment.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int index) {
                binding.contentViewPager.setCurrentItem(index);
            }

            @Override
            public void onTabUnselected(int index) {

            }

            @Override
            public void onTabReselected(int index) {
                binding.contentViewPager.setCurrentItem(index);
            }

            @Override
            public void onDoubleTap(int index) {
                binding.contentViewPager.setCurrentItem(index);
            }
        });
        pageAdapter = new PageAdapter(tabs);
        binding.contentViewPager.setAdapter(pageAdapter);
        binding.contentViewPager.setUserInputEnabled(false);
        binding.tabSegment.setupWithViewPager(binding.contentViewPager);
    }

    private void initApiConfig(ViewDataBinding viewDataBinding) {
        if (!(viewDataBinding instanceof PageAiChatApiConfigBinding)) {
            return;
        }
        PageAiChatApiConfigBinding layout = (PageAiChatApiConfigBinding) viewDataBinding;
        layout.setModel(model);
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder();
        //查询模型
        layout.searchModelBtn.setOnClickListener((e) -> {
            processSearchModel(e, builder);
        });
    }

    private void initApp(ViewDataBinding binding) {

    }

    private void initAppData(ViewDataBinding binding) {

    }

    private void initController(ViewDataBinding binding) {

    }

    private void initIntent() {

    }

    private void processSearchModel(View e, OpenAIOkHttpClient.Builder builder) {
        AiChatConfig config = model.getAiChatConfig();
        if (config == null || ObjUtil.isEmpty(config.getUrl())) {
            Toast.makeText(this, "请先填写路径", Toast.LENGTH_SHORT).show();
            return;
        }
        if (model.getSearchingModel()) {
            return;
        }
        model.setSearchingModel(true);
        builder.baseUrl(config.getUrl());
        if (ObjUtil.isEmpty(config.getKey())) {
            builder.apiKey("");
        } else {
            builder.apiKey(config.getKey());
        }
        OpenAIClient client = builder.build();
        DataSearchModelBinding dataSearchModelBinding = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.data_search_model,
                null,
                false
        );
        ModelAdapter modelAdapter = new ModelAdapter(model.getModels());
        @SuppressLint("NotifyDataSetChanged") Runnable callback = () -> {
            try {
                //获取模型列表
                ModelListPage list = client.models().list();
                boolean first = true;
                do {
                    if (first) {
                        first = false;
                        model.getModels().clear();
                    } else {
                        list = list.nextPage();
                    }
                    for (com.openai.models.models.Model line : list.data()) {
                        AiChatApiModel apiModel = new AiChatApiModel();
                        apiModel.setId(line.id());
                        apiModel.setCreated(getJsonFieldValue(line, "created"));
                        apiModel.setOwnedBy(getJsonFieldValue(line, "ownedBy"));
                        apiModel.setObject_(line._object_());
                        model.getModels().add(apiModel);
                    }
                } while (list.hasNextPage());
                ThreadUtil.runOnUi(() -> {
                    model.setSearchingModel(false);
                    //刷新数据
                    modelAdapter.notifyDataSetChanged();
                });
            } catch (OpenAIIoException exception) {
                ThreadUtil.runOnUi(() -> Toast.makeText(this, "获取模型列表失败,网络超时", Toast.LENGTH_SHORT).show());
            } catch (Exception exception) {
                ThreadUtil.runOnUi(() -> Toast.makeText(this, "获取模型列表失败,未知错误:" + exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        };
        //设置重新查询
        dataSearchModelBinding.searchModelBtn.setOnClickListener((e2) -> ThreadUtil.runOnCpu(callback));
        dataSearchModelBinding.modelListView.setAdapter(modelAdapter);
        dataSearchModelBinding.showDetailBtn.setOnClickListener(e3 -> modelAdapter.showDetail.set(!modelAdapter.showDetail.get()));
        dataSearchModelBinding.setShowDetail(modelAdapter.showDetail);
        QMUIBottomSheetCustomBuilder<?> customBuilder = new QMUIBottomSheetCustomBuilder<>(this);
        customBuilder.setContentView(dataSearchModelBinding.getRoot());
        customBuilder.build().show();
        //先进行查询
        ThreadUtil.runOnCpu(callback);
    }

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    private static <T> T getJsonFieldValue(Object obj, String fieldName) {
        try {

            Field field = obj.getClass().getDeclaredField(fieldName);

            field.setAccessible(true);

            JsonField<T> jsonField = (JsonField<T>) field.get(obj);

            if (jsonField == null || jsonField.isMissing() || jsonField.isNull()) return null;

            return jsonField.asKnown().orElse(null);

        } catch (Exception e) {

            return null;
        }
    }

    static class PageAdapter extends RecyclerView.Adapter<PageAdapter.VH> {

        private final List<AiChatTabEntity> tabs;

        public PageAdapter(List<AiChatTabEntity> tabs) {
            this.tabs = tabs;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {
            return new VH(DataBindingUtil.inflate(
                    LayoutInflater.from(parent.getContext()),
                    tabs.get(viewType).getResId(),
                    parent,
                    false
            ));
        }

        @Override
        public void onBindViewHolder(
                @NonNull VH holder,
                int position) {
            AiChatTabEntity entity = tabs.get(position);
            if (entity.getCallback() != null) {
                entity.getCallback().accept(holder.binding);
            }
        }

        @Override
        public int getItemCount() {
            return tabs.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        static class VH extends RecyclerView.ViewHolder {
            ViewDataBinding binding;

            public VH(@NonNull ViewDataBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    static class ModelAdapter
            extends RecyclerView.Adapter<ModelAdapter.VH> {

        private final List<AiChatApiModel> list;
        private final ObservableBoolean showDetail = new ObservableBoolean(false);

        public ModelAdapter(List<AiChatApiModel> list) {
            this.list = list;
        }

        public ObservableBoolean getShowDetail() {
            return showDetail;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            ItemModelBinding binding =
                    DataBindingUtil.inflate(
                            LayoutInflater.from(parent.getContext()),
                            R.layout.item_model,
                            parent,
                            false
                    );

            return new VH(binding);
        }

        @Override
        public void onBindViewHolder(
                @NonNull VH holder,
                int position) {

            holder.binding.setItem(list.get(position));
            holder.binding.setDetail(new ObservableBoolean(false));
            holder.binding.setShowDetail(showDetail);
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {

            ItemModelBinding binding;

            public VH(ItemModelBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}

package pub.carzy.auto_script.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.errors.OpenAIIoException;
import com.openai.models.conversations.Message;
import com.openai.models.models.Model;
import com.openai.models.models.ModelListPage;
import com.openai.models.models.ModelListPageAsync;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.StructuredResponseOutputItem;
import com.openai.services.async.ResponseServiceAsync;
import com.openai.services.blocking.ResponseService;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.tab.QMUIBasicTabSegment;
import com.qmuiteam.qmui.widget.tab.QMUITabBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjUtil;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.databinding.ComAiChatAppCheckItemBinding;
import pub.carzy.auto_script.databinding.ComAiChatAppConfigWechatBinding;
import pub.carzy.auto_script.databinding.DataSearchModelBinding;
import pub.carzy.auto_script.databinding.DialogAiTestResultBinding;
import pub.carzy.auto_script.databinding.ItemModelBinding;
import pub.carzy.auto_script.databinding.PageAiChatApiConfigBinding;
import pub.carzy.auto_script.databinding.PageAiChatAppBinding;
import pub.carzy.auto_script.databinding.PageAiChatAppControllerBinding;
import pub.carzy.auto_script.databinding.PageAiChatAppDataBinding;
import pub.carzy.auto_script.databinding.ViewAiChatBinding;
import pub.carzy.auto_script.entity.AiChatApiModel;
import pub.carzy.auto_script.entity.AiChatAppBaseConfig;
import pub.carzy.auto_script.entity.AiChatAppListModel;
import pub.carzy.auto_script.entity.AiChatConfig;
import pub.carzy.auto_script.entity.AiChatTabEntity;
import pub.carzy.auto_script.entity.ai.HelloEntity;
import pub.carzy.auto_script.ext.ai_abs.AiChatAppManager;
import pub.carzy.auto_script.ext.ai_abs.AiChatLifeCycle;
import pub.carzy.auto_script.model.AiChatAppDataModel;
import pub.carzy.auto_script.model.AiChatModel;
import pub.carzy.auto_script.ui.BottomCustomSheetBuilder;
import pub.carzy.auto_script.ui.QMUIBottomSheetCustomBuilder;
import pub.carzy.auto_script.utils.AiTaskUtil;
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
        //查询模型
        layout.searchModelBtn.setOnClickListener(this::processSearchModel);
        layout.testBtn.setOnClickListener(e -> {
            testModel();
        });
    }

    private AiTaskUtil.TaskResult<String> testTask;

    private void testModel() {
        AiChatConfig config = model.getAiChatConfig();
        if (config == null || ObjUtil.isEmpty(config.getModel())) {
            Toast.makeText(this, "请先填写模型", Toast.LENGTH_SHORT).show();
            return;
        }
        if (testTask != null && !testTask.getFuture().isDone()) {
            cancelTestTask();
            return;
        }
        model.setRunning(true);
        String question = "简单介绍一下你自己";
        testTask = requestResponse(config, question);
        testTask.getFuture()
                .thenAccept(answer -> ThreadUtil.runOnUi(() -> {
                    DialogAiTestResultBinding b = DialogAiTestResultBinding.inflate(LayoutInflater.from(this));
                    b.setQuestion(question);
                    b.setAnswer(answer);
                    new BottomCustomSheetBuilder(this)
                            .setTitle("测试结果")
                            .setContentView(b.getRoot())
                            .build()
                            .show();
                }))
                .exceptionally(throwable -> {
                    ThreadUtil.runOnUi(() -> Toast.makeText(this, "未知错误：" + throwable.getMessage(), Toast.LENGTH_LONG).show());
                    return null;
                }).whenComplete((r, t) -> ThreadUtil.runOnUi(() -> model.setRunning(false)));
    }

    /**
     * 改造后的内部网络阻塞请求
     */
    private AiTaskUtil.TaskResult<String> requestResponse(AiChatConfig config, String question) {
        return AiTaskUtil.wrapper(() -> createClient(config.getUrl(), config.getKey()), client -> {
            ResponseServiceAsync service = client.async().responses();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.convertValue(new JsonSchemaGenerator(mapper).generateJsonSchema(HelloEntity.class), new TypeReference<>() {
            });
            ResponseFormatTextJsonSchemaConfig.Schema.Builder schemaBuilder = ResponseFormatTextJsonSchemaConfig.Schema.builder();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                schemaBuilder.putAdditionalProperty(entry.getKey(), JsonValue.from(entry.getValue()));
            }
            ResponseFormatTextJsonSchemaConfig format = ResponseFormatTextJsonSchemaConfig.builder()
                    .name("hello_entity")
                    .strict(true)
                    .schema(schemaBuilder.build())
                    .build();
            ResponseTextConfig textConfig = ResponseTextConfig.builder()
                    .format(format)
                    .build();
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(config.getModel())
                    .input(question)
                    .maxOutputTokens(528)
                    .text(textConfig)
                    .build();
            return service.create(params)
                    .thenApply(response ->
                            response.output()
                                    .stream()
                                    .map(ResponseOutputItem::message)
                                    .filter(Objects::nonNull)
                                    .flatMap(Optional::stream)
                                    .flatMap(msg -> msg.content().stream())
                                    .map(content -> content.outputText().orElse(null))
                                    .filter(Objects::nonNull)
                                    .map(ResponseOutputText::text)
                                    .collect(Collectors.joining("\n"))
                    );
        });
    }

    /**
     * ⭐ 抽取专属的取消控制方法
     */
    private void cancelTestTask() {
        // 2. 重置 Loading 状态
        if (testTask != null && !testTask.getFuture().isDone()) {
            testTask.cancel(true);
            testTask = null;
        }
        model.setRunning(false);
    }

    List<AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding>> list;

    private synchronized List<AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding>> getList() {
        if (list == null) {
            Map<String, AiChatLifeCycle<?, ?>> all = AiChatAppManager.all();
            list = new ArrayList<>();
            all.forEach((k, v) -> list.add(AiChatAppListModel.create(v)));
        }
        return list;
    }

    CheckAppViewAdapter appViewAdapter;

    private synchronized CheckAppViewAdapter getAppViewAdapter() {
        if (appViewAdapter == null) {
            appViewAdapter = new CheckAppViewAdapter(getList(), this);
        }
        return appViewAdapter;
    }

    private void initApp(ViewDataBinding binding) {
        if (!(binding instanceof PageAiChatAppBinding)) {
            return;
        }
        PageAiChatAppBinding b = (PageAiChatAppBinding) binding;
        b.viewGroup.setAdapter(appViewAdapter);
        b.checkGroup.setAdapter(new CheckAppGroupAdapter(list, appViewAdapter::notifyItemChanged));
        b.saveBtn.setOnClickListener(e -> {
            list.forEach(item -> {
                item.getLifeCycle().saveConfigRaw(item.getConfig());
            });
        });
    }

    private void initAppData(ViewDataBinding binding) {
        if (!(binding instanceof PageAiChatAppDataBinding)) {
            return;
        }
        PageAiChatAppDataBinding b = (PageAiChatAppDataBinding) binding;
        AiChatAppDataModel selfModel = new AiChatAppDataModel();
        b.setModel(model);
        b.setSelfModel(selfModel);
        //暂时不处理 todo
    }

    private void initController(ViewDataBinding binding) {
        if (!(binding instanceof PageAiChatAppControllerBinding)) {
            return;
        }
        PageAiChatAppControllerBinding b = (PageAiChatAppControllerBinding) binding;
        b.setModel(model);
        b.checkGroup.setAdapter(getAppViewAdapter());

    }

    private void initIntent() {

    }

    public static final String SEARCH_MODEL_TASK_TAG = "SearchModel_Task";

    private void processSearchModel(View e) {
        AiChatConfig config = model.getAiChatConfig();
        if (config == null || ObjUtil.isEmpty(config.getUrl())) {
            Toast.makeText(this, "请先填写路径", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 静态创建并绑定
        OpenAIClient client = AiTaskUtil.createClient(SEARCH_MODEL_TASK_TAG, config.getUrl(), config.getKey());

        DataSearchModelBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(this), R.layout.data_search_model, null, false
        );
        QMUIBottomSheet sheet = new QMUIBottomSheetCustomBuilder<>(this)
                .setContentView(binding.getRoot())
                .build();

        ModelAdapter modelAdapter = new ModelAdapter(
                model.getModels(),
                item -> {
                    sheet.dismiss(); // 触发 dismiss 监听
                    model.getAiChatConfig().setModel(item.getId());
                }
        );
        binding.modelListView.setAdapter(modelAdapter);
        binding.showDetailBtn.setOnClickListener(v -> modelAdapter.showDetail.set(!modelAdapter.showDetail.get()));
        binding.setModel(model);
        binding.setShowDetail(modelAdapter.showDetail);

        binding.searchModelBtn.setOnClickListener(v -> {
            if (searchTask != null && !searchTask.isDone()) {
                cancelSearch();
            } else {
                startSearch(client, modelAdapter);
            }
        });
        // ⭐ 2. 弹窗消失，直接一枪干掉所有大模型网络阻塞
        sheet.setOnDismissListener(dialog -> cancelSearch());
        sheet.show();
        startSearch(client, modelAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private CompletableFuture<Void> searchTask;
    private CompletableFuture<ModelListPageAsync> searchModelTask;

    @SuppressLint("NotifyDataSetChanged")
    private void startSearch(OpenAIClient client, ModelAdapter adapter) {
        model.setSearchingModel(true);
        List<AiChatApiModel> result = Collections.synchronizedList(new ArrayList<>());
        searchModelTask = client.async().models().list();
        searchTask = searchModelTask.thenCompose(firstPage -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            AtomicReference<ModelListPageAsync> currentPage =
                    new AtomicReference<>(firstPage);
            while (currentPage.get().hasNextPage()) {
                chain = chain.thenCompose(v -> {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new CancellationException();
                    }
                    ModelListPageAsync page = currentPage.get();
                    for (Model line : page.data()) {
                        AiChatApiModel m = new AiChatApiModel();
                        m.setId(line.id());
                        m.setCreated(getValue(line::created, null));
                        m.setOwnedBy(getValue(line::ownedBy, null));
                        m.setObject_(getValue(line::_object_, null));
                        result.add(m);
                    }
                    if (!page.hasNextPage()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    CompletableFuture<ModelListPageAsync> nextFuture =
                            page.nextPage();
                    searchModelTask = nextFuture;
                    return nextFuture.thenAccept(currentPage::set);
                });
            }
            return chain.thenRun(() -> {
                ModelListPageAsync page = currentPage.get();
                for (Model line : page.data()) {
                    AiChatApiModel m = new AiChatApiModel();
                    m.setId(line.id());
                    m.setCreated(getValue(line::created, null));
                    m.setOwnedBy(getValue(line::ownedBy, null));
                    m.setObject_(getValue(line::_object_, null));
                    result.add(m);
                }
            });
        }).thenRun(() -> ThreadUtil.runOnUi(() -> {
            model.getModels().clear();
            model.getModels().addAll(result);
            adapter.notifyDataSetChanged();
            model.setSearchingModel(false);
        })).exceptionally(ex -> {
            if (ex instanceof CancellationException ||
                    ex.getCause() instanceof CancellationException) {
                Log.i("SearchModel", "任务已取消");
            } else {
                Log.e("SearchModel", ex.getMessage(), ex);
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                ThreadUtil.runOnUi(() -> {
                    String msg;
                    if (cause instanceof OpenAIIoException) {
                        msg = "网络超时";
                    } else {
                        msg = "错误：" + cause.getMessage();
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                });
            }
            return null;
        }).whenComplete((r, t) -> ThreadUtil.runOnUi(() -> model.setSearchingModel(false)));
    }

    /**
     * ⭐ 极简取消：不需要算术运算，直接物理超度
     */
    private void cancelSearch() {
        if (searchModelTask != null) {
            searchModelTask.cancel(true);
            searchModelTask = null;
        }
        if (searchTask != null) {
            searchTask.cancel(true);
            searchTask = null;
        }

        model.setSearchingModel(false);
    }

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    private static <T> T getValue(Supplier<T> back, T defaultValue) {
        try {
            return back.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public OpenAIClient createClient(String url, String key) {
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder();
        builder.baseUrl(url);
        if (ObjUtil.isEmpty(key)) {
            builder.apiKey("");
        } else {
            builder.apiKey(key);
        }
        return builder.build();
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
        private final Consumer<AiChatApiModel> clickHandler;

        public ModelAdapter(List<AiChatApiModel> list, Consumer<AiChatApiModel> clickHandler) {
            this.list = list;
            this.clickHandler = clickHandler;
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
            holder.binding.setPosition(position);
            holder.binding.getRoot().setOnClickListener(e -> clickHandler.accept(list.get(position)));
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

    static class CheckAppGroupAdapter extends RecyclerView.Adapter<CheckAppGroupAdapter.VH> {
        private Consumer<Integer> consumer;

        static class VH extends RecyclerView.ViewHolder {

            ComAiChatAppCheckItemBinding binding;

            VH(ComAiChatAppCheckItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        private final List<AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding>> data;

        public CheckAppGroupAdapter(List<AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding>> data, Consumer<Integer> consumer) {
            this.data = data;
            this.consumer = consumer;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent,
                                     int viewType) {
            ComAiChatAppCheckItemBinding b = ComAiChatAppCheckItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder,
                                     int position) {
            AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding> model = data.get(position);
            holder.binding.setChecked(model.getEnable());
            holder.binding.setItem(model);
            holder.binding.root.setOnClickListener(v -> {
                model.setEnable(!model.getEnable());
                notifyItemChanged(position);
                consumer.accept(position);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    static class CheckAppViewAdapter extends RecyclerView.Adapter<CheckAppViewAdapter.VH> {

        private final List<AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding>> data;
        private final Activity activity;

        public CheckAppViewAdapter(List<AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding>> data, Activity activity) {
            this.data = data;
            this.activity = activity;
        }

        static class VH extends RecyclerView.ViewHolder {

            FrameLayout container;

            public VH(@NonNull FrameLayout itemView) {
                super(itemView);
                this.container = itemView;
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent,
                                     int viewType) {

            FrameLayout root = new FrameLayout(parent.getContext());
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            return new VH(root);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder,
                                     int position) {

            AiChatAppListModel<? extends AiChatAppBaseConfig, ? extends ViewDataBinding> model = data.get(position);
            holder.container.removeAllViews();
            if (model.getEnable() == null || !model.getEnable()) return;
            holder.container.addView(createBinding(model, activity).getRoot());
        }

        private static <C extends AiChatAppBaseConfig, V extends ViewDataBinding> V createBinding(AiChatAppListModel<C, V> model, Activity activity) {

            return model.getCreateView().apply(model, activity);
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }
    }
}

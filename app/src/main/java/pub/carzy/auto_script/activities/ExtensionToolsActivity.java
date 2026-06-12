package pub.carzy.auto_script.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import java.util.List;
import pub.carzy.auto_script.activities.ext.ExtManager;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.common_library.entity.ExtToolEntity;
import pub.carzy.auto_script.databinding.ItemExtToolBinding;
import pub.carzy.auto_script.databinding.ViewExtensionToolsBinding;

public class ExtensionToolsActivity extends BaseActivity {
    private ViewExtensionToolsBinding binding;

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_extension_tools);
        initTopBar();
        initData();
    }

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.extension_tools);
    }

    public void initData() {
        List<ExtToolEntity> tools = ExtManager.getTools(this);
        binding.toolList.setAdapter(new ExtToolBindingAdapter(tools));
        //设置布局管理器
        GridLayoutManager manager = new GridLayoutManager(this, 4);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });
        binding.toolList.setLayoutManager(manager);
    }

    static class ExtToolBindingAdapter extends RecyclerView.Adapter<ExtToolBindingAdapter.VH> {

        private final List<ExtToolEntity> data;

        public ExtToolBindingAdapter(List<ExtToolEntity> data) {
            this.data = data;
        }

        static class VH extends RecyclerView.ViewHolder {
            ItemExtToolBinding binding;

            public VH(@NonNull ItemExtToolBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemExtToolBinding binding = ItemExtToolBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ExtToolEntity item = data.get(position);
            holder.binding.setData(item);
            holder.binding.executePendingBindings();
            // 点击事件
            holder.binding.getRoot().setOnClickListener(v -> {
                if (item.isEnable()&& item.getClazz() != null) {
                    v.getContext().startActivity(new Intent(v.getContext(), item.getClazz()));
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}

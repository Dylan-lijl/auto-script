package pub.carzy.auto_script.activities.ext.children;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ViewCommandTerminalBinding;
import pub.carzy.auto_script.entity.CommandTerminalModel;
import pub.carzy.auto_script.utils.ThreadUtil;

public class CommandTerminalActivity extends BaseActivity {
    private ViewCommandTerminalBinding binding;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader stdout;
    private BufferedReader stderr;
    private CommandTerminalModel model;
    private CommandLogAdapter adapter;

    @Override
    protected QMUITopBarLayout getTopBar() {
        return binding.topBarLayout.actionBar;
    }

    @Override
    protected String getActionBarTitle() {
        return "命令行工具";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_command_terminal);
        initTopBar();
        model = new CommandTerminalModel();
        binding.setModel(model);
        binding.btnSend.setOnClickListener(e -> {
            if (model.getCommand() == null || model.getCommand().isEmpty() || model.isInit()) {
                return;
            }
            //启动监听程序
            if (process == null) {
                model.setInit(true);
                startShell();
            }
            //这里
        });
        binding.rvOutput.setAdapter(adapter = new CommandLogAdapter());
    }

    private void startShell() {
        ThreadUtil.runOnCpu(() -> {
            try {
                process = Runtime.getRuntime().exec("sh");
                stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            } catch (IOException e) {
                Log.e("startShell", "打开shell失败", e);
                ThreadUtil.runOnUi(() -> Toast.makeText(this, "打开shell失败:" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show());
            }
            // stdout线程
            new Thread(() -> readStream(stdout)).start();
            // stderr线程
            new Thread(() -> readStream(stderr)).start();
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    ThreadUtil.runOnUi(() -> {
                        Toast.makeText(this, "shell退出:" + exitCode, Toast.LENGTH_LONG).show();
                    });
                } catch (InterruptedException e) {
                    Log.e("startShell", "等待shell结束异常", e);
                }
            }).start();
            ThreadUtil.runOnUi(() -> model.setInit(false));
        });
    }

    private void readStream(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String out = line;
                ThreadUtil.runOnUi(() -> {
                    adapter.addLine(out);
                    binding.rvOutput.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
        } catch (Exception e) {
            Log.e("shell", "读取流失败", e);
        }
    }

    public static class CommandLogEntity {
        private final String text;

        public CommandLogEntity(String text) {
            this.text = text;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeShell();
    }

    private void closeShell() {
        try {
            if (writer != null) {
                writer.write("exit\n");
                writer.flush();
            }
            if (stdout != null) {
                stdout.close();
            }
            if (stderr != null) {
                stderr.close();
            }
            if (process != null) process.destroy();
        } catch (IOException ignored) {
        } finally {
            writer = null;
            stdout = null;
            stderr = null;
            process = null;
        }
    }

    public static class CommandLogAdapter extends RecyclerView.Adapter<CommandLogAdapter.VH> {

        private final List<CommandLogEntity> data = new ArrayList<>();

        public void addLine(String line) {
            data.add(new CommandLogEntity(line));
            notifyItemInserted(data.size() - 1);
        }

        public void clear() {
            data.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            tv.setTextSize(12);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(Typeface.MONOSPACE);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tv.setText(data.get(position).text);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;

            public VH(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }
    }
}

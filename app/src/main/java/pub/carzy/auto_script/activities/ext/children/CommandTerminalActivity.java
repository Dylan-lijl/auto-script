package pub.carzy.auto_script.activities.ext.children;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pub.carzy.auto_script.R;
import pub.carzy.auto_script.activities.BaseActivity;
import pub.carzy.auto_script.databinding.ItemCommandLineBinding;
import pub.carzy.auto_script.databinding.ViewCommandTerminalBinding;
import pub.carzy.auto_script.entity.CommandTerminalModel;
import pub.carzy.auto_script.utils.ActivityUtils;
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
        return getString(R.string.command_tools_title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.view_command_terminal);
        initTopBar();
        model = new CommandTerminalModel();
        binding.setModel(model);
        binding.btnSend.setOnClickListener(e -> {
            if (model.isRunning()) {
                stopShell();
            } else {
                String command = model.getCommand();
                if (command == null || model.getCommand().isEmpty() || model.isInit()) {
                    return;
                }
                if (process == null) {
                    startShell(() -> {
                        //先运行获取pid
                        getPid = true;
                        writeCommand("echo pid:$$", false);
                        ThreadUtil.runOnCpu(() -> writeCommand(command));
                    });
                } else {
                    ThreadUtil.runOnCpu(() -> writeCommand(command));
                }
            }
        });
        binding.rvOutput.setAdapter(adapter = new CommandLogAdapter(model));
        binding.clearBtn.setOnClickListener((e) -> {
            adapter.clear();
        });
        binding.copyBtn.setOnClickListener(e -> copyLog());
        binding.exportBtn.setOnClickListener(e -> exportLog());
        binding.historyBtn.setOnClickListener(e -> {
            showHistoryCommands();
        });
        binding.killBtn.setOnClickListener(e -> {
            killShell();
        });
        model.setInit(true);
        startShell(() -> {
            //先运行获取pid
            getPid = true;
            writeCommand("echo pid:$$", false);
        });
    }

    private void showHistoryCommands() {
        new QMUIDialog.CheckableDialogBuilder(this)
                .setMaxPercent(QMUIDisplayHelper.dp2px(this, 500) * 1f / QMUIDisplayHelper.getScreenHeight(this))
                .addItems(model.getCommands().toArray(new String[0]), (dialog, which) -> {
                    dialog.dismiss();
                    model.setCommand(model.getCommands().get(which));
                })
                .create()
                .show();
    }

    private void killShell() {
        new QMUIDialog.MessageDialogBuilder(this)
                .setTitle(R.string.kill_title)
                .setMessage(R.string.kill_detail)
                .addAction(R.string.cancel, (dialog, index) -> {
                    dialog.dismiss();
                })
                .addAction(R.string.confirm, (dialog, index) -> {
                    dialog.dismiss();
                    if (model.isRunning()) {
                        stopShell(this::closeShell);
                    } else {
                        closeShell();
                    }
                })
                .create().show();
    }

    private void stopShell() {
        stopShell(null);
    }

    private void stopShell(Runnable runnable) {
        model.setStopping(true);
        ThreadUtil.runOnCpu(() -> {
            Process killerExec = null;
            BufferedReader reader = null;
            try {
                int parentPid = model.getPid();
                if (parentPid > 0) {
                    killerExec = Runtime.getRuntime().exec("sh");
                    OutputStream stream = killerExec.getOutputStream();
                    String end = createCmdEnd();
                    String findChildCmd = "ps -A -o PID,PPID | grep " + parentPid + "\n";
                    stream.write(findChildCmd.getBytes());
                    stream.write("exit\n".getBytes());
                    stream.flush();
                    stream.close();
                    reader = new BufferedReader(new InputStreamReader(killerExec.getInputStream()));
                    String line;
                    int targetChildPid = -1;
                    while ((line = reader.readLine()) != null) {
                        if (line.equals(end)) {
                            break;
                        }
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            try {
                                int curPid = Integer.parseInt(parts[0]);
                                int curPpid = Integer.parseInt(parts[1]);
                                if (curPpid == parentPid && curPid != parentPid) {
                                    targetChildPid = curPid;
                                    break;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    killerExec.waitFor();
                    if (targetChildPid > 0) {
                        Process killAction = Runtime.getRuntime().exec("sh");
                        OutputStream actionStream = killAction.getOutputStream();
                        actionStream.write(("kill -2 " + targetChildPid + "\n").getBytes());
                        actionStream.write("exit\n".getBytes());
                        actionStream.flush();
                        actionStream.close();
                        killAction.waitFor();
                        killAction.destroy();
                    } else {
                        Log.w("CommandTerminal", "未找到前台阻塞的子进程");
                    }
                }
            } catch (Exception ex) {
                Log.e("CommandTerminal", "精准查找并结束子进程失败", ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
                if (killerExec != null) {
                    killerExec.destroy();
                }
                ThreadUtil.runOnUi(() -> model.setStopping(false));
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }

    private void exportLog() {
        if (adapter == null || adapter.getItemCount() == 0) {
            return;
        }
        if (model.isExporting()) {
            return;
        }
        model.setExporting(true);
        ThreadUtil.runOnCpu(() -> {
            String fileName = "auto_script_terminal_" + System.currentTimeMillis() + ".log";
            File file = new File(ActivityUtils.getDownloadDir(this), fileName);
            if (file.exists()) {
                if (!file.delete()) {
                    ThreadUtil.runOnUi(() -> {
                        Toast.makeText(this, "删除旧文件失败", Toast.LENGTH_SHORT).show();
                        model.setExporting(false);
                    });
                    return;
                }
                try {
                    boolean newFile = file.createNewFile();
                } catch (IOException e) {
                    Log.e("exportLog", "删除文件失败!", e);
                    ThreadUtil.runOnUi(() -> {
                        Toast.makeText(this, "创建新文件失败:" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        model.setExporting(false);
                    });
                    return;
                }
            }
            boolean success = false;
            try (FileOutputStream fos = new FileOutputStream(file)) {
                for (CommandLogEntity entity : adapter.getData()) {
                    fos.write(entity.getText().getBytes());
                    fos.write("\n".getBytes());
                }
                success = true;
                fos.flush();
            } catch (Exception e) {
                Log.e("exportLog", "导出失败", e);
            } finally {
                boolean finalSuccess = success;
                ThreadUtil.runOnUi(() -> {
                    Toast.makeText(this, "导出" + (finalSuccess ? "成功" : "失败") + ": " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    model.setExporting(false);
                });
            }
        });
    }

    private void copyLog() {
        if (adapter == null || adapter.getItemCount() == 0) {
            Toast.makeText(this, "没有日志可复制", Toast.LENGTH_SHORT).show();
            return;
        }
        // 拼接所有日志内容
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            sb.append(adapter.getData().get(i).getText()).append("\n");
        }
        // 复制到剪贴板
        ActivityUtils.copyToClipboard(this, "text", sb.toString());
        Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private String cmdEnd = "";
    private boolean getPid = false;

    protected String createCmdEnd() {
        return "__CMD__END_" + UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }

    private void writeCommand(String command) {
        writeCommand(command, true);
    }

    private void writeCommand(String command, boolean add) {
        if (command == null || command.isEmpty() || model.isRunning()) {
            return;
        }
        //降低冲突概率
        cmdEnd = createCmdEnd();
        try {
            writer.write(command);
            writer.newLine();
            writer.write("echo " + cmdEnd);
            writer.newLine();
            writer.flush();
            ThreadUtil.runOnUi(() -> {
                if (add) {
                    adapter.addLine(CommandLogEntity.COMMAND, command);
                    model.getCommands().add(command);
                }
                model.setRunning(true);
            });
        } catch (IOException ex) {
            Log.e("", "写命令失败", ex);
        }
    }

    private void startShell(Runnable runnable) {
        ThreadUtil.runOnCpu(() -> {
            try {
                process = Runtime.getRuntime().exec("sh");
                stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            } catch (IOException e) {
                Log.e("startShell", "打开shell失败", e);
                ThreadUtil.runOnUi(() -> Toast.makeText(this, "打开shell失败:" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show());
            }
            // stdout线程
            new Thread(() -> readStream(stdout), "process stdout").start();
            // stderr线程
            new Thread(() -> readStream(stderr), "process stderr").start();
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    ThreadUtil.runOnUi(() -> {
                        Toast.makeText(this, "shell退出:" + exitCode, Toast.LENGTH_LONG).show();
                    });
                } catch (InterruptedException e) {
                    Log.e("startShell", "等待shell结束异常", e);
                } finally {
                    closeShell();
                }
            }, "process waitFor").start();
            if (runnable != null) {
                runnable.run();
            }
            ThreadUtil.runOnUi(() -> model.setInit(false));
        });
    }

    private void readStream(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String out = line;
                ThreadUtil.runOnUi(() -> {
                    if (cmdEnd.equals(out)) {
                        model.setRunning(false);
                        model.setCommand("");
                        return;
                    }
                    if (getPid && out.startsWith("pid:")) {
                        getPid = false;
                        model.setPid(Integer.parseInt(out.trim().replace("pid:", "")));
                        return;
                    }
                    adapter.addLine(CommandLogEntity.RESULT, out);
                    binding.rvOutput.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
        } catch (Exception e) {
            Log.e("shell", "读取流失败", e);
        }
    }

    public static class CommandLogEntity extends BaseObservable {
        public static final int COMMAND = 0;
        public static final int RESULT = 1;
        private final Integer role;
        private final String text;

        public CommandLogEntity(Integer role, String text) {
            this.role = role;
            this.text = text;
        }

        @Bindable
        public Integer getRole() {
            return role;
        }

        @Bindable
        public String getText() {
            return text;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeShell();
    }

    private void closeShell() {
        try {
            if (process != null) process.destroy();
            if (writer != null) {
                writer.write("exit\n");
                writer.flush();
                writer.close();
            }
            if (stdout != null) {
                stdout.close();
            }
            if (stderr != null) {
                stderr.close();
            }
        } catch (IOException e) {
            Log.e("closeShell", "关闭流失败", e);
        } finally {
            writer = null;
            stdout = null;
            stderr = null;
            process = null;
            model.setPid(-1);
            model.setRunning(false);
            model.setInit(false);
        }
    }

    public static class CommandLogAdapter extends RecyclerView.Adapter<CommandLogAdapter.VH> {

        private final List<CommandLogEntity> data = new ArrayList<>();
        private final CommandTerminalModel model;

        public List<CommandLogEntity> getData() {
            return data;
        }

        public CommandLogAdapter(CommandTerminalModel model) {
            this.model = model;
        }

        public synchronized void addLine(Integer role, String line) {
            data.add(new CommandLogEntity(role, line));
            notifyItemInserted(data.size() - 1);
            model.setLogLength(data.size());
            model.setCommandLength(model.getCommands().size());
        }

        public void clear() {
            data.clear();
            notifyDataSetChanged();
            model.setLogLength(data.size());
            model.setCommandLength(0);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCommandLineBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                    R.layout.item_command_line, parent, false);
            return new VH(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.binding.setItem(data.get(position));
            holder.binding.setPosition(position + 1);
            holder.binding.setModel(model);
            holder.binding.executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            ItemCommandLineBinding binding;

            public VH(@NonNull ItemCommandLineBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}

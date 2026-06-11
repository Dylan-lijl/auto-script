package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.BR;

public class CommandTerminalModel extends BaseObservable {
    private String command;
    private boolean running = false;
    private boolean init = false;
    private boolean showLineNumber = false;
    private boolean exporting = false;
    private boolean stopping = false;
    private Integer pid = -1;
    private final List<String> commands = new ArrayList<>();
    private Integer logLength = 0;
    private Integer commandLength = 0;

    @Bindable
    public Integer getCommandLength() {
        return commandLength;
    }

    public void setCommandLength(Integer commandLength) {
        this.commandLength = commandLength;
        notifyPropertyChanged(BR.commandLength);
    }

    @Bindable
    public boolean isStopping() {
        return stopping;
    }

    @Bindable
    public Integer getLogLength() {
        return logLength;
    }

    public void setLogLength(Integer logLength) {
        this.logLength = logLength;
        notifyPropertyChanged(BR.logLength);
    }

    public void setStopping(boolean stopping) {
        this.stopping = stopping;
        notifyPropertyChanged(BR.stopping);
    }

    @Bindable
    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
        notifyPropertyChanged(BR.pid);
    }

    public List<String> getCommands() {
        return commands;
    }

    @Bindable
    public boolean isExporting() {
        return exporting;
    }

    public void setExporting(boolean exporting) {
        this.exporting = exporting;
        notifyPropertyChanged(BR.exporting);
    }

    @Bindable
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
        notifyPropertyChanged(BR.command);
    }

    @Bindable
    public boolean isShowLineNumber() {
        return showLineNumber;
    }

    public void setShowLineNumber(boolean showLineNumber) {
        this.showLineNumber = showLineNumber;
        notifyPropertyChanged(BR.showLineNumber);
    }

    @Bindable
    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        notifyPropertyChanged(BR.running);
    }

    @Bindable
    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
        notifyPropertyChanged(BR.init);
    }
}

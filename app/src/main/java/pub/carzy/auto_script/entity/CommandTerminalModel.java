package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

public class CommandTerminalModel extends BaseObservable {
    private String command;
    private boolean running = false;
    private boolean init = false;
    @Bindable
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
        notifyPropertyChanged(BR.command);
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

package pub.carzy.auto_script.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.util.function.Consumer;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class AboutDevTestModel extends BaseObservable {
    private Integer count = 32;
    private boolean dragging = false;
    private Consumer<Integer> consumer;
    private int process=0;
    private int max=1;

    @Bindable
    public int getProcess() {
        return process;
    }

    public void setProcess(int process) {
        this.process = process;
        notifyPropertyChanged(BR.process);
    }

    @Bindable
    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        notifyPropertyChanged(BR.max);
    }

    public void setListener(Consumer<Integer> consumer) {
        this.consumer = consumer;
    }

    @Bindable
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
        if (consumer != null) {
            consumer.accept(count);
        }
        notifyPropertyChanged(BR.count);
        notifyPropertyChanged(BR.countString);
    }

    @Bindable
    public String getCountString() {
        return count + "";
    }

    public void setCountString(String countString) {
        if ("".equals(countString)) {
            setCount(0);
        } else {
            setCount(Integer.parseInt(countString));
        }
    }

    @Bindable
    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
        notifyPropertyChanged(BR.dragging);
    }

    public void reset() {
        setCount(32);
        setDragging(false);
    }
}

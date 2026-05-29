package pub.carzy.auto_script.entity;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ViewDataBinding;

import java.util.function.Consumer;

import pub.carzy.auto_script.BR;

public class AiChatTabEntity extends BaseObservable {
    public AiChatTabEntity() {
    }

    public AiChatTabEntity(String title, @LayoutRes int resId, Consumer<ViewDataBinding> callback) {
        this.title = title;
        this.resId = resId;
        this.callback = callback;
    }

    private String title;
    @LayoutRes
    private int resId;

    private Consumer<ViewDataBinding> callback;

    @Bindable
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyPropertyChanged(BR.title);
    }

    @Bindable
    public int getResId() {
        return resId;
    }

    public void setResId(@LayoutRes int resId) {
        this.resId = resId;
        notifyPropertyChanged(BR.resId);
    }

    @Bindable
    public Consumer<ViewDataBinding> getCallback() {
        return callback;
    }

    public void setCallback(Consumer<ViewDataBinding> callback) {
        this.callback = callback;
        notifyPropertyChanged(BR.callback);
    }
}

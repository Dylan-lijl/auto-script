package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import pub.carzy.auto_script.BR;

/**
 * @author admin
 */
public class Style extends BaseObservable {
    private long id;
    private long currentVersion;
    /**
     * 样式名称
     */
    private String name;
    /**
     * 状态栏背景颜色
     */
    private int statusBarBackgroundColor;
    /**
     * 状态栏字体模式 true为浅色false为深色
     */
    private boolean statusBarMode;
    /**
     * 标题栏背景颜色
     */
    private int topBarBackgroundColor;
    /**
     * 标题栏字体颜色
     */
    private int topBarTextColor;
    /**
     * 标题栏图片颜色
     */
    private int topBarImageColor;

    @Bindable
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
        notifyPropertyChanged(BR.id);
    }

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
    }

    @Bindable
    public long getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(long currentVersion) {
        this.currentVersion = currentVersion;
        notifyPropertyChanged(BR.currentVersion);
    }

    @Bindable
    public int getStatusBarBackgroundColor() {
        return statusBarBackgroundColor;
    }

    public void setStatusBarBackgroundColor(int statusBarBackgroundColor) {
        this.statusBarBackgroundColor = statusBarBackgroundColor;
        notifyPropertyChanged(BR.statusBarBackgroundColor);
    }

    @Bindable
    public boolean isStatusBarMode() {
        return statusBarMode;
    }

    public void setStatusBarMode(boolean statusBarMode) {
        this.statusBarMode = statusBarMode;
        notifyPropertyChanged(BR.statusBarMode);
    }

    @Bindable
    public int getTopBarBackgroundColor() {
        return topBarBackgroundColor;
    }

    public void setTopBarBackgroundColor(int topBarBackgroundColor) {
        this.topBarBackgroundColor = topBarBackgroundColor;
        notifyPropertyChanged(BR.topBarBackgroundColor);
    }

    @Bindable
    public int getTopBarTextColor() {
        return topBarTextColor;
    }

    public void setTopBarTextColor(int topBarTextColor) {
        this.topBarTextColor = topBarTextColor;
        notifyPropertyChanged(BR.topBarTextColor);
    }

    @Bindable
    public int getTopBarImageColor() {
        return topBarImageColor;
    }

    public void setTopBarImageColor(int topBarImageColor) {
        this.topBarImageColor = topBarImageColor;
        notifyPropertyChanged(BR.topBarImageColor);
    }
}

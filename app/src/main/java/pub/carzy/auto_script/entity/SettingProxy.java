package pub.carzy.auto_script.entity;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.List;

import pub.carzy.auto_script.BR;
import pub.carzy.auto_script.R;
import pub.carzy.auto_script.Startup;

/**
 * 录制设置和播放设置等写完root模式之后再决定怎么配置
 *
 * @author admin
 */
public class SettingProxy extends BaseObservable implements Cloneable {
    public static final SettingProxy DEFAULT;
    public static final int AUTO = 0;
    public static final int ACCESSIBILITY = 1;
    public static final int ROOT = 2;


    static {
        DEFAULT = new SettingProxy();
        DEFAULT.setType(AUTO);
        DEFAULT.setTick(10);
        DEFAULT.setAutoClose(true);
        DEFAULT.setShowOperation(true);
        DEFAULT.setOperationConfig(new OperationConfig(50, Color.parseColor("#1677ff"), Color.parseColor("#ffffff"), 3));
        DEFAULT.setAutoPlay(true);
        DEFAULT.setIgnoreFloatingScript(true);
        DEFAULT.setFloatPoint(new FloatPoint(200, 200));
        DEFAULT.setDynamicUpdate(true);
        DEFAULT.setMaskConfig(new MaskConfig(Color.parseColor("#6b6b6b1a"), 10, false, 2, Color.BLACK, true, 16, Color.BLACK));
    }

    /**
     * 类型
     */
    private Integer type = AUTO;
    /**
     * 时间片
     */
    private Integer tick = 10;
    /**
     * 自动关闭
     */
    private Boolean autoClose = true;
    /**
     * 显示点按
     */
    private Boolean showOperation = true;
    /**
     * 点按操作配置
     */
    private OperationConfig operationConfig;
    /**
     * 自动回放
     */
    private Boolean autoPlay = true;
    /**
     * 忽略悬浮窗脚本
     */
    private Boolean ignoreFloatingScript = true;
    /**
     * 悬浮窗位置
     */
    private FloatPoint floatPoint;
    /**
     * 动态更新
     */
    private Boolean dynamicUpdate = true;
    /**
     * 蒙层设置
     */
    private MaskConfig maskConfig;
    /**
     * 样式
     */
    private ObservableList<Style> styles = new ObservableArrayList<>();


    @Bindable
    public Boolean getAutoPlay() {
        return autoPlay;
    }

    @Bindable
    public Integer getTick() {
        return tick;
    }

    public void setTick(Integer tick) {
        this.tick = tick;
        notifyPropertyChanged(BR.tick);
    }

    public void setAutoPlay(Boolean autoPlay) {
        this.autoPlay = autoPlay;
        notifyPropertyChanged(BR.autoReplay);
    }

    public SettingProxy() {
        styles.addOnListChangedCallback(new ObservableList.OnListChangedCallback<>() {
            @Override
            public void onChanged(ObservableList<Style> sender) {
                updateCurrentStyle();
            }

            @Override
            public void onItemRangeChanged(ObservableList<Style> sender, int positionStart, int itemCount) {
                updateCurrentStyle();
            }

            @Override
            public void onItemRangeInserted(ObservableList<Style> sender, int positionStart, int itemCount) {
                updateCurrentStyle();
            }

            @Override
            public void onItemRangeMoved(ObservableList<Style> sender, int fromPosition, int toPosition, int itemCount) {
                updateCurrentStyle();
            }

            @Override
            public void onItemRangeRemoved(ObservableList<Style> sender, int positionStart, int itemCount) {
                updateCurrentStyle();
            }
        });
    }

    @Bindable
    public ObservableList<Style> getStyles() {
        return styles;
    }

    public void updateCurrentStyle() {
        notifyPropertyChanged(BR.currentStyle);
    }

    public void setStyles(List<Style> styles) {
        this.styles.clear();
        this.styles.addAll(styles);
        notifyPropertyChanged(BR.styles);
        notifyPropertyChanged(BR.currentStyle);
    }

    /**
     * 先更新索引后更新style列表就不会出问题
     *
     * @return 当前样式
     */
    @Bindable
    public Style getCurrentStyle() {
        if (!styles.isEmpty()) {
            //获取版本最大的那个
            Style s = null;
            long version = -1;
            for (Style style : styles) {
                if (style.getCurrentVersion() > version) {
                    version = style.getCurrentVersion();
                    s = style;
                }
            }
            if (version > -1) {
                return s;
            }
        }
        return null;
    }

    @Bindable
    public FloatPoint getFloatPoint() {
        return floatPoint;
    }

    public void setFloatPoint(FloatPoint floatPoint) {
        this.floatPoint = floatPoint;
        notifyPropertyChanged(BR.floatPoint);
    }

    @Bindable
    public Integer getType() {
        return type;
    }

    @Bindable
    public String getTypeString() {
        switch (type) {
            case ROOT:
                return Startup.CURRENT.getString(R.string.root_mode);
            case ACCESSIBILITY:
                return Startup.CURRENT.getString(R.string.accessibility_mode);
            case AUTO:
                return Startup.CURRENT.getString(R.string.auto);
            default:
                return Startup.CURRENT.getString(R.string.unknown);
        }
    }

    public void setType(Integer type) {
        this.type = type;
        notifyPropertyChanged(BR.type);
        notifyPropertyChanged(BR.typeString);
    }

    @Bindable
    public Boolean getAutoClose() {
        return autoClose;
    }

    public void setAutoClose(Boolean autoClose) {
        this.autoClose = autoClose;
        notifyPropertyChanged(BR.autoClose);
    }

    @Bindable
    public Boolean getShowOperation() {
        return showOperation;
    }

    public void setShowOperation(Boolean showOperation) {
        this.showOperation = showOperation;
        notifyPropertyChanged(BR.showOperation);
    }

    @Bindable
    public OperationConfig getOperationConfig() {
        return operationConfig;
    }

    public void setOperationConfig(OperationConfig operationConfig) {
        this.operationConfig = operationConfig;
        notifyPropertyChanged(BR.operationConfig);
    }

    @Bindable
    public Boolean getIgnoreFloatingScript() {
        return ignoreFloatingScript;
    }

    public void setIgnoreFloatingScript(Boolean ignoreFloatingScript) {
        this.ignoreFloatingScript = ignoreFloatingScript;
        notifyPropertyChanged(BR.ignoreFloatingScript);
    }

    @Bindable
    public Boolean getDynamicUpdate() {
        return dynamicUpdate;
    }

    public void setDynamicUpdate(Boolean dynamicUpdate) {
        this.dynamicUpdate = dynamicUpdate;
        notifyPropertyChanged(BR.dynamicUpdate);
    }

    @Bindable
    public MaskConfig getMaskConfig() {
        return maskConfig;
    }

    public void setMaskConfig(MaskConfig maskConfig) {
        this.maskConfig = maskConfig;
        notifyPropertyChanged(BR.maskConfig);
    }

    @NonNull
    @Override
    public SettingProxy clone() {
        try {
            SettingProxy clone = (SettingProxy) super.clone();
            clone.styles = new ObservableArrayList<>();
            for (Style style : styles) {
                clone.styles.add(style.clone());
            }
            if (this.floatPoint != null) {
                clone.floatPoint = this.floatPoint.clone();
            }
            if (this.maskConfig != null) {
                clone.maskConfig = this.maskConfig.clone();
            }
            if (this.operationConfig != null) {
                clone.operationConfig = this.operationConfig.clone();
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

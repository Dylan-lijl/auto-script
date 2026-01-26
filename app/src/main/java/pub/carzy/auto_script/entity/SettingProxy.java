package pub.carzy.auto_script.entity;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.List;

import pub.carzy.auto_script.BR;

/**
 * 录制设置和播放设置等写完root模式之后再决定怎么配置
 *
 * @author admin
 */
public class SettingProxy extends BaseObservable {
    private ObservableList<Style> styles = new ObservableArrayList<>();

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
    public void updateCurrentStyle(){
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

    /**
     * 悬浮窗位置
     */
    private FloatPoint floatPoint;

    @Bindable
    public FloatPoint getFloatPoint() {
        return floatPoint;
    }

    public void setFloatPoint(FloatPoint floatPoint) {
        this.floatPoint = floatPoint;
        notifyPropertyChanged(BR.floatPoint);
    }
}

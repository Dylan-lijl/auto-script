package pub.carzy.auto_script.common_library.entity;

import android.app.Activity;
import android.graphics.drawable.Drawable;

public class ExtToolEntity{
    /**
     * 图标
     */
    private Drawable icon;
    /**
     * 标题
     */
    private String title;
    /**
     * 详细内容
     */
    private String detail;
    /**
     * 是否启用
     */
    private boolean enable;
    /**
     * 跳转的clazz
     */
    private final Class<? extends Activity> clazz;

    public ExtToolEntity(Class<? extends Activity> clazz, Boolean enable) {
        this(clazz, enable, null, null, null);
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Class<? extends Activity> getClazz() {
        return clazz;
    }

    public ExtToolEntity(Class<? extends Activity> clazz, Boolean enable, Drawable icon, String title, String detail) {
        this.clazz = clazz;
        this.enable = enable;
        this.icon = icon;
        this.title = title;
        this.detail = detail;
    }
}

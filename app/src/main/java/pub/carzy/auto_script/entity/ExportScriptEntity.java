package pub.carzy.auto_script.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import pub.carzy.auto_script.db.view.ScriptVoEntity;

/**
 * @author admin
 */
public class ExportScriptEntity {
    public static final int SIZE_DISCREPANCY = 1;
    public static final int DATA_ALREADY_EXISTS = 2;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(Integer screenWidth) {
        this.screenWidth = screenWidth;
    }

    public Integer getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(Integer screenHeight) {
        this.screenHeight = screenHeight;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Integer getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(Integer sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    public void setAndroidVersion(String androidVersion) {
        this.androidVersion = androidVersion;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Collection<ScriptVoEntity> getData() {
        return data;
    }

    public void setData(Collection<ScriptVoEntity> data) {
        this.data = data;
    }

    /**
     * 版本
     */
    private Integer version;
    /**
     * 屏幕宽度
     */
    private Integer screenWidth;
    /**
     * 屏幕高度
     */
    private Integer screenHeight;
    /**
     * 设备名称
     */
    private String device;
    /**
     * sdk版本
     */
    private Integer sdkVersion;
    /**
     * android版本
     */
    private String androidVersion;
    /**
     * 导出时间
     */
    private Date time;
    /**
     * 数据
     */
    private Collection<ScriptVoEntity> data = new ArrayList<>();
}

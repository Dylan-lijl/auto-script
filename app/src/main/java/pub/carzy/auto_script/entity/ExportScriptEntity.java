package pub.carzy.auto_script.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import lombok.Data;
import pub.carzy.auto_script.db.view.ScriptVoEntity;

/**
 * @author admin
 */
@Data
public class ExportScriptEntity {
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

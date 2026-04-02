package pub.carzy.auto_script.config.pojo;

import lombok.Getter;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.entity.MaskConfig;
import pub.carzy.auto_script.entity.OperationConfig;
import pub.carzy.auto_script.entity.Style;

/**
 * @author admin
 */
@Getter
public class SettingKey<T> {
    private final String key;
    private final Class<T> type;

    public SettingKey(String key, Class<T> type) {
        this.key = key;
        this.type = type;
    }

    public static final SettingKey<Boolean> AUTO_CLOSE = new SettingKey<>("autoClose", Boolean.class);
    public static final SettingKey<OperationConfig> OPERATION_CONFIG = new SettingKey<>("operationConfig", OperationConfig.class);
    public static final SettingKey<Boolean> SHOW_OPERATION = new SettingKey<>("showOperation", Boolean.class);
    public static final SettingKey<Boolean> IGNORE_FLOATING_SCRIPT = new SettingKey<>("ignoreFloatingScript", Boolean.class);
    public static final SettingKey<Boolean> ACCEPTED = new SettingKey<>("disclaimerAccepted", Boolean.class);
    public static final SettingKey<Boolean> DYNAMIC_UPDATE = new SettingKey<>("dynamicUpdate", Boolean.class);
    public static final SettingKey<MaskConfig> MASK_CONFIG = new SettingKey<>("maskConfig", MaskConfig.class);
    public static final SettingKey<Integer> TICK = new SettingKey<>("tick", Integer.class);
    public static final SettingKey<String> LANGUAGE = new SettingKey<>("language", String.class);
    public static final SettingKey<String> UUID = new SettingKey<>("uuid", String.class);
    public static final SettingKey<FloatPoint> FLOAT_POINT = new SettingKey<>("floatPoint", FloatPoint.class);
    public static final SettingKey<Style> STYLE = new SettingKey<>("style_", Style.class);
    public static final SettingKey<Boolean> AUTO_PLAY = new SettingKey<>("autoPlay", Boolean.class);
    public static final SettingKey<Integer> TYPE = new SettingKey<>("type", Integer.class);
}

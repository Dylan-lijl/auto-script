package pub.carzy.auto_script.config.pojo;

import lombok.Getter;
import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.entity.Style;

/**
 * @author admin
 */
@Getter
public class SettingKey<T> {
    private final String key;
    private final Class<T> type;
    private final T defaultValue;

    public SettingKey(String key, Class<T> type, T defaultValue) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
    }
    public static final SettingKey<Boolean> ACCEPTED = new SettingKey<>("disclaimer_accepted", Boolean.class, false);
    public static final SettingKey<Integer> TICK = new SettingKey<>("tick", Integer.class, 10);
    public static final SettingKey<String> LANGUAGE = new SettingKey<>("language", String.class, null);
    public static final SettingKey<String> UUID = new SettingKey<>("uuid", String.class, null);
    public static final SettingKey<FloatPoint> FLOAT_POINT = new SettingKey<>("floatPoint", FloatPoint.class, null);
    public static final SettingKey<Style> STYLE = new SettingKey<>("style_", Style.class, null);
    public static final SettingKey<Boolean> AUTO_PLAY = new SettingKey<>("autoPlay", Boolean.class, true);
    public static final SettingKey<Integer> TYPE = new SettingKey<>("type", Integer.class, 0);
}

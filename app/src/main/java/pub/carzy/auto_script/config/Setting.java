package pub.carzy.auto_script.config;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pub.carzy.auto_script.config.pojo.SettingKey;
import pub.carzy.auto_script.entity.Style;

/**
 * @author admin
 */
public interface Setting extends Serializable {
    List<String> keys();

    void reset();

    <T> void write(SettingKey<T> key, T value);

    <T> T read(SettingKey<T> key, T defaultValue);

    default <T> Map<String, T> getAll(SettingKey<T> settingKey) {
        Map<String, T> map = new LinkedHashMap<>();
        for (String key : keys()) {
            if (key.startsWith(settingKey.getKey())) {
                T read = read(settingKey, null);
                if (read != null) {
                    map.put(key, read);
                }
            }
        }
        return map;
    }

    <T> void remove(SettingKey<T> settingKey);

    <T> void update(SettingKey<T> settingKey, T changed);
}

package pub.carzy.auto_script.config.impl;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.config.AbstractSetting;
import pub.carzy.auto_script.config.pojo.SettingKey;

/**
 * pref配置方式
 * @author admin
 */
public class PrefsSetting extends AbstractSetting {
    private static final String PREFS_NAME = "auto_script_prefs";
    private final SharedPreferences prefs;

    private final Gson gson = new Gson();

    public PrefsSetting(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public List<String> keys() {
        return new ArrayList<>(prefs.getAll().keySet());
    }

    @Override
    public void reset() {
        prefs.edit().clear().apply();
    }

    @Override
    public <T> void write(SettingKey<T> settingKey, T value) {
        if (value == null) {
            prefs.edit().putString(settingKey.getKey(), null).apply();
        } else {
            prefs.edit().putString(settingKey.getKey(), new Gson().toJson(value)).apply();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(SettingKey<T> settingKey, T defaultValue) {
        String key = settingKey.getKey();
        Class<T> clazz = settingKey.getType();
        if (!prefs.contains(key)) {
            return defaultValue;
        }
        //兼容之前版本
        String json = null;
        try {
            json = prefs.getString(key, null);
        } catch (ClassCastException e) {
            Object object = prefs.getAll().get(key);
            if (object != null) {
                json = gson.toJson(object);
                write(settingKey, (T) object);
            }
        }
        if (json == null) {
            return defaultValue;
        }
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            Log.d(TAG, "read: 转换失败:" + e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public <T> void remove(SettingKey<T> settingKey) {
        prefs.edit().remove(settingKey.getKey()).apply();
    }

    @Override
    public <T> void update(SettingKey<T> settingKey, T changed) {
        remove(settingKey);
        write(settingKey, changed);
    }

}

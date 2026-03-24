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
import pub.carzy.auto_script.entity.Style;
import pub.carzy.auto_script.utils.MixedUtil;

/**
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
        String key = settingKey.getKey();
        SharedPreferences.Editor editor = prefs.edit();
        if (value == null) {
            editor.remove(key);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float || value instanceof Double) {
            editor.putFloat(key, ((Number) value).floatValue());
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else {
            try {
                editor.putString(key, gson.toJson(value));
            } catch (Exception e) {
                Log.d(TAG, "write: 转成json失败:" + e.getMessage());
            }
        }
        editor.apply();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(SettingKey<T> settingKey, T defaultValue) {
        String key = settingKey.getKey();
        Class<T> clazz = settingKey.getType();
        if (!prefs.contains(key)) {
            return defaultValue == null ? settingKey.getDefaultValue() : defaultValue;
        }
        if (clazz == Integer.class) {
            return (T) Integer.valueOf(prefs.getInt(key, -1));
        } else if (clazz == Long.class) {
            return (T) Long.valueOf(prefs.getLong(key, -1));
        } else if (clazz == Boolean.class) {
            return (T) Boolean.valueOf(prefs.getBoolean(key, false));
        } else if (clazz == Float.class) {
            return (T) Float.valueOf(prefs.getFloat(key, -1));
        } else if (clazz == Double.class) {
            return (T) Double.valueOf(prefs.getFloat(key,-1));
        } else if (clazz == String.class) {
            return (T) prefs.getString(key,"");
        } else {
            String json = prefs.getString(key, null);
            if (json == null) return defaultValue;
            try {
                return gson.fromJson(json, clazz);
            } catch (JsonSyntaxException e) {
                Log.d(TAG, "read: 转成json失败:" + e.getMessage());
                return defaultValue;
            }
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

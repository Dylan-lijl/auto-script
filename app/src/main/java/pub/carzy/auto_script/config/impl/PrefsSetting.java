package pub.carzy.auto_script.config.impl;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import pub.carzy.auto_script.config.AbstractSetting;

public class PrefsSetting extends AbstractSetting {
    private static final String PREFS_NAME = "auto_script_prefs";
    private final SharedPreferences prefs;

    private final Gson gson = new Gson();

    public PrefsSetting(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void reset() {
        prefs.edit().clear();
    }

    @Override
    protected <T> void write(String key, T value) {
        if (value == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        if (value instanceof Integer) {
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
    protected <T> T read(String key, T defaultValue, Class<T> clazz) {
        if (!prefs.contains(key)) {
            return defaultValue;
        }
        if (clazz == Integer.class) {
            return (T) Integer.valueOf(prefs.getInt(key, (Integer) defaultValue));
        } else if (clazz == Long.class) {
            return (T) Long.valueOf(prefs.getLong(key, (Long) defaultValue));
        } else if (clazz == Boolean.class) {
            return (T) Boolean.valueOf(prefs.getBoolean(key, (Boolean) defaultValue));
        } else if (clazz == Float.class) {
            return (T) Float.valueOf(prefs.getFloat(key, (Float) defaultValue));
        } else if (clazz == Double.class) {
            return (T) Double.valueOf(prefs.getFloat(key, ((Number) defaultValue).floatValue()));
        } else if (clazz == String.class) {
            return (T) prefs.getString(key, defaultValue != null ? (String) defaultValue : null);
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

}

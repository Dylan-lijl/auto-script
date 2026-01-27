package pub.carzy.auto_script.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.RawRes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.BasicFileImport;
import pub.carzy.auto_script.entity.ExportScriptEntity;
import pub.carzy.auto_script.entity.WrapperEntity;
import pub.carzy.auto_script.utils.statics.StaticValues;

/**
 * @author admin
 */
public class MixedUtil {
    public static void exportScript(Collection<ScriptVoEntity> entities, Context context, Consumer<String> callback) {
        if (entities.isEmpty()) {
            return;
        }
        Gson gson = new Gson();
        ExportScriptEntity exportScript = new ExportScriptEntity();
        //基础数据
        exportScript.setVersion(StaticValues.DATA_VERSION);
        exportScript.setTime(new Date());
        exportScript.setDevice(Build.BRAND + "_" + Build.MODEL);
        exportScript.setAndroidVersion(Build.VERSION.RELEASE);
        exportScript.setSdkVersion(Build.VERSION.SDK_INT);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        exportScript.setScreenWidth(metrics.widthPixels);
        exportScript.setScreenHeight(metrics.heightPixels);
        //脚本数据
        exportScript.getData().addAll(entities);
        //调用报错文件api
        StoreUtil.promptAndSaveFile(gson.toJson(exportScript),
                "auto_script_" + new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss", Locale.getDefault())
                        .format(exportScript.getTime()) + ".json",
                result -> {
                    if (callback != null) {
                        callback.accept(result);
                    }
                });
    }

    public static String githubSourceRepositoryUrl() {
        return "https://github.com/Dylan-lijl/auto-script";
    }

    @SuppressLint("SimpleDateFormat")
    public static Date getReleaseTime() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse("2026-01-19");
        } catch (ParseException e) {
            return null;
        }
    }

    public static <T> T getValueOrDefault(T v, T d) {
        if (v == null) {
            return d;
        }
        return v;
    }

    public static String colorToHex(int color) {
        return String.format("#%08X", color);
    }

    /**
     * 返回 true 表示是亮色，文字应设为黑色
     *
     * @param color 颜色
     * @return 是否是亮色
     */
    public static boolean isColorLight(int color) {
        if (color == Color.TRANSPARENT) return true;
        // 提取 RGB 分量
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // 计算亮度值 (0-255)
        double darkness = 1 - (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        // 阈值通常设为 0.5 或 128
        return darkness < 0.5;
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadFileData(Context context, @RawRes int resId, TypeToken<T> token) {
        InputStream is = context.getResources().openRawResource(resId);
        try {
            try (InputStreamReader reader = new InputStreamReader(is)) {
                Gson gson = new Gson();
                T wrapper = gson.fromJson(reader, token);
                if (wrapper instanceof WrapperEntity<?>) {
                    WrapperEntity<?> we = (WrapperEntity<?>) wrapper;
                    List<?> data = we.getData();
                    if (data != null && !data.isEmpty() && data.get(0) instanceof BasicFileImport) {
                        ((List<BasicFileImport>) data).sort(Comparator.comparing(BasicFileImport::getOrder));
                    }
                }
                return (T) wrapper;
            } catch (IOException e) {
                Log.e("DialogueProcessCallback#loadData", "", e);
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e("DialogueProcessCallback#loadData", "", e);
            }
        }
        return null;
    }
}

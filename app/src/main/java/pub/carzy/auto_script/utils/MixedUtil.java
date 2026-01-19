package pub.carzy.auto_script.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import pub.carzy.auto_script.db.entity.ScriptActionEntity;
import pub.carzy.auto_script.db.entity.ScriptEntity;
import pub.carzy.auto_script.db.entity.ScriptPointEntity;
import pub.carzy.auto_script.db.view.ScriptVoEntity;
import pub.carzy.auto_script.entity.ExportScriptEntity;
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
}

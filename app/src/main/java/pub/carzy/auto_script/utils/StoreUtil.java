package pub.carzy.auto_script.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cn.hutool.core.lang.Pair;
import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanFactory;
import pub.carzy.auto_script.config.ControllerCallback;

/**
 * @author admin
 */
public class StoreUtil {
    public static void promptAndSaveFile(InputStream stream, String suggestedName, @NonNull ControllerCallback<String> runnable) {
        //这里如果read等于null或者以及结束了就直接返回
        if (stream == null) {
            runnable.catchMethod(new Exception("reader is null!"));
            runnable.finallyMethod();
            return;
        }
        //写文件
        Startup startup = BeanFactory.getInstance().get(Startup.class);
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(file, suggestedName);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            try (BufferedInputStream reader = new BufferedInputStream(stream);
                 BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(file))) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, len);
                }
                writer.flush();
                runnable.complete(file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            runnable.finallyMethod();
        }
    }

    public static void promptAndSaveFile(String data, String suggestedName, @NonNull ControllerCallback<String> runnable, Charset charset) {
        if (data == null) {
            runnable.catchMethod(new Exception("data is null!"));
            return;
        }
        try {
            // 1. 将字符串按照指定的字符集（推荐使用 UTF-8）编码成字节数组
            // 使用 StandardCharsets.UTF_8 是 JDK 1.6 及以上版本的推荐做法
            byte[] bytes = data.getBytes(charset == null ? StandardCharsets.UTF_8 : charset);
            // 2. 使用字节数组创建 ByteArrayInputStream
            InputStream inputStream = new ByteArrayInputStream(bytes);
            promptAndSaveFile(inputStream, suggestedName, runnable);
        } catch (Exception e) {
            runnable.catchMethod(e);
            runnable.finallyMethod();
        }
    }

    public static void promptAndSaveFile(String data, String suggestedName, @NonNull ControllerCallback<String> runnable) {
        promptAndSaveFile(data, suggestedName, runnable, null);
    }
}

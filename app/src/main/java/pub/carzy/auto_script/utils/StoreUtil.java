package pub.carzy.auto_script.utils;

import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import pub.carzy.auto_script.Startup;
import pub.carzy.auto_script.config.BeanContainer;

/**
 * @author admin
 */
public class StoreUtil {
    public static void promptAndSaveFile(InputStream stream, String suggestedName, @NonNull Consumer<String> runnable) {
        //这里如果read等于null或者以及结束了就直接返回
        if (stream == null) {
            return;
        }
        //写文件
        Startup startup = BeanContainer.getInstance().get(Startup.class);
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
            if (!mkdir) {
                return;
            }
        }
        file = new File(file, suggestedName);
        if (file.exists()) {
            boolean delete = file.delete();
            if (!delete) {
                return;
            }
        }
        try {
            boolean newFile = file.createNewFile();
            if (!newFile) {
                return;
            }
            try (BufferedInputStream reader = new BufferedInputStream(stream);
                 BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(file))) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, len);
                }
                writer.flush();
                runnable.accept(file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void promptAndSaveFile(String data, String suggestedName, @NonNull Consumer<String> runnable, Charset charset) {
        // 1. 将字符串按照指定的字符集（推荐使用 UTF-8）编码成字节数组
        // 使用 StandardCharsets.UTF_8 是 JDK 1.6 及以上版本的推荐做法
        byte[] bytes = data.getBytes(charset == null ? StandardCharsets.UTF_8 : charset);
        // 2. 使用字节数组创建 ByteArrayInputStream
        InputStream inputStream = new ByteArrayInputStream(bytes);
        promptAndSaveFile(inputStream, suggestedName, runnable);
    }

    public static void promptAndSaveFile(String data, String suggestedName, @NonNull Consumer<String> runnable) {
        promptAndSaveFile(data, suggestedName, runnable, null);
    }
}

package pub.carzy.auto_script.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author admin
 */
public class ThreadUtil {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /**
     * I/O 密集型线程池（网络、文件、数据库等）
     */
    private static final ExecutorService IO = Executors.newCachedThreadPool();

    /**
     * CPU 密集型线程池（计算，解析等）
     */
    private static final ExecutorService CPU = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    public static void runOnUi(Runnable runnable) {
        MAIN.post(runnable);
    }

    public static void runOnUi(Runnable runnable, int delay) {
        MAIN.postDelayed(runnable, delay);
    }

    public static void runOnIo(Runnable runnable) {
        IO.execute(runnable);
    }

    public static void runOnCpu(Runnable runnable) {
        CPU.execute(runnable);
    }
}

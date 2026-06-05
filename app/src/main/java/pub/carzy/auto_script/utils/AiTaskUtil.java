package pub.carzy.auto_script.utils;

import android.util.Log;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.HttpClient;

import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.hutool.core.lang.func.VoidFunc0;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class AiTaskUtil {
    private static final String TAG = "AiTaskUtil";

    // 1. 全局静态标准线程池（必须用它提交任务，cancel(true) 才会触发网络层的 InterruptedException）
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    // 2. 静态映射表：管理各个界面的异步任务句柄
    private static final ConcurrentHashMap<Object, Future<?>> TASK_MAP = new ConcurrentHashMap<>();

    // 3. 静态映射表：管理各个界面的 Client 引用，用于反射
    private static final ConcurrentHashMap<Object, com.openai.client.OpenAIClient> CLIENT_MAP = new ConcurrentHashMap<>();

    /**
     * 静态创建 Client
     */
    public static com.openai.client.OpenAIClient createClient(Object tag, String url, String key) {
        com.openai.client.okhttp.OpenAIOkHttpClient.Builder builder =
                com.openai.client.okhttp.OpenAIOkHttpClient.builder();
        builder.baseUrl(url);
        if (key == null || key.isEmpty()) {
            builder.apiKey("");
        } else {
            builder.apiKey(key);
        }

        com.openai.client.OpenAIClient client = builder.build();
        if (tag != null) {
            CLIENT_MAP.put(tag, client);
        }
        return client;
    }

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void setCurrentTask(Object tag, Future<?> task) {
        if (tag != null && task != null) {
            TASK_MAP.put(tag, task);
        }
    }

    /**
     * ⭐ 终极静态秒杀：一键物理掐断当前界面的网络阻塞
     */
    public static void cancel(Object tag) {
        if (tag == null) return;

        // 1. 逻辑终止异步链条（发送线程 interrupt 信号）
        Future<?> task = TASK_MAP.remove(tag);
        if (task != null && !task.isDone()) {
            task.cancel(true);
        }

        // 2. 物理掐断当前 tag 对应的底层网络连接
        com.openai.client.OpenAIClient client = CLIENT_MAP.remove(tag);
        if (client != null) {
            reflectAndCancelOkHttp(client);
        }
    }

    /**
     * ⭐ 针对 Stainless 官方 SDK 专门定制的黑魔法反射
     */
    public static void reflectAndCancelOkHttp(com.openai.client.OpenAIClient client) {
        try {
            // 路径 1: OpenAIClientImpl 内部持有 clientOptions
            Field clientOptionsField = client.getClass().getDeclaredField("clientOptions");
            clientOptionsField.setAccessible(true);
            Object clientOptions = clientOptionsField.get(client);

            // 路径 2: ClientOptions 内部持有 httpClient (类型为 com.openai.core.http.HttpClient)
            Field httpClientField = clientOptions.getClass().getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            Object httpClient = httpClientField.get(clientOptions);

            // 路径 3: 这里的 httpClient 实际实现类大概率是 com.openai.client.okhttp.OkHttpClient
            // 它的内部一定持有了原生的 okhttp3.OkHttpClient 变量。
            // 我们直接遍历它所有的属性，只要找到类型是 okhttp3.OkHttpClient 的，直接揪出来！
            OkHttpClient rawOkHttpClient = null;
            Field[] fields = httpClient.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object object = field.get(httpClient);
                if (object instanceof HttpClient) {
                    try {
                        Field clientField = object.getClass().getDeclaredField("httpClient");
                        clientField.setAccessible(true);
                        Object httpC = clientField.get(object);
                        if (httpC instanceof com.openai.client.okhttp.OkHttpClient) {
                            Field okHttpClient = httpC.getClass().getDeclaredField("okHttpClient");
                            okHttpClient.setAccessible(true);
                            rawOkHttpClient = (OkHttpClient) okHttpClient.get(httpC);
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // 路径 4: 成功拿到原生 OkHttpClient，直接下达终结指令
            if (rawOkHttpClient != null) {
                Consumer<Call> cancel = Call::cancel;
                rawOkHttpClient.dispatcher().queuedCalls().forEach(cancel);
                rawOkHttpClient.dispatcher().runningCalls().forEach(cancel);
                Log.i(TAG, "🔥 成功破开官方 SDK 封装，强行掐断所有网络阻塞！");
            } else {
                Log.e(TAG, "未能在 HttpClient 内部找到原生的 OkHttpClient 变量");
            }

        } catch (Exception e) {
            Log.e(TAG, "反射终止网络阻塞失败: " + e.getMessage(), e);
        }
    }

    public static boolean isCancelException(Throwable throwable) {
        if (throwable == null) return false;
        Throwable cause = throwable instanceof java.util.concurrent.CompletionException ? throwable.getCause() : throwable;
        return cause instanceof CancellationException
                || cause instanceof InterruptedException
                || cause instanceof java.io.InterruptedIOException
                || "Canceled".equals(cause.getMessage());
    }

    public static class TaskResult<T> {
        private final Runnable cancel;
        private final CompletableFuture<T> future;

        public TaskResult(Runnable cancel, CompletableFuture<T> future) {
            this.cancel = cancel;
            this.future = future;
        }

        public Runnable getCancel() {
            return cancel;
        }

        public CompletableFuture<T> getFuture() {
            return future;
        }

        public void cancel(boolean mayInterruptIfRunning) {
            future.cancel(mayInterruptIfRunning);
            cancel.run();
        }
    }

    public static <T> TaskResult<T> wrapper(Supplier<OpenAIClient> clientSupplier, Function<OpenAIClient, CompletableFuture<T>> function) {
        OpenAIClient client = clientSupplier.get();
        return new TaskResult<>(() -> {
            if (client != null) {
                reflectAndCancelOkHttp(client);
            }
        }, function.apply(client));
    }
}
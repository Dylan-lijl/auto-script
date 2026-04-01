package pub.carzy.auto_script.core.sub;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.Stopwatch;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * 监听获取事件实现抽象类
 *
 * @author admin
 */
public abstract class AbstractGetEventRecorder<T> extends AbstractRecorderLifeCycle<T> {
    protected Runnable runnable;
    protected final Stopwatch stopwatch;
    protected Process process;

    public AbstractGetEventRecorder(Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
    }

    /**
     * 公共逻辑,先得到root进程,然后写入监听命令并循环读取返回内容
     *
     * @param devicePath 设备路径
     * @param listener   回调处理器 解析成完整对象时回调
     */
    @Override
    protected void doStart(String devicePath, OnRecordListener<T> listener) {
        try {
            process = Shell.getRootProcess();
            OutputStream os = process.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            //写命令
            os.write(String.format(START_COMMAND, devicePath).getBytes());
            os.flush();
            //创建停止回调
            runnable = createSendEndCommand(os);
            String line;
            boolean started = false;
            //循环读取数据
            while (isRunning.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(START_CONTENT)) {
                    started = true;
                    continue;
                }
                if (line.contains(END_CONTENT)) {
                    break;
                }
                //暂停时,标记第一次
                if (isPaused.get()) {
                    if (!consumed.get()) {
                        if (reading != null) {
                            reading.pause();
                        }
                        consumed.set(true);
                    }
                    continue;
                }
                //过滤不是数据行,暂停状态以及未解析到开始符
                if (!line.contains("]") || !started) {
                    continue;
                }

                // 解析标准格式: [ timestamp] type code value
                String[] parts = line.substring(line.indexOf("]") + 1).trim().split("\\s+");
                if (parts.length < 3) {
                    continue;
                }

                int type = Integer.parseInt(parts[0], 16);
                int code = Integer.parseInt(parts[1], 16);
                // 使用Long解析防止十六进制符号位溢出
                int value = (int) Long.parseLong(parts[2], 16);
                Log.d("doStart", type + " " + code + " " + value);
                //子类实现实际解析逻辑
                handleEvent(type, code, value, stopwatch.getElapsedMillis(), listener);
            }
        } catch (Exception e) {
            Log.e(AbstractGetEventRecorder.class.getCanonicalName(), "Read Error: " + e.getMessage());
        }
    }

    /**
     * 处理内容 标准格式: [ timestamp] type code value
     * 具体参考值参考这个: {@link pub.carzy.auto_script.utils.InputConstants}
     *
     * @param type          type
     * @param code          code
     * @param value         value
     * @param elapsedMillis 时间点
     * @param listener      回调
     */
    protected abstract void handleEvent(int type, int code, int value, long elapsedMillis, OnRecordListener<T> listener);

    /**
     * 创建停止命令回调
     *
     * @param os 流
     * @return 回调
     */
    protected Runnable createSendEndCommand(OutputStream os) {
        return () -> {
            try {
                Thread.sleep(5);
                os.write(END_COMMAND.getBytes());
                os.flush();
            } catch (Exception e) {
                Log.d(AbstractGetEventRecorder.class.getCanonicalName(), "写ctrl c命令失败!", e);
            }
        };
    }

    @Override
    public void stop() {
        super.stop();
        if (runnable != null) {
            ThreadUtil.runOnCpu(() -> runnable.run());
        }
    }

    @Override
    public void pause() {
        super.pause();
        consumed.set(false);
    }

    @Override
    public void destroy() {
        super.destroy();
        process.destroy();
    }
}

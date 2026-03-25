package pub.carzy.auto_script.service.sub;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.utils.InputConstants;
import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.Stopwatch;
import pub.carzy.auto_script.utils.ThreadUtil;

/**
 * @author admin
 */
public class KeyRecorder extends AbstractRecorderLifeCycle<KeyEntity> {
    private static final String TAG = "KeyRecorder";
    private final Map<Integer, KeyEntity> keyMap;
    private Runnable runnable;
    private final Stopwatch stopwatch;
    private Process process;

    public KeyRecorder(Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
        keyMap = new HashMap<>();
    }

    @Override
    public void start(String devicePath, OnRecordListener<KeyEntity> listener) {
        isRunning.set(true);
        keyMap.clear();
        try {
            process = Shell.getRootProcess();
            OutputStream os = process.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 启动 getevent
            os.write(("getevent -t " + devicePath + "& echo " + START_CONTENT + "\n").getBytes());
            os.flush();
            runnable = () -> {
                try {
                    Thread.sleep(5);
                    os.write(("echo " + END_CONTENT + "\n").getBytes());
                    os.flush();
                } catch (Exception e) {
                    Log.d(TAG, "写ctrl c命令失败!", e);
                }
            };
            String line;
            boolean started = false;
            while (isRunning.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(START_CONTENT)) {
                    started = true;
                    continue;
                }
                if (line.contains(END_CONTENT)) {
                    break;
                }
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
                if (!line.contains("]") || !started) continue;

                // 解析标准格式: [ timestamp] type code value
                String[] parts = line.substring(line.indexOf("]") + 1).trim().split("\\s+");
                if (parts.length < 3) continue;

                int type = Integer.parseInt(parts[0], 16);
                int code = Integer.parseInt(parts[1], 16);
                // 使用Long解析防止十六进制符号位溢出
                int value = (int) Long.parseLong(parts[2], 16);
                handleEvent(type, code, value, stopwatch.getElapsedMillis(), listener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Read Error: " + e.getMessage());
        }
        if (reading != null) {
            reading.stop();
        }
    }

    private void handleEvent(int type, int code, int value, long elapsed, OnRecordListener<KeyEntity> listener) {
        // 1. 处理 Slot 切换 (EV_ABS: 0003, ABS_MT_SLOT: 002f)
        if (type != InputConstants.EV_KEY) return;
        // value:
        // 0 = UP
        // 1 = DOWN
        // 2 = REPEAT
        if (value == 1) {
            // 按下
            KeyEntity key = new KeyEntity();
            key.setCode(code);
            key.setDownTime(elapsed);
            keyMap.put(code, key);
        } else if (value == 0) {
            // 松开
            KeyEntity key = keyMap.remove(code);
            if (key != null) {
                key.setUpTime(elapsed);
                if (listener != null) listener.onCaptured(key);
            }
        }
    }

    @Override
    public void stop() {
        isRunning.set(false);
        if (runnable != null) {
            ThreadUtil.runOnCpu(runnable);
        }
    }

    @Override
    public void pause() {
        isPaused.set(true);
    }

    /**
     * 恢复录制
     */
    @Override
    public void resume() {
        isPaused.set(false);
    }

    @Override
    public void destroy() {
        isRunning.set(false);
        process.destroy();
    }

}
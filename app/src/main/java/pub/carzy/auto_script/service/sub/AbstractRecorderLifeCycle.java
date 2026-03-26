package pub.carzy.auto_script.service.sub;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.utils.Shell;

/**
 * @author admin
 */
public abstract class AbstractRecorderLifeCycle<T> implements RecorderLifeCycle<T> {
    protected OnRecordReading reading;
    protected final AtomicBoolean isRunning;
    protected final AtomicBoolean isPaused;
    protected final AtomicBoolean consumed;
    public static final String START_COMMAND = "getevent -t %s & echo " + START_CONTENT + "\n";
    public static final String END_COMMAND = "echo " + END_CONTENT + "\n";

    public AbstractRecorderLifeCycle() {
        isRunning = new AtomicBoolean(false);
        isPaused = new AtomicBoolean(false);
        consumed = new AtomicBoolean(false);
    }

    @Override
    public void setReadingBack(OnRecordReading readingBack) {
        reading = readingBack;
    }

    @Override
    public void start(String devicePath, OnRecordListener<T> listener) {
        if (isRunning.get()) {
            return;
        }
        isRunning.set(true);
        clear();
        doStart(devicePath, listener);
        if (reading != null) {
            reading.stop();
        }
    }

    protected abstract void doStart(String devicePath, OnRecordListener<T> listener);

    @Override
    public void stop() {
        isRunning.set(false);
    }

    @Override
    public void pause() {
        isPaused.set(true);
    }

    @Override
    public void resume() {
        isPaused.set(false);
    }

    @Override
    public void destroy() {
        isRunning.set(false);
    }
}

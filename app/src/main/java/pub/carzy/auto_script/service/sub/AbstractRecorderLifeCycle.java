package pub.carzy.auto_script.service.sub;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author admin
 */
public abstract class AbstractRecorderLifeCycle<T> implements RecorderLifeCycle<T> {
    protected OnRecordReading reading;
    protected final AtomicBoolean isRunning;
    protected final AtomicBoolean isPaused;
    protected final AtomicBoolean consumed;

    public AbstractRecorderLifeCycle() {
        isRunning = new AtomicBoolean(false);
        isPaused = new AtomicBoolean(false);
        consumed = new AtomicBoolean(false);
    }

    @Override
    public void setReadingBack(OnRecordReading readingBack) {
        reading = readingBack;
    }
}

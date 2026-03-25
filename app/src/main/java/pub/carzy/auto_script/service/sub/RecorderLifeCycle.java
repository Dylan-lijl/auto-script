package pub.carzy.auto_script.service.sub;

public interface RecorderLifeCycle<T> {

    void start(String devicePath, OnRecordListener<T> listener);

    void stop();

    void pause();

    void resume();

    void destroy();

    void setReadingBack(OnRecordReading readingBack);

    interface OnRecordListener<T> {
        void onCaptured(T data);
    }

    interface OnRecordReading {
        void pause(Object... args);

        void stop(Object... args);
    }

    String START_CONTENT = "__-start-__";
    String END_CONTENT = "__-end-__";
}

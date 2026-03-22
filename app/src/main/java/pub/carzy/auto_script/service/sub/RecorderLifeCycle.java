package pub.carzy.auto_script.service.sub;

public interface RecorderLifeCycle<T> {

    void start(String devicePath, OnRecordListener<T> listener);

    void stop();

    void pause();

    void resume();

    void destroy();

    interface OnRecordListener<T> {
        void onCaptured(T data);
    }

    String START_CONTENT = "__-start-__";
    String END_CONTENT = "__-end-__";
}

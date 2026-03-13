package pub.carzy.auto_script.service;

/**
 * @author admin
 */
public interface ScriptEngine {
    void init(ResultCallback callback);
    void start(Object... args);
    void close();
    void reset();
    interface ResultCallback{
        /**
         * 用户取消弹窗
         */
        int CANCEL = 0;
        /**
         * 异常导致
         */
        int EXCEPTION = -1;
        /**
         * 未知错误
         */
        int UNKNOWN = -2;
        /**
         * 跳转页面
         */
        int JUMP = 1;
        void onFail(int code,Object... args);
        void onSuccess();
    }
}

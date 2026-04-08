package pub.carzy.auto_script.core;

import java.util.function.BiConsumer;

import pub.carzy.auto_script.entity.FloatPoint;

/**
 * @author admin
 */
public interface ScriptEngine {
    void init(ResultCallback callback);

    void start(Object... args);

    void close();

    void reset();
    void savePointCallback(BiConsumer<Integer,Integer> consumer);
    interface ResultCallback {
        /**
         * 异常导致
         */
        int EXCEPTION = 0b0001;
        /**
         * 未知错误
         */
        int UNKNOWN = 0b0010;
        /**
         * 跳转页面
         */
        int JUMP = 0b0100;
        /**
         * 用户取消弹窗
         */
        int CANCEL = 0b1000;
        int ACCESSIBLE = 0b1_0000;
        int ROOT = 0b10_0000;
        int FLOATING = 0b100_0000;
        int MISSING = 0b1000_0000;

        void onFail(int code, Object... args);

        void onSuccess();

        static boolean hasFlags(int code, int... args) {
            if (args == null || args.length == 0) {
                return false;
            }
            for (int flag : args) {
                // 使用 (code & flag) == flag 来确保该位完全匹配
                if ((code & flag) != flag) {
                    return false;
                }
            }
            return true;
        }
    }
    class ScriptConfig{
        public FloatPoint floatPoint;
        public Boolean dynamicUpdate;
    }
}

package pub.carzy.auto_script.service;

/**
 * 全局脚本管理器
 *
 * @author admin
 */
public class GlobalSingletonScriptEngineController {
    private static final GlobalSingletonScriptEngineController INSTANCE = new GlobalSingletonScriptEngineController();
    private volatile ScriptEngine engine;

    private GlobalSingletonScriptEngineController() {
    }

    public static GlobalSingletonScriptEngineController getInstance() {
        return INSTANCE;
    }

    public synchronized void open(ScriptEngine engine, ScriptEngine.ResultCallback callback) {
        if (this.engine != null && this.engine != engine) {
            this.engine.close();
        }
        if (this.engine == engine) {
            engine.reset();
            callback.onSuccess();
        } else {
            engine.init(new ScriptEngine.ResultCallback() {
                @Override
                public void onFail(int code, Object... args) {
                    callback.onFail(code, args);
                }

                @Override
                public void onSuccess() {
                    synchronized (GlobalSingletonScriptEngineController.this) {
                        GlobalSingletonScriptEngineController.this.engine = engine;
                    }
                    callback.onSuccess();
                }
            });
        }
    }

    public boolean start(Object... objects) {
        if (engine == null) {
            return false;
        } else {
            engine.start(objects);
            return true;
        }
    }

    public synchronized void close() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }
}

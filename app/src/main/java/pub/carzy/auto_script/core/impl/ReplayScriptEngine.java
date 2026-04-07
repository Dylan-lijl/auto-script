package pub.carzy.auto_script.core.impl;

import java.util.function.BiConsumer;

import pub.carzy.auto_script.core.ScriptEngine;

/**
 * @author admin
 */
public interface ReplayScriptEngine extends ScriptEngine {
    class ReplayConfig extends ScriptConfig{
        public Integer tick;
    }
}

package pub.carzy.auto_script.core.impl;


import pub.carzy.auto_script.core.ScriptEngine;

/**
 * 回放脚本引擎
 * @author admin
 */
public interface ReplayScriptEngine extends ScriptEngine {
    class ReplayConfig extends ScriptConfig{
        public Integer tick;
    }
}

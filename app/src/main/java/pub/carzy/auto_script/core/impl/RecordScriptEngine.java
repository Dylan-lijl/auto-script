package pub.carzy.auto_script.core.impl;

import pub.carzy.auto_script.core.ScriptEngine;
import pub.carzy.auto_script.entity.MaskConfig;

/**
 * @author admin
 */
public interface RecordScriptEngine extends ScriptEngine {
    class RecordConfig extends ScriptConfig{
        public MaskConfig maskConfig;
    }
}

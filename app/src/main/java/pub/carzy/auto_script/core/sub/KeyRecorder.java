package pub.carzy.auto_script.core.sub;

import java.util.HashMap;
import java.util.Map;

import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.utils.InputConstants;
import pub.carzy.auto_script.utils.Stopwatch;

/**
 * @author admin
 */
public class KeyRecorder extends AbstractGetEventRecorder<KeyEntity> {
    private final Map<Integer, KeyEntity> keyMap;

    public KeyRecorder(Stopwatch stopwatch) {
        super(stopwatch);
        keyMap = new HashMap<>();
    }

    @Override
    protected void handleEvent(int type, int code, int value, long elapsed, OnRecordListener<KeyEntity> listener) {
        if (type != InputConstants.EV_KEY) {
            return;
        }
        if (value == InputConstants.KEY_PRESS) {
            // 1 = DOWN 按下
            KeyEntity key = new KeyEntity();
            key.setCode(code);
            key.setDownTime(elapsed);
            keyMap.put(code, key);
        } else if (value == InputConstants.KEY_RELEASE) {
            // 0 = UP 松开
            KeyEntity key = keyMap.remove(code);
            if (key != null) {
                key.setUpTime(elapsed);
                if (listener != null) {
                    listener.onCaptured(key);
                }
            }
        }
        //2 = REPEAT
    }

    @Override
    public void clear() {
        keyMap.clear();
    }
}
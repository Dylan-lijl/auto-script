package pub.carzy.auto_script.service.sub;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pub.carzy.auto_script.entity.KeyEntity;
import pub.carzy.auto_script.entity.MotionEntity;
import pub.carzy.auto_script.entity.PointEntity;
import pub.carzy.auto_script.utils.InputConstants;
import pub.carzy.auto_script.utils.Shell;
import pub.carzy.auto_script.utils.Stopwatch;
import pub.carzy.auto_script.utils.ThreadUtil;

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
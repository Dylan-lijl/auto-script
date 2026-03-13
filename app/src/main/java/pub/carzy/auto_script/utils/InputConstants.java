package pub.carzy.auto_script.utils;

/**
 * @author admin
 */
public class InputConstants {
    // Event types
    public static final int EV_SYN = 0x00;
    public static final int SYN_REPORT = 0x00;
    public static final int EV_KEY = 0x01;
    public static final int EV_ABS = 0x03;

    // ABS codes
    public static final int ABS_MT_SLOT = 0x2f;
    public static final int ABS_MT_TRACKING_ID = 0x39;
    public static final int ABS_MT_POSITION_X = 0x35;
    public static final int ABS_MT_POSITION_Y = 0x36;
    public static final int ABS_MT_TOUCH_MAJOR = 0x30;

    // KEY codes
    public static final int BTN_TOUCH = 330;
    public static final int KEY_PRESS = 1;
    public static final int KEY_RELEASE = 0;
}

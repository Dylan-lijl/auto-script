package pub.carzy.auto_script.utils;

/**
 * @author admin
 */
public class InputConstants {
    //公共常量
    public static final int EMPTY = 0x00;
    public static final int TRACKING_ID_END = -1;
    //----------------type----------------
    //同步事件
    public static final int EV_SYN = 0x00;
    //按键
    public static final int EV_KEY = 0x01;
    //相对坐标，比如鼠标
    public static final int EV_REL = 0x02;
    //绝对坐标，比如触摸屏
    public static final int EV_ABS = 0x03;
    // 杂项
    public static final int EV_MSC = 0x04;
    // 开关
    public static final int EV_SW = 0x05;
    public static final int EV_LED = 0x11;
    public static final int EV_SND = 0x12;
    // 重复
    public static final int EV_REP = 0x14;
    public static final int EV_FF = 0x15;
    public static final int EV_PWR = 0x16;
    //----------------code----------------
    //================EV_ABS==============
    // X坐标
    public static final int ABS_MT_POSITION_X = 0x35;
    // Y坐标
    public static final int ABS_MT_POSITION_Y = 0x36;
    // 手指ID
    public static final int ABS_MT_TRACKING_ID = 0x39;
    // 压力
    public static final int ABS_MT_PRESSURE = 0x003A;
    // 多点触控槽位
    public static final int ABS_MT_SLOT = 0x2f;
    // 接触面积
    public static final int ABS_MT_TOUCH_MAJOR = 0x30;

    public static final int ABS_MT_WIDTH_MAJOR = 0x32;
    //================EV_KEY================
    // 音量+
    public static final int KEY_VOLUME_UP = 0x0072;
    // 音量-
    public static final int KEY_VOLUME_DOWN = 0x0073;
    // W键
    public static final int KEY_W = 0x001A;
    // 回车
    public static final int KEY_ENTER = 0x001C;
    // Home
    public static final int KEY_HOME = 0x0066;
    // 返回
    public static final int KEY_BACK = 0x009E;
    public static final int KEY_MENU = 0x008B;
    //拨号
    public static final int KEY_CALL = 0x0005;
    // 挂断
    public static final int KEY_END_CALL = 0x0006;
    // 电源键
    public static final int KEY_POWER = 0x0074;
    //===============EV_SYN==================
    public static final int SYN_REPORT = 0x00;
    public static final int SYN_CONFIG = 0x01;
    //多点触控同步
    public static final int SYN_MT_REPORT = 0x02;
    public static final int SYN_DROPPED = 0x03;
    //================ EV_REL（鼠标） =================
    public static final int REL_X = 0x00;
    public static final int REL_Y = 0x01;
    //----------------code----------------
    public static final int BTN_TOUCH = 0x14A;
    //----------------value----------------
    // 抬起
    public static final int KEY_RELEASE = 0x00;
    // 按下
    public static final int KEY_PRESS = 0x01;
    // 重复
    public static final int KEY_REPEAT = 0x02;
}

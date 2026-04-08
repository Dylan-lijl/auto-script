package pub.carzy.auto_script.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.carzy.auto_script.entity.EventContent;
import pub.carzy.auto_script.entity.EventDevice;
import pub.carzy.auto_script.entity.InputProp;
import pub.carzy.auto_script.entity.events.AbsEventContent;
import pub.carzy.auto_script.entity.events.KeyEventContent;
import pub.carzy.auto_script.entity.events.MscEventContent;
import pub.carzy.auto_script.entity.events.RelEventContent;
import pub.carzy.auto_script.entity.events.SwEventContent;

/**
 * @author admin
 */
public class EventDeviceUtil {
    public static final Pattern EVENT_LINE = Pattern.compile("(\\w+)\\s*\\((\\p{XDigit}+?)\\)\\s*:\\s*(.*)");
    public static final Pattern ABS_PROP = Pattern.compile("\\d+");
    public static final Map<String, Function<String, List<EventContent>>> EVENT_MAP;

    static {
        EVENT_MAP = new LinkedHashMap<>();
        EVENT_MAP.put(KeyEventContent.KEY, s -> {
            if (s == null) {
                return null;
            }
            return Collections.singletonList(new KeyEventContent(s.trim().split("\\s+")));
        });
        EVENT_MAP.put(AbsEventContent.KEY, s -> {
            if (s == null) {
                return null;
            }
            String[] split = s.split("\\n");
            List<EventContent> list = new ArrayList<>(split.length);
            loop:
            for (String line : split) {
                line = line.trim();
                if (StringUtils.isEmpty(line)) {
                    continue;
                }
                //ABS_Z : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
                String[] info = line.split(":");
                if (info.length != 2) {
                    continue;
                }
                String name = info[0].trim();
                String[] items = info[1].split(",");
                if (items.length != 6) {
                    continue;
                }
                try {
                    List<Integer> values = new ArrayList<>();
                    for (String item : items) {
                        Matcher matcher = ABS_PROP.matcher(item);
                        if (!matcher.find()) {
                            continue loop;
                        }
                        values.add(Integer.parseInt(matcher.group()));
                    }
                    list.add(new AbsEventContent(name, values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5)));
                } catch (Exception ignored) {
                }
            }
            return list;
        });
        EVENT_MAP.put(MscEventContent.KEY, s -> {
            if (s == null) {
                return null;
            }
            return Collections.singletonList(new MscEventContent(s.trim().split("\\s+")));
        });
        EVENT_MAP.put(RelEventContent.KEY, s -> {
            if (s == null) {
                return null;
            }
            return Collections.singletonList(new RelEventContent(s.trim().split("\\s+")));
        });
        EVENT_MAP.put(SwEventContent.KEY, s -> {
            if (s == null) {
                return null;
            }
            return Collections.singletonList(new SwEventContent(s.trim().split("\\s+")));
        });
    }

    public static List<EventDevice> parse(String content) {
        List<EventDevice> devices = new ArrayList<>();
        if (content != null && !content.isEmpty()) {
            String[] split = content.split("\\n");
            EventDevice device = null;
            for (int i = 0; i < split.length; i++) {
                String line = split[i].trim();
                if (line.startsWith("add device")) {
                    String[] v = line.split(":");
                    if (v.length != 2) {
                        continue;
                    }
                    if (device != null) {
                        devices.add(device);
                    }
                    device = new EventDevice();
                    device.setId(Integer.parseInt(v[0].replaceAll("add\\s+device", "").trim()));
                    device.setPath(v[1].trim());
                    continue;
                }
                if (device == null) {
                    continue;
                }
                if (line.startsWith("name:")) {
                    device.setName(line.replace("name:", "").replace("\"", "").trim());
                }// 开始事件部分
                else if (line.startsWith("events:")) {
                    continue;
                } else if (line.startsWith("input props:")) {
                    StringBuilder builder = new StringBuilder(line.replace("input props:", "").trim());
                    String item;
                    //查找多行
                    while (i < split.length - 1 && !(EVENT_LINE.matcher(item = split[i + 1].trim()).find()
                            || item.startsWith("add device"))) {
                        i++;
                        builder.append('\n').append(item);
                    }
                    String propsLine = builder.toString().trim();
                    if (!StringUtils.isEmpty(propsLine) && !"<none>".equals(propsLine)) {
                        String[] props = propsLine.split("\\s+");
                        for (String p : props) {
                            device.getInputProps().add(new InputProp(p));
                        }
                    }
                } else if (EVENT_LINE.matcher(line).find()) {
                    // 示例：KEY (0001): KEY_UP KEY_LEFT KEY_RIGHT
                    String type = line.substring(0, line.indexOf("(")).trim();
                    String code = line.substring(line.indexOf(")"));
                    String[] parts = new String[]{line.substring(0, line.indexOf(":")), line.substring(line.indexOf(":") + 1)};
                    StringBuilder builder = new StringBuilder(parts[1]);
                    String item;
                    //查找多行
                    while (i < split.length - 1 && !(EVENT_LINE.matcher(item = split[i + 1].trim()).find()
                            || item.startsWith("input props:") || item.startsWith("add device"))) {
                        i++;
                        builder.append('\n').append(item);
                    }
                    String valueString = builder.toString();
                    Function<String, List<EventContent>> function = EVENT_MAP.get(type);
                    if (function != null) {
                        List<EventContent> list = function.apply(valueString);
                        if (list != null) {
                            device.getEvents().addAll(list);
                        }
                    }
                }
            }
            if (device != null) {
                devices.add(device);
            }
        }
        return devices;
    }

    public static EventDevice findGestureActuator(List<EventDevice> list) {
        for (EventDevice device : list) {
            if (device.getEvents() != null) {
                boolean[] marks = new boolean[]{false, false, false, false};
                for (EventContent content : device.getEvents()) {
                    if (content instanceof AbsEventContent) {
                        AbsEventContent absEventContent = (AbsEventContent) content;
                        if ("ABS_MT_SLOT".equals(absEventContent.getName())) {
                            marks[0] = true;
                        } else if ("ABS_MT_POSITION_X".equals(absEventContent.getName())) {
                            marks[1] = true;
                        } else if ("ABS_MT_POSITION_Y".equals(absEventContent.getName())) {
                            marks[2] = true;
                        } else if ("ABS_MT_TRACKING_ID".equals(absEventContent.getName())) {
                            marks[3] = true;
                        }
                    }
                }
                if (marks[0] && marks[1] && marks[2] && marks[3]) {
                    return device;
                }
            }
        }
        return null;
    }

    public static EventDevice findKeyActuator(List<EventDevice> list) {
        for (EventDevice device : list) {
            List<EventContent> events = device.getEvents();
            if (events == null || events.isEmpty()) {
                continue;
            }
            boolean hasOther = false;
            boolean isReal = false;
            for (EventContent content : events) {
                if (!(content instanceof KeyEventContent)) {
                    hasOther = true;
                } else {
                    List<String> codes = ((KeyEventContent) content).getCodes();
                    isReal = codes != null &&
                            (codes.contains("KEY_VOLUMEUP")
                                    || codes.contains("KEY_VOLUMEDOWN")
                                    || (codes.contains("KEY_POWER") &&
                                    device.getName().contains("kpd")));
                }
            }
            if (hasOther) {
                continue;
            }
            if (isReal) {
                return device;
            }
        }
        return null;
    }
}

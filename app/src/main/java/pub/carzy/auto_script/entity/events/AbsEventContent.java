package pub.carzy.auto_script.entity.events;

import pub.carzy.auto_script.entity.EventContent;

/**
 * @author admin
 */
public class AbsEventContent extends EventContent {
    public String getName() {
        return name;
    }

    public Integer getValue() {
        return value;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }

    public Integer getFuzz() {
        return fuzz;
    }

    public Integer getFlat() {
        return flat;
    }

    public Integer getResolution() {
        return resolution;
    }

    public static final String KEY = "ABS";
    public static final int CODE = 0x0003;
    @Override
    public String getType() {
        return KEY;
    }

    @Override
    public int getCode() {
        return CODE;
    }
    private final String name;
    private final Integer value;
    private final Integer min;
    private final Integer max;
    private final Integer fuzz;
    private final Integer flat;
    private final Integer resolution;

    public AbsEventContent(String name,Integer value, Integer min, Integer max, Integer fuzz, Integer flat, Integer resolution) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.fuzz = fuzz;
        this.flat = flat;
        this.resolution = resolution;
    }
}

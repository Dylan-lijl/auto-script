package pub.carzy.auto_script.entity.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.carzy.auto_script.entity.EventContent;

/**
 * @author admin
 */
public class MscEventContent extends EventContent {
    public static final String KEY = "MSC";
    public static final int CODE = 0x0004;

    @Override
    public String getType() {
        return KEY;
    }

    @Override
    public int getCode() {
        return CODE;
    }
    private final List<String> codes = new ArrayList<>();

    public MscEventContent(String[] codes) {
        this(Arrays.asList(codes));
    }

    public MscEventContent(List<String> codes) {
        this.codes.addAll(codes);
    }

    public List<String> getCodes() {
        return new ArrayList<>(codes);
    }
}

package pub.carzy.auto_script.entity.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.carzy.auto_script.entity.EventContent;

/**
 * @author admin
 */
public class SwEventContent extends EventContent {
    public static final String KEY = "SW";
    public static final int CODE = 0x0005;

    @Override
    public String getType() {
        return KEY;
    }

    @Override
    public int getCode() {
        return CODE;
    }

    private final List<String> codes = new ArrayList<>();

    public List<String> getCodes() {
        return new ArrayList<>(codes);
    }

    public SwEventContent(List<String> codes) {
        this.codes.addAll(codes);
    }

    public SwEventContent(String[] codes) {
        this.codes.addAll(Arrays.asList(codes));
    }
}

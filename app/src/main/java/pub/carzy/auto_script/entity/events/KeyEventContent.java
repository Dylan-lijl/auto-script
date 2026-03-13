package pub.carzy.auto_script.entity.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.carzy.auto_script.entity.EventContent;

/**
 * @author admin
 */
public class KeyEventContent extends EventContent {
    public static final String KEY = "KEY";
    public static final int CODE = 0x0001;
    @Override
    public String getType() {
        return KEY;
    }

    @Override
    public int getCode() {
        return CODE;
    }
    private final List<String> codes = new ArrayList<>();

    public KeyEventContent(String[] codes) {
        this(Arrays.asList(codes));
    }

    public KeyEventContent(List<String> codes) {
        this.codes.addAll(codes);
    }

    public List<String> getCodes() {
        return new ArrayList<>(codes);
    }
}

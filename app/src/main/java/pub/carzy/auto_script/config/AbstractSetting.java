package pub.carzy.auto_script.config;

import java.util.ArrayList;
import java.util.List;

import pub.carzy.auto_script.entity.FloatPoint;
import pub.carzy.auto_script.entity.Style;

/**
 * @author admin
 */
public abstract class AbstractSetting implements Setting {
    public static class Key {
        protected static final String KEY_ACCEPTED = "disclaimer_accepted";
        protected static final String KEY_TICK = "tick";
        protected static final String KEY_LANGUAGE = "language";
        protected static final String KEY_UUID = "uuid";
        protected static final String FLOAT_POINT = "floatPoint";
        protected static final String SHOW_RECORD_WINDOW = "show_record_window";
        protected static final String AUTO_RUN_RECORD = "auto_run_record";
        protected static final String STYLE = "style_";
        protected static final String AUTO_PLAY = "auto_play";
    }

    public static class DefaultValue {
        public static final int TICK = 10;
        public static final boolean SHOW_RECORD_WINDOW = true;
        public static final Integer AUTO_RUN_RECORD = -1;
    }

    protected abstract <T> void write(String key, T value);

    public AbstractSetting() {

    }

    protected abstract <T> T read(String key, T defaultValue, Class<T> clazz);

    @Override
    public void setAccepted(boolean accepted) {
        write(Key.KEY_ACCEPTED, accepted);
    }

    @Override
    public void setTick(Integer time) {
        write(Key.KEY_TICK, time);
    }

    @Override
    public Integer getTick() {
        return read(Key.KEY_TICK, null, Integer.class);
    }

    @Override
    public boolean isAccepted() {
        return read(Key.KEY_ACCEPTED, false, Boolean.class);
    }

    @Override
    public void setLanguage(String language) {
        write(Key.KEY_LANGUAGE, language);
    }

    @Override
    public String getLanguage() {
        return read(Key.KEY_LANGUAGE, null, String.class);
    }

    @Override
    public void setUUID(String uuid) {
        write(Key.KEY_UUID, uuid);
    }

    @Override
    public String getUUID() {
        return read(Key.KEY_UUID, null, String.class);
    }

    @Override
    public void setPoint(FloatPoint point) {
        write(Key.FLOAT_POINT, point);
    }

    @Override
    public FloatPoint getPoint() {
        return read(Key.FLOAT_POINT, null, FloatPoint.class);
    }

    @Override
    public void setShowRecordWindow(Boolean b) {
        write(Key.SHOW_RECORD_WINDOW, b);
    }

    @Override
    public Boolean getShowRecordWindow() {
        return read(Key.SHOW_RECORD_WINDOW, null, Boolean.class);
    }

    @Override
    public void setAutoRunRecord(Integer delay) {
        write(Key.AUTO_RUN_RECORD, delay);
    }

    @Override
    public Integer getAutoRunRecord() {
        return read(Key.AUTO_RUN_RECORD, null, Integer.class);
    }

    @Override
    public List<Style> getAllStyle() {
        List<Style> styles = new ArrayList<>();
        keys().forEach(item -> {
            if (!item.startsWith(Key.STYLE)) {
                return;
            }
            Style style = read(item, null, Style.class);
            if (style == null) {
                return;
            }
            styles.add(style);
        });
        return styles;
    }

    @Override
    public void updateStyle(Style style) {
        write(Key.STYLE + style.getId(), style);
    }

    @Override
    public Style getStyle(long id) {
        return read(Key.STYLE + id, null, Style.class);
    }

    @Override
    public void removeStyle(long id) {
        write(Key.STYLE + id, null);
    }

    @Override
    public void setAutoPlay(Boolean v) {
        write(Key.AUTO_PLAY, v);
    }

    @Override
    public Boolean getAutoPlay() {
        return read(Key.AUTO_PLAY, true, Boolean.class);
    }
}

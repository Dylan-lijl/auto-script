package pub.carzy.auto_script.config;

import java.util.UUID;

import pub.carzy.auto_script.entity.FloatPoint;

/**
 * @author admin
 */
public abstract class AbstractSetting implements Setting {
    protected static final String KEY_ACCEPTED = "disclaimer_accepted";
    protected static final String KEY_TICK = "tick";
    protected static final String KEY_LANGUAGE = "language";
    protected static final String KEY_UUID = "uuid";
    protected static final String FLOAT_POINT = "floatPoint";

    protected abstract <T> void write(String key, T value);

    public AbstractSetting() {

    }

    protected abstract <T> T read(String key, T defaultValue, Class<T> clazz);

    @Override
    public void setAccepted(boolean accepted) {
        write(KEY_ACCEPTED, accepted);
    }

    @Override
    public void setTick(Integer time) {
        write(KEY_TICK, time);
    }

    @Override
    public Integer getTick() {
        return read(KEY_TICK, 10, Integer.class);
    }

    @Override
    public boolean isAccepted() {
        return read(KEY_ACCEPTED, false, Boolean.class);
    }

    @Override
    public void setLanguage(String language) {
        write(KEY_LANGUAGE, language);
    }

    @Override
    public String getLanguage() {
        return read(KEY_LANGUAGE, null, String.class);
    }

    @Override
    public void setUUID(String uuid) {
        write(KEY_UUID, uuid);
    }

    @Override
    public String getUUID() {
        return read(KEY_UUID, null, String.class);
    }

    @Override
    public void setPoint(FloatPoint point) {
        write(FLOAT_POINT, point);
    }

    @Override
    public FloatPoint getPoint() {
        return read(FLOAT_POINT, null, FloatPoint.class);
    }


}

package pub.carzy.auto_script.config;

public abstract class AbstractSetting implements Setting {
    protected static final String KEY_ACCEPTED = "disclaimer_accepted";
    protected static final String KEY_TICK = "tick";

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
        return read(KEY_TICK, 15,Integer.class);
    }

    @Override
    public boolean isAccepted() {
        return read(KEY_ACCEPTED, false,Boolean.class);
    }
}

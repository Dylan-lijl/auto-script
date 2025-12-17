package pub.carzy.auto_script.utils;

import androidx.annotation.NonNull;

/**
 * @author admin
 */
public class Option<V> {
    private String key;
    private V value;

    public Option(String key, V value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @NonNull
    @Override
    public String toString() {
        return key;
    }
}

package pub.carzy.auto_script.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.MapChangeRegistry;
import androidx.databinding.ObservableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author admin
 */
public class ObservableLinkedHashMap<K, V> implements ObservableMap<K, V> {
    private final LinkedHashMap<K, V> backing = new LinkedHashMap<>();
    private transient MapChangeRegistry mCallbacks = new MapChangeRegistry();

    @Override
    public void addOnMapChangedCallback(OnMapChangedCallback<? extends ObservableMap<K, V>, K, V> callback) {
        mCallbacks.add(callback);
    }

    @Override
    public void removeOnMapChangedCallback(OnMapChangedCallback<? extends ObservableMap<K, V>, K, V> callback) {
        mCallbacks.remove(callback);
    }

    private void notifyChange(K key, V value) {
        mCallbacks.notifyChange(this, key);
    }

    /* --------- Map Methods Delegation --------- */

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return backing.containsValue(value);
    }

    @Nullable
    @Override
    public V get(@Nullable Object key) {
        return backing.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        V prev = backing.put(key, value);
        notifyChange(key, value);
        return prev;
    }

    @Nullable
    @Override
    public V remove(@Nullable Object key) {
        V prev = backing.remove(key);
        notifyChange((K) key, prev);
        return prev;
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            backing.put(e.getKey(), e.getValue());
            notifyChange(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        if (!backing.isEmpty()) {
            backing.clear();
            notifyChange(null, null);
        }
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(backing.keySet());
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(backing.values());
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(backing.entrySet());
    }

    /* ----------- Ordered Access Extensions ----------- */

    public K keyAt(int index) {
        if (index < 0 || index >= backing.size()) throw new IndexOutOfBoundsException();
        return new ArrayList<>(backing.keySet()).get(index);
    }

    public V valueAt(int index) {
        return backing.get(keyAt(index));
    }

    public int indexOfKey(K key) {
        int i = 0;
        for (K k : backing.keySet()) {
            if (Objects.equals(k, key)) return i;
            i++;
        }
        return -1;
    }

    public V removeAt(int index) {
        K k = keyAt(index);
        return remove(k);
    }

    public V setValueAt(int index, V value) {
        K k = keyAt(index);
        return put(k, value);
    }

    /* -------- equals & hashCode -------- */

    @Override
    public boolean equals(@Nullable Object o) {
        return backing.equals(o);
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }
}


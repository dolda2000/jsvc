package dolda.jsvc;

import java.util.*;

public interface MultiMap<K, V> extends Map<K, V> {
    public Collection<V> values(K key);
    public void add(K key, V value);
    public Collection<V> putValues(K key, Collection<V> values);
}

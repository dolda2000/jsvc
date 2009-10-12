package dolda.jsvc.util;

import dolda.jsvc.MultiMap;
import java.util.*;

public class WrappedMultiMap<K, V> implements MultiMap<K, V> {
    private Map<K, Collection<V>> bk;
    private EntrySet entryset;
    private Values values;
    
    public WrappedMultiMap(Map<K, Collection<V>> bk) {
	this.bk = bk;
    }
    
    private V get1(Collection<V> vs) {
	if(vs == null)
	    return(null);
	Iterator<V> i = vs.iterator();
	if(!i.hasNext())
	    return(null);
	return(i.next());
    }

    public void clear() {
	modified();
	bk.clear();
    }
    
    public boolean containsKey(Object key) {
	Collection<V> vs = bk.get(key);
	if(vs == null)
	    return(false);
	return(!vs.isEmpty());
    }
    
    public boolean equals(Object o) {
	return(bk.equals(o));
    }
    
    public V get(Object key) {
	return(get1(bk.get(key)));
    }
    
    public Collection<V> values(K key) {
	Collection<V> vs = bk.get(key);
	if(vs == null) {
	    vs = new LinkedList<V>();
	    bk.put(key, vs);
	}
	return(vs);
    }
    
    public int hashCode() {
	return(bk.hashCode());
    }
    
    public boolean isEmpty() {
	return(values().isEmpty());
    }
    
    public Set<K> keySet() {
	return(bk.keySet());
    }
    
    public V put(K key, V value) {
	Collection<V> vs = new LinkedList<V>();
	vs.add(value);
	modified();
	return(get1(bk.put(key, vs)));
    }
    
    public void add(K key, V value) {
	modified();
	values(key).add(value);
    }
    
    public Collection<V> putValues(K key, Collection<V> values) {
	modified();
	return(bk.put(key, values));
    }
    
    private class DumbEntry implements Map.Entry<K, V> {
	private K key;
	private V value;
	
	public DumbEntry(K key, V value) {
	    this.key = key;
	    this.value = value;
	}
	
	public boolean equals(Object o) {
	    if(!(o instanceof Map.Entry))
		return(false);
	    Map.Entry oe = (Map.Entry)o;
	    return(((key == null)?(oe.getKey() == null):key.equals(oe.getKey())) &&
		   ((value == null)?(oe.getValue() == null):value.equals(oe.getValue())));
	}
	
	public K getKey() {
	    return(key);
	}
	
	public V getValue() {
	    return(value);
	}
	
	public int hashCode() {
	    return(key.hashCode() + value.hashCode());
	}
	
	public V setValue(V value) {
	    throw(new UnsupportedOperationException());
	}
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
	public Iterator<Map.Entry<K, V>> iterator() {
	    return(new Iterator<Map.Entry<K, V>>() {
		    private Iterator<Map.Entry<K, Collection<V>>> bki = bk.entrySet().iterator();
		    private K curkey;
		    private Iterator<V> vsi = null;
		    
		    public boolean hasNext() {
			if((vsi != null) && vsi.hasNext())
			    return(true);
			return(bki.hasNext());
		    }
		    
		    public Map.Entry<K, V> next() {
			if((vsi == null) || !vsi.hasNext()) {
			    Map.Entry<K, Collection<V>> ne = bki.next();
			    curkey = ne.getKey();
			    vsi = ne.getValue().iterator();
			}
			return(new DumbEntry(curkey, vsi.next()));
		    }
		    
		    public void remove() {
			modified();
			vsi.remove();
		    }
		});
	}
	
	public int size() {
	    return(WrappedMultiMap.this.size());
	}
	
	public boolean remove(Object o) {
	    modified();
	    return(WrappedMultiMap.this.remove(o) != null);
	}
	
	public void clear() {
	    modified();
	    bk.clear();
	}
    }

    private class Values extends AbstractCollection<V> {
	public Iterator<V> iterator() {
	    return(new Iterator<V>() {
		    Iterator<Map.Entry<K, V>> bki = WrappedMultiMap.this.entrySet().iterator();
		    
		    public boolean hasNext() {
			return(bki.hasNext());
		    }

		    public V next() {
			return(bki.next().getValue());
		    }
		    
		    public void remove() {
			modified();
			bki.remove();
		    }
		});
	}
	
	public int size() {
	    return(WrappedMultiMap.this.size());
	}
	
	public boolean contains(Object o) {
	    return(containsValue(o));
	}
	
	public void clear() {
	    modified();
	    bk.clear();
	}
    }

    public Set<Map.Entry<K, V>> entrySet() {
	if(entryset == null)
	    entryset = new EntrySet();
	return(entryset);
    }
    
    public Collection<V> values() {
	if(values == null)
	    values = new Values();
	return(values);
    }
    
    public void putAll(Map<? extends K, ? extends V> m) {
	modified();
	for(Map.Entry<? extends K, ? extends V> e : m.entrySet())
	    add(e.getKey(), e.getValue());
    }
    
    public V remove(Object key) {
	modified();
	return(get1(bk.remove(key)));
    }
    
    public boolean containsValue(Object value) {
	for(Collection<V> vs : bk.values()) {
	    if(vs.contains(value))
		return(true);
	}
	return(false);
    }
    
    public int size() {
	int i = 0;
	for(Collection<V> vs : bk.values())
	    i += vs.size();
	return(i);
    }
    
    protected void modified() {}
    
    public String toString() {
	StringBuilder buf = new StringBuilder();
	buf.append("{\n");
	for(Map.Entry<K, V> e : entrySet()) {
	    buf.append("\t");
	    buf.append(e.getKey().toString());
	    buf.append(" = \"");
	    buf.append(e.getValue().toString());
	    buf.append("\",\n");
	}
	buf.append("}\n");
	return(buf.toString());
    }
}

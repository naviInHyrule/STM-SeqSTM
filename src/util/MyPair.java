package util;

/**
 * 
 * @author Lan Du
 *
 * @param <K>
 * @param <V>
 */
public class MyPair<K, V> {

    private K key;
    private V value;

    public MyPair(K key, V value) {
        this.key = key;
        this.value = value;
    }
    
    public K key(){
    	return key;
    }
    
    public V value(){
    	return value;
    }
    
    public String toString(){
    	return key+":"+value;
    }
}

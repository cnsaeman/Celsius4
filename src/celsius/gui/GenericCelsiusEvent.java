package celsius.gui;

/**
 * Class describing a generic event in Celsius.
 * 
 * @author cnsaeman
 */
public class GenericCelsiusEvent {
    
    public final int type;
    public final Object source;
    
    public GenericCelsiusEvent(Object s, int t) {
        type=t;
        source=s;
    }
}

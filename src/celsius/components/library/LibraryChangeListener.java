package celsius.components.library;

/**
 * Listener defining the method for being notified of a Library change
 * @author cnsaeman
 */
public interface LibraryChangeListener {

    public void libraryElementChanged(String type,String id);
    
}

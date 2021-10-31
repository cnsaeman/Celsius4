/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.gui;

/**
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

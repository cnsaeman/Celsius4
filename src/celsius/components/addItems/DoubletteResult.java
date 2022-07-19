/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.addItems;

import celsius.data.Item;

/**
 *
 * @author cnsaeman
 */
public class DoubletteResult {

    public int type; // 100 direct match of unique fields, 
    public Item item;
    public String message;
    
    public DoubletteResult(int type, Item item, String message) {
        this.type=type;
        this.item=item;
        this.message=message;
    }
    
}

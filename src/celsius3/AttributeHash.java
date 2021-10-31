/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.io.Serializable;

/**
 *
 * @author cnsaeman
 */
public class AttributeHash {
    
    public boolean modified;
    public HashMap<String,String> hash;
    public String table;
    public String id;
    
    public AttributeHash(byte[] attBytes) throws IOException, ClassNotFoundException {
        modified=false;
        if ((attBytes!=null) && (attBytes.length>2)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(attBytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            ois.close();
            bais.close();
            hash=((HashMap) ois.readObject());
        } else {
            hash=new HashMap<>();
        }
    }

    public AttributeHash() {
        modified=false;
        hash=new HashMap<>();
    }

    public Iterable<String> keySet() {
        return(hash.keySet());
    }

    public String get(String key) {
        return(hash.get(key));
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(hash);
        return(baos.toByteArray());
    }

    public void put(String key, String value) {
       if (!hash.containsKey(key) || !hash.get(key).equals(value)) {
            hash.put(key,value);
           modified=true;
       }
    }

    public void appendAttributes(String key, String value) {
        modified=true;
        String content=hash.get(key);
        if (content==null) {
            content="";
        } else {
            if (!content.isBlank()) {
                content+="\n";
            } 
        }
        content+=value;
        hash.put(key, content);
    }
    
}

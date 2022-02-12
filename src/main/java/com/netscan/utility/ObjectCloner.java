package com.netscan.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ObjectCloner -- Cloner di oggetti basato su stream
 *
 * @author Alex Della Bruna
 */
public class ObjectCloner {

    private ObjectCloner() {
    }

    /**
     * Ritorna la deep copy di un oggetto
     *
     * @param oldObj Oggeto da clonare
     * @return Nuovo oggetto clonato
     * @throws IOException Ritorna IOException nel caso di errori nell'utilizzo
     *                     degli stream
     */
    static public Object deepCopy(Object oldObj) throws IOException {
        Object res = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(oldObj);
            oos.flush();

            ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
            ois = new ObjectInputStream(bin);
            res = ois.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("CLASS NOT FOUND IN OBJECT CLONER = " + e);
            System.exit(-1);
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return res;
    }

    /**
     * Esegue deep copy e controlla eccezioni
     *
     * @param obj Oggeto da clonare
     * @return Nuovo oggeto clonato
     */
    public static Object objDeepCopy(Object obj) {
        if (obj == null) {
            throw new NullPointerException("a null pointer was given as parameter");
        }
        Object res = null;
        try {
            res = deepCopy(obj);
        } catch (IOException e) {
            System.err.println("IOException while trying to clone the object " + e);
            System.exit(-1);
        }
        return res;
    }
}

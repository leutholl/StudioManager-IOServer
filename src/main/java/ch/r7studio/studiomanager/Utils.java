/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

/**
 *
 * @author leutholl
 */
public class Utils {
    
    public static String betweenRundeKlammerInDbString(String dbString) {
        return dbString.substring(dbString.indexOf("(") + 1, dbString.indexOf(")"));
    }
    
    public static String betweenEckigeKlammerInDbString(String dbString) {      
        if (!dbString.contains("[")) {
            return "";
        }
        return dbString.substring(dbString.indexOf("[") + 1, dbString.indexOf("]"));
    }
    
    public static String rightFromEquals(String dbString) {
        return dbString.substring(dbString.indexOf("=") + 1);
    }
    
     private String getIPByte(String ip, int bytePos) {
        String seg = (ip.split("\\."))[bytePos-1];
        Integer val = Integer.parseInt(seg);
        byte b = (byte)(val.intValue() & 0xFF);
        byte[] bytes = { b };
        String res = new String(bytes);
        //logger.debug("IP:"+ip+"["+bytePos+"]="+res+" ["+val+"]");
        return res;
    }
    
}

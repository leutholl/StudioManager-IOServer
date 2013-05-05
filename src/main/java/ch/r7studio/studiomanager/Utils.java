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
    
}

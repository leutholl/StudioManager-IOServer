/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    public static String getIPByte(String ip, int bytePos) {
        String seg = (ip.split("\\."))[bytePos - 1];
        Integer val = Integer.parseInt(seg);
        byte b = (byte) (val.intValue() & 0xFF);
        byte[] bytes = {b};
        String res = new String(bytes);
        //logger.debug("IP:"+ip+"["+bytePos+"]="+res+" ["+val+"]");
        return res;
    }

    public static String getUnsignedByte(int val) {
        byte b = (byte) (val & 0XFF);
        byte[] bytes = {b};
        return new String(bytes);
    }

    public static String getByteAsHex(int val) {
        return String.format("%02X", val);
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
}

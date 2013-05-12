/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

/**
 *
 * @author leutholl
 * http://ww1.microchip.com/downloads/en/DeviceDoc/21919e.pdf
 */
public class MCP23008 {
    
    private static final String IODIR = "00";
    private static final String OLAT  = "0A";
    
    
    public static String toOut(String i2c_HEX_address, int dbyte) {
        return "S"+i2c_HEX_address+OLAT+Utils.getByteAsHex(dbyte)+"P";
    }
    
    public static String init(String i2c_HEX_address) {
        //set all pins as output
        return "S"+i2c_HEX_address+IODIR+"00"+"P";
    }
    
    
}

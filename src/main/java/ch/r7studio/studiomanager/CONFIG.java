/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

/**
 *
 * @author leutholl
 */
public abstract class CONFIG {
    
    //DaeNetIP Agent settings
    public final static String SNMP_COMMUNITY = "private";
    public final static int    SNMP_PORT      =  16100;
    
    //Modtronix Agent settings
    public final static long   LCD_SLEEP      =  35L; //ms to wait after UDP to LCD
    public final static String BASE_URI       = "http://127.0.0.1:8081/rest"; //REST address
    
    //DMX4ALL Agent settings
    public final static String DMX_IP         = "192.168.1.185";
    public final static int    DMX_PORT       = 10001;
    public final static long   DMX_SLEEP      = 100L; //ms to wait after UDP to DMX
    public final static int    DMX_UDP_TIMEOUT= 2000; //2sec
    
}

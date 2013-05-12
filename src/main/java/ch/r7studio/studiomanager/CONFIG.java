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
    
    //Snmp Generic Agent settings
    public final static String SNMP_COMMUNITY = "private";
    public final static int    SNMP_PORT      =  161;
    
    //Modtronix Agent settings
    public final static long   LCD_SLEEP      =  35L; //ms to wait after UDP to LCD
    public final static String BASE_URI       = "http://127.0.0.1:8081/rest"; //REST address
    
    //DMX4ALL Agent settings
    public final static String DMX_IP         = "192.168.1.185";
    public final static int    DMX_PORT       = 10001;
    public final static long   DMX_SLEEP      = 100L; //ms to wait after UDP to DMX
    public final static int    DMX_UDP_TIMEOUT= 2000; //2sec
    
    //HD44780 Agent settings
    // http://sprut.de/electronic/lcd/index.htm
    // lcd model:
    public final static int     LCD_DISPLAY_ROWS = 2;
    public final static int     LCD_DISPLAY_LENGTH = 16;
    public final static int     LCD_MAX_DISPLAY_LENGTH = 40;
    // interface mode:
    public final static int     LCD_DISPLAY_MODE = 0x28; //4-bit-interface, 2 rows, 5*7 fonts
    // initial config
    public final static int     LCD_CURSOR_MODE = 0x0C; //LCD on, Cursor off, blink off
    public final static int     LCD_CURSOR_HOME = 0x02;
    public final static int     LCD_CURSOR_SCROLL_RIGHT = 0x14;
    public final static int     LCD_DISPLAY_CLEAR = 0x01;
    // pin mapping from GPIO chip to HD44780
    public final static int     LCD_SOFT_RESET = 0x03 << 3;
    public final static int     LCD_FOUR_BIT_MODE = 0x02 << 3;
    public final static int     LCD_RS_PIN = 0x02;
    // private final static int LCD_RW_PIN = 0x20; //not needed
    public final static int     LCD_ENABLE_PIN = 0x04;
    public final static int     LCD_BACKLIGHT_PIN = 0x80;
    // timing
    public final static int     LCD_WAIT1 = 500; //500ms wait for init
    public final static int     LCD_WAIT2 = 50; //50ms wait for init
    public final static int     LCD_WAIT3 = 2;  //2ms wait
    //print line separator
    public final static String  LCD_CRLF = "\\n";
    
    //DAENetIp1 Agent settings
    public final static String  DAENET_COMMUNITY = "private";
    public final static int     DAENET_SET_GET_PORT      =  16100;
    public final static int     DAENET_TRAP_PORT         =  16100;
    public final static String  DAENET_OID_TEMP  = ".1.3.6.1.4.1.32111.1.3.4.10";
    
    public static final String[] DANET_OID_RELAYS={
        ".1.3.6.1.4.1.32111.1.1.2.1", //RELAY 1
        ".1.3.6.1.4.1.32111.1.1.2.2", //RELAY 2
        ".1.3.6.1.4.1.32111.1.1.2.3", //RELAY 3
        ".1.3.6.1.4.1.32111.1.1.2.4", //RELAY 4
        ".1.3.6.1.4.1.32111.1.1.2.5", //RELAY 5
        ".1.3.6.1.4.1.32111.1.1.2.6", //RELAY 6
        ".1.3.6.1.4.1.32111.1.1.2.7", //RELAY 7
        ".1.3.6.1.4.1.32111.1.1.2.8", //RELAY 8
         //new bank according to DAENetIP1 MIB
        ".1.3.6.1.4.1.32111.1.4.2.1", //RELAY 9
        ".1.3.6.1.4.1.32111.1.4.2.2", //RELAY 10
        ".1.3.6.1.4.1.32111.1.4.2.3", //RELAY 11
        ".1.3.6.1.4.1.32111.1.4.2.4", //RELAY 12
    };
    
    //TODO replace with correct OID
    //assure that GPIO port is configured as output
    public static final String[] DANET_OID_DIGITALOUTS={
        ".1.3.6.1.4.1.32111.1.1.2.1", //DigiOut 1
        ".1.3.6.1.4.1.32111.1.1.2.2", //DigiOut 2
        ".1.3.6.1.4.1.32111.1.1.2.3", //DigiOut 3
        ".1.3.6.1.4.1.32111.1.1.2.4", //DigiOut 4
        ".1.3.6.1.4.1.32111.1.1.2.5", //DigiOut 5
        ".1.3.6.1.4.1.32111.1.1.2.6", //DigiOut 6
        ".1.3.6.1.4.1.32111.1.1.2.7", //DigiOut 7
        ".1.3.6.1.4.1.32111.1.1.2.8", //DigiOut 8
     };

    
  

    
}

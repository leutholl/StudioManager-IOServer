/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.actions.HD44780Action;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author leutholl
 * http://www.elektronik.nmp24.de/downloads/LCD.h
 */
public class HD44780Agent implements I_OutAgent {
    
    public List<TriggerListener> listeners = new ArrayList<TriggerListener>();
    
    private static Logger logger = Logger.getLogger(HD44780Agent.class);
    private static I2CAgent i2c;
    
    //CONSTANTS FOR HD44780
    private final static int ON  = 1;
    private final static int OFF = 0;
    
    private final static int HIGH_ZERO_MASK = 0x0f;
    
   
    
    private int backlight_status = OFF;
    
    private String I2C_ADDR_LCD;
    
     
    public HD44780Agent(String board_ip, int udp_port, String i2c_HEX_address) {
        super();
        this.I2C_ADDR_LCD = i2c_HEX_address;
        try {
            i2c = new I2CAgent(InetAddress.getByName(board_ip), udp_port);
            //init MCP23008
            i2c.send(MCP23008.init(i2c_HEX_address));
        } catch (UnknownHostException ex) {
            logger.warn(ex);
        }
        
    }
    
    public void initLCD() {
        try {
            Thread.sleep(10);
            reset();
            
            //init
            Thread.sleep(CONFIG.LCD_WAIT1);
            for (int i=0; i<4; i++) {
                //3x soft-reset -> see data sheet
                i2c.send(MCP23008.toOut(I2C_ADDR_LCD, CONFIG.LCD_ENABLE_PIN | CONFIG.LCD_SOFT_RESET));
                Thread.sleep(CONFIG.LCD_WAIT2);
                i2c.send(MCP23008.toOut(I2C_ADDR_LCD, CONFIG.LCD_SOFT_RESET));
            }
            
            //first write attempt after reset is a 8bit access.
            //we switch to 4bits
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, CONFIG.LCD_FOUR_BIT_MODE | CONFIG.LCD_ENABLE_PIN)); //Enable -> On
            Thread.sleep(CONFIG.LCD_WAIT2);
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, CONFIG.LCD_FOUR_BIT_MODE)); //Enable -> Off
            Thread.sleep(CONFIG.LCD_WAIT2);
            
            //use the 4bit mode form here
            sendcommand(CONFIG.LCD_DISPLAY_MODE);
            Thread.sleep(CONFIG.LCD_WAIT2);
            sendcommand(CONFIG.LCD_CURSOR_MODE);
            Thread.sleep(CONFIG.LCD_WAIT2);
            lightOn();
        } catch (InterruptedException ex) {
            logger.warn(ex);
        }
        
    }
    
    public void clear() {
            sendcommand(CONFIG.LCD_DISPLAY_CLEAR);
    }
    
    public void cursorhome() {
            sendcommand(CONFIG.LCD_CURSOR_HOME);       
    }
    
    public void sendcommand(int command) {
        try {
            int high_nibble;
            int low_nibble;
            int send_data;
            Thread.sleep(CONFIG.LCD_WAIT3+CONFIG.LCD_WAIT3);
            high_nibble = command >> 4;
            high_nibble = high_nibble << 3;
            //high_nibble = high_nibble & HIGH_ZERO_MASK;
            low_nibble  = command & HIGH_ZERO_MASK;
            low_nibble  = low_nibble << 3;
            if (backlight_status == OFF) {
                send_data = high_nibble | CONFIG.LCD_ENABLE_PIN;
            } else {
                send_data = high_nibble | CONFIG.LCD_ENABLE_PIN | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
            if (backlight_status == OFF) {
                send_data = high_nibble;
            } else {
                send_data = high_nibble | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
            if (backlight_status == OFF) {
                send_data = low_nibble | CONFIG.LCD_ENABLE_PIN;
            } else {
                send_data = low_nibble | CONFIG.LCD_ENABLE_PIN | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
            if (backlight_status == OFF) {
                send_data = low_nibble | CONFIG.LCD_RS_PIN;
            } else {
                send_data = low_nibble | CONFIG.LCD_RS_PIN | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
        } catch (InterruptedException ex) {
             logger.warn(ex);
        }
        
    }
    
    public void sendchar(char c) {
        try {
            int high_nibble;
            int low_nibble;
            int send_data;
            
            high_nibble = c >> 4;
            high_nibble = high_nibble << 3;
            //high_nibble = high_nibble & HIGH_ZERO_MASK;
            low_nibble  = c & HIGH_ZERO_MASK;
            low_nibble  = low_nibble << 3;
            if (backlight_status == OFF) {
                send_data = high_nibble | CONFIG.LCD_ENABLE_PIN | CONFIG.LCD_RS_PIN;
            } else {
                send_data = high_nibble | CONFIG.LCD_ENABLE_PIN | CONFIG.LCD_RS_PIN | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
            if (backlight_status == OFF) {
                send_data = high_nibble | CONFIG.LCD_RS_PIN;
            } else {
                send_data = high_nibble | CONFIG.LCD_RS_PIN | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
            if (backlight_status == OFF) {
                send_data = low_nibble | CONFIG.LCD_ENABLE_PIN | CONFIG.LCD_RS_PIN;
            } else {
                send_data = low_nibble | CONFIG.LCD_ENABLE_PIN | CONFIG.LCD_RS_PIN | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
            if (backlight_status == OFF) {
                send_data = low_nibble | CONFIG.LCD_RS_PIN;
            } else {
                send_data = low_nibble | CONFIG.LCD_RS_PIN | CONFIG.LCD_BACKLIGHT_PIN;
            }
            i2c.send(MCP23008.toOut(I2C_ADDR_LCD, send_data));
            Thread.sleep(CONFIG.LCD_WAIT3);
        } catch (InterruptedException ex) {
             logger.warn(ex);
        }
    }
    
    public void print(String s) {
        int length = s.length();
        char[] ascii_chars = new char[length];
        for (int j=0;j < length; j++) {
            ascii_chars[j] = s.charAt(j);
            
            //german character support!
            switch (ascii_chars[j]) {
                case 'ä' : ascii_chars[j] = 225; break;
                case 'ö' : ascii_chars[j] = 239; break;
                case 'ü' : ascii_chars[j] = 245; break;
                case 'Ä' : ascii_chars[j] = 225; break;
                case 'Ö' : ascii_chars[j] = 239; break;
                case 'Ü' : ascii_chars[j] = 245; break;
                case 'ß' : ascii_chars[j] = 226; break;               
            }
        }
        
        if (length > CONFIG.LCD_DISPLAY_LENGTH) {
            length = CONFIG.LCD_DISPLAY_LENGTH; //Truncate text if too long for LCD
        }
        
        for (int i=0; i<length; i++) {
            sendchar(ascii_chars[i]);
        }
    }
    
    public void printAll(String s) {
        int i = 1;
        String[] lines = s.split(CONFIG.LCD_CRLF);
        int j = lines.length;
        for (String line : lines) {
            locate(i,1);
            print(line);
            if (i<j) locate(i+1,1);
            i++;
        }
    }
    
    public void lightOn() {
        backlight_status = ON;
    }
    
    public void lightOff() {
        backlight_status = OFF;
        sendcommand(CONFIG.LCD_DISPLAY_CLEAR);
    }
    
    public void lightToggle() {
        if (backlight_status == ON) {
            lightOff();
        } else lightOn();
    }
    
    public void reset() {
        
    }
    
    
    
    public void locate(int row, int col) {
        cursorhome();
        int i;
        if (row==1) {
            for (i=0;i<col;i++) {
                sendcommand(CONFIG.LCD_CURSOR_SCROLL_RIGHT);
            }
        } else {
            for (i=1;i<(col+CONFIG.LCD_MAX_DISPLAY_LENGTH);i++) {
                sendcommand(CONFIG.LCD_CURSOR_SCROLL_RIGHT);
            }
        }
    }
 
    public boolean doAction(Action action) {
        HD44780Action a = (HD44780Action)action;
        switch (a.getCommandType()) {
            case TEXT_MSG: {
                printAll(a.getCommandValue());
                break;
            }
            case BACKLIGHT_CMD: {
                if (a.getCommandValue().equals("1") || a.getCommandValue().equalsIgnoreCase("ON")) {
                    lightOn();
                }
                if (a.getCommandValue().equals("0") || a.getCommandValue().equalsIgnoreCase("OFF")) {
                    lightOff();
                }
                if (a.getCommandValue().equals("!") || a.getCommandValue().equalsIgnoreCase("~")) {
                    lightToggle();
                }
                break;
            }
        }
        logger.debug("doAction: "+a.getActionString());
        return true;
    }
    
}

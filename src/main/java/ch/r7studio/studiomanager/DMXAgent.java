/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.actions.DMXAction;
import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 *
 * @author leutholl
 */
public class DMXAgent implements I_OutAgent {

    private static Logger logger = Logger.getLogger(DMXAgent.class);
    private static DatagramSocket socket;

    public DMXAgent() {
        try {
            DMXAgent.socket = new DatagramSocket(CONFIG.DMX_PORT);
            DMXAgent.socket.setSoTimeout(CONFIG.DMX_UDP_TIMEOUT); 
        } catch (SocketException ex) {
            logger.warn(ex);
        }

    }
    
    protected boolean sendDMXBlackout(String ip, boolean isBlack) {  
        String data;
        if (isBlack) {
            data = "B1";
        }
        else data = "B0";
        return sendUDPandWaitForACK(ip, data);

    }
    
    protected boolean isDMXBlackOut(String ip) {
        int res = sendUDPandGetValue(ip,"B?",1);
        if (res == 1) {
            return true;
        } else return false;
    }
    
    protected int getDMXValue(String ip, int dmxCh) {
        if (dmxCh > 511) return -1;
        if (dmxCh < 0) return -1;
        return sendUDPandGetValue(ip,"C"+String.format("%03d", dmxCh)+"?",3);
    }
    
    protected boolean setDMXChannelRange(String ip, int channels ) {
        if (channels > 511) return false;
        if (channels < 0) return false;
         return sendUDPandWaitForACK(ip, "N"+String.format("%03d", channels));
    }
    
    protected int getDMXChannelRange(String ip) {
        return sendUDPandGetValue(ip,"N?",3); 
    }

    protected boolean sendDMXValue(String ip, int dmxCh, int dmxVal) {
        if (dmxVal > 255) dmxVal = 255;
        if (dmxVal < 0) dmxVal = 0;
        if (dmxCh > 511) return false;
        if (dmxCh < 0) return false;
        String data = "C" + String.format("%03d", dmxCh) + "L" + String.format("%03d", dmxVal);
        return sendUDPandWaitForACK(ip, data);
         
    }

    protected boolean sendUDPandWaitForACK(String ip, String message) {      
        byte[] bytes = (message+"\r").getBytes();
        boolean result = false;
        
        try {
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
                    InetAddress.getByName(ip), CONFIG.DMX_PORT);

            // Send it
            DMXAgent.socket.send(packet);
            result = getUDPACK();

        } catch (IOException ex) {
            logger.warn(ex);
        } 
        logger.debug("sendUDPandWaitForACK["+ip+"]: "+message+ " = "+result);
        return result;

    }
    
    protected int sendUDPandGetValue(String ip, String message, int expectedDigits) {
        int result = -1;
        byte[] bytes = (message+"\r").getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
                    InetAddress.getByName(ip), CONFIG.DMX_PORT);

            // Send it
            DMXAgent.socket.send(packet);
            result = getUDPResponse(expectedDigits);

        } catch (IOException ex) {
            logger.warn(ex);
        } 
        logger.debug("sendUDPandGetValue["+ip+"]: "+message+ " = "+result);
        return result;

    }
    
    protected int getUDPResponse(int expectedDigits) {
        int result = -1;
        byte[] buffer = new byte[1024];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);  
            try {
                //incoming.setLength(buffer.length);
                socket.receive(incoming);
                String data = "";
                data = new String(incoming.getData());
                data = data.replaceAll("\\p{C}", ""); //remove non-printable characters
                    if (data.endsWith("G")) {
                        logger.debug("DMXAgent: ACK'd");
                        //check if there is an answer
                        if (data.matches(".*\\d.*"))  { //is there a digit?                         
                            result = Integer.parseInt(data.substring(0, expectedDigits+1));
                        }
                    }
                    if (data.isEmpty()) {
                        logger.warn("DMXAgent: Not acknowledged");
                        result=-1;
                    }
                
                //logger.debug("received data: " + data.length() + ":" + data);

            } catch (IOException ex) {
                logger.warn(ex);
                result=-1;
            }
            socket.close();
            
            return result;
    }
    
    protected boolean getUDPACK() {
        boolean result = false;
        byte[] buffer = new byte[1024];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);  
            try {
                //incoming.setLength(buffer.length);
                socket.receive(incoming);
                String data = "";
                data = new String(incoming.getData());
                data = data.replaceAll("\\p{C}", ""); //remove non-printable characters
                    if (data.endsWith("G")) {
                        logger.debug("DMXAgent: ACK'd");
                        result=true;
                        //check if there is an answer
                        if (data.matches(".*\\d.*"))  { //is there a digit?
                            //result with value are always 3 digits and "G"
                            
                        }
                    }
                    if (data.isEmpty()) {
                        logger.warn("DMXAgent: Not acknowledged");
                        result=false;
                    }
                
                //logger.debug("received data: " + data.length() + ":" + data);

            } catch (IOException ex) {
                logger.warn(ex);
                result=false;
            }
            socket.close();
            
            return result;
       
    }

    public boolean doAction(Action a) {
      
        boolean success = false;
        
        DMXAction action = (DMXAction)a;
        //currently only support single address actions
        //TODO Iterate through csv...
        switch (action.getCommandType()) {
            case ABSOLUTE_VALUE: {
                try {
                    Color c = Color.decode(action.getCommandValue());
                    success &= sendDMXValue(action.getHost(),action.getDmxChannelStart(),c.getRed());
                    success &= sendDMXValue(action.getHost(),action.getDmxChannelStart()+1,c.getGreen());
                    success &= sendDMXValue(action.getHost(),action.getDmxChannelStart()+2,c.getBlue());
                } catch (NumberFormatException e) {
                    //if not a RGB format - assume regular integer value
                    success = sendDMXValue(action.getHost(),action.getDmxChannelStart(),Integer.parseInt(action.getCommandValue()));
                }
                break;
            }
            case RELATIVE_VALUE: {
                int val = getDMXValue(action.getHost(),action.getDmxChannelStart());
                int delta = Integer.parseInt(action.getCommandValue());
                if (delta > 0) {
                    success = sendDMXValue(action.getHost(),action.getDmxChannelStart(),val+delta);
                }
                if (delta < 0) {
                    success = sendDMXValue(action.getHost(),action.getDmxChannelStart(),val-delta);
                }
                break;
            }
            case INTELLIGENT: {
                
            }
            case SYSTEM: {
                if (action.getCommandValue().equals("B0")) { //Blackout enable
                    success = sendDMXBlackout(action.getHost(), false);
                    break;
                }
                if (action.getCommandValue().equals("B1")) { //Blackout disable
                    success = sendDMXBlackout(action.getHost(), true);
                    break;
                }
                if (action.getCommandValue().equals("B!")) { //toggle Blackout?
                    success = sendDMXBlackout(action.getHost(), isDMXBlackOut(action.getHost()));
                    break;
                }
                if (action.getCommandValue().startsWith("N")) { //set DMX channels if different                  
                    int channels = Integer.parseInt(action.getCommandValue().substring(1));
                    if (getDMXChannelRange(action.getHost()) != channels) {
                        success = setDMXChannelRange(action.getHost(), channels);
                    }
                    break;
                }
                
                break;
            }
        }
        
        return success;
        
    }

}

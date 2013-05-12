/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

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
public class I2CAgent extends Thread {

    private Logger logger = Logger.getLogger(I2CAgent.class);
    private InetAddress board_ip;
    private int udp_port;
    private boolean listening = false;

    public I2CAgent(InetAddress board_ip, int udp_port) {

            this.board_ip = board_ip;
            this.udp_port = udp_port;
       
    }

    protected void startListener() {
        listening = true;
        this.start();
    }

    protected void stopListener() {
        listening = false;
        
    }

    public void send(String msg) {
        
        logger.debug("I2C message: "+msg);
        
        try {
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket dp = new DatagramPacket(msg.getBytes(), msg.length(),
                    board_ip, udp_port);
            socket.send(dp);
        } catch (SocketException ex) {
            logger.warn(ex);
        } catch (IOException ex) {
            logger.warn(ex);
        }

    }

    @Override
    public void run() {
 
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(udp_port);
            byte[] buffer = new byte[1024];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            while (listening) {
                incoming.setLength(buffer.length);
                socket.receive(incoming);
                byte[] data = incoming.getData();
                System.out.print(new String(data, 0, incoming.getLength()));
            }
            socket.disconnect();
            socket.close();
        } catch (SocketException ex) {
            logger.warn(ex);
        } catch (IOException ex) {
            logger.warn(ex);
        } 

    }
}

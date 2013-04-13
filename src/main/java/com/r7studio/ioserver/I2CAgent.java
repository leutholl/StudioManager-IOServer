/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.r7studio.ioserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *
 * @author leutholl
 */
public class I2CAgent {
    
  private final static int PACKETSIZE = 100 ;

   public static String sendAndRecevieI2C(String host, int port, byte[] message)
   {
      

      DatagramSocket socket = null ;
      String response = "";

      try
      {
         // Convert the arguments first, to ensure that they are valid
         InetAddress address = InetAddress.getByName(host) ;
        
         // Construct the socket
         socket = new DatagramSocket() ;

      
         DatagramPacket packet = new DatagramPacket( message, message.length, address, port ) ;

         // Send it
         socket.send( packet ) ;

         // Set a receive timeout, 2000 milliseconds
         socket.setSoTimeout( 2000 ) ;

         // Prepare the packet for receive
         packet.setData( new byte[PACKETSIZE] ) ;

         // Wait for a response from the server
         socket.receive( packet ) ;

         // Print the response
         
         response = new String(packet.getData());
         
      }
      catch( Exception e )
      {
         System.out.println( e ) ;
      }
      finally
      {
         if( socket != null )
            socket.close() ;
      }
      return response;
   }
}

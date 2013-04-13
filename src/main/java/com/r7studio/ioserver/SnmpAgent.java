/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.r7studio.ioserver;

import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 *
 * @author leutholl
 */
public abstract class SnmpAgent {
    
    private static Logger logger = Logger.getLogger(SnmpAgent.class);

    private SnmpAgent() {
    }

    public static String snmpGet(String ipAddress, String port, String community, String oid) {
        String data = "";
        try {

            // Create TransportMapping and Listen
            TransportMapping transport = new DefaultUdpTransportMapping();
            transport.listen();

            // Create Target Address object
            CommunityTarget comtarget = new CommunityTarget();
            comtarget.setCommunity(new OctetString(community));
            comtarget.setVersion(SnmpConstants.version1);
            comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
            comtarget.setRetries(2);
            comtarget.setTimeout(1000);

            // Create the PDU object
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);
            pdu.setRequestID(new Integer32(1));

            // Create Snmp object for sending data to Agent
            Snmp snmp = new Snmp(transport);

            //System.out.println("Sending Request to Agent...");
            ResponseEvent response = snmp.get(pdu, comtarget);

            // Process Agent Response
            if (response != null) {
                //System.out.println("Got Response from Agent");
                PDU responsePDU = response.getResponse();

                if (responsePDU != null) {
                    int errorStatus = responsePDU.getErrorStatus();
                    int errorIndex = responsePDU.getErrorIndex();
                    String errorStatusText = responsePDU.getErrorStatusText();

                    if (errorStatus == PDU.noError) {                     
                        //System.out.println("Snmp Get Response = " + responsePDU.get(0).toValueString());
                        data = responsePDU.get(0).toValueString();
                    } else {
                        logger.warn("snmpGet: ResponsePDU Error: Status="+errorStatus+" index="+errorIndex+" Text="+errorStatusText);
                    }
                } else {
                    logger.warn("snmpGet: Response PDU is null!");
                    return null;
                }
            } else {
                logger.warn("SNMP agent Timeout...");
                return null;
            }
            snmp.close();
        } catch (IOException ex) {
            logger.error(ex);
        }
        return data;
    }

    public static void snmpSet(String host, String port, String community, String strOID, int Value) {  
        logger.info("snmpSet: host="+host+" port="+port+" community="+community+" OID="+strOID+" value="+Value);
        host = host + "/" + port;
        Address tHost = GenericAddress.parse(host);
        Snmp snmp;
        try {
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(tHost);
            target.setRetries(2);
            target.setTimeout(5000);
            target.setVersion(SnmpConstants.version1); //Set the correct SNMP version here
            PDU pdu = new PDU();
            //Depending on the MIB attribute type, appropriate casting can be done here
            pdu.add(new VariableBinding(new OID(strOID), new Integer32(Value)));
            pdu.setType(PDU.SET);
            ResponseListener listener = new ResponseListener() {
                public void onResponse(ResponseEvent event) {
                    PDU strResponse;
                    String result;
                    ((Snmp) event.getSource()).cancel(event.getRequest(), this);
                    strResponse = event.getResponse();
                    if (strResponse != null) {
                        result = strResponse.getErrorStatusText();
                        logger.warn("SnmpSet Response = null. Set Status is: "+result);
                    }
                }
            };
            snmp.send(pdu, target, null, listener);
            snmp.close();
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    public static void sendTrap(String oid, int value) {
        try {
            // create a protocol data-unit for the snmp-trap
            PDU trap = new PDU();

            trap.setType(PDU.TRAP);

            // add the oid
            trap.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(oid)));
            // add some nice stuff
            trap.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(999999)));

            // Specify receiver (ip 10.1.1.42, port 162)
            //Address targetaddress = new UdpAddress("127.0.0.1/16200");
            Address targetaddress = new UdpAddress("192.168.2.104/16100");
            CommunityTarget target = new CommunityTarget();
            target.setVersion(SnmpConstants.version1);
            target.setAddress(targetaddress);

            // Send the trap
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.send(trap, target, null, null);
        } catch (IOException ex) {
            logger.error(ex);
        }

    }
}

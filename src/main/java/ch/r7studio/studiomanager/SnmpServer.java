/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.triggers.SnmpTrigger;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.apache.log4j.Logger;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

/**
 *
 * @author leutholl
 */
public class SnmpServer implements CommandResponder {

    private static Logger logger = Logger.getLogger(SnmpServer.class);

    private MultiThreadedMessageDispatcher dispatcher;
    private Snmp snmp = null;
    private Address listenAddress;
    private ThreadPool threadPool;
    
    private SnmpAgent snmpAgent = null;

    public boolean init(SnmpAgent snmpAgent) {
        boolean success = true;
        try {
            this.snmpAgent = snmpAgent;
            threadPool = ThreadPool.create("Trap Listener Pool", 2);
            dispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());
            listenAddress = new UdpAddress("localhost/"+CONFIG.DAENET_TRAP_PORT);
            TransportMapping transport;

            transport = new DefaultUdpTransportMapping((UdpAddress) listenAddress);

            snmp = new Snmp(dispatcher, transport);
            snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
            snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
            /*
             * V3 - Stuff
            snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3());
            final USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityModels.getInstance().addSecurityModel(usm);
            final String userName = "admin";
            final String authenticationPwd = "";
            final String privacyPwd = "";
            final byte[] localEngineId = MPv3.createLocalEngineID();

            //Add user to the USM
            snmp.getUSM().addUser(
                    new OctetString(userName),
                    new OctetString(localEngineId),
                    new UsmUser(new OctetString(userName), AuthMD5.ID, new OctetString(authenticationPwd), PrivDES.ID,
                    new OctetString(privacyPwd), new OctetString(localEngineId)));

            */
            snmp.listen();
        } catch (IOException ex) {
            logger.warn(ex);
            success = false;
        }
        return success;
    }

    public boolean run() {
        boolean success = true;
        try {
            snmp.addCommandResponder(this);
        } catch (final Exception ex) {
            logger.warn(ex);
            success = false;
        }
        return success;
    }

    @Override
    public synchronized void processPdu(final CommandResponderEvent respEvnt) {
        if (respEvnt != null && respEvnt.getPDU() != null) {
            logger.info("Got Trap from: "+respEvnt.getPeerAddress().toString());
            final ArrayList<VariableBinding> recVBs = new ArrayList(respEvnt.getPDU().getVariableBindings());
            for (int i = 0; i < recVBs.size(); i++) {
                final VariableBinding recVB = (VariableBinding)recVBs.get(i);
                 logger.debug(recVB.getOid() + " : " + recVB.getVariable());
            }
            //notify SNMPAgent about the trap
            snmpAgent.notifyTrigger(new TriggerEvent(this, new SnmpTrigger(recVBs,respEvnt.getPeerAddress().toString())));

        }
    }

}

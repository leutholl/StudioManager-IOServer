/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.actions.DAENetAction;
import ch.r7studio.studiomanager.actions.E_DAENetChannelType;
import ch.r7studio.studiomanager.actions.SnmpSetAction;
import ch.r7studio.studiomanager.pojo.Daenetip;
import ch.r7studio.studiomanager.triggers.DAENetTrigger;
import ch.r7studio.studiomanager.triggers.SnmpTrigger;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author leutholl
 */
public class DAENetAgent implements I_OutAgent, I_InAgent, TriggerListener {

    public List<TriggerListener> listeners = new ArrayList<TriggerListener>();
    private static Logger logger = Logger.getLogger(DAENetAgent.class);
    private SnmpAgent snmpAgent = new SnmpAgent();

    public DAENetAgent() {
    }

    public int discoverDaeNetIpBoards() {
        //IOBoard List
        List<Daenetip> ioboards = (List<Daenetip>) StudioManagerServer.hib_session.createQuery("from Daenetip").list();
        if (ioboards.isEmpty()) {
            logger.warn("No DAENetBoard registered!");
        } else {
            logger.info("List of DAENetBoards:");
            for (Daenetip io : ioboards) {
                // handle each IOBoard
                logger.info("|-> connecting to IOBoard at: " + io.getIp() + "...");
                String temp = SnmpAgent.snmpGet(io.getIp(), CONFIG.SNMP_PORT, CONFIG.SNMP_COMMUNITY, CONFIG.DAENET_OID_TEMP);
                if (temp == null) {
                    logger.error("...|-> can't reach DAENetBoard with registered IP: " + io.getIp());
                } else {
                    io.setTemp(new Integer(temp));
                    StudioManagerServer.hib_session.saveOrUpdate(io);
                    logger.info("...|-> connected to DAENetBoard at: " + io.getIp() + " of description " + io.getDescription() + ". Temperature is " + io.getTemp() + " C.");

                }
            }
            snmpAgent.addListener(this); //we register as a listener as we want to get notified of traps and see if it's about a DAENetBoard.
        }
        StudioManagerServer.hib_session.flush();
        return ioboards.size();
    }

    public boolean doAction(Action action) {
        DAENetAction a = (DAENetAction) action; //cast to DAENetAction
        switch (a.getCommandType()) {
            case RELAYS_PORT: {
                String[] channels = a.getCommandValue().split(",");
                int channel = 1;
                //assume entire bank in binary tri-state notation 1,1,0,!,X,0,0,1 (X=don't care, !=invert)
                for (String ch : channels) {     
                    if (channel>CONFIG.DANET_OID_RELAYS.length) break;
                    if (ch.equals("1") || ch.equalsIgnoreCase("ON")) {
                        retrieveOidAndFireSnmpSet(a.getToIP(),E_DAENetChannelType.RELAYS_PORT,channel,1);
                    }
                    if (ch.equals("0") || ch.equalsIgnoreCase("OFF")) {
                        //get OID and fire SnmpSet of channel channel
                        retrieveOidAndFireSnmpSet(a.getToIP(),E_DAENetChannelType.RELAYS_PORT,channel,0);
                    }
                    if (ch.equals("") || ch.equalsIgnoreCase("X")) {
                        //skip this channels
                        continue;
                    }
                    if (ch.equals("!") || ch.equalsIgnoreCase("~")) {
                        //invert this channels
                        //TODO SNMPget and then SNMPset
                        throw new UnsupportedOperationException();
                    } 
                    channel++;
                }                
                break;
            }
            case DIGITAL_PORT: String[] channels = a.getCommandValue().split(",");
                int channel = 1;
                //assume entire bank in binary tri-state notation 1,1,0,!,X,0,0,1 (X=don't care, !=invert)
                for (String ch : channels) {     
                    if (channel>CONFIG.DANET_OID_DIGITALOUTS.length) break;
                    if (ch.equals("1") || ch.equalsIgnoreCase("ON")) {
                        //get OID and fire SnmpSet of channel channel
                    }
                    if (ch.equals("0") || ch.equalsIgnoreCase("OFF")) {
                        //get OID and fire SnmpSet of channel channel
                    }
                    if (ch.isEmpty() || ch.equalsIgnoreCase("X")) {
                        //skip this channels
                        continue;
                    }
                    if (ch.equals("!") || ch.equalsIgnoreCase("~")) {
                        //invert this channels
                        //TODO SNMPget and then SNMPset
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates. //To change body of generated methods, choose Tools | Templates.
    } 
                    channel++;
                }                
                break;
            }

        //SnmpAgent.snmpSet(snmpAction.getHost(), snmpAction.getPort(), snmpAction.getCommunity(), snmpAction.getStrOID(), snmpAction.getValue());
        return snmpAgent.doAction(action);
        
    }
    
    public boolean retrieveOidAndFireSnmpSet(String ip, E_DAENetChannelType channelType, int channel, int value) {
        //TODO Lookup in table
        boolean success = false;
        switch (channelType) {
            case RELAYS_PORT: {
                String oid = CONFIG.DANET_OID_RELAYS[channel-1];
                success = snmpAgent.doAction(new SnmpSetAction(ip, CONFIG.DAENET_SET_GET_PORT, CONFIG.DAENET_COMMUNITY, oid, value));
                break;
            }
            case DIGITAL_PORT: {
                String oid = CONFIG.DANET_OID_DIGITALOUTS[channel-1];
                success = snmpAgent.doAction(new SnmpSetAction(ip, CONFIG.DAENET_SET_GET_PORT, CONFIG.DAENET_COMMUNITY, oid, value));
                break;
            }
        }
        return success;   
    }

    public void notifyTrigger(TriggerEvent event) {
        //we received a new Trigger from SnmpAgent (most probaby a Trap) - transform and forward to ChannelHandler
        SnmpTrigger t1 = (SnmpTrigger) event.getTrigger();
        switch (isDAENetOID(t1.getRundeKlammer())) {
            case RELAYS_PORT: {
                //getValue
                ChannelHandler.process(new DAENetTrigger(E_DAENetChannelType.RELAYS_PORT,t1.getRightFromEquals(),t1.getEckigeKlammer()));
                break;
            }
            case DIGITAL_PORT: {
                ChannelHandler.process(new DAENetTrigger(E_DAENetChannelType.DIGITAL_PORT,t1.getRightFromEquals(),t1.getEckigeKlammer()));
                break;
            }
            //else: the OID from the trap isn't a DAENetOID. We are not firing Triggers here. SNMPAgent will do it in a raw format.
        }
    }

    public boolean addListener(TriggerListener toAdd) {
        return listeners.add(toAdd);
    }

    public boolean removeListener(TriggerListener toRemove) {
        return listeners.remove(toRemove);
    }
    
    public boolean hasListener() {
        return (listeners.size() > 0);
    }
    
    public E_DAENetChannelType isDAENetOID(String oid) {
        E_DAENetChannelType found = null;
        if (java.util.Arrays.asList(CONFIG.DANET_OID_RELAYS).contains(oid)) {
            found = E_DAENetChannelType.RELAYS_PORT;
        }
        if (java.util.Arrays.asList(CONFIG.DANET_OID_DIGITALOUTS).contains(oid)) {
            found = E_DAENetChannelType.DIGITAL_PORT;
        }
        return found;
    }

    public void handleTrigger(TriggerEvent e) {
        
    }
    
}

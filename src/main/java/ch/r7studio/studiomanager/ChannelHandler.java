/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.ActionType;
import ch.r7studio.studiomanager.actions.BusinessAction;
import ch.r7studio.studiomanager.actions.CodedSnomLedAction;
import ch.r7studio.studiomanager.actions.DummyAction;
import ch.r7studio.studiomanager.actions.ModLcdAction;
import ch.r7studio.studiomanager.actions.SnmpAction;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import ch.r7studio.studiomanager.pojo.Channel;
import ch.r7studio.studiomanager.triggers.Trigger;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Query;

/**
 *
 * @author leutholl
 */
public class ChannelHandler implements TriggerListener {
    
    private static Logger logger = Logger.getLogger(ChannelHandler.class);

    public void init() {
        
        List<Channel> channels = (List<Channel>) StudioManagerServer.hib_session.createQuery("from Channel").list();
        if (channels.isEmpty()) {
            logger.warn("No Service Channels registered!");
        } else {
            logger.info("List of Service Channels:");
            for (Channel ios : channels) {
                logger.info("--> [" + ios.getId() + "]:" + ios.getName() + " Trigger:" + ios.getTrigger() + " -> " + ios.getAction());
                //IOServer.hib_session.saveOrUpdate(ios);
            }
        }
        //IOServer.hib_session.flush();
    }
    
    //implements TriggerListener
    public void handleTrigger(TriggerEvent e) {
        logger.info("handleTrigger: New Event ["+ e.getTrigger().getTriggerType()+ "]:"+e.getTrigger().getTriggerString());
        process(e.getTrigger());
    }


    protected static void process(Trigger trigger) {
        //search for trigger in trigger string for each chervice
        Query query;
        String hql = "FROM Channel ch WHERE ch.trigger LIKE :trigger_str";
        query = StudioManagerServer.hib_session.createQuery(hql);
        String like_str = trigger.getTriggerType().name()+"("+trigger.getRundeKlammer()+")";
        query.setParameter("trigger_str", '%' + like_str + '%');
        List<Channel> channels = (List<Channel>) query.list();
        for (Channel ch : channels) {
            logger.info("processing Action for channel: "+ch.getName()+ " with trigger: "+ch.getTrigger());
            //check address
            String address = Utils.betweenEckigeKlammerInDbString(ch.getAction());
            if (!address.isEmpty()) { //there is an address filter in the DB
                if (address.equals(trigger.getEckigeKlammer())) {
                    //address from trigger matches address in DB
                    parseAction(ch.getAction());
                } //else break, as the filter don't match
            } else {
                //no address filter in DB => allow anny trigger addresses
                parseAction(ch.getAction());
            }
        }
    }

    protected static void parseAction(String action_str) {
        //do action
        String[] actions = action_str.split(";");
        for (String action : actions) {
            action = action.trim();
            logger.info("action to do: " + action);
            if (action.startsWith(ActionType.BObj.name())) {
                //BusinessObject
                StudioManagerServer.boAgent.doAction(new BusinessAction(action));
            }
            
            if (action.startsWith(ActionType.SnomLED.name())) {
                //SnomLED(action); --OLD
                StudioManagerServer.snomAgent.doAction(new CodedSnomLedAction(action));
            }
            if (action.startsWith(ActionType.ModLCD.name())) {
                StudioManagerServer.modAgent.doAction(new ModLcdAction(action));
            }
            if (action.startsWith(ActionType.SNMP.name())) {
                StudioManagerServer.snmpAgent.doAction(new SnmpAction(action));
            }
            if (action.startsWith(ActionType.Dummy.name())) {
                StudioManagerServer.dummyAgent.doAction(new DummyAction(action));
            }
            if (action.startsWith(ActionType.MIDI.name())) {
               //not implemented
            }
            if (action.startsWith(ActionType.Console.name())) {
                logger.info("ConsoleAction: "+Utils.betweenRundeKlammerInDbString(action));
            }

        }
    }

}

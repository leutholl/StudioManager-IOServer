/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.actions.DummyAction;
import ch.r7studio.studiomanager.triggers.DummyTrigger;
import java.util.Timer;
import java.util.TimerTask;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author leutholl
 */
public class DummyAgent implements I_InAgent, I_OutAgent {
    
    public List<TriggerListener> listeners = new ArrayList<TriggerListener>();
    
    private static Logger logger = Logger.getLogger(DummyAgent.class);
     
    public DummyAgent() {
        super();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                DummyTrigger dt = new DummyTrigger("Message","127.0.0.1");
                notifyTrigger(new TriggerEvent(this, dt));
                logger.info("notifyTrigger: ip="+dt.getFrom_ip()+" message="+dt.getMessage());
            }
        }, 0, 5000); //every 5sec
    }

    public synchronized void notifyTrigger(TriggerEvent event) {
        for (TriggerListener tl : listeners)
            tl.handleTrigger(event);
    }
 
    public boolean doAction(Action action) {
        DummyAction dummyAction = (DummyAction)action;
        logger.info("doAction: "+dummyAction.getMessage());
        return true;
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
    
}

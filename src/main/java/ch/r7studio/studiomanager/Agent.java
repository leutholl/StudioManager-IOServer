/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;

/**
 *
 * @author leutholl
 */
public interface Agent {
    
    public boolean doAction(Action action);
    
    public void notifyTrigger(TriggerEvent event);
    
    public boolean addListener(TriggerListener toAdd);

    public boolean removeListener(TriggerListener toRemove);
    
}

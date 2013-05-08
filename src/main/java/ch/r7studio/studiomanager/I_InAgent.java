/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;

/**
 *
 * @author leutholl
 */
public interface I_InAgent {
    
    public void notifyTrigger(TriggerEvent event);
    
    public boolean addListener(TriggerListener toAdd);

    public boolean removeListener(TriggerListener toRemove);
    
    public boolean hasListener();
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;

import java.util.EventObject;

/**
 *
 * @author leutholl
 */
public class TriggerEvent extends EventObject {

    private Trigger trigger;

    public TriggerEvent(Object source, Trigger trigger) {
        super(source);
        this.trigger = trigger;
    }
    
    public Trigger getTrigger() {
        return trigger;
    } 
    
    @Override
    public String toString() {
        return this.trigger.toString();
    }
    
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

/**
 *
 * @author leutholl
 */
public abstract class Action {

    private long timestamp;
    
    private E_ActionType actiontype; 
    
    public Action(E_ActionType actiontype) {
        this.actiontype = actiontype;
        this.timestamp = System.currentTimeMillis();
    }
       
    public long getTimestamp() {
        return this.timestamp;
    }
    
    public abstract String getActionString();

    /**
     * @return the actiontype
     */
    public E_ActionType getActiontype() {
        return actiontype;
    }
    
}

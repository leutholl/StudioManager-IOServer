/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;

/**
 *
 * @author leutholl
 */
public abstract class Trigger {
   
    private final TriggerType triggerType;
    private long timestamp;
    
    
    public Trigger(TriggerType tt) {
        this.timestamp = System.currentTimeMillis();
        this.triggerType = tt;
    }
    
    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * @return the triggerType
     */
    public TriggerType getTriggerType() {
        return triggerType;
    }
    
    //each Subtype of Trigger must overwrite getTriggerString to be specific.
    public abstract String getTriggerString();
    
    public abstract Object getRundeKlammer();
    
    public abstract Object getEckigeKlammer();
    
    public abstract Object getRightFromEquals();
    
}

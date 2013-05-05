/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;

/**
 *
 * @author leutholl
 */
public class DummyTrigger extends Trigger {
    
    private String            message;
    private String            from_ip;
    
    public DummyTrigger(String message, String from_ip) {
        super(TriggerType.Dummy);
        this.message = message;
        this.from_ip = from_ip;
    }

    @Override
    public String getTriggerString() {
        return TriggerType.Dummy.name()+"("+message+")["+from_ip+"]";
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the from_ip
     */
    public String getFrom_ip() {
        return from_ip;
    }

    @Override
    public String getRundeKlammer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getEckigeKlammer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getRightFromEquals() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

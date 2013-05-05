/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;

/**
 *
 * @author leutholl
 */
public class BusinessTrigger extends Trigger {
    
    private String            object;
    private String            value;

    public BusinessTrigger(String object, String value) {
        super(TriggerType.BObj);
        this.object = object;
        this.value = value;
    }
    
    @Override
    public String getTriggerString() {
        //dbformat is BusinessObject(Bell)=On
        return TriggerType.BObj.name()+"("+getObject()+")="+getValue();
    }

    public String getRundeKlammer() {
        return getObject();
    }

    @Override
    public String getEckigeKlammer() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getRightFromEquals() {
        return getValue();
    }

    /**
     * @return the object
     */
    public String getObject() {
        return object;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
}

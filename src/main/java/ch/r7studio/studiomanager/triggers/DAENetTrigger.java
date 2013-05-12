/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;

import ch.r7studio.studiomanager.actions.E_DAENetChannelType;

/**
 *
 * @author leutholl
 */
public class DAENetTrigger extends Trigger {
    
    private E_DAENetChannelType commandType;
    private String              commandValue;
    private String              fromIP;
    
    public DAENetTrigger(E_DAENetChannelType commandType, String commandValue, String fromIP) {
        super(E_TriggerType.DAENet);
        this.commandType = commandType;
        this.commandValue = commandValue;
        this.fromIP = fromIP;
    }

    @Override
    public String getTriggerString() {
        //return E_TriggerType.DAENet.name()+"("+message+")["+from_ip+"]="+getValue();
        return E_TriggerType.DAENet.name();
    }


    @Override
    public E_DAENetChannelType getRundeKlammer() {
        return commandType;
    }

    @Override
    public String getEckigeKlammer() {
        return fromIP;
    }

    @Override
    public String getRightFromEquals() {
        return commandValue;
    }

   
    
}

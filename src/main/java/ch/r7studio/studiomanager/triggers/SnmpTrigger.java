/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;

import java.util.ArrayList;
import org.snmp4j.smi.VariableBinding;

/**
 *
 * @author leutholl
 */
public class SnmpTrigger extends Trigger {
    
    private ArrayList<VariableBinding>      variableBinding;
    private String                          from_ip;
    
    public SnmpTrigger(ArrayList<VariableBinding> variableBinding, String from_ip) {
        super(E_TriggerType.SNMP);
        this.variableBinding = variableBinding;
        this.from_ip         = from_ip;
    }

    @Override
    public String getTriggerString() {
        return E_TriggerType.SNMP.name();
    }

    /**
     * @return the from_ip
     */
    public String getFrom_ip() {
        return from_ip;
    }
    

    @Override
    public String getRundeKlammer() {
        return variableBinding.get(0).getOid().getSyntaxString();
    }

    @Override
    public String getEckigeKlammer() {
        return from_ip;
    }

    @Override
    public String getRightFromEquals() {
          return variableBinding.get(0).toValueString();
    }
    
}

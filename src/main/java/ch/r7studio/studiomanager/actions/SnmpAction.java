/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

import ch.r7studio.studiomanager.CONFIG;
import ch.r7studio.studiomanager.Utils;

/**
 *
 * @author leutholl
 */
public class SnmpAction extends Action {
   
    private String host;
    private int    port         = CONFIG.SNMP_PORT;
    private String community    = CONFIG.SNMP_COMMUNITY;
    private String strOID;
    private int    value;
    
    public SnmpAction(String host, String strOID, int value) {
        super(ActionType.SNMP);
        this.host   = host;
        this.strOID = strOID;
        this.value  = value;
    }
    
    public SnmpAction(String dbString) {
        //Snmp(OID)[IP]=Value
        super(ActionType.SNMP);
        this.host = Utils.betweenEckigeKlammerInDbString(dbString);
        this.strOID = Utils.betweenRundeKlammerInDbString(dbString);
        this.value = Integer.parseInt(Utils.rightFromEquals(dbString));   
    }

    @Override
    public String getActionString() {
       return ActionType.SNMP.name();
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the community
     */
    public String getCommunity() {
        return community;
    }

    /**
     * @return the strOID
     */
    public String getStrOID() {
        return strOID;
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
    }
    
}

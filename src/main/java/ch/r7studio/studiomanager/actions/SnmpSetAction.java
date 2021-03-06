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
public class SnmpSetAction extends Action {
   
    private String host;
    private int    port         = CONFIG.SNMP_PORT;
    private String community    = CONFIG.SNMP_COMMUNITY;
    private String strOID;
    private int    value;
    
    public SnmpSetAction(String host, String strOID, int value) {
        super(E_ActionType.SnmpSet);
        this.host   = host;
        this.strOID = strOID;
        this.value  = value;
    }
    
    public SnmpSetAction(String host, int port, String community, String strOID, int value) {
        super(E_ActionType.SnmpSet);
        this.host      = host;
        this.port      = port;
        this.community = community;
        this.strOID    = strOID;
        this.value     = value;
    }
    
    public SnmpSetAction(String dbString) {
        //SnmpSet(OID)[IP]=Value
        super(E_ActionType.SnmpSet);
        this.host = Utils.betweenEckigeKlammerInDbString(dbString);
        this.strOID = Utils.betweenRundeKlammerInDbString(dbString);
        this.value = Integer.parseInt(Utils.rightFromEquals(dbString));   
    }

    @Override
    public String getActionString() {
       return E_ActionType.SnmpSet.name();
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

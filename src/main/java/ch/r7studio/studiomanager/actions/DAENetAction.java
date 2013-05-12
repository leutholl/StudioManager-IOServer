/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

import ch.r7studio.studiomanager.Utils;

/**
 *
 * @author leutholl
 */
public class DAENetAction extends Action {

    private E_DAENetChannelType commandType;
    private String              commandValue;
    private String              toIP;
    
    public DAENetAction(E_DAENetChannelType commandType, String commandValue, String toIP) {
        super(E_ActionType.DAENet);
        this.commandType = commandType;
        this.commandValue = commandValue;
        this.toIP = toIP;
    }
    
    public DAENetAction(String dbString) {
        //DAENet(Ch)[IP]=Value
        super(E_ActionType.DAENet);
        this.toIP          = Utils.betweenEckigeKlammerInDbString(dbString);
        this.commandType   = E_DAENetChannelType.get(Utils.betweenRundeKlammerInDbString(dbString));
        if (this.commandType == null) {
            //default type
            this.commandType = E_DAENetChannelType.RELAYS_PORT;
        }
        this.commandValue = Utils.rightFromEquals(dbString);
    }
    
    

    @Override
    public String getActionString() {
         return E_ActionType.DAENet.name();
    }  

    /**
     * @return the commandType
     */
    public E_DAENetChannelType getCommandType() {
        return commandType;
    }

    /**
     * @return the commandValue
     */
    public String getCommandValue() {
        return commandValue;
    }

    /**
     * @return the toIP
     */
    public String getToIP() {
        return toIP;
    }
    
}

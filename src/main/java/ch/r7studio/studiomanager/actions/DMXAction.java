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
public class DMXAction extends Action {
   
    private String         host;
    private int            dmxChannelStart;
    private E_DMXCommandType commandType;
    private String         commandValue;
    
    public DMXAction(String host, int dmxChannelStart, E_DMXCommandType commandType, String commandValue) {
        super(E_ActionType.DMX);
        this.host   = host;
        this.dmxChannelStart = dmxChannelStart;
        this.commandType = commandType;
        this.commandValue  = commandValue;
    }
    
    public DMXAction(String dbString) {
        //DMX(Adr)[IP]=XValue   X=Command, Value=csv starting from Address
        super(E_ActionType.DMX);
        this.host = Utils.betweenEckigeKlammerInDbString(dbString);
        this.dmxChannelStart = Integer.parseInt(Utils.betweenRundeKlammerInDbString(dbString));
        String equals = Utils.rightFromEquals(dbString);
        this.commandType = E_DMXCommandType.get(equals.substring(0, 1));
        this.commandValue = equals.substring(1);
    }

    @Override
    public String getActionString() {
       return E_ActionType.DMX.name();
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the dmxChannelStart
     */
    public int getDmxChannelStart() {
        return dmxChannelStart;
    }

    /**
     * @return the commandType
     */
    public E_DMXCommandType getCommandType() {
        return commandType;
    }

    /**
     * @return the commandValue
     */
    public String getCommandValue() {
        return commandValue;
    }

   
    
}

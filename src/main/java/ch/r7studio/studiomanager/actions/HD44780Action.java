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
public class HD44780Action extends Action {
    
    private E_HD44780CommandType commandType;
    private String commandValue = "";
    private String host = "";
    
    public HD44780Action(String dbString) {
        //HD44780()[IP]=XCommand   X=CommandType, Command=csv String line by line
        super(E_ActionType.HD44780);
        this.host = Utils.betweenEckigeKlammerInDbString(dbString);
        String equals = Utils.rightFromEquals(dbString);
        this.commandType = E_HD44780CommandType.get(equals.substring(0, 1));
        this.commandValue = equals.substring(1);
    }
    

    @Override
    public String getActionString() {
         return E_ActionType.HD44780.name()+"()["+this.getHost()+"]="+this.getCommandValue();
    }

    /**
     * @return the commandType
     */
    public E_HD44780CommandType getCommandType() {
        return commandType;
    }

    /**
     * @return the commandValue
     */
    public String getCommandValue() {
        return commandValue;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

}

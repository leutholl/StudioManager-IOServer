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
public class CodedSnomLedAction extends Action {
    
    private int value;
    private int key;
    
    public CodedSnomLedAction(String dbString) {
        super(E_ActionType.CodedSnomLED);
        
        this.key      = Integer.parseInt(Utils.betweenRundeKlammerInDbString(dbString));
        this.value    = Integer.parseInt(Utils.rightFromEquals(dbString));
        String host   = Utils.betweenEckigeKlammerInDbString(dbString);
    
    }
    
    public String getActionString() {
        return E_ActionType.CodedSnomLED.name();
    }

    /**
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * @return the key
     */
    public int getKey() {
        return key;
    }


    
    
}

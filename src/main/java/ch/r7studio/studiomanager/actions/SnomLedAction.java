/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

/**
 *
 * @author leutholl
 */
public class SnomLedAction extends Action {
    
    private E_SnomLedFreq      freq;
    private E_SnomLedColor     color;
    private int              address;
    
    public SnomLedAction(int address, E_SnomLedFreq freq, E_SnomLedColor color ) {
        super(E_ActionType.SnomLED);
        this.address = address;
        this.freq = freq;
        this.color = color;   
    }
    
    public String getActionString() {
        return E_ActionType.SnomLED.name();
    }

    /**
     * @return the freq
     */
    public E_SnomLedFreq getFreq() {
        return freq;
    }

    /**
     * @return the color
     */
    public E_SnomLedColor getColor() {
        return color;
    }

    /**
     * @return the address
     */
    public int getAddress() {
        return address;
    }
    
    
    
}

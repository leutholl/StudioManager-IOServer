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
    
    private SnomLedFreq      freq;
    private SnomLedColor     color;
    private int              address;
    
    public SnomLedAction(int address, SnomLedFreq freq, SnomLedColor color ) {
        super(ActionType.SnomLED);
        this.address = address;
        this.freq = freq;
        this.color = color;   
    }
    
    public String getActionString() {
        return ActionType.SnomLED.name();
    }

    /**
     * @return the freq
     */
    public SnomLedFreq getFreq() {
        return freq;
    }

    /**
     * @return the color
     */
    public SnomLedColor getColor() {
        return color;
    }

    /**
     * @return the address
     */
    public int getAddress() {
        return address;
    }
    
    
    
}

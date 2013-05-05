/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

/**
 *
 * @author leutholl
 */
public enum SnomLedFreq {

    OFF(0), ON(1), BLINK(2), BLINK_SLOW(3), BLINK_FAST(4);
    
    private int code;

    private SnomLedFreq(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }
}

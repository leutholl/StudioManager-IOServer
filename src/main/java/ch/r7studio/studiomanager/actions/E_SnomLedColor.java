/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

/**
 *
 * @author leutholl
 */
public enum E_SnomLedColor {
    
    GREEN(1), ORANGE(2), RED(3);
    
    private int code;

    private E_SnomLedColor(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }
    
}

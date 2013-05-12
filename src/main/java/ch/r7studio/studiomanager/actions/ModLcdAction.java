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
public class ModLcdAction extends Action {
    
    private String message = "";
    private String lcdHEXAddress = "";
    private String toIP = "";
    
    
    public ModLcdAction(String message, String lcdHEXAddress, String toIP) {
        super(E_ActionType.ModLCD);
        this.message = message;
        this.lcdHEXAddress = lcdHEXAddress;
        this.toIP = toIP;
    }
    
    public ModLcdAction(String actionMessage) {
        super(E_ActionType.ModLCD);
        this.message = Utils.betweenRundeKlammerInDbString(actionMessage);
        this.lcdHEXAddress = Utils.betweenRundeKlammerInDbString(actionMessage);
        this.toIP = Utils.betweenEckigeKlammerInDbString(actionMessage);
    }
    

    @Override
    public String getActionString() {
         return E_ActionType.ModLCD.name()+"["+this.toIP+"]("+this.getLcdHEXAddress()+")="+this.message;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the toIP
     */
    public String getToIP() {
        return toIP;
    }

    /**
     * @return the lcdHEXAddress
     */
    public String getLcdHEXAddress() {
        return lcdHEXAddress;
    }

    

    
    
}

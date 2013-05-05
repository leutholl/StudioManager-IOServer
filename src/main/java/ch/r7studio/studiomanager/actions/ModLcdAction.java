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
    
    public ModLcdAction(String message) {
        super(ActionType.ModLCD);
        this.message = Utils.betweenRundeKlammerInDbString(message);
    }
    

    @Override
    public String getActionString() {
         return ActionType.ModLCD.name();
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    

    
    
}

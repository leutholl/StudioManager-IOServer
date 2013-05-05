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
public class BusinessAction extends Action {
    
    private String object_name = "";
    private String value       = "";
    
    
    public BusinessAction(String message) {
        super(ActionType.BObj);
        this.object_name = Utils.betweenRundeKlammerInDbString(message);
        this.value = Utils.rightFromEquals(message);
    }
    

    @Override
    public String getActionString() {
         return ActionType.BObj.name();
    }

    /**
     * @return the object_name
     */
    public String getObjectName() {
        return object_name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

   
    
}

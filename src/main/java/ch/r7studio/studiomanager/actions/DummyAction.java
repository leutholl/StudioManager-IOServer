/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

/**
 *
 * @author leutholl
 */
public class DummyAction extends Action {
    
    private String message = "";
    
    public DummyAction(String message) {
        super(ActionType.Dummy);
        this.message = message;
    }
    

    @Override
    public String getActionString() {
         return ActionType.Dummy.name();
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    

    
    
}

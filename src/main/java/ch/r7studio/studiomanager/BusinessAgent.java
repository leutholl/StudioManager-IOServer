/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.actions.BusinessAction;
import ch.r7studio.studiomanager.pojo.Businessobject;
import ch.r7studio.studiomanager.triggers.BusinessTrigger;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Transaction;

/**
 *
 * @author leutholl
 */
public class BusinessAgent implements Agent {
    
    public List<TriggerListener> listeners = new ArrayList<TriggerListener>();
    
    private static Logger logger = Logger.getLogger(BusinessAgent.class);
    
    public BusinessAgent() {
        super();
        
    }

    public boolean doAction(Action action) {
        BusinessAction bobj = (BusinessAction)action;
        logger.info("BusinessObject: "+bobj.getObjectName()+ " = "+bobj.getValue());
        return updateBusinessObject(bobj);
    }

    public void notifyTrigger(TriggerEvent event) {
        for (TriggerListener tl : listeners)
            tl.handleTrigger(event);
    }

    public boolean addListener(TriggerListener toAdd) {
        return listeners.add(toAdd);
    }

    public boolean removeListener(TriggerListener toRemove) {
         return listeners.remove(toRemove);
    }

    private boolean updateBusinessObject(BusinessAction bact) {
        Query query;
        String hql = "FROM Businessobject bo WHERE bo.name = :name";
        query = StudioManagerServer.hib_session.createQuery(hql);
        query.setParameter("name", bact.getObjectName());
        Businessobject bobj = (Businessobject)query.uniqueResult();
        bobj.setValue(bact.getValue());
        Transaction t = StudioManagerServer.hib_session.beginTransaction();
            StudioManagerServer.hib_session.saveOrUpdate(bobj);
            StudioManagerServer.hib_session.flush();
        t.commit();
        notifyTrigger(new TriggerEvent(this, new BusinessTrigger(bact.getObjectName(),bact.getValue())));
        return t.wasCommitted();
    }
    
    
    
}

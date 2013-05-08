/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;

/**
 *
 * @author leutholl
 */
@Path("/restagent")
public class RestAgent implements I_InAgent {
    
    public List<TriggerListener> listeners = new ArrayList<TriggerListener>();
    private static Logger logger = Logger.getLogger(RestAgent.class);
    HttpServer server = null;
    
    public RestAgent() {
        try {
            server = HttpServerFactory.create(CONFIG.BASE_URI);
        } catch (IOException ex) {
            logger.error(ex);
        } catch (IllegalArgumentException ex) {
            logger.error(ex);
        }
    }
    
    public void start() {
        server.start();
    }

    public boolean doAction(Action action) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void notifyTrigger(TriggerEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean addListener(TriggerListener toAdd) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean removeListener(TriggerListener toRemove) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean hasListener() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @GET
    @Path("/exit")
    @Produces(MediaType.TEXT_PLAIN)
    public String exit() {
        Thread.currentThread().setName("RestAgent");
        StudioManagerServer.exit();
        return "stopping server";
    }

    
}

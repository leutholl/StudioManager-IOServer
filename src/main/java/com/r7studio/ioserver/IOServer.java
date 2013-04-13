/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.r7studio.ioserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
//import javax.xml.ws.Endpoint;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import javax.ws.rs.*;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//slf4j-log4j12
//slf4j-api-1.6.1jar
//dom4j
/**
 *
 * @author leutholl
 */
@Path("/ioserver")
public class IOServer {

    static protected SnomIOServer snomIoServer = null;
    private static SessionFactory hib_session_factory;
    protected static Session hib_session;
    protected final static String SNMP_COMMUNITY = "private";
    protected final static String SNMP_PORT = "16100";
    static final String BASE_URI = "http://127.0.0.1:8081/rest";
    private static Connection conn;
    private static boolean terminated = false;
    private final static Object TERMINATION_LOCK = new Integer(2);
    private static Thread hook;
    
    private static Logger logger = Logger.getRootLogger();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            
            
            //Logger  logger = Logger.getLogger("org.apache");
            //Logger nlogger = Logger.getLogger("org.apache.log4j");

            logger.setLevel(Level.ALL);
            
          
            
            hook = new IOServer.ShutdownHook();
            hook.setName("Hook");
            Runtime.getRuntime().addShutdownHook(hook);
            
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "Connecting to Derby");
            boolean buildDBSession = buildDBSession();
            if (buildDBSession) {
                Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                        "OK.");
            } else System.exit(0);
            
           

            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "Snom server starting up");

            snomIoServer = new SnomIOServer(); //create server

            //snomIoServer.init();
            snomIoServer.start(); //run the Thread
           

            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "REST server starting up");
            HttpServer server = HttpServerFactory.create(BASE_URI);
            server.start();
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "listening for REST calls on: " + BASE_URI);

            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "IOServer running.");
            System.out.println("IOServer running.");
        } catch (IOException ex) {
            logger.error(ex);
        } catch (IllegalArgumentException ex) {
            logger.error(ex);
        } finally {
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "IOServer running.");
            System.out.println("IOServer running.");

            System.out.println("Call REST://exit to exit.");

            try {
                synchronized (TERMINATION_LOCK) {
                    while (!terminated) {
                        TERMINATION_LOCK.wait();
                    }
                }
            } catch (InterruptedException ex) {
                logger.error(ex);
            }

            System.exit(0); //call Hook
        }

    }

    @GET
    @Path("/onSnomKeyPress")
    @Produces(MediaType.TEXT_PLAIN)
    public String onSnomKeyPress(@QueryParam("key") int key) {
        System.out.println("REST: onSnomKeyPress() key=" + key);
        return "" + snomIoServer.onSnomKeyPress(key);
    }

    @GET
    @Path("/getOneOrAllLeds")
    @Produces(MediaType.TEXT_PLAIN)
    public String getOneOrAllLeds(@QueryParam("key") short key) {
        System.out.println("REST: onSnomKeyPress() key=" + key);
        return "" + snomIoServer.syncOneOrAllLeds(null, key);
    }

    @GET
    @Path("/getIOBoards")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIOBoards() {
        String result = "";

        List<Ioboard> ioboards = (List<Ioboard>) IOServer.hib_session.createQuery("from Ioboard").list();

        for (Ioboard io : ioboards) {
            result += io.getIp() + ",";
        }

        IOServer.hib_session.flush();
        System.out.println("REST: getIOBoards(): " + result);
        return result;
    }

    @GET
    @Path("/getSnomClients")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSnomClients() {
        String result = "";

        List<Snomclient> clients = (List<Snomclient>) IOServer.hib_session.createQuery("from Snomclient").list();

        for (Snomclient cl : clients) {
            result += cl.getIp() + ",";
        }

        IOServer.hib_session.flush();
        System.out.println("REST: getSnomClients: " + result);
        return result;
    }

    @GET
    @Path("/exit")
    @Produces(MediaType.TEXT_PLAIN)
    public String exit() {
        System.out.println("REST: exit");
        terminated = true;
        synchronized (TERMINATION_LOCK) {
            TERMINATION_LOCK.notify();
        }
        return "stopping server";
    }

    private static boolean buildDBSession() {
        try {
            // Do Hibernate stuff
            // Create SessionFactory and Session object
            hib_session_factory = new Configuration().configure().
                    buildSessionFactory();
            if (hib_session_factory.isClosed()) {
                logger.error("Could not create Session Factory to Derby!");
                return false;
            }

            conn = DriverManager.getConnection("jdbc:derby://localhost:1527/snomDB;create=true;user=snom;password=snomsnom");
            if (conn.isValid(10)) {
                hib_session = hib_session_factory.openSession(conn);
            } else {
                hib_session = hib_session_factory.openSession(); //use config
            }


            if (!hib_session.isConnected()) {
                logger.error("Could not connect to Derby!");
                return false;
            }          
        } catch (SQLException ex) {
            logger.error(ex);
            return false;
        }
        return true;
        
    }

    private static class ShutdownHook extends Thread {

        @Override
        public void run() {
            try {
                System.out.println("IOServer Shutdown Hook called.");
                SnomIOServer.terminated = true;
                synchronized (SnomIOServer.TERMINATION_LOCK) {
                    SnomIOServer.TERMINATION_LOCK.notify();
                }
                Thread.sleep((1000L * SnomIOServer.TIMEOUT + 1));
                if (IOServer.hib_session != null) {
                    if (IOServer.hib_session.isConnected()) {
                        System.out.println("Java DB session closing.");
                        IOServer.hib_session.flush();
                        IOServer.hib_session.disconnect();
                        if (IOServer.hib_session.isOpen()) {
                            IOServer.hib_session.close();    // close connection to Java DB
                        }
                        System.out.println("Java DB session closed.");
                    }
                }
                System.out.println("Exiting IOServer...");
                System.out.println("bye!");
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
    }
}

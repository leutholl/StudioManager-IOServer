/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.r7studio.ioserver;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

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
    static protected MidiAgent midiAgent = null;
    static protected ModtronixAgent modAgent = null;
    private static SessionFactory hib_session_factory;
    protected static Session hib_session;
    protected final static String SNMP_COMMUNITY = "private";
    protected final static String SNMP_PORT = "16100";
    static final String BASE_URI = "http://127.0.0.1:8081/rest";
    private static boolean terminated = false;
    private final static Object TERMINATION_LOCK = new Integer(2);
    private static Thread hook;
    private static Logger logger = Logger.getRootLogger();
    private static ServiceRegistry serviceRegistry;
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        init();
    }

    private static void init() {
        try {
            //Thread.currentThread().setName("IOServer");
            //logger.setLevel(Level.INFO);
            logger.info("Initializing IOServer");

            hook = new IOServer.ShutdownHook();
            Runtime.getRuntime().addShutdownHook(hook);

            logger.info("Connecting to Derby");
            boolean buildDBSession = buildDBSession();
            if (buildDBSession) {
                logger.info("...successfully connected to Derby");
            } else {
                logger.fatal("...can't connect to Derby. Exiting.");
                System.exit(0);
            }

            logger.info("Initializing Modtronix Boards");
            modAgent = new ModtronixAgent();
            modAgent.initModule();

            modAgent.clearLCD();

            //------------------12345678901234567890
            modAgent.writeLCD1("====================");
            modAgent.writeLCD2("     STARTING UP    ");
            modAgent.writeLCD3("     please wait    ");
            modAgent.writeLCD4("====================");
            Thread.sleep(2000);


            logger.info("Creating Snom Service");
            snomIoServer = new SnomIOServer(); //create server
            logger.info("Initializing Snom Service");
            snomIoServer.init();
            Thread.currentThread().setName("IOServer");
            logger.info("Starting Snom Service");
            snomIoServer.start(); //run the Thread
            Thread.sleep(1000);
            //----------------12345678901234567890
            modAgent.rollLCD("SnomIO Srv.: started");


            logger.info("Initializing REST Service");
            HttpServer server = HttpServerFactory.create(BASE_URI);
            logger.info("Starting REST Service");
            server.start();
            //----------------12345678901234567890
            modAgent.rollLCD("REST Srv.:   started");
            logger.info("...listening for REST calls on: " + BASE_URI);

            Thread.sleep(750);


            //listen for I2C Messages and send init string
            logger.info("Starting I2C listener");
            modAgent.startI2CListener();
            modAgent.sendI2C("Successfully started!");
            //----------------12345678901234567890
            modAgent.rollLCD("I2C Srv.:    started");

            Thread.sleep(750);

            //register and listen for Events
            //first listen then register
            //otherwise the listener gets all the queued UDP datagrams in one rush
            logger.info("Starting event listener");
            modAgent.startEventListener();
            //----------------12345678901234567890
            modAgent.rollLCD("Event Srv.:  started");

            Thread.sleep(750);

            logger.info("registering for events");
            modAgent.registerForEvents();
            //----------------12345678901234567890
            modAgent.rollLCD("registering events  ");

            Thread.sleep(750);

            logger.info("Starting MIDI listener");
            midiAgent = new MidiAgent();
            midiAgent.queryPortsAndListen();
            //----------------12345678901234567890
            modAgent.rollLCD("MIDI Srv.:  started");

            logger.info("IOServer running. Call " + BASE_URI + "/ioserver/exit to quit the application");
            logger.info("------------------------------------------------------------------------------");

            Thread.sleep(1000);

            //modAgent.backlightLCD(1);
            //modAgent.contrastLCD(1);
            //modAgent.clearLCD();
            //modAgent.backlightLCD(1);
            //modAgent.contrastLCD(1);
            //------------------12345678901234567890
            modAgent.writeLCD1("====================");
            modAgent.writeLCD2("     successfuly    ");
            modAgent.writeLCD3("      started!      ");
            modAgent.writeLCD4("====================");
            /**
             * for (short i=0;i<=255;i+=10) { modAgent.backlightLCD(i);
             * Thread.sleep(10); modAgent.contrastLCD(i); Thread.sleep(650); }
             */
            try {
                synchronized (TERMINATION_LOCK) {
                    while (!terminated) {
                        TERMINATION_LOCK.wait(1000);
                        //modAgent.heartbeatLCD();
                    }
                }
            } catch (InterruptedException ex) {
                logger.error(ex);
            }

            System.exit(0); //INVOKE SHUTDOWN HOOK


        } catch (IOException ex) {
            logger.error(ex);
        } catch (IllegalArgumentException ex) {
            logger.error(ex);
        } catch (InterruptedException ex) {
            logger.error(ex);
        }

    }

    public static void onI2CKey(int key, InetAddress host) {
        logger.info("onI2Ckey: " + key);
        if (key == 10) {
            //ringer
            if (snomIoServer != null) {
                snomIoServer.onSnomKeyPress(10);
            }
        }
    }

    @GET
    @Path("/onSnomKeyPress")
    @Produces(MediaType.TEXT_PLAIN)
    public String onSnomKeyPress(@QueryParam("key") int key) {
        Thread.currentThread().setName("REST Handler");
        int val = snomIoServer.onSnomKeyPress(key);
        logger.info("REST: onSnomKeyPress() key=" + key + ":" + val);
        return "" + val;
    }

    @GET
    @Path("/getOneOrAllLeds")
    @Produces(MediaType.TEXT_PLAIN)
    public String getOneOrAllLeds(@QueryParam("key") short key) {
        Thread.currentThread().setName("REST Handler");
        String result = snomIoServer.syncOneOrAllLeds(null, key);
        logger.info("REST: getOneOrAllLeds() key=" + key + ":" + result);
        return result;
    }

    @GET
    @Path("/getIOBoards")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIOBoards() {
        Thread.currentThread().setName("REST Handler");
        String result = "";

        List<Ioboard> ioboards = (List<Ioboard>) IOServer.hib_session.createQuery("from Ioboard").list();

        for (Ioboard io : ioboards) {
            result += io.getIp() + ",";
        }

        IOServer.hib_session.flush();
        logger.info("REST: getIOBoards(): " + result);
        return result;
    }

    @GET
    @Path("/getSnomClients")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSnomClients() {
        Thread.currentThread().setName("REST Handler");
        String result = "";

        List<Snomclient> clients = (List<Snomclient>) IOServer.hib_session.createQuery("from Snomclient").list();

        for (Snomclient cl : clients) {
            result += cl.getIp() + ",";
        }

        IOServer.hib_session.flush();
        logger.info("REST: getSnomClients: " + result);
        return result;
    }

    @GET
    @Path("/exit")
    @Produces(MediaType.TEXT_PLAIN)
    public String exit() {
        Thread.currentThread().setName("REST Handler");
        logger.info("REST: exit called");
        terminated = true;
        synchronized (TERMINATION_LOCK) {
            TERMINATION_LOCK.notify();
        }
        return "stopping server";
    }

    private static boolean buildDBSession() {

        // Do Hibernate stuff
        // Create SessionFactory and Session object
        //
        // Build a SessionFactory object from session-factory configuration
        // defined in the hibernate.cfg.xml file. In this file we register
        // the JDBC connection information, connection pool, the hibernate
        // dialect that we used and the mapping to our hbm.xml file for each
        // Pojo (Plain Old Java Object).
        //
        try {

            Configuration configuration = new Configuration();
            configuration.configure();
            serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
            hib_session_factory = configuration.buildSessionFactory(serviceRegistry);

            if (hib_session_factory.isClosed()) {
                logger.error("Could not create Session Factory to Derby!");
                return false;
            }

            hib_session = hib_session_factory.openSession(); //use config

            if (!hib_session.isConnected()) {
                logger.error("Could not connect to Derby!");
                return false;
            }
        } catch (Exception e) {
            logger.fatal("Exception: ", e);
        }

        return true;
    }

    private static class ShutdownHook extends Thread {

        @Override
        public void run() {
            Thread.currentThread().setName("main Shutdown Hook");
            try {
                logger.warn("IOServer Shutdown Hook called.");

                //modAgent.clearLCD();
                //------------------12345678901234567890
                modAgent.writeLCD1("====================");
                modAgent.writeLCD2("   SHUTTING DOWN    ");
                modAgent.writeLCD3("    please wait     ");
                modAgent.writeLCD4("====================");

                /**
                 * for (short i=255;i>=1;i--) { modAgent.backlightLCD(i);
                 * modAgent.contrastLCD(i); Thread.sleep(50); }
                 */
                logger.info("deregistering for events");
                modAgent.deregisterForEvents();
                Thread.sleep(1000); //wait so that datagrams currently on the way won't disturb log
                //----------------12345678901234567890
                modAgent.rollLCD("deregistering events");

                logger.info("shutting down MIDI listener");
                midiAgent.closeAllListener();
                Thread.sleep(333);
                //----------------12345678901234567890
                modAgent.rollLCD("MIDI handler:  stopd");


                logger.info("shutting down event listener");
                modAgent.stopEventListener();
                Thread.sleep(333);
                //----------------12345678901234567890
                modAgent.rollLCD("Event handler: stopd");


                logger.info("shutting down I2C listener");
                modAgent.stopI2CListener();
                Thread.sleep(333);
                //----------------12345678901234567890
                modAgent.rollLCD("I2C handler:   stopd");

                logger.info("shutting down SnomIOServer");
                SnomIOServer.terminated = true;
                synchronized (SnomIOServer.TERMINATION_LOCK) {
                    SnomIOServer.TERMINATION_LOCK.notify();
                }
                Thread.sleep((1000L * SnomIOServer.TIMEOUT + 1));
                //----------------12345678901234567890
                modAgent.rollLCD("SnomIO serv:   stopd");

                if (IOServer.hib_session != null) {
                    if (IOServer.hib_session.isConnected()) {
                        logger.info("Derby session closing...");
                        IOServer.hib_session.flush();
                        IOServer.hib_session.disconnect();
                        if (IOServer.hib_session.isOpen()) {
                            IOServer.hib_session.close();    // close connection to Java DB
                        }
                        //----------------12345678901234567890
                        modAgent.rollLCD("Derby sess.:   stopd");
                        logger.info("Derby session closed.");
                    }
                }
                Thread.sleep(333);
                //----------------12345678901234567890
                modAgent.rollLCD("------- BYE -------");
                Thread.sleep(2000);
                modAgent.clearLCD();
                Thread.sleep(2000);
                modAgent.homeScreen();
                logger.warn("Exiting IOServer...");
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
    }
}

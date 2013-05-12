/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.DMXAction;
import ch.r7studio.studiomanager.pojo.Snomvision;
import ch.r7studio.studiomanager.pojo.Daenetip;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
public class StudioManagerServer {
 
    protected static SnomAgent      snomAgent  = null;
    protected static MidiAgent      midiAgent  = null;
    protected static ModtronixAgent modAgent   = null;
    protected static DummyAgent     dummyAgent = null;
    protected static DAENetAgent    daeNetAgent= null;
    protected static SnmpAgent      snmpAgent  = null;
    protected static DMXAgent       dmxAgent   = null;
    protected static HD44780Agent   lcdAgent   = null;
    protected static BusinessAgent  boAgent    = null;
    protected static RestAgent      restAgent  = null;
    protected static ChannelHandler handler    = null;
    protected static SessionFactory hib_session_factory;
    protected static Session        hib_session;
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
        logger.info("Initializing StudioManager");
        hook = new StudioManagerServer.ShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);
        logger.info("Connecting to DB");
        boolean buildDBSession = buildDBSession();
        if (buildDBSession) {
            logger.info("...successfully connected to DB");
        } else {
            logger.fatal("...can't connect to DB. Exiting.");
            System.exit(0);
        }

        handler = new ChannelHandler();
        handler.init();

        logger.info("Starting up all Agents");
        logger.info(" --> Initializing BusinessObjectAgent");
        boAgent = new BusinessAgent();
        boAgent.addListener(handler);
        
        lcdAgent = new HD44780Agent("192.168.1.190",54126,"40");
        lcdAgent.initLCD();
        lcdAgent.lightOff();
        lcdAgent.lightOn();
        lcdAgent.clear();
        lcdAgent.print("Current Time is: ");
        lcdAgent.locate(2, 1);
        lcdAgent.print(Utils.getCurrentTimeStamp());
        
        
        //System.exit(0);
        
        logger.info(" --> Initializing DMXAgent");
        dmxAgent = new DMXAgent();
        //dmxAgent.doAction(new DMXAction("DMX(1)[192.168.1.185]=a127"));
       
        logger.info(" --> Initializing ModtronixAgent");
        modAgent = new ModtronixAgent();
        modAgent.initModule();
        logger.info(" ----> Starting UDP event listener");
        modAgent.startEventListener();
        logger.info(" ----> Registering for UDP events");
        modAgent.registerForEvents();
        modAgent.addListener(handler);
        logger.info(" --> Initializing SnomAgent");
        snomAgent = new SnomAgent();
        snomAgent.init();
        snomAgent.addListener(handler);
        snomAgent.start();
        logger.info(" --> Initializing MidiAgent");
        midiAgent = new MidiAgent();
        logger.info(" ----> Discovering MIDI ports");
        int ports = midiAgent.init();
        if (ports > 0) {
            logger.info(" ----> using MIDI Agent");
            midiAgent.addListener(handler);          
        } else {
            logger.warn(" ----> not using the MIDI Agent");
        }
        
        logger.info(" --> Initializing DAENetAgent");
        daeNetAgent = new DAENetAgent();
        logger.info(" ----> Discovering DAENetIP Boards");
        if (daeNetAgent.discoverDaeNetIpBoards() > 0) {
            logger.info(" ----> using SnmpAgent");
            snmpAgent.addListener(handler);
        } else {
            logger.warn(" ----> not using the DAENetAgent");
        }
        
        
        logger.info(" --> Initializing SnmpAgent");
        snmpAgent = new SnmpAgent();
        snmpAgent.addListener(handler);
        

        //logger.info(" --> Initializing DummyAgent");
        //dummyAgent = new DummyAgent();
        //dummyAgent.addListener(handler);
        
        logger.info(" --> Initializing REST Service");
            restAgent = new RestAgent();
            logger.info(" ----> Starting REST Service");
            restAgent.start();
            //----------------12345678901234567890
            modAgent.rollLCD("REST Srv.:   started");
            logger.info(" ----> ...listening for REST calls on: " + CONFIG.BASE_URI);

        logger.info(StudioManagerServer.class+" running. Call " + CONFIG.BASE_URI + "/ioserver/exit to quit the application");
        logger.info("------------------------------------------------------------------------------");

    }

    private static void init2() {
        try {
            //Thread.currentThread().setName("StudioManagerServer");
            //logger.setLevel(Level.INFO);
            logger.info("Initializing IOServer");

            hook = new StudioManagerServer.ShutdownHook();
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
            snomAgent = new SnomAgent(); //create server
            logger.info("Initializing Snom Service");
            snomAgent.init();
            Thread.currentThread().setName("IOServer");
            logger.info("Starting Snom Service");
            snomAgent.start(); //run the Thread
            Thread.sleep(1000);
            //----------------12345678901234567890
            modAgent.rollLCD("SnomIO Srv.: started");


            logger.info("Initializing REST Service");
            HttpServer server = HttpServerFactory.create(CONFIG.BASE_URI);
            logger.info("Starting REST Service");
            server.start();
            //----------------12345678901234567890
            modAgent.rollLCD("REST Srv.:   started");
            logger.info("...listening for REST calls on: " + CONFIG.BASE_URI);

            Thread.sleep(750);


            //listen for I2C Messages and send start string
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
            midiAgent.run();
            //----------------12345678901234567890
            modAgent.rollLCD("MIDI Srv.:  started");

            logger.info("IOServer running. Call " + CONFIG.BASE_URI + "/ioserver/exit to quit the application");
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

    @GET
    @Path("/getIOBoards")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIOBoards() {
        Thread.currentThread().setName("REST Handler");
        String result = "";

        List<Daenetip> ioboards = (List<Daenetip>) StudioManagerServer.hib_session.createQuery("from Daenetip").list();

        for (Daenetip io : ioboards) {
            result += io.getIp() + ",";
        }

        StudioManagerServer.hib_session.flush();
        logger.info("REST: getIOBoards(): " + result);
        return result;
    }

    @GET
    @Path("/getSnomClients")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSnomClients() {
        Thread.currentThread().setName("REST Handler");
        String result = "";

        List<Snomvision> clients = (List<Snomvision>) StudioManagerServer.hib_session.createQuery("from Snomvision").list();

        for (Snomvision cl : clients) {
            result += cl.getIp() + ",";
        }

        StudioManagerServer.hib_session.flush();
        logger.info("REST: getSnomClients: " + result);
        return result;
    }

    @GET
    @Path("/exit")
    @Produces(MediaType.TEXT_PLAIN)
    public static String exit() {
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
                
                if (lcdAgent != null) {
                    lcdAgent.clear();
                    lcdAgent.print("Shutting down...");
                    Thread.sleep(1000);
                    lcdAgent.clear();
                    lcdAgent.lightOff();
                }
                

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

                while (midiAgent.hasListener()) {
                    logger.info("shutting down MIDI listener");
                    midiAgent.removeListener(handler);
                    midiAgent.closeAllListener();
                }
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
                SnomAgent.terminated = true;
                synchronized (SnomAgent.TERMINATION_LOCK) {
                    SnomAgent.TERMINATION_LOCK.notify();
                }
                Thread.sleep((1000L * SnomAgent.TIMEOUT + 1));
                //----------------12345678901234567890
                modAgent.rollLCD("SnomIO serv:   stopd");

                if (StudioManagerServer.hib_session != null) {
                    if (StudioManagerServer.hib_session.isConnected()) {
                        logger.info("DB session closing...");
                        StudioManagerServer.hib_session.flush();
                        StudioManagerServer.hib_session.disconnect();
                        if (StudioManagerServer.hib_session.isOpen()) {
                            StudioManagerServer.hib_session.close();    // close connection to Java DB
                        }
                        //----------------12345678901234567890
                        modAgent.rollLCD("DB session:   stopd");
                        logger.info("DB session closed.");
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.actions.ModLcdAction;
import ch.r7studio.studiomanager.pojo.Modtronix;
import ch.r7studio.studiomanager.triggers.ModLcdKeyTrigger;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;

/**
 *
 * @author leutholl
 */
@Path("/modtronix")
public class ModtronixAgent extends Thread implements Agent {
    
    public List<TriggerListener> listeners = new ArrayList<TriggerListener>();

    protected I2CAgent i2c = null;
    
    private final static int CONF_PORT = 54123; //to configure SBC65EC
    private final static int EVENT_PORT = 54124; //to listen to event from SBC65EC
    private final static int USART1_PORT = 54125; //to listen to event from SBC65EC
    private static InetAddress address;
    private static DatagramSocket conf_socket;
    private static DatagramSocket event_socket;
    private static Logger logger = Logger.getLogger(ModtronixAgent.class);
    private int heartbeat = 0;
    private boolean play;
    private boolean rec;
    private boolean listening = true;
    private int number_of_armed_tracks;

    public void ModtronixAgent() {
    }

    public void initModule() {
        try {
            //todo configure Module and Events
            // see http://www.modtronix.com/products/sbc65ec/websrvr65_v310/

            //get host address
            //IOBoard List
            List<Modtronix> modclients = (List<Modtronix>) StudioManagerServer.hib_session.createQuery("from Modtronix").list();
            if (modclients.isEmpty()) {
                logger.warn("No Modtronix board registered!");
            } else {
                logger.info("List of Modtronix boards:");
                for (Modtronix mc : modclients) {
                    // handle each IOBoard
                    logger.info("|-> connecting to Modtronix Board at: " + mc.getIp() + "...");
                    ModtronixAgent.address = InetAddress.getByName(mc.getIp());
                    StudioManagerServer.hib_session.saveOrUpdate(mc);
                    logger.info("...|-> connected to Modtronix Board at: " + mc.getIp());
                }
            }
            StudioManagerServer.hib_session.flush();

            // Construct the conf_socket
            ModtronixAgent.conf_socket = new DatagramSocket(CONF_PORT);
            ModtronixAgent.event_socket = new DatagramSocket(EVENT_PORT);
            //socket.setSendBufferSize(UDP_SEND_BUF);
            //event_socket.setSoTimeout(1000); //timeout 1 sec
            //sendConf("ac0r=1;ac1r=1;ac2r=1;ac3r=1;ac4r=1;ac5r=1;ac6r=1".getBytes());
            
            this.i2c = new I2CAgent(ModtronixAgent.address, ModtronixAgent.USART1_PORT);


        } catch (SocketException ex) {
            logger.warn(ex);
            //java.util.logging.Logger.getLogger(ModtronixAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            logger.warn(ex);
            //java.util.logging.Logger.getLogger(ModtronixAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendI2C(String msg) {
        i2c.send(msg);
    }

    public void registerForEvents() {
        sendEvent("01".getBytes());
    }

    public void deregisterForEvents() {
        sendEvent("00".getBytes());
    }

    @GET
    @Path("/showLCD")
    public void showLCD(@QueryParam("on") boolean on) {
        if (on) {
            sendConf(("lk=_1A").getBytes());
        } else {
            sendConf(("lk=_12").getBytes());
        }
    }

    public void backlightLCD(boolean on) {
        if (on) {
            sendConf(("lk=_28").getBytes());
        } else {
            sendConf(("lk=_20").getBytes());
        }
    }

    @GET
    @Path("/backlightLCD")
    @Produces(MediaType.TEXT_PLAIN)
    public String backlightLCD(@QueryParam("val") int brightness) {
        //return "Not Implemented yet";

        int val = brightness;
        if (val >= 255) {
            val = 255;
        }
        if (val < 0) {
            val = 0;
        }
        sendConf(("lk=_81_" + (char) (val & 0xFF)).getBytes());
        return "" + val;

    }

    @GET
    @Path("/contrastLCD")
    @Produces(MediaType.TEXT_PLAIN)
    public String contrastLCD(@QueryParam("val") int contrast) {
        //return "Not Implemented yet";
        int val = contrast;
        if (val >= 254) {
            val = 254;
        }
        if (val
                < 0) {
            val = 0;
        }
        String data = "lk=_82_" + (char) (val & 0xFF);
        //sendAndReceive(("lk=_82"+Character.toString((char)val)).getBytes());
        sendConf(data.getBytes());
        return "" + val;


    }

    @GET
    @Path("/clearLCD")
    public void clearLCD() {
        //clear and set cursor to 1,1
        sendConf(("lk=_8C&lk=_8B").getBytes());
    }

    @GET
    @Path("/rollLCD")
    public boolean rollLCD(@QueryParam("msg") String msg) {
        try {
            ModtronixAgent.sleep(200);
        } catch (InterruptedException ex) {
            logger.warn(ex);
        }
        //shift up
        sendConf(("lk=_87").getBytes());
        //place cursor to line 4, 1 and write
        sendConf(("ll=_r_n_n_n" + msg).getBytes());
        return true;
    }
    
    public void homeScreen() {
        //---------12345678901234567890
        writeLCD1("   Studio Manager   ");
        writeLCD2("   ------ -------   ");
        writeLCD3("                    ");
        writeLCD4("connecting to server");
    }

    public void setPlay(boolean play) {
        this.play = play;
    }

    public void setRec(boolean rec) {
        this.rec = rec;
    }

    public void incArmedTracks() {
        this.number_of_armed_tracks++;
    }

    public void decArmedTracks() {
        this.number_of_armed_tracks--;
    }

    public int getArmedTracks() {
        return this.number_of_armed_tracks;
    }

    public synchronized void updateDAWLCD(boolean safe_to_enter) {
        if (!safe_to_enter) {
            //clearLCD();
            //---------12345678901234567890
            writeLCD1("====================");
            writeLCD2("   Wait after last  ");
            writeLCD3("  R E C O R D I N G ");
            writeLCD4("====================");
            return;
        }
        if (number_of_armed_tracks == 0 && !rec && !play) {
            //NOTHING - CLEAR SCREEN
            //clearLCD();
            //return;
        }
        //---------12345678901234567890

        if (number_of_armed_tracks > 0 && !rec && !play) {
            //---------12345678901234567890
            writeLCD1("====================");
            writeLCD2("    PREPARE FOR     ");
            writeLCD3("  R E C O R D I N G ");
            writeLCD4("====================");
            return;
        }

        if (play) {
            if (rec) {
                //RECORINDG
                //---------12345678901234567890
                writeLCD1("====================");
                writeLCD2("    !ATTENTION!     ");
                writeLCD3("  R E C O R D I N G ");
                writeLCD4("====================");
            }
            if (!rec) {
                //PLAYING
                //---------12345678901234567890
                writeLCD1("====================");
                writeLCD2("    !ATTENTION!     ");
                writeLCD3("   P L A Y I N G    ");
                writeLCD4("====================");
            }
        }
        if (!play) {
            if (rec) {
                //RECORINDG
                //---------12345678901234567890
                writeLCD1("====================");
                writeLCD2("    PREPARE FOR     ");
                writeLCD3("  R E C O R D I N G ");
                writeLCD4("====================");
            }
            if (!rec) {
                //NOTHING - CLEAR SCREEN
                //ENTER
                //---------12345678901234567890
                writeLCD1("====================");
                writeLCD2("    PLEASE ENTER    ");
                writeLCD3("   BITTE EINTRETEN  ");
                writeLCD4("====================");
            }
        }

    }

    @GET
    @Path("/writeLCD")
    public void writeLCD(@QueryParam("msg") String msg) {
        sendConf(("ll=" + msg).getBytes());
    }

    public void writeLCD1(String msg) {
        sendConf(("lk=_8B&ll=_f" + msg).getBytes());
    }

    public void writeLCD2(String msg) {
        sendConf(("lk=_8B&ll=_n" + msg).getBytes());
    }

    public void writeLCD3(String msg) {
        sendConf(("lk=_8B&ll=_n_n" + msg).getBytes());
    }

    public void writeLCD4(String msg) {
        sendConf(("lk=_8B&ll=_n_n_n" + msg).getBytes());
    }

    public void heartbeatLCD() {

        heartbeat++;
        if (heartbeat > 1) {
            heartbeat = 0;
        }
        sendConf(("lk=_8A" + Character.toString((char) 1)
                + Character.toString((char) 20)).getBytes());
        switch (heartbeat) {
            case 0:
                sendConf(("ll=_" + (char) 0xB0).getBytes());
                //sendConf(("ll=-").getBytes());
                break;
            case 1:
                sendConf(("ll=_" + (char) 0xB1).getBytes());
                //sendConf(("ll==").getBytes());
                break;
        }
        //set cursor to 1,1
        sendConf("lk=_8B".getBytes());
    }

    public synchronized static void sendConf(byte[] message) {

        DatagramPacket packet = new DatagramPacket(message, message.length,
                ModtronixAgent.address, CONF_PORT);
        try {
            // Send it
            conf_socket.send(packet);
            ModtronixAgent.sleep(CONFIG.LCD_SLEEP); //allow the LCD to proccess the data before sending the next command

        } catch (IOException ex) {
            logger.warn(ex);

        } catch (InterruptedException ex) {
            logger.warn(ex);
        }


    }

    protected void startEventListener() {
        listening = true;
        this.start();
    }

    protected void stopEventListener() {
        listening = false;
    }

    protected void startI2CListener() {
        if (i2c != null) {
            i2c.startListener();
        }
    }

    protected void stopI2CListener() {
        if (i2c != null) {
            i2c.stopListener();
        }
    }

    @Override
    public void run() {

        Thread.currentThread().setName("UDP Event Listener");
        byte[] buffer = new byte[1024];
        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
        while (listening) {
            try {
                incoming.setLength(buffer.length);
                event_socket.receive(incoming);
                byte[] data = incoming.getData();
                String[] events = new String(data, 0, incoming.getLength()).split(";");
                for (String event : events) {
                    if (event.equals("l40=2")) {
                        this.heartbeatLCD();
                    }
                    if (event.startsWith("l34=")) {
                        char key = Character.toUpperCase(event.charAt(4));
                        notifyTrigger(new TriggerEvent(incoming.getAddress().getHostAddress(),new ModLcdKeyTrigger(key,"0x80")));
                        //StudioManagerServer.onLcdKey(key, "0x80" ,incoming.getAddress());
                    }

                }
                //logger.debug("received data: " + new String(data, 0, incoming.getLength()) + ":" + data);

            } catch (IOException ex) {
                logger.warn(ex);
            }
        }
        event_socket.close();

    }

    public static void sendEvent(byte[] message) {

        DatagramPacket packet = new DatagramPacket(message, message.length,
                ModtronixAgent.address, EVENT_PORT);
        try {
            // Send it
            event_socket.send(packet);

        } catch (IOException ex) {
            //TODO -> HANDLE EXCEPTION WHEN PORT IS ALREADY USED
            logger.warn(ex);

        }

    }
    
    public boolean addListener(TriggerListener toAdd) {
        return listeners.add(toAdd);
    }

    public boolean removeListener(TriggerListener toRemove) {
        return listeners.remove(toRemove);
    }

    public boolean doAction(Action action) {
        boolean success = false;
        switch (action.getActiontype()) {
             case ModLCD:
                 ModLcdAction a = (ModLcdAction)action;
                 success = rollLCD(a.getMessage());
                 break;

        }
        return success;
    }

    public void notifyTrigger(TriggerEvent event) {
        for (TriggerListener tl : listeners)
            tl.handleTrigger(event);
    }
}

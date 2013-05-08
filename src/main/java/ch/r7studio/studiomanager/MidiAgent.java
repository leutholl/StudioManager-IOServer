/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
import java.util.ArrayList;
import javax.sound.midi.*;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author leutholl
 */
public class MidiAgent implements I_InAgent {
    
    public List<TriggerListener> listeners = new ArrayList<TriggerListener>();

    private final static byte DEV_ID = 127; //reserved for all devicide must respond - used by MMC
    private final static int NOTE_ON = 144;
    private final static int ACTIVE_SENSING_STATUS_BYTE = 254;
    private final static int END_OF_SYSEX = 247;
    //MIDI MSG for CM MOTORMIX CUBASE
    private final static int CC = 0xB0;
    private final static byte[] CM_MIDI_PLAY = {(byte) 0xB0, (byte) 0x2C, (byte) 0x47};
    private final static byte[] CM_MIDI_STOP = {(byte) 0xB0, (byte) 0x2C, (byte) 0x07};
    private final static byte[] CM_MIDI_REC_ON = {(byte) 0xB0, (byte) 0x2C, (byte) 0x46};
    private final static byte[] CM_MIDI_REC_OFF = {(byte) 0xB0, (byte) 0x2C, (byte) 0x06};
    private final static byte[] CM_MIDI_ARMED_ON = {(byte) 0xB0, (byte) 0x2C, (byte) 0x45};
    private final static byte[] CM_MIDI_ARMED_OFF = {(byte) 0xB0, (byte) 0x2C, (byte) 0x05};
    //MIDI MSG for MMC
    private final static byte[] MMC_STOP = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x01, (byte) 0xF7};
    private final static byte[] MMC_PLAY = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x02, (byte) 0xF7};
    private final static byte[] MMC_DEF_PLAY = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x03, (byte) 0xF7};
    private final static byte[] MMC_FFWD = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x04, (byte) 0xF7};
    private final static byte[] MMC_REW = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x05, (byte) 0xF7};
    private final static byte[] MMC_PUNCH_IN = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x06, (byte) 0xF7};
    private final static byte[] MMC_PUNCH_OUT = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x07, (byte) 0xF7};
    private final static byte[] MMC_REC_PAUSE = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x08, (byte) 0xF7};
    private final static byte[] MMC_PAUSE = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x09, (byte) 0xF7};
    private final static byte[] MMC_EJECT = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x0A, (byte) 0xF7};
    private final static byte[] MMC_CHASE = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x0B, (byte) 0xF7};
    private final static byte[] MMC_RESET = {(byte) 0xF0, (byte) 0x7F, DEV_ID, (byte) 0x06, (byte) 0x0D, (byte) 0xF7};
    private long REC_STOP = 0L;
    private boolean HOLD_OFF = false;
    private final static long SAFE_TO_ENTER = 1000 * 10 * 1; //10 Sec
    private static Logger logger = Logger.getLogger(MidiAgent.class);
    private Runnable holdoff_ceaser;
    private List<Transmitter> transmitters = null;
   

    //public MidiAgent() {
    protected void run() {

        this.holdoff_ceaser = new Runnable() {
            public void run() {
                if (HOLD_OFF) {
                    StudioManagerServer.modAgent.updateDAWLCD(isSafeToEnter());
                }

            }
        };

       
    }
    
    protected int init() {
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            try {
                device = MidiSystem.getMidiDevice(infos[i]);

                if (device.getMaxReceivers() == 0) {
                    //does the device have any transmitters?
                    //if it does, add it to the device list
                    logger.debug("checking: " + infos[i].getName());

                    //get all transmitters
                    transmitters = device.getTransmitters();
                    //and for each transmitter

                    for (int j = 0; j < transmitters.size(); j++) {
                        //create a new receiver
                        transmitters.get(j).setReceiver(
                                //using my own MidiInputReceiver
                                new MidiInputReceiver(device.getDeviceInfo().toString()));
                    }

                    Transmitter trans = device.getTransmitter();
                    trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));

                    //open each device
                    device.open();
                    //if code gets this far without throwing an exception
                    //print a success message
                    logger.info("using " + device.getDeviceInfo().getName() + " [" + device.getDeviceInfo().getVendor() + "," + device.getDeviceInfo().getDescription() + "] for MMC");
                }


            } catch (MidiUnavailableException e) {
                logger.warn(e);
            }
            if (this.holdoff_ceaser == null) {
                this.run();
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(holdoff_ceaser, 0, 5, TimeUnit.SECONDS);
            }
            
        }
        return infos.length;
    }
    
    protected void closeAllListener() {
        for (Transmitter tr : transmitters) {
            tr.getReceiver().close();
            tr.close();
        }
   
    }

    private boolean isSafeToEnter() {
        return (System.currentTimeMillis() - this.REC_STOP > SAFE_TO_ENTER);
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public boolean doAction(Action action) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void notifyTrigger(TriggerEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public boolean addListener(TriggerListener toAdd) {
         return listeners.add(toAdd);
    }

    public boolean removeListener(TriggerListener toRemove) {
        return listeners.remove(toRemove);
    }
    
    public boolean hasListener() {
        return (listeners.size() > 0);
    }

    //tried to write my own class. I thought the send method handles an MidiEvents sent to it
    public class MidiInputReceiver implements Receiver {

        public String name;

        public MidiInputReceiver(String name) {
            this.name = name;
        }

        public void send(MidiMessage msg, long timeStamp) {

            byte[] data = msg.getMessage();

            logger.debug("MIDI Status Byte: " + msg.getStatus() + ":" + bytesToHex(data));

            if (msg.getStatus() != ACTIVE_SENSING_STATUS_BYTE && msg.getStatus() != END_OF_SYSEX) {


                if (Arrays.equals(data, CM_MIDI_PLAY)) {
                    //CM MOTORMIX PLAY
                    logger.info("PLAY");
                    HOLD_OFF = false;
                    StudioManagerServer.modAgent.setPlay(true);
                    StudioManagerServer.modAgent.updateDAWLCD(isSafeToEnter());

                }
                if (Arrays.equals(data, CM_MIDI_STOP)) {
                    //CM MOTORMIX STOP
                    logger.info("STOP");
                    HOLD_OFF = true;
                    StudioManagerServer.modAgent.setPlay(false);
                    StudioManagerServer.modAgent.updateDAWLCD(isSafeToEnter());
                }
                if (Arrays.equals(data, CM_MIDI_REC_ON)) {
                    //CM MOTORMIX CM_MIDI_REC_ON
                    logger.info("RECORDING ON");
                    HOLD_OFF = false;
                    StudioManagerServer.modAgent.setRec(true);
                    StudioManagerServer.modAgent.updateDAWLCD(isSafeToEnter());
                }
                if (Arrays.equals(data, CM_MIDI_REC_OFF)) {
                    //CM MOTORMIX CM_MIDI_REC_OFF
                    logger.info("RECORDING OFF");
                    HOLD_OFF = true;
                    REC_STOP = System.currentTimeMillis();
                    StudioManagerServer.modAgent.setRec(false);
                    StudioManagerServer.modAgent.updateDAWLCD(isSafeToEnter());
                }
                if (Arrays.equals(data, CM_MIDI_ARMED_ON)) {
                    //CM MOTORMIX CM_MIDI_REC_OFF                           
                    logger.info("INC ARMED TRACKS");
                    StudioManagerServer.modAgent.incArmedTracks();
                    if (StudioManagerServer.modAgent.getArmedTracks() > 0) {
                        StudioManagerServer.modAgent.updateDAWLCD(isSafeToEnter());
                    }
                }
                if (Arrays.equals(data, CM_MIDI_ARMED_OFF)) {
                    //CM MOTORMIX CM_MIDI_REC_OFF
                    logger.info("DEC ARMED TRACKS");
                    StudioManagerServer.modAgent.decArmedTracks();
                    StudioManagerServer.modAgent.updateDAWLCD(isSafeToEnter());
                }


                if (Arrays.equals(data, MMC_STOP)) {
                    //MMC_STOP
                    logger.info("MCC_STOP");
                }
                if (Arrays.equals(data, MMC_PLAY)) {
                    //MMC_PLAY
                    logger.info("MCC_PLAY");
                }
                if (Arrays.equals(data, MMC_DEF_PLAY)) {
                    //MMC_DEF_PLAY
                    logger.info("MCC_DEF_PLAY");
                }
                if (Arrays.equals(data, MMC_FFWD)) {
                    //MMC_FFWD
                    logger.info("MCC_FFWD");
                }
                if (Arrays.equals(data, MMC_REW)) {
                    //MMC_REW
                    logger.info("MMC_REW");
                }
                if (Arrays.equals(data, MMC_PUNCH_IN)) {
                    //MMC_PUNCH_IN
                    logger.info("MMC_PUNCH_IN");
                }
                if (Arrays.equals(data, MMC_PUNCH_OUT)) {
                    //MMC_PUNCH_OUT
                    logger.info("MMC_PUNCH_OUT");
                }
                if (Arrays.equals(data, MMC_REC_PAUSE)) {
                    //MMC_REC_PAUSE
                    logger.info("MMC_REC_PAUSE");
                }
                if (Arrays.equals(data, MMC_PAUSE)) {
                    //MMC_PAUSE
                    logger.info("MMC_PAUSE");
                }
                if (Arrays.equals(data, MMC_EJECT)) {
                    //MMC_EJECT
                    logger.info("MMC_EJECT");
                }
                if (Arrays.equals(data, MMC_CHASE)) {
                    //MMC_CHASE
                    logger.info("MMC_CHASE");
                }
                if (Arrays.equals(data, MMC_RESET)) {
                    //MMC_RESET
                    logger.info("MMC_RESET");

                }
            }

        }

        public void close() {
        }
    }
}

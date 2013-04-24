/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.r7studio.ioserver;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Level;
import org.hibernate.Query;
import org.hibernate.Transaction;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author leutholl
 */
public class SnomIOServer extends Thread {

    protected static HttpServer httpSnomServer;
    //private final static String IOBOARD_IP = "192.168.1.180";
    //private final static String TEMP_OID = ".1.3.6.1.4.1.32111.1.3.4.10"; //temp
    private static int[] keyAdrMap = {
        1, 2, 3, 4, 5, 6, 7, 8, //page 1 left 1..8
        17, 18, 19, 20, 21, 22, 23, 24, //page 2 left 9..16
        33, 34, 35, 36, 37, 38, 39, 40, //page 3 left 17..24
        9, 10, 11, 12, 13, 14, 15, 16, //page 1 right 25..32
        25, 26, 27, 28, 29, 30, 31, 32, //page 2 right 33..40
        41, 42, 43, 44, 45, 46, 47, 48, //page 3 right 41..48
    };
    private static int[] ledAdrMap = {
        1, 2, 3, 4, 5, 6, 7, 8, //page 1 left 1..8
        25, 26, 27, 28, 29, 30, 31, 32, //page 1 right 9..16
        9, 10, 11, 12, 13, 14, 15, 16, //page 2 left 17..24
        33, 34, 35, 36, 37, 38, 39, 40, //page 2 right 25..32
        17, 18, 19, 20, 21, 22, 23, 24, //page 3 left 33..40
        41, 42, 43, 44, 45, 46, 47, 48, //page 3 right 41..48
    };
    public static boolean terminated = false; //set this to true and I will exit
    protected static final Object TERMINATION_LOCK = new Integer(1);
    protected static final int TIMEOUT = 5; //5 seconds
    private static Logger logger = Logger.getLogger(SnomIOServer.class);
    private HttpParams httpParams = new BasicHttpParams();
    private DefaultHttpClient httpclient;
    private boolean logToLCD;

    public boolean isLogToLCD() {
        return logToLCD;
    }

    public void setLogToLCD(boolean logToLCD) {
        this.logToLCD = logToLCD;
    }

    public SnomIOServer() {
    }

    public void init() {
        Thread.currentThread().setName("SnomIOServer");

        logger.info("Initializing HTTP Server");
        try {
            // Setup HttpServer for Snom Visions to handle SOAP and HTML
            httpSnomServer = HttpServer.create(
                    new InetSocketAddress(8080), 10);
        } catch (IOException ex) {
            logger.error(ex);
        }

        logger.info("...add SOAP context");
        HttpContext httpSnomContext = httpSnomServer.createContext("/",
                new MySnomHtmlHandler());
        httpSnomContext.getFilters().add(new ParameterFilter());

        logger.info("...add CSTA context");
        httpSnomServer.createContext("/csta", new MySnomXmlHandler());

        httpSnomServer.setExecutor(null); //single Threaded server


        //IOBoard List
        List<Ioboard> ioboards = (List<Ioboard>) IOServer.hib_session.createQuery("from Ioboard").list();
        if (ioboards.isEmpty()) {
            logger.warn("No IOBoard registered!");
        } else {
            logger.info("List of IOBoards:");
            for (Ioboard io : ioboards) {
                // handle each IOBoard
                logger.info("|-> connecting to IOBoard at: " + io.getIp() + "...");
                String temp = SnmpAgent.snmpGet(io.getIp(), IOServer.SNMP_PORT, IOServer.SNMP_COMMUNITY, ".1.3.6.1.4.1.32111.1.3.4.10");
                if (temp == null) {
                    logger.error("...|-> can't reach IOBoard with registered IP: " + io.getIp());
                }
                io.setTemp(new Integer(temp));
                IOServer.hib_session.saveOrUpdate(io);
                logger.info("...|-> connected to IOBoard at: " + io.getIp() + " of type " + io.getType() + ". Temperature is " + io.getTemp() + " C.");
            }
        }

        IOServer.hib_session.flush();


        //Channel List
        List<Channel> channels = (List<Channel>) IOServer.hib_session.createQuery("from Channel").list();

        logger.info("List of channels:");
        for (Channel ch : channels) {
            // handle each channel
            logger.info("...address: " + ch.getAddress() + " value: " + ch.getValue() + " action: " + ch.getOid());
        }

        IOServer.hib_session.flush();


    }

    @Override
    public void run() {

        this.setName("SnomIOServer");

        logger.info("Starting HTTP server");
        httpSnomServer.start();

        logger.info("...accepting SOAP,CSTA,HMTL calls on port: " + httpSnomServer.getAddress().getPort());

        try {
            synchronized (TERMINATION_LOCK) {
                while (!terminated) {
                    TERMINATION_LOCK.wait();
                }
            }
            //SHUTDOWN INVOKED HERE!
            logger.info("HTTP server shutting down... Force quiting in " + TIMEOUT + " seconds");
            httpSnomServer.stop(TIMEOUT); //force quite after 5 seconds

            logger.info("Snom server stopped");
        } catch (InterruptedException ex) {
            logger.error(ex);
        }

    }

    private String getXMLByMethod(File method) throws
            FileNotFoundException, IOException {
        String xmlResult = "";
        //find xml file with method as filename
        FileReader fi = new FileReader(method);
        BufferedReader br = new BufferedReader(fi);
        String d;

        while ((d = br.readLine()) != null) {
            xmlResult += d + "\r\n";
        }

        br.close();

        return xmlResult;

    }

    protected synchronized int onSnomKeyPress(int button) {
        //update the channel value
        Transaction t = IOServer.hib_session.beginTransaction();
        Channel ch = (Channel) IOServer.hib_session.get(Channel.class, (short) button);
        short val = ch.getValue();

        logger.debug("onSnomKeyPress pre-DB: button: " + button + " is " + ch.getValue());

        //toggle value
        if (val == 0) {
            ch.setValue((short) 1);
        } else {
            ch.setValue((short) 0);
        }

        //write to DB
        IOServer.hib_session.saveOrUpdate(ch);
        IOServer.hib_session.flush();
        t.commit();

        logger.info("...updated DB with keypress: " + button + " from " + val + " to " + ch.getValue());

        //update LED to all clients
        syncLeds(ch.getAddress());

        //handle IO
        if (!ch.getOid().isEmpty()) {
            //SnmpSet to control Output
            //SnmpAgent.snmpSet(IOBOARD_IP, SNMP_PORT, SNMP_COMMUNITY, ch.getOid() , val);
            //IOBoard
            List<Ioboard> ioboards = (List<Ioboard>) IOServer.hib_session.createQuery("from Ioboard").list();
            if (ioboards.isEmpty()) {
                logger.warn("Can't execute action. no IOBoard registered!");
            } else {
                for (Ioboard io : ioboards) {
                    //we use val as we have to send it inverted and val is still the original value which is inverted.
                    SnmpAgent.snmpSet(io.getIp(), IOServer.SNMP_PORT, IOServer.SNMP_COMMUNITY, ch.getOid(), val);
                }
            }
        }

        logger.debug("onSnomKeyPress post-DB: button: " + button + " is " + ch.getValue());
        if (this.logToLCD) {
            IOServer.modAgent.rollLCD("Key: " + button + ":" + ch.getValue());
        }
        return ch.getValue();

    }

    private boolean syncLeds(short address) {

        List<Snomclient> clients = (List<Snomclient>) IOServer.hib_session.createQuery("from Snomclient").list();
        logger.debug("syncLeds for all hosts with channel: " + address);
        //-------------------------12345678901234567890
        if (this.logToLCD) {
            IOServer.modAgent.rollLCD("syncLEDs for:       ");
        }
        for (Snomclient cl : clients) {
            syncOneOrAllLeds(cl.getIp(), address);
            //-------------------------12345678901234567890
            if (this.logToLCD) {
                IOServer.modAgent.rollLCD("-> " + cl.getIp());
            }
        }
        IOServer.hib_session.flush();
        return true;
    }

    protected String syncOneOrAllLeds(String remoteHostname, short address) {
        //remoteHostname = null: Update all registered Snoms
        //address < 0: Update all LEDs
        //combinations work as well

        String result = "";

        Query query;

        if (address < 0) {
            String hql = "FROM Channel c";
            query = IOServer.hib_session.createQuery(hql);
        } else {
            String hql = "FROM Channel c WHERE c.address = :address_id";
            query = IOServer.hib_session.createQuery(hql);
            query.setParameter("address_id", address);
        }

        List<Channel> channels = (List<Channel>) query.list();

        //for REST call
        if (remoteHostname == null) {
            //return entire list
            for (Channel ch : channels) {
                // handle each channel
                if (ch.getValue() == 1) {
                    //channel is on
                    result += ch.getAddress() + ":ON";
                }
                if (ch.getValue() == 0) {
                    //channel is off
                    result += ch.getAddress() + ":OFF";
                }
                result += ",";
            }
            return result;
        }

        httpclient = new DefaultHttpClient(httpParams);
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT * 1000);

        //else syncLEDs
        logger.debug("syncLeds for host: " + remoteHostname + " channel: " + address);
        for (Channel ch : channels) {
            // handle each channel
            if (ch.getValue() == 1) {
                //channel is on
                //update LED
                controlLED(remoteHostname, ch.getAddress(), ch.getOnMode(), ch.getOnColor());
                result += ch.getAddress() + ":ON";
            }
            if (ch.getValue() == 0) {
                //channel is off
                //update LED
                controlLED(remoteHostname, ch.getAddress(), ch.getOffMode(), ch.getOffColor());
                result += ch.getAddress() + ":OFF";
            }
            result += ",";

        }

        // When HttpClient instance is no longer needed,
        // shut down the connection manager to ensure
        // immediate deallocation of all system resources
        httpclient.getConnectionManager().shutdown();
        IOServer.hib_session.flush();
        return result;
    }

    private boolean registerSnomClient(InetSocketAddress remoteHostname) {
        logger.info("Register Snom client: " + remoteHostname.getHostName());
        
        if (this.logToLCD) {
            //-------------------------12345678901234567890
            IOServer.modAgent.rollLCD("Register Snom:      ");
            IOServer.modAgent.rollLCD("-> " + remoteHostname.getHostName());
        }
        Snomclient client = new Snomclient(remoteHostname.getHostName());
        Transaction t = IOServer.hib_session.beginTransaction();
        IOServer.hib_session.saveOrUpdate(client);
        IOServer.hib_session.flush();
        t.commit();

        return true;
    }

    private void controlLED(String remoteHostname,
            short addr, short freq, short color) {

        logger.info("...controlLED: addr=" + addr + " state=" + freq + " color=" + color + " ip=" + remoteHostname);

        HttpGet method = new HttpGet("http://" + remoteHostname
                + "/ExtensionGuiModule/actionUrlListener");

        URIBuilder urib = new URIBuilder(method.getURI());

        urib.addParameter("local", "");
        urib.addParameter("remote", "");
        urib.addParameter("csta_id", "");
        urib.addParameter("action", "led");
        urib.addParameter("led_nr", "" + channelToLedAddr(addr));

        String m = "";
        switch (freq) {
            case 1:
                m = "On";
                break;
            case 0:
                m = "Off";
                break;
            case 2:
                m = "Blink";
                break;
            case 4:
                m = "BlinkFast";
                break;
            case 3:
                m = "BlinkSlow";
                break;
        }

        urib.addParameter("led_freq", m);

        String c = "";

        switch (color) {
            case 3:
                c = "Red";
                break;
            case 2:
                c = "Orange";
                break;
            case 1:
                c = "Green";
                break;
        }

        urib.addParameter("led_color", c);

        URI uri;
        HttpUriRequest hrb = null;
        try {
            uri = urib.build();
            method.setURI(uri);
            hrb = ((HttpUriRequest) method);
        } catch (URISyntaxException ex) {
            logger.error(ex);
        }


        String uristr = method.getRequestLine().getUri();
        logger.debug("controlLED HTTP-GET: " + uristr);

        HttpResponse response;

        try {
            response = httpclient.execute(hrb);

            // Examine the response status
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("Error in http response: " + response.getStatusLine().getReasonPhrase());
                return;
            }

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();

            // If the response does not enclose an entity, there is no need
            // to worry about connection release
            if (entity != null) {
                InputStream instream = entity.getContent();
                try {

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(instream));
                    // do something useful with the response
                    String s = reader.readLine();
                    if (s != null) {
                        if (s.equals("null") || !s.isEmpty()) {
                            logger.debug("response entity: " + s);
                        }
                    }

                } catch (IOException ex) {

                    // In case of an IOException the connection will be released
                    // back to the connection manager automatically
                    throw ex;

                } catch (RuntimeException ex) {

                    // In case of an unexpected exception you may want to abort
                    // the HTTP request in order to shut down the underlying
                    // connection and release it back to the connection manager.
                    method.abort();
                    throw ex;

                } finally {
                    // Closing the input stream will trigger connection release
                    instream.close();

                }
            }

        } catch (IOException ex) {
            logger.error(ex);
        }

    }

    private class MySnomHtmlHandler implements HttpHandler {

        private int sync = 0; //if two calls to dummy.htm -> sync LEDs

        @Override
        public synchronized void handle(HttpExchange he) throws IOException {


            Thread.currentThread().setName("SnomIOServer:HTML");

            boolean registerKey = false;

            logger.debug("Got HTML request!");
            //printing remote Hostname
            InetSocketAddress remoteHostname = he.getRemoteAddress();
            logger.debug("RemoteAddress: " + remoteHostname.getHostName());

            //printing Header URI
            logger.debug("Request URI: " + he.getRequestURI().toASCIIString());

            logger.debug("Request URI Parameters: ");
            //see if dummy.htm
            if (he.getRequestURI().toASCIIString().equals("/dummy.htm")) {
                sync++;
                logger.debug("dummy.htm called. We're counting to two: " + sync);
            }

            Map params = (Map) he.getAttribute("parameters");

            if (logger.isEnabledFor(Level.DEBUG)) {
                List<String> keys = new ArrayList<String>(params.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    logger.debug("Param [" + key + "]:" + params.get(key));
                }
            }

            int key = 0;
            //look up key: "key" to see the key pressed
            if (params.containsKey("key")) {
                String s = (String) params.get("key");
                try {
                    key = Integer.parseInt(s.substring(1));
                } catch (NumberFormatException ex) {
                    logger.error(ex);
                }
                logger.debug("keypressed: " + key);
                registerKey = true;
            }
            try {
                //printing Request Body
                InputStream is = he.getRequestBody();
                byte[] b = new byte[1500];
                is.read(b);
                logger.debug("Request Body: " + new String(b));
                String response = "OK";
                he.sendResponseHeaders(200, response.length());
                logger.debug("Response: " + response);
                OutputStream os = he.getResponseBody();
                if (os != null) {
                    os.write(response.getBytes());
                    os.close();
                }
            } catch (IOException ex) {
                logger.error(ex);
            }

            /**
             * snom vision calls dummy.htm twice (don't ask why) we should sync
             * the LEDs after the last dummy.htm call since we do not know how
             * the 2nd call is identified we simply have to count to two.
             */
            //do something AFTER sending the initial response!
            if (sync >= 2) {
                logger.warn("Snom client: " + he.getRemoteAddress() + " needs LED sync!");
                syncOneOrAllLeds(he.getRemoteAddress().getHostName(), (short) -1); //syncAll
                sync = 0; //could possibly be removed as handle() quits anyway.
            }

            if (registerKey) {
                logger.info("processing keypress");
                onSnomKeyPress(keyAddrToChannel(key));
            }

        }
    }

    private int keyAddrToChannel(int key_adr) {
        return keyAdrMap[key_adr - 1] - 1;
    }

    private int channelToLedAddr(int ch_adr) {
        return ledAdrMap[ch_adr] - 1;
    }

    private class MySnomXmlHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {

            Thread.currentThread().setName("SnomIOServer:CSTA");

            try {
                //read request
                /**
                 * InputStream is = t.getRequestBody(); System.out.println("Got
                 * request body. Reading request body..."); byte[] b = new
                 * byte[1500]; is.read(b); System.out.println("This is the
                 * request: " + new String(b)); is.reset();
                 *
                 */
                XPathFactory factory = XPathFactory.newInstance();

                XPath xPath = factory.newXPath();
                xPath.setNamespaceContext(new MyNamespaceContext());

                NodeList nodes = (NodeList) xPath.evaluate(
                        "//SOAP-ENV:Envelope/SOAP-ENV:Body/*",
                        new InputSource(t.getRequestBody()),
                        XPathConstants.NODESET);

                String method = "";
                for (int i = 0; i < nodes.getLength(); i++) {
                    method = nodes.item(i).getLocalName();
                    logger.debug("Method Call: " + method);
                }

                //find file with method as file name
                File xml = new File("xml/" + method + ".xml");
                if (xml.exists()) {
                    String response = getXMLByMethod(xml); //working directory
                    t.sendResponseHeaders(200, response.length());
                    OutputStream os = t.getResponseBody();
                    if (os != null) {
                        os.write(response.getBytes());
                        os.flush();
                        os.close();
                        //System.out.println(response);
                    }
                    //register Client if method = getSettings after response successfuly sent
                    //TODO: enrich all params
                    if (method.equalsIgnoreCase("getSettings")) {
                        registerSnomClient(t.getRemoteAddress());
                    }


                } else {
                    String response = "not found";
                    t.sendResponseHeaders(500, response.length());
                    logger.error("Response not found. Check xml!");
                    OutputStream os = t.getResponseBody();
                    if (os != null) {
                        os.write(response.getBytes());
                        os.flush();
                        os.close();
                    }
                }

            } catch (XPathExpressionException ex) {
                logger.error(ex);
            }
        }
    }

    private class MyNamespaceContext implements NamespaceContext {

        void MyNameSpaceContext() {
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("csta")) {
                return "http://www.ecma-international.org/standards/ecma-323/csta/ed4";
            } else if (prefix.equals("SOAP-ENV")) {
                return "http://schemas.xmlsoap.org/soap/envelope/";
            } else {
                return XMLConstants.NULL_NS_URI;
            }
        }

        @Override
        public String getPrefix(String namespace) {
            if (namespace.equals("http://www.ecma-international.org/standards/ecma-323/csta/ed4")) {
                return "csta";
            } else if (namespace.equals("http://schemas.xmlsoap.org/soap/envelope/")) {
                return "SOAP-ENV";
            } else {
                return null;
            }
        }

        @Override
        public Iterator getPrefixes(String namespace) {
            ArrayList list = new ArrayList();

            if (namespace.equals("http://www.ecma-international.org/standards/ecma-323/csta/ed4")) {
                list.add("csta");
            } else if (namespace.equals("http://schemas.xmlsoap.org/soap/envelope/")) {
                list.add("SOAP-ENV");
            }
            return list.iterator();
        }
    }

    private enum Color {

        RED, ORANGE, GREEN
    }

    private enum Freq {

        OFF, ON, BLINK, BLINKFAST, BLINKSLOW
    }
}

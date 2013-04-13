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
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public SnomIOServer() {
        Logger.getLogger(SnomIOServer.class.getName()).setLevel(Level.WARNING);

        //Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                "SnomIOServer starting up");

        Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO, "Starting HTML/CSTA Server");

        try {
            // Setup HttpServer for Snom Visions to handle SOAP and HTML
            httpSnomServer = HttpServer.create(
                    new InetSocketAddress(8080), 10);
        } catch (IOException ex) {
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        httpSnomServer.createContext("/csta", new MySnomXmlHandler());

        HttpContext httpSnomContext = httpSnomServer.createContext("/",
                new MySnomHtmlHandler());
        httpSnomContext.getFilters().add(new ParameterFilter());

        httpSnomServer.setExecutor(null); //use SnomIOServer thread to handle contexts


        //IOBoard List
        List<Ioboard> ioboards = (List<Ioboard>) IOServer.hib_session.createQuery("from Ioboard").list();
        if (ioboards.isEmpty()) {
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.WARNING,
                    "No IOBoard registered!");
        } else {
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "list of IOBoards:");
            for (Ioboard io : ioboards) {
                // handle each IOBoard
                Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                        "reaching IOBoard at: {0}...", io.getIp());
                String temp = SnmpAgent.snmpGet(io.getIp(), IOServer.SNMP_PORT, IOServer.SNMP_COMMUNITY, ".1.3.6.1.4.1.32111.1.3.4.10");
                if (temp == null) {
                    Logger.getLogger(SnomIOServer.class.getName()).log(Level.SEVERE,
                            "can''t reach IOBoard with registered IP:{0}", io.getIp());
                    return;
                }
                io.setTemp(new Integer(temp));
                IOServer.hib_session.saveOrUpdate(io);
                Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                        "successfully reached IOBoard: {0} of Type {1}. Temperature is {2} C",
                        new Object[]{io.getIp(), io.getType(), io.getTemp()});
            }
        }

        IOServer.hib_session.flush();


        //Channel List
        List<Channel> channels = (List<Channel>) IOServer.hib_session.createQuery("from Channel").list();

        Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                "list of channels:");
        for (Channel ch : channels) {
            // handle each channel
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO, "channel: {0} state: {1} action: {2}",
                    new Object[]{ch.getAddress(), ch.getValue(), ch.getOid()});
        }

        IOServer.hib_session.flush();

    }

    @Override
    public void run() {
        
        
        Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO, "Starting SOAP Server");
        httpSnomServer.start();

        Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO, "SOAP Server listening on port: {0}",
                httpSnomServer.getAddress().getPort());
        
        try {
            synchronized (TERMINATION_LOCK) {
                while (!terminated) TERMINATION_LOCK.wait();
            }
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                    "SnomIOServer shutting down... Force quiting in {0} seconds", TIMEOUT);
            System.out.println("SnomIOServer shutting down... Force quiting in " + TIMEOUT + " seconds");          
            httpSnomServer.stop(TIMEOUT); //force quite after 5 seconds
            System.out.println("SnomIOServer stopped.");
        } catch (InterruptedException ex) {
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.SEVERE, null, ex);
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

    protected int onSnomKeyPress(int button) {
        //update the channel value
        Transaction t = IOServer.hib_session.beginTransaction();
        Channel ch = (Channel) IOServer.hib_session.get(Channel.class, (short) button);
        short val = ch.getValue();
        Logger.getLogger(SnomIOServer.class.getName()).log(Level.FINE,
                "registerKeyPress pre-DB: button: {0} is {1}",
                new Object[]{button, ch.getValue()});
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

        //update LED to all clients
        syncLeds(ch.getAddress());

        //handle IO
        if (!ch.getOid().isEmpty()) {
            //SnmpSet to control Output
            //SnmpAgent.snmpSet(IOBOARD_IP, SNMP_PORT, SNMP_COMMUNITY, ch.getOid() , val);
            //IOBoard
            List<Ioboard> ioboards = (List<Ioboard>) IOServer.hib_session.createQuery("from Ioboard").list();
            if (ioboards.isEmpty()) {
                Logger.getLogger(SnomIOServer.class.getName()).log(Level.WARNING,
                        "Can't execute action. no IOBoard registered!");
            } else {
                for (Ioboard io : ioboards) {
                    //we use val as we have to send it inverted and val is still the original value which is inverted.
                    SnmpAgent.snmpSet(io.getIp(), IOServer.SNMP_PORT, IOServer.SNMP_COMMUNITY, ch.getOid(), val);
                }
            }
        }

        Logger.getLogger(SnomIOServer.class.getName()).log(Level.FINE,
                "registerKeyPress post-DB, post-SNMP: button: {0} is {1}",
                new Object[]{button, ch.getValue()});

        return ch.getValue();

    }

    private boolean syncLeds(short address) {

        List<Snomclient> clients = (List<Snomclient>) IOServer.hib_session.createQuery("from Snomclient").list();
        Logger.getLogger(SnomIOServer.class.getName()).log(Level.FINE,
                "syncLeds for all hosts with channel: {0}", address);
        for (Snomclient cl : clients) {
            syncOneOrAllLeds(cl.getIp(), address);
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

        //else syncLEDs
        Logger.getLogger(SnomIOServer.class.getName()).log(Level.FINE,
                "syncLeds for host: {0} channel: {1}", new Object[]{remoteHostname, address});
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

        IOServer.hib_session.flush();

        return result;
    }

    private boolean registerSnomClient(InetSocketAddress remoteHostname) {
        Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                "register client: {0}", remoteHostname.getHostName());
        Snomclient client = new Snomclient(remoteHostname.getHostName());
        Transaction t = IOServer.hib_session.beginTransaction();
        IOServer.hib_session.saveOrUpdate(client);
        IOServer.hib_session.flush();
        t.commit();

        return true;
    }

    private void controlLED(String remoteHostname,
            short addr, short freq, short color) {



        Logger.getLogger(SnomIOServer.class.getName()).log(Level.INFO,
                "controlLED: addr={0} state={1} color={2} ip={3}",
                new Object[]{addr, freq, color,
            remoteHostname});

        // set the connection timeout value to 30 seconds (30000 milliseconds)
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);

        DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);

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
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.SEVERE,
                    null, ex);
        }


        String uristr = method.getRequestLine().getUri();
        Logger.getLogger(SnomIOServer.class.getName()).log(Level.FINE, "controlLED HTTP-GET: {0}", uristr);

        HttpResponse response;

        try {
            response = httpclient.execute(hrb);

            // Examine the response status
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                Logger.getLogger(SnomIOServer.class.getName()).log(Level.SEVERE, response.getStatusLine().getReasonPhrase());
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
                            System.out.println(s);
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

                // When HttpClient instance is no longer needed,
                // shut down the connection manager to ensure
                // immediate deallocation of all system resources
                httpclient.getConnectionManager().shutdown();
            }

        } catch (IOException ex) {
            Logger.getLogger(SnomIOServer.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

    }

    private class MySnomHtmlHandler implements HttpHandler {

        private int sync = 0; //if two calls to dummy.htm -> sync LEDs

        @Override
        public void handle(HttpExchange he) throws IOException {

            Logger.getLogger(MySnomHtmlHandler.class.getName()).setLevel(Level.ALL);

            try {

                boolean registerKey = false;

                Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO, "Got HTML request!");

                //printing remote Hostname
                InetSocketAddress remoteHostname = he.getRemoteAddress();
                Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO, "RemoteAdress: {0}",
                        he.getRemoteAddress().getHostName());

                //printing Header URI
                Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO, "Request URI: {0}",
                        he.getRequestURI().toASCIIString());
                Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO, "Request URI Parameters: ");

                //see if dummy.htm
                if (he.getRequestURI().toASCIIString().equals("/dummy.htm")) {
                    sync++;
                    Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO,
                            "dummy.htm called. We're counting to two: {0}", sync);
                }

                Map params = (Map) he.getAttribute("parameters");
                List<String> keys = new ArrayList<String>(params.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.FINE,
                            "Param [{0}]:{1}", new Object[]{key, params.get(key)});
                }

                int key = 0;
                //look up key: "key" to see the key pressed
                if (params.containsKey("key")) {
                    String s = (String) params.get("key");
                    try {
                        key = Integer.parseInt(s.substring(1));
                    } catch (NumberFormatException ex) {
                        Logger.getLogger(SnomIOServer.class.getName()).log(
                                Level.SEVERE, null, ex);
                    }
                    Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO,
                            "keypressed: {0}", key);
                    registerKey = true;
                }



                //printing Request Body
                InputStream is = he.getRequestBody();
                byte[] b = new byte[1500];
                is.read(b);
                Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO,
                        "Request Body: {0}", new String(b));

                String response = "OK";
                he.sendResponseHeaders(200, response.length());
                Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.INFO,
                        "Response: {0}", response);
                OutputStream os = he.getResponseBody();
                if (os != null) {
                    os.write(response.getBytes());
                    os.close();
                }

                /**
                 * snom vision calls dummy.htm twice (don't ask why) we should
                 * sync the LEDs after the last dummy.htm call since we do not
                 * know how the 2nd call is identified we simply have to count
                 * to two.
                 */
                //do something AFTER sending the initial response!
                if (sync >= 2) {
                    Logger.getLogger(MySnomHtmlHandler.class.getName()).log(Level.WARNING,
                            "client: {0} needs LED sync!", he.getRemoteAddress());
                    syncOneOrAllLeds(he.getRemoteAddress().getHostName(), (short) -1); //syncAll
                    sync = 0; //could possibly be removed as handle() quits anyway.
                }

                if (registerKey) {
                    onSnomKeyPress(keyAddrToChannel(key));
                }

            } catch (IOException ex) {
                Logger.getLogger(MySnomHtmlHandler.class.getName()).log(
                        Level.SEVERE, null, ex);
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


            Logger.getLogger(MySnomXmlHandler.class.getName()).setLevel(Level.ALL);

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
                    Logger.getLogger(MySnomXmlHandler.class.getName()).log(
                            Level.INFO, "Method Call: {0}", method);
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
                        System.out.println(response);
                    }
                    //register Client if method = getSettings after response successfuly sent
                    //TODO: enrich all params
                    if (method.equalsIgnoreCase("getSettings")) {
                        registerSnomClient(t.getRemoteAddress());
                    }


                } else {
                    String response = "not found";
                    t.sendResponseHeaders(500, response.length());
                    Logger.getLogger(MySnomXmlHandler.class.getName()).log(
                            Level.SEVERE, "Response not found. Check xml!");
                    OutputStream os = t.getResponseBody();
                    if (os != null) {
                        os.write(response.getBytes());
                        os.flush();
                        os.close();
                    }
                }



            } catch (XPathExpressionException ex) {
                Logger.getLogger(MySnomXmlHandler.class.getName()).log(
                        Level.SEVERE, null, ex);
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

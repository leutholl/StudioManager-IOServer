/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager;

import ch.r7studio.studiomanager.actions.Action;
import ch.r7studio.studiomanager.actions.CodedSnomLedAction;
import ch.r7studio.studiomanager.actions.SnomLedAction;
import ch.r7studio.studiomanager.actions.SnomLedColor;
import ch.r7studio.studiomanager.actions.SnomLedFreq;
import ch.r7studio.studiomanager.pojo.Snomvisionbutton;
import ch.r7studio.studiomanager.pojo.Snomvision;
import ch.r7studio.studiomanager.triggers.SnomKeyTrigger;
import ch.r7studio.studiomanager.triggers.TriggerEvent;
import ch.r7studio.studiomanager.triggers.TriggerListener;
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
import org.apache.log4j.Level;
import org.hibernate.Query;
import org.hibernate.Transaction;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author leutholl
 */
public class SnomAgent extends Thread implements Agent {

    
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
    
    protected static final  Object                  TERMINATION_LOCK = new Integer(1);
    protected static final  int                     TIMEOUT          = 5; //5 seconds
    public    static        boolean                 terminated       = false; //set this to true and I will exit
    private   static        Logger                  logger           = Logger.getLogger(SnomAgent.class);
    protected static        HttpServer              httpSnomServer;
    private                 boolean                 logToLCD;
    private                 List<TriggerListener>   listeners        = new ArrayList<TriggerListener>();
    
    public boolean isLogToLCD() {
        return logToLCD;
    }

    public void setLogToLCD(boolean logToLCD) {
        this.logToLCD = logToLCD;
    }

    public SnomAgent() {
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
        httpSnomContext.getFilters().add(new SnomAgentParameterFilter());

        logger.info("...add CSTA context");
        httpSnomServer.createContext("/csta", new MySnomXmlHandler());

        httpSnomServer.setExecutor(null); //single Threaded server


        //SnomvisionButton List
        List<Snomvisionbutton> channels = (List<Snomvisionbutton>) StudioManagerServer.hib_session.createQuery("from Snomvisionbutton").list();

        logger.info("List of channels:");
        for (Snomvisionbutton button : channels) {
            // handle each channel
            logger.info("...address: " + button.getAddress() + " value: " + button.getValue());
        }

        StudioManagerServer.hib_session.flush();


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

    private boolean registerSnomClient(InetSocketAddress remoteHostname) {
        logger.info("Register Snom client: " + remoteHostname.getHostName());

        if (this.logToLCD) {
            //-------------------------12345678901234567890
            StudioManagerServer.modAgent.rollLCD("Register Snom:      ");
            StudioManagerServer.modAgent.rollLCD("-> " + remoteHostname.getHostName());
        }
        Snomvision client = new Snomvision(remoteHostname.getHostName());
        Transaction t = StudioManagerServer.hib_session.beginTransaction();
        StudioManagerServer.hib_session.saveOrUpdate(client);
        StudioManagerServer.hib_session.flush();
        t.commit();

        return true;
    }

    protected URI createURI(HttpGet method,
            short addr, short freq, short color) {

        logger.info("...controlLED: addr=" + addr + " state=" + freq + " color=" + color);

        URIBuilder urib = new URIBuilder(method.getURI());


        urib.addParameter("local", "");
        urib.addParameter("remote", "");
        urib.addParameter("csta_id", "");
        urib.addParameter("action", "led");
        urib.addParameter("led_nr", "" + buttonToLedAddr(addr));

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

        URI uri = null;
        try {
            uri = urib.build();
        } catch (URISyntaxException ex) {
            logger.warn(ex);
        }


        return uri;
    }


    private Snomvisionbutton getSnomvisionbutton(int address) {
        Query query;
        String hql = "FROM Snomvisionbutton c WHERE c.address = :address_id";
        query = StudioManagerServer.hib_session.createQuery(hql);
        query.setParameter("address_id", address);
        return (Snomvisionbutton) query.uniqueResult();
    }

    private List<Snomvisionbutton> getAllSnomvisionbuttons() {
        Query query;
        String hql = "FROM Snomvisionbutton b";
        query = StudioManagerServer.hib_session.createQuery(hql);
        return (List<Snomvisionbutton>) query.list();
    }

    private List<Snomvision> querySnomvision(String ip) {
        Query query;

        if (ip.equals("all")) {
            String hql = "FROM Snomvision s";
            query = StudioManagerServer.hib_session.createQuery(hql);
        } else {
            String hql = "FROM Snomvision s WHERE s.ip = :ip";
            query = StudioManagerServer.hib_session.createQuery(hql);
            query.setParameter("ip", ip);
        }
        return (List<Snomvision>) query.list();

    }

    protected Snomvisionbutton updateSnomvisionButton(int address, SnomLedFreq freq, SnomLedColor color) {
        Snomvisionbutton b = getSnomvisionbutton(address);
        short value = (short) (freq.getCode() * 10 + color.getCode());
        b.setValue(value);
        Transaction t = StudioManagerServer.hib_session.beginTransaction();
        StudioManagerServer.hib_session.saveOrUpdate(b);
        StudioManagerServer.hib_session.flush();
        t.commit();
        return b;
    }

    protected Snomvisionbutton updateCodedSnomvisionButton(int address, int dbValue) {
        Snomvisionbutton b = getSnomvisionbutton(address);
        short value = (short) dbValue;
        b.setValue(value);
        Transaction t = StudioManagerServer.hib_session.beginTransaction();
        StudioManagerServer.hib_session.saveOrUpdate(b);
        StudioManagerServer.hib_session.flush();
        t.commit();
        return b;
    }

    public boolean doAction(Action action) {
        List<Snomvisionbutton> buttons = new ArrayList<Snomvisionbutton>();
        boolean success = true;
        switch (action.getActiontype()) {
            case SnomLED: {
                SnomLedAction a = (SnomLedAction) action;
                //safe new "button" in DB
                Snomvisionbutton b = updateSnomvisionButton(a.getAddress(), a.getFreq(), a.getColor());
                //then send it to the snom(s)
                success &= buttons.add(b);
                success &= updateLED(querySnomvision("all"), buttons);
                break;
            }
            case CodedSnomLED: {
                CodedSnomLedAction a = (CodedSnomLedAction) action;
                //safe new "button" in DB
                Snomvisionbutton b = updateCodedSnomvisionButton(a.getKey(), a.getValue());
                //then send it to the snom(s)
                success &= buttons.add(b);
                success &= updateLED(querySnomvision("all"), buttons);
                break;
            }
        }
        return success;
    }

    protected boolean updateLED(List<Snomvision> visions, List<Snomvisionbutton> buttons) {

        DefaultHttpClient client = new DefaultHttpClient();
        boolean success = true;
        for (Snomvision vision : visions) {
            for (Snomvisionbutton led : buttons) {
                //method
                HttpGet method = new HttpGet("http://" + vision.getIp()
                        + "/ExtensionGuiModule/actionUrlListener");
                //uri
                URI uri = createURI(method, (short) led.getAddress(), (short) (led.getValue() / 10), (short) (led.getValue() % 10));

                method.setURI(uri);
                HttpUriRequest hrb = ((HttpUriRequest)method);
                success &= doReqest(client, hrb);
            }
        }
        
        return success;

    }

    private boolean doReqest(DefaultHttpClient client, HttpUriRequest hrb) {

        HttpResponse response;

        try {

            response = client.execute(hrb);

            // Examine the response status
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("Error in http response: " + response.getStatusLine().getReasonPhrase());
                return false;
            }

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();

            // If the response does not enclose an entity, there is no need
            // to worry about connection release
            if (entity != null) {
                logger.debug("entity != null");
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
                    logger.debug("ioException");
                    throw ex;

                } catch (RuntimeException ex) {

                    // In case of an unexpected exception you may want to abort
                    // the HTTP request in order to shut down the underlying
                    // connection and release it back to the connection manager.
                    logger.debug("method.abort");
                    return false;

                } finally {
                    // Closing the input stream will trigger connection release
                    logger.debug("close");
                    instream.close();
                }
            }

        } catch (IOException ex) {
            logger.error(ex);
        }
        return true;
    }

    private int keyAddrToButton(int key_adr) {
        return keyAdrMap[key_adr - 1] - 1;
    }

    private int buttonToLedAddr(int ch_adr) {
        return ledAdrMap[ch_adr] - 1;
    }

    public boolean addListener(TriggerListener toAdd) {
        return listeners.add(toAdd);
    }

    public boolean removeListener(TriggerListener toRemove) {
        return listeners.remove(toRemove);
    }

    public void notifyTrigger(TriggerEvent event) {
        for (TriggerListener tl : listeners) {
            tl.handleTrigger(event);
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
                boolean success = updateLED(querySnomvision(he.getRemoteAddress().getHostName()),
                        getAllSnomvisionbuttons());
           
                logger.info("success of all updateLED(): "+success);
                
                sync = 0; //could possibly be removed as handle() quits anyway.
            }

            if (registerKey) {
                logger.info("processing keypress");
                notifyTrigger(new TriggerEvent(this, 
                        new SnomKeyTrigger(keyAddrToButton(key), 
                            he.getRemoteAddress().getHostName())));
            }

        }
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
}

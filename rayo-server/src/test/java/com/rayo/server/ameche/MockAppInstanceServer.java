package com.rayo.server.ameche;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import org.dom4j.Element;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

public class MockAppInstanceServer {

    public class ResponseHandler implements Container {

        public void handle(Request req, Response response) {
            
            hits++;
            
            String data = null;
            try {
                if (req.getContentLength() > 0) {
                    data = req.getContent();
                }
            }
            catch (IOException e) {
                throw new IllegalStateException(e);                
            }

            if(expectedData.equals(data)) {

                dataMatch = true;

                if(expectedHeaders != null) {
                    for(String key : expectedHeaders.keySet()) {
                        String headerValue = req.getValue(key);
                        if(!expectedHeaders.get(key).equals(headerValue)) {
                            System.err.println("Header mismatch for " + key + " [expected:" + expectedHeaders.get(key) + ", got:" +  headerValue + "]");
                            dataMatch = false;
                            break;
                        }
                    }
                }
                
                if(responseDelay > 0) {
                    try {
                        Thread.sleep(responseDelay);
                    }
                    catch (InterruptedException e) {
                    }
                }

                if(forceFail) {
                    response.setCode(404);
                }
                else {
                    response.setCode(203);
                }
                
                PrintStream body = null;
                try {
                    body = response.getPrintStream();
                }
                catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                
                body.print(response);
                body.close();
                
            }
            else {
                response.setCode(500);
                response.set("Content-Type", "text/plain;charset=utf-8");
                PrintStream body;
                try {
                    body = response.getPrintStream();
                    body.print("Received unexpected request " + req.getMethod() + ":" + req.getTarget() + " with data: " + data);
                    body.close();
                }
                catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

    }

    public int port;
    public boolean dataMatch;
    
    private Connection connection;
    private ResponseHandler handler;
    
    int hits = 0;
    int expectedHits = 1;
    
    String expectedData;
    int responseDelay;
    boolean forceFail;
    private Map<String, String> expectedHeaders;

    public MockAppInstanceServer(int port) throws IOException {
        this.port = port;
        handler = new ResponseHandler();
        connection = new SocketConnection(handler);
        SocketAddress address = new InetSocketAddress(port);
        connection.connect(address);
    }

    public void stop() throws Exception {
        connection.close();
    }

    public MockAppInstanceServer expect(Element data) {
        return expect(data, null, 0);
    }

    public MockAppInstanceServer expect(Element data, Map<String, String> headers) {
        return expect(data, headers, 0);
    }

    public MockAppInstanceServer expect(Element data, Map<String, String> headers, int responseDelay) {
        reset();
        this.expectedData = data.asXML();
        this.expectedHeaders = headers;
        this.responseDelay = responseDelay;
        return this;
    }

    public MockAppInstanceServer forceFail() {
        this.forceFail = true;
        return this;
    }
    
    public void expectNothing() {
        reset();
        this.expectedHits = 0;
    }

    public void verify() {
        if(expectedHits == 0 && hits == 0) {
            return;
        }
        
        if(expectedHits != hits) {
            throw new IllegalStateException("Received strange number of hits [port=" + port + ", hits=" + hits + "]");
        }
        
        if(!dataMatch) {
            throw new IllegalStateException("Request did not match [port=" + port + "]");
        }
    }
    
    private void reset() {
        this.hits = 0;
        this.dataMatch = false;
        this.forceFail = false;
        this.expectedHits = 1;
        this.expectedData = null;
        this.expectedHeaders = null;
    }


}

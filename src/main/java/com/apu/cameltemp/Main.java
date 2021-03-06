/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apu.cameltemp;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import com.apu.cameltemp.converter.JsonSerializer;
import com.apu.TcpServerForAccessControlAPI.packet.AccessPacket;
import com.apu.TcpServerForAccessControlAPI.packet.EventType;
import java.util.Date;
import java.util.concurrent.Future;

/**
 *
 * @author apu
 */
public class Main {
    
    private static final int CONNECTION_PORT = 65530;
    private static final String CONNECTION_HOST = "127.0.0.1";
    
    public static void main(String[] args) throws Exception { 
        
        int deviceNumber = 15;
        int packetNumber = 25;
        AccessPacket packet = new AccessPacket();
        packet.setEventId(EventType.ENTER_QUERY.getIndex());
        packet.setDeviceNumber(deviceNumber); 
        packet.setCardNumber("11111111");
        packet.setPacketNumber(packetNumber++);
        packet.setTime(new Date());
        
        JsonSerializer serializer = new JsonSerializer();
        
        byte[] packetBytes;
        byte[] packetBytesForSend; 
        packetBytes = serializer.serializeBytes(packet);
        packetBytesForSend = new byte[packetBytes.length + 2];
        int i = 0;
        for(i=0; i<packetBytes.length; i++) {
            packetBytesForSend[i] = packetBytes[i];
        }
        packetBytesForSend[i++] = '\r';
        packetBytesForSend[i++] = '\n';
        String sendStr = new String(packetBytesForSend);
        
                CamelContext context = new DefaultCamelContext();        
                try {  
                    Processor httpRouteProcessor = new Processor() {
                        public void process(Exchange exchange) throws Exception {
                          String pktdata = (String)exchange.getIn().getHeader("pktdata");
                          String name = (String)exchange.getIn().getHeader("name");
                          
                          ProducerTemplate template = exchange.getContext().createProducerTemplate();
                          
                          Future<Object> future = template.asyncRequestBody(
                                  "netty:tcp://localhost:65530?sync=true&textline=true", 
                                  sendStr);
                          while(!future.isDone()) {
                            System.out.println("Doing something else while processing..." + future);
                            Thread.sleep(5);
                          }
                          String response = (String) future.get();
                          
                          exchange.getOut().setBody(pktdata + "\r\n" 
                              + name + "\r\n"
                              + response
                              );
                          template.stop();
                        }
                      };
                    
                    RouteBuilder httpRouteBuilder = new RouteBuilder() {
                        public void configure() {
                            from("jetty://http://localhost:8095/add.php") 
                            .threads()
                            .log("Received a request")  
                            .process(httpRouteProcessor);
                          }
                      }; 
       
                    context.addRoutes(httpRouteBuilder);               
                    context.start();        
                    while(true) {}       
                } finally {        
                    context.stop();        
                }        
            }
    
}

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

/**
 *
 * @author apu
 */
public class Main {
    
    public static void main(String[] args) throws Exception {        
                CamelContext context = new DefaultCamelContext();        
                try {  
                    Processor httpRouteProcessor = new Processor() {
                        public void process(Exchange exchange) throws Exception {
                          String pktdata = (String)exchange.getIn().getHeader("pktdata");
                          String name = (String)exchange.getIn().getHeader("name");
                          
                          ProducerTemplate template = exchange.getContext().createProducerTemplate();

                          Exchange exchangeInner = template.request("http4://www.google.com/search", new Processor() {
                              public void process(Exchange exchange2) throws Exception {
                                  exchange2.getIn().setHeader(Exchange.HTTP_QUERY, ("hl=en&q=activemq"));
                              }
                          });
                          Message out = exchangeInner.getOut();
                          
                          exchange.getOut().setBody(pktdata + "\r\n" 
                              + name + "\r\n"
                              + out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) + "\r\n"
                              + out.getBody()
                              );
                        }
                      };
                    
                    RouteBuilder httpRouteBuilder = new RouteBuilder() {
                        public void configure() {
                            from("jetty://http://localhost:8095/add.php")  
                            .log("Received a request")  
                            .process(httpRouteProcessor);
//                            .to("seda:incoming");
                          }
                      };  
    
                      
                      
//                    RouteBuilder tcpInputRouteBuilder = new RouteBuilder() {
//                          public void configure() {
//                              from("seda:incoming")  
//                              .log("Received a request")  
////                              .process(httpRouteProcessor)
//                              .to("seda:outgoing");
//                            }
//                        };
//                        
//                    RouteBuilder tcpOutputRouteBuilder = new RouteBuilder() {
//                            public void configure() {
//                                from("seda:outgoing")  
//                                .log("Received a request")  
////                                .process(httpRouteProcessor)
//                                .to("seda:outgoing");
//                              }
//                          };
                    
                    
//                    context.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"));        
                    context.addRoutes(httpRouteBuilder);        
//                    ProducerTemplate template = context.createProducerTemplate();        
                    context.start();        
//                    template.sendBody("activemq:test.queue", "Hello World");
                    while(true) {}
//                    Thread.sleep(2000);        
                } finally {        
                    context.stop();        
                }        
            }
    
}

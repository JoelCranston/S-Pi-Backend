package edu.pdx.spi.verticles;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class GcmRegistrationServer extends AbstractVerticle {
    int port = 9996;
    EventBus eb;

    @Override
    public void start() {
        eb = vertx.eventBus();

        System.out.println("Google Cloud listening server starting");

        // create listening server
        NetServer gcmServer = vertx.createNetServer();

        // handle incoming GCM Registration tokens from clients
        gcmServer.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(NetSocket sock) {
                sock.handler(new Handler<Buffer>(){
                    @Override
                    public void handle(Buffer buffer) {
                        // code for unregistering GCM recipients
//                        String cmd = buffer.getString(0, 4);
//                        System.out.println("GCM Command recieved: " + cmd); 
//                        String regToken = buffer.getString(4, buffer.length());
//                        System.out.println("GCM Token recieved: " + regToken);
//                        sock.close();
//                        if(cmd.contains("STOP")){
//                            eb.send("stopGcmToken",regToken);
//                        }
//                        if(cmd.contains("STRT")){
//                        // send registration token to alerts verticle
//                            eb.send("newGcmToken", regToken);
//                        }
                        //send responce.
                        sock.write("Recieved");
                        sock.close();    
                        String regToken = buffer.getString(0, buffer.length());
                        eb.send("newGcmToken", regToken);

                    }
                });
            }
        }).listen(port, "0.0.0.0");


    }
}

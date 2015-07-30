package edu.pdx.spi.handlers;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.awt.Toolkit;
import java.util.Base64;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class WatchWebSocketHandler implements Handler<ServerWebSocket> {
    private boolean active;
    private ServerWebSocket ws;
    SendTimer st;
    @Override
    public void handle(ServerWebSocket e) {
        ws = e;
        //this handler is called on incoming data.
        e.handler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buf) {
                String s = buf.getString(0, buf.length());
                System.out.print(s + '\n');
                if (s.contentEquals("send")) {
                    //sends data through the socket.
                    //e.writeBinaryMessage(buf);
                    active = true;
                    
                    e.writeFinalTextFrame("Starting data flow");
                    send(e);
                }
                if (s.contentEquals("stop")) {
                    //sends data through the socket.
                    //e.writeBinaryMessage(buf);
                    active = false;
                    st.timer.cancel();
                    e.writeFinalTextFrame("Stoping data flow");
                }

            }

        });
        //only performed on inital connection
        e.writeFinalTextFrame("Connected");
        //e.write(Buffer.buffer("WebSocket Connected"));

    }
    private void send(ServerWebSocket e) {
        long startTime = System.currentTimeMillis();
        JsonObject data = new JsonObject();
        JsonArray jsonArr = new JsonArray();
        Random rn = new Random(System.currentTimeMillis());
        if (active) {            
            for (int i = 0; i < 3; i++) {
                JsonObject json = new JsonObject();
                json.put("TS", startTime + 8 * i);
                json.put("SIGNAL", Math.abs(rn.nextGaussian()) * 25);
                jsonArr.add(json);
            }
            //e.writeFinalBinaryFrame(Buffer.buffer(data.encode()));
            e.writeFinalTextFrame(jsonArr.toString());
            st = new SendTimer(1);
        }
    }
    public class SendTimer {

        Toolkit toolkit;

        Timer timer;

        public SendTimer(int seconds) {
            toolkit = Toolkit.getDefaultToolkit();
            timer = new Timer();
            timer.schedule(new SendTask(), seconds * 1000);
        }

        class SendTask extends TimerTask {

            public void run() {
                send(ws);
                //timer.cancel(); //Not necessary because we call System.exit
            }
        }
    }

}

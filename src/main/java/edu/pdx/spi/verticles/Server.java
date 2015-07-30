package edu.pdx.spi.verticles;

import edu.pdx.spi.handlers.AlertMonitorHandler;
import edu.pdx.spi.handlers.StreamingHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.CorsHandler;
import static edu.pdx.spi.ChannelNames.*;
import edu.pdx.spi.handlers.*;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import java.util.ArrayList;

public class Server extends AbstractVerticle {
  EventBus eb;

  @Override
  public void start() {
    int port = this.config().getInteger("vertxPort");
    String hostname = this.config().getString("vertxHost");


    Router router = Router.router(vertx);
    router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET));
    router.route().handler(BodyHandler.create());
    eb = vertx.eventBus();

    // Patient data routes
    PatientsHandler ph = new PatientsHandler();
    router.route("/patients/:id").handler(ph);
    router.route("/patients").handler(ph);

    // Alerts
    AlertMonitorHandler ah = new AlertMonitorHandler();
    router.route("/alerts/:id").handler(ah);
    // Route for handling BigDawg posted replies
    router.post("/alerts/incoming").handler(ah);

    // Numerical stream endpoints
    StreamingHandler streamHandle = new StreamingHandler();
    router.route("/stream/numerical/:type/:id").handler(streamHandle);
    // Waveform stream endpoints
    router.route("/stream/waveform/:type/:id").handler(streamHandle);

    // WebSockets endpoints
    WatchWebSocketHandler webSocketHandle = new WatchWebSocketHandler();
    //WatchHandler watchHttpHandle = new WatchHandler();
    //router.route("/websocket/waveform/:type/:id").handler(watchHttpHandle);
    // Bridged eventbus permissions
    BridgeOptions options = new BridgeOptions();
    options.addOutboundPermitted(new PermittedOptions().setAddressRegex(".+\\..+"));
    options.addOutboundPermitted(new PermittedOptions().setAddress("alerts"));

    router.route("/streambus/*").handler(SockJSHandler.create(vertx).bridge(options, event -> {
      switch (event.type()) {
        case SOCKET_CREATED:
          System.out.println("Socket created.");
          break;
        case SOCKET_CLOSED:
          System.out.println("Socket closed.");
          // Socket got closed, trigger the end event.
          eb.send(STREAM_END, event.socket().headers().get("X-Real-IP"));
          break;
        case RECEIVE:
          break;
        case SEND:
          break;
      }

      event.complete(true);
    }));

    vertx.createHttpServer()
        .requestHandler(router::accept)//normal http requests.
        .websocketHandler(webSocketHandle)//needed for handling websocket requests.
        .listen(port, hostname);
    
    System.out.println("Server started on " + hostname + ":" + port);
  }
}

package edu.pdx.spi.verticles;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pdx.spi.GcmContent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import io.vertx.core.json.JsonObject;
import java.util.Random;


public class Alerts extends AbstractVerticle {
    EventBus eb;
    int port = 9994;
    public static final String API_KEY = "AIzaSyBe-3d2hooDYXHHQt5Rb8Qo4wn0vRVRMNE";
    String UserKey;

    // alert sending interval
    int interval = 20000;


  @Override
  public void start() {
      // TODO: delete inactive registration numbers?
      // (maintain mapping of active users instead of just ones who've registered...)

      // GCM registration numbers
      List<String> userGcmRegistrationTokens = new LinkedList<String>();

      eb = vertx.eventBus();

      System.out.println("running alerts simulation, sending every " + interval + " milliseconds");

      // handle new registration tokens from android app
      eb.consumer("newGcmToken", message -> {
          String userRegToken = message.body().toString();
          for(String s: userGcmRegistrationTokens){
              if(s.contentEquals(userRegToken)){
                  System.out.println("User already Registered: " + userRegToken);
                  return;
              }
          }
          // add new token to list
          System.out.println("Received New User Registration: " + userRegToken);
          if(userRegToken != null){
            userGcmRegistrationTokens.add(userRegToken);
          }
      });
      //handle registration stop messagages.
      eb.consumer("stopGcmToken", message -> {
          String userRegToken = message.body().toString();
          for(int i = 0; i < userGcmRegistrationTokens.size();i++){
              if(userGcmRegistrationTokens.get(i).contentEquals(userRegToken)){
                  userGcmRegistrationTokens.remove(i);
                  System.out.println("Removed User Registration: " + userRegToken);
              }
          }
      });
      
      // Sends a new alert every 20 seconds
      vertx.setPeriodic(interval, id -> {
          
          // create the content, should be from eventbus
          if(userGcmRegistrationTokens.isEmpty()){
              return;
          }
          GcmContent content = createContent();
          for(String t :userGcmRegistrationTokens){
              content.addRegId(t);
          }
          // Send message to all the users
          sendMessage(content, API_KEY);
    });
  }

    // creates message content to be sent to watch
    public GcmContent createContent(){
        Random rn;
        rn = new Random(System.currentTimeMillis());
        GcmContent c = new GcmContent();
        JsonObject alert = new JsonObject();
        alert.put("PATIENT_ID", rn.nextInt(4));
        alert.put("TS", System.currentTimeMillis());
        alert.put("SIGNAME", "ABP");
        alert.put("INTERVAL", rn.nextInt(7));
        alert.put("ALERT_MSG", "This is a Alert");
        alert.put("ACTION_MSG", "Do something!");
        c.createData("SPI ALERT",alert.toString()); //title,message
        return c;
    }
    
    private void sendMessage(GcmContent content, String apiKey) {
        try{

            // 1. URL
            URL url = new URL("https://android.googleapis.com/gcm/send");

            // 2. Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 3. Specify POST method
            conn.setRequestMethod("POST");

            // 4. Set the headers
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "key="+apiKey);

            conn.setDoOutput(true);

            // 5. Add JSON data into POST request body

            // 5.1 Use Jackson object mapper to convert Content object into JSON
            ObjectMapper mapper = new ObjectMapper();

            // 5.2 Get connection output stream
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

            // 5.3 Copy Content "JSON" into
            mapper.writeValue(wr, content);

            // 5.4 Send the request
            wr.flush();

            // 5.5 close
            wr.close();

            // 6. Get the response
            int responseCode = conn.getResponseCode();
            // System.out.println("\nSending 'POST' request to URL : " + url);
            // System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 7. Print result
            // System.out.println(response.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

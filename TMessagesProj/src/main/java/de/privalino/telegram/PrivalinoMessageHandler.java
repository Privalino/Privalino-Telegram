package de.privalino.telegram;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.R.id.message;

/**
 * Created by erbs on 01.08.17.
 */

public class PrivalinoMessageHandler {
    public static JSONObject handleOutgoingMessage(TLRPC.Message messageObject) throws IOException, JSONException {
        int toChatId = messageObject.to_id.chat_id;
        int toUserId = messageObject.to_id.user_id;
        int from = messageObject.from_id;
        int messageId = messageObject.id;
        boolean outgoingMessage = true;

        URL url = new URL("http://35.156.90.81:8080/server-webogram/protection");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        String input = "{\"sender\":" + from + ",\"senderUserName\":\"" + String.valueOf(from) + "\",\"senderName\":\"" + String.valueOf(from) + "\",\"id\":" + messageObject.id + ",\"channel\":\"" + privalino_channel + "\",\"text\":\"" + messageObject.message + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(input.getBytes());
        os.flush();


        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));


        JSONObject privalinoRating = new JSONObject(br.readLine());
        conn.disconnect();
        return privalinoRating;
    }

    public static JSONObject handleIncomingMessage(TLRPC.Message messageObject) throws IOException, JSONException {

        int from = messageObject.from_id;
        String fromName = getUser(UserConfig.getClientUserId()).first_name + " " + getUser(UserConfig.getClientUserId()).last_name;
        String fromUserName = getUser(UserConfig.getClientUserId()).username;
        ;
        int to = UserConfig.getClientUserId();

        // Channel immer gleich machen. Immer kleinere ID vorne.
        String privalino_channel = Math.min(from, to) + "_" + Math.max(from, to);

        URL url = new URL("http://35.156.90.81:8080/server-webogram/protection");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        String input = "{\"sender\":" + from + ",\"senderUserName\":\"" + fromUserName + "\",\"senderName\":\"" + fromName + "\",\"id\":" + message.id + ",\"channel\":\"" + privalino_channel + "\",\"text\":\"" + message.message + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(input.getBytes());
        os.flush();


        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        String serverResponse = br.readLine();

        conn.disconnect();

        JSONObject privalinoFeedback = new JSONObject(serverResponse);
        Log.d("[Privalino]", privalinoFeedback.toString());

        return privalinoFeedback;
    }
}

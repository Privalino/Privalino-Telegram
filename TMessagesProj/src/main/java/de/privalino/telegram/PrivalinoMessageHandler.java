package de.privalino.telegram;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.R.id.message;
import static android.app.PendingIntent.getActivity;

/**
 * Created by erbs on 01.08.17.
 */

public class PrivalinoMessageHandler extends DialogFragment {
    public static JSONObject handleOutgoingMessage(TLRPC.Message messageObject) throws IOException, JSONException {
        int toChatId = messageObject.to_id.chat_id;
        int toUserId = messageObject.to_id.user_id;
        int from = messageObject.from_id;
        int messageId = messageObject.id;
        boolean outgoingMessage = true;
        String privalino_channel = from < toUserId ? from + "_" + toUserId : toUserId + "_" + from;

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
        String fromName = MessagesStorage.getInstance().getUser(UserConfig.getClientUserId()).first_name + " " + MessagesStorage.getInstance().getUser(UserConfig.getClientUserId()).last_name;
        String fromUserName = MessagesStorage.getInstance().getUser(UserConfig.getClientUserId()).username;

        int to = UserConfig.getClientUserId();

        // Channel immer gleich machen. Immer kleinere ID vorne.
        String privalino_channel = Math.min(from, to) + "_" + Math.max(from, to);

        URL url = new URL("http://35.156.90.81:8080/server-webogram/protection");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        String input = "{\"sender\":" + from + ",\"senderUserName\":\"" + fromUserName + "\",\"senderName\":\"" + fromName + "\",\"id\":" + messageObject.id + ",\"channel\":\"" + privalino_channel + "\",\"text\":\"" + messageObject.message + "\"}";

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

    public static void blockUser(final int user_id)
    {
        //Privalino info fürs Blocken
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://35.156.90.81:8080/server-webogram/webogramblock");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");

                    String input = "{\"blockedUser\":" + user_id + ",\"blockingUser\":" + UserConfig.getClientUserId() + " ,\"blocked\":true}";

                    OutputStream os = conn.getOutputStream();
                    os.write(input.getBytes());
                    os.flush();


                    //BufferedReader br = new BufferedReader(new InputStreamReader(
                    //        (conn.getInputStream())));

                    //JSONObject privalinoRating = new JSONObject(br.readLine());

                    conn.disconnect();

                } catch (IOException e) {
                    Log.e("Privalino Exception", e.getMessage());
                    //e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public static void unblockUser(final int user_id)
    {
        //Privalino info fürs Blocken
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://35.156.90.81:8080/server-webogram/webogramblock");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");

                    String input = "{\"blockedUser\":" + user_id + ",\"blockingUser\":" + UserConfig.getClientUserId() + " ,\"blocked\":false}";

                    OutputStream os = conn.getOutputStream();
                    os.write(input.getBytes());
                    os.flush();


                    //BufferedReader br = new BufferedReader(new InputStreamReader(
                    //        (conn.getInputStream())));

                    //JSONObject privalinoRating = new JSONObject(br.readLine());

                    conn.disconnect();

                } catch (IOException e) {
                    Log.e("Privalino Exception", e.getMessage());
                    //e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public static AlertDialog createPrivalinoMenu(final TLRPC.Message message, Activity activity)  {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message.privalino_question)
                .setPositiveButton(message.privalino_questionOptions[0], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {

                                    //TODO Move to PrivalinoMessageHandler
                                    URL url = new URL("http://35.156.90.81:8080/server-webogram/popupanswer");
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setDoOutput(true);
                                    conn.setRequestMethod("POST");
                                    conn.setRequestProperty("Content-Type", "application/json");

                                    String input = "{\"id\":" + message.privalino_questionId + ",\"answer\":" + message.privalino_questionOptions[0] + " }";

                                    OutputStream os = conn.getOutputStream();
                                    os.write(input.getBytes());
                                    os.flush();


                                    //BufferedReader br = new BufferedReader(new InputStreamReader(
                                    //        (conn.getInputStream())));

                                    //JSONObject privalinoRating = new JSONObject(br.readLine());

                                    conn.disconnect();

                                } catch (IOException e) {
                                    Log.e("Privalino Exception", e.getMessage());
                                    //e.printStackTrace();
                                }
                            }
                        });
                        thread.start();

                    }
                })
                .setNegativeButton(message.privalino_questionOptions[1], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Der Chatpartner wird gespert.
                        MessagesController.getInstance().blockUser(message.from_id);

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    URL url = new URL("http://35.156.90.81:8080/server-webogram/popupanswer");
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setDoOutput(true);
                                    conn.setRequestMethod("POST");
                                    conn.setRequestProperty("Content-Type", "application/json");

                                    String input = "{\"id\":" + message.privalino_questionId + ",\"answer\":" + message.privalino_questionOptions[1] + " }";

                                    OutputStream os = conn.getOutputStream();
                                    os.write(input.getBytes());
                                    os.flush();


                                    //BufferedReader br = new BufferedReader(new InputStreamReader(
                                    //        (conn.getInputStream())));

                                    //JSONObject privalinoRating = new JSONObject(br.readLine());

                                    conn.disconnect();

                                } catch (IOException e) {
                                    Log.e("Privalino Exception", e.getMessage());
                                    //e.printStackTrace();
                                }
                            }
                        });
                        thread.start();
                    }
                });
        builder.setTitle(LocaleController.getString("Privalino", R.string.Message));
        return builder.create();
    }

}



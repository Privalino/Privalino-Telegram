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

import de.privalino.telegram.model.PrivalinoBlockedUser;
import de.privalino.telegram.model.PrivalinoFeedback;
import de.privalino.telegram.model.PrivalinoMessageContainer;
import de.privalino.telegram.rest.PrivalinoBlockedUserApi;
import de.privalino.telegram.rest.PrivalinoMessageContainerApi;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.R.id.message;
import static android.app.PendingIntent.getActivity;

/**
 * Created by erbs on 01.08.17.
 */

public class PrivalinoMessageHandler extends DialogFragment {

    private static final String API_URL = "http://35.156.90.81:8080/server-webogram/protection/";

    private static PrivalinoMessageContainerApi protectionApi = null;
    private static PrivalinoBlockedUserApi blockUserApi = null;

    private static PrivalinoMessageContainer initPrivalinoMessageContainer(PrivalinoMessageContainer messageContainer, TLRPC.Message messageObject)
    {
        int senderId = messageObject.from_id;
        int receiverId = messageObject.to_id.user_id;

        messageContainer.setChannelId(messageObject.to_id.channel_id);
        messageContainer.setText(messageObject.message);
        messageContainer.setChatId(messageObject.to_id.chat_id);
        messageContainer.setReceiverId(receiverId);
        messageContainer.setSenderId(senderId);
        messageContainer.setMessageId(messageObject.id);

        MessagesStorage messagesStore = MessagesStorage.getInstance();
        messageContainer.setSenderUserName(messagesStore.getUser(senderId).username);
        messageContainer.setSenderFirstName(messagesStore.getUser(senderId).first_name);
        messageContainer.setSenderLastName(messagesStore.getUser(senderId).last_name);
        messageContainer.setReceiverUserName(messagesStore.getUser(receiverId).username);
        messageContainer.setReceiverFirstName(messagesStore.getUser(receiverId).first_name);
        messageContainer.setReceiverLastName(messagesStore.getUser(receiverId).last_name);
        return messageContainer;
    }

    public static PrivalinoFeedback handleOutgoingMessage(TLRPC.Message messageObject) throws IOException, JSONException
    {
        messageObject.message += "wau";
        return handleMessage(messageObject, false);
    }

    public static PrivalinoFeedback handleIncomingMessage(TLRPC.Message messageObject) throws IOException, JSONException
    {
        return handleMessage(messageObject, true);
    }

    private static PrivalinoFeedback handleMessage(TLRPC.Message messageObject, boolean isIncoming) throws IOException, JSONException
    {
        PrivalinoMessageContainer messageContainer = new PrivalinoMessageContainer();
        messageContainer.setIncoming(isIncoming);
        messageContainer = initPrivalinoMessageContainer(messageContainer, messageObject);

        Log.i("[Privalino]", "Prepared message: " + messageContainer.toString());


        PrivalinoFeedback feedback = callServer(messageContainer);

        if (feedback != null)
            Log.i("[Privalino]", "Received feedback: " + feedback.toString());

        return feedback;
    }


    private static PrivalinoFeedback callServer(PrivalinoMessageContainer messageContainer) throws IOException
    {
        Log.i("[Privalino]", "Sending message: " + messageContainer.toString());
        Call<PrivalinoFeedback> call = getProtectionApi().analyze(messageContainer);
        Log.i("[Privalino]", "Call: " + call.request().url());

        //synchronous call
        Response<PrivalinoFeedback> response = call.execute();
        Log.i("[Privalino]", "Response: " + response.isSuccessful());
        Log.i("[Privalino]", "Response: " + response.code());
        Log.i("[Privalino]", "Response: " + response.raw());
        return response.body();
    }

    private static void callServer(int userId, boolean isBlocked) throws IOException {

        PrivalinoBlockedUser blockedUser = new PrivalinoBlockedUser();
        blockedUser.setUser(userId);
        blockedUser.setBlockingUser(UserConfig.getClientUserId());
        blockedUser.setBlocked(isBlocked);

        Log.i("[Privalino]", "Sending blocking user: " + blockedUser.toString());
        Call<Void> call = getBlockingApi().analyze(blockedUser);
        Log.i("[Privalino]", "Call: " + call.request().url());

        //synchronous call
        Response<Void> response = call.execute();
        Log.i("[Privalino]", "Response: " + response.isSuccessful());
        Log.i("[Privalino]", "Response: " + response.code());
        Log.i("[Privalino]", "Response: " + response.raw());
    }

    private static PrivalinoMessageContainerApi getProtectionApi() {
        if(protectionApi == null){
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            // prepare call in Retrofit 2.0
            protectionApi = retrofit.create(PrivalinoMessageContainerApi.class);
        }
        return protectionApi;
    }

    private static PrivalinoBlockedUserApi getBlockingApi() {
        if(blockUserApi == null){
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            // prepare call in Retrofit 2.0
            blockUserApi = retrofit.create(PrivalinoBlockedUserApi.class);
        }
        return blockUserApi;
    }

    public static void blockUser(int userId) throws IOException {
        callServer(userId, true);
    }

    public static void unblockUser(int userId) throws IOException {
        callServer(userId, false);
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
                                try {
                                    blockUser(message.from_id);
                                } catch (IOException e) {
                                    Log.e("Privalino Exception", e.getMessage());
                                    //e.printStackTrace();
                                }
                            }
                        });
                        builder.setTitle(LocaleController.getString("Privalino", R.string.Message));
        return builder.create();
    }

}
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
import org.telegram.messenger.SendMessagesHelper;
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
    private static final String PRIVALINO_AGB = "Dieses Gespräch wird durch Privalino als Dritten gepeichert und ausgewertet. Mit dem weiteren Senden von Nachrichten stimmen Sie einer Speicherung und Auswertung zu. Weitere Informationen erhalten Sie unter http://www.privalino.de/agb-messenger/";


    private static PrivalinoMessageContainerApi protectionApi = null;
    private static PrivalinoBlockedUserApi blockUserApi = null;

    private static PrivalinoMessageContainer initPrivalinoMessageContainer(PrivalinoMessageContainer messageContainer, TLRPC.Message messageObject)
    {
        int senderId = messageObject.from_id;
        int receiverId = messageObject.to_id.user_id;
        if (messageContainer.isIncoming())
        {
            receiverId = UserConfig.getClientUserId();
        }

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
        return handleMessage(messageObject, false);
    }

    public static PrivalinoFeedback handleIncomingMessage(TLRPC.Message messageObject) throws IOException, JSONException
    {
        PrivalinoFeedback feedback = handleMessage(messageObject, true);
        if (feedback != null && feedback.isBlocked()) {
            // User Blocken
            boolean userBlocked = MessagesController.getInstance().blockedUsers.contains(messageObject.from_id);
            if (!userBlocked) //{
                blockUser(messageObject.from_id);
        }

        return feedback;
    }

    private static PrivalinoFeedback handleMessage(TLRPC.Message messageObject, boolean isIncoming) throws IOException, JSONException
    {
        PrivalinoMessageContainer messageContainer = new PrivalinoMessageContainer();
        messageContainer.setIncoming(isIncoming);
        messageContainer = initPrivalinoMessageContainer(messageContainer, messageObject);

        Log.i("[Privalino]", "Prepared message: " + messageContainer.toString());


        PrivalinoFeedback feedback = callServer(messageContainer);

        if (feedback != null)
        {
            Log.i("[Privalino]", "Received feedback: " + feedback.toString());

            if (feedback.isFirstMessage())
            {
                SendMessagesHelper.getInstance().sendMessage(LocaleController.getString("PrivalinoTerms", R.string.PrivalinoTerms), messageObject.from_id, null, null, false, null, null, null);
            }
        }

        return feedback;
    }


    private static PrivalinoFeedback callServer(PrivalinoMessageContainer messageContainer) throws IOException
    {
        Log.i("[Privalino]", "Sending message: " + messageContainer.toString());
        Call<PrivalinoFeedback> call = getProtectionApi().analyze(messageContainer);
        Log.i("[Privalino]", "Call: " + call.request().url());

        //synchronous call
        Response<PrivalinoFeedback> response = call.execute();
        Log.i("[Privalino]", "Response: " + response.raw());
        return response.body();
    }

    private static void callServer(int userId, boolean isBlocked) throws IOException {

        PrivalinoBlockedUser blockedUser = new PrivalinoBlockedUser();
        blockedUser.setUser(userId);
        blockedUser.setBlockingUser(UserConfig.getClientUserId());
        blockedUser.setIsBlocked(isBlocked);

        //Log.i("[Privalino]", "Sending blocking user: " + blockedUser.toString());
        Call<Boolean> call = getBlockingApi().inform(blockedUser);
        Log.i("[Privalino]", "Call: " + call.request().url());

        //synchronous call
        Response<Boolean> response = call.execute();
        Log.i("[Privalino]", "Response: " + response.isSuccessful());
        Log.i("[Privalino]", "Response: " + response.code());
        Log.i("[Privalino]", "Response: " + response.body());
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
                .setNegativeButton(message.privalino_questionOptions[1], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Der Chatpartner wird gespert.
                        MessagesController.getInstance().blockUser(message.from_id);
                        try {
                            Log.e("[Privalino]", "Trying to block user " + message.from_id);
                            //TODO It might help to open another thread for it
                            blockUser(message.from_id);
                        } catch (IOException e) {
                            Log.e("Privalino Exception", e.getMessage());
                            //e.printStackTrace();
                        }


                    }
                })
                .setPositiveButton(message.privalino_questionOptions[0], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Der Chatpartner wird nicht mehr gesperrt.
                        Log.i("[Privalino]", "Do nothing because they know each other");
                    }
                });
        builder.setTitle(LocaleController.getString("Privalino", R.string.Message));
        return builder.create();
    }

}
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
import java.util.HashMap;

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

import net.hockeyapp.android.metrics.MetricsManager;

/**
 * Created by erbs on 01.08.17.
 */

public class PrivalinoMessageHandler extends DialogFragment {

    private static final String API_URL = "https://api.privalino.de/server_v1801/protection/";

    private static final String TAG = "Privalino";

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
        if(messagesStore.getUser(senderId)!=null) {
            messageContainer.setSenderUserName(messagesStore.getUser(senderId).username);
            messageContainer.setSenderFirstName(messagesStore.getUser(senderId).first_name);
            messageContainer.setSenderLastName(messagesStore.getUser(senderId).last_name);
        }
        if(receiverId!=0){
            messageContainer.setReceiverUserName(messagesStore.getUser(receiverId).username);
            messageContainer.setReceiverFirstName(messagesStore.getUser(receiverId).first_name);
            messageContainer.setReceiverLastName(messagesStore.getUser(receiverId).last_name);
        }
        return messageContainer;
    }

    public static PrivalinoFeedback handleOutgoingMessage(TLRPC.Message messageObject)
    {
        return handleMessage(messageObject, false);
    }

    public static PrivalinoFeedback handleIncomingMessage(TLRPC.Message messageObject)
    {
        return handleMessage(messageObject, true);
    }

    private static PrivalinoFeedback handleMessage(TLRPC.Message messageObject, boolean isIncoming)
    {
        PrivalinoMessageContainer messageContainer = new PrivalinoMessageContainer();
        messageContainer.setIncoming(isIncoming);
        messageContainer = initPrivalinoMessageContainer(messageContainer, messageObject);

        Log.d(TAG, "Prepared message:\t" + messageContainer.toString());
        try {

            PrivalinoFeedback feedback = callServer(messageContainer);

            if (feedback != null)
            {
                Log.i(TAG, "Received feedback:\t" + feedback.toString());

                if (feedback.isFirstMessage())
                {
                    SendMessagesHelper.getInstance().sendMessage(LocaleController.getString("PrivalinoTerms", R.string.PrivalinoTerms), messageObject.from_id, null, null, false, null, null, null);
                }
                if(!feedback.isWhitelisted()){
                    filterMedia(messageObject);
                }

            }

            return feedback;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            HashMap<String, String> properties = new HashMap<>();
            properties.put("Error", e.getMessage());
            properties.put("IsIncoming", String.valueOf(isIncoming));
            properties.put("FromId", String.valueOf(messageObject.from_id));
            properties.put("Message", messageObject.message);
            MetricsManager.trackEvent("handleMessage", properties);
            return null;
        }
    }

    private static void filterMedia(TLRPC.Message messageObject){
        if(messageObject.media != null) {
            Log.i(TAG, "is photo:\t" + (messageObject.media.photo != null));
            // If it is a photo, it will be overriden with a blank photo
            if (messageObject.media.photo != null) {
                messageObject.media.photo = new TLRPC.TL_photoEmpty();
                messageObject.media.caption = "Bilder sind bei Privalino zu deiner Sicherheit gesperrt. " + messageObject.media.caption;
            }
            if (messageObject.media.document != null)
            {
                messageObject.media.document = new TLRPC.TL_documentEmpty();
                messageObject.media.caption = "Dateien sind bei Privalino zu deiner Sicherheit gesperrt. " + messageObject.media.caption;
            }
        }

    }


    private static PrivalinoFeedback callServer(PrivalinoMessageContainer messageContainer) throws IOException
    {
        Log.i(TAG, "Sending message: " + messageContainer.toString());

        //workaround if message is null
        if(messageContainer.getText() == null){
            PrivalinoFeedback feedback = new PrivalinoFeedback();
            feedback.setMessage("");
            feedback.setPopUp(null);
            feedback.setBlocked(false);
            feedback.setFirstMessage(false);
            return feedback;
        }

        Call<PrivalinoFeedback> call = getProtectionApi().analyze(messageContainer);
        Log.i(TAG, "Call: " + call.request().url());

        //synchronous call
        Response<PrivalinoFeedback> response = call.execute();
        Log.i(TAG, "Response: " + response.raw());
        return response.body();
    }

    private static void callServer(final int userId, final boolean isBlocked)
    {

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){

                try {
                    PrivalinoBlockedUser blockedUser = new PrivalinoBlockedUser();
                    blockedUser.setUser(userId);
                    blockedUser.setBlockingUser(UserConfig.getClientUserId());
                    blockedUser.setIsBlocked(isBlocked);

                    //Log.i(TAG, "Sending blocking user: " + blockedUser.toString());
                    Call<Boolean> call = getBlockingApi().inform(blockedUser);
                    Log.i(TAG, "Call: " + call.request().url());

                    //synchronous call
                    Response<Boolean> response = call.execute();
                    Log.i(TAG, "Response: " + response.body());

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

            }
        });
        thread.start();

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

    public static void blockUser(int userId)
    {
        callServer(userId, true);
    }

    public static void unblockUser(int userId)
    {
        callServer(userId, false);
    }

    public static AlertDialog createPrivalinoMenu(final TLRPC.Message message, Activity activity)  {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message.privalino_question)
                .setNegativeButton(message.privalino_questionOptions[1], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Der Chatpartner wird gespert.
                        MessagesController.getInstance().blockUser(message.from_id);
                        //Send blocking information to server
                        blockUser(message.from_id);
                    }
                })
                .setPositiveButton(message.privalino_questionOptions[0], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Der Chatpartner wird gespert.
                        MessagesController.getInstance().unblockUser(message.from_id);
                    }
                });
        builder.setTitle(LocaleController.getString(TAG, R.string.Message));
        return builder.create();
    }

}
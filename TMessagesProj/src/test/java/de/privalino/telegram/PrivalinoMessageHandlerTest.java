package de.privalino.telegram;

import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.UserConfig;

import de.privalino.telegram.model.PrivalinoBlockedUser;
import de.privalino.telegram.rest.PrivalinoBlockedUserApi;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.*;

/**
 * Created by pinguin on 16.08.17.
 */
public class PrivalinoMessageHandlerTest {

    private static final String API_URL = "http://api.privalino.de:8080/server-webogram/protection/";

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    private static PrivalinoBlockedUserApi getBlockingApi() {
        PrivalinoBlockedUserApi blockUserApi = null;
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

    @Test
    public void getBlockedFromServerTest() throws Exception {
        int userId = 1234567;
        boolean isBlocked = true;

        PrivalinoBlockedUser blockedUser = new PrivalinoBlockedUser();
        blockedUser.setUser(userId);
        blockedUser.setBlockingUser(UserConfig.getClientUserId());
        blockedUser.setIsBlocked(isBlocked);

        //Log.i("[Privalino]", "Sending blocking user: " + blockedUser.toString());
        Call<Boolean> call = getBlockingApi().inform(blockedUser);
        //Log.i("[Privalino]", "Call: " + call.request().url());

        //synchronous call
        Response<Boolean> response = call.execute();
        //Log.i("[Privalino]", "Response: " + response.isSuccessful());
        //Log.i("[Privalino]", "Response: " + response.code());
        //Log.i("[Privalino]", "Response: " + response.body());
    }
}

package de.privalino.telegram.rest;

import de.privalino.telegram.model.PrivalinoBlockedUser;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by nico on 29/03/16.
 */

public interface PrivalinoBlockedUserApi {
    @POST("block/")
    Call<Boolean> inform(@Body PrivalinoBlockedUser blockedUser);
}
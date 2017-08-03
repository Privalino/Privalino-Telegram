package de.privalino.telegram.rest;

import de.privalino.telegram.model.PrivalinoFeedback;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import de.privalino.telegram.model.PrivalinoMessageContainer;

/**
 * Created by nico on 29/03/16.
 */
public interface PrivalinoMessageContainerApi {
    @POST("message/")
    Call<PrivalinoFeedback> analyze(@Body PrivalinoMessageContainer messageContainer);
}

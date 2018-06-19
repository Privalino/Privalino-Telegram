package de.privalino.telegram.rest;

import de.privalino.telegram.model.Child;
import de.privalino.telegram.model.Parent;
import de.privalino.telegram.model.RegisterResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface PrivalinoOnBoardApi {

    static String type = "application/json";

    @POST("register/")
    Call<RegisterResponse> parent(@Header("Content-Type") String type, @Body Parent parent);

    @POST("register/")
    Call<RegisterResponse> child(@Header("Content-Type") String type, @Body Child child);

}

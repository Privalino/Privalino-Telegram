package de.privalino.telegram;

import android.provider.Settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import de.privalino.telegram.model.Child;
import de.privalino.telegram.model.Parent;
import de.privalino.telegram.model.RegisterResponse;
import de.privalino.telegram.rest.PrivalinoOnboardApi;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PrivalinoOnboardHandler {

    private static String BASE_URL = "https://api.privalino.de/app-registration/";

    private PrivalinoOnboardApi retrofitAPI;

    //static models which hold the onboarding data
    public static Parent parentModel = null;
    public static Child childModel = null;

    public PrivalinoOnboardHandler() {
        this(BASE_URL);
    }



    public PrivalinoOnboardHandler(String baseUrl) {

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        retrofitAPI = retrofit.create(PrivalinoOnboardApi.class);
    }

    public PrivalinoOnboardApi getAPI() {
        return retrofitAPI;
    }





}

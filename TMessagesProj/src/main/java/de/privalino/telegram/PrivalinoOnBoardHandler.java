package de.privalino.telegram;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.privalino.telegram.model.Child;
import de.privalino.telegram.model.Parent;
import de.privalino.telegram.rest.PrivalinoOnBoardApi;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PrivalinoOnBoardHandler {

    private static String BASE_URL = "https://api.privalino.de/app-registration/";

    private PrivalinoOnBoardApi retrofitAPI;

    //static models which hold the onboarding data
    public static Parent parentModel = null;
    public static Child childModel = null;

    public PrivalinoOnBoardHandler() {
        this(BASE_URL);
    }



    public PrivalinoOnBoardHandler(String baseUrl) {

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        retrofitAPI = retrofit.create(PrivalinoOnBoardApi.class);
    }

    public PrivalinoOnBoardApi getAPI() {
        return retrofitAPI;
    }





}

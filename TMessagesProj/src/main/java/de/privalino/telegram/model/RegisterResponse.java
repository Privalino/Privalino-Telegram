package de.privalino.telegram.model;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {

    @SerializedName("text")
    private String text;

    public RegisterResponse(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

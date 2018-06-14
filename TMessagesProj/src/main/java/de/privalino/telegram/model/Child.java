package de.privalino.telegram.model;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.privalino.telegram.PrivalinoOnboardHandler;

public class Child {

    @Expose
    @SerializedName("phoneId")
    private String androidId;
    @Expose
    @SerializedName("userType")
    private String userType;
    @Expose
    @SerializedName("phoneNumber")
    private String phoneNumber;
    @Expose
    @SerializedName("parentsNumbers")
    private List<String> parentsNumbers;

    private final String DELIMITER = "&";

    public Child(String userType, String phoneNumber, List<String> parentsNumbers) {
        this.userType = userType;
        this.phoneNumber = phoneNumber;
        this.parentsNumbers = parentsNumbers;
    }

    public Child(){}

    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<String> getParentsNumbers() {
        return parentsNumbers;
    }

    public void setParentsNumbers(List<String> parentsNumbers) {
        this.parentsNumbers = parentsNumbers;
    }

    /**
     * Concatenates collected parents phone numbers and joins them into a single string.
     * Delimiter between the joined items is specified with a final class field.
     * @return concatenated parents numbers
     */
    public String getParentsNumbersAsString(){
        String phoneNumbers = "";
        for (String phoneNumber: parentsNumbers){
            phoneNumbers += phoneNumber;
            if (parentsNumbers.indexOf(phoneNumber) != parentsNumbers.size() - 1){
                phoneNumbers += DELIMITER;
            }
        }
        return phoneNumbers;
    }

    /**
     * Parses the given parents numbers string and saves results to the class field
     * which holds the parents phone numbers.
     * @param parentsNumbers string to parse
     */
    public void parseParentsNumbersString(String parentsNumbers){
        List<String> numbers = new ArrayList<>(Arrays.asList(parentsNumbers.split(DELIMITER)));
        this.setParentsNumbers(numbers);
    }

    /**
     * Initializes the two default values of this object, user type and android id.
     * @param context application context to get the android id
     */
    public void initialize(Context context){
        this.setUserType("child");
        this.setAndroidId(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
    }
}

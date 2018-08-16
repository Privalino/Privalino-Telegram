package de.privalino.telegram.model;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.google.android.gms.common.api.Api;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.ApplicationLoader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_CHILDREN;
import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_EMAIL;
import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_PARENTS;
import static de.privalino.telegram.AppConstants.SHAREDRPREFS_KEY_ON_BOARDING_INFO;
import static de.privalino.telegram.AppConstants.USER_TYPE_PARENT;

public class Parent {

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
    @SerializedName("email")
    private String email;
    @Expose
    @SerializedName("children")
    private List<Child> children;

    private final String DELIMITER = "&";

    public Parent(){
        children = new ArrayList<>();
    }

    public Parent(String userType, String phoneNumber, String email) {
        this.userType = userType;
        this.phoneNumber = phoneNumber;
        this.email = email;
        children = new ArrayList<>();
    }

    public void resetChildren(){
        children.clear();
    }

    public void addChild(String name, String phoneNumber){
        children.add(new Child(name,phoneNumber));
    }

    public void setChildren(List<Child> children) {
        this.children = children;
    }

    public List<Child> getChildren(){
        return this.children;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAndroidId() {
        return androidId;
    }

    /**
     * Initializes the two default values of this object, user type and android id.
     * @param context application context to get the android id
     */
    public void initialize(Context context){
        this.setUserType(USER_TYPE_PARENT);
        this.setAndroidId(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
    }


    /**
     * Updates missing non-default values (email and children information) fields from
     * data stored in shared preferences (if there is any).
     */
    public void updateMissingFromSharedPrefs(){
        SharedPreferences preferences  = ApplicationLoader.applicationContext.getSharedPreferences(SHAREDRPREFS_KEY_ON_BOARDING_INFO, Activity.MODE_PRIVATE);

        if (email == null){
            this.setEmail(preferences.getString(SHAREDPREFS_KEY_EMAIL, null));
        }

        if (phoneNumber == null){
            this.setPhoneNumber(preferences.getString(SHAREDPREFS_KEY_PARENTS, null));
        }

        if (children.size() == 0){
            String children = preferences.getString(SHAREDPREFS_KEY_CHILDREN, null);
            if (children != null){
                this.parseChildren(children);
            }
        }
    }

    /**
     * Concatenates collected child objects turned into string with the overridden toString method
     * and joins them into a single string.
     * Delimiter between the joined items is specified with a final class field.
     * @return concatenated children objects
     */
    public String getChildrenAsString(){
        String childrenString = "";
        for (Child child: this.children){
            childrenString += child.toString();
            if(this.children.indexOf(child) != this.children.size() - 1){
                childrenString += DELIMITER;
            }
        }
        return childrenString;
    }

    /**
     * Parses the given child objects string and saves results to the class field
     * which holds the list of children.
     * @param childrenString string to parse
     */

    public void parseChildren(String childrenString){
        ArrayList<Child> children = new ArrayList<>();
        String[] childStrings = childrenString.split(DELIMITER);
        for (String childString: childStrings){
            children.add(new Child(childString));
        }
        this.children = children;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    public class Child{
        @Expose
        @SerializedName("name")
        private String name;
        @Expose
        @SerializedName("phoneNumber")
        private String phoneNumber;
        @Expose
        @SerializedName("birthYear")
        private int birthYear;

        private final String DELIMITER = "%";

        public Child(String name, String phoneNumber, int birthYear) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.birthYear = birthYear;
        }

        public Child(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public int getBirthYear() {
            return birthYear;
        }

        public void setBirthYear(int birthYear) {
            this.birthYear = birthYear;
        }



        /**
         * Constructor which takes a string generated by the toString method of this class,
         * parses it and creates an object from the values gotten.
         * @param childString
         */
        public Child(String childString){
            String[] parts = childString.split(DELIMITER);
            this.name = parts[0];
            this.phoneNumber = parts[1];
            int year = Integer.parseInt(parts[2]);
            if (year != 0){
                this.birthYear = year;
            }
        }

        @Override
        public String toString() {
            //concatenates the fields of this object with a delimiter specified by a final class field
            return this.name + DELIMITER + this.phoneNumber + DELIMITER + this.birthYear;
        }

    }

}

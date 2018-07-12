package de.privalino.telegram;

public class AppConstants {

    //user types
    public static final String USER_TYPE_PARENT = "parent";
    public static final String USER_TYPE_CHILD = "child";


    //shared preferences keys
    public static final String SHAREDRPREFS_KEY_INSTALLED = "installed";
    public static final String SHAREDRPREFS_KEY_ON_BOARDING_INFO = "onboardinfo";
    public static final String SHAREDPREFS_KEY_USER_TYPE_SELECTED = "selected";
    public static final String SHAREDPREFS_KEY_IS_PARENT = "is_parent";
    public static final String SHAREDPREFS_KEY_EMAIL = "email";
    public static final String SHAREDPREFS_KEY_CHILDREN = "children";
    public static final String SHAREDPREFS_KEY_PARENTS = "parents";

    //permission request and result codes
    public static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 314;
    public static final int RESULT_PICK_CONTACT = 159;

    //intent extras keys
    public static final String INTENT_EXTRA_KEY_FROM_SETTINGS = "fromSettings";
    public static final String INTENT_EXTRA_KEY_FROM_INTRO = "fromIntro";
    public static final String INTENT_EXTRA_KEY_IS_PARENT = "isParent";

}

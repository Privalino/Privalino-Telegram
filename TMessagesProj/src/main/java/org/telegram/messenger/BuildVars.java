/*
 * This is the source code of Telegram for Android v. 4.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

public class BuildVars {

    public static boolean DEBUG_VERSION = false;
    public static boolean DEBUG_PRIVATE_VERSION = false;
    public static int APP_ID = 67270; //obtain your own APP_ID at https://core.telegram.org/api/obtaining_api_id
    public static String APP_HASH = "a3589d4517fd8c12384fc7bd9c0ac573"; //obtain your own APP_HASH at https://core.telegram.org/api/obtaining_api_id
    public static String HOCKEY_APP_HASH = "baf18098164e4e1786bd8acb7d9fd406";
    public static String HOCKEY_APP_HASH_DEBUG = "a3589d4517fd8c12384fc7bd9c0ac573";
    public static String BING_SEARCH_KEY = ""; //obtain your own KEY at https://www.bing.com/dev/en-us/dev-center
    public static String FOURSQUARE_API_KEY = ""; //obtain your own KEY at https://developer.foursquare.com/
    public static String FOURSQUARE_API_ID = ""; //obtain your own API_ID at https://developer.foursquare.com/
    public static String GOOGLE_API_KEY = "";
    public static String FOURSQUARE_API_VERSION = "20150326";
    public static boolean LOGS_ENABLED = false;
    public static boolean CHECK_UPDATES = false;
    public static int BUILD_VERSION = 1358;
    public static String BUILD_VERSION_STRING = "4.9.1";
    public static String PLAYSTORE_APP_URL = "";

    static {
        if (ApplicationLoader.applicationContext != null) {
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION);
        }
    }
}

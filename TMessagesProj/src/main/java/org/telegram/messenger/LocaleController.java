/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Xml;

import org.telegram.messenger.time.FastDateFormat;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class LocaleController {

    static final int QUANTITY_OTHER = 0x0000;
    static final int QUANTITY_ZERO = 0x0001;
    static final int QUANTITY_ONE = 0x0002;
    static final int QUANTITY_TWO = 0x0004;
    static final int QUANTITY_FEW = 0x0008;
    static final int QUANTITY_MANY = 0x0010;

    public static boolean isRTL = false;
    public static int nameDisplayOrder = 1;
    public static boolean is24HourFormat = false;
    public FastDateFormat formatterDay;
    public FastDateFormat formatterWeek;
    public FastDateFormat formatterMonth;
    public FastDateFormat formatterYear;
    public FastDateFormat formatterMonthYear;
    public FastDateFormat formatterYearMax;
    public FastDateFormat formatterStats;
    public FastDateFormat formatterBannedUntil;
    public FastDateFormat formatterBannedUntilThisYear;
    public FastDateFormat chatDate;
    public FastDateFormat chatFullDate;

    private HashMap<String, PluralRules> allRules = new HashMap<>();

    private Locale currentLocale;
    private Locale systemDefaultLocale;
    private PluralRules currentPluralRules;
    private LocaleInfo currentLocaleInfo;
    private HashMap<String, String> localeValues = new HashMap<>();
    private String languageOverride;
    private boolean changingConfiguration = false;
    private boolean reloadLastFile;

    private HashMap<String, String> currencyValues;
    private HashMap<String, String> translitChars;

    private class TimeZoneChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ApplicationLoader.applicationHandler.post(() -> {
                if (!formatterMonth.getTimeZone().equals(TimeZone.getDefault())) {
                    LocaleController.getInstance().recreateFormatters();
                }
            });
        }
    }

    public static class LocaleInfo {
        public String name;
        public String nameEnglish;
        public String shortName;
        public String pathToFile;
        public int version;
        public boolean builtIn;

        public String getSaveString() {
            return name + "|" + nameEnglish + "|" + shortName + "|" + pathToFile + "|" + version;
        }

        public static LocaleInfo createWithString(String string) {
            if (string == null || string.length() == 0) {
                return null;
            }
            String[] args = string.split("\\|");
            LocaleInfo localeInfo = null;
            if (args.length >= 4) {
                localeInfo = new LocaleInfo();
                localeInfo.name = args[0];
                localeInfo.nameEnglish = args[1];
                localeInfo.shortName = args[2].toLowerCase();
                localeInfo.pathToFile = args[3];
                if (args.length >= 5) {
                    localeInfo.version = Utilities.parseInt(args[4]);
                }
            }
            return localeInfo;
        }

        public File getPathToFile() {
            if (isRemote()) {
                return new File(ApplicationLoader.getFilesDirFixed(), "remote_" + shortName + ".xml");
            }
            return !TextUtils.isEmpty(pathToFile) ? new File(pathToFile) : null;
        }

        public String getKey() {
            if (pathToFile != null && !"remote".equals(pathToFile)) {
                return "local_" + shortName;
            }
            return shortName;
        }

        public boolean isRemote() {
            return "remote".equals(pathToFile);
        }

        public boolean isLocal() {
            return !TextUtils.isEmpty(pathToFile) && !isRemote();
        }

        public boolean isBuiltIn() {
            return builtIn;
        }
    }

    private boolean loadingRemoteLanguages;

    public ArrayList<LocaleInfo> languages = new ArrayList<>();
    public ArrayList<LocaleInfo> remoteLanguages = new ArrayList<>();
    public HashMap<String, LocaleInfo> languagesDict = new HashMap<>();

    private ArrayList<LocaleInfo> otherLanguages = new ArrayList<>();

    private static volatile LocaleController Instance = null;
    public static LocaleController getInstance() {
        LocaleController localInstance = Instance;
        if (localInstance == null) {
            synchronized (LocaleController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new LocaleController();
                }
            }
        }
        return localInstance;
    }

    public LocaleController() {
        // XXX
        // To ensure langpacks are not read from cached / downloaded from server, we delete the
        // langconfig.
        ApplicationLoader.applicationContext.getSharedPreferences("langconfig", 0).edit().clear().commit();

        addRules(new String[]{"bem", "brx", "da", "de", "el", "de", "eo", "es", "et", "fi", "fo", "gl", "he", "iw", "it", "nb",
                "nl", "nn", "no", "sv", "af", "bg", "bn", "ca", "eu", "fur", "fy", "gu", "ha", "is", "ku",
                "lb", "ml", "mr", "nah", "ne", "om", "or", "pa", "pap", "ps", "so", "sq", "sw", "ta", "te",
                "tk", "ur", "zu", "mn", "gsw", "chr", "rm", "pt", "an", "ast"}, new PluralRules_One());

        LocaleInfo localeInfo = new LocaleInfo();
        localeInfo.name = "Deutsch";
        localeInfo.nameEnglish = "German";
        localeInfo.shortName = "de";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        loadOtherLanguages();
        // XXX
        // It should never download langpacks from the server.
//        if (remoteLanguages.isEmpty()) {
//            loadRemoteLanguages();
//        }
        if (remoteLanguages.isEmpty()) {
            AndroidUtilities.runOnUIThread(() -> loadRemoteLanguages(UserConfig.selectedAccount));
        }

        for (int a = 0; a < otherLanguages.size(); a++) {
            LocaleInfo locale = otherLanguages.get(a);
            languages.add(locale);
            languagesDict.put(locale.getKey(), locale);
        }

        for (int a = 0; a < remoteLanguages.size(); a++) {
            LocaleInfo locale = remoteLanguages.get(a);
            LocaleInfo existingLocale = getLanguageFromDict(locale.getKey());
            if (existingLocale != null) {
                existingLocale.pathToFile = locale.pathToFile;
                existingLocale.version = locale.version;
                remoteLanguages.set(a, existingLocale);
            } else {
                languages.add(locale);
                languagesDict.put(locale.getKey(), locale);
            }
        }

        systemDefaultLocale = Locale.getDefault();
        is24HourFormat = DateFormat.is24HourFormat(ApplicationLoader.applicationContext);
        LocaleInfo currentInfo = null;
        boolean override = false;

        try {
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            String lang = preferences.getString("language", null);
            if (lang != null) {
                currentInfo = getLanguageFromDict(lang);
                if (currentInfo != null) {
                    override = true;
                }
            }

            if (currentInfo == null && systemDefaultLocale.getLanguage() != null) {
                currentInfo = getLanguageFromDict(systemDefaultLocale.getLanguage());
            }
            if (currentInfo == null) {
                currentInfo = getLanguageFromDict(getLocaleString(systemDefaultLocale));
                if (currentInfo == null) {
                    currentInfo = getLanguageFromDict("de");
                }
            }

            applyLanguage(currentInfo, override, true, UserConfig.selectedAccount);
        } catch (Exception e) {
            FileLog.e(e);
        }

        try {
            IntentFilter timezoneFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ApplicationLoader.applicationContext.registerReceiver(new TimeZoneChangedReceiver(), timezoneFilter);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private LocaleInfo getLanguageFromDict(String key) {
        if (key == null) {
            return null;
        }
        return languagesDict.get(key.toLowerCase().replace("-", "_"));
    }

    private void addRules(String[] languages, PluralRules rules) {
        for (String language : languages) {
            allRules.put(language, rules);
        }
    }

    private String stringForQuantity(int quantity) {
        switch (quantity) {
            case QUANTITY_ZERO:
                return "zero";
            case QUANTITY_ONE:
                return "one";
            case QUANTITY_TWO:
                return "two";
            case QUANTITY_FEW:
                return "few";
            case QUANTITY_MANY:
                return "many";
            default:
                return "other";
        }
    }

    public Locale getSystemDefaultLocale() {
        return systemDefaultLocale;
    }

    public boolean isCurrentLocalLocale() {
        return currentLocaleInfo.isLocal();
    }

    public void reloadCurrentRemoteLocale(int currentAccount) {
        applyRemoteLanguage(currentLocaleInfo, true, currentAccount);
    }

    public void checkUpdateForCurrentRemoteLocale(int currentAccount, int version) {
        if (currentLocaleInfo == null || currentLocaleInfo != null && !currentLocaleInfo.isRemote()) {
            return;
        }
        if (currentLocaleInfo.version < version) {
            applyRemoteLanguage(currentLocaleInfo, false, currentAccount);
        }
    }

    private String getLocaleString(Locale locale) {
        if (locale == null) {
            return "de";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "de";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('_');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getSystemLocaleStringIso639() {
        Locale locale = getInstance().getSystemDefaultLocale();
        if (locale == null) {
            return "de";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "de";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('-');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getLocaleStringIso639() {
        Locale locale = getInstance().currentLocale;
        if (locale == null) {
            return "de";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "de";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('-');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getLocaleAlias(String code) {
        if (code == null) {
            return null;
        }
        switch (code) {
            case "in":
                return "id";
            case "iw":
                return "he";
            case "jw":
                return "jv";
            case "no":
                return "nb";
            case "tl":
                return "fil";
            case "ji":
                return "yi";
            case "id":
                return "in";
            case "he":
                return "iw";
            case "jv":
                return "jw";
            case "nb":
                return "no";
            case "fil":
                return "tl";
            case "yi":
                return "ji";
        }

        return null;
    }

    public boolean applyLanguageFile(File file, int currentAccount) {
        try {
            HashMap<String, String> stringMap = getLocaleFileStrings(file);

            String languageName = stringMap.get("LanguageName");
            String languageNameInEnglish = stringMap.get("LanguageNameInEnglish");
            String languageCode = stringMap.get("LanguageCode");

            if (languageName != null && languageName.length() > 0 &&
                    languageNameInEnglish != null && languageNameInEnglish.length() > 0 &&
                    languageCode != null && languageCode.length() > 0) {

                if (languageName.contains("&") || languageName.contains("|")) {
                    return false;
                }
                if (languageNameInEnglish.contains("&") || languageNameInEnglish.contains("|")) {
                    return false;
                }
                if (languageCode.contains("&") || languageCode.contains("|") || languageCode.contains("/") || languageCode.contains("\\")) {
                    return false;
                }

                File finalFile = new File(ApplicationLoader.getFilesDirFixed(), languageCode + ".xml");
                if (!AndroidUtilities.copyFile(file, finalFile)) {
                    return false;
                }

                String key = "local_" + languageCode.toLowerCase();
                LocaleInfo localeInfo = getLanguageFromDict(key);
                if (localeInfo == null) {
                    localeInfo = new LocaleInfo();
                    localeInfo.name = languageName;
                    localeInfo.nameEnglish = languageNameInEnglish;
                    localeInfo.shortName = languageCode.toLowerCase();

                    localeInfo.pathToFile = finalFile.getAbsolutePath();
                    languages.add(localeInfo);
                    languagesDict.put(localeInfo.getKey(), localeInfo);
                    otherLanguages.add(localeInfo);

                    saveOtherLanguages();
                }
                localeValues = stringMap;
                applyLanguage(localeInfo, true, false, true, false, currentAccount);
                return true;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    private void saveOtherLanguages() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("langconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder stringBuilder = new StringBuilder();
        for (int a = 0; a < otherLanguages.size(); a++) {
            LocaleInfo localeInfo = otherLanguages.get(a);
            String loc = localeInfo.getSaveString();
            if (loc != null) {
                if (stringBuilder.length() != 0) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(loc);
            }
        }
        editor.putString("locales", stringBuilder.toString());
        stringBuilder.setLength(0);
        for (int a = 0; a < remoteLanguages.size(); a++) {
            LocaleInfo localeInfo = remoteLanguages.get(a);
            String loc = localeInfo.getSaveString();
            if (loc != null) {
                if (stringBuilder.length() != 0) {
                    stringBuilder.append("&");
                }
                stringBuilder.append(loc);
            }
        }
        editor.putString("remote", stringBuilder.toString());
        editor.commit();
    }

    public boolean deleteLanguage(LocaleInfo localeInfo, int currentAccount) {
        if (localeInfo.pathToFile == null || localeInfo.isRemote()) {
            return false;
        }
        if (currentLocaleInfo == localeInfo) {
            LocaleInfo info = null;
            if (systemDefaultLocale.getLanguage() != null) {
                info = getLanguageFromDict(systemDefaultLocale.getLanguage());
            }
            if (info == null) {
                info = getLanguageFromDict(getLocaleString(systemDefaultLocale));
            }
            if (info == null) {
                info = getLanguageFromDict("de");
            }
            applyLanguage(info, true, false, currentAccount);
        }

        otherLanguages.remove(localeInfo);
        languages.remove(localeInfo);
        languagesDict.remove(localeInfo.shortName);
        File file = new File(localeInfo.pathToFile);
        file.delete();
        saveOtherLanguages();
        return true;
    }

    private void loadOtherLanguages() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("langconfig", Activity.MODE_PRIVATE);
        String locales = preferences.getString("locales", null);
        if (!TextUtils.isEmpty(locales)) {
            String[] localesArr = locales.split("&");
            for (String locale : localesArr) {
                LocaleInfo localeInfo = LocaleInfo.createWithString(locale);
                if (localeInfo != null) {
                    otherLanguages.add(localeInfo);
                }
            }
        }
        locales = preferences.getString("remote", null);
        if (!TextUtils.isEmpty(locales)) {
            String[] localesArr = locales.split("&");
            for (String locale : localesArr) {
                LocaleInfo localeInfo = LocaleInfo.createWithString(locale);
                localeInfo.shortName = localeInfo.shortName.replace("-", "_");
                if (localeInfo != null) {
                    remoteLanguages.add(localeInfo);
                }
            }
        }
    }

    private HashMap<String, String> getLocaleFileStrings(File file) {
        return getLocaleFileStrings(file, false);
    }

    private HashMap<String, String> getLocaleFileStrings(File file, boolean preserveEscapes) {
        FileInputStream stream = null;
        reloadLastFile = false;
        try {
            if (!file.exists()) {
                return new HashMap<>();
            }
            HashMap<String, String> stringMap = new HashMap<>();
            XmlPullParser parser = Xml.newPullParser();
            //AndroidUtilities.copyFile(file, new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "locale10.xml"));
            stream = new FileInputStream(file);
            parser.setInput(stream, "UTF-8");
            int eventType = parser.getEventType();
            String name = null;
            String value = null;
            String attrName = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    int c = parser.getAttributeCount();
                    if (c > 0) {
                        attrName = parser.getAttributeValue(0);
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (attrName != null) {
                        value = parser.getText();
                        if (value != null) {
                            value = value.trim();
                            if (preserveEscapes) {
                                value = value.replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'").replace("& ", "&amp; ");
                            } else {
                                value = value.replace("\\n", "\n");
                                value = value.replace("\\", "");
                                String old = value;
                                value = value.replace("&lt;", "<");
                                if (!reloadLastFile && !value.equals(old)) {
                                    reloadLastFile = true;
                                }
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    value = null;
                    attrName = null;
                    name = null;
                }
                if (name != null && name.equals("string") && value != null && attrName != null && value.length() != 0 && attrName.length() != 0) {
                    stringMap.put(attrName, value);
                    name = null;
                    value = null;
                    attrName = null;
                }
                eventType = parser.next();
            }
            return stringMap;
        } catch (Exception e) {
            FileLog.e(e);
            reloadLastFile = true;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return new HashMap<>();
    }

    public void applyLanguage(LocaleInfo localeInfo, boolean override, boolean init, final int currentAccount) {
        applyLanguage(localeInfo, override, init, false, false, currentAccount);
    }

    public void applyLanguage(final LocaleInfo localeInfo, boolean override, boolean init, boolean fromFile, boolean force, final int currentAccount) {
        if (localeInfo == null) {
            return;
        }
        File pathToFile = localeInfo.getPathToFile();
        String shortName = localeInfo.shortName;
        if (!init) {
            ConnectionsManager.setLangCode(shortName.replace("_", "-"));
        }
        if (localeInfo.isRemote() && (force || !pathToFile.exists())) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("reload locale because file doesn't exist " + pathToFile);
            }
            if (init) {
                AndroidUtilities.runOnUIThread(() -> applyRemoteLanguage(localeInfo, true, currentAccount));
            } else {
                applyRemoteLanguage(localeInfo, true, currentAccount);
            }
        }
        try {
            Locale newLocale;
            String[] args = localeInfo.shortName.split("_");
            if (args.length == 1) {
                newLocale = new Locale(localeInfo.shortName);
            } else {
                newLocale = new Locale(args[0], args[1]);
            }
            if (override) {
                languageOverride = localeInfo.shortName;

                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("language", localeInfo.getKey());
                editor.commit();
            }
            if (pathToFile == null) {
                localeValues.clear();
            } else if (!fromFile) {
                localeValues = getLocaleFileStrings(pathToFile);
            }
            currentLocale = newLocale;
            currentLocaleInfo = localeInfo;
            currentPluralRules = allRules.get(args[0]);
            if (currentPluralRules == null) {
                currentPluralRules = allRules.get("de");
            }
            if (currentPluralRules == null) {
                currentPluralRules = new PluralRules_None();
            }
            changingConfiguration = true;
            Locale.setDefault(currentLocale);
            android.content.res.Configuration config = new android.content.res.Configuration();
            config.locale = currentLocale;
            ApplicationLoader.applicationContext.getResources().updateConfiguration(config, ApplicationLoader.applicationContext.getResources().getDisplayMetrics());
            changingConfiguration = false;
            if (reloadLastFile) {
                if (init) {
                    AndroidUtilities.runOnUIThread(() -> reloadCurrentRemoteLocale(currentAccount));
                } else {
                    reloadCurrentRemoteLocale(currentAccount);
                }
                reloadLastFile = false;
            }
        } catch (Exception e) {
            FileLog.e(e);
            changingConfiguration = false;
        }
        recreateFormatters();
    }

    public LocaleInfo getCurrentLocaleInfo() {
        return currentLocaleInfo;
    }

    public static String getCurrentLanguageName() {
        return getString("LanguageName", R.string.LanguageName);
    }

    private String getStringInternal(String key, int res) {
        String value = localeValues.get(key);
        if (value == null) {
            try {
                value = ApplicationLoader.applicationContext.getString(res);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (value == null) {
            value = "LOC_ERR:" + key;
        }
        return value;
    }

    public static String getServerString(String key) {
        String value = getInstance().localeValues.get(key);
        if (value == null) {
            int resourceId = ApplicationLoader.applicationContext.getResources().getIdentifier(key, "string", ApplicationLoader.applicationContext.getPackageName());
            if (resourceId != 0) {
                value = ApplicationLoader.applicationContext.getString(resourceId);
            }
        }
        return value;
    }

    public static String getString(String key, int res) {
        return getInstance().getStringInternal(key, res);
    }

    public static String getPluralString(String key, int plural) {
        if (key == null || key.length() == 0 || getInstance().currentPluralRules == null) {
            return "LOC_ERR:" + key;
        }
        String param = getInstance().stringForQuantity(getInstance().currentPluralRules.quantityForNumber(plural));
        param = key + "_" + param;
        int resourceId = ApplicationLoader.applicationContext.getResources().getIdentifier(param, "string", ApplicationLoader.applicationContext.getPackageName());
        return getString(param, resourceId);
    }

    public static String formatPluralString(String key, int plural) {
        if (key == null || key.length() == 0 || getInstance().currentPluralRules == null) {
            return "LOC_ERR:" + key;
        }
        String param = getInstance().stringForQuantity(getInstance().currentPluralRules.quantityForNumber(plural));
        param = key + "_" + param;
        int resourceId = ApplicationLoader.applicationContext.getResources().getIdentifier(param, "string", ApplicationLoader.applicationContext.getPackageName());
        return formatString(param, resourceId, plural);
    }

    public static String formatString(String key, int res, Object... args) {
        try {
            String value = getInstance().localeValues.get(key);
            if (value == null) {
                value = ApplicationLoader.applicationContext.getString(res);
            }

            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, value, args);
            } else {
                return String.format(value, args);
            }
        } catch (Exception e) {
            FileLog.e(e);
            return "LOC_ERR: " + key;
        }
    }

    public static String formatTTLString(int ttl) {
        if (ttl < 60) {
            return LocaleController.formatPluralString("Seconds", ttl);
        } else if (ttl < 60 * 60) {
            return LocaleController.formatPluralString("Minutes", ttl / 60);
        } else if (ttl < 60 * 60 * 24) {
            return LocaleController.formatPluralString("Hours", ttl / 60 / 60);
        } else if (ttl < 60 * 60 * 24 * 7) {
            return LocaleController.formatPluralString("Days", ttl / 60 / 60 / 24);
        } else {
            int days = ttl / 60 / 60 / 24;
            if (ttl % 7 == 0) {
                return LocaleController.formatPluralString("Weeks", days / 7);
            } else {
                return String.format("%s %s", LocaleController.formatPluralString("Weeks", days / 7), LocaleController.formatPluralString("Days", days % 7));
            }
        }
    }

    public String formatCurrencyString(long amount, String type) {
        type = type.toUpperCase();
        String customFormat;
        double doubleAmount;
        boolean discount = amount < 0;
        amount = Math.abs(amount);
        Currency currency = Currency.getInstance(type);
        switch (type) {
            case "CLF":
                customFormat = " %.4f";
                doubleAmount = amount / 10000.0;
                break;

            case "IRR":
                doubleAmount = amount / 100.0f;
                if (amount % 100 == 0) {
                    customFormat = " %.0f";
                } else {
                    customFormat = " %.2f";
                }
                break;

            case "BHD":
            case "IQD":
            case "JOD":
            case "KWD":
            case "LYD":
            case "OMR":
            case "TND":
                customFormat = " %.3f";
                doubleAmount = amount / 1000.0;
                break;

            case "BIF":
            case "BYR":
            case "CLP":
            case "CVE":
            case "DJF":
            case "GNF":
            case "ISK":
            case "JPY":
            case "KMF":
            case "KRW":
            case "MGA":
            case "PYG":
            case "RWF":
            case "UGX":
            case "UYI":
            case "VND":
            case "VUV":
            case "XAF":
            case "XOF":
            case "XPF":
                customFormat = " %.0f";
                doubleAmount = amount;
                break;

            case "MRO":
                customFormat = " %.1f";
                doubleAmount = amount / 10.0;
                break;

            default:
                customFormat = " %.2f";
                doubleAmount = amount / 100.0;
                break;
        }
        String result;
        if (currency != null) {
            NumberFormat format = NumberFormat.getCurrencyInstance(currentLocale != null ? currentLocale : systemDefaultLocale);
            format.setCurrency(currency);
            if (type.equals("IRR")) {
                format.setMaximumFractionDigits(0);
            }
            return (discount ? "-" : "") + format.format(doubleAmount);
        }
        return (discount ? "-" : "") + String.format(Locale.US, type + customFormat, doubleAmount);
    }

    public String formatCurrencyDecimalString(long amount, String type, boolean inludeType) {
        type = type.toUpperCase();
        String customFormat;
        double doubleAmount;
        amount = Math.abs(amount);
        switch (type) {
            case "CLF":
                customFormat = " %.4f";
                doubleAmount = amount / 10000.0;
                break;

            case "IRR":
                doubleAmount = amount / 100.0f;
                if (amount % 100 == 0) {
                    customFormat = " %.0f";
                } else {
                    customFormat = " %.2f";
                }
                break;

            case "BHD":
            case "IQD":
            case "JOD":
            case "KWD":
            case "LYD":
            case "OMR":
            case "TND":
                customFormat = " %.3f";
                doubleAmount = amount / 1000.0;
                break;

            case "BIF":
            case "BYR":
            case "CLP":
            case "CVE":
            case "DJF":
            case "GNF":
            case "ISK":
            case "JPY":
            case "KMF":
            case "KRW":
            case "MGA":
            case "PYG":
            case "RWF":
            case "UGX":
            case "UYI":
            case "VND":
            case "VUV":
            case "XAF":
            case "XOF":
            case "XPF":
                customFormat = " %.0f";
                doubleAmount = amount;
                break;

            case "MRO":
                customFormat = " %.1f";
                doubleAmount = amount / 10.0;
                break;

            default:
                customFormat = " %.2f";
                doubleAmount = amount / 100.0;
                break;
        }
        return String.format(Locale.US, inludeType ? type : "" + customFormat, doubleAmount).trim();
    }

    public static String formatStringSimple(String string, Object... args) {
        try {
            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, string, args);
            } else {
                return String.format(string, args);
            }
        } catch (Exception e) {
            FileLog.e(e);
            return "LOC_ERR: " + string;
        }
    }

    public static String formatCallDuration(int duration) {
        if (duration > 3600) {
            String result = LocaleController.formatPluralString("Hours", duration / 3600);
            int minutes = duration % 3600 / 60;
            if (minutes > 0) {
                result += ", " + LocaleController.formatPluralString("Minutes", minutes);
            }
            return result;
        } else if (duration > 60) {
            return LocaleController.formatPluralString("Minutes", duration / 60);
        } else {
            return LocaleController.formatPluralString("Seconds", duration);
        }
    }

    public void onDeviceConfigurationChange(Configuration newConfig) {
        if (changingConfiguration) {
            return;
        }
        is24HourFormat = DateFormat.is24HourFormat(ApplicationLoader.applicationContext);
        systemDefaultLocale = newConfig.locale;
        if (languageOverride != null) {
            LocaleInfo toSet = currentLocaleInfo;
            currentLocaleInfo = null;
            applyLanguage(toSet, false, false, UserConfig.selectedAccount);
        } else {
            Locale newLocale = newConfig.locale;
            if (newLocale != null) {
                String d1 = newLocale.getDisplayName();
                String d2 = currentLocale.getDisplayName();
                if (d1 != null && d2 != null && !d1.equals(d2)) {
                    recreateFormatters();
                }
                currentLocale = newLocale;
                currentPluralRules = allRules.get(currentLocale.getLanguage());
                if (currentPluralRules == null) {
                    currentPluralRules = allRules.get("de");
                }
            }
        }
    }

    public static String formatDateChat(long date) {
        try {
            Calendar rightNow = Calendar.getInstance();
            date *= 1000;

            rightNow.setTimeInMillis(date);

            if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return getInstance().chatDate.format(date);
            }
            return getInstance().chatFullDate.format(date);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR: formatDateChat";
    }

    public static String formatDate(long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return getInstance().formatterDay.format(new Date(date));
            } else if (dateDay + 1 == day && year == dateYear) {
                return getString("Yesterday", R.string.Yesterday);
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return getInstance().formatterMonth.format(new Date(date));
            } else {
                return getInstance().formatterYear.format(new Date(date));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR: formatDate";
    }

    public static String formatDateAudio(long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return LocaleController.formatString("TodayAtFormatted", R.string.TodayAtFormatted, getInstance().formatterDay.format(new Date(date)));
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString("YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance().formatterDay.format(new Date(date)));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterMonth.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
            } else {
                return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterYear.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    public static String formatDateCallLog(long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return getInstance().formatterDay.format(new Date(date));
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString("YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance().formatterDay.format(new Date(date)));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().chatDate.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
            } else {
                return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().chatFullDate.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    public static String formatLocationUpdateDate(long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                int diff = (int) (ConnectionsManager.getInstance(UserConfig.selectedAccount).getCurrentTime() - date / 1000) / 60;
                if (diff < 1) {
                    return LocaleController.getString("LocationUpdatedJustNow", R.string.LocationUpdatedJustNow);
                } else if (diff < 60) {
                    return LocaleController.formatPluralString("UpdatedMinutes", diff);
                }
                return LocaleController.formatString("LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, LocaleController.formatString("TodayAtFormatted", R.string.TodayAtFormatted, getInstance().formatterDay.format(new Date(date))));
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString("LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, LocaleController.formatString("YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance().formatterDay.format(new Date(date))));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterMonth.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, format);
            } else {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterYear.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LocationUpdatedFormatted", R.string.LocationUpdatedFormatted, format);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    public static String formatLocationLeftTime(int time) {
        String text;
        int hours = time / 60 / 60;
        time -= hours * 60 * 60;
        int minutes = time / 60;
        time -= minutes * 60;
        if (hours != 0) {
            text = String.format("%dh", hours + (minutes > 30 ? 1 : 0));
        } else if (minutes != 0) {
            text = String.format("%d", minutes + (time > 30 ? 1 : 0));
        } else {
            text = String.format("%d", time);
        }
        return text;
    }

    public static String formatDateOnline(long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return LocaleController.formatString("LastSeenFormatted", R.string.LastSeenFormatted, LocaleController.formatString("TodayAtFormatted", R.string.TodayAtFormatted, getInstance().formatterDay.format(new Date(date))));
                /*int diff = (int) (ConnectionsManager.getInstance().getCurrentTime() - date) / 60;
                if (diff < 1) {
                    return LocaleController.getString("LastSeenNow", R.string.LastSeenNow);
                } else if (diff < 60) {
                    return LocaleController.formatPluralString("LastSeenMinutes", diff);
                } else {
                    return LocaleController.formatPluralString("LastSeenHours", (int) Math.ceil(diff / 60.0f));
                }*/
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString("LastSeenFormatted", R.string.LastSeenFormatted, LocaleController.formatString("YesterdayAtFormatted", R.string.YesterdayAtFormatted, getInstance().formatterDay.format(new Date(date))));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterMonth.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LastSeenDateFormatted", R.string.LastSeenDateFormatted, format);
            } else {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, getInstance().formatterYear.format(new Date(date)), getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LastSeenDateFormatted", R.string.LastSeenDateFormatted, format);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    private FastDateFormat createFormatter(Locale locale, String format, String defaultFormat) {
        if (format == null || format.length() == 0) {
            format = defaultFormat;
        }
        FastDateFormat formatter;
        try {
            formatter = FastDateFormat.getInstance(format, locale);
        } catch (Exception e) {
            format = defaultFormat;
            formatter = FastDateFormat.getInstance(format, locale);
        }
        return formatter;
    }

    public void recreateFormatters() {
        Locale locale = currentLocale;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String lang = locale.getLanguage();
        if (lang == null) {
            lang = "de";
        }
        lang = lang.toLowerCase();
        isRTL = lang.startsWith("ar") || lang.startsWith("fa") || lang.startsWith("he") || lang.startsWith("iw");
        nameDisplayOrder = lang.equals("ko") ? 2 : 1;

        formatterMonth = createFormatter(locale, getStringInternal("formatterMonth", R.string.formatterMonth), "dd MMM");
        formatterYear = createFormatter(locale, getStringInternal("formatterYear", R.string.formatterYear), "dd.MM.yy");
        formatterYearMax = createFormatter(locale, getStringInternal("formatterYearMax", R.string.formatterYearMax), "dd.MM.yyyy");
        chatDate = createFormatter(locale, getStringInternal("chatDate", R.string.chatDate), "d MMMM");
        chatFullDate = createFormatter(locale, getStringInternal("chatFullDate", R.string.chatFullDate), "d MMMM yyyy");
        formatterWeek = createFormatter(locale, getStringInternal("formatterWeek", R.string.formatterWeek), "EEE");
        formatterMonthYear = createFormatter(locale, getStringInternal("formatterMonthYear", R.string.formatterMonthYear), "MMMM yyyy");
        formatterDay = createFormatter(lang.toLowerCase().equals("ar") || lang.toLowerCase().equals("ko") ? locale : Locale.US, is24HourFormat ? getStringInternal("formatterDay24H", R.string.formatterDay24H) : getStringInternal("formatterDay12H", R.string.formatterDay12H), is24HourFormat ? "HH:mm" : "h:mm a");
        formatterStats = createFormatter(locale, is24HourFormat ? getStringInternal("formatterStats24H", R.string.formatterStats24H) : getStringInternal("formatterStats12H", R.string.formatterStats12H), is24HourFormat ? "MMM dd yyyy, HH:mm" : "MMM dd yyyy, h:mm a");
        formatterBannedUntil = createFormatter(locale, is24HourFormat ? getStringInternal("formatterBannedUntil24H", R.string.formatterBannedUntil24H) : getStringInternal("formatterBannedUntil12H", R.string.formatterBannedUntil12H), is24HourFormat ? "MMM dd yyyy, HH:mm" : "MMM dd yyyy, h:mm a");
        formatterBannedUntilThisYear = createFormatter(locale, is24HourFormat ? getStringInternal("formatterBannedUntilThisYear24H", R.string.formatterBannedUntilThisYear24H) : getStringInternal("formatterBannedUntilThisYear12H", R.string.formatterBannedUntilThisYear12H), is24HourFormat ? "MMM dd, HH:mm" : "MMM dd, h:mm a");
    }

    public static boolean isRTLCharacter(char ch) {
        return Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
    }

    public static String formatDateForBan(long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (year == dateYear) {
                return getInstance().formatterBannedUntilThisYear.format(new Date(date));
            } else {
                return getInstance().formatterBannedUntil.format(new Date(date));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    public static String stringForMessageListDate(long date) {
        try {
            date *= 1000;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);

            if (Math.abs(System.currentTimeMillis() - date) >= 31536000000L) {
                return getInstance().formatterYear.format(new Date(date));
            } else {
                int dayDiff = dateDay - day;
                if (dayDiff == 0 || dayDiff == -1 && System.currentTimeMillis() - date < 60 * 60 * 8 * 1000) {
                    return getInstance().formatterDay.format(new Date(date));
                } else if (dayDiff > -7 && dayDiff <= -1) {
                    return getInstance().formatterWeek.format(new Date(date));
                } else {
                    return getInstance().formatterMonth.format(new Date(date));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    public static String formatShortNumber(int number, int[] rounded) {
        StringBuilder K = new StringBuilder();
        int lastDec = 0;
        int KCount = 0;
        while (number / 1000 > 0) {
            K.append("K");
            lastDec = (number % 1000) / 100;
            number /= 1000;
        }
        if (rounded != null) {
            double value = number + lastDec / 10.0;
            for (int a = 0; a < K.length(); a++) {
                value *= 1000;
            }
            rounded[0] = (int) value;
        }
        if (lastDec != 0 && K.length() > 0) {
            if (K.length() == 2) {
                return String.format(Locale.US, "%d.%dM", number, lastDec);
            } else {
                return String.format(Locale.US, "%d.%d%s", number, lastDec, K.toString());
            }
        }
        if (K.length() == 2) {
            return String.format(Locale.US, "%dM", number);
        } else {
            return String.format(Locale.US, "%d%s", number, K.toString());
        }
    }

    public static String formatUserStatus(int currentAccount, TLRPC.User user) {
        if (user != null && user.status != null && user.status.expires == 0) {
            if (user.status instanceof TLRPC.TL_userStatusRecently) {
                user.status.expires = -100;
            } else if (user.status instanceof TLRPC.TL_userStatusLastWeek) {
                user.status.expires = -101;
            } else if (user.status instanceof TLRPC.TL_userStatusLastMonth) {
                user.status.expires = -102;
            }
        }
        if (user != null && user.status != null && user.status.expires <= 0) {
            if (MessagesController.getInstance(currentAccount).onlinePrivacy.containsKey(user.id)) {
                return getString("Online", R.string.Online);
            }
        }
        if (user == null || user.status == null || user.status.expires == 0 || UserObject.isDeleted(user) || user instanceof TLRPC.TL_userEmpty) {
            return getString("ALongTimeAgo", R.string.ALongTimeAgo);
        } else {
            int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            if (user.status.expires > currentTime) {
                return getString("Online", R.string.Online);
            } else {
                if (user.status.expires == -1) {
                    return getString("Invisible", R.string.Invisible);
                } else if (user.status.expires == -100) {
                    return getString("Lately", R.string.Lately);
                } else if (user.status.expires == -101) {
                    return getString("WithinAWeek", R.string.WithinAWeek);
                } else if (user.status.expires == -102) {
                    return getString("WithinAMonth", R.string.WithinAMonth);
                }  else {
                    return formatDateOnline(user.status.expires);
                }
            }
        }
    }

    private String escapeString(String str) {
        if (str.contains("[CDATA")) {
            return str;
        }
        return str.replace("<", "&lt;").replace(">", "&gt;").replace("& ", "&amp; ");
    }

    public void saveRemoteLocaleStrings(final TLRPC.TL_langPackDifference difference, int currentAccount) {
        if (difference == null || difference.strings.isEmpty() || currentLocaleInfo == null) {
            return;
        }
        final String langCode = difference.lang_code.replace('-', '_').toLowerCase();
        if (!langCode.equals(currentLocaleInfo.shortName)) {
            return;
        }
        File finalFile = new File(ApplicationLoader.getFilesDirFixed(), "remote_" + langCode + ".xml");
        try {
            final HashMap<String, String> values;
            if (difference.from_version == 0) {
                values = new HashMap<>();
            } else {
                values = getLocaleFileStrings(finalFile, true);
            }
            for (int a = 0; a < difference.strings.size(); a++) {
                TLRPC.LangPackString string = difference.strings.get(a);
                if (string instanceof TLRPC.TL_langPackString) {
                    values.put(string.key, escapeString(string.value));
                } else if (string instanceof TLRPC.TL_langPackStringPluralized) {
                    values.put(string.key + "_zero", string.zero_value != null ? escapeString(string.zero_value) : "");
                    values.put(string.key + "_one", string.one_value != null ? escapeString(string.one_value) : "");
                    values.put(string.key + "_two", string.two_value != null ? escapeString(string.two_value) : "");
                    values.put(string.key + "_few", string.few_value != null ? escapeString(string.few_value) : "");
                    values.put(string.key + "_many", string.many_value != null ? escapeString(string.many_value) : "");
                    values.put(string.key + "_other", string.other_value != null ? escapeString(string.other_value) : "");
                } else if (string instanceof TLRPC.TL_langPackStringDeleted) {
                    values.remove(string.key);
                }
            }
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("save locale file to " + finalFile);
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(finalFile));
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<resources>\n");
            for (HashMap.Entry<String, String> entry : values.entrySet()) {
                writer.write(String.format("<string name=\"%1$s\">%2$s</string>\n", entry.getKey(), entry.getValue()));
            }
            writer.write("</resources>");
            writer.close();
            final HashMap<String, String> valuesToSet = getLocaleFileStrings(finalFile);
            AndroidUtilities.runOnUIThread(() -> {
                LocaleInfo localeInfo = getLanguageFromDict(langCode);
                if (localeInfo != null) {
                    localeInfo.version = difference.version;
                }
                saveOtherLanguages();
                if (currentLocaleInfo != null && currentLocaleInfo.isLocal()) {
                    return;
                }
                try {
                    Locale newLocale;
                    String[] args = localeInfo.shortName.split("_");
                    if (args.length == 1) {
                        newLocale = new Locale(localeInfo.shortName);
                    } else {
                        newLocale = new Locale(args[0], args[1]);
                    }
                    if (newLocale != null) {
                        languageOverride = localeInfo.shortName;

                        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("language", localeInfo.getKey());
                        editor.commit();
                    }
                    if (newLocale != null) {
                        localeValues = valuesToSet;
                        currentLocale = newLocale;
                        currentLocaleInfo = localeInfo;
                        currentPluralRules = allRules.get(currentLocale.getLanguage());
                        if (currentPluralRules == null) {
                            currentPluralRules = allRules.get("de");
                        }
                        changingConfiguration = true;
                        Locale.setDefault(currentLocale);
                        Configuration config = new Configuration();
                        config.locale = currentLocale;
                        ApplicationLoader.applicationContext.getResources().updateConfiguration(config, ApplicationLoader.applicationContext.getResources().getDisplayMetrics());
                        changingConfiguration = false;
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                    changingConfiguration = false;
                }
                recreateFormatters();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
            });
        } catch (Exception ignore) {

        }
    }

    public void loadRemoteLanguages(final int currentAccount) {
        if (loadingRemoteLanguages) {
            return;
        }
        loadingRemoteLanguages = true;
        TLRPC.TL_langpack_getLanguages req = new TLRPC.TL_langpack_getLanguages();
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response != null) {
                AndroidUtilities.runOnUIThread(() -> {
                    loadingRemoteLanguages = false;
                    TLRPC.Vector res = (TLRPC.Vector) response;
                    HashMap<String, LocaleInfo> remoteLoaded = new HashMap<>();
                    remoteLanguages.clear();
                    for (int a = 0; a < res.objects.size(); a++) {
                        TLRPC.TL_langPackLanguage language = (TLRPC.TL_langPackLanguage) res.objects.get(a);
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.d("loaded lang " + language.name);
                        }
                        LocaleInfo localeInfo = new LocaleInfo();
                        localeInfo.nameEnglish = language.name;
                        localeInfo.name = language.native_name;
                        localeInfo.shortName = language.lang_code.replace('-', '_').toLowerCase();
                        localeInfo.pathToFile = "remote";

                        LocaleInfo existing = getLanguageFromDict(localeInfo.getKey());
                        if (existing == null) {
                            languages.add(localeInfo);
                            languagesDict.put(localeInfo.getKey(), localeInfo);
                            existing = localeInfo;
                        } else {
                            existing.nameEnglish = localeInfo.nameEnglish;
                            existing.name = localeInfo.name;
                            existing.pathToFile = localeInfo.pathToFile;
                            localeInfo = existing;
                        }
                        remoteLanguages.add(localeInfo);
                        remoteLoaded.put(localeInfo.getKey(), existing);
                    }
                    for (int a = 0; a < languages.size(); a++) {
                        LocaleInfo info = languages.get(a);
                        if (info.isBuiltIn() || !info.isRemote()) {
                            continue;
                        }
                        LocaleInfo existing = remoteLoaded.get(info.getKey());
                        if (existing == null) {
                            if (BuildVars.LOGS_ENABLED) {
                                FileLog.d("remove lang " + info.getKey());
                            }
                            languages.remove(a);
                            languagesDict.remove(info.getKey());
                            a--;
                            if (info == currentLocaleInfo) {
                                if (systemDefaultLocale.getLanguage() != null) {
                                    info = getLanguageFromDict(systemDefaultLocale.getLanguage());
                                }
                                if (info == null) {
                                    info = getLanguageFromDict(getLocaleString(systemDefaultLocale));
                                }
                                if (info == null) {
                                    info = getLanguageFromDict("de");
                                }
                                applyLanguage(info, true, false, currentAccount);
                                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
                            }
                        }
                    }
                    saveOtherLanguages();
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.suggestedLangpack);
                    applyLanguage(currentLocaleInfo, true, false, currentAccount);
                });
            }
        }, ConnectionsManager.RequestFlagWithoutLogin);
    }

    private void applyRemoteLanguage(LocaleInfo localeInfo, boolean force, final int currentAccount) {
        if (localeInfo == null || localeInfo != null && !localeInfo.isRemote()) {
            return;
        }
        if (localeInfo.version != 0 && !force) {
            TLRPC.TL_langpack_getDifference req = new TLRPC.TL_langpack_getDifference();
            req.from_version = localeInfo.version;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (response != null) {
                    AndroidUtilities.runOnUIThread(() -> saveRemoteLocaleStrings((TLRPC.TL_langPackDifference) response, currentAccount));
                }
            }, ConnectionsManager.RequestFlagWithoutLogin);
        } else {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                ConnectionsManager.setLangCode(localeInfo.shortName);
            }
            TLRPC.TL_langpack_getLangPack req = new TLRPC.TL_langpack_getLangPack();
            req.lang_code = localeInfo.shortName.replace("_", "-");
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (TLObject response, TLRPC.TL_error error) -> {
                if (response != null) {
                    AndroidUtilities.runOnUIThread(() -> saveRemoteLocaleStrings((TLRPC.TL_langPackDifference) response, currentAccount));
                }
            }, ConnectionsManager.RequestFlagWithoutLogin);
        }
    }

    public String getTranslitString(String src) {
        return getTranslitString(src, false);
    }

    public String getTranslitString(String src, boolean onlyEnglish) {
        if (src == null) {
            return null;
        }
        if (translitChars == null) {
            translitChars = new HashMap<>(520);
            translitChars.put("ȼ", "c");
            translitChars.put("ᶇ", "n");
            translitChars.put("ɖ", "d");
            translitChars.put("ỿ", "y");
            translitChars.put("ᴓ", "o");
            translitChars.put("ø", "o");
            translitChars.put("ḁ", "a");
            translitChars.put("ʯ", "h");
            translitChars.put("ŷ", "y");
            translitChars.put("ʞ", "k");
            translitChars.put("ừ", "u");
            translitChars.put("ꜳ", "aa");
            translitChars.put("ĳ", "ij");
            translitChars.put("ḽ", "l");
            translitChars.put("ɪ", "i");
            translitChars.put("ḇ", "b");
            translitChars.put("ʀ", "r");
            translitChars.put("ě", "e");
            translitChars.put("ﬃ", "ffi");
            translitChars.put("ơ", "o");
            translitChars.put("ⱹ", "r");
            translitChars.put("ồ", "o");
            translitChars.put("ǐ", "i");
            translitChars.put("ꝕ", "p");
            translitChars.put("ý", "y");
            translitChars.put("ḝ", "e");
            translitChars.put("ₒ", "o");
            translitChars.put("ⱥ", "a");
            translitChars.put("ʙ", "b");
            translitChars.put("ḛ", "e");
            translitChars.put("ƈ", "c");
            translitChars.put("ɦ", "h");
            translitChars.put("ᵬ", "b");
            translitChars.put("ṣ", "s");
            translitChars.put("đ", "d");
            translitChars.put("ỗ", "o");
            translitChars.put("ɟ", "j");
            translitChars.put("ẚ", "a");
            translitChars.put("ɏ", "y");
            translitChars.put("л", "l");
            translitChars.put("ʌ", "v");
            translitChars.put("ꝓ", "p");
            translitChars.put("ﬁ", "fi");
            translitChars.put("ᶄ", "k");
            translitChars.put("ḏ", "d");
            translitChars.put("ᴌ", "l");
            translitChars.put("ė", "e");
            translitChars.put("ё", "yo");
            translitChars.put("ᴋ", "k");
            translitChars.put("ċ", "c");
            translitChars.put("ʁ", "r");
            translitChars.put("ƕ", "hv");
            translitChars.put("ƀ", "b");
            translitChars.put("ṍ", "o");
            translitChars.put("ȣ", "ou");
            translitChars.put("ǰ", "j");
            translitChars.put("ᶃ", "g");
            translitChars.put("ṋ", "n");
            translitChars.put("ɉ", "j");
            translitChars.put("ǧ", "g");
            translitChars.put("ǳ", "dz");
            translitChars.put("ź", "z");
            translitChars.put("ꜷ", "au");
            translitChars.put("ǖ", "u");
            translitChars.put("ᵹ", "g");
            translitChars.put("ȯ", "o");
            translitChars.put("ɐ", "a");
            translitChars.put("ą", "a");
            translitChars.put("õ", "o");
            translitChars.put("ɻ", "r");
            translitChars.put("ꝍ", "o");
            translitChars.put("ǟ", "a");
            translitChars.put("ȴ", "l");
            translitChars.put("ʂ", "s");
            translitChars.put("ﬂ", "fl");
            translitChars.put("ȉ", "i");
            translitChars.put("ⱻ", "e");
            translitChars.put("ṉ", "n");
            translitChars.put("ï", "i");
            translitChars.put("ñ", "n");
            translitChars.put("ᴉ", "i");
            translitChars.put("ʇ", "t");
            translitChars.put("ẓ", "z");
            translitChars.put("ỷ", "y");
            translitChars.put("ȳ", "y");
            translitChars.put("ṩ", "s");
            translitChars.put("ɽ", "r");
            translitChars.put("ĝ", "g");
            translitChars.put("в", "v");
            translitChars.put("ᴝ", "u");
            translitChars.put("ḳ", "k");
            translitChars.put("ꝫ", "et");
            translitChars.put("ī", "i");
            translitChars.put("ť", "t");
            translitChars.put("ꜿ", "c");
            translitChars.put("ʟ", "l");
            translitChars.put("ꜹ", "av");
            translitChars.put("û", "u");
            translitChars.put("æ", "ae");
            translitChars.put("и", "i");
            translitChars.put("ă", "a");
            translitChars.put("ǘ", "u");
            translitChars.put("ꞅ", "s");
            translitChars.put("ᵣ", "r");
            translitChars.put("ᴀ", "a");
            translitChars.put("ƃ", "b");
            translitChars.put("ḩ", "h");
            translitChars.put("ṧ", "s");
            translitChars.put("ₑ", "e");
            translitChars.put("ʜ", "h");
            translitChars.put("ẋ", "x");
            translitChars.put("ꝅ", "k");
            translitChars.put("ḋ", "d");
            translitChars.put("ƣ", "oi");
            translitChars.put("ꝑ", "p");
            translitChars.put("ħ", "h");
            translitChars.put("ⱴ", "v");
            translitChars.put("ẇ", "w");
            translitChars.put("ǹ", "n");
            translitChars.put("ɯ", "m");
            translitChars.put("ɡ", "g");
            translitChars.put("ɴ", "n");
            translitChars.put("ᴘ", "p");
            translitChars.put("ᵥ", "v");
            translitChars.put("ū", "u");
            translitChars.put("ḃ", "b");
            translitChars.put("ṗ", "p");
            translitChars.put("ь", "");
            translitChars.put("å", "a");
            translitChars.put("ɕ", "c");
            translitChars.put("ọ", "o");
            translitChars.put("ắ", "a");
            translitChars.put("ƒ", "f");
            translitChars.put("ǣ", "ae");
            translitChars.put("ꝡ", "vy");
            translitChars.put("ﬀ", "ff");
            translitChars.put("ᶉ", "r");
            translitChars.put("ô", "o");
            translitChars.put("ǿ", "o");
            translitChars.put("ṳ", "u");
            translitChars.put("ȥ", "z");
            translitChars.put("ḟ", "f");
            translitChars.put("ḓ", "d");
            translitChars.put("ȇ", "e");
            translitChars.put("ȕ", "u");
            translitChars.put("п", "p");
            translitChars.put("ȵ", "n");
            translitChars.put("ʠ", "q");
            translitChars.put("ấ", "a");
            translitChars.put("ǩ", "k");
            translitChars.put("ĩ", "i");
            translitChars.put("ṵ", "u");
            translitChars.put("ŧ", "t");
            translitChars.put("ɾ", "r");
            translitChars.put("ƙ", "k");
            translitChars.put("ṫ", "t");
            translitChars.put("ꝗ", "q");
            translitChars.put("ậ", "a");
            translitChars.put("н", "n");
            translitChars.put("ʄ", "j");
            translitChars.put("ƚ", "l");
            translitChars.put("ᶂ", "f");
            translitChars.put("д", "d");
            translitChars.put("ᵴ", "s");
            translitChars.put("ꞃ", "r");
            translitChars.put("ᶌ", "v");
            translitChars.put("ɵ", "o");
            translitChars.put("ḉ", "c");
            translitChars.put("ᵤ", "u");
            translitChars.put("ẑ", "z");
            translitChars.put("ṹ", "u");
            translitChars.put("ň", "n");
            translitChars.put("ʍ", "w");
            translitChars.put("ầ", "a");
            translitChars.put("ǉ", "lj");
            translitChars.put("ɓ", "b");
            translitChars.put("ɼ", "r");
            translitChars.put("ò", "o");
            translitChars.put("ẘ", "w");
            translitChars.put("ɗ", "d");
            translitChars.put("ꜽ", "ay");
            translitChars.put("ư", "u");
            translitChars.put("ᶀ", "b");
            translitChars.put("ǜ", "u");
            translitChars.put("ẹ", "e");
            translitChars.put("ǡ", "a");
            translitChars.put("ɥ", "h");
            translitChars.put("ṏ", "o");
            translitChars.put("ǔ", "u");
            translitChars.put("ʎ", "y");
            translitChars.put("ȱ", "o");
            translitChars.put("ệ", "e");
            translitChars.put("ế", "e");
            translitChars.put("ĭ", "i");
            translitChars.put("ⱸ", "e");
            translitChars.put("ṯ", "t");
            translitChars.put("ᶑ", "d");
            translitChars.put("ḧ", "h");
            translitChars.put("ṥ", "s");
            translitChars.put("ë", "e");
            translitChars.put("ᴍ", "m");
            translitChars.put("ö", "o");
            translitChars.put("é", "e");
            translitChars.put("ı", "i");
            translitChars.put("ď", "d");
            translitChars.put("ᵯ", "m");
            translitChars.put("ỵ", "y");
            translitChars.put("я", "ya");
            translitChars.put("ŵ", "w");
            translitChars.put("ề", "e");
            translitChars.put("ứ", "u");
            translitChars.put("ƶ", "z");
            translitChars.put("ĵ", "j");
            translitChars.put("ḍ", "d");
            translitChars.put("ŭ", "u");
            translitChars.put("ʝ", "j");
            translitChars.put("ж", "zh");
            translitChars.put("ê", "e");
            translitChars.put("ǚ", "u");
            translitChars.put("ġ", "g");
            translitChars.put("ṙ", "r");
            translitChars.put("ƞ", "n");
            translitChars.put("ъ", "");
            translitChars.put("ḗ", "e");
            translitChars.put("ẝ", "s");
            translitChars.put("ᶁ", "d");
            translitChars.put("ķ", "k");
            translitChars.put("ᴂ", "ae");
            translitChars.put("ɘ", "e");
            translitChars.put("ợ", "o");
            translitChars.put("ḿ", "m");
            translitChars.put("ꜰ", "f");
            translitChars.put("а", "a");
            translitChars.put("ẵ", "a");
            translitChars.put("ꝏ", "oo");
            translitChars.put("ᶆ", "m");
            translitChars.put("ᵽ", "p");
            translitChars.put("ц", "ts");
            translitChars.put("ữ", "u");
            translitChars.put("ⱪ", "k");
            translitChars.put("ḥ", "h");
            translitChars.put("ţ", "t");
            translitChars.put("ᵱ", "p");
            translitChars.put("ṁ", "m");
            translitChars.put("á", "a");
            translitChars.put("ᴎ", "n");
            translitChars.put("ꝟ", "v");
            translitChars.put("è", "e");
            translitChars.put("ᶎ", "z");
            translitChars.put("ꝺ", "d");
            translitChars.put("ᶈ", "p");
            translitChars.put("м", "m");
            translitChars.put("ɫ", "l");
            translitChars.put("ᴢ", "z");
            translitChars.put("ɱ", "m");
            translitChars.put("ṝ", "r");
            translitChars.put("ṽ", "v");
            translitChars.put("ũ", "u");
            translitChars.put("ß", "ss");
            translitChars.put("т", "t");
            translitChars.put("ĥ", "h");
            translitChars.put("ᵵ", "t");
            translitChars.put("ʐ", "z");
            translitChars.put("ṟ", "r");
            translitChars.put("ɲ", "n");
            translitChars.put("à", "a");
            translitChars.put("ẙ", "y");
            translitChars.put("ỳ", "y");
            translitChars.put("ᴔ", "oe");
            translitChars.put("ы", "i");
            translitChars.put("ₓ", "x");
            translitChars.put("ȗ", "u");
            translitChars.put("ⱼ", "j");
            translitChars.put("ẫ", "a");
            translitChars.put("ʑ", "z");
            translitChars.put("ẛ", "s");
            translitChars.put("ḭ", "i");
            translitChars.put("ꜵ", "ao");
            translitChars.put("ɀ", "z");
            translitChars.put("ÿ", "y");
            translitChars.put("ǝ", "e");
            translitChars.put("ǭ", "o");
            translitChars.put("ᴅ", "d");
            translitChars.put("ᶅ", "l");
            translitChars.put("ù", "u");
            translitChars.put("ạ", "a");
            translitChars.put("ḅ", "b");
            translitChars.put("ụ", "u");
            translitChars.put("к", "k");
            translitChars.put("ằ", "a");
            translitChars.put("ᴛ", "t");
            translitChars.put("ƴ", "y");
            translitChars.put("ⱦ", "t");
            translitChars.put("з", "z");
            translitChars.put("ⱡ", "l");
            translitChars.put("ȷ", "j");
            translitChars.put("ᵶ", "z");
            translitChars.put("ḫ", "h");
            translitChars.put("ⱳ", "w");
            translitChars.put("ḵ", "k");
            translitChars.put("ờ", "o");
            translitChars.put("î", "i");
            translitChars.put("ģ", "g");
            translitChars.put("ȅ", "e");
            translitChars.put("ȧ", "a");
            translitChars.put("ẳ", "a");
            translitChars.put("щ", "sch");
            translitChars.put("ɋ", "q");
            translitChars.put("ṭ", "t");
            translitChars.put("ꝸ", "um");
            translitChars.put("ᴄ", "c");
            translitChars.put("ẍ", "x");
            translitChars.put("ủ", "u");
            translitChars.put("ỉ", "i");
            translitChars.put("ᴚ", "r");
            translitChars.put("ś", "s");
            translitChars.put("ꝋ", "o");
            translitChars.put("ỹ", "y");
            translitChars.put("ṡ", "s");
            translitChars.put("ǌ", "nj");
            translitChars.put("ȁ", "a");
            translitChars.put("ẗ", "t");
            translitChars.put("ĺ", "l");
            translitChars.put("ž", "z");
            translitChars.put("ᵺ", "th");
            translitChars.put("ƌ", "d");
            translitChars.put("ș", "s");
            translitChars.put("š", "s");
            translitChars.put("ᶙ", "u");
            translitChars.put("ẽ", "e");
            translitChars.put("ẜ", "s");
            translitChars.put("ɇ", "e");
            translitChars.put("ṷ", "u");
            translitChars.put("ố", "o");
            translitChars.put("ȿ", "s");
            translitChars.put("ᴠ", "v");
            translitChars.put("ꝭ", "is");
            translitChars.put("ᴏ", "o");
            translitChars.put("ɛ", "e");
            translitChars.put("ǻ", "a");
            translitChars.put("ﬄ", "ffl");
            translitChars.put("ⱺ", "o");
            translitChars.put("ȋ", "i");
            translitChars.put("ᵫ", "ue");
            translitChars.put("ȡ", "d");
            translitChars.put("ⱬ", "z");
            translitChars.put("ẁ", "w");
            translitChars.put("ᶏ", "a");
            translitChars.put("ꞇ", "t");
            translitChars.put("ğ", "g");
            translitChars.put("ɳ", "n");
            translitChars.put("ʛ", "g");
            translitChars.put("ᴜ", "u");
            translitChars.put("ф", "f");
            translitChars.put("ẩ", "a");
            translitChars.put("ṅ", "n");
            translitChars.put("ɨ", "i");
            translitChars.put("ᴙ", "r");
            translitChars.put("ǎ", "a");
            translitChars.put("ſ", "s");
            translitChars.put("у", "u");
            translitChars.put("ȫ", "o");
            translitChars.put("ɿ", "r");
            translitChars.put("ƭ", "t");
            translitChars.put("ḯ", "i");
            translitChars.put("ǽ", "ae");
            translitChars.put("ⱱ", "v");
            translitChars.put("ɶ", "oe");
            translitChars.put("ṃ", "m");
            translitChars.put("ż", "z");
            translitChars.put("ĕ", "e");
            translitChars.put("ꜻ", "av");
            translitChars.put("ở", "o");
            translitChars.put("ễ", "e");
            translitChars.put("ɬ", "l");
            translitChars.put("ị", "i");
            translitChars.put("ᵭ", "d");
            translitChars.put("ﬆ", "st");
            translitChars.put("ḷ", "l");
            translitChars.put("ŕ", "r");
            translitChars.put("ᴕ", "ou");
            translitChars.put("ʈ", "t");
            translitChars.put("ā", "a");
            translitChars.put("э", "e");
            translitChars.put("ḙ", "e");
            translitChars.put("ᴑ", "o");
            translitChars.put("ç", "c");
            translitChars.put("ᶊ", "s");
            translitChars.put("ặ", "a");
            translitChars.put("ų", "u");
            translitChars.put("ả", "a");
            translitChars.put("ǥ", "g");
            translitChars.put("р", "r");
            translitChars.put("ꝁ", "k");
            translitChars.put("ẕ", "z");
            translitChars.put("ŝ", "s");
            translitChars.put("ḕ", "e");
            translitChars.put("ɠ", "g");
            translitChars.put("ꝉ", "l");
            translitChars.put("ꝼ", "f");
            translitChars.put("ᶍ", "x");
            translitChars.put("х", "h");
            translitChars.put("ǒ", "o");
            translitChars.put("ę", "e");
            translitChars.put("ổ", "o");
            translitChars.put("ƫ", "t");
            translitChars.put("ǫ", "o");
            translitChars.put("i̇", "i");
            translitChars.put("ṇ", "n");
            translitChars.put("ć", "c");
            translitChars.put("ᵷ", "g");
            translitChars.put("ẅ", "w");
            translitChars.put("ḑ", "d");
            translitChars.put("ḹ", "l");
            translitChars.put("ч", "ch");
            translitChars.put("œ", "oe");
            translitChars.put("ᵳ", "r");
            translitChars.put("ļ", "l");
            translitChars.put("ȑ", "r");
            translitChars.put("ȭ", "o");
            translitChars.put("ᵰ", "n");
            translitChars.put("ᴁ", "ae");
            translitChars.put("ŀ", "l");
            translitChars.put("ä", "a");
            translitChars.put("ƥ", "p");
            translitChars.put("ỏ", "o");
            translitChars.put("į", "i");
            translitChars.put("ȓ", "r");
            translitChars.put("ǆ", "dz");
            translitChars.put("ḡ", "g");
            translitChars.put("ṻ", "u");
            translitChars.put("ō", "o");
            translitChars.put("ľ", "l");
            translitChars.put("ẃ", "w");
            translitChars.put("ț", "t");
            translitChars.put("ń", "n");
            translitChars.put("ɍ", "r");
            translitChars.put("ȃ", "a");
            translitChars.put("ü", "u");
            translitChars.put("ꞁ", "l");
            translitChars.put("ᴐ", "o");
            translitChars.put("ớ", "o");
            translitChars.put("ᴃ", "b");
            translitChars.put("ɹ", "r");
            translitChars.put("ᵲ", "r");
            translitChars.put("ʏ", "y");
            translitChars.put("ᵮ", "f");
            translitChars.put("ⱨ", "h");
            translitChars.put("ŏ", "o");
            translitChars.put("ú", "u");
            translitChars.put("ṛ", "r");
            translitChars.put("ʮ", "h");
            translitChars.put("ó", "o");
            translitChars.put("ů", "u");
            translitChars.put("ỡ", "o");
            translitChars.put("ṕ", "p");
            translitChars.put("ᶖ", "i");
            translitChars.put("ự", "u");
            translitChars.put("ã", "a");
            translitChars.put("ᵢ", "i");
            translitChars.put("ṱ", "t");
            translitChars.put("ể", "e");
            translitChars.put("ử", "u");
            translitChars.put("í", "i");
            translitChars.put("ɔ", "o");
            translitChars.put("с", "s");
            translitChars.put("й", "i");
            translitChars.put("ɺ", "r");
            translitChars.put("ɢ", "g");
            translitChars.put("ř", "r");
            translitChars.put("ẖ", "h");
            translitChars.put("ű", "u");
            translitChars.put("ȍ", "o");
            translitChars.put("ш", "sh");
            translitChars.put("ḻ", "l");
            translitChars.put("ḣ", "h");
            translitChars.put("ȶ", "t");
            translitChars.put("ņ", "n");
            translitChars.put("ᶒ", "e");
            translitChars.put("ì", "i");
            translitChars.put("ẉ", "w");
            translitChars.put("б", "b");
            translitChars.put("ē", "e");
            translitChars.put("ᴇ", "e");
            translitChars.put("ł", "l");
            translitChars.put("ộ", "o");
            translitChars.put("ɭ", "l");
            translitChars.put("ẏ", "y");
            translitChars.put("ᴊ", "j");
            translitChars.put("ḱ", "k");
            translitChars.put("ṿ", "v");
            translitChars.put("ȩ", "e");
            translitChars.put("â", "a");
            translitChars.put("ş", "s");
            translitChars.put("ŗ", "r");
            translitChars.put("ʋ", "v");
            translitChars.put("ₐ", "a");
            translitChars.put("ↄ", "c");
            translitChars.put("ᶓ", "e");
            translitChars.put("ɰ", "m");
            translitChars.put("е", "e");
            translitChars.put("ᴡ", "w");
            translitChars.put("ȏ", "o");
            translitChars.put("č", "c");
            translitChars.put("ǵ", "g");
            translitChars.put("ĉ", "c");
            translitChars.put("ю", "yu");
            translitChars.put("ᶗ", "o");
            translitChars.put("ꝃ", "k");
            translitChars.put("ꝙ", "q");
            translitChars.put("г", "g");
            translitChars.put("ṑ", "o");
            translitChars.put("ꜱ", "s");
            translitChars.put("ṓ", "o");
            translitChars.put("ȟ", "h");
            translitChars.put("ő", "o");
            translitChars.put("ꜩ", "tz");
            translitChars.put("ẻ", "e");
            translitChars.put("о", "o");
        }
        StringBuilder dst = new StringBuilder(src.length());
        int len = src.length();
        boolean upperCase = false;
        for (int a = 0; a < len; a++) {
            String ch = src.substring(a, a + 1);
            if (onlyEnglish) {
                String lower = ch.toLowerCase();
                upperCase = !ch.equals(lower);
                ch = lower;
            }
            String tch = translitChars.get(ch);
            if (tch != null) {
                if (onlyEnglish && upperCase) {
                    if (tch.length() > 1) {
                        tch = tch.substring(0, 1).toUpperCase() + tch.substring(1);
                    } else {
                        tch = tch.toUpperCase();
                    }
                }
                dst.append(tch);
            } else {
                if (onlyEnglish) {
                    char c = ch.charAt(0);
                    if (((c < 'a' || c > 'z') || (c < '0' || c > '9')) && c != ' ' && c != '\'' && c != ',' && c != '.' && c != '&' && c != '-' && c != '/') {
                        return null;
                    }
                    if (upperCase) {
                        ch = ch.toUpperCase();
                    }
                }
                dst.append(ch);
            }
        }
        return dst.toString();
    }

    abstract public static class PluralRules {
        abstract int quantityForNumber(int n);
    }

    public static class PluralRules_Zero extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0 || count == 1) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Welsh extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (count == 3) {
                return QUANTITY_FEW;
            } else if (count == 6) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Two extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Tachelhit extends PluralRules {
        public int quantityForNumber(int count) {
            if (count >= 0 && count <= 1) {
                return QUANTITY_ONE;
            } else if (count >= 2 && count <= 10) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Slovenian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (rem100 == 1) {
                return QUANTITY_ONE;
            } else if (rem100 == 2) {
                return QUANTITY_TWO;
            } else if (rem100 >= 3 && rem100 <= 4) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Romanian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if ((count == 0 || (rem100 >= 1 && rem100 <= 19))) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Polish extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14) && !(rem100 >= 22 && rem100 <= 24)) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_One extends PluralRules {
        public int quantityForNumber(int count) {
            return count == 1 ? QUANTITY_ONE : QUANTITY_OTHER;
        }
    }

    public static class PluralRules_None extends PluralRules {
        public int quantityForNumber(int count) {
            return QUANTITY_OTHER;
        }
    }

    public static class PluralRules_Maltese extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 0 || (rem100 >= 2 && rem100 <= 10)) {
                return QUANTITY_FEW;
            } else if (rem100 >= 11 && rem100 <= 19) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Macedonian extends PluralRules {
        public int quantityForNumber(int count) {
            if (count % 10 == 1 && count != 11) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Lithuanian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Latvian extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count % 10 == 1 && count % 100 != 11) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Langi extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count > 0 && count < 2) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_French extends PluralRules {
        public int quantityForNumber(int count) {
            if (count >= 0 && count < 2) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Czech extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count >= 2 && count <= 4) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Breton extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (count == 3) {
                return QUANTITY_FEW;
            } else if (count == 6) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Balkan extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (rem10 == 1 && rem100 != 11) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) {
                return QUANTITY_FEW;
            } else if ((rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14))) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Arabic extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (rem100 >= 3 && rem100 <= 10) {
                return QUANTITY_FEW;
            } else if (rem100 >= 11 && rem100 <= 99) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static String addNbsp(String src) {
        return src.replace(' ', '\u00A0');
    }
}

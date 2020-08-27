package ca.uvic.ece.ecg.heartcarer1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferencesUtility {
    private static final String TAG = "SharedPreferences";

    /**
     * Use SharedPreferences to store the unsent content of patient note
     */
    public synchronized static void setPreference(Context context, String st, String et, String comments) {
        Log.i(TAG, "setPreference");
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.notes), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("from", st);
        editor.putString("to", et);
        editor.putString("notes", comments);
        editor.apply();
    }

    /**
     * Clear SharedPreferences when the content is sent successfully
     */
    public synchronized static void emptyReference(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.notes), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }

    public static String getStartTime(Context context) {
        return getString(context, "from");
    }

    public static String getEndTime(Context context) {
        return getString(context, "to");
    }

    public static String getNotes(Context context) {
        return getString(context, "notes");
    }

    private static String getString(Context context, String key) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.notes), Context.MODE_PRIVATE);
        return sharedPref.getString(key, null);
    }

}

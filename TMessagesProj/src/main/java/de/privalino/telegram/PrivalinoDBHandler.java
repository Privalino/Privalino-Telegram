package de.privalino.telegram;

/**
 * Created by pinguin on 10.08.17.
 */

import android.database.Cursor;
import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class PrivalinoDBHandler extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "MyDBName.db";

    public PrivalinoDBHandler(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE dialogs " +
                        "(ID INT PRIMARY KEY autoincrement    NOT NULL," +
                        " DIALOG           LONG    NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public boolean isDialogFirstContact(long dialog) {
        SQLiteDatabase db;

        db = this.getWritableDatabase();

        Cursor res = db.rawQuery("SELECT dialog FROM dialogs where dialog = " + Long.toString(dialog) + " ;", null);
        if (res.getColumnCount() >= 1) {
            return false;
        }

        db.execSQL("INSERT INTO dialog (dialog) VALUES ( " + Long.toString(dialog) + " );", null);
        return true;
    }
}

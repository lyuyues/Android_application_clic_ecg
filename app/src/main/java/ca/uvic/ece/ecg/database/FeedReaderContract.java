package ca.uvic.ece.ecg.database;

import android.provider.BaseColumns;

public final class FeedReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public FeedReaderContract() {}

    /* Inner class that defines the table contents */
    public static abstract class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "quick_results";
        public static final String COLUMN_NAME_UserID = "UserId";
        public static final String COLUMN_NAME_TestTime = "TestTime";
        public static final String COLUMN_NAME_HR = "HR";
        public static final String COLUMN_NAME_QRS = "QRS";
        public static final String COLUMN_NAME_QTC = "QTC";
        public static final String COLUMN_NAME_PR = "PR";
        public static final String COLUMN_NAME_ST = "ST";
        public static final String COLUMN_NAME_data_name = "DataName";
    }
}
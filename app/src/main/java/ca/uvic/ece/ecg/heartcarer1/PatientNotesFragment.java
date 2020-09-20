package ca.uvic.ece.ecg.heartcarer1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * This Fragment allows user to change input notes
 */
public class PatientNotesFragment extends Fragment {
    private static final String TAG = "PatientNotesFragment";
    private View view;
    private TextView commentView;
    private TextView startTimeView;
    private TextView endTimeView;
    private String comments = "";
    private Button buttonSend;
    private AlertDialog.Builder dialog;
    private final SimpleDateFormat showTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm aaa", Locale.ENGLISH);
    private Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
    private Handler mHandler = new Handler();
    private String st;
    private String et;
    private Calendar startTime;
    private Calendar endTime;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        view = inflater.inflate(R.layout.patientnotes, container, false);
        findViewsById();
        setViewsText();
        setListener();
        return view;
    }

    private void findViewsById() {
        startTimeView = view.findViewById(R.id.start_time_input);
        endTimeView = view.findViewById(R.id.end_time_input);
        commentView = view.findViewById(R.id.notes);
        buttonSend = view.findViewById(R.id.button_send);
    }

    private void setViewsText() {
        String st = SharedPreferencesUtility.getStartTime(getActivity());
        st = st == null ? showTimeFormat.format(calendar.getTime()) : st;
        String et = SharedPreferencesUtility.getEndTime(getActivity());
        et =  et == null ? showTimeFormat.format(calendar.getTime()) : et;
        String ct = SharedPreferencesUtility.getNotes(getActivity());
        ct = ct == null ? "" : ct;

        startTimeView.setText(st);
        endTimeView.setText(et);
        commentView.setText(ct);
    }

    private void setListener() {
        startTimeView.setOnClickListener(startTimeListener);
        endTimeView.setOnClickListener(endTimeListener);
        buttonSend.setOnClickListener(sendListener);
    }

    /**
     * The listener for send button
     */
    private View.OnClickListener sendListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            comments = commentView.getText().toString().replaceAll("\n", " ").trim();
            // If note is empty, return
            if (comments.length() == 0) {
                Toast.makeText(getContext(), "Note is empty. Please make sure fill note in.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Global.isWifiOrCellularConnected(getContext())) {
                Toast.makeText(getContext(), "Network is unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Send Note");
            builder.setMessage("Are you ready to send the note to the clinic?");
            builder.setPositiveButton("Yes", (dialogInterface, i) -> {
                String commentsBody = sdf.format(startTime.getTime()) + " " + sdf.format(endTime.getTime()) + " " + comments;
                sendNotesCallback(getActivity(), commentsBody);
            });
            builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
            });
            builder.show();
        }
    };

    /**
     * The listener for start time text editor
     */
    private View.OnClickListener startTimeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startTimeView.setEnabled(false);
            endTimeView.setEnabled(false);
            View pickers = getLayoutInflater().inflate(R.layout.date_time_picker, null);
            DatePicker datePicker = pickers.findViewById(R.id.note_datePicker);
            TimePicker timePicker = pickers.findViewById(R.id.note_timePicker);
            dialog = new AlertDialog.Builder(getContext());
            dialog.setTitle("Start Time");
            dialog.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startTime = Calendar.getInstance(Locale.ENGLISH);;
                    startTime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getHour(), timePicker.getMinute());
                    st = showTimeFormat.format(startTime.getTime());
                    startTimeView.setText(st);
                    startTimeView.setEnabled(true);
                    endTimeView.setEnabled(true);
                }
            });
            dialog.setNegativeButton("cancel", (dialog, which) -> {
                dialog.dismiss();
                startTimeView.setEnabled(true);
                endTimeView.setEnabled(true);
            });

            dialog.setView(pickers);
            dialog.show();
        }
    };

    /**
     * The listener for end time text editor
     */
    private View.OnClickListener endTimeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startTimeView.setEnabled(false);
            endTimeView.setEnabled(false);
            View pickers = getLayoutInflater().inflate(R.layout.date_time_picker, null);
            DatePicker datePicker = pickers.findViewById(R.id.note_datePicker);
            TimePicker timePicker = pickers.findViewById(R.id.note_timePicker);
            dialog = new AlertDialog.Builder(getContext());
            dialog.setTitle("End Time");
            dialog.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    endTime = Calendar.getInstance(Locale.ENGLISH);
                    endTime.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getHour(), timePicker.getMinute());
                    et = showTimeFormat.format(endTime.getTime());
                    endTimeView.setText(et);
                    startTimeView.setEnabled(true);
                    endTimeView.setEnabled(true);
                }
            });
            dialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startTimeView.setEnabled(true);
                    endTimeView.setEnabled(true);
                }
            });

            dialog.setView(pickers);
            dialog.show();
        }
    };

    /**
     * Define the callback functions of sending patient notes
     *
     * @param mActivity : the context using
     * @param body  : the whole content string send to server
     */
    public void sendNotesCallback(final Activity mActivity, String body) {
        SendToServer.sendPatientNotes(body, new SendToServer.FuncInterface() {
            @Override
            public void callbackAfterSuccess(Object obj) {
                mHandler.post(() -> Toast.makeText(getContext(), "Notes sent successfully.", Toast.LENGTH_LONG).show());
                SharedPreferencesUtility.emptyReference(getActivity());
                setViewsText();
            }

            @Override
            public void callbackAfterFail(Object obj) {
                mHandler.post(() -> Toast.makeText(getContext(), "Send notes failed, please try again.", Toast.LENGTH_LONG).show());
            }

            @Override
            public void handleException(Exception e) {
                mHandler.post(() -> Toast.makeText(mActivity, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause()");
        SharedPreferencesUtility.setPreference(getActivity(), st, et, commentView.getText().toString().trim());
        super.onPause();
    }
}

package ca.uvic.ece.ecg.heartcarer1;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
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
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This Fragment allows user to change input notes
 */
public class PatientNotesFragment extends Fragment {
    private static final String TAG = "PatientNotesFragment";
    private View view;
    private TextView commentView;
    private TextView startTimeView;
    private TextView endTimeView;
    private Date startTime;
    private Date endTime;
    private String comments;
    private Button buttonSend;
    private AlertDialog.Builder dialog;
    private final SimpleDateFormat showTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm aaa", Locale.ENGLISH);
    private Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
    private final TimeZone timeZone = calendar.getTimeZone();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");

        view = inflater.inflate(R.layout.patientnotes, container, false);
        findViewsById();
        setListener();
        comments = commentView.getText().toString().trim();
        return view;
    }

    private void findViewsById() {
        startTimeView = view.findViewById(R.id.start_time_input);
        startTimeView.setText(showTimeFormat.format(calendar.getTime()));
        endTimeView = view.findViewById(R.id.end_time_input);
        endTimeView.setText(showTimeFormat.format(calendar.getTime()));
        commentView = view.findViewById(R.id.notes);
        buttonSend = view.findViewById(R.id.button_send);
    }

    private void setListener() {
        startTimeView.setOnClickListener(startTimeListener);
        endTimeView.setOnClickListener(endTimeListener);
        buttonSend.setOnClickListener(sendListener);
    }

    private View.OnClickListener sendListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String st = startTimeView.getText().toString();
            String et = endTimeView.getText().toString();
            comments = commentView.getText().toString().replaceAll("\n", " ").trim();
            // If note is empty, return
            if (comments == null || comments.length() == 0) {
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
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //SendToServer.sendPatientNotes(st + " " + timeZone.getDisplayName(false, 0, Locale.ENGLISH) + " " + et + " " + timeZone.getDisplayName(false, 0, Locale.ENGLISH) + " " + comments);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder.show();
        }
    };

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
                    startTime = new Date(datePicker.getYear() - 1900, datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getHour(), timePicker.getMinute());
                    startTimeView.setText(showTimeFormat.format(startTime));
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
                    endTime = new Date(datePicker.getYear() - 1900, datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getHour(), timePicker.getMinute());
                    endTimeView.setText(showTimeFormat.format(endTime));
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

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }
}

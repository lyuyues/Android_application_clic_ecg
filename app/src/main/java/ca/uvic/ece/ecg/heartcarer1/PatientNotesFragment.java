package ca.uvic.ece.ecg.heartcarer1;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * This Fragment allows user to change input notes
 */
public class PatientNotesFragment extends Fragment {
    private static final String TAG = "PatientNotesFragment";
    private View view;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.i(TAG, "onCreateView()");

        view = inflater.inflate(R.layout.patientnotes, container, false);
        findViewsById();
        setListener();

        return view;
    }

    private void findViewsById() {
    }

    private void setListener() {
    }
}
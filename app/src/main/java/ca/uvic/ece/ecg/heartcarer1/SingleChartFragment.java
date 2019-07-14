package ca.uvic.ece.ecg.heartcarer1;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import ca.uvic.ece.ecg.ECG.DataFilter;

/**
 * Single Chart Fragment
 * @author yizhou
 *
 */
public class SingleChartFragment extends Fragment {
    private View view;
    private ECGChart ecgChart;

    private DataFilter dataFilter = new DataFilter();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.single_chart_fragment, container, false);

        ecgChart = new ECGChart(getActivity(), Global.getYAxisMin(), Global.getYAxisMax(), getResources().getString(R.string.ecgsignal));
        ecgChart.addToLayout((LinearLayout) view.findViewById(R.id.single_chart),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        return view;
    }

    public void handleHrmFragmentMes(Message msg) {
        switch (msg.what) {
        case BleService.STATE_MULTI_VAL:
            int[] multiValue = msg.getData().getIntArray("data");
            int length = multiValue.length / 2;

            for (int i = 0; i < length; i++)
                ecgChart.appendPointWithoutPaint(dataFilter.dataConvert(multiValue[i * 2 + Global.Channel_selection]) / 2);
            ecgChart.repaint();
            break;
        case BleService.STATE_DISCONNECTED:
            ecgChart.clearPlot();
            break;
        default:
            break;
        }
    }
}

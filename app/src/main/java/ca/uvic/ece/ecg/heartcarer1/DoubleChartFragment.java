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
 * Double Chart Fragment
 * @author yizhou
 *
 */
public class DoubleChartFragment extends Fragment {
    private View view;

    private ECGChart ecgChart1;
    private ECGChart ecgChart2;

    private DataFilter dataFilter1 = new DataFilter();
    private DataFilter dataFilter2 = new DataFilter();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.two_chart_fragment, container, false);

        ecgChart1 = new ECGChart(getActivity(), Global.yAxis_Min_Channel1, Global.yAxis_Max_Channel1, getResources().getString(R.string.ecgsignal1));
        ecgChart1.addToLayout((LinearLayout) view.findViewById(R.id.chart1),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ecgChart2 = new ECGChart(getActivity(), Global.yAxis_Min_Channel2, Global.yAxis_Max_Channel2, getResources().getString(R.string.ecgsignal2));
        ecgChart2.addToLayout((LinearLayout) view.findViewById(R.id.chart2),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        return view;
    }

    public void handleHrmFragmentMes(Message msg) {
        switch (msg.what) {
        case BleService.STATE_MULTI_VAL:
            int[] multiValue = msg.getData().getIntArray("data");
            int length = multiValue.length / 2;

            for (int i = 0; i < length; i++) {
                ecgChart1.appendPointWithoutPaint(dataFilter1.dataConvert(multiValue[i * 2]) / 2);
                ecgChart2.appendPointWithoutPaint(dataFilter2.dataConvert(multiValue[i * 2 + 1]) / 2);
            }
            ecgChart1.repaint();
            ecgChart2.repaint();
            break;
        case BleService.STATE_DISCONNECTED:
            ecgChart1.clearPlot();
            ecgChart2.clearPlot();
            break;
        default:
            break;
        }
    }
}

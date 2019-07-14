package ca.uvic.ece.ecg.heartcarer1;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

/**
 * Chart used to plot the curves using achart engine
 * @author yizhou
 *
 */
public class ECGChart {
    private XYMultipleSeriesRenderer renderer;
    private XYSeries series;
    private GraphicalView view;

    private int xAxis = 0;

    public ECGChart(Activity activity, double yMin, double yMax, String seriesTitle) {
        initRenderer(yMin, yMax);

        series = new XYSeries(seriesTitle);

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(series);

        view = ChartFactory.getLineChartView(activity, dataset, renderer);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                @SuppressWarnings("unused")
                SeriesSelection seriesSelection = view.getCurrentSeriesAndPoint();
            }
        });
    }

    public void addToLayout(LinearLayout layout, LayoutParams layoutParams) {
        layout.addView(view, layoutParams);
        view.repaint();
    }

    private void initRenderer(double yMin, double yMax) {
        renderer = new XYMultipleSeriesRenderer();

        renderer.setAxisTitleTextSize(24);
        renderer.setChartTitleTextSize(24);
        renderer.setLabelsTextSize(24);
        renderer.setLegendTextSize(24);
        renderer.setMargins(new int[] {15, 45, 35, 10});//up,left,down,right
        renderer.setZoomButtonsVisible(true);
        renderer.setPointSize(1);
        renderer.setShowGrid(true);

        //Set Color
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(Global.color_Pink);
        renderer.setMarginsColor(Global.color_Pink);
        renderer.setGridColor(Global.color_Red);
        renderer.setAxesColor(Global.color_Red);
        renderer.setLabelsColor(Global.color_Black);
        renderer.setXLabelsColor(Global.color_Black);
        renderer.setYLabelsColor(0, Global.color_Black);

        //Set X,Y Axis Range
        renderer.setXTitle("Time index: (1 sec = 250)");
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(Global.xAxis_Max);
        renderer.setYTitle("Voltage: mV");
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        renderer.setYLabels(10);

        XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setPointStyle(PointStyle.CIRCLE);
        seriesRenderer.setFillPoints(true);
        seriesRenderer.setColor(Global.color_Black);
        renderer.addSeriesRenderer(seriesRenderer);
    }

    public void appendPoint(double y) {
        if (xAxis == Global.xAxis_Total_Max)
            clearPlot();

        series.add(xAxis++, y);
        view.repaint();

        if (xAxis > Global.xAxis_Max)
            restRangePlot();
    }

    public void appendPointWithoutPaint(double y) {
        if (xAxis == Global.xAxis_Total_Max)
            clearPlot();

        series.add(xAxis++, y);

        if (xAxis > Global.xAxis_Max)
            restRangePlot();
    }

    public void repaint() {
        view.repaint();
    }

    public void clearPlot() {
        series.clear();
        xAxis = 0;

        renderer.setXAxisMin(0);
        renderer.setXAxisMax(Global.xAxis_Max);
    }

    private void restRangePlot() {
        renderer.setXAxisMin(xAxis - Global.xAxis_Max);
        renderer.setXAxisMax(xAxis);
    }
}

package ca.uvic.ece.ecg.ECG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Filter Queue
 * @author yizhou
 *
 */
public class Filter_Queue {
    private static final int LIST_SIZE = 21;

    private final Queue<Double> Q = new LinkedList<Double>();
    private final List<Double> h = new ArrayList<Double>();

    public Filter_Queue() {
        // Convert 1~45 (out of 250) Hz to radian
        double omec1 = 2.0 / 250 * Math.PI;
        double omec2 = 90.0 / 250 * Math.PI;

        for (int i = 0; i < LIST_SIZE / 2; ++i) {
            h.add((Math.sin((i - LIST_SIZE / 2) * omec2) - Math.sin((i - LIST_SIZE / 2) * omec1)) / ((i - LIST_SIZE / 2) * Math.PI)
                    * (0.54 - 0.46 * Math.cos(Math.PI * i / (LIST_SIZE / 2))));
        }
        h.add((omec2 - omec1) / Math.PI);
        for (int i = LIST_SIZE / 2 - 1; i >= 0; --i) {
            h.add(h.get(i));
        }
    }

    public int get_size() {
        return Q.size();
    }

    public boolean isFull() {
        return get_size() >= LIST_SIZE;
    }

    public void push(double value) {
        if (isFull())
            return;

        Q.offer(value);
    }

    public double pop() {
        if (!isFull())
            return -1;

        Iterator<Double> it = Q.iterator();
        int i = 20;
        double sum = 0;
        while (it.hasNext())
            sum += it.next() * h.get(i--);
        Q.poll();
        return sum;
    }
}

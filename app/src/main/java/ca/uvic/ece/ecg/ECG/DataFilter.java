package ca.uvic.ece.ecg.ECG;

/**
 * Data Filter that filter the raw input data from BLE device
 * @author yizhou
 *
 */
public class DataFilter {
    private static final int MAX_INPUT = (1 << 16) - 1;
    private static final int DIV = (1 << 15) - 1;
    private static final double MAX_VOLTAGE = 4.84d;

    private Filter_Queue filter = new Filter_Queue();

    public double dataConvert(int data){
        double convertedData = convertInput(data);
        if (!filter.isFull()) {
            filter.push(convertedData);
            return 0;
        }
        double tmp = filter.pop();
        filter.push(convertedData);
        return tmp / 12 * 1000;
    }

    private double convertInput(int input) {
        if (input > DIV)
            input = -(((~input) & MAX_INPUT) + 1);
        return input * 1.0d / DIV * MAX_VOLTAGE;
    }
}

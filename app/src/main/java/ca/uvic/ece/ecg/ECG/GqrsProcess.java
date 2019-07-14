package ca.uvic.ece.ecg.ECG;

import java.io.*;
import java.lang.*;

import ca.uvic.ece.ecg.heartcarer1.Global;


/* this class converts the data from original to the format fit in format 212 used in gqrs
    (the format detail in  https://www.physionet.org/physiotools/wag/signal-5.htm)
    then generate header file and data file for gqrs using, call gqrs application in the end.
    @author yue
 */
public class GqrsProcess {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final int MAX_INPUT = (1 << 16) - 1;
    private static final int DIV = (1 << 15) - 1;
    private static final double MAX_VOLTAGE = 4.84d;

    private static final int FS = 250;
    private static final int LengthPerMin = 60 * 5 * FS;

    private static double convertInput(int input) {
        if (input > DIV)
            input = -(((~input) & MAX_INPUT) + 1);
        return input * 1.0d / DIV * MAX_VOLTAGE;
    }

    public static int gqrsProcess(byte[] input) throws Exception {
        int chanOne = 0;  // the two byte int sample in first channel, in original format
        int chanTwo = 0;  // the two byte int sample in second channel, in original format
        byte high = 0;    // most 4 significant digits of samples, 4 bits from channel 1 followed by 4 bits from channel 2

        int nsamp = 0;    // number of samples
        int sample1 = 0;
        int sample2 = 0;

        FileOutputStream ps = null;
        try {
            ps = new FileOutputStream(new File(Global.gqrsTempPath,"TempData.dat"));

            for (int i = 0; i < input.length; i++) {
                if (i % 5 == 0)
                    continue;

                if (i % 5 == 1 || i % 5 == 2) {
                    if (chanOne == 0) {
                        chanOne = (input[i] & 0xff);
                    } else {
                        chanOne = (chanOne << 8 | (input[i] & 0xff));
                        double input1 = convertInput(chanOne); // the first channel sample before filter (-4.84~4.84mv)

                        input1 = (input1 - 0.15) * 50 + 0.15;

                        int output1 = (int) ((input1 + 5.12) / 0.005); // filter to gqrs pattern (212)  11 bits

                        // abstract the most 4 significant digits of sample from channel 1
                        high = (byte) (output1 >> 8);
                        high = (byte) (high << 4);

                        // abstract the lease 8 significant digits of sample from channel 1
                        byte low = (byte) output1;
                        ps.write(low);

                        if(i < 5){
                            sample1 = output1;
                        }

                        chanOne = 0;
                    }
                } else {
                    if (chanTwo == 0) {
                        chanTwo = (input[i] & 0x0f);
                    } else {
                        chanTwo = (chanTwo << 8 | (input[i] & 0xff));
                        double input2 = convertInput(chanTwo); // the first channel sample before filter (-4.84~4.84mv)
                        input2 = (input2 - 0.15) * 10 + 0.15;

                        int output2 = (int) ((input2 + 5.12) / 0.005); // filter to gqrs pattern (212) 11 bits

                        // abstract the most 4 significant digits of sample from channel 2, follow the 4 bits from channel 1
                        byte a = (byte) (output2 >> 8) ;
                        high = (byte) (high | a);

                        ps.write(high);
                        nsamp++;

                        // // abstract the lease 8 significant digits of sample from channel 2
                        byte low = (byte) output2;
                        ps.write(low);

                        if(i < 5){
                            sample2 = output2;
                        }

                        chanTwo = 0;
                    }
                }
            }

        } catch (FileNotFoundException ignore) {
        } finally {
            if (null != ps) {
                ps.flush();
                ps.close();
            }
        }

        updateHeader(nsamp, sample1, sample2);

        //return calBPM();
        return (int)Math.ceil(calBPM() * 1.0 / input.length * LengthPerMin);
    }

    private static void updateHeader(int nsamp, int sample1, int sample2){
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(new File(Global.gqrsTempPath, "TempData.hea")));

            //line1: record name 2 250 nsamp
            ps.print("TempData 2 250 ");
            ps.append(Integer.toString(nsamp));
            ps.append("\n");

            // line2: record name.dat 212 200 0 0 first sample 0 0 col 0
            ps.append("TempData.dat 212 200 0 0 ");
            ps.append(Integer.toString(sample1));
            ps.append(" 0 0 col 0\n");

            // line3: record name.dat 212 200 0 0 first sample 0 0 col 1
            ps.append("TempData.dat 212 200 0 0 ");
            ps.append(Integer.toString(sample2));
            ps.append(" 0 0 col 1\n");

        } catch (FileNotFoundException ignore) {
        } finally {
            if (ps != null) {
                ps.flush();
                ps.close();
            }
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native int calBPM();
}

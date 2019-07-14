package ca.uvic.ece.ecg.ECG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HR_detect {
	private List<Double> ecg_data = new ArrayList<Double>();

	private List<Integer> R_i = new ArrayList<Integer>();
	private List<Integer> S_i = new ArrayList<Integer>();
	private List<Integer> T_i = new ArrayList<Integer>();
	private List<Integer> Q_i = new ArrayList<Integer>();
	private List<Double> buffer_plot = new ArrayList<Double>();
	private double HR = 0;
	
	private int fs = 250;
	public void reset() {
		ecg_data.clear();
		R_i.clear();
		S_i.clear();
		T_i.clear();
		Q_i.clear();
		buffer_plot.clear();
	}
	//private String path = null;
	
	public double getHR() {
		return HR;
	}
	
	private void preprocess_ECG_Data() {
		double sum = 0.0;
		for (int i = 0; i < ecg_data.size(); ++i){
			sum += (ecg_data.get(i)/ecg_data.size());
		}
		
		for (int i = 0; i < ecg_data.size(); i++){
			ecg_data.set(i, (ecg_data.get(i)-sum));
		}
	}
	
	private boolean Detect() {
		double time_scale = ecg_data.size() / fs;
		int window = 1;
		int state = 0;
		double weight = 1.8;
		int dum = 0;
		int co = 0;
		int S_on = 0;
		int c = 0;
		int T_on = 0;
		int T_on1 = 0;
		int sleep = 0;

		double buffer_T;
		double buffer_mean;
		List<Double> buffer_base = new ArrayList<Double>();
		double buffer_long;
		double mean_online;
		double current_max = 0;
		int ind = 0;
		double thres2;
		
		List<Double> R_amp = new ArrayList<Double>();
		List<Double> S_amp = new ArrayList<Double>();
		List<Double> T_amp = new ArrayList<Double>();
		List<Double> Q_amp = new ArrayList<Double>();
		List<Double> thres2_p = new ArrayList<Double>();
		List<Integer> thres2_p_i = new ArrayList<Integer>();
		// ------------------------------------------------------------//
		int idx;
		int jdx;
		int count;
		buffer_T = 0;
		if (ecg_data.size() < 2*fs){
			System.out.println("data length is too short.");
			return false;
		}
		for (idx = 0, count = 0; idx < 2 * fs; ++idx, ++count) {
			buffer_T = buffer_T * count / (count + 1) + ecg_data.get(idx) / (count + 1);
		}
		buffer_mean = 0;
		for (idx = 0, count = 0; idx < 2 * fs; ++idx, ++count) {
			buffer_mean = buffer_mean * count / (count + 1) + Math.abs(ecg_data.get(idx) - buffer_T) / (count + 1);
		}

		buffer_long = 0;
		
		// start online inference (Assuming the signal is being acquired online)
		for (idx = 0, count = 1; idx < ecg_data.size(); ++idx, ++count) {

			buffer_long += ecg_data.get(idx);

			// Renew the mean and adapt it to the signal after 1 second of processing
			if (count == 2 * fs) {
				count = 0;
				buffer_T = 0;
				for (jdx = 0; jdx < 2 * fs; ++jdx) {
					buffer_T = buffer_T * jdx / (jdx + 1) + ecg_data.get(idx - jdx) / (jdx + 1);
				}
				buffer_mean = 0;
				for (jdx = 0; jdx < 2 * fs; ++jdx) {
					buffer_mean = buffer_mean * jdx / (jdx + 1) + Math.abs(ecg_data.get(idx - jdx) - buffer_T) / (jdx + 1);
				}
			}

			// smooth the signal by taking the average of 15 samples and add the new upcoming samples
			if (idx >= window - 1) {
				mean_online = buffer_long / window;
				buffer_plot.add(mean_online);
				
				// Enter state 1 (putative R wave) as soon as that the mean exceeds the double time of threshold
				if (state == 0) {
					if (buffer_plot.size() >= 3) {
						if ((mean_online > buffer_mean * weight) && (buffer_plot.get(idx - 1 - window) > buffer_plot.get(idx - window))) {
							state = 1;
							current_max = buffer_plot.get(idx - 1 - window);
							ind = idx - 1 - window;

						}
						else {
							state = 0;
						}
					}
				}
				
				// Locate the R wave location by finding the highest local maximum
				if (state == 1) {
					if (current_max > buffer_plot.get(idx - window)) {
						dum++;
						if (dum > 4) {
							R_i.add(ind);
							R_amp.add(buffer_plot.get(ind));

							// Locate Q wave
							if (ind > (int)(0.04 * fs + 0.5)) {
								int Q_ti = ind;
								double Q_tamp = buffer_plot.get(ind);
								for (jdx = 1; jdx <= (int)(0.04 * fs + 0.5); ++jdx) {
									if (Q_tamp > buffer_plot.get(ind - jdx)) {
										Q_tamp = buffer_plot.get(ind - jdx);
										Q_ti = ind - jdx;
									}
								}
								Q_i.add(Q_ti);
								Q_amp.add(Q_tamp);
							}

							if (R_i.size() > 8) {
								weight = 0;
								for (jdx = 0; jdx < 8; ++jdx) {
									weight += 0.3 * R_amp.get(R_i.size() - jdx - 1);
								}
								weight = weight / buffer_mean / 8;
							}

							state = 2;
							dum = 0;
						}
					}
					else {
						dum = 0;
						state = 0;
					}
				}

				// Check whether the signal drops below the threshold to look for S wave
				if (state == 2) {
					if (mean_online <= buffer_mean) {
						state = 3;
					}
				}

				// Enter S wave detection state 3 (S detection)
				if (state == 3) {
					co++;
					if (co < (int)(0.2 * fs + 0.5)) {
						if (buffer_plot.get(idx - window - 1) <= buffer_plot.get(idx - window)) {
							S_on++;
							if (S_on >= (int)(0.012 * fs + 0.5)) {
								S_i.add(idx - window - 4);
								S_amp.add(buffer_plot.get(idx - window - 4));
								state = 4;
								S_on = 0;
								co = 0;
							}
						}
					}
					else {
						state = 4;
						co = 0;
					}
				}

				// enter state 4 possible T wave detection
				if (state == 4) {
					if (mean_online < buffer_mean) {
						state = 6;
					}
				}

				// Enter State 6 which is T wave possible detection
				if (state == 6) {
					c++;
					if (c <= (int)(0.7 * fs + 0.5)) {
						thres2 = Math.abs(Math.abs(buffer_T) - Math.abs(S_amp.get(S_i.size() - 1))) * 0.75 + S_amp.get(S_i.size() - 1);
						thres2_p.add(thres2);
						thres2_p_i.add(ind);
						if (mean_online > thres2) {
							T_on++;
							if (T_on >= (int)(0.012 * fs + 0.5)) {
								if (buffer_plot.get(idx - window - 1) >= buffer_plot.get(idx - window)) {
									T_on1++;
									if (T_on1 > (int)(0.032 * fs + 0.5)) {
										T_i.add(idx - window - 11);
										T_amp.add(buffer_plot.get(idx - window - 11));
										state = 5;
										T_on = 0;
										T_on1 = 0;
									}
								}
							}
						}
					}
					else {
						state = 5;
					}
				}

				// This state is for avoiding the detection of a highly variate noise or another peak
				// this avoids detection of two peaks R waves less than half a second
				if (state == 5) {
					sleep = sleep + c + 1;
					c = 0;
					if (sleep * 1.0 / fs >= 0.4) {
						state = 0;
						sleep = 0;
					}
				}

				buffer_long -= ecg_data.get(idx - window + 1);
			}
		}
		
		//calculate HR
		HR = Math.ceil((R_i.size() / time_scale) * 60);
		
		return true;
	}
	
	public boolean begin(List <Double> pass_data) {
		
		ecg_data = pass_data;
		preprocess_ECG_Data();
		boolean result = Detect();
		return result;
	}
}
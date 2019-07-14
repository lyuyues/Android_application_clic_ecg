package ca.uvic.ece.ecg.ECG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AveCC {
//Sec01: declare variables and methods
	private List<Double> ecg_data = new ArrayList<Double>();
	
	private double avecc = 0.0;

	private List<Integer> R_i = new ArrayList<Integer>();
	private List<Integer> S_i = new ArrayList<Integer>();
	private List<Integer> T_i = new ArrayList<Integer>();
	private List<Integer> Q_i = new ArrayList<Integer>();
	private List<Double> buffer_plot = new ArrayList<Double>();
	private List<Integer> MAresult = new ArrayList<Integer>();
	boolean result;
	private double HR = 0;
	private String path = null;
	//processed ECG data
	//private List<Double> new_ecg = new ArrayList<Double>();

	private int fs = 250; //or 1000
	
	public boolean getResult (){
		return result;
	}
	

	//Sec03: filter the read data
	private void ecg_filter(List<Double> ecg_data) {
		double f_para[] = { 1.0/fs, 90.0/fs }; 
		int N = 21;
		double tmp;
		double omec1 = f_para[0] * Math.PI;
		double omec2 = f_para[1] * Math.PI;
		List<Double> h = new ArrayList<Double>();

		for (int i = 0; i < N / 2; ++i) {
			h.add(((Math.sin((i - N / 2) * omec2) - Math.sin((i - N / 2)
					* omec1)) / ((i - N / 2) * Math.PI))
					* (0.54 - 0.46 * Math.cos(Math.PI * i / (N / 2))));
		}
		h.add((omec2 - omec1) / Math.PI);
		for (int i = N / 2 - 1; i >= 0; --i) {
			h.add(h.get(i));
		}
		for (int i = ecg_data.size() - 1; i >= N - 1; --i) {
			tmp = 0;
			for (int j = 0; j < N; ++j) {
				tmp += ecg_data.get(i - j) * h.get(j);
			}
			ecg_data.set(i, tmp);
		}
		double ecg_mean = 0;
	    for (int i = 0; i < ecg_data.size(); ++i) {
	        ecg_mean += (ecg_data.get(i)/ecg_data.size());
	    }
		for (int i = 0; i < ecg_data.size(); ++i) {
			ecg_data.set(i, ecg_data.get(i) - ecg_mean);
		}
		
	}
	
//Sec04: R-peak detection
	private boolean Detect(List<Double> ecg_data) {
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
		ecg_filter(ecg_data);
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
		return true;
	}//Detect	
	

	//Sec05: AveCC extraction
	public boolean AveCC_extract(List<Double> template){
	double delta = 168;//ms of the time window
	int win = 0;
	int win_1 = 0;
	int win_2 = 0;
	int len = 0;
	int val = 0;
	double v1=0;
	double v2=0;
	double v3=0;
	double y1=0;
	
	double devcc=0;
	
	
	List<Integer> qrs = new ArrayList<Integer>();
	List<Integer> start_p = new ArrayList<Integer>();
	List<Integer> stop_p = new ArrayList<Integer>();
	List<Integer> idx_cc = new ArrayList<Integer>();
	List<Double> tpl = new ArrayList<Double>(template);
	List<Double> x1 = new ArrayList<Double>();
	List<Double> y = new ArrayList<Double>();
//	List<Double> data1 = new ArrayList<Double>();
	
	qrs = R_i;
	win = (int) Math.rint(delta * fs / 1000);
	win_1 = (int) Math.rint(win / 3);
	win_2 = win - win_1;
	
	for(int i=0;i<qrs.size();i++){
//		start_p += ecg_data.get(i);
	start_p.add (qrs.get(i) - win_1);
	stop_p.add(qrs.get(i) + win_2);
	}
	
	//RRrange = delta *[30/60 220/60] / 1000; %important
	len = stop_p.size()-1;
	val = stop_p.get(len);
	
	if(val > ecg_data.size()){
		for(int i=0; i < qrs.size()-1;i++){
			idx_cc.add(i);
		}
	}

	if(start_p.get(0) <= 0){
		for(int i=1; i < qrs.size();i++){
			idx_cc.add(i);
		}
	}
	
	if((val <= ecg_data.size()) || (start_p.get(0)) > 0){
		for(int i=0; i < qrs.size();i++){
			idx_cc.add(i);
		}
	}
	
	
	if((start_p != null) && (start_p.size() > 0)){
	  for(int j=0;j< idx_cc.size();j++){
		  for(int i=start_p.get(j);i<stop_p.get(j)+1;i++){
			  v1 += ecg_data.get(i) * ecg_data.get(i) ;
			  v2 += tpl.get(i-start_p.get(j))*tpl.get(i-start_p.get(j));
			  v3 += ecg_data.get(i) * tpl.get(i-start_p.get(j));
			  x1.add(ecg_data.get(i));
		  }
		  y1 += v3 / Math.sqrt(v1 * v2); // cross-correlation between the template and current QRS complex.
		  y.add(y1);
	  }
       avecc = y1 / y.size();
       
       if (avecc >= 0.75){
    	   result =  false;
       }else {
    	   result = true;
       }
       
       for(int i=0; i < idx_cc.size();i++){
    	   devcc += (y.get(i)-avecc)*(y.get(i)-avecc);
	   }
       devcc = Math.sqrt(devcc/y.size());
	}
	
	return result;
	}// end of AveCC_extract


	//Sec06: run function
	public void run(List<Double> data, List<Double> ecg) {
		ecg_data = ecg;
		Detect(ecg_data);
		AveCC_extract(data);
		System.out.println(avecc);
	
	}

}

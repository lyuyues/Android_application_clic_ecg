package ca.uvic.ece.ecg.ECG;





import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ECGDetect {
	
	private List<Double> ecg_data = new ArrayList<Double>();

	private List<Integer> R_i = new ArrayList<Integer>();
	private List<Integer> S_i = new ArrayList<Integer>();
	private List<Integer> T_i = new ArrayList<Integer>();
	private List<Integer> T_i2 = new ArrayList<Integer>();
	private List<Integer> Q_i = new ArrayList<Integer>();
	private List<Integer> S_end = new ArrayList<Integer>();
	private List<Integer> T_end = new ArrayList<Integer>();

	private List<Double> buffer_plot = new ArrayList<Double>();
	private double HR = 0;
	private double QRS_duration = 0;
	private double QT_interval = 0;
	private double PR_interval = 0;
	private double QTc = 0;
	
	//ST parameters
	double ST_max = -999;
	double ST_min = 99999;
	double ST_avg = 0.0;
	
	private int fs = 250;
	public void setFS(int fs) {
		this.fs = fs;
	}
	
	public List<Integer> getRi() {
		return R_i;
	}
	
	public List<Integer> getTi() {
		return T_i;
	}
	
	public List<Integer> getSi() {
		return S_i;
	}
	
	public List<Integer> getQi() {
		return Q_i;
	}
	
	public List<Integer> getS_end() {
		return S_end;
	}
	
	public List<Integer> getT_end() {
		return T_end;
	}
	
	public double getHR() {
		return HR;
	}
	
	public double getQT_interval() {
		return QT_interval;
	}
	
	public double getQRS_duration() {
		return QRS_duration;
	}
	
	public double getPR_interval() {
		return PR_interval;
	}
	
	public double getQTc() {
		return QTc;
	}
	
	public double getSTmax(){
		return ST_max;
	}
	public double getSTmin(){
		return ST_min;
	}
	public double getSTavg(){
		return ST_avg;
	}
	private void read_ECG_Data(List<Double> new_data, List<Double> ecg_data) {
		//ecg_data = new ArrayList<Double>(new_data);
		for(int i = 0; i < new_data.size(); ++i){
			ecg_data.add(new_data.get(i));
		}
	}


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
		
	}
	
	private boolean Detect() {
		// ----------------------------------------------------------------------------//
		double time_scale = ecg_data.size() / fs;
		//int window = fs / 50;
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
		double slope_q = 0;
	
		double buffer_T;
		double buffer_mean;
		List<Double> buffer_base = new ArrayList<Double>();
		double buffer_long;
		double mean_online;
		double current_max = 0;
		int ind = 0;
		double thres2;
		
		int dist_ST;
		int I = 0;
		double min_slope;
		int qi_index = 0;
		double slope;
		int I0;
		double slope_p;
		int po_index = 0;
		
	
		List<Double> R_amp = new ArrayList<Double>();
		List<Double> S_amp = new ArrayList<Double>();
		List<Double> T_amp = new ArrayList<Double>();
		List<Double> Q_amp = new ArrayList<Double>();
		List<Double> S_end_amp = new ArrayList<Double>();
		List<Double> T_end_amp = new ArrayList<Double>();
		List<Integer> P_peak = new ArrayList<Integer>();
		List<Double> P_peak_amp = new ArrayList<Double>();
		List<Double> thres2_p = new ArrayList<Double>();
		List<Integer> thres2_p_i = new ArrayList<Integer>();
		List<Integer> ST_test = new ArrayList<Integer>();
		List<Double> grad_one = new ArrayList<Double>();
		List<Integer> Q_onset = new ArrayList<Integer>();
		List<Double> Q_onset_amp = new ArrayList<Double>();
		List<Integer> indexh = new ArrayList<Integer>();
		List<Integer> index_order = new ArrayList<Integer>();
		List<Double> height = new ArrayList<Double>();
		List<Integer> dist = new ArrayList<Integer>();
		List<Double> max_slope = new ArrayList<Double>();
		List<Integer> index_max_slope = new ArrayList<Integer>();
		List<Double> height_diff = new ArrayList<Double>();
		List<Integer> dist_x = new ArrayList<Integer>();
		List<Integer> P_onset = new ArrayList<Integer>();
		List<Double> P_onset_amp = new ArrayList<Double>();
		List<Integer> PR_mat = new ArrayList<Integer>();
		List<Integer> PR_inter = new ArrayList<Integer>();
		List<Integer> QT_mat = new ArrayList<Integer>();
		List<Integer> QRS_mat = new ArrayList<Integer>();
		List<Integer> QT_inter = new ArrayList<Integer>();
		List<Integer> QRS_inter = new ArrayList<Integer>();
		//ST elevation or degression 
		List<Double> ST_base = new ArrayList<Double>();
		List<Double> ST_move = new ArrayList<Double>();
	
		int idx;
		int jdx;
		int count;
		
		// ----------------------------------------------------------------------------//
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
		//using buffer_plot to replace ecg_data
		for (int i = 0; i < buffer_plot.size(); ++i){
			ecg_data.set(i, buffer_plot.get(i));
		}
		
		//------------------------------------------------------------------------------------------//
		// Self-design Error control part 1
		int uni_size = R_i.size()-3;
		if (uni_size <= 0){
			System.out.println("data is abnormal.");
			return false;
		}
		int qi = 0;
		while (Q_i.get(qi) < R_i.get(0)){
			++qi;
		}
		for (int i = 0; i < uni_size; ++i){
			Q_i.set(i, Q_i.get(qi+i));
		}
		//T_i detection may crash
		if (T_i.size() < uni_size){
			System.out.println("T wave detection may be false");
		}
		int ti = 0;
		while (T_i.get(ti) < R_i.get(1)){
			++ti;
		}
		int ind_tmp = ti;
		for (int i = 0; i < uni_size; ++i){
			if (ind_tmp < T_i.size()){
				if (T_i.get(ind_tmp) <R_i.get(2+i)){
					T_i2.add(T_i.get(ind_tmp));
					ind_tmp++;
				}else{
					T_i2.add(R_i.get(1+i)+60);
				}
			}else {
				T_i2.add(R_i.get(2+i)-60);
			}
		}
		
		int si = 0;
		while (S_i.get(si) < R_i.get(1)){
			++si;
		}
		for (int i = 0; i < uni_size; ++i){
			S_i.set(i, S_i.get(si+i));
		}
		
		// detect the end of s wave
		for (idx = 0; idx < uni_size; ++idx) {
			dist_ST = (int)Math.ceil((T_i2.get(idx) - S_i.get(idx)) / 2.5);
			ST_test.add(dist_ST + S_i.get(idx));
			// compute each gradient during ST_test
			for (jdx = 3; jdx <= dist_ST - 2; ++jdx) {				
				if (jdx - 3 >= grad_one.size()) {
					grad_one.add(Math.abs(-2 * ecg_data.get(S_i.get(idx) + jdx - 2) - ecg_data.get(S_i.get(idx) + jdx - 1)
							+ ecg_data.get(S_i.get(idx) + jdx + 1) + 2 * ecg_data.get(S_i.get(idx) + jdx + 2)));
				}				
			}
			if (grad_one.size() > 0) {
				I = 0;
				for (jdx = 1; jdx < grad_one.size(); ++jdx) {
					if (grad_one.get(jdx) < grad_one.get(I)) {
						I = jdx;
					}
				}
			}			
			S_end.add(S_i.get(idx)+I+2);
			//S_end_amp.add(ecg_data.get(S_end.get(S_end.size() - 1)));			
		}
		
		// Q onset detection --window 40ms
		int winQ = 15;
		if (fs == 1000){
			winQ = 30;
		}else if (fs == 250){
			winQ = 15;
		}
		for (idx = 0; idx < uni_size; ++idx) {
			min_slope = 9999.99;
			slope_q = 999999.99;
			for (jdx = 3; jdx <= winQ - 2; ++jdx) {
				if (Q_i.get(idx) - jdx - 2 >= 0) {
					slope_q = Math.abs(-2 * ecg_data.get(Q_i.get(idx) - jdx - 2) - ecg_data.get(Q_i.get(idx) - jdx - 1)
						+ ecg_data.get(Q_i.get(idx) - jdx + 1) + 2 * ecg_data.get(Q_i.get(idx) - jdx + 2));
				}
				if (slope_q < min_slope) {
					min_slope = slope_q;
					qi_index = jdx;
				}
			}
			Q_onset.add(Q_i.get(idx) - qi_index);
			Q_onset_amp.add(ecg_data.get(Q_onset.get(idx)));
		}
		
		// preparation work
		indexh.add(thres2_p_i.get(0));
		index_order.add(1);
		
		for (idx = 0; idx < thres2_p_i.size() - 1; ++idx) {
			
			if (!thres2_p_i.get(idx).equals(thres2_p_i.get(idx + 1))) {
				indexh.add(thres2_p_i.get(idx + 1));
				index_order.add(idx + 1);
			}
		}
	
		for (idx = 0; idx < indexh.size(); ++idx) {
			height.add(thres2_p.get(index_order.get(idx)));
		}
		//new error-avoiding code
		if (height.size() > ti+uni_size-1){
			for (int i = 0; i < uni_size; ++i){
				height.set(i, height.get(si+i));
			}
		}
			
		int dist_tmp;
		// T wave end detection
		for (idx = 0; idx < uni_size; ++idx) {
			dist_tmp = (int)Math.ceil(0.65 * (T_i2.get(idx) - S_i.get(idx)));
			if (dist_tmp > 100){
				dist.add(100);
			}else{
				dist.add((int)Math.ceil(0.65 * (T_i2.get(idx) - S_i.get(idx))));
			}
			max_slope.clear();
			for (jdx = 0; jdx < T_i2.size(); ++jdx) {
				max_slope.add(0.0);
			}
			
			index_max_slope.add(0);
			for (jdx = 0; jdx < dist.get(idx); ++jdx) {
				if (T_i2.get(idx) + jdx + 1 < ecg_data.size()){
					slope = Math.abs(ecg_data.get(T_i2.get(idx) + jdx) - ecg_data.get(T_i2.get(idx) + jdx + 1));
					if (slope > max_slope.get(idx)) {
						max_slope.set(idx, slope);
						index_max_slope.set(idx, jdx);
					}
				}
				
			}
			height_diff.add(ecg_data.get(T_i2.get(idx) + index_max_slope.get(idx)) - height.get(idx));
			dist_x.add((int)Math.ceil(height_diff.get(idx) / max_slope.get(idx)));
			
				T_end.add(T_i2.get(idx) + index_max_slope.get(idx) + dist_x.get(idx));
				//T_end_amp.add(ecg_data.get(T_end.get(T_end.size() - 1)));
			
		}
		
		// P peak detection
		int winP = 240/(1000/fs);
		for (idx = 0; idx < uni_size; ++idx) {		
				I0 = Q_i.get(idx);
				for (jdx = 0; jdx < winP; ++jdx) {
					if (ecg_data.get(Q_i.get(idx) - jdx - 1) > ecg_data.get(I0)) {
						I0 = Q_i.get(idx) - jdx - 1;
					}
				}
			
			P_peak.add(I0);
			//P_peak_amp.add(ecg_data.get(I0));
		}
		
		// P onset detection based on time range threshold
		int winPOn = 80/(1000/fs);
		for (idx = 0; idx < uni_size; ++idx) {
			min_slope = 9999;
			
			for (jdx = 1; jdx <= (winPOn/2); ++jdx) {
				slope_p = Math.abs(ecg_data.get(P_peak.get(idx) - winPOn + jdx) - ecg_data.get(P_peak.get(idx) - winPOn + jdx + 1));
				if (slope_p < min_slope) {
					min_slope = slope_p;
					po_index = jdx;
				}
			}
			P_onset.add(P_peak.get(idx) - winPOn + po_index);
			P_onset_amp.add(ecg_data.get(P_onset.get(idx)));
			
		}
		//-----------------------------------------------------------------------------------------//
		//Error control on parameters
		
		 int count_PR = 0;
		 int count_QT = 0;
		 int count_QRS = 0;
		 
			for (idx = 0; idx < uni_size; ++idx) {
				PR_mat.add(Q_onset.get(idx) - P_onset.get(idx));
			}
			for (idx = 0; idx < uni_size; ++idx) {
				QT_mat.add(T_end.get(idx) - Q_onset.get(idx));
			}
			for (idx = 0; idx < uni_size; ++idx) {
				QRS_mat.add(S_end.get(idx) - Q_onset.get(idx));
			}
			int elem = 1000/fs;
			
			for (idx = 0; idx < uni_size; ++idx) {
				if ((20/elem) < PR_mat.get(idx) && PR_mat.get(idx) < (250/elem)) {
					PR_inter.add(PR_mat.get(idx));
					++count_PR;
				}
				if ((QT_mat.get(idx) > 0) && QT_mat.get(idx) < (500/elem)) {
					QT_inter.add(QT_mat.get(idx));
					++count_QT;
				}
				if ((30/elem) < QRS_mat.get(idx) && QRS_mat.get(idx) < (250/elem)) {
					QRS_inter.add(QRS_mat.get(idx));
					++count_QRS;
				}
			}
		
		//-----------------------------------------------------------------------------------------//
		// compute heart rate
		HR = R_i.size() / time_scale * 60;
		// compute PR interval
		PR_interval = 0.0;
		if (count_PR > 0) {
			for (idx = 0; idx < PR_inter.size(); ++idx) {
				PR_interval = PR_interval * idx / (idx + 1) + PR_inter.get(idx) * 1.0 / (idx + 1);
			}
			PR_interval *= elem;
		}
		// compute QRS duration
		QRS_duration = 0.0;
		if (count_QRS > 0) {
			for (idx = 0; idx < QRS_inter.size(); ++idx) {
				QRS_duration = QRS_duration * idx / (idx + 1) + QRS_inter.get(idx) * 1.0 / (idx + 1);
			}
			QRS_duration *= elem;
		}
		// compute QT interval
		QT_interval = 0.0;
		if (count_QT > 0) {
			for (idx = 0; idx < QT_inter.size(); ++idx) {
				QT_interval = QT_interval * idx / (idx + 1) + QT_inter.get(idx) * 1.0 / (idx + 1);
			}
			QT_interval *= elem;
		}
		// compute QTc based on Bazett
		QTc = QT_interval / (Math.sqrt(60 / HR));
		
		//ST elevation or degression detection
		for (idx = 0; idx < uni_size; ++idx){
			ST_base.add((Q_onset_amp.get(idx) + P_onset_amp.get(idx))/2.0);
		}
		int win60 = 60*fs/1000;
		
		for (idx = 0; idx < uni_size; ++idx){
			ST_move.add((ecg_data.get(S_end.get(idx)+win60-1)+ecg_data.get(S_end.get(idx)+win60)+ecg_data.get(S_end.get(idx)+win60+1))/3.0 - ST_base.get(idx));
		}
		
		
		for(idx = 0; idx < uni_size; ++idx){
			if(ST_max < ST_move.get(idx)){
				ST_max = ST_move.get(idx);
			}
			if(ST_min > ST_move.get(idx)){
				ST_min = ST_move.get(idx);
			}
			ST_avg += ST_move.get(idx)/(uni_size -2);
		}
		ST_avg = ST_avg - ST_max/(uni_size - 2) - ST_min/(uni_size - 2);
		ST_avg = ST_avg/(65536.0*12.0)*2.42*1000;
		
		
		return true;
	}
	
	public boolean run(List<Double> data) {
		read_ECG_Data(data, ecg_data);
		boolean result = Detect();
		return result;
		
		}
	}
	
	
	

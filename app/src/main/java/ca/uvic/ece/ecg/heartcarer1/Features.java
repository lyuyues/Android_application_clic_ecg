package ca.uvic.ece.ecg.heartcarer1;

import java.util.ArrayList;

/**
 * Written by Raphael Mendes, a previous co-op student in our team
 */
public class Features {
	private int beats;
	private int bpm;
	private float RRinterval;
	private ArrayList<QRS> qrs;
	
	public Features() {
		super();
		this.qrs = new ArrayList<QRS>();
	}
	public int getBeats() {
		return beats;
	}
	public void setBeats(int beats) {
		this.beats = beats;
	}
	public int getBpm() {
		return bpm;
	}
	public void setBpm(int bpm) {
		this.bpm = bpm;
	}
	
	public float getRRinterval(){
		return RRinterval;
	}
	
	public float calculateRRinterval() {
		if(qrs.size() == 0){
			return 0;
		}
		float[] rr = new float[qrs.size() - 1];
		float total = 0;
		float ret;
		
		for(int i = qrs.size() - 1, j = 0; i > 0;j++, i--){
			rr[j] = qrs.get(i).getTime() - qrs.get(i - 1).getTime();
			//System.out.println(rr[j]);
		}
		for(int i = 0; i<rr.length; i++){
			total += rr[i];
			}
		
		//System.out.println("Total: " + total);
		ret = total/rr.length; 
		//System.out.println("rr.length: " + rr.length);
		
		this.setRRinterval(ret);
		System.out.println("getRR: " + getRRinterval());
		return this.getRRinterval();
	
	}
	protected void setRRinterval(float rRinterval) {
		RRinterval = rRinterval;
	}
	public ArrayList<QRS> getQrs() {
		return qrs;
	}
	protected void setQrs(ArrayList<QRS> qrs) {
		this.qrs = qrs;
	}
}
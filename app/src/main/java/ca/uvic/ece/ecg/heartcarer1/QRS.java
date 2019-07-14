package ca.uvic.ece.ecg.heartcarer1;

/**
 * Written by Raphael Mendes, a previous co-op student in our team
 */
public class QRS {
	private int time;
	private char condition;

	public QRS(int time, char condition) {
		super();
		this.time = time;
		this.condition = condition;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public char getCondition() {
		return condition;
	}

	public void setCondition(char condition) {
		this.condition = condition;
	}
}
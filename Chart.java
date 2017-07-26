import java.util.BitSet;
import java.util.Arrays;
import java.io.File;

public class Chart {
	public enum GameMode {
		DANCE, PUMP, KB7, PARA, BEAT, TECHNO, LIGHTS, KICKBOX
	}
	public enum StepsType {
		SINGLE, DOUBLE, COUPLE, SOLO, ROUTINE, THREE_PANEL
	}
	public enum Difficulty {
		BEGINNER, EASY, MEDIUM, HARD, CHALLENGE, EDIT
	}
	
	SMFile smFile;
	
	public int difficultyValue;
	public Difficulty difficultyType;
	public String difficultyDescription;
	public GameMode gameMode;
	public StepsType stepsType;
	byte[][] notes;
	
	double offset = -1;
	
	TimeCourse timeCourse = null;
	
	public Chart() {
		
	}
	
	// <return>: In bars
	public double length() {
		return notes.length / 192.0;
	}
	
	public byte getState(int position, int arrow) {
		return notes[position][arrow];
	}
	
	public double getBPM(int position) {
		if (timeCourse == null) {
			return smFile.timeCourse.getBPM(position);
		} else {
			return timeCourse.getBPM(position);
		}
	}
	
	public double getOffset() {
		if (offset == -1) return smFile.offset;
		else return offset;
	}
}

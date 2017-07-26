import java.util.ArrayList;
import java.util.Arrays;

// Saves bpmChanges, delays, stops, warps
public class TimeCourse {
	private ArrayList<Integer> bpmChangesPosition = new ArrayList<>();
	private ArrayList<Double> bpmChangesValue = new ArrayList<>();
	
	public TimeCourse(double initialBPM) {
		bpmChangesPosition.add(0);
		bpmChangesValue.add(initialBPM);
	}
	
	public void setBpmChange(int position, double bpm) {
		int index = Arrays.binarySearch(bpmChangesPosition.toArray(), new Integer(position));
		if (index < 0) {
			index = -index - 1;
			bpmChangesPosition.add(index, position);
			bpmChangesValue.add(index, bpm);
		} else {
			bpmChangesValue.set(index, bpm);
		}
	}
	
	public double getBPM(int position) {
		int index = Arrays.binarySearch(bpmChangesPosition.toArray(), new Integer(position)); // Index of bpmChange in bpmChangesPosition and bpmChangesValue
		return bpmChangesValue.get(index < 0 ? (-index - 2) : index);
	}
}

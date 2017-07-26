import java.util.ArrayList;
import java.io.File;

public class SMFile {
	public String
		version,
		title,
		subtitle,
		artist,
		credit;
	
	public File
		directoryLocation,
		banner,
		background,
		song;
	
	public double offset, sampleStart, sampleLength;
	
	public TimeCourse timeCourse;
	
	ArrayList<Chart> charts = new ArrayList<>();
}

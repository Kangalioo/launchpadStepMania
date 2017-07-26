import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.nio.CharBuffer;
import java.util.HashMap;

public class SSCParser {
	private File file;
	private BufferedReader reader;
	private SMFile smfile;
	
	private LinkedList<Character> readingStack = new LinkedList<>();
	private int currentLine = 0;
	
	public SSCParser(File file) {
		this.file = file;
	}
	
	public SMFile parse() {
		smfile = new SMFile();
		smfile.directoryLocation = file.getParentFile();
		
		try {
			reader = new BufferedReader(new FileReader(file));
			
			HashMap<String, String> map = new HashMap<>();
			String[] pair;
			
			while ((pair = parseKeyValuePair()) != null) {
				map.put(pair[0].toLowerCase(), pair[1].equals("") ? null : pair[1]);
				if (pair[0].equals("ATTACKS")) break;
			}
			insertGlobalMetadata(map);
			
			while (true) {
				map.clear();
				while ((pair = parseKeyValuePair()) != null) {
					map.put(pair[0].toLowerCase(), pair[1].equals("") ? null : pair[1]);
					if (pair[0].equals("NOTES")) break;
				}
				if (map.isEmpty()) break;
				insertChart(map);
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return smfile;
	}
	
	private void insertGlobalMetadata(HashMap<String, String> map) throws IOException {
		smfile.version = map.get("version");
		smfile.title = map.get("title");
		smfile.subtitle = map.get("subtitle");
		smfile.artist = map.get("artist");
		smfile.credit = map.get("credit");
		smfile.offset = Double.parseDouble(map.get("offset"));
		smfile.sampleStart = Double.parseDouble(map.get("samplestart"));
		smfile.sampleLength = Double.parseDouble(map.get("samplelength"));
		smfile.banner = new File(map.get("banner"));
		smfile.background = new File(map.get("background"));
		smfile.song = new File(map.get("music"));
		smfile.timeCourse = generateTimeCourse(map);
	}
	
	private void insertChart(HashMap<String, String> map) throws IOException {
		Chart chart = new Chart();
		chart.smFile = smfile;
		
		String[] stepsTypeTokens = map.get("stepstype").split("-");
		chart.gameMode = Chart.GameMode.valueOf(stepsTypeTokens[0].toUpperCase());
		try {
			chart.stepsType = Chart.StepsType.valueOf(stepsTypeTokens[1].replaceAll("3", "three_").toUpperCase());
		} catch (IllegalArgumentException e) {
			System.err.println("WARNING: Dance chart with StepsType \"" + map.get("stepstype") + "\" not loaded.");
			return;
		}
		chart.difficultyValue = Integer.parseInt(map.get("meter"));
		chart.difficultyType = Chart.Difficulty.valueOf(map.get("difficulty").toUpperCase());
		chart.difficultyDescription = map.get("description");
		
		if (map.containsKey("bpms")) chart.timeCourse = generateTimeCourse(map);
		chart.notes = parseNotes(map.get("notes"), 4); // TODO: Replace 4 with something variable
		
		smfile.charts.add(chart);
	}
	
	private TimeCourse generateTimeCourse(HashMap<String, String> map) {
		TimeCourse timeCourse = new TimeCourse(0);
		for (String bpmChange : map.get("bpms").split(",\\s*")) {
			String[] bpmChangeTokens = bpmChange.split("=");
			timeCourse.setBpmChange((int) Math.round(Double.parseDouble(bpmChangeTokens[0])), Double.parseDouble(bpmChangeTokens[1]));
		}
		
		return timeCourse;
	}
	
	private byte[][] parseNotes(String noteData, int rowSize) {
		String[] bars = noteData.split(",\\s*");
		byte[][] notes = new byte[bars.length * 192][4];
		int currentTick = 0;
		for (String bar : bars) {
			int resolution = bar.length() / rowSize;
			for (int i = 0; i < resolution; i++) {
				for (int j = 0; j < rowSize; j++) {
					byte noteByte = noteCharToByte(bar.charAt(i * rowSize + j));
					notes[currentTick + (192 / resolution) * i][j] = noteByte;
				}
			}
			currentTick += 192;
		}
		
		currentTick = 0;
		for (byte[] row : notes) {
			for (int i = 0; i < 4; i++) {
				byte noteByte = notes[currentTick][i];
				if (noteByte == 2 || noteByte == 4) {
					for (int k = 1; true; k++) {
						if (notes[currentTick + k][i] == 3) break; // TODO: Be able to replace "== 3" with "== noteByte + 1"
						notes[currentTick + k][i] = (byte) (noteByte + 1);
					}
				}
			}
			currentTick++;
		}
		
		return notes;
	}
	
	private byte noteCharToByte(char c) {
		if (c <= '4') {
			return (byte) (c - '0');
		} else {
			switch (c) {
				case 'M': return (byte) 6;
				case 'K': return (byte) 7;
				case 'L': return (byte) 8;
				case 'F': return (byte) 9;
			}
		}
		
		throwException("Invalid note character " + c);
		return -123; // This shouldn't be executed
	}
	
	// Returns null if no key value pair is found
	private String[] parseKeyValuePair() throws IOException {
		char c;
		while (true) {
			c = read();
			if (!Character.isWhitespace(c)) {
				if (c == '#') break;
				else return null;
			}
		}
		
		StringBuilder builder = new StringBuilder();
		while ((c = read()) != ':') builder.append(c);
		String key = builder.toString();
		
		builder.setLength(0);
		while ((c = read()) != ';') builder.append(c);
		String value = builder.toString();
		
		return new String[]{key, value};
	}
	
	// This splits the value at regex ",\s*" while skipping newlines (and carriage returns) and single-line comments
	private char read() throws IOException {
		if (readingStack.size() > 0) return readingStack.pop();
		char c;
		do {
			c = (char) reader.read();
			if (c == '\n') currentLine++; // This does not work for Mac :v
		} while (c == '\r' || c == '\n');
		if (c == '/') {
			char c2 = (char) reader.read();
			if (c2 == '/') {
				while (reader.read() != '\n'); // Skips characters until newline is reached
				c = read(); // Yes, this is intentional
			} else {
				c = '/';
				readingStack.push(c2);
			}
		}
		return c;
	}
	
	private void throwException(String message) {
		throw new RuntimeException("Problem while parsing \"" + file.getName() + "\" (Line " + currentLine + "): " + message);
	}
	/*public Chart parse() {
		chart = new Chart();
		
		try {
			reader = new BufferedReader(new FileReader(file));
			parseMetadata1();
			parseMetadata2();
			parseNotes();
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return chart;
	}
	
	// Parse first chunk of metadata
	private void parseMetadata1() throws IOException {
		String line;
		while (!(line = reader.readLine()).equals("")) { // Read until gap between first and second chunk of metadata is reached
			if (line.length() == 0 || line.charAt(0) != '#') continue;
			int colonIndex = line.indexOf(":");
			if (colonIndex == line.length() - 1) continue;
			String text = line.substring(colonIndex + 1, line.endsWith(";") ? line.length() - 1 : line.length());
			if (!text.equals("")) {
				String key = line.substring(1, colonIndex).toLowerCase();
				switch (key) {
					case "version":
						chart.version = text;
						break;
					case "title":
						chart.title = text;
						break;
					case "subtitle":
						chart.subtitle = text;
						break;
					case "artist":
						chart.artist = text;
						break;
					case "credit":
						chart.credit = text;
						break;
					case "banner":
						chart.banner = new File(text);
						break;
					case "background":
						chart.background = new File(text);
						break;
					case "song":
						chart.song = new File(text);
						break;
					//case "offset":
						//chart.offset = Double.parseDouble(text);
						//break;
					case "samplestart":
						chart.sampleStart = Double.parseDouble(text);
						break;
					case "samplelength":
						chart.sampleLength = Double.parseDouble(text);
						break;
					case "bpms":
						readBPMs(text);
					
				}
			}
		}
	}
	
	// Parse second chunk of metadata (dance-single)
	private void parseMetadata2() throws IOException {
		while (reader.readLine().charAt(0) != '/'); // Skip lines until "dance-single" comment is reached
		
		String line;
		while (!(line = reader.readLine()).equals("#NOTES:")) {
			if (line.length() == 0 || line.charAt(0) != '#') continue;
			int colonIndex = line.indexOf(":");
			String text = line.substring(colonIndex + 1, line.endsWith(";") ? line.length() - 1 : line.length());
			switch (line.substring(1, colonIndex).toLowerCase()) {
				case "offset":
					chart.offset = Double.parseDouble(text);
					break;
				case "bpms":
					readBPMs(text);
					break;
				case "delays":
					// TODO
					break;
			}
		}
	}
	
	private void readBPMs(String text) throws IOException {
		ArrayList<String> bpmStrings = new ArrayList<>();
		String line = text;
		do {
			if (line.charAt(0) == '#') break;
			bpmStrings.add(line.replaceAll(",", "").replaceAll(";", ""));
			if (line.endsWith(";")) break;
		} while (!(line = reader.readLine()).equals(";"));
		chart.bpmChangesPosition = new int[bpmStrings.size()];
		chart.bpmChangesValue = new double[chart.bpmChangesPosition.length];
		for (int i = 0; i < chart.bpmChangesPosition.length; i++) {
			String bpmString = bpmStrings.get(i);
			int equalIndex = bpmString.indexOf("=");
			chart.bpmChangesPosition[i] = (int) Double.parseDouble(bpmString.substring(0, equalIndex));
			chart.bpmChangesValue[i] = Double.parseDouble(bpmString.substring(equalIndex + 1, bpmString.length()));
		}
	}
	
	private void parseNotes() throws IOException {
		reader.readLine();
		String line;
		ArrayList<char[]> measures = new ArrayList<>();
		ArrayList<char[]> chartNotes = new ArrayList<>();
		int measureIndex = 0;
		while ((line = reader.readLine()) != null) {
			if (line.charAt(0) == ',' || line.charAt(0) == ';') {
				for (int tick = 0; tick < measure.size(); tick++) {
					for (int i = 0; i < 4; i++) {
						int absoluteTick = measureIndex * 192 + tick * 192 / measure.size();
						chartNotes.set
						chartNotes.get(absoluteTick)[i] = measure.get(tick)[i];
					}
				}
				
				measure.clear();
				measureIndex++;
				
				if (line.charAt(0) == ';') break;
			} else {
				char[] tick = new char[4];
				for (int i = 0; i < tick.length; i++) {
					tick[i] = line.charAt(i);
				}
				measure.add(tick);
			}
		}
	}*/
}

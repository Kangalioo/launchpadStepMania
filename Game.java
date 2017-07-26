import javax.sound.midi.MidiUnavailableException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public class Game extends LaunchpadInterface {
	private Chart chart;
	private Future<?> timerFuture;
	
	private int resolution = 16;
	private int stepTicks = 192 / resolution; // Amount of ticks per launchpad row
	private double tickLength;
	
	private int[] colorTypes = {4, 8, 12, 16, 24, 32, 48, 64, 192};
	private byte[] colors    = {STRONG_RED, STRONG_GREEN, LIME_1, STRONG_YELLOW, LIME_1};
	private byte tailColor = WEAK_RED;
	
	private int tick = -stepTicks; // The notes' destination now is at the second to last row
	
	private Clip clip = null;
	
	public Game(Chart chart) throws MidiUnavailableException {
		super();
		this.chart = chart;
	}
	
	public void init() {
		initMusic();
		
		if (chart.getOffset() >= 0) initNotes();
		else clip.start();
		
		wait(Math.abs(chart.getOffset()));
		
		if (chart.getOffset() >= 0) clip.start();
		else initNotes();
	}
	
	private void wait(double time) {
		try {
			Thread.sleep((int) (time * 1000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void initMusic() {
		File musicFile = new File(chart.smFile.directoryLocation.getAbsolutePath() + "/" + chart.smFile.song.getName());
		try {
			clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(musicFile));
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initNotes() {
		System.out.println("BPM: " + chart.getBPM(0));
		tickLength = 1_000_000 * 60 * 4 / chart.getBPM(0) / 192;
		startTimer();
	}
	
	// This timer is triggered 192 times a bar (speed depends on bpm)
	private void startTimer() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		timerFuture = executor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					tick();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 0, (int) tickLength, TimeUnit.MICROSECONDS);
	}
	
	private void tick() {
		if (tick % stepTicks == 0) {
			try {
				redrawArrows();
			} catch (ArrayIndexOutOfBoundsException e) {
				stopTimer();
			}
		}
		
		tick++;
	}
	
	private void stopTimer() {
		timerFuture.cancel(true);
	}
	
	private void redrawArrows() {
		setPreparing(true);
		/*for (int row = 0; row < 8; row++) {
			int tickDrawn = tick + row * stepTicks;
			if (tickDrawn < 0) continue;
			for (int col = 0; col < 4; col++) {
				byte arrow = chart.getState(tickDrawn, col);
				byte color = EMPTY;
				if (arrow == 1 || arrow == 2 || arrow == 4) color = colors[getRhythmPosition(tickDrawn)];
				else if (arrow == 3 || arrow == 5) color = tailColor;
				turnOn(8 - 1 - row, col, color);
			}
		}*/
		for (int row = 7; row > 0; row--) {
			for (int i = 0; i < 4; i++) turnOn(row, i, getButton(row - 1, i));
		}
		int tickDrawn = tick + 7 * stepTicks;
		for (int col = 0; col < 4; col++) {
			turnOn(0, col, getColor(tickDrawn, col));
		}
		setPreparing(false);
	}
	
	private byte getColor(int tick, int col) {
		byte arrow = chart.getState(tick, col);
		byte color = EMPTY;
		if (arrow == 1 || arrow == 2 || arrow == 4) color = colors[getRhythmPosition(tick)];
		else if (arrow == 3 || arrow == 5) color = tailColor;
		return color;
	}
	
	private int getRhythmPosition(int tick) {
		for (int i = 0; i < colorTypes.length; i++) {
			if (tick % (192 / colorTypes[i]) == 0) return i;
		}
		return colorTypes.length - 1;
	}
	
	public void buttonPressed(int row, int column) {
		
	}
}

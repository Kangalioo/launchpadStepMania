import java.io.File;

public class Main {
	public static void main(String[] args) {
		try {
			File file = new File("/opt/stepmania/Songs/In The Groove/Charlene/Charlene.ssc");
			Game launchpad = new Game((new SSCParser(file)).parse().charts.get(3));
			launchpad.open();
			
			System.out.println("Connection is ready");
			launchpad.init();
			System.console().readLine();
			launchpad.close();
			System.exit(0);
		} catch (javax.sound.midi.MidiUnavailableException e) {
			e.printStackTrace();
		}
	}
}


package logging;

import java.io.*;

public class RotatingLogger extends ThreadedWriter {

	private File logDir;

	private static final int LINES_PER_CHECK = 1000;
	private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;

	public RotatingLogger(File myLogDir, boolean autoFlush) throws IOException {
		super(new File(myLogDir, Long.toString(System.currentTimeMillis())), autoFlush);
		this.logDir = myLogDir;
	}

	@Override
	public void run() {

		int stepCounter = 0;
		while (true) {
			if (!this.handleLineDump()) {
				break;
			}

			/*
			 * Every so often check if our file is too large
			 */
			stepCounter = (stepCounter + 1) % RotatingLogger.LINES_PER_CHECK;
			if (stepCounter == 0) {
				/*
				 * If too large, rotate
				 */
				if (this.outputFileRef.length() > RotatingLogger.MAX_FILE_SIZE) {
					try {
						this.actualOutput.close();
						this.outputFileRef = new File(this.logDir, Long.toString(System.currentTimeMillis()));
						this.actualOutput = new BufferedWriter(new FileWriter(this.outputFileRef));
					} catch (Exception e) {
						System.err.println("ERROR IN LOG ROTATING");
						System.exit(-2);
					}
				}
			}
		}
		this.interalShutdown();
	}

}

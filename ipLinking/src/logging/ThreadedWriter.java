package logging;

import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ThreadedWriter extends Writer implements Runnable {

	private boolean autoFlush;
	protected BufferedWriter actualOutput;
	protected File outputFileRef;
	private LinkedBlockingQueue<String> internalQueue;

	protected static final String POISON_PILL = "094892489237489237589237589234289347289";

	public ThreadedWriter(String fileName) throws IOException {
		this(new File(fileName), false);
	}

	public ThreadedWriter(String fileName, boolean fastFlush) throws IOException {
		this(new File(fileName), fastFlush);
	}

	public ThreadedWriter(File outFile, boolean fastFlush) throws IOException {
		super();
		this.autoFlush = fastFlush;
		this.outputFileRef = outFile;
		this.actualOutput = new BufferedWriter(new FileWriter(this.outputFileRef));
		this.internalQueue = new LinkedBlockingQueue<String>();
	}

	@Override
	public void run() {
		while (true) {
			if (!this.handleLineDump()) {
				break;
			}
		}
		this.interalShutdown();
	}

	protected void interalShutdown() {
		try {
			this.actualOutput.close();
		} catch (IOException e2) {
			System.err.println("HOLY FUCK IO EXCEPTION");
			e2.printStackTrace();
			System.exit(-2);
		}

		if (this.internalQueue.size() > 0) {
			throw new RuntimeException("Information added to threaded writer after close!");
		}
	}

	protected boolean handleLineDump() {
		boolean writeableString = true;
		try {
			String currentStr = this.internalQueue.take();
			writeableString = !(currentStr.equals(ThreadedWriter.POISON_PILL));
			if (writeableString) {
				this.actualOutput.write(currentStr);
				if (this.autoFlush) {
					this.actualOutput.flush();
				}
			}
		} catch (Exception e) {
			System.err.println("EXCEPTION IN I/O OF LOGGER");
			System.exit(-2);
		}

		return writeableString;
	}

	@Override
	public void close() throws IOException {
		try {
			this.internalQueue.put(ThreadedWriter.POISON_PILL);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		throw new RuntimeException("Incorrect method to write with.");
	}

	public void write(String outStr) throws IOException {
		try {
			this.internalQueue.put(outStr);
		} catch (InterruptedException e) {
			throw new IOException("error inserting into internal queue");
		}
	}

	public void writeOrDie(String outStr) {
		try {
			this.write(outStr);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

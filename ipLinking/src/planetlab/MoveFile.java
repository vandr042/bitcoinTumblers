package planetlab;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class MoveFile implements Runnable {

	private String fromFile;
	private String toFile;
	private String idFile;

	private static final long TIMEOUT = 10;
	private static final TimeUnit TIMEOUT_UNITS = TimeUnit.SECONDS;
	private static File DUMP_DIR = new File("fetch/");

	public static MoveFile fetchRemoteFile(String user, String idFile, String theHost, String theFile) {
		/*
		 * Ensure the dump dir exists
		 */
		File localDir = new File(MoveFile.DUMP_DIR, theHost);
		localDir.mkdirs();

		/*
		 * Build the worker
		 */
		MoveFile resultantWorker = new MoveFile(user + "@" + theHost + ":" + theFile, localDir.getAbsolutePath(),
				idFile);
		return resultantWorker;
	}

	public static MoveFile pushLocalFile(String user, String idFile, String theHost, String localFile,
			String remoteDir) {
		MoveFile resultantWorker = new MoveFile(localFile, user + "@" + theHost + ":" + remoteDir, idFile);
		return resultantWorker;
	}

	public MoveFile(String fromPath, String toPath, String idFile) {
		this.fromFile = fromPath;
		this.toFile = toPath;
		this.idFile = idFile;
	}

	public void blockingExecute() throws InterruptedException {
		this.blockingExecute(0);
	}
	
	public void blockingExecute(long msTimeOut) throws InterruptedException{
		Thread selfThread = new Thread(this);
		selfThread.start();
		selfThread.join(msTimeOut);		
	}

	// TODO think deeply about how we want to handle errors
	public void run() {
		String scpCmd = "scp -i " + this.idFile + " " + this.fromFile + " " + this.toFile;
		Runtime myRT = Runtime.getRuntime();

		boolean finished = false;
		try {
			Process mySCPProc = myRT.exec(scpCmd);
			finished = mySCPProc.waitFor(MoveFile.TIMEOUT, MoveFile.TIMEOUT_UNITS);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// TODO enh logging
		if (finished) {
			System.out.println("finished");
		} else {
			System.out.println("didn't finish");
		}
	}

	public static void main(String args[]) {
		// MoveFile tSelf = MoveFile.fetchRemoteFile("pendgaft",
		// "~/.ssh/id_rsa", "waterhouse-umh.cs.umn.edu", "~/test");
		MoveFile tSelf = MoveFile.pushLocalFile("pendgaft", "~/.ssh/id_rsa", "waterhouse-umh.cs.umn.edu", "foo", "~/");
		Thread tThread = new Thread(tSelf);
		tThread.start();
	}
}

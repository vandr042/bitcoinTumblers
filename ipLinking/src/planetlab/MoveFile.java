package planetlab;

import java.io.*;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class MoveFile implements Runnable {

	private String fromFile;
	private String toFile;
	private String idFile;

	private boolean local;

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
		MoveFile resultantWorker = null;
		if (MoveFile.isLocal(theHost)) {
			resultantWorker = new MoveFile(theFile, localDir.getAbsolutePath(), null, true);
		} else {
			resultantWorker = new MoveFile(user + "@" + theHost + ":" + theFile, localDir.getAbsolutePath(), idFile);
		}
		return resultantWorker;
	}

	public static MoveFile fetchRemoteFile(String user, String idFile, String theHost, String theFile, String destDir) {
		MoveFile resultantWorker = null;
		if (MoveFile.isLocal(theHost)) {
			resultantWorker = new MoveFile(theFile, destDir, null, true);
		} else {
			resultantWorker = new MoveFile(user + "@" + theHost + ":" + theFile, destDir, idFile);
		}
		return resultantWorker;
	}

	public static MoveFile pushLocalFile(String user, String idFile, String theHost, String localFile,
			String remoteDir) {
		MoveFile resultantWorker = null;
		if (MoveFile.isLocal(theHost)) {
			resultantWorker = new MoveFile(localFile, remoteDir, null, true);
		} else {
			resultantWorker = new MoveFile(localFile, user + "@" + theHost + ":" + remoteDir, idFile);
		}
		return resultantWorker;
	}

	public MoveFile(String fromPath, String toPath, String idFile) {
		this(fromPath, toPath, idFile, false);
	}

	public MoveFile(String fromPath, String toPath, String idFile, boolean local) {
		this.fromFile = fromPath;
		this.toFile = toPath;
		this.idFile = idFile;
		this.local = local;
	}

	public void blockingExecute() throws InterruptedException {
		this.blockingExecute(0);
	}

	public void blockingExecute(long msTimeOut) throws InterruptedException {
		Thread selfThread = new Thread(this);
		selfThread.start();
		selfThread.join(msTimeOut);
	}

	// TODO think deeply about how we want to handle errors
	public void run() {
		String cmd = null;
		if (this.local) {
			cmd = "cp " + this.fromFile + " " + this.toFile;
		} else {

			cmd = "scp -i " + this.idFile + " " + this.fromFile + " " + this.toFile;
		}
		Runtime myRT = Runtime.getRuntime();

		boolean finished = false;
		try {
			Process childProc = myRT.exec(cmd);
			finished = childProc.waitFor(MoveFile.TIMEOUT, MoveFile.TIMEOUT_UNITS);
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

	public static boolean isLocal(String host) {
		try {
			String hostName = InetAddress.getLocalHost().getHostName();
			return hostName.equals(host) || hostName.split(".")[0].equals(host);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}
}

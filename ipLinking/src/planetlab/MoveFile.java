package planetlab;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class MoveFile implements Runnable {

	private String fromFile;
	private String toFile;
	private String idFile;

	private static final long TIMEOUT = 10;
	private static final TimeUnit TIMEOUT_UNITS = TimeUnit.SECONDS;
	private static final File DUMP_DIR = new File("fetch/");

	public static MoveFile fetchRemoteFile(String user, String idFile, String theHost, String theFile) {
		File localDir = new File(MoveFile.DUMP_DIR, theHost);
		localDir.mkdirs();

		MoveFile resultantWorker = new MoveFile(user + "@" + theHost + ":" + theFile, localDir.getAbsolutePath(),
				idFile);
		return resultantWorker;
	}
	
	public static MoveFile pushLocalFile(String user, String localFile, String remoteDir){
		//TODO implement
		return null;
	}

	public MoveFile(String fromPath, String toPath, String idFile) {
		this.fromFile = fromPath;
		this.toFile = toPath;
		this.idFile = idFile;
	}

	// TODO think deeply about how we want to handle errors
	public void run() {
		String scpCmd = "scp -i " + this.idFile + " " + this.fromFile + " " + this.toFile;
		Runtime myRT = Runtime.getRuntime();

		try {
			Process mySCPProc = myRT.exec(scpCmd);
			boolean finished = mySCPProc.waitFor(MoveFile.TIMEOUT, MoveFile.TIMEOUT_UNITS);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		//TODO test!
		MoveFile tSelf = MoveFile.fetchRemoteFile("pendgaft", "~/.ssh/id_rsa", "waterhouse-umh.cs.umn.edu", "test");
		Thread tThread = new Thread(tSelf);
		tThread.start();
	}
}

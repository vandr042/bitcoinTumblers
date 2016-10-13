package planetlab;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;

//TODO a resume method/mode would be really cool
public class PlanetLabMonitor implements Runnable {

	private Runtime myRT;

	private List<String> planetlabHosts;
	private Set<String> activePlanetlabHosts;
	private Set<String> activeBTCHosts;
	private HashMap<String, Set<String>> bitcoinPeerToVP;
	private HashMap<String, Set<String>> vpToBitcoinPeer;

	// TODO return to normal values
	private static final int MAX_FDS_PER_VP = 10000;
	private static final int VPS_PER_BTC = 1;

	private static final double MIN_MEM = 2.5;

	private static final long REFRESH_INTERVAL_SEC = 3600;

	public static final File PL_RANK_FILE = new File("/home/pendgaft/git/software/planetlab/high-mem.csv");

	public static final File PL_MON_DIR = new File("/home/btc/");
	public static final File FULL_RECOVERY_DIR = new File(PlanetLabMonitor.PL_MON_DIR, "recovery");
	public static final File FULL_LOGS_DIR = new File(PlanetLabMonitor.PL_MON_DIR, "logs");
	private static final Pattern RECOV_FILE_PAT = Pattern.compile("([0-9]+)\\-recovery\\-(.+)");

	private static final String PL_USER = "umn_hopper";
	private static final String PL_KEY = "~/.ssh/planetlab";

	private static final String PL_RECOVERY_DIR = "ipLinking/recovery/";

	public PlanetLabMonitor() throws IOException {

		/*
		 * Ensure dirs exist
		 */
		PlanetLabMonitor.FULL_RECOVERY_DIR.mkdirs();
		PlanetLabMonitor.FULL_LOGS_DIR.mkdirs();

		this.myRT = Runtime.getRuntime();
		this.planetlabHosts = new LinkedList<String>();
		this.activePlanetlabHosts = new HashSet<String>();
		this.bitcoinPeerToVP = new HashMap<String, Set<String>>();
		this.vpToBitcoinPeer = new HashMap<String, Set<String>>();

		/*
		 * Init state load
		 */
		this.initVPs();
		this.refresh();
	}

	public void run() {
		try {
			while (true) {
				Thread.sleep(PlanetLabMonitor.REFRESH_INTERVAL_SEC * 1000);
				this.refresh();
			}
		} catch (InterruptedException e) {
			// silently exit thread
		}
	}

	/**
	 * Loads the list of PlanetLab nodes, ignoring nodes lower than the memory
	 * threshold
	 * 
	 * @throws IOException
	 */
	private void initVPs() throws IOException {
		BufferedReader inBuffer = new BufferedReader(new FileReader(PlanetLabMonitor.PL_RANK_FILE));
		String line = null;
		while ((line = inBuffer.readLine()) != null && this.planetlabHosts.size() < PlanetLabMonitor.VPS_PER_BTC * 2) {
			String[] tokens = line.split(",");
			if (tokens.length > 1) {
				double availMem = Double.parseDouble(tokens[1]);
				if (availMem > PlanetLabMonitor.MIN_MEM) {
					this.planetlabHosts.add(tokens[0]);
				}
			}
		}
		inBuffer.close();

		System.out.println("Loaded pool of " + this.planetlabHosts.size() + " poss PlanetLab hosts.");
	}

	private void refresh() {

		/*
		 * Update the publicly reachable BTC nodes that we're watching from the
		 * full nodes
		 */
		try {
			this.activeBTCHosts = this.getMostRecentRecoveries();
			for (String tPeer : this.activeBTCHosts) {
				if (!this.bitcoinPeerToVP.containsKey(tPeer)) {
					this.bitcoinPeerToVP.put(tPeer, new HashSet<String>());
				}
			}
			Set<String> peersToDelete = new HashSet<String>();
			peersToDelete.addAll(this.bitcoinPeerToVP.keySet());
			peersToDelete.removeAll(this.activeBTCHosts);
			for (String tPeer : peersToDelete) {
				this.bitcoinPeerToVP.remove(tPeer);
			}
			for (String tVP : this.vpToBitcoinPeer.keySet()) {
				this.vpToBitcoinPeer.get(tVP).removeAll(peersToDelete);
			}
		} catch (IOException e) {
			System.err.println("Fatal error while refreshing monitor state!");
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("monitoring " + this.activeBTCHosts.size() + " BTC hosts");

		/*
		 * Hunt for failed VPs and clear them
		 */
		Set<String> failedVPs = new HashSet<String>();
		for (String tHost : this.activePlanetlabHosts) {
			if (!this.testVPAlive(tHost)) {
				failedVPs.add(tHost);
			}
		}
		for (String tFailedHost : failedVPs) {
			System.out.println("Handling failed VP: " + tFailedHost);
			/*
			 * Desperately try to kill any zombies as we use these nodes too...
			 */
			try {
				this.executeAndFetch(tFailedHost, "killall python", 10000);
				this.executeAndFetch(tFailedHost, "killall java", 10000);
			} catch (Exception e) {
				System.out.println("Pos zombie poss on: " + tFailedHost);
			}
			for (String tBTCPeer : this.vpToBitcoinPeer.get(tFailedHost)) {
				this.bitcoinPeerToVP.get(tBTCPeer).remove(tFailedHost);
			}
			this.vpToBitcoinPeer.remove(tFailedHost);
			this.activePlanetlabHosts.remove(tFailedHost);
		}

		/*
		 * Spin up new VPs as needed
		 */
		int goalVPs = Math.max((int) Math.ceil(((double) (this.activeBTCHosts.size() * PlanetLabMonitor.VPS_PER_BTC))
				/ (double) PlanetLabMonitor.MAX_FDS_PER_VP), PlanetLabMonitor.VPS_PER_BTC);
		System.out.println("Need " + goalVPs + " vps, have " + this.activePlanetlabHosts.size());
		while (this.activePlanetlabHosts.size() < goalVPs) {
			/*
			 * Select node, exit loop and report if we can't find a node
			 */
			String selectedHost = this.getNextPlanetLabNode(failedVPs);

			/*
			 * Try failed nodes, because while possibly unreliable, better than
			 * nothing
			 */
			if (selectedHost == null) {
				selectedHost = this.getNextPlanetLabNode(null);
			}

			/*
			 * Rather then die, yell about our plight, and live in suboptimal
			 * land
			 */
			if (selectedHost == null) {
				System.out.println(
						"Can't find sufficent hosts!  Need: " + goalVPs + " Have: " + this.activePlanetlabHosts.size());
				break;
			} else {
				System.out.println("trying to add " + selectedHost);
			}

			/*
			 * First deploy the code base, then try and compile and run
			 */
			try {
				/*
				 * Move JDK and inflate
				 */
				//FIXME this should only be done if the JDK is not there already!
				MoveFile jdkMove = MoveFile.pushLocalFile(PL_USER, PL_KEY, selectedHost, "deploy/jdk.tar.gz", "~/");
				jdkMove.blockingExecute(15 * 60 * 1000);
				this.executeAndFetch(selectedHost, "tar -xzf jdk.tar.gz", 60 * 1000);

				/*
				 * Move Codebase and inflate
				 */
				MoveFile codeMove = MoveFile.pushLocalFile(PL_USER, PL_KEY, selectedHost, "deploy/ipLinking.tgz", "~/");
				codeMove.blockingExecute(2 * 60 * 1000);
				this.executeAndFetch(selectedHost, "tar -xzf ipLinking.tgz", 60 * 1000);

				/*
				 * Fire up the bass cannon
				 */
				this.executeAndFetch(selectedHost, "cd ipLinking;./runPLNode.py --pl --vp&", 10000);
				this.activePlanetlabHosts.add(selectedHost);
				this.vpToBitcoinPeer.put(selectedHost, new HashSet<String>());
				System.out.println("added " + selectedHost);
				System.out.println("size " + this.activePlanetlabHosts.size());
			} catch (Exception e) {
				// TODO what about errors here????
				e.printStackTrace();
			}
		}

		/*
		 * Write live list
		 */
		try {
			BufferedWriter statBuff = new BufferedWriter(new FileWriter("activeVPs.txt"));
			for (String tVP : this.activeBTCHosts) {
				statBuff.write(tVP);
			}
			statBuff.close();
		} catch (Exception e) {
			System.out.println("ERROR DUMPING ACTIVE VPs");
		}

		/*
		 * Update ownership of BTC nodes, attempting to assign nodes
		 */
		List<String> vpList = new ArrayList<String>();
		vpList.addAll(this.activePlanetlabHosts);
		Random rng = new Random();
		for (String tPeer : this.bitcoinPeerToVP.keySet()) {
			Set<String> respVPs = this.bitcoinPeerToVP.get(tPeer);
			int rngOffset = rng.nextInt(vpList.size());
			for (int pos = 0; pos < vpList.size() && respVPs.size() < PlanetLabMonitor.VPS_PER_BTC; pos++) {
				int slot = (pos + rngOffset) % vpList.size();
				String tVP = vpList.get(slot);
				if ((!respVPs.contains(tVP))
						&& this.vpToBitcoinPeer.get(tVP).size() < PlanetLabMonitor.MAX_FDS_PER_VP) {
					respVPs.add(tVP);
					this.vpToBitcoinPeer.get(tVP).add(tPeer);
				}
			}
		}

		/*
		 * Push new Resp lists
		 */
		File currTempFile = new File(System.currentTimeMillis() + "-recovery");
		for (String tHost : this.vpToBitcoinPeer.keySet()) {
			try {
				BufferedWriter outBuff = new BufferedWriter(new FileWriter(currTempFile));
				for (String tBTC : this.vpToBitcoinPeer.get(tHost)) {
					outBuff.write(tBTC + "\n");
				}
				outBuff.close();

				MoveFile tMover = MoveFile.pushLocalFile(PL_USER, PL_KEY, tHost, currTempFile.getAbsolutePath(),
						PlanetLabMonitor.PL_RECOVERY_DIR);
				// TODO handle this error more intelligently
				tMover.blockingExecute(10000);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		currTempFile.delete();
	}

	private String getNextPlanetLabNode(Set<String> excludeSet) {
		String selectedHost = null;
		for (String tHost : this.planetlabHosts) {
			if (this.activePlanetlabHosts.contains(tHost)) {
				continue;
			}

			if (excludeSet != null && excludeSet.contains(tHost)) {
				continue;
			}

			selectedHost = tHost;
			break;
		}

		return selectedHost;
	}

	private boolean testVPAlive(String host) {
		String[] queryResults = null;
		try {
			queryResults = this.executeAndFetch(host, "ps aux | grep runSpy.py", 30000);
		} catch (Exception e) {
			// TODO log this exception
			return false;
		}

		if (queryResults == null) {
			return false;
		}

		for (String tLine : queryResults) {
			if (tLine.contains("python") && tLine.contains("runSpy.py")) {
				return true;
			}
		}

		return false;
	}

	private Set<String> getMostRecentRecoveries() throws IOException {
		File[] recFiles = PlanetLabMonitor.FULL_RECOVERY_DIR.listFiles();
		HashMap<String, Long> oldestTSMap = new HashMap<String, Long>();

		/*
		 * Find the most recent recovery file for each host
		 */
		for (File tFile : recFiles) {
			String fName = tFile.getName();
			Matcher recMatch = PlanetLabMonitor.RECOV_FILE_PAT.matcher(fName);
			if (recMatch.find()) {
				String host = recMatch.group(2);
				Long ts = Long.parseLong(recMatch.group(1));
				if ((!oldestTSMap.containsKey(host)) || oldestTSMap.get(host) < ts) {
					oldestTSMap.put(host, ts);
				}
			}
		}

		Set<String> retSet = new HashSet<String>();
		for (String tHost : oldestTSMap.keySet()) {
			// TODO freshness check of recovery file

			/*
			 * Read in the contents of the file, adding the socket addresses to
			 * the return set
			 */
			BufferedReader fBuff = new BufferedReader(new FileReader(
					new File(PlanetLabMonitor.FULL_RECOVERY_DIR, "" + oldestTSMap.get(tHost) + "-recovery-" + tHost)));
			// TODO remove after test
			System.out.println("Freshest TS for " + tHost + " is " + oldestTSMap.get(tHost));
			String readStr = null;
			while ((readStr = fBuff.readLine()) != null) {
				readStr = readStr.trim();
				if (readStr.length() > 0) {
					retSet.add(readStr);
				}
			}
			fBuff.close();

			// TODO clean the old files since we don't want to keep them around
		}

		return retSet;
	}

	private String[] executeAndFetch(String host, String cmd, long timeoutMS) throws Exception {

		/*
		 * Create a temp script to handle complex commands and move it to remote
		 * host
		 */
		File scriptFile = this.generateTempScript(cmd);
		MoveFile scriptMove = MoveFile.pushLocalFile(PlanetLabMonitor.PL_USER, PlanetLabMonitor.PL_KEY, host,
				scriptFile.getAbsolutePath(), "~/");
		scriptMove.blockingExecute(10000);

		/*
		 * EXECUTE ALL THE THINGS!
		 */
		String sshCmd = "ssh -i " + PlanetLabMonitor.PL_KEY + " " + PlanetLabMonitor.PL_USER + "@" + host + " './"
				+ scriptFile.getName() + "'";
		Process sshProc = this.myRT.exec(sshCmd);
		sshProc.waitFor(10000, TimeUnit.MILLISECONDS);

		/*
		 * Parse output like an adult....
		 */
		InputStream outIn = sshProc.getInputStream();
		byte[] readBuff = new byte[outIn.available()];
		outIn.read(readBuff);
		String foo = new String(readBuff);

		if (foo.length() > 0) {
			return foo.split("\\v");
		} else {
			return null;
		}
	}

	private File generateTempScript(String cmd) throws IOException {
		File scriptFile = new File("tmpScript.sh");
		BufferedWriter outBuffer = new BufferedWriter(new FileWriter(scriptFile));
		outBuffer.write("#!/bin/bash\n");
		outBuffer.write(cmd);
		outBuffer.close();
		scriptFile.setExecutable(true);
		return scriptFile;
	}

	public static void main(String[] args) throws Exception {
		PlanetLabMonitor self = new PlanetLabMonitor();
		Thread selfThread = new Thread(self);
		selfThread.start();
	}

}

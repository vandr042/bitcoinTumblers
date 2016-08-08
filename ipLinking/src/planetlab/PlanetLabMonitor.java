package planetlab;

import java.util.*;
import java.io.*;

public class PlanetLabMonitor {

	private List<String> planetlabHosts;
	private Set<String> activePlanetlabHosts;
	private HashMap<String, Set<String>> bitcoinPeerToVP;

	private Set<String> fullNodes;

	private static final int MAX_FDS_PER_VP = 3800;
	private static final int VPS_PER_BTC = 10;

	public PlanetLabMonitor(String fullNodeFile) throws IOException {

		this.planetlabHosts = new LinkedList<String>();
		this.activePlanetlabHosts = new HashSet<String>();
		this.bitcoinPeerToVP = new HashMap<String, Set<String>>();
		this.fullNodes = new HashSet<String>();

		this.loadFullNodes(fullNodeFile);
	}

	private void loadFullNodes(String confFile) throws IOException {
		BufferedReader inBuff = new BufferedReader(new FileReader(confFile));
		String readStr = null;
		while ((readStr = inBuff.readLine()) != null) {
			readStr = readStr.trim();
			if (readStr.length() > 0) {
				this.fullNodes.add(readStr);
			}
		}
		inBuff.close();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

package peerLink;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/* PLVOperator runs PLVoter on all cluster sizes for a given vp_count and writes 
 * the results in a single line to the FILE provided 
 */
class PLVOperator {
	private int vp_count;
	private int fp;
	private String vpDir;
	private String mapSuffix;
	private BufferedWriter outWriter;
	private float fpCorr;
	private float totalTx;

	/* vpDir must have / at the end */
	public PLVOperator(BufferedWriter w, String vpDir, int vp, int falsePos){
		this.vp_count = vp;
		this.fp = falsePos;
		this.vpDir = vpDir;	
		
		this.fpCorr = 0;
		this.totalTx = 0;
	
		this.mapSuffix = "_" + vp + "vp_" + falsePos + "fp";
		this.outWriter = w;
	}

	public void runTest() throws IOException, ClassNotFoundException{
		PLVoter plv;
		String outString = "";

		plv = genPLV("c1");
		plv.link(1);
		updateStats(plv);
		outString = updateOutString(plv, outString);

		plv = genPLV("c2");
		plv.link(2);
		updateStats(plv);
		outString = updateOutString(plv, outString);

		plv = genPLV("c4");
		plv.link(4);
		updateStats(plv);
		outString = updateOutString(plv, outString);
		
		plv = genPLV("c5");
		plv.link(5);
		updateStats(plv);
		outString = updateOutString(plv, outString);
		
		plv = genPLV("c10");
		plv.link(10);
		updateStats(plv);
		outString = updateOutString(plv, outString);

		outString = this.vp_count + "," + (this.fpCorr/this.totalTx) + outString;
		outWriter.write(outString + "\n");
		
	}

	private void updateStats(PLVoter plv){
		this.fpCorr += plv.getFPCorr();
		this.totalTx += plv.getTotalTx();
	}

	private String updateOutString(PLVoter plv, String outString){	
		plv.checkLinks();
		float tpAcc = plv.getTruePeerAcc();
		float anonSetSize = plv.getAS(); 
		outString = outString + "," + anonSetSize + "," + tpAcc;
		return outString;
	}

	private PLVoter genPLV(String cluster) throws IOException, ClassNotFoundException {
		PLVoter plv = new PLVoter(this.vpDir + cluster + this.mapSuffix, "../../miscScripts/groundTruthLogs/" + cluster + this.mapSuffix + "-txLinkSynth-groundTruth.log");
		return plv;
	}
}

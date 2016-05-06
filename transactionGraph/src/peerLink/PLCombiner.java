package peerLink;
import java.io.IOException;

public class PLCombiner {
	/* mode: 1 for voting 0 for intersection */
	private String[] scriptArr;
	private int mode;
	private int depth;
	public PLCombiner(String[] scripts, int simMode, int searchDepth) throws IOException{
		scriptArr = scripts;
		mode = simMode;
		depth = searchDepth;
	}
	
	public void combine() throws IOException{
		for (int i = 0; i < scriptArr.length-2; i++){
			if (mode == 0){
				PeerLink p = new PeerLink(scriptArr[i]);
				p.sim(depth);
			}else if(mode == 1){
				PeerLink p = new PeerLink(scriptArr[i]);
				p.simVoting(depth);
			}
		}
	}
	
	/* 
	 * Run the combiner from the command line by supplying 
	 * any number of script arguments followed by the mode
	 * and the search depth, in that order.
	 */
	public static void main(String args[]) throws NumberFormatException, IOException{
		PLCombiner plc = new PLCombiner(args, Integer.parseInt(args[args.length-2]), Integer.parseInt(args[args.length-1]));
		
	}
}
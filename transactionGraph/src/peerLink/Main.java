package peerLink;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
class Main { 

	public static void main(String args[]) throws IOException, ClassNotFoundException {
		String filename = args[0];
		File mapDir = new File(filename);
		File[] contents = mapDir.listFiles();

		File outFile = new File(args[1]);
		BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
		bw.write("vp_count,fpAcc,c1AS,c1TP,c2AS,c2TP,c4AS,c4TP,c5AS,c5TP,c10AS,c10TP\n");	
		
		for (File f : contents) {
			String fn = f.getName();
			int vp_count = Integer.parseInt(fn.substring(0, fn.indexOf('v')));
			String vpDir = filename + fn + "/";
			System.out.println(vpDir);
			PLVOperator plvO = new PLVOperator(bw, vpDir, vp_count, 300);	
			plvO.runTest();
		}
		bw.close();
	}
}	

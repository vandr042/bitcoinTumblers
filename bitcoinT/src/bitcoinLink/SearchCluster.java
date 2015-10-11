package bitcoinLink;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
public class SearchCluster {
	private static int address_length = 34;
	/**
	 * @param args
	 * @throws IOException 
	 */
	
	public static HashMap<String,LinkedList<String>> Search(LinkedList<String> search_list) throws IOException{
		HashMap<String,LinkedList<String>> clusters = new HashMap();
		BufferedReader reader = new BufferedReader(new FileReader("clustersParse.txt"));
		int numfound = 0;
		
		while (numfound < search_list.size()){
			String line = reader.readLine();
		
			/* if line is empty return */
			if (line == null){
				if (numfound > 0){
					return clusters;
				}else{
					System.out.println("No matching addresses found");
					return null;
				}
			}
			
			/* loop through input addresses and search for them in file */
			for (int i = 0; i < search_list.size(); i++){
				String address = search_list.get(i);
				int index = line.indexOf(address);
				String temp = "";
				if (index != -1){
					numfound += 1;
					LinkedList<String> new_cluster = new LinkedList();
					int start = 1;
					int last = start + address_length;
					
					/* add all of the values in adress's cluster and add them to list to be added to map */
					while(true){
						try{
							temp = line.substring(start, last);
							if (temp.compareTo(address) != 0){
								new_cluster.add(temp);
							}
							start = start + 36;
							last = start + address_length;
							
						}catch (NullPointerException e){
							break;
						}
					}
					clusters.put(address, new_cluster);
				}
			}
		}
		return clusters;
	}
	public static void main(String[] args) throws IOException {
			LinkedList<String> list = new LinkedList();
			for (String arg : args){
				list.add(arg);
			}
			HashMap<String, LinkedList<String>> map = Search(list);
			if (map != null){
				System.out.println(map.keySet());
				System.out.println(map.values());
			}
	}
}

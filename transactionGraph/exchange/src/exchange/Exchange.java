package exchange;

import java.net.*;
import java.io.*;

public final class Exchange {

    public static void main(String[] args) throws Exception {
        String url = "https://btc-e.com/api/3/depth/btc_usd";
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		System.out.println(response.toString());
    }
}

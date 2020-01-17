
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLContext;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class LiveOptionPriceFetcher {
	
	static List<OptionData> list = new ArrayList<OptionData>();

	public static void getLivePrice() throws Exception {
		Iterator<OptionData> iterator = list.iterator();
		while(iterator.hasNext()) {
			OptionData data = iterator.next();
			if(BGTask.buy.contains(data.getSymbol()+"###"+data.getType()) && BGTask.sell.contains(data.getSymbol()+"###"+data.getType())) {
				continue;
			}
			try {
				//System.out.println("fetching from web for " + data.getSymbol() + " " + data.getType() + " " + data.getStrikePrice());
				String query = "underlying="+data.getSymbol()+"&instrument=OPTSTK&expiry=30JAN2020&type="+data.getType()+"&strike="+String.format("%.2f", data.getStrikePrice());
				String[] command = new String[]{"curl", "https://www1.nseindia.com/live_market/dynaContent/live_watch/get_quote/ajaxFOGetQuoteJSON.jsp?"+query+"","--compressed","-H", "Accept-Language: en-US,en;q=0.5", "-H", "User-Agent: Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)", "-H", "Accept: */*", "-H", "Referer: https://www.nseindia.com/"};
				System.out.println(Arrays.asList(command).toString());
				//executeCommand(command);
				String response = executeCommandV2(query);
				Gson gson = new Gson();
				//OptionLiveDataMain data1 = gson.fromJson(new JsonReader(new FileReader(new File("curloutput.txt"))), OptionLiveDataMain.class);
				OptionLiveDataMain data1 = gson.fromJson(new JsonReader(new StringReader(response)), OptionLiveDataMain.class);
				//System.out.println("Valid : " + data1.getValid());
				data.setBuyPrice(Double.parseDouble(data1.getData().get(0).getSellPrice1().replace(",", "")));
				data.setSellPrice(Double.parseDouble(data1.getData().get(0).getBuyPrice1().replace(",", "")));
				data.setCurrentPrice(Double.parseDouble(data1.getData().get(0).getLastPrice().replace(",", "")));
				data.setOpen(Double.parseDouble(data1.getData().get(0).getOpenPrice().replace(",", "")));
				data.setLow(Double.parseDouble(data1.getData().get(0).getLowPrice().replace(",", "")));
				
				
			} catch (Exception e) {
				//System.out.println(e.getLocalizedMessage());
			}
		}
	}
	
	public static void getLivePriceDummy() throws Exception {
		System.out.println("fetching dummy...");
		Iterator<OptionData> iterator = list.iterator();
		while(iterator.hasNext()) {
			OptionData data = iterator.next();
			try {
				data.setCurrentPrice(1d);
				data.setOpen(1d);
				data.setLow(0.5d);
				
				data.setBuyPrice(1d);
				data.setSellPrice(1d);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void executeCommand(String... command) throws Exception {
	  ProcessBuilder builder = new ProcessBuilder(command);
	  builder.redirectOutput(new File("curloutput.txt"));
	  builder.redirectError(new File("curloutputerror.txt"));
	  Process start = builder.start();
	  start.waitFor();
	}
	
	private static String executeCommandV2(String query) {
        try {
        	SSLContext ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, null, null);
            SSLContext.setDefault(ctx);
            URL url = new URL("https://www1.nseindia.com/live_market/dynaContent/live_watch/get_quote/ajaxFOGetQuoteJSON.jsp?"+query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Referer", "https://www.nseindia.com/");
            //conn.setReadTimeout(60000);
            System.out.println(conn);
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP Error code : "
                        + conn.getResponseCode());
            }
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(in);
            String finalRes = "";
            String output;
            while ((output = br.readLine()) != null) {
            	finalRes += output;
            }
            System.out.println(finalRes);
            conn.disconnect();
            return finalRes;
        } catch (Exception e) {
            System.out.println("Exception :- " + e);
        	return "";
        }
    }
}

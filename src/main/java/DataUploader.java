import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataUploader {
	
	public static void insertInDB(String sql) throws SQLException {
		CommonUtil.stmt.executeUpdate(sql);
	}
	
	public static void main(String[] args) throws ParseException, Exception {
		
	}

	public static void uploadOptionData(Date date, boolean loadFromLocal) throws Exception {
		
		try {
			SimpleDateFormat df = new SimpleDateFormat("ddMMMyyyy");
			String folder = CommonUtil.rootDirForOptionCSV + (date.getYear() + 1900);
			String csvFile = folder + "/fo" + df.format(date).toUpperCase() + "bhav.csv";
			String zipFile = csvFile + ".zip";
			
			if(!loadFromLocal) {
				System.out.println("Fetching from web for option data ...");
				Process exec = Runtime.getRuntime().exec("wget https://www.nseindia.com/content/historical/DERIVATIVES/" + new SimpleDateFormat("yyyy").format(date) + "/" + new SimpleDateFormat("MMM").format(date).toUpperCase() + "/fo" + df.format(date).toUpperCase() + "bhav.csv.zip" +" --user-agent='Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36' -P " + folder);
				exec.waitFor();
				exec = Runtime.getRuntime().exec("unzip " + zipFile + " -d " + folder);
			    exec.waitFor();
				exec = Runtime.getRuntime().exec("rm " + zipFile);
				exec.waitFor();
			}
			
			if(!new File(csvFile).exists()) {
				return;
			}
			 
			String sql = "LOAD DATA LOCAL INFILE '" + csvFile + "' " +
							"INTO TABLE Option_History_V2_2019 " +
							"FIELDS TERMINATED BY ',' " +
							"OPTIONALLY ENCLOSED BY '\"' " + 
							"LINES TERMINATED BY '\\n' " + 
							"IGNORE 1 LINES " + 
							"(INSTRUMENT, SYMBOL, EXPIRY_DT, STRIKE_PR, OPTION_TYP, `OPEN`, HIGH, LOW, `CLOSE`, SETTLE_PR, CONTRACTS, VAL_INLAKH, OPEN_INT, CHG_IN_OI) " +
							"SET date = '" + new java.sql.Date(date.getTime()) + "';";
			System.out.println(sql);
			insertInDB(sql);
			
			insertInDB("delete from Option_History_V2_2019 where MONTH(str_to_date(EXPIRY_DT, '%d-%M-%Y')) != MONTH(date) OR INSTRUMENT != 'OPTSTK'");
		} catch (Exception e) {
			System.out.println("Holiday 1" + date);
			e.printStackTrace();
		}
	}
	
	public static void uploadStocksData(Date date, boolean loadFromLocal) throws Exception {
		BufferedReader br = null;
		BufferedReader br1 = null;
		try {
			String folder = CommonUtil.rootDirForStockCSV + (date.getYear() + 1900);
			SimpleDateFormat df = new SimpleDateFormat("ddMM");
			SimpleDateFormat df2 = new SimpleDateFormat("MMM");
			SimpleDateFormat df3 = new SimpleDateFormat("ddMMM");
			SimpleDateFormat df4 = new SimpleDateFormat("MM-dd");
			String csvFile = folder + "/" + df4.format(date).toUpperCase() + ".csv";
			String csvFileRate = folder + "/" + df4.format(date).toUpperCase() + "-Rate.csv";
			String csvFileRateZip = folder + "/" + df4.format(date).toUpperCase() + "-Rate.zip";
			
			if(!loadFromLocal) {
				System.out.println("Fetching from web for stocks data ...");
				Process exec = Runtime.getRuntime().exec("wget https://www.nseindia.com/content/historical/EQUITIES/2019/"+df2.format(date).toUpperCase()+"/cm" + df3.format(date).toUpperCase() + "2019bhav.csv.zip --user-agent='Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36' -O " + csvFileRateZip);
				exec.waitFor();
				exec = Runtime.getRuntime().exec("unzip " + csvFileRateZip + " -d " + folder);
				exec.waitFor();
				exec = Runtime.getRuntime().exec("mv " + folder + "/cm" + df3.format(date).toUpperCase() +"2019bhav.csv " + csvFileRate);
				exec.waitFor();
				exec = Runtime.getRuntime().exec("rm " + csvFileRateZip);
				exec.waitFor();
				exec = Runtime.getRuntime().exec("wget https://www.nseindia.com/archives/equities/mto/MTO_" + df.format(date) + "2019.DAT --user-agent='Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36' -O " + csvFile);
				exec.waitFor();
			}
			
			if(!new File(csvFile).exists() || !new File(csvFileRate).exists()) {
				return;
			}
			
			br = new BufferedReader(new FileReader(csvFile));
			String line = "";
			String cvsSplitBy = ",";
			while ((line = br.readLine()) != null) {
				String[] file = line.split(cvsSplitBy);
				if(file.length < 4) {
					continue;
				}
				if(!file[3].equalsIgnoreCase("EQ")) {
					continue;
				}
				String csvFileDay1RateLine = "";
				br1 = new BufferedReader(new FileReader(csvFileRate));
				while ((csvFileDay1RateLine = br1.readLine()) != null) {
					String[] rateStr = csvFileDay1RateLine.split(cvsSplitBy);
					if(rateStr[0].trim().equalsIgnoreCase(file[2].trim()) && rateStr[1].trim().equalsIgnoreCase(file[3].trim())) {
						String sql = "INSERT IGNORE INTO s_test.STable_2019 (`date`, `open`, high, low, `close`, `last`, name, `type`, total_volume, deliver_volumne, trade_count, del_perct) "
								+ "VALUES ('" + new java.sql.Date((new SimpleDateFormat("dd-MMM-yyyy").parse(rateStr[10].trim())).getTime()) + "', " + Double.parseDouble(rateStr[2]) + ", " + Double.parseDouble(rateStr[3]) 
								+ ", " + Double.parseDouble(rateStr[4]) + ", " + Double.parseDouble(rateStr[5]) 
								+ ", " + Double.parseDouble(rateStr[6]) + ", '" + rateStr[0].trim() + "', '" + rateStr[1].trim() + "',"
								+  Integer.parseInt(rateStr[8]) + " , " + Integer.parseInt(file[5]) + " , " + + Integer.parseInt(rateStr[11]) + " , " + Double.parseDouble(file[6]) + ");";
						insertInDB(sql);
						break;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Holiday 2" + date);
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (br1 != null) {
				try {
					br1.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
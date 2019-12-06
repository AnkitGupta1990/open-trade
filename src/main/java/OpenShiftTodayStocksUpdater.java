import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OpenShiftTodayStocksUpdater extends HttpServlet {

	static Integer year = null;
	static Date latestDBDate = null;
	static List<OptionData> list = new ArrayList<>();
	static String replacement = null;
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			updateOpenShiftStocks(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<h1>" + "OK" + "</h1>");
	}

	public static void updateOpenShiftStocks(String[] args) throws Exception {
		try {
			System.out.println("---------------------------------------");
			System.out.println("Started on " + new Date());
			CommonUtil.openDBConnection();
			getDates();
			latestDBDate = setTimeToMidnight(latestDBDate);
			Date currentDate = setTimeToMidnight(new Date());
			if (latestDBDate.before(currentDate)) {
				Date startDate = CommonUtil.getNextCalendarDate(latestDBDate);
				Date endDate = CommonUtil.getNextCalendarDate(new Date());
				while (startDate.before(endDate)) {
					DataUploader.uploadOptionData(startDate, false);
					DataUploader.uploadStocksData(startDate, false);
					startDate = CommonUtil.getNextCalendarDate(startDate);
				}
			}
			getDates();
			year = latestDBDate.getYear() + 1900;
			list = new ArrayList<>();
			run(latestDBDate);
			updateOpenShiftTodayStocks();
			System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CommonUtil.closeDBConnection();
		}
	}

	public static Date setTimeToMidnight(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	public static void fetchStocksAndType(String sql, Date date) throws Exception {
		ResultSet rs = CommonUtil.stmt.executeQuery(sql);
		while (rs.next()) {
			list.add(new OptionData(rs.getString(1), rs.getString(2)));
		}
		for (OptionData data : list) {
			if (data.getType().equalsIgnoreCase("PE")) {
				fetchPEStrikePrice(data,false);
				fetchCEStrikePrice(data, true);
			} else {
				fetchCEStrikePrice(data,false);
				fetchPEStrikePrice(data, true);
			}
		}
		replacement = "";
		for (OptionData data : list) {
			replacement += "'" + data.getSymbol() + "###" + data.getType() + "###" + String.format("%.2f", data.getStrikePrice()) + "###" + String.format("%.2f", data.getReverseStrikePrice()) + "',";
		}
		replacement = replacement.substring(0, replacement.length() - 1);
		System.out.println(replacement);
	}
	
	public static void updateOpenShiftTodayStocks() throws Exception {
		System.out.println("Committing to Open-Shift");
		String[] command = new String[]{"curl", "http://open-trade-open-trade.apps.ca-central-1.starter.openshift-online.com/UpdateTodayStocks?today_stocks="+URLEncoder.encode(replacement.replace("'", ""), "UTF-8" )};
		executeCommand(command);
		System.out.println("Open-Shift Done");
	}
	
	private static void executeCommand(String... command) throws Exception {
		  ProcessBuilder builder = new ProcessBuilder(command);
		  builder.redirectOutput(new File("curloutputopentrade.txt"));
		  Process start = builder.start();
		  start.waitFor();
	}

	public static void fetchFromDB1(String sql, OptionData data, boolean isReverse) throws Exception {
		ResultSet rs = CommonUtil.stmt.executeQuery(sql);
		while (rs.next()) {
			if(isReverse) {
				data.setReverseStrikePrice(rs.getDouble(1));
			} else {
				data.setStrikePrice(rs.getDouble(1));
			}
		}
	}

	public static void getDates() throws Exception {
		String sql = "select distinct date from STable_2019 order by date desc limit 1";
		ResultSet rs = CommonUtil.stmt.executeQuery(sql);
		rs.next();
		latestDBDate = rs.getDate(1);
	}

	public static void run(Date dt) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(dt);
		String sql = "(select a.SYMBOL,'PE' from ( " + "(select oh.SYMBOL,sum(oh.CHG_IN_OI)/sum(oh.OPEN_INT) as ch "
				+ "from Option_History_V2_" + year + " oh " + "where oh.date = '" + date + "' and oh.OPTION_TYP = 'CE' "
				+ "group by oh.SYMBOL having sum(oh.CHG_IN_OI) > 0  "
				+ "order by sum(oh.CHG_IN_OI)/sum(oh.OPEN_INT) desc) a " + "inner join  "
				+ "(select oh.SYMBOL,sum(oh.CHG_IN_OI)/sum(oh.OPEN_INT) as ch " + "from Option_History_V2_" + year
				+ " oh " + "where oh.date = '" + date + "' and oh.OPTION_TYP = 'PE' "
				+ "group by oh.SYMBOL having sum(CHG_IN_OI) < 0 " + "order by sum(CHG_IN_OI)/sum(OPEN_INT) asc) b "
				+ "on a.SYMBOL = b.SYMBOL) order by (a.ch + (b.ch * -1)) desc limit 10) " + "union "
				+ "(select a.SYMBOL,'CE' from ( " + "(select oh.SYMBOL,sum(oh.CHG_IN_OI)/sum(oh.OPEN_INT) as ch "
				+ "from Option_History_V2_" + year + " oh " + "where oh.date = '" + date + "' and oh.OPTION_TYP = 'PE' "
				+ "group by oh.SYMBOL having sum(oh.CHG_IN_OI) > 0  "
				+ "order by sum(oh.CHG_IN_OI)/sum(oh.OPEN_INT) desc) a " + "inner join  "
				+ "(select oh.SYMBOL,sum(oh.CHG_IN_OI)/sum(oh.OPEN_INT) as ch " + "from Option_History_V2_" + year
				+ " oh " + "where oh.date = '" + date + "' and oh.OPTION_TYP = 'CE' "
				+ "group by oh.SYMBOL having sum(CHG_IN_OI) < 0 " + "order by sum(CHG_IN_OI)/sum(OPEN_INT) asc) b "
				+ "on a.SYMBOL = b.SYMBOL) order by (a.ch + (b.ch * -1)) desc limit 10)";
		// System.out.println(sql);
		fetchStocksAndType(sql, dt);
	}

	public static void fetchPEStrikePrice(OptionData data, boolean isReverse) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(latestDBDate);
		String sql = "select STRIKE_PR from Option_History_V2_" + year + " oh " + "where oh.SYMBOL = '"
				+ data.getSymbol() + "' and oh.`date` = '" + date + "' "
				+ "and oh.STRIKE_PR < (select s.`close` from STable_" + year + " s where s.name = '" + data.getSymbol()
				+ "' and `date` = '" + date + "') " + "order by STRIKE_PR desc limit 1";
		// System.out.println(sql);
		fetchFromDB1(sql, data, isReverse);
	}

	public static void fetchCEStrikePrice(OptionData data, boolean isReverse) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(latestDBDate);
		String sql = "select STRIKE_PR from Option_History_V2_" + year + " oh " + "where oh.SYMBOL = '"
				+ data.getSymbol() + "' and oh.`date` = '" + date + "' "
				+ "and oh.STRIKE_PR > (select s.`close` from STable_" + year + " s where s.name = '" + data.getSymbol()
				+ "' and `date` = '" + date + "') " + "order by STRIKE_PR asc limit 1";
		// System.out.println(sql);
		fetchFromDB1(sql, data, isReverse);
	}
}

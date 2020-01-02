
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StockSelector {

	static Integer year = null;
	static Date latestDBDate = null;
	static List<OptionData> list = new ArrayList<>();
	static String replacement = null;

	public static void main(String[] args) throws Exception {
		try {
			CommonUtil.openDBConnection();
			getDates();
			year = latestDBDate.getYear() + 1900;
			run(latestDBDate);
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
				fetchPEStrikePrice(data);
			} else {
				fetchCEStrikePrice(data);
			}
		}
	}

	public static void fetchFromDB1(String sql, OptionData data) throws Exception {
		ResultSet rs = CommonUtil.stmt.executeQuery(sql);
		while (rs.next()) {
			data.setStrikePrice(rs.getDouble(1));
		}
	}

	public static void getDates() throws Exception {
		String sql = "select distinct date from STable_2020 order by date desc limit 1";
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

	public static void fetchPEStrikePrice(OptionData data) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(latestDBDate);
		String sql = "select STRIKE_PR from Option_History_V2_" + year + " oh " + "where oh.SYMBOL = '"
				+ data.getSymbol() + "' and oh.`date` = '" + date + "' "
				+ "and oh.STRIKE_PR < (select s.`close` from STable_" + year + " s where s.name = '" + data.getSymbol()
				+ "' and `date` = '" + date + "') " + "order by STRIKE_PR desc limit 1";
		// System.out.println(sql);
		fetchFromDB1(sql, data);
	}

	public static void fetchCEStrikePrice(OptionData data) throws Exception {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(latestDBDate);
		String sql = "select STRIKE_PR from Option_History_V2_" + year + " oh " + "where oh.SYMBOL = '"
				+ data.getSymbol() + "' and oh.`date` = '" + date + "' "
				+ "and oh.STRIKE_PR > (select s.`close` from STable_" + year + " s where s.name = '" + data.getSymbol()
				+ "' and `date` = '" + date + "') " + "order by STRIKE_PR asc limit 1";
		// System.out.println(sql);
		fetchFromDB1(sql, data);
	}
}
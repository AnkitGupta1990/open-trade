
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class BGTask implements Runnable {

	static Date date = null;
	static Set<String> buy = new HashSet<>();
	static Set<String> sell = new HashSet<>();

	public void run() {
		//TimeZone.setDefault(TimeZone.getTimeZone("IST"));
		//System.out.println("NOW : " + new Date());
		try {
			while (true) {
				if (!isTradingOn()) {
					TimeUnit.HOURS.sleep(24);
				}
				if (date == null || !isDateEqual(date, new Date())) {
					System.out.println("Fetching Today Stocks.....");
					String todayStocks = getTodayStocks();
					if (todayStocks == null || todayStocks == "") {
						OpenShiftTodayStocksUpdater.updateOpenShiftStocks(null);
						System.out.println("Fetching Today Stocks Again.....");
						todayStocks = getTodayStocks();
						if (todayStocks == null || todayStocks == "") {
							buy.clear();
							sell.clear();
							BGTaskV2.buy.clear();
							BGTaskV2.sell.clear();
							date = new Date();
							System.out.println("No Today Stocks found!!!");
							continue;
						}
					}
					LiveOptionPriceFetcher.list = new ArrayList<OptionData>();
					String[] str = todayStocks.split(",");
					for (String s : str) {
						String[] split = s.split("###");
						OptionData optionData = new OptionData(split[0], split[1]);
						optionData.setStrikePrice(Double.parseDouble(split[2]));
						LiveOptionPriceFetcher.list.add(optionData);
						optionData = new OptionData(split[0], split[1].equalsIgnoreCase("CE") ? "PE" : "CE");
						optionData.setStrikePrice(Double.parseDouble(split[3]));
						optionData.setReverseTrade(true);
						LiveOptionPriceFetcher.list.add(optionData);
					}
					System.out.println("Fetching Today Stocks Done!!!");
					buy.clear();
					sell.clear();
					BGTaskV2.buy.clear();
					BGTaskV2.sell.clear();
					date = new Date();
				}
				if (new Date().getHours() == 9 && new Date().getMinutes() == 00 && !isTradingOn()) {
					buy.clear();
					sell.clear();
					BGTaskV2.buy.clear();
					BGTaskV2.sell.clear();
					TimeUnit.HOURS.sleep(24);
				}
				if (new Date().getHours() == 9 && new Date().getMinutes() == 00 && isTradingOn()) {
					buy.clear();
					sell.clear();
					BGTaskV2.buy.clear();
					BGTaskV2.sell.clear();
					System.out.println("Auto Trading ON....");
				}
				if (new Date().getHours() >= 9 && (new Date().getHours() < 15
						|| (new Date().getHours() == 15 && new Date().getMinutes() <= 35))) {
					LiveOptionPriceFetcher.getLivePrice();
					Iterator<OptionData> iterator = LiveOptionPriceFetcher.list.iterator();
					while (iterator.hasNext()) {
						OptionData data = iterator.next();
						if (data.getOpen() > 0) {
							insert(data, false);
							if (data.getLow() < data.getOpen()
									|| (new Date().getHours() == 15 && new Date().getMinutes() > 28)) {
								if (new Date().getHours() == 15 && new Date().getMinutes() > 28) {
									data.setSell(true);
									insert(data, false);
								} else if (!data.isReverseTrade() && data.getLow() < data.getOpen()
										&& new Date().getHours() >= 11) {
									data.setSell(true);
									insert(data, false);

									OptionData reverseTrade = OptionData.getReverseTrade(data,
											LiveOptionPriceFetcher.list);
									if (reverseTrade.getLow() >= reverseTrade.getOpen()) {
										reverseTrade.setExecuteReverseTrade(true);
										insert(reverseTrade, false);
									}
								} else {
									if (data.getTargetSellPrice().doubleValue() != 0 && data.getTargetSellPrice()
											.doubleValue() <= data.getSellPrice().doubleValue()) {
										data.setSell(true);
										insert(data, false);
									}
								}
							}
						} else if (data.getBuyPrice() > 0) {
							//insert(data, true);
						}
						new BGTaskV2().runV2(data);
					}
				}

				if (new Date().getHours() >= 16 || (new Date().getHours() == 15 && new Date().getMinutes() > 37)) {
					buy.clear();
					sell.clear();
					BGTaskV2.buy.clear();
					BGTaskV2.sell.clear();
					date = null;
					CommonUtil.closeDBConnection();
					System.out.println("sleeping now for " + (31 - new Date().getHours()) + " .....");
					TimeUnit.HOURS.sleep(31 - new Date().getHours());
				}

				TimeUnit.SECONDS.sleep(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getTodayStocks() throws SQLException {
		String sql = "SELECT today_stocks FROM `today_stocks` where DATE(date) = DATE(NOW())";
		System.out.println(sql);
		CommonUtil.openDBConnection();
		ResultSet result = CommonUtil.stmt.executeQuery(sql);
		String string = null;
		if (result.next()) {
			string = result.getString(1);
			System.out.println("DB Result : " + string);
			CommonUtil.closeDBConnection();
		}
		return string;
	}

	public void insert(OptionData data, boolean noOpenBuy) throws SQLException {
		Double buyPrice = data.getOpen();
		if (noOpenBuy) {
			buyPrice = data.getBuyPrice();
		}
		if (data.isReverseTrade()) {
			buyPrice = data.getCurrentPrice();
		}
		if(!data.isSell() && buyPrice <= 0) {
			return;
		}
		if (data.isReverseTrade() && !data.isExecuteReverseTrade()) {
			return;
		}
		if (!data.isSell() && data.getSellPrice() <= (data.getBuyPrice() * 0.5)) {
			// return;
		}
		if ((data.isSell() && sell.add(data.getSymbol() + "###" + data.getType()))
				|| (!data.isSell() && buy.add(data.getSymbol() + "###" + data.getType()))) {
			if (!data.isSell()) {
				if (data.getOpen() > data.getBuyPrice()) {
					// return;
				}
			}
			String sql = "INSERT IGNORE INTO AUTO_TRADES (`date`, SYMBOL, OPTION_TYP, STRIKE_PR, BUY) VALUES(DATE(NOW()), '"
					+ data.getSymbol() + "', '" + data.getType() + "', '" + data.getStrikePrice() + "', '" + buyPrice
					+ "')";
			data.setTargetSellPrice(buyPrice);
			if (data.isSell()) {
				sql = "UPDATE IGNORE AUTO_TRADES SET SELL = " + data.getSellPrice()
						+ " WHERE DATE(`date`) = DATE(NOW()) AND SYMBOL = '" + data.getSymbol() + "' AND OPTION_TYP = '"
						+ data.getType() + "' AND STRIKE_PR = '" + data.getStrikePrice() + "'";
			}
			//System.out.println(sql);
			CommonUtil.openDBConnection();
			CommonUtil.stmt.executeUpdate(sql);
			CommonUtil.closeDBConnection();
			
			try {
				boolean flag = buy.contains(data.getSymbol() + "###" + data.getType());
				if(flag) {
					String message = (data.isSell() ? "Sell " : "Buy ") + data.getSymbol() + " " + data.getType() + " "
							+ data.getStrikePrice() + " at " + (data.isSell() ? data.getSellPrice() : buyPrice);
					String[] command = new String[] { "curl",
							"https://api.telegram.org/bot834944814:AAFb8KRmfQHLsVqBxqr3OH3BIf8TovItdlM/sendMessage", "-d",
							"chat_id=771084079&text=" + message };
					executeCommand(command);
				}
			} catch (Exception e) {
				System.out.println("Telegram Error : " + e);
			}

		}
	}

	public boolean isTradingOn() throws SQLException {
		if (new SimpleDateFormat("EEEE").format(new Date()).equalsIgnoreCase("Saturday")
				|| new SimpleDateFormat("EEEE").format(new Date()).equalsIgnoreCase("Sunday")) {
			return false;
		}

		String sql = "SELECT count(*) FROM `holiday_dates` where DATE(date) = DATE(NOW())";
		//System.out.println(sql);
		CommonUtil.openDBConnection();
		ResultSet result = CommonUtil.stmt.executeQuery(sql);
		result.next();
		int int1 = result.getInt(1);
		CommonUtil.closeDBConnection();
		return int1 <= 0;
	}

	public boolean isDateEqual(Date date1, Date date2) {
		if (date1.getYear() != date2.getYear()) {
			return false;
		} else if (date1.getMonth() != date2.getMonth()) {
			return false;
		} else if (date1.getDate() != date2.getDate()) {
			return false;
		}
		return true;
	}

	private static void executeCommand(String... command) {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectOutput(new File("curloutput11.txt"));
		Process start;
		try {
			start = builder.start();
			start.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

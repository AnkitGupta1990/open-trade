
import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class BGTaskV2 {
	
	static Set<String> buy = new HashSet<>();
	static Set<String> sell = new HashSet<>();
	
	public void runV2(OptionData data) {
		try {
			if ((new Date().getHours() > 9 || new Date().getHours() == 9 && new Date().getMinutes() >= 30) && 
					(new Date().getHours() < 15 || (new Date().getHours() == 15 && new Date().getMinutes() <= 35))) {
				if (data.getOpen() > 0) {
					data.setSell(true);
					insert(data);
					if (new Date().getHours() == 15 && new Date().getMinutes() > 28) {
						data.setSell(false);
						insert(data);
					} else if (data.getLow() >= data.getOpen() && new Date().getHours() >= 11) {
						data.setSell(false);
						insert(data);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insert(OptionData data) throws SQLException {
		if (data.isReverseTrade()) {
			return;
		}
		if ((data.isSell() && sell.add(data.getSymbol() + "###" + data.getType()))
				|| (!data.isSell() && buy.add(data.getSymbol() + "###" + data.getType()))) {
			String sql = "INSERT IGNORE INTO AUTO_TRADES_V2 (`date`, SYMBOL, OPTION_TYP, STRIKE_PR, SELL) VALUES(DATE(NOW()), '"
					+ data.getSymbol() + "', '" + data.getType() + "', '" + data.getStrikePrice() + "', '" + data.getSellPrice()
					+ "')";
			if (!data.isSell()) {
				sql = "UPDATE IGNORE AUTO_TRADES_V2 SET BUY = " + data.getBuyPrice()
						+ " WHERE DATE(`date`) = DATE(NOW()) AND SYMBOL = '" + data.getSymbol() + "' AND OPTION_TYP = '"
						+ data.getType() + "' AND STRIKE_PR = '" + data.getStrikePrice() + "'";
			}
			CommonUtil.openDBConnection();
			CommonUtil.stmt.executeUpdate(sql);
			CommonUtil.closeDBConnection();
			
			try {
				String message = (data.isSell() ? "V2 : Sell " : "V2 : Buy ") + data.getSymbol() + " " + data.getType() + " "
						+ data.getStrikePrice() + " at " + (data.isSell() ? data.getSellPrice() : data.getBuyPrice());
				String[] command = new String[] { "curl",
						"https://api.telegram.org/bot834944814:AAFb8KRmfQHLsVqBxqr3OH3BIf8TovItdlM/sendMessage", "-d",
						"chat_id=771084079&text=" + message };
				executeCommand(command);
			} catch (Exception e) {
				System.out.println("Telegram Error : " + e);
			}

		}
	}
	
	private static void executeCommand(String... command) {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectOutput(new File("curloutputv2.txt"));
		Process start;
		try {
			start = builder.start();
			start.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

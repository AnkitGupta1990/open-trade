
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

public class CommonUtil {

	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://sql12.freemysqlhosting.net:3306/sql12303442?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&allowLoadLocalInfile=true";
	//static final String DB_URL = "jdbc:mysql://localhost:3306/s_test?useSSL=false";
	static final String password = "aptyqRXkmX";
	//static final String password = "root";
	static Connection conn = null;
	static Statement stmt = null;
	
	static String rootDirForStockCSV = "StockData/";
	static String rootDirForOptionCSV = "OptionData/";

	public static void openDBConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(DB_URL, "sql12303442", password);
			stmt = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void closeDBConnection() {
		try {
			if (stmt != null)
				conn.close();
		} catch (SQLException se) {

		}
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	public static Date getNextCalendarDate(Date date) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		return calendar.getTime();
	}
}

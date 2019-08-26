
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetTodayStocks extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		dumpData(GetTodayStocks(), out);
		out.println("<br>");
		out.println("Profit/Loss : " + GetTodayStocksPerformance());
	}

	public ResultSet GetTodayStocks() {
		String sql = "SELECT *,(sell-buy)*lot_size FROM AUTO_TRADES inner join stocks_lot_size on SYMBOL = stock WHERE DATE(date) =  DATE(NOW())";
		System.out.println(sql);
		ResultSet result = null;
		CommonUtil.openDBConnection();
		try {
			result = CommonUtil.stmt.executeQuery(sql);
		} catch (SQLException e) {
			System.out.println(e.getLocalizedMessage());
		}
		return result;
	}

	public String GetTodayStocksPerformance() {
		String sql = "select sum((sell-buy)*lot_size) from AUTO_TRADES inner join stocks_lot_size on SYMBOL = stock WHERE DATE(date) =  DATE(NOW()) group by DATE(date)";
		System.out.println(sql);
		String str = null;
		CommonUtil.openDBConnection();
		try {
			ResultSet result = CommonUtil.stmt.executeQuery(sql);
			result.next();
			str = result.getString(1);
		} catch (SQLException e) {
			System.out.println(e.getLocalizedMessage());
		}
		CommonUtil.closeDBConnection();
		return str;
	}

	private int dumpData(java.sql.ResultSet rs, java.io.PrintWriter out) {
		int rowCount = 0;
		try {
			out.println("<P ALIGN='center'><TABLE BORDER=1>");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			out.println("<TR>");
			for (int i = 0; i < columnCount; i++) {
				out.println("<TH>" + rsmd.getColumnLabel(i + 1) + "</TH>");
			}
			out.println("</TR>");
			while (rs.next()) {
				rowCount++;
				out.println("<TR>");
				for (int i = 0; i < columnCount; i++) {
					out.println("<TD>" + rs.getString(i + 1) + "</TD>");
				}
				out.println("</TR>");
			}
			out.println("</TABLE></P>");
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
		}
		CommonUtil.closeDBConnection();
		return rowCount;
	}

	public void destroy() {

	}
}
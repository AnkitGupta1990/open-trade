
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UpdateTodayStocks extends HttpServlet {

   public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
      
	  insert(request.getParameter("today_stocks"));
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println("<h1>" + "OK" + "</h1>");
   }
   
   public void insert(String today_stocks) {
		String sql = "INSERT INTO today_stocks (today_stocks, `date`) VALUES('"+today_stocks+"', DATE(NOW()))";
		System.out.println(sql);
		CommonUtil.openDBConnection();
		try {
			CommonUtil.stmt.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println(e.getLocalizedMessage());
			sql = "UPDATE today_stocks set today_stocks = '"+today_stocks+"' WHERE DATE(date) =  DATE(NOW())";
			System.out.println(sql);
			try {
				CommonUtil.stmt.executeUpdate(sql);
			} catch (SQLException e1) {
				System.out.println(e.getLocalizedMessage());
			}
		}
		CommonUtil.closeDBConnection();
	}

   public void destroy() {

   }
}


import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class BackgroudTask implements ServletContextListener {

    private ExecutorService executor;

    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newSingleThreadExecutor();
        TimeZone.setDefault(TimeZone.getTimeZone("IST"));
		System.out.println("thread started....");
        executor.submit(new BGTask());
    }

    public void contextDestroyed(ServletContextEvent event) {
    	executor.shutdown();
    	System.out.println("thread stopped!!!");
    }

}
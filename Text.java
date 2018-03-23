package com.trs.init.application;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Text  implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub
		
	}

	public void contextInitialized(ServletContextEvent sce) {
		VisitorThread visitorThread=new VisitorThread();
		visitorThread.run();
		
		
	}
	
	
}

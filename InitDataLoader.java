package com.trs.init.application;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.trs.init.dao.InitSysInfoDao;

/**
 * @author tan.hongyan
 * @version: Dec 25, 2009 10:11:22 AM
 * @desc: 
 */
public class InitDataLoader implements ServletContextListener {

	//定义一个HashMap
	public static HashMap initSysInfo = new HashMap();
	//定义一个HashMap(英文)
	public static HashMap initSysInfo_en=new HashMap();
	
	
	public void contextDestroyed(ServletContextEvent sce) {
		initSysInfo = null;
		initSysInfo_en=null;
	}
	
	

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext ctx = sce.getServletContext();
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(ctx);
		InitSysInfoDao initSysInfoDao = (InitSysInfoDao)wac.getBean("initSysInfoDao");
		
		System.out.println("---------------初始化系统数据>>>");
		System.out.println("sql ---> SELECT c_fundcode init_1, c_fundname init_2, '' init_3, '' init_4, '1' type FROM TFUNDINFO " +
				"UNION SELECT c_businflag, c_businname, c_type, '', '2' FROM TBUSINFLAG " +
				"UNION SELECT c_agencyno, c_agencyname, '', '', '3' FROM TAGENCYINFO " +
				"UNION SELECT c_sysname, c_caption, c_keyvalue, to_char(l_keyno), '4' FROM TDICTIONARY WHERE UPPER(c_sysname)='TA' ");
		System.out.println("sql ---> SELECT c_fundcode init_1, c_fundname init_2, '' init_3, '' init_4, '1' type FROM TFUNDINFO " +
				"UNION SELECT c_businflag, c_businname, c_type, '', '2' FROM TBUSINFLAG " +
				"UNION SELECT c_agencyno, c_agencyname, '', '', '3' FROM TAGENCYINFO " +
				"UNION SELECT c_sysname, c_english, c_keyvalue, to_char(l_keyno), '4' FROM TDICTIONARY WHERE UPPER(c_sysname)='EQUERY' ");
		initSysInfo = initSysInfoDao.getInitSysInfo();
		initSysInfo_en=initSysInfoDao.getInitSysInfo_en();//获取英文版的HashMap
		System.out.println("---------------系统数据加载完毕>>>");
		sce.getServletContext().setAttribute("initSysInfo", initSysInfo);
		sce.getServletContext().setAttribute("initSysInfo_en", initSysInfo_en);
		System.out.println("---------------初始化系统数据结束>>>");
	}

}

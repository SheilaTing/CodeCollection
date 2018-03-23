package com.ssh.util;
/**
 * @author Tan hongyan E-mail:tan.hongyan@trs.com.cn
 * @version 创建时间：Feb 22, 2009 3:54:34 PM
 * 类说明
 */
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

public class ExportDB {

	public static void main(String[] args) {
		//读取hibernate.cfg.xml文件
		Configuration cfg = new Configuration().configure();
		
		SchemaExport export = new SchemaExport(cfg);
		
		export.create(true, true);
	}

}

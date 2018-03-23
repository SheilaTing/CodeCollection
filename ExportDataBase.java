package com.ssh.util;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * 作用：通过POJO和Object.xml转化为数据库ddl 
 */
public class ExportDataBase {

	public static void main(String[] args) {
		// 读取hibernate.cfg.xml
		Configuration cfg = new Configuration().configure();
		SchemaExport export = new SchemaExport(cfg);
		export.create(true, true);
	}

}


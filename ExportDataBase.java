package com.ssh.util;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * ���ã�ͨ��POJO��Object.xmlת��Ϊ���ݿ�ddl 
 */
public class ExportDataBase {

	public static void main(String[] args) {
		// ��ȡhibernate.cfg.xml
		Configuration cfg = new Configuration().configure();
		SchemaExport export = new SchemaExport(cfg);
		export.create(true, true);
	}

}


package com.trs.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

public class FormatDateTool {

	public static String formatDateToString(Date date) {

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = simpleDateFormat.format(date);
		return dateString;
	}

	public static String stringFormat(String str) {
		String vals = "";
		if (str != null && !str.equals("")) {
			String[] strs = null;

			if (str.lastIndexOf(",") != -1) {
				strs = str.split(",");

				if (strs != null) {
					for (int i = 0; i < strs.length; i++) {
						vals += "'" + strs[i] + "'";
						if (i < strs.length - 1) {
							vals += ",";
						}
					}
				}
			} else {
				vals = "'" + str + "'";
			}
		}
		return vals;
	}
	
	public static String stringFormat1(String str) {
		String vals = "";
		if (str != null && !str.equals("")) {
			String[] strs = null;

			if (str.lastIndexOf(",") != -1) {
				strs = str.split(",");

				if (strs != null) {
					for (int i = 0; i < strs.length; i++) {
						vals += strs[i].substring(1, strs[i].length() - 1);
						if (i < strs.length - 1) {
							vals += ",";
						}
					}
				}
			} else {
				vals = str.substring(1,str.length() - 1);
			}
		}
		return vals;
	}
	
	public static String accountCheck(String str, String lang) {
		String accounts = "";
		String fundAcco = "";
		String gdAcco = "";
		String zdAcco = "";

		if (str.indexOf(",") != -1) {
			String[] accoArray = str.split(",");
			
			if (accoArray != null && accoArray.length > 1) {
				
				for (int i = 0; i < accoArray.length; i++) {
					String temp = accoArray[i].substring(0, 2);
					
					if (temp.equals("37")) {
						fundAcco +=  accoArray[i] + ",";
						
					} else if (temp.indexOf("A") != -1) {
						
						gdAcco += accoArray[i] + ",";
						
					} else {
						zdAcco +=  accoArray[i] + ",";
					}
					
				}
				if(lang.equalsIgnoreCase("cn")){
					if(fundAcco != ""){
						accounts += "基金账号: " + fundAcco.substring(0,fundAcco.length() - 1) + "/";
					}
					
					if(gdAcco != ""){
						accounts += "股东账号: " + gdAcco.substring(0,gdAcco.length() - 1) + "/";
					}
					
					if(zdAcco != ""){
						accounts += "中登账号: " +zdAcco.substring(0,zdAcco.length() - 1) + "/";
					}
				} else {
					if(fundAcco != "" || zdAcco != ""){
//						accounts += "Fund Acco Number: " + fundAcco.substring(0,fundAcco.length() - 1) + "/";
						if(!zdAcco.equals("") && zdAcco.trim().length() > 0){
							zdAcco = zdAcco.substring(0,zdAcco.length() - 1);
						} else {
							fundAcco = fundAcco.substring(0,fundAcco.length() - 1);
						}
						
						accounts += "Fund Acco Number: " + fundAcco.trim() + zdAcco.trim() + "/";
					}
					
					if(gdAcco != ""){
						accounts += "Sharehloders'Acco Number: " + gdAcco.substring(0,gdAcco.length() - 1) + "/";
					}
					
//					if(zdAcco != ""){
//						accounts += "ZhongDeng Acco Number: " +zdAcco.substring(0,zdAcco.length() - 1) + "/";
//					}
					
				}
			}
			accounts = accounts.substring(0,accounts.length() - 1);
		} else {
			String temp = str.substring(0, 2);
			if(lang.equalsIgnoreCase("cn")){
				if (temp.equals("37")) {
					accounts = "基金账号: " +str;
				} else if (temp.indexOf("A") != -1) {
					accounts = "股东账号: " + str;
				} else {
					accounts = "中登账号: " + str;
				}
			} else {
				if (temp.equals("37") || temp.equals("98") || temp.equals("99")) {
					accounts = "Fund Acco Number: " +str;
				} else if (temp.indexOf("A") != -1) {
					accounts = "Sharehloders'Acco Number: " + str;
				} 
//				else {
//					accounts = "ZhongDeng Acco Number: " + str;
//				}
			}
		}
		
		String accoShow = "";//基金账号:371800003811/股东账号:A6000001606
		if(accounts.indexOf("/") != -1){
			String[] strArr = accounts.split("/");
			
			for(int i = 0; i < strArr.length; i++){
				String tmp = strArr[i];
				if(lang.equalsIgnoreCase("cn")) {
					if(tmp.indexOf("基金账号") != -1 || tmp.indexOf("股东账号") != -1 || tmp.indexOf("中登账号") != -1){
						accoShow += tmp + "\n";
					}
				} else {
					if(tmp.indexOf("Fund Acco Number:") != -1 || tmp.indexOf("Sharehloders'Acco Number:") != -1 || tmp.indexOf("ZhongDeng Acco Number:") != -1){
						accoShow += tmp + "\n";
					}
				}
				
			}
			accounts = accoShow;
		}
		
		return accounts;
	}
}

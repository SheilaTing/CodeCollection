package com.ssh.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Tan hongyan E-mail:tan.hongyan@trs.com.cn
 * @version 创建时间：Jun 16, 2009 12:31:30 PM
 * 类说明	返回N天以前的时间
 */
public class DateTools {
	
	//昨天
	public static Date getYestoday(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(calendar.DATE, -1);
		date = calendar.getTime();
		return date;
	}
	
	//一周前
	public static Date getLately7(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(calendar.DATE, -7);
		date = calendar.getTime();
		return date;
	}
	
	//一个月前
	public static Date getLately30(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(calendar.MONTH, -1);
		date = calendar.getTime();
		return date;
	}
	
	//三个月前
	public static Date getLately90(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(calendar.MONTH, -3);
		date = calendar.getTime();
		return date;
	}
	
	//半年
	public static Date getLately180(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(calendar.MONTH, -6);
		date = calendar.getTime();
		return date;
	}
	
	//一年
	public static Date getLately360(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(calendar.YEAR, -1);
		date = calendar.getTime();
		return date;
	}
	
	//年初日期
	public static Date getLatelyStart(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String strDate = sdf.format(date);
		strDate = strDate.substring(0, 4) + "-01-01";
		Date retDate = new Date();
		try {
			retDate = sdf.parse(strDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return retDate;
	}
	
	//date转string
	public static String getDateString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String strDate = sdf.format(date);
		return strDate;
	}
	
	//date转string 精确到分
	public static String getDateStringMin(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String strDate = sdf.format(date);
		return strDate;
	}
	
	//string转date
	public static Date getString2Date(String strDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date = sdf.parse(strDate);
		} catch (Exception e) {
		}
		return date;
	}
	
	//日期格式
	public static String dateStrEight2Ten(String datestr)
    {
        return datestr.substring(0, 4) + "-" + datestr.substring(4, 6) + "-" + datestr.substring(6);
    }
	
	public static void main(String[] args) {
		System.out.println(dateStrEight2Ten("20090330"));
	}
}

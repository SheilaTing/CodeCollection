/**
 * 存储一些公共函数的集合
 */
package com.ssh.util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PublicMethodUtil {
	
	
	/**
	 * 返回指定精度的String值
	 */
	public static  String getNumberScaled(int nScale,Object obj) {
		BigDecimal bd=null;
		try {
			 bd=new BigDecimal(obj.toString());			
		}catch(Exception e) {
			bd=new BigDecimal(0.00);	
		}
		return bd.setScale(nScale,BigDecimal.ROUND_HALF_UP).toString();
	}
	
	/**
	 * 返回指定精度的时间值
	 * @param formatType
	 * @param calendar
	 * @return
	 */
	public static String getFormatDate(String formatType,Calendar calendar) {
		if(formatType==null || formatType.trim().length()==0){
			formatType="yyyy-MM-dd";
		}
		SimpleDateFormat sdf=new SimpleDateFormat(formatType);
		
		return sdf.format(calendar.getTime());
	}
	
	/**
	 * 返回指定精度的时间值
	 * @param formatType
	 * @param calendar
	 * @return
	 */
	public static String getFormatDate(String formatType,Date date) {
		if(date==null) {
			return "";
		}
		if(formatType==null || formatType.trim().length()==0){
			formatType="yyyy-MM-dd";
		}
		SimpleDateFormat sdf=new SimpleDateFormat(formatType);
		
		return sdf.format(date);
	}
	
	/**
	 * 
	 * @param formatType
	 * @param calendarTime
	 * @return
	 */
	public static String getFormateDate(String formatType,String calendarTime) {
		if(formatType==null || formatType.trim().length()==0){
			formatType="yyyy-MM-dd";
		}
		SimpleDateFormat sdf=new SimpleDateFormat(formatType);
		DateFormat df=DateFormat.getInstance();
		Date date=null;
		try {
			date=df.parse(calendarTime);
		}catch(Exception e){
		
		}
		 
		return sdf.format(date);
	}
	/**
	 * 获取字符串的长度
	 * @param str
	 * @return
	 */
	public static int getStringLength( String str ) {
		if(str==null) {
			return 0;
		}
		str=str.trim();
		byte[] bytes=str.getBytes();
		Byte bObject;
		int nLength=0;
		for(int i=0;i<bytes.length;i++) {
			bObject=new Byte(bytes[i]);
			if(i>=0&&i<=255) {
				nLength++;
			}else {
				nLength+=2;
			}
		}
		return nLength;
	}
}

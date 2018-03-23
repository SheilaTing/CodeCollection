package com.trs.util;
/**
 * @author tan.hongyan
 * @version: Apr 15, 2010 1:49:41 PM
 * @desc: 参数合法性验证 
 */
public class Validate {

	/**
	 * 十位日期字符串合法性验证：2010-04-15
	 * @param date
	 * @return false:不合法的日期字符串；true:合法的日期字符串
	 */
	public static boolean date10Validate(String _date) throws Exception {

		if(_date == null || "".equals(_date)) {
			return false;
		}
		
		if(_date.length() != 10) {
			return false;
		}
		
		String year = _date.substring(0, 4);
		int intYear = Integer.parseInt(year);
		
		if(intYear < 1000 || intYear > 9999) {
			return false;
		}
		
		String rod1 = _date.substring(4, 5);
		String rod2 = _date.substring(7, 8);
		
		if(!"-".equals(rod1) || !"-".equals(rod2)) {
			return false;
		}
		
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		
		Validate validate = new Validate();
		boolean b = validate.date10Validate("2010-04-11");
		System.out.println(b);
	}
}

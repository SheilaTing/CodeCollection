/**
 * 
 */
package com.ssh.util;

import com.ssh.util.SafeCheck;

/**
 * @author Administrator
 *
 */
public class CheckData {
	

	
	public static  boolean isMobile(String s) {
		if(s==null||s.equals("")) return false;
		return true;
	}

	
	public static boolean isEmail(String s){
		if(s==null || s.equals("")) {return true;}
		int len = s.length();
		if(len <5){ return false;}
		int t = s.lastIndexOf("@");
		if(t<1 || t>(len-3)){ return false;}
		int r = s.lastIndexOf(".");
		if(r<3 || r>(len-2)) { return false;}
		if((r-t)<2) {return false;}
		return true;
	}
	
	
	public static String k(String s) {
		return checkPara(s,0);
	}
	
	public static String k(Object s) {
		return checkPara(s,0);
	}
	
	public static  String checkPara(Object s ,int type) {
    	if(s==null) return "";
    	String ret ="";
    	if(s instanceof String){
    		ret = ((String)s).trim();
    		switch(type) {
    			case 0: {break;}
    			case 1: {return SafeCheck.getSafeStr(ret);}
    			case 2: {if(!ret.matches("\\d*")) ret=""; break;} //[0-9]
    			case 3: {if(!ret.matches("\\w*")) ret=""; break;} //[a-zA-Z_0-9]   
     			default:{ return ret;}
    		}	
    	}
    	return ret;
    }
	
	/**
	 * 验证脚本
	 * @param s
	 * @return
	 */
	public static boolean checkScript(String s){
		 if(s==null || s.equals("")) return true;
		 if(s.indexOf(">")>=0) return false;
		 if(s.indexOf("<")>=0) return false;
		 if(s.indexOf("\\")>=0) return false;
		 if(s.indexOf(";")>=0) return false;
		 if(s.indexOf("#")>=0) return false;
		 if(s.indexOf("$")>=0) return false;
		 if(s.indexOf((char)34)>0) return false;
		 if(s.indexOf((char)39)>0) return false;
		 return true;
	} 
	
	public static String c(Object s){
		if(s==null) return "";
    	String ret ="";
    	if(s instanceof String){
    		ret = (String)s;
    		if(checkScript(ret)) return ret;
    	}
    	return "";
	}
	
	public static void main(String[] ss) {
		/*System.out.println(k("<script></script>"));
		System.out.println(k("<script></script>"));
		System.out.println(k("<(&"));
		System.out.println(k("-"));
		System.out.println("23424".matches("\\d*"));
		System.out.println(CheckData.checkPara("21 3424", 2));
		System.out.println(CheckData.checkPara("2@424", 2));
		System.out.println(CheckData.checkPara("21qwerQVAZaz3424", 2));
		System.out.println(CheckData.c("ssss"));*/
		/*for(int i=0;i<10;i++){
			if(i==5)continue;
			System.out.print(i);
		}*/
		String a="abcdjavajavaabdjavaabcdjavacabd";
		System.out.println(a.substring(8));
		String c="java";
		int index=-1;
		int count=0;
		while((index=a.indexOf(c))!=-1){
			System.out.println(index+c.length());
			a=a.substring(index+c.length());
			System.out.println(a);
			count++;
		}
		System.out.println(count);
	   }
	 
	
}

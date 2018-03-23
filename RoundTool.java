package com.ssh.util;

import java.math.BigDecimal;

/**
 * @author Tan hongyan E-mail:tan.hongyan@trs.com.cn
 * @version 创建时间：Jun 2, 2009 6:25:04 PM
 * 类说明
 */
public class RoundTool {
	
	public static double roundDouble(double value, int scale, int roundingMode) {   
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }
	
	public static float roundFloat(double value, int scale, int roundingMode) {
		BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        float f = bd.floatValue();
        bd = null;
        return f;
	}
	
	public static String roundFiltRate(double value, int scale, int roundingMode) {
		BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        float f = bd.floatValue();
        bd = null;
        if(f<-10000) {
        	return "-";
        }
        else {
        	String filtResult = String.valueOf(f);
        	int point = filtResult.indexOf(".") + 1;
        	int zero = filtResult.substring(point, filtResult.length()).length();
        	if(zero < scale) {
        		for(int i=0; i<scale-zero; i++) {
        			filtResult += "0";
        		}
        	}
        	return filtResult + "%";
        }
	}
	
	public static String roundFiltNav(double value, int scale, int roundingMode) {
		BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        float f = bd.floatValue();
        bd = null;
        if(f<-100) {
        	return "-";
        }
        else {
        	String filtResult = String.valueOf(f);
        	int point = filtResult.indexOf(".") + 1;
        	int zero = filtResult.substring(point, filtResult.length()).length();
        	if(zero < scale) {
        		for(int i=0; i<scale-zero; i++) {
        			filtResult += "0";
        		}
        	}
        	return filtResult;
        }
	}
	
	//目标试算
	public static String roundCalculate(double value, int scale, int roundingMode) {
		BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        float f = bd.floatValue();
        bd = null;
    	String filtResult = String.valueOf(f);
    	int point = filtResult.indexOf(".") + 1;
    	int zero = filtResult.substring(point, filtResult.length()).length();
    	if(zero < scale) {
    		for(int i=0; i<scale-zero; i++) {
    			filtResult += "0";
    		}
    	}
    	return filtResult + "%";
	}
	
	//我的账本--四舍五入后存库
	public static double roundDouble(double value, int scale) {
		int roundingMode = BigDecimal.ROUND_HALF_UP;
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }
	//同上，参数类型不同
	public static double roundDouble(Float value, int scale) {
		int roundingMode = BigDecimal.ROUND_HALF_UP;
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }
	//同上，参数类型不同
	public static double roundDouble(Double value, int scale) {
		int roundingMode = BigDecimal.ROUND_HALF_UP;
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }
	/**  
     * 测试用的main方法.  
     *   
     * @param argc  
     *            运行参数.  
     */ 
	
	public static void main(String[] argc) {   
        //下面都以保留2位小数为例   
           
        //ROUND_UP   
        //只要第2位后面存在大于0的小数，则第2位就+1   
        System.out.println(roundDouble(12.3401,2,BigDecimal.ROUND_UP));//12.35   
        System.out.println(roundDouble(-12.3401,2,BigDecimal.ROUND_UP));//-12.35   
        //ROUND_DOWN   
        //与ROUND_UP相反   
        //直接舍弃第2位后面的所有小数   
        System.out.println(roundDouble(12.349,2,BigDecimal.ROUND_DOWN));//12.34   
        System.out.println(roundDouble(-12.349,2,BigDecimal.ROUND_DOWN));//-12.34   
        //ROUND_CEILING   
        //如果数字>0 则和ROUND_UP作用一样   
        //如果数字<0 则和ROUND_DOWN作用一样   
        System.out.println(roundDouble(12.3401,2,BigDecimal.ROUND_CEILING));//12.35   
        System.out.println(roundDouble(-12.349,2,BigDecimal.ROUND_CEILING));//-12.34   
        //ROUND_FLOOR   
        //如果数字>0 则和ROUND_DOWN作用一样   
        //如果数字<0 则和ROUND_UP作用一样   
        System.out.println(roundDouble(12.349,2,BigDecimal.ROUND_FLOOR));//12.34   
        System.out.println(roundDouble(-12.3401,2,BigDecimal.ROUND_FLOOR));//-12.35   
        //ROUND_HALF_UP [这种方法最常用]   
        //如果第3位数字>=5,则第2位数字+1   
        //备注:只看第3位数字的值,不会考虑第3位之后的小数的   
        System.out.println(roundDouble(12.345,2,BigDecimal.ROUND_HALF_UP));//12.35   
        System.out.println(roundDouble(12.3449,2,BigDecimal.ROUND_HALF_UP));//12.34   
        System.out.println(roundDouble(-12.345,2,BigDecimal.ROUND_HALF_UP));//-12.35   
        System.out.println(roundDouble(-12.3449,2,BigDecimal.ROUND_HALF_UP));//-12.34   
        //ROUND_HALF_DOWN   
        //如果第3位数字>=5,则做ROUND_UP   
        //如果第3位数字<5,则做ROUND_DOWN   
        System.out.println(roundDouble(12.345,2,BigDecimal.ROUND_HALF_DOWN));//12.35   
        System.out.println(roundDouble(12.3449,2,BigDecimal.ROUND_HALF_DOWN));//12.34   
        System.out.println(roundDouble(-12.345,2,BigDecimal.ROUND_HALF_DOWN));//-12.35   
        System.out.println(roundDouble(-12.3449,2,BigDecimal.ROUND_HALF_DOWN));//-12.34   
        //ROUND_HALF_EVEN   
        //如果第3位是偶数,则做ROUND_HALF_DOWN   
        //如果第3位是奇数,则做ROUND_HALF_UP   
        System.out.println(roundDouble(12.346,2,BigDecimal.ROUND_HALF_EVEN));//12.35   
        System.out.println(roundDouble(12.345,2,BigDecimal.ROUND_HALF_EVEN));//12.35   
    }   
}

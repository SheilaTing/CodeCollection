package com.ssh.util;

import java.util.HashMap;

public class StringTool {

		
		public static String splitStr = "[$]";
		
		public static HashMap getParam(String para){
			HashMap hm = new HashMap();
			para = para == null?"":para.trim();
			if(para.equals("")) return hm;
			String temp ="";
			int idx =para.indexOf(splitStr);
			while(idx>=0){
				temp = idx>=0? para.substring(0,idx):para;
				para = para.substring(idx+3);
				if(!temp.trim().equals("")){
					String[] s = temp.split("="); 
		        	if(s.length ==2){
		        		hm.put(new String(s[0]),new String(s[1]));		        		
		        	}
				}
				if(idx<0) break;
				idx = para.indexOf(splitStr);
			}
			if(idx<0 && para.length()>0){
				if(!para.equals("")){
					String[] s = para.split("="); 
		        	if(s.length ==2){
		        		hm.put(new String(s[0]),new String(s[1]));		        		
		        	}
				}
			}
	        return hm;
		}
		
		public static String getPara(String org,String name){
			if(name==null || name.equals("")) return "";
			HashMap hm = getParam(org);
			return (String)hm.get(name);
		}
		
		public static void main(String[] args) {
			StringTool.getParam("[&]active=true[&]asf=eee[&]");

		}

	}


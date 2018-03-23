package com.trs.init.application;

import java.util.Date;

public class VisitorThread extends Thread { 
	public String s="demo";
	public void run() {
	while (true) { 
	try { 
	sleep(3000); 
	  try { 
		System.out.println(new Date());
		System.out.println(s);
	   }catch (Exception e) { 
	    e.printStackTrace(); 
	    } 
	} catch (InterruptedException e1) { 
	// TODO Auto-generated catch block 
	e1.printStackTrace(); 
	}

	}
	}
	public static void main(String[] args) {
		VisitorThread VisitorThread=new VisitorThread();
		VisitorThread.run();
		
	}

}

	

package com.trs.util;

import java.util.List;

import com.trs.init.beans.InitAgencyInfo;
import com.trs.init.beans.InitBusinFlag;
import com.trs.init.beans.InitDictionary;
import com.trs.init.beans.InitFundInfo;
import com.trs.init.beans.InitStatus;

/**
 * 
 * @author thy
 * 通过字典转换，一般是代号转成中文名字
 */
public class DictionaryChange {

	//基金代码转基金名称
	public static String changeFundInfo(List initFundInfo, String fundCode) {
		if(initFundInfo==null) return "--";
		for(int i=0; i<initFundInfo.size(); i++) {
			InitFundInfo fundInfo = (InitFundInfo)initFundInfo.get(i);
			if(fundInfo.getFundCode().trim().equals(fundCode)) {
				return fundInfo.getFundName();
			}
		}
		return "--";
	}
	//基金代码转基金名称(英文版)
	public static String changeFundInfo_en(List initFundInfo, String fundCode) {
		if(initFundInfo==null) return fundCode;
		for(int i=0; i<initFundInfo.size(); i++) {
			InitFundInfo fundInfo = (InitFundInfo)initFundInfo.get(i);
			if(fundInfo.getFundCode().trim().equals(fundCode)) {
				return fundInfo.getFundName();
			}
		}
		return fundCode;
	}
	//查询基金状态
	public static String changeStatus(List initStatus, String initSta) {
		if(initStatus==null) return "--";
		for(int i=0; i<initStatus.size(); i++) {
			InitStatus initStatu = (InitStatus)initStatus.get(i);
			if(initStatu.getInitNo().trim().equals(initSta)) {
				return initStatu.getInitStatu();
			}
		}
		return "--";
	}
	
	//查询基金状态(英文版)
	public static String changeStatus_en(List initStatus, String initSta) {
		if(initStatus==null) return initSta;
		for(int i=0; i<initStatus.size(); i++) {
			InitStatus initStatu = (InitStatus)initStatus.get(i);
			if(initStatu.getInitNo().trim().equals(initSta)) {
				return initStatu.getInitStatu();
			}
		}
		return initSta;
	}
	
	//销售商代号转名称
	public static String changeAgency(List initAgencyInfo, String agencyno) {
		if(initAgencyInfo==null) return "--";
		for(int i=0; i<initAgencyInfo.size(); i++) {
			InitAgencyInfo agencyInfo = (InitAgencyInfo)initAgencyInfo.get(i);
			if(agencyInfo.getAgencyNo().trim().equals(agencyno)) {
				return agencyInfo.getAgencyName();
			}
		}
		return "--";
	}
	//销售商代号转名称（英文版）
	public static String changeAgency_en(List initAgencyInfo, String agencyno) {
		if(initAgencyInfo==null) return agencyno;
		for(int i=0; i<initAgencyInfo.size(); i++) {
			InitAgencyInfo agencyInfo = (InitAgencyInfo)initAgencyInfo.get(i);
			if(agencyInfo.getAgencyNo().trim().equals(agencyno)) {
				return agencyInfo.getAgencyName();
			}
		}
		return agencyno;
	}
	
	//业务类别转业务名称
	public static String changeBusinFlag(List initBusinFlag, String businFlag) {
		if(initBusinFlag==null) return "--";
		for(int i=0; i<initBusinFlag.size(); i++) {
			InitBusinFlag businFlagBean = (InitBusinFlag)initBusinFlag.get(i);
			if(businFlagBean.getBusinFlag().trim().equals(businFlag)) {
				return businFlagBean.getBusinName();
			}
		}
		return "--";
	}
	//业务类别转业务名称(英文版)
	public static String changeBusinFlag_en(List initBusinFlag, String businFlag) {
		if(initBusinFlag==null) return businFlag;
		for(int i=0; i<initBusinFlag.size(); i++) {
			InitBusinFlag businFlagBean = (InitBusinFlag)initBusinFlag.get(i);
			if(businFlagBean.getBusinFlag().trim().equals(businFlag)) {
				return businFlagBean.getBusinName();
			}
		}
		return businFlag;
	}
	
	
	//字段表名称转换
	/**
	 * 1017 - status - 申请确认状态
	 * 
	 */
	public static String changeDict(List initDictionary, String keyValue, String keyNo) {
		if(initDictionary==null) return "--";
		for(int i=0; i<initDictionary.size(); i++) {
			InitDictionary dictionary = (InitDictionary)initDictionary.get(i);
			if(dictionary.getKeyNo().trim().equals(keyNo) && dictionary.getSysName().trim().equals("TA") && dictionary.getKeyValue().trim().equals(keyValue)) {
				return dictionary.getCaption();
				}
		}
		return "--";
	}
	
	//字段表名称转换(英文版)
	/**
	 * 1017 - status - 申请确认状态
	 * 
	 */
	public static String changeDict_en(List initDictionary, String keyValue, String keyNo) {
		if(initDictionary==null) return keyNo;
		for(int i=0; i<initDictionary.size(); i++) {
			InitDictionary dictionary = (InitDictionary)initDictionary.get(i);
			if(dictionary.getKeyNo().trim().equals(keyNo) && dictionary.getSysName().trim().equals("EQUERY") && dictionary.getKeyValue().trim().equals(keyValue)) {
				return dictionary.getCaption();
				}
		}
		return keyNo;
	}
	
}

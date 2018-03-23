package com.trs.query.struts.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.ssh.util.CheckData;
import com.ssh.util.DateTools;
import com.trs.ids.QueryActor;
import com.trs.ids.UserInfo;
import com.trs.init.beans.InitFundInfo;
import com.trs.init.dao.InitSysInfoDao;
import com.trs.query.beans.BalanceInfo;
import com.trs.query.beans.CheckAccountTitleInfo;
import com.trs.query.beans.EtfDayStocks;
import com.trs.query.beans.HistoryApply;
import com.trs.query.beans.Income;
import com.trs.query.beans.ShareDetail;
import com.trs.query.beans.TradeConfirm;
import com.trs.query.dao.QueryDao_modify;
import com.trs.util.DictionaryChange;
import com.trs.util.FormatDateTool;
import com.trs.util.Validate;

public class QueryAction_modify extends DispatchAction {

	private InitSysInfoDao initSysInfoDao;
	private QueryDao_modify queryDao;
	private static Log logger = LogFactory.getLog(QueryAction_modify.class);
	
	
	
	// 持有情况
	public ActionForward shareDetail(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(QueryActor.LOGIN_FLAG)).getCustomNo();
		
		String fundAccos = null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return null;
		}
		request.getSession().setAttribute("accounts", fundAccos);//填充对账单页面上的下拉列表
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		logger.info("持有情况 基金账号 = " + fundAccos);
		
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}
		
		String sysDate = queryDao.findIncomeDate();
		
		List<ShareDetail> list = queryDao.queryShareDetail(fundAcco, sysInfo);
		
		DecimalFormat df2 = new java.text.DecimalFormat("##0.00");
		double temp = 0d;
		
		for(ShareDetail sd : list){
			temp += Double.parseDouble(df2.format(Double.parseDouble(sd.getSz())));
			System.out.println("前市值:" + Double.parseDouble(sd.getSz()) + " 未分配收益:" + Double.parseDouble(sd.getUnassign()));
			System.out.println("后市值:" + df2.format(Double.parseDouble(sd.getSz())) + " 未分配收益:" + df2.format(Double.parseDouble(sd.getUnassign())));
		}
		String totalValue = String.valueOf(temp) ;
		
		
//		String sysDate = queryDao.querySysDate();
//
//		String beginDate = "2006-01-01";// auther yanzhou
//		String endDate = sysDate;
//		String taccoInfo = "";
//		java.text.DecimalFormat df2 = new java.text.DecimalFormat("##0.00");
//		logger.info("TA系统日期：" + sysDate);
//		
//		List balanceInfoList = queryDao.queryBalanceInfo(fundAcco, beginDate,
//				endDate, sysInfo, taccoInfo);// auther yanzhou
//		System.out.println(balanceInfoList);
//		double temp = 0d;
//		double totalValue = 0d;// 总市值
//		for (int i = 0; i < balanceInfoList.size(); i++) {
//			BalanceInfo bi = (BalanceInfo) balanceInfoList.get(i);
//			if (bi != null) {
//				temp = Double.parseDouble(df2.format(Double.parseDouble(bi.getTotalValue()))) + Double.parseDouble(df2.format(Double.parseDouble(bi.getIncome()))); // 加上未付收益
//				totalValue += temp;
//			}
//			bi = null;
//		}
		

	// 之前取基金净值的一种做法
	/*
	 * Iterator<ShareDetail> iter = list.iterator(); double totalValue = 0;
	 * while(iter.hasNext()) { ShareDetail shareDetail = iter.next();
	 * totalValue +=
	 * RoundTool.roundDouble(Double.parseDouble(shareDetail.getSz()), 2); }
	 */
		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("totalValue", totalValue);
		hashMap.put("sysDate", sysDate);
		
		JSONObject jsonObject = JSONObject.fromObject(hashMap);
		
		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("持有情况查询结果数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		
		//将用户所有账号下的持有基金代码放到SESSION中.
		if(list != null && list.size() > 0){
			StringBuffer sb = new StringBuffer();
			for(ShareDetail sd : list){
				if(sb.indexOf(sd.getFundCode()) == -1){
					sb.append(sd.getFundCode());
					sb.append(";");
				}
			}
			request.getSession().setAttribute("HAVEFUNDCODES", sb.toString().substring(0,sb.length()-1));
			logger.info("持有情况 基金账号：" + fundAcco + " ----- 基金代码：" + sb.toString());
			logger.info("持有情况 基金账号：" + fundAccos + " ----- 总市值：" + totalValue);
			System.out.println("持有情况 基金账号：" + fundAcco + " ----- 基金代码：" + sb.toString());
		}
		return null;
	}

	// 持有情况(英文版)
	public ActionForward shareDetail_en(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(QueryActor.LOGIN_FLAG)).getCustomNo();
		
		String fundAccos = null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return null;
		}
		request.getSession().setAttribute("accounts", fundAccos);//填充对账单页面上的下拉列表
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		logger.info("持有情况 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		log.info("-----" + sysInfo.size() + "------");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}
		List<ShareDetail> list = queryDao.queryShareDetail_en(fundAcco, sysInfo);
		
		
		DecimalFormat df2 = new java.text.DecimalFormat("##0.00");
		double temp = 0d;
		
		for(ShareDetail sd : list){
			temp += Double.parseDouble(df2.format(Double.parseDouble(sd.getSz())));
			System.out.println("市值:" + df2.format(Double.parseDouble(sd.getSz())) + " 未分配收益:" + df2.format(Double.parseDouble(sd.getUnassign())));
		}
		String totalValue = String.valueOf(temp) ;
		
		String sysDate = queryDao.findIncomeDate();
		
//		String sysDate = queryDao.querySysDate();
//
//		String beginDate = "2006-01-01";// auther yanzhou
//		String endDate = sysDate;
//		String taccoInfo = "";
//		java.text.DecimalFormat df2 = new java.text.DecimalFormat("##0.00");
//		logger.info("TA系统日期：" + sysDate);
//		List balanceInfoList = queryDao.queryBalanceInfo(fundAcco, beginDate,
//				endDate, sysInfo, taccoInfo);// auther yanzhou
//		double temp = 0d;
//		double totalValue = 0d;// 总市值
//		for (int i = 0; i < balanceInfoList.size(); i++) {
//			BalanceInfo bi = (BalanceInfo) balanceInfoList.get(i);
//			if (bi != null) {
//				temp = Double.parseDouble(df2.format(Double.parseDouble(bi
//						.getTotalValue())))
//						+ Double.parseDouble(df2.format(Double.parseDouble(bi
//								.getIncome()))); // 加上未付收益
//				totalValue += temp;
//			}
//			bi = null;
//		}

		/*
		 * Iterator<ShareDetail> iter = list.iterator(); double totalValue = 0;
		 * while(iter.hasNext()) { ShareDetail shareDetail = iter.next();
		 * totalValue +=
		 * RoundTool.roundDouble(Double.parseDouble(shareDetail.getSz()), 2); }
		 */

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("totalValue", totalValue);
		hashMap.put("sysDate", sysDate);
		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("持有情况查询结果数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		if(list != null && list.size() > 0){
			//将用户所有账号下的持有基金代码放到SESSION中.
			StringBuffer sb = new StringBuffer();
			for(ShareDetail sd : list){
				if(sb.indexOf(sd.getFundCode()) == -1){
					sb.append(sd.getFundCode());
					sb.append(";");
				}
			}
			request.getSession().setAttribute("HAVEFUNDCODES", sb.toString().substring(0,sb.length()-1));
			logger.info("持有情况 基金账号：" + fundAcco + " ----- 基金代码：" + sb.toString());
			logger.info("持有情况 基金账号：" + fundAcco + " ----- 总市值：" + totalValue);
		}
		return null;
	}

	// 历史申请列表查询
	public ActionForward historyApply(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return null;
		}

		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		int pageSize = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
		} catch (Exception e) {
			logger.warn("历史申请 pageSize = " + pageSize);
		}
		int currentPage = 0;
		if (request.getParameter("cp") != null
				&& !"".equals(request.getParameter("cp"))) {
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} else {
			currentPage = 0;
		}

		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("历史申请(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("历史申请(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
			
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}
		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		List list = queryDao.queryHistoryApply(beginDate, endDate, fundAcco,
				currentPage, pageSize, sysInfo, false, fundTaccoInfo);
		
		Integer count = queryDao.queryHistoryApplySum(beginDate, endDate,
				fundAcco, fundTaccoInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("查询历史申请出错 " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	// 历史申请列表查询(英文版)
	public ActionForward historyApply_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.setContentType("text/html; charset=utf-8");

		 //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos=queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return null;
		}
		
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		int pageSize = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
		} catch (Exception e) {
			logger.warn("历史申请 pageSize = " + pageSize);
		}
		int currentPage = 0;
		if (request.getParameter("cp") != null
				&& !"".equals(request.getParameter("cp"))) {
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} else {
			currentPage = 0;
		}

		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("历史申请(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("历史申请(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
			
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}
		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		List list = queryDao.queryHistoryApply_en(beginDate, endDate, fundAcco,
				currentPage, pageSize, sysInfo, false, fundTaccoInfo);
		
		for (int i = 0; i < list.size(); i++) {
			HistoryApply history = (HistoryApply) list.get(i);
			System.out.println("历史申请状态" + history.getStatus());
		}
		
		Integer count = queryDao.queryHistoryApplySum(beginDate, endDate,
				fundAcco, fundTaccoInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("查询历史申请出错 " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	// 历史申请详细查询
	public ActionForward historyApplyDetail(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		// 判断登陆
		String fundAcco = ((UserInfo) request.getSession().getAttribute(QueryActor.LOGIN_FLAG)).getAccount();
		String serialno = request.getParameter("serialno");
		String applyDate = request.getParameter("applydate");
		
		logger.info("历史申请详细查询 L_SEROALNO=" + serialno + " 申请日期applyDate=" + applyDate);
		System.out.println("历史申请详细查询 L_SEROALNO=" + serialno + " 申请日期applyDate=" + applyDate);
		
		if (fundAcco == null && fundAcco.trim().length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}
		
		List historyApplyDetail = queryDao.queryHistoryApplyDeatil(serialno,applyDate,sysInfo);
		request.setAttribute("historyApplyDetail", historyApplyDetail);

		logger.debug("Action成功返回历史申请细缆查询结果...");
		return mapping.findForward("historyApplyDetail");

	}

	// 历史申请详细查询(英文版)
	public ActionForward historyApplyDetail_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String fundAcco = ((UserInfo) request.getSession().getAttribute(QueryActor.LOGIN_FLAG)).getAccount();
		String serialno = request.getParameter("serialno");
		String applyDate = request.getParameter("applydate");
		
		logger.info("历史申请详细查询 L_SEROALNO=" + serialno + " 申请日期applyDate=" + applyDate);
		System.out.println("历史申请详细查询 L_SEROALNO=" + serialno + " 申请日期applyDate=" + applyDate);
		
		if (fundAcco == null && fundAcco.trim().length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}
		

		List historyApplyDetail = queryDao.queryHistoryApplyDeatil_en(serialno, applyDate, sysInfo);
		request.setAttribute("historyApplyDetail", historyApplyDetail);

		logger.debug("Action成功返回历史申请细缆查询结果...");
		return mapping.findForward("historyApplyDetail_en");

	}

	// 交易确认初始化信息
	public ActionForward tradeConfirmInit(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
//		String fundAcco = FormatDateTool.stringFormat(fundAccos);
//		logger.info("持有情况 基金账号 = " + fundAcco);
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("交易确认初始化 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		List myFunds = (List) request.getSession().getAttribute("myFunds");
		if (myFunds == null) {
			myFunds = queryDao.queryTradeConfirmFund(fundAcco, sysInfo);
			request.getSession().setAttribute("myFunds", myFunds);
		}

		List businFlag = queryDao.queryTradeConfirmBusinFlag();
		List agency = queryDao.queryTradeConfirmAgency();

		HashMap hashMap = new HashMap();

		hashMap.put("tradeConfirmInitFund", myFunds);
		hashMap.put("tradeConfirmInitBusinFlag", businFlag);
		hashMap.put("tradeConfirmInitAgency", agency);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("交易确认查询初始化下拉框数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.debug("Action成功返回交易确认查询下拉框数据...");
		return null;
	}

	// 交易确认初始化信息(英文版)
	public ActionForward tradeConfirmInit_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}

		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("交易确认初始化 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		List myFunds = (List) request.getSession().getAttribute("myFunds");
		log.info("----" + myFunds.size() + "-----");
		if (myFunds == null) {
			myFunds = queryDao.queryTradeConfirmFund_en(fundAcco, sysInfo);// 基金代码向基金名称的转换
			request.getSession().setAttribute("myFunds", myFunds);
		}

		List businFlag = queryDao.queryTradeConfirmBusinFlag_en(sysInfo);
		log.info("businFlag" + businFlag.size());

		List agency = queryDao.queryTradeConfirmAgency_en(sysInfo);
		log.info("agency" + agency.size());

		HashMap hashMap = new HashMap();

		hashMap.put("tradeConfirmInitFund", myFunds);
		hashMap.put("tradeConfirmInitBusinFlag", businFlag);
		hashMap.put("tradeConfirmInitAgency", agency);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("交易确认查询初始化下拉框数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.debug("Action成功返回交易确认查询下拉框数据...");
		return null;
	}

	// 交易确认
	public ActionForward tradeConfirm(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setContentType("text/html; charset=utf-8");
		
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}
		
		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("交易确认 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		String queryCondition = request.getParameter("queryCondition");
		String fundCode = request.getParameter("fundCode");
		String agencyNo = request.getParameter("agencyNo");
		String businFlag = request.getParameter("businFlag");
		String requestno = request.getParameter("requestno");
		String beginDate = request.getParameter("beginDate");
		String endDate = request.getParameter("endDate");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		logger.info("选中的账号类型: " + taccotype);
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("交易确认(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("交易确认(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}

		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		List list = queryDao.queryTradeConfirm(fundAcco, queryCondition,
				fundCode, agencyNo, businFlag, requestno, beginDate, endDate,
				currentPage, pageSize, sysInfo, false, fundTaccoInfo);
		Integer count = queryDao.queryTradeConfirmSum(fundAcco, queryCondition,
				fundCode, agencyNo, businFlag, requestno, beginDate, endDate, fundTaccoInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);
		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("交易确认查询出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.debug("Action成功返回交易确认查询结果...");
		return null;
	}
	
	// 交易确认(英文版)
	public ActionForward tradeConfirm_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.debug("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("交易确认 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		String queryCondition = request.getParameter("queryCondition");
		String fundCode = request.getParameter("fundCode");
		String agencyNo = request.getParameter("agencyNo");
		String businFlag = request.getParameter("businFlag");
		String requestno = request.getParameter("requestno");
		String beginDate = request.getParameter("beginDate");
		String endDate = request.getParameter("endDate");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("交易确认(EN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("交易确认(EN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
			
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}
		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		List list = queryDao.queryTradeConfirm_en(fundAcco, queryCondition,
				fundCode, agencyNo, businFlag, requestno, beginDate, endDate,
				currentPage, pageSize, sysInfo, false,fundTaccoInfo);
		Integer count = queryDao.queryTradeConfirmSum(fundAcco, queryCondition,
				fundCode, agencyNo, businFlag, requestno, beginDate, endDate,fundTaccoInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);
		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("交易确认查询出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.debug("Action成功返回交易确认查询结果...");
		return null;
	}
	
	//交易确认详细
	public ActionForward tradeDetail(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.debug("获取客户编号失败 = " + custno);
			return null;
		}

		if (fundAccos == null || fundAccos.length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.debug("交易确认查询 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}
		
		String serialno = request.getParameter("serialno");
		List<TradeConfirm> list = queryDao.tradeDetailBySerialno(serialno, sysInfo);
		
		logger.debug("Action成功返回交易确认查询结果...");
		
		request.setAttribute("list", list);
		return mapping.findForward("tradeDetail");
	}
	
	//交易确认详细(英文版)
	public ActionForward tradeDetail_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}

		if (fundAccos == null || fundAccos.length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("交易确认查询 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}
		System.out.println(sysInfo.size());
		
		String serialno = request.getParameter("serialno");
		List list = queryDao.tradeDetailBySerialno(serialno, sysInfo);
		
		logger.info("Action成功返回交易确认查询结果...");
		
		request.setAttribute("list", list);
		return mapping.findForward("tradeDetail_en");
	}

	// 历史分红初始化信息
	public ActionForward historyBonusInit(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}

		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("历史分红初始化 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		try {
			PrintWriter out = response.getWriter();
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("历史分红查询初始化下拉框数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回历史分红查询下拉框数据...");
		return null;

	}

	// 历史分红初始化信息(英文版)
	public ActionForward historyBonusInit_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("历史分红初始化 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		try {
			PrintWriter out = response.getWriter();
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("历史分红查询初始化下拉框数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回历史分红查询下拉框数据...");
		return null;

	}

	// 历史分红
	public ActionForward historyBonus(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		response.setContentType("text/html; charset=utf-8");
		
		/// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos=null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("历史分红 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		String beginDate = request.getParameter("beginDate");
		String endDate = request.getParameter("endDate");
		String fundCode = request.getParameter("fundCode");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("历史分红(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("历史分红(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
			
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}

		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		List list = queryDao.queryHistoryBonus(fundAcco, fundCode, beginDate,
				endDate, currentPage, pageSize, sysInfo,fundTaccoInfo);
		Integer count = queryDao.queryHistoryBonusSum(fundAcco, fundCode,
				beginDate, endDate,fundTaccoInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);
		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("历史分红查询结果数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回历史分红结果集...");
		return null;
	}

	// 历史分红(英文版)
	public ActionForward historyBonus_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		response.setContentType("text/html; charset=utf-8");
		
		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos=null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}


		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("历史分红 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		String beginDate = request.getParameter("beginDate");
		String endDate = request.getParameter("endDate");
		String fundCode = request.getParameter("fundCode");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("历史分红(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("历史分红(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
			
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}

		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		List list = queryDao.queryHistoryBonus_en(fundAcco, fundCode,
				beginDate, endDate, currentPage, pageSize, sysInfo,fundTaccoInfo);
		Integer count = queryDao.queryHistoryBonusSum(fundAcco, fundCode,
				beginDate, endDate,fundTaccoInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);
		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("历史分红查询结果数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回历史分红结果集...");
		return null;
	}

	// 分红信息查询
	public ActionForward bonusType(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("分红类型查询 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("分红类型 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		List list = queryDao.queryBobusType(fundAcco, currentPage, pageSize,
				sysInfo);
		Integer count = queryDao.queryBonusTypeSum(fundAcco);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);
		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("分红方式查询结果数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回分红方式结果集...");
		return null;

	}

	// 分红信息查询（英文版）
	public ActionForward bonusType_en(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("分红类型查询 基金账号 = " + fundAcco);

		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("分红类型 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		List list = queryDao.queryBobusType_en(fundAcco, currentPage, pageSize,
				sysInfo);
		Integer count = queryDao.queryBonusTypeSum(fundAcco);

		HashMap hashMap = new HashMap();
		hashMap.put("list", list);
		hashMap.put("count", count);
		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("分红方式查询结果数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回分红方式结果集...");
		return null;

	}

	// 对账单查询
	public ActionForward checkAccounts(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		response.setContentType("text/html; charset=utf-8");
		java.io.InputStream in = getClass().getClassLoader()
				.getResourceAsStream("ip.properties");
		
		Properties prop = new Properties();
		try {
			prop.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String pass1 = prop.getProperty("key1").replace(" ", "");
		String pass2 = prop.getProperty("key2").replace(" ", "");
		String pass3 = prop.getProperty("key3").replace(" ", "");
		String pass4 = prop.getProperty("key4").replace(" ", "");
		String pass5 = prop.getProperty("key5").replace(" ", "");
		String pass6 = prop.getProperty("key6").replace(" ", "");
		String pass7 = prop.getProperty("key7").replace(" ", "");
		String pass8 = prop.getProperty("key8").replace(" ", "");
		String custip = request.getHeader("x-forwarded-for");
		if (custip == null || custip.length() == 0
				|| "unknown".equalsIgnoreCase(custip)) {
			custip = request.getHeader("Proxy-Client-IP");
		}

		if (custip == null || custip.length() == 0
				|| "unknown".equalsIgnoreCase(custip)) {
			custip = request.getHeader("WL-Proxy-Client-IP");
		}

		if (custip == null || custip.length() == 0
				|| "unknown".equalsIgnoreCase(custip)) {
			custip = request.getRemoteAddr();
		}
		System.out.println("******" + custip);
		if (custip.equals(pass1) || custip.equals(pass2)
				|| custip.equals(pass3) || custip.equals(pass4)
				|| custip.equals(pass5) || custip.equals(pass6)
				|| custip.equals(pass7) || custip.equals(pass8)) {

			String fundAccos = request.getParameter("fundacco");
			
			// 系统查询所需公共信息
			HashMap sysInfo = (HashMap) request.getSession()
					.getServletContext().getAttribute("initSysInfo");
			if (sysInfo == null) {
				sysInfo = initSysInfoDao.getInitSysInfo();
				request.getSession().getServletContext().setAttribute(
						"initSysInfo", sysInfo);
			}
			
			String beginDate = request.getParameter("begindate");
			String endDate = request.getParameter("enddate");
			String fundAcco = FormatDateTool.stringFormat(fundAccos);
			System.out.println(fundAcco);
			logger.info("对账单查询 基金账号 = " + fundAcco);
			
			if (!Validate.date10Validate(beginDate)
					|| !Validate.date10Validate(endDate)) {
				logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
				return null;
			}

			List balanceInfoList = queryDao.queryBalanceInfo(fundAcco,
					beginDate, endDate, sysInfo, null);
			List periodTradeList = queryDao.queryPeriodTrade(fundAcco,
					beginDate, endDate, sysInfo, null);

			String userName = queryDao.queryUserName(fundAcco); // 户名
			java.text.DecimalFormat df2 = new java.text.DecimalFormat("##0.00");

			double temp = 0d;
			double sumTotalValue = 0d; // 总市值
			for (int i = 0; i < balanceInfoList.size(); i++) {
				BalanceInfo bi = (BalanceInfo) balanceInfoList.get(i);
				if (bi != null) {
					temp = Double.parseDouble(df2.format(Double.parseDouble(bi
							.getTotalValue())))
							+ Double.parseDouble(df2.format(Double
									.parseDouble(bi.getIncome()))); // 加上未付收益
					sumTotalValue += temp;
					bi.setTotalValue(String.valueOf(temp));
				}
				bi = null;
			}
			CheckAccountTitleInfo titleInfo = new CheckAccountTitleInfo();
			List titleInfoList = new ArrayList(1); // 对账单头部信息
			titleInfo.setUserName(userName);
			titleInfo.setFundAcco(fundAcco);
			titleInfo.setBeginDate(beginDate);
			titleInfo.setEndDate(endDate);
			titleInfo.setSumTotalValue(String.valueOf(sumTotalValue));
			titleInfoList.add(titleInfo);
			request.getSession().setAttribute("balanceInfo", balanceInfoList);
			request.getSession().setAttribute("periodTrade", periodTradeList);
			request.getSession().setAttribute("titleInfo", titleInfoList);

			return mapping.findForward("account");
		} else {
			return null;
		}
	}

	// 对账单查询(CN)
	public ActionForward checkAccount(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		System.out.println("custon:" + custno);
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		
		if ( (fundAccos == null || fundAccos.length() <= 0) && (tradeaccos == null || tradeaccos.length() <= 0) ) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("对账单查询(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taccoinfo");//all:所有交易帐号.交易帐号
		logger.info("对账单查询(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}

		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("不合法的日期格式");
			return null;
		}

		List balanceInfoList = queryDao.queryBalanceInfo(fundAcco, beginDate, endDate, sysInfo, fundTaccoInfo);
		List periodTradeList = queryDao.queryPeriodTrade(fundAcco, beginDate, endDate, sysInfo, fundTaccoInfo);

		String userName = queryDao.queryUserName(fundAcco); // 户名

		java.text.DecimalFormat df2 = new java.text.DecimalFormat("##0.00");
		double temp = 0d;
		double sumTotalValue = 0d; // 总市值
		for (int i = 0; i < balanceInfoList.size(); i++) {
			BalanceInfo bi = (BalanceInfo) balanceInfoList.get(i);
			if (bi != null) {
				temp = Double.parseDouble(df2.format(Double.parseDouble(bi.getTotalValue()))) + Double.parseDouble(df2.format(Double.parseDouble(bi.getIncome()))); // 加上未付收益
				sumTotalValue += temp;
				bi.setTotalValue(String.valueOf(temp));
			}
			bi = null;
		}
		CheckAccountTitleInfo titleInfo = new CheckAccountTitleInfo();
		List titleInfoList = new ArrayList(1); // 对账单头部信息
		titleInfo.setUserName(userName.indexOf(",") != -1 ? "所有账号" : userName);
		if(fundAcco != null && taccotype.trim().equalsIgnoreCase("1")){
			titleInfo.setFundAcco(taccoAcco.equalsIgnoreCase("all") || taccoAcco.length() > 14 ? "所有账户" : taccoAcco);
		} else {
			titleInfo.setFundAcco(taccoInfo.equalsIgnoreCase("all") || taccoInfo.length() > 14 ? "所有账户" : taccoInfo);
		}
		titleInfo.setBeginDate(beginDate);
		titleInfo.setEndDate(endDate);
		titleInfo.setSumTotalValue(String.valueOf(sumTotalValue));
		titleInfoList.add(titleInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("balanceInfo", balanceInfoList);
		hashMap.put("periodTrade", periodTradeList);
		hashMap.put("titleInfo", titleInfoList);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("对账单查询数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回对账单查询结果集数据...");
		return null;
	}

	// 对账单查询(英文版)
	public ActionForward checkAccount_en(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
			
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}

		if ( (fundAccos == null || fundAccos.length() <= 0) && (tradeaccos == null && tradeaccos.length() <= 0) ) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号

		String fundAcco = null;
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("对账单查询(EN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = null;
		String taccoInfo = request.getParameter("taccoinfo");//all:所有交易帐号.交易帐号
		logger.info("对账单查询(EN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}
		
		
		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("不合法的日期格式");
			return null;
		}

		List balanceInfoList = queryDao.queryBalanceInfo_en(fundAcco,
				beginDate, endDate, sysInfo, fundTaccoInfo);
		List periodTradeList = queryDao.queryPeriodTrade_en(fundAcco,
				beginDate, endDate, sysInfo, fundTaccoInfo);

		String userName = queryDao.queryUserName(fundAcco); // 户名
		java.text.DecimalFormat df2 = new java.text.DecimalFormat("##0.00");

		double temp = 0d;
		double sumTotalValue = 0d; // 总市值
		for (int i = 0; i < balanceInfoList.size(); i++) {
			BalanceInfo bi = (BalanceInfo) balanceInfoList.get(i);
			if (bi != null) {
				temp = Double.parseDouble(df2.format(Double.parseDouble(bi
						.getTotalValue())))
						+ Double.parseDouble(df2.format(Double.parseDouble(bi
								.getIncome()))); // 加上未付收益
				sumTotalValue += temp;
				bi.setTotalValue(String.valueOf(temp));
			}
			bi = null;
		}
		CheckAccountTitleInfo titleInfo = new CheckAccountTitleInfo();
		List titleInfoList = new ArrayList(1); // 对账单头部信息
		titleInfo.setUserName(userName.indexOf(",") != -1 ? "All Accounts" : userName);
		if(fundAcco != null && taccotype.trim().equalsIgnoreCase("1")){
			titleInfo.setFundAcco(taccoAcco.equalsIgnoreCase("all") || taccoAcco.length() > 14 ? "All Accounts" : taccoAcco);
		} else {
			titleInfo.setFundAcco(taccoInfo.equalsIgnoreCase("all") || taccoInfo.length() > 14 ? "All Accounts" : taccoInfo);
		}
		titleInfo.setBeginDate(beginDate);
		titleInfo.setEndDate(endDate);
		titleInfo.setSumTotalValue(String.valueOf(sumTotalValue));
		titleInfoList.add(titleInfo);

		HashMap hashMap = new HashMap();
		hashMap.put("balanceInfo", balanceInfoList);
		hashMap.put("periodTrade", periodTradeList);
		hashMap.put("titleInfo", titleInfoList);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);

		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("对账单查询数据出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回对账单查询结果集数据...");
		return null;
	}

	// 盈亏查询
	public ActionForward income(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("盈亏查询　基金账号 = " + fundAcco);
		
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String fund = CheckData.c(request.getParameter("fund")); // 页面选的基金
		logger.info("cu=" + custno + "---bd=" + beginDate + "---ed=" + endDate + "---fund=" + fund);
		System.out.println("cu=" + custno + "---bd=" + beginDate + "---ed=" + endDate + "---fund=" + fund);
		
		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		ArrayList<Income> incomelist = new ArrayList<Income>();
		List initFundInfo = (List) sysInfo.get("initFundInfo");
		
		String[] fundArray = fundAcco.split(",");
		
		for (int k = 0; k < fundArray.length; k++) {
			String temp = fundArray[k].toString();
			if (fund == null || fund.equals("")) {
				if (initFundInfo != null && initFundInfo.size() > 0) {
					for (int i = 0; i < initFundInfo.size(); i++) {
						Income income = new Income();
						String _fundCode = ((InitFundInfo) initFundInfo.get(i)).getFundCode(); // 全部旗下的基金
						income = queryDao.queryIncome(temp, _fundCode,beginDate, endDate, sysInfo);
						logger.info("income---->" + _fundCode + "=FfShares:" + income.getFfShares() + ",FfNetValue:" + income.getFfNetValue() + ",FfEndShares:" + income.getFfEndShares() + ",FfEndNetValue:" + income.getFfEndNetValue() + ",FfProfit:" + income.getFfProfit() + ",FfAFare:" + income.getFfAFare());
						System.out.println("income---->" + _fundCode + "=FfShares:" + income.getFfShares() + ",FfNetValue:" + income.getFfNetValue() + ",FfEndShares:" + income.getFfEndShares() + ",FfEndNetValue:" + income.getFfEndNetValue() + ",FfProfit:" + income.getFfProfit() + ",FfAFare:" + income.getFfAFare());
						if (income != null
								&& (!((income.getFfShares()
										* income.getFfNetValue() == 0)
										&& (income.getFfEndShares()
												* income.getFfEndNetValue() == 0) && (income
										.getFfProfit()
										- income.getFfAFare() == 0)))) {
							// equerying/income.jsp row 337
							logger.info("incomelist.add:" + income);
							incomelist.add(income);
						}
					}
				}
			} else {
				Income income = new Income();
				income = queryDao.queryIncome(temp, fund, beginDate,endDate, sysInfo);
				System.out.println("income---->" + fund + "=FfShares:" + income.getFfShares() + ",FfNetValue:" + income.getFfNetValue() + ",FfEndShares:" + income.getFfEndShares() + ",FfEndNetValue:" + income.getFfEndNetValue() + ",FfProfit:" + income.getFfProfit() + ",FfAFare:" + income.getFfAFare());
				if (income != null
						&& (!((income.getFfShares() * income.getFfNetValue() == 0)
								&& (income.getFfEndShares()
										* income.getFfEndNetValue() == 0) && (income
								.getFfProfit()
								- income.getFfAFare() == 0)))) {
					// equerying/income.jsp row 337
					logger.info("incomelist.add:" + income);
					incomelist.add(income);
				}
			}
		}

		request.setAttribute("list", incomelist);
		request.setAttribute("begindate", beginDate);
		request.setAttribute("enddate", endDate);
		request.setAttribute("fund", fund); // 页面上选的

		return mapping.findForward("income");
	}

	// 盈亏查询(英文版)
	public ActionForward income_en(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos); // 基金账号
		logger.info("盈亏查询　基金账号 = " + fundAcco);
		
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}

		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String fund = CheckData.c(request.getParameter("fund")); // 页面选的基金代码
		System.out.println("(EN)    cu=" + custno + "---bd=" + beginDate + "---ed=" + endDate + "---fund=" + fund);
		
		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		ArrayList<Income> incomelist = new ArrayList<Income>();
		List initFundInfo = (List) sysInfo.get("initFundInfo");
		
		String[] fundArray = fundAcco.split(",");
		
		for (int k = 0; k < fundArray.length; k++) {
			String temp = fundArray[k].toString();
			if (fund == null || fund.equals("")) {
				if (initFundInfo != null && initFundInfo.size() > 0) {
					for (int i = 0; i < initFundInfo.size(); i++) {
						Income income = new Income();
						String _fundCode = ((InitFundInfo) initFundInfo.get(i))
								.getFundCode(); // 全部旗下的基金
						income = queryDao.queryIncome_en(temp, _fundCode,
								beginDate, endDate, sysInfo);
						System.out.println("income_en---->" + _fundCode + "=FfShares:" + income.getFfShares() + ",FfNetValue:" + income.getFfNetValue() + ",FfEndShares:" + income.getFfEndShares() + ",FfEndNetValue:" + income.getFfEndNetValue() + ",FfProfit:" + income.getFfProfit() + ",FfAFare:" + income.getFfAFare());
						if (income != null
								&& (!((income.getFfShares()
										* income.getFfNetValue() == 0)
										&& (income.getFfEndShares()
												* income.getFfEndNetValue() == 0) && (income
										.getFfProfit()
										- income.getFfAFare() == 0)))) {
							// equerying/income.jsp row 337
							logger.info("incomelist.add:" + income);
							incomelist.add(income);
						}
					}
				}
			} else {
				Income income = new Income();
				income = queryDao.queryIncome_en(temp, fund, beginDate,
						endDate, sysInfo);
				System.out.println("income_en---->" + fund + "=FfShares:" + income.getFfShares() + ",FfNetValue:" + income.getFfNetValue() + ",FfEndShares:" + income.getFfEndShares() + ",FfEndNetValue:" + income.getFfEndNetValue() + ",FfProfit:" + income.getFfProfit() + ",FfAFare:" + income.getFfAFare());
				if (income != null
						&& (!((income.getFfShares() * income.getFfNetValue() == 0)
								&& (income.getFfEndShares()
										* income.getFfEndNetValue() == 0) && (income
								.getFfProfit()
								- income.getFfAFare() == 0)))) {
					// equerying/income.jsp row 337
					logger.info("incomelist.add:" + income);
					incomelist.add(income);
				}
			}
		}

		request.setAttribute("list", incomelist);
		request.setAttribute("begindate", beginDate);
		request.setAttribute("enddate", endDate);
		request.setAttribute("fund", fund); // 页面上选的

		return mapping.findForward("income_en");
	}

	// 未付收益
	public ActionForward unincome(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}
		
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo", sysInfo);
		}

		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("未付收益 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("未付收益(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("未付收益(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
			
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}

		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		logger.info("未付收益查询 基金账号 = " + fundAcco + " 开始日期 = " + beginDate
				+ " 结束日期 = " + endDate);
		System.out.println("未付收益查询 基金账号 = " + fundAcco + " 开始日期 = " + beginDate
				+ " 结束日期 = " + endDate);

		List list = queryDao.queryUnincome(fundAcco, beginDate, endDate,
				currentPage, pageSize, false, fundTaccoInfo);
		int count = queryDao.queryUnincomeCount(fundAcco, beginDate, endDate, fundTaccoInfo);

		HashMap hashMap = new HashMap();

		DecimalFormat df = new DecimalFormat(",##0.00");

		for (int i = 0; i < list.size(); i++) {
			Map map = (Map) list.get(i);
			String custType = map.get("C_CUSTTYPE").toString();
			String income = map.get("F_INCOME") == null ? "0" : map.get(
					"F_INCOME").toString();
			String frozenIncome = map.get("F_FROZENINCOME") == null ? "0" : map
					.get("F_FROZENINCOME").toString();
			String realShares = map.get("F_LASTSHARES") == null ? "0" : map
					.get("F_LASTSHARES").toString();
			String frozenShares = map.get("F_LASTFREEZESHARE") == null ? "0"
					: map.get("F_LASTFREEZESHARE").toString();
			String confirmDate = map.get("D_CDATE") == null ? "--" : DateTools
					.getDateString((Date) map.get("D_CDATE"));

			map.put("F_INCOME", df.format(Double.parseDouble(income)));
			map.put("F_FROZENINCOME", df.format(Double
					.parseDouble(frozenIncome)));
			map.put("F_LASTSHARES", df.format(Double.parseDouble(realShares)));
			map.put("F_LASTFREEZESHARE", df.format(Double
					.parseDouble(frozenShares)));
			map.put("D_CDATE", confirmDate);
		}
		hashMap.put("list", list);
		hashMap.put("count", count);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);
		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("查询未付收益出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回未付收益查询结果...");
		return null;
	}

	// 未付收益(英文版)
	public ActionForward unincome_en(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setContentType("text/html; charset=utf-8");

		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;

		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		if (fundAccos == null || fundAccos.length() <= 0) {
			return mapping.findForward("pleaseLogin");
		}
		// 系统查询所需公共信息
		HashMap sysInfo = (HashMap) request.getSession().getServletContext()
				.getAttribute("initSysInfo_en");
		if (sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo_en();
			request.getSession().getServletContext().setAttribute(
					"initSysInfo_en", sysInfo);
		}
		
		List initFundInfos = (List) sysInfo.get("initFundInfo");

		int pageSize = 0;
		int currentPage = 0;
		try {
			pageSize = Integer.parseInt(request.getParameter("pageSize"));
			currentPage = Integer.parseInt(request.getParameter("cp"));
		} catch (Exception e) {
			logger.warn("未付收益 pageSize = " + pageSize + " currentPage = "
					+ currentPage);
		}

		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		
		String taccotype = request.getParameter("accotype");//0:交易账号  1:基金账号
		
		String fundAcco = "";
		String taccoAcco = request.getParameter("fundacco");//all:所有基金帐号.基金帐号
		logger.info("未付收益(CN)-基金账号 = " + taccoAcco);
		
		String fundTaccoInfo = "";
		String taccoInfo = request.getParameter("taco");//all:所有交易帐号.交易帐号
		logger.info("未付收益(CN)-交易账号 = " + taccoInfo);
		
		if(taccotype.trim().equalsIgnoreCase("1")){
			if(!taccoAcco.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(taccoAcco);
			} else {
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
			
		} else { 
			if(!taccoInfo.trim().equalsIgnoreCase("all")){
				fundAcco = FormatDateTool.stringFormat(fundAccos);
				fundTaccoInfo = FormatDateTool.stringFormat(taccoInfo);
			} else {
//				fundTaccoInfo = FormatDateTool.stringFormat(tradeaccos);
				fundAcco = FormatDateTool.stringFormat(fundAccos);
			}
		}
		

		if (!Validate.date10Validate(beginDate)
				|| !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
			return null;
		}

		logger.info("未付收益查询 基金账号 = " + fundAcco + " 开始日期 = " + beginDate
				+ " 结束日期 = " + endDate);

		List list = queryDao.queryUnincome_en(fundAcco, beginDate, endDate,
				currentPage, pageSize, false, fundTaccoInfo);
		int count = queryDao.queryUnincomeCount(fundAcco, beginDate, endDate, fundTaccoInfo);

		HashMap hashMap = new HashMap();
		
		DecimalFormat df = new DecimalFormat(",##0.00");

		for (int i = 0; i < list.size(); i++) {
			Map map = (Map) list.get(i);
			String custType = map.get("C_CUSTTYPE").toString();
			String income = map.get("F_INCOME") == null ? "0" : map.get(
					"F_INCOME").toString();
			String frozenIncome = map.get("F_FROZENINCOME") == null ? "0" : map
					.get("F_FROZENINCOME").toString();
			String realShares = map.get("F_LASTSHARES") == null ? "0" : map
					.get("F_LASTSHARES").toString();
			String frozenShares = map.get("F_LASTFREEZESHARE") == null ? "0"
					: map.get("F_LASTFREEZESHARE").toString();
			String confirmDate = map.get("D_CDATE") == null ? "--" : DateTools
					.getDateString((Date) map.get("D_CDATE"));
			
			String fundName = DictionaryChange.changeFundInfo_en(initFundInfos,map.get("C_FUNDCODE").toString());

			map.put("F_INCOME", df.format(Double.parseDouble(income)));
			map.put("F_FROZENINCOME", df.format(Double
					.parseDouble(frozenIncome)));
			map.put("F_LASTSHARES", df.format(Double.parseDouble(realShares)));
			map.put("F_LASTFREEZESHARE", df.format(Double
					.parseDouble(frozenShares)));
			map.put("D_CDATE", confirmDate);
			map.put("C_FUNDNAME", fundName);
		}

		hashMap.put("list", list);
		hashMap.put("count", count);

		JSONObject jsonObject = JSONObject.fromObject(hashMap);
		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("查询未付收益出错 " + e.getMessage());
			e.printStackTrace();
		}
		logger.info("Action成功返回未付收益查询结果...");
		return null;
	}
	
	//查询ETF申购赎回清单
	public ActionForward etfQuery(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Map hashMap = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		try{
			String date = request.getParameter("today");
			String code = request.getParameter("fundcode");
			
			if (date == null || date.equalsIgnoreCase("")) {
				date = sdf.format(queryDao.getNewDate());
				System.out.println("获取参数:" + date);
			}
	
			if(code != null && !code.equalsIgnoreCase("")){
				hashMap = queryDao.queryInfo(date, code);
				List<EtfDayStocks> list = queryDao.queryStocksInfo(date, code);
				if(list != null){
					hashMap.put("list", list);
					request.setAttribute("map", hashMap);
					logger.info("Action成功返回ETF申购赎回清单查询结果...");
				} else {
					request.setAttribute("codeerr", "未查询到数据,请选择其他的日期");
				}
				
			} else {
				request.setAttribute("codeerr", "请输入基金代码");
			}
			
			request.setAttribute("date", date);
			return mapping.findForward("etf_index");
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw new Exception("系统异常.");
		}
	}

	

	/**
	 * 根据登录用户的CUSTNO查询基金和交易帐号（填充下拉列表框）
	 * @param mapping
	 * @param from
	 * @param request
	 * @param response
	 * @return
	 */
	public ActionForward loadAccountsOption(ActionMapping mapping,ActionForm form,HttpServletRequest request,
			HttpServletResponse response){
		Map<String, String> map = new HashMap<String, String>(); 
		// 判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeaccos = null;
		System.out.println("custon:" + custno);
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
			tradeaccos = queryDao.findTradeAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}
		
		/*if ( (fundAccos == null || fundAccos.length() <= 0) && (tradeaccos == null || tradeaccos.length() <= 0) ) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("nologin", "nologin");
			try {
				PrintWriter out = response.getWriter();
				out.println(jsonObject.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}*/
		
		//填充对账单页面上的下拉列表
		//request.getSession().setAttribute("accounts", fundAccos);
		//request.getSession().setAttribute("tradeaccos", tradeaccos);
		
		map.put("accounts", fundAccos);
		map.put("tradeaccos", tradeaccos);
		JSONObject jsonObject = JSONObject.fromObject(map);
		try {
			PrintWriter out = response.getWriter();
			out.println(jsonObject.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			logger.error("加载账号信息错误:" + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public void setInitSysInfoDao(InitSysInfoDao initSysInfoDao) {
		this.initSysInfoDao = initSysInfoDao;
	}

	public void setQueryDao(QueryDao_modify queryDao) {
		this.queryDao = queryDao;
	}

}

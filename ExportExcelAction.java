package com.trs.query.struts.action;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.trs.ids.UserInfo;
import com.trs.init.beans.InitFundInfo;
import com.trs.init.dao.InitSysInfoDao;
import com.trs.query.beans.BalanceInfo;
import com.trs.query.beans.CheckAccountTitleInfo;
import com.trs.query.beans.EtfDay;
import com.trs.query.beans.EtfDayStocks;
import com.trs.query.beans.EtfList;
import com.trs.query.beans.Income;
import com.trs.query.beans.PeriodTrade;
import com.trs.query.beans.ShareDetail;
import com.trs.query.dao.QueryDao;
import com.trs.util.FormatDateTool;
import com.trs.util.Validate;

/**
 * @author tan.hongyan
 * @version: Nov 24, 2009 12:05:55 PM
 * @desc: 
 */
public class ExportExcelAction extends DispatchAction {
	
	private InitSysInfoDao initSysInfoDao;
	private QueryDao queryDao;
	
	private static Log logger = LogFactory.getLog(ExportExcelAction.class);
	private String checkAccount;

	//持有基金
	public ActionForward shareDetail(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		//判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
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
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		logger.info("持有情况 基金账号 = " + fundAccos);
		
		//系统查询所需公共信息
		HashMap sysInfo = (HashMap)request.getSession().getServletContext().getAttribute("initSysInfo");
		if(sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
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
		
		/*Iterator<ShareDetail> iter = list.iterator();
		double totalValue = 0;
		while(iter.hasNext()) {
			ShareDetail shareDetail = iter.next();
			totalValue += RoundTool.roundDouble(Double.parseDouble(shareDetail.getSz()), 2);
		}*/
		
		request.setAttribute("list", list);
		request.setAttribute("totalValue", totalValue);
		request.setAttribute("sysDate", sysDate);
		
		logger.info("totalValue 2 = " + totalValue);
		
		logger.info("基金账号：" + fundAcco + "---导出持有基金excel");
		
		return mapping.findForward("shareDetail");
	}

	//历史申请
	public ActionForward historyApply(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		//判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
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
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		System.out.println("客户身份证号:" + fundAcco);
		
		//系统查询所需公共信息
		HashMap sysInfo = (HashMap)request.getSession().getServletContext().getAttribute("initSysInfo");
		if(sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
		}
		
		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		
		if(!Validate.date10Validate(beginDate) ||  !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "---导出历史申请excel---不合法的日期格式");
			return null;
		}
		
		List list = queryDao.queryHistoryApply(beginDate, endDate, fundAcco, 0, 0, sysInfo, true, null);
		
		request.setAttribute("list", list);
		
		logger.info("基金账号：" + fundAcco + "---导出历史申请excel");
		
		return mapping.findForward("historyApply");
	}
	
	//交易确认
	public ActionForward tradeConfirm(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		//判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
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
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		System.out.println("客户身份证号:" + fundAcco);
		
		HashMap sysInfo = (HashMap)request.getSession().getServletContext().getAttribute("initSysInfo");
		if(sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
		}
		
		String queryCondition = request.getParameter("queryCondition");
		
		String fundCode = request.getParameter("fund");
		String businFlag = request.getParameter("businFlag");
		String agencyNo = request.getParameter("agency");
		String requestno = request.getParameter("requestno");
		
		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		
		if(!Validate.date10Validate(beginDate) ||  !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "---导出交易确认excel---不合法的日期格式");
			return null;
		}
		
		List list = queryDao.queryTradeConfirm(fundAcco, queryCondition, fundCode, agencyNo, businFlag, requestno, beginDate, endDate, 0, 0, sysInfo, true, null);
		
		request.setAttribute("list", list);
		
		logger.info("基金账号：" + fundAcco + "---导出交易确认excel");
		
		return mapping.findForward("tradeConfirm");
	}
	
	//盈亏查询
	public ActionForward income(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		//判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
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
		
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		System.out.println("客户身份证号:" + fundAcco);
		
		HashMap sysInfo = (HashMap)request.getSession().getServletContext().getAttribute("initSysInfo");
		if(sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
		}
		
		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String fundCode = request.getParameter("fund");	//用户买过的基金中的其中一个基金
		
		if(!Validate.date10Validate(beginDate) ||  !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "---导出盈亏查询excel---不合法的日期格式");
			return null;
		}
		
		ArrayList<Income> incomelist = new ArrayList<Income>();
		List initFundInfo = (List)sysInfo.get("initFundInfo");
		
		String[] fundArray = fundAcco.split(",");
		
		for (int k = 0; k < fundArray.length; k++) {
			String temp = fundArray[k].toString();
			if(fundCode == null || fundCode.equals("")) {
				if(initFundInfo != null && initFundInfo.size() > 0) {
					for(int i=0; i<initFundInfo.size(); i++) {
						Income income = new Income();
						String _fundCode = ((InitFundInfo)initFundInfo.get(i)).getFundCode();	//全部旗下的基金
						income = queryDao.queryIncome(temp, _fundCode, beginDate, endDate, sysInfo);
						
						if (income != null
								&& (!((income.getFfShares() * income.getFfNetValue() == 0)
										&& (income.getFfEndShares() * income.getFfEndNetValue() == 0) && (income.getFfProfit() - income.getFfAFare() == 0)))) {
											//equerying/income.jsp   row 337
							incomelist.add(income);
						}
					}
				}
			}
			else {
				Income income = new Income();
				income = queryDao.queryIncome(temp, fundCode, beginDate, endDate, sysInfo);
				if (income != null
						&& (!((income.getFfShares() * income.getFfNetValue() == 0)
								&& (income.getFfEndShares() * income.getFfEndNetValue() == 0) && (income.getFfProfit() - income.getFfAFare() == 0)))) {
									//equerying/income.jsp   row 337
					incomelist.add(income);
				}
			}
		}
		
		request.setAttribute("list", incomelist);
		
		logger.info("基金账号：" + fundAcco + "---导出盈亏查询excel");
		
		return mapping.findForward("income");
	}
	
	//对账单查询
	public ActionForward checkAccount(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		//判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
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
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		
		HashMap sysInfo = (HashMap)request.getSession().getServletContext().getAttribute("initSysInfo");
		if(sysInfo == null) {
			sysInfo = initSysInfoDao.getInitSysInfo();
			request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
		}
		
		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		String taccoInfo = request.getParameter("requestno");
		String accoType = request.getParameter("fundAccoType");
		String tfundAcco = request.getParameter("fundAcco");
		
		
		if(accoType.trim().equalsIgnoreCase("1") && !tfundAcco.trim().equalsIgnoreCase("all")){
			fundAcco = FormatDateTool.stringFormat(tfundAcco);
		}
		System.out.println("客户身份证号:" + fundAcco);//基金账号
		
		if(!Validate.date10Validate(beginDate) ||  !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "---导出对账单查询excel---不合法的日期格式");
			return null;
		}
		
		List<BalanceInfo> balanceInfoList = queryDao.queryBalanceInfo(fundAcco, beginDate, endDate, sysInfo,taccoInfo);
		List<PeriodTrade> periodTradeList = queryDao.queryPeriodTrade(fundAcco, beginDate, endDate, sysInfo,taccoInfo);
		
		String userName = queryDao.queryUserName(fundAcco);	//户名
		java.text.DecimalFormat df2 = new java.text.DecimalFormat("##0.00");
		double sumTotalValue = 0;							//总市值
		double temp=0;
		for(int i=0; i<balanceInfoList.size(); i++) {
			temp  =  Double.parseDouble(df2.format(Double.parseDouble(((BalanceInfo)balanceInfoList.get(i)).getTotalValue()))) + Double.parseDouble(df2.format(Double.parseDouble(((BalanceInfo)balanceInfoList.get(i)).getIncome()))); //加上未付收益
			sumTotalValue += temp;
		}
		
		CheckAccountTitleInfo titleInfo = new CheckAccountTitleInfo();//对账单头部信息
		titleInfo.setUserName(userName);
		titleInfo.setFundAcco(FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "cn"));
		titleInfo.setBeginDate(beginDate);
		titleInfo.setEndDate(endDate);
		titleInfo.setSumTotalValue(String.valueOf(sumTotalValue));
		
		request.setAttribute("balanceInfoList", balanceInfoList);
		request.setAttribute("periodTradeList", periodTradeList);
		request.setAttribute("titleInfo", titleInfo);
		
		logger.info("基金账号：" + fundAcco + "---导出对账单查询excel");
		
		return mapping.findForward(checkAccount);
	}

	//未付收益
	public ActionForward unincome(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		//判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
		String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
		String fundAccos = null;
		String tradeacco = null;
		if(custno != null && custno.length() > 0){
			fundAccos = queryDao.findFundAccoByCustNo(custno);
		} else {
			logger.info("获取客户编号失败 = " + custno);
			return null;
		}

		if (fundAccos == null || fundAccos.length() <= 0) {
			return null;
		}
		String fundAcco = FormatDateTool.stringFormat(fundAccos);
		System.out.println("客户账号:" + fundAcco);
		
		String beginDate = request.getParameter("begindate");
		String endDate = request.getParameter("enddate");
		
		if(!Validate.date10Validate(beginDate) ||  !Validate.date10Validate(endDate)) {
			logger.warn("基金账号：" + fundAcco + "---导出未付收益excel---不合法的日期格式");
			return null;
		}
		
		List list = queryDao.queryUnincome(fundAcco, beginDate, endDate, 0, 0, true, tradeacco);
		
		request.setAttribute("list", list);
		
		logger.info("基金账号：" + fundAcco + "---导出未付收益查询excel");
		
		return mapping.findForward("unincome");
	}
	
	
	//ETF申购赎回清单文件下载
	public ActionForward etfXlsDownload(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			String uri = request.getParameter("uri");
			String date = request.getParameter("today");
			String code = request.getParameter("fundcode");
			
			if (date == null || date.equalsIgnoreCase("")) {
				date = sdf.format(queryDao.getNewDate());
				System.out.println("获取参数:" + date);
			}
			
			String year = date.substring(0, 4);
			String month = date.substring(5, 7);
			String day = date.substring(8, 10);
			String url = uri + "/" + year + "/";
		
		
			if(code != null && !code.equalsIgnoreCase("")){
				EtfList etfList = queryDao.findEtfListByFundCode(code);
				if(etfList != null){
					url += etfList.getFileName() + month + day + "." + etfList.getSuffix();
					request.setAttribute("url", url);
				} else {
					request.setAttribute("codeerr", "下载的文件不存在,请选择其他日期.");
				}
				
			} else {
				request.setAttribute("codeerr", "请输入基金代码.");
			}
			
			System.out.println("ETF申购赎回清单文件下载接收参数: 日期:" + date + "代码:" + code);
			
			request.setAttribute("date", date);
			return mapping.findForward("etf");
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw new Exception("系统异常.");
		}
	}
	
	
	//setter
	public void setInitSysInfoDao(InitSysInfoDao initSysInfoDao) {
		this.initSysInfoDao = initSysInfoDao;
	}
	public void setQueryDao(QueryDao queryDao) {
		this.queryDao = queryDao;
	}
}

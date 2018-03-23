package com.trs.query.struts.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.trs.ids.QueryActor;
import com.trs.ids.UserInfo;
import com.trs.init.dao.InitSysInfoDao;
import com.trs.query.dao.QueryDao;

public class QueryLoginAction extends DispatchAction{
	
	private InitSysInfoDao initSysInfoDao;
	
	public void setInitSysInfoDao(InitSysInfoDao initSysInfoDao) {
		this.initSysInfoDao = initSysInfoDao;
	}

	private QueryDao queryDao;
	public QueryDao getQueryDao() {
		return queryDao;
	}

	public void setQueryDao(QueryDao queryDao) {
		this.queryDao = queryDao;
	}

	
	private static Log logger = LogFactory.getLog(QueryLoginAction.class);
	
	public ActionForward login(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String loginMode = request.getParameter("hloginMode");
		String loginName = request.getParameter("hloginId");
		String cardType = request.getParameter("hzjtype");
		String password = request.getParameter("pwd");
		UserInfo userInfo = null;
		if(loginMode != null && !loginMode.trim().equals("")){
			userInfo = queryDao.queryLogin(loginName, cardType, loginMode);
			
			if(userInfo != null){
				request.getSession().setAttribute(QueryActor.LOGIN_FLAG, userInfo);
				String fundAccos = queryDao.findFundAccoByCustNo(userInfo.getCustomNo());
				
				if(fundAccos != null && fundAccos.length() > 0){
					System.out.println(fundAccos);
					request.getSession().setAttribute("fundAccos", fundAccos);
					request.getSession().setAttribute("CUSTNO", userInfo.getCustomNo());
					return mapping.findForward("success");
				}
			}
		}
		return mapping.findForward("error");
	}
}

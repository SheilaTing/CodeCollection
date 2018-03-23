package com.trs.query.struts.action;

import java.awt.Color;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.ssh.util.DateTools;
import com.ssh.util.RoundTool;
import com.trs.ids.UserInfo;
import com.trs.init.dao.InitSysInfoDao;
import com.trs.query.beans.BalanceInfo;
import com.trs.query.beans.HistoryApply;
import com.trs.query.beans.PeriodTrade;
import com.trs.query.beans.ShareDetail;
import com.trs.query.beans.TradeConfirm;
import com.trs.query.dao.QueryDao;
import com.trs.util.DictionaryChange;
import com.trs.util.FormatDateTool;
import com.trs.util.PdfTool;
import com.trs.util.Validate;
import com.ssh.util.CheckData;

/**
 * @author tan.hongyan
 * @version: Nov 24, 2009 9:44:26 PM
 * @desc:
 */
public class ExportPdfAction extends DispatchAction {

    private InitSysInfoDao initSysInfoDao;
    private QueryDao queryDao;

    private static Log logger = LogFactory.getLog(ExportPdfAction.class);

    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat();
        double s = 1.82640398484E7;
        System.out.println(df.format(s));
    }

    //持有基金
    public ActionForward shareDetail(ActionMapping mapping, ActionForm form,
                                     HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
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
        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo();
            request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
        }

        String sysDate = queryDao.findIncomeDate();

        List<ShareDetail> list = queryDao.queryShareDetail(fundAcco, sysInfo);

        DecimalFormat df1 = new java.text.DecimalFormat("##0.00");
        double totalValue = 0d;

        for (ShareDetail sd : list) {
            totalValue += Double.parseDouble(sd.getSz());
            System.out.println("前市值:" + Double.parseDouble(sd.getSz()) + " 未分配收益:" + Double.parseDouble(sd.getUnassign()));
            System.out.println("后市值:" + df1.format(Double.parseDouble(sd.getSz())) + " 未分配收益:" + df1.format(Double.parseDouble(sd.getUnassign())));
        }

		/*Iterator<ShareDetail> iter = list.iterator();
        double totalValue = 0;
		while(iter.hasNext()) {
			ShareDetail shareDetail = iter.next();
			totalValue += RoundTool.roundDouble(Double.parseDouble(shareDetail.getSz()), 2);
		}*/

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "上投摩根基金持有状况\n";

        // 生成8列的表格
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);    //table 100%
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle = {"基金", "销售商", "数量", "最新净值", "市值", "基金状态", "分红方式", "收费方式", "未分配收益"};

        //表头颜色
        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        //表格内容
        for (int i = 0; i < list.size(); i++) {
            ShareDetail shareDetail = list.get(i);
            if (!"".equals(shareDetail.getShareClass()) && !"--".equals(shareDetail.getShareClass())) {
                table.addCell(new Paragraph(shareDetail.getFundName() + "\n(" + shareDetail.getShareClass() + "类客户)", contentFont));
            } else {
                table.addCell(new Paragraph(shareDetail.getFundName(), contentFont));
            }
            table.addCell(new Paragraph(shareDetail.getAgencyName(), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(shareDetail.getYe())), contentFont));
            table.addCell(new Paragraph(df2.format(Double.parseDouble(shareDetail.getNetValue())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(shareDetail.getSz())), contentFont));
            table.addCell(new Paragraph(shareDetail.getTodayStatus(), contentFont));
            table.addCell(new Paragraph(shareDetail.getBonusType(), contentFont));
            table.addCell(new Paragraph(shareDetail.getChargeType(), contentFont));
            double unassign = Double.parseDouble(shareDetail.getUnassign());
            if (unassign != 0) {
                table.addCell(new Paragraph(df.format(unassign) + "\n截止（截止\n" + sysDate + "）", contentFont));
            } else {
                table.addCell(new Paragraph("", contentFont));
            }
        }

        Paragraph graph = new Paragraph("市价总值：" + df.format(totalValue) + "元", contentFont);

        //body
        Element[] addContent = new Element[2];
        addContent[0] = table;
        addContent[1] = graph;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf("hundFundPDF.pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出持有基金pdf");

        return null;
    }

    //持有基金英文版pdf
    public ActionForward shareDetail_en(ActionMapping mapping, ActionForm form,
                                        HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
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
        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo_en");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo_en();
            request.getSession().getServletContext().setAttribute("initSysInfo_en", sysInfo);
        }

        String sysDate = queryDao.findIncomeDate();

        List<ShareDetail> list = queryDao.queryShareDetail_en(fundAcco, sysInfo);

        DecimalFormat df1 = new java.text.DecimalFormat("##0.00");
        double totalValue = 0d;

        for (ShareDetail sd : list) {
            totalValue += Double.parseDouble(df1.format(Double.parseDouble(sd.getSz())));
            System.out.println("前市值:" + Double.parseDouble(sd.getSz()) + " 未分配收益:" + Double.parseDouble(sd.getUnassign()));
            System.out.println("后市值:" + df1.format(Double.parseDouble(sd.getSz())) + " 未分配收益:" + df1.format(Double.parseDouble(sd.getUnassign())));
        }

		/*Iterator<ShareDetail> iter = list.iterator();
        double totalValue = 0;
		while(iter.hasNext()) {
			ShareDetail shareDetail = iter.next();
			totalValue += RoundTool.roundDouble(Double.parseDouble(shareDetail.getSz()), 2);
		}*/

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "Account details\n";

        // 生成8列的表格
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);    //table 100%
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle = {"Fund Name", "Distribution\n Channel", "Unit", "NAV(RMB)", "Market\n Value", "Status", "Dividend\n Type", "Charge Method", "Unpaid\n Distributions"};

        //表头颜色
        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        //表格内容
        for (int i = 0; i < list.size(); i++) {
            ShareDetail shareDetail = list.get(i);
            if (!"".equals(shareDetail.getShareClass()) && !"--".equals(shareDetail.getShareClass())) {
                table.addCell(new Paragraph(shareDetail.getFundName() + "\n(Class" + shareDetail.getShareClass() + ")", contentFont));
            } else {
                table.addCell(new Paragraph(shareDetail.getFundName(), contentFont));
            }
            table.addCell(new Paragraph(shareDetail.getAgencyName(), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(shareDetail.getYe())), contentFont));
            table.addCell(new Paragraph(df2.format(Double.parseDouble(shareDetail.getNetValue())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(shareDetail.getSz())), contentFont));
            table.addCell(new Paragraph(shareDetail.getTodayStatus(), contentFont));
            table.addCell(new Paragraph(shareDetail.getBonusType(), contentFont));
            table.addCell(new Paragraph(shareDetail.getChargeType(), contentFont));
            double unassign = Double.parseDouble(shareDetail.getUnassign());
            if (unassign != 0) {
                table.addCell(new Paragraph(df.format(unassign) + "\n（End\n" + sysDate + "）", contentFont));
            } else {
                table.addCell(new Paragraph("", contentFont));
            }
        }

        Paragraph graph = new Paragraph("Total market value：" + df.format(totalValue) + " RMB", contentFont);

        //body
        Element[] addContent = new Element[2];
        addContent[0] = table;
        addContent[1] = graph;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf_en("hundFundPDF.pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出持有基金pdf");

        return null;
    }

    //历史申请
    public ActionForward historyApply(ActionMapping mapping, ActionForm form,
                                      HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
            fundAccos = queryDao.findFundAccoByCustNo(custno);
        } else {
            logger.info("获取客户编号失败 = " + custno);
            return null;
        }

        if (fundAccos == null || fundAccos.length() <= 0) {
            return null;
        }

        String fundAcco = FormatDateTool.stringFormat(fundAccos);
        logger.info("历史申请 基金账号 = " + fundAccos);

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo();
            request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
        }

        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));

        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        //查询数据
        List<HistoryApply> list = queryDao.queryHistoryApply(beginDate, endDate, fundAcco, 0, 0, sysInfo, true, null);
        String tradeNos = "";
        if (list != null && list.size() > 0) {
            for (HistoryApply apply : list) {
                if (tradeNos.indexOf(apply.getTradeAcco()) == -1) {
                    tradeNos += apply.getTradeAcco() + ",";
                }
            }
            tradeNos = tradeNos.substring(0, tradeNos.length() - 1);
        }
        String tradeNo = tradeNos;
        String userName = queryDao.queryUserName(fundAcco);    //户名


        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "上投摩根基金历史申请查询";

        String accoName = "账户名称：" + userName + "\n";

        String account = FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "cn") + "\n";

        String tradeAcco = "交易账号：" + tradeNo + "\n";

        String tradeTime = "交易时间段：" + beginDate.replaceAll("-", "") + " ~ " + endDate.replaceAll("-", "") + "\n\n";
        Paragraph graph = new Paragraph(accoName + account + tradeAcco + tradeTime, contentFont);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);

        String[] tableTitle = {"基金名称", "业务名称", "申请份额", "申请金额", "申请日期", "确认结果"};

        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");

        //数据
        for (int i = 0; i < list.size(); i++) {
            HistoryApply historyApply = list.get(i);
            table.addCell(new Paragraph(historyApply.getFundName(), contentFont));
            table.addCell(new Paragraph(historyApply.getBusinName(), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(historyApply.getApplyShares())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(historyApply.getApplyBalance())), contentFont));
            table.addCell(new Paragraph(historyApply.getApplyDate(), contentFont));
            table.addCell(new Paragraph(historyApply.getStatus(), contentFont));
        }

        //body
        Element[] addContent = new Element[2];
        addContent[0] = graph;
        addContent[1] = table;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf("historyPDF " + beginDate.replaceAll("-", "") + "~" + endDate.replaceAll("-", "") + ".pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出历史申请pdf");

        return null;
    }

    //历史申请英文版pdf
    public ActionForward historyApply_en(ActionMapping mapping, ActionForm form,
                                         HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
            fundAccos = queryDao.findFundAccoByCustNo(custno);
        } else {
            logger.info("获取客户编号失败 = " + custno);
            return null;
        }

        if (fundAccos == null || fundAccos.length() <= 0) {
            return null;
        }

        String fundAcco = FormatDateTool.stringFormat(fundAccos);
        logger.info("历史申请 基金账号 = " + fundAccos);

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo_en");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo_en();
            request.getSession().getServletContext().setAttribute("initSysInfo_en", sysInfo);
        }

        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));

        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        //查询数据
        List<HistoryApply> list = queryDao.queryHistoryApply_en(beginDate, endDate, fundAcco, 0, 0, sysInfo, true, null);
        String tradeNos = "";
        if (list != null && list.size() > 0) {
            for (HistoryApply apply : list) {
                if (tradeNos.indexOf(apply.getTradeAcco()) == -1) {
                    tradeNos += apply.getTradeAcco() + ",";
                }
            }
            tradeNos = tradeNos.substring(0, tradeNos.length() - 1);
        }
        String tradeNo = tradeNos;
        String userName = queryDao.queryUserName(fundAcco);    //户名


        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "History application inquires";

        String accoName = "UserName：" + userName + "\n";
        String account = FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "en") + "\n";
        String tradeAcco = "Trade Number:" + tradeNo + "\n";

        String tradeTime = "Trading Time：" + beginDate.replaceAll("-", "") + " ~ " + endDate.replaceAll("-", "") + "\n\n";
        Paragraph graph = new Paragraph(accoName + account + tradeAcco + tradeTime, contentFont);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle = {"Fund name", "Transaction\n Type", "Unit", "Amount", "Application\n Date", "Status"};

        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");

        //数据
        for (int i = 0; i < list.size(); i++) {
            HistoryApply historyApply = list.get(i);
            table.addCell(new Paragraph(historyApply.getFundName(), contentFont));
            table.addCell(new Paragraph(historyApply.getBusinName(), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(historyApply.getApplyShares())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(historyApply.getApplyBalance())), contentFont));
            table.addCell(new Paragraph(historyApply.getApplyDate(), contentFont));
            table.addCell(new Paragraph(historyApply.getStatus(), contentFont));
        }

        //body
        Element[] addContent = new Element[2];
        addContent[0] = graph;
        addContent[1] = table;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf_en("historyPDF " + beginDate.replaceAll("-", "") + "~" + endDate.replaceAll("-", "") + ".pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出历史申请pdf");

        return null;
    }

    //交易确认
    public ActionForward tradeConfirm(ActionMapping mapping, ActionForm form,
                                      HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
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

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo");

        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo();
            request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
        }

        String queryCondition = request.getParameter("queryCondition");

        String fundCode = request.getParameter("fund");
        String businFlag = request.getParameter("businFlag");
        String agencyNo = request.getParameter("agency");
        String requestno = request.getParameter("requestno");

        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));

        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        //查询数据
        List<TradeConfirm> list = queryDao.queryTradeConfirm(fundAcco, queryCondition, fundCode, agencyNo, businFlag, requestno, beginDate, endDate, 0, 0, sysInfo, true, null);

        String userName = queryDao.queryUserName(fundAcco);    //户名
        String tradeNo = queryDao.queryTradeAcco(fundAcco); //交易账号

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "上投摩根基金交易确认书";

        String accoName = "账户名称：" + userName + "\n";
        String account = FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "cn") + "\n";
        String tradeAccos = "";
        if (list != null && list.size() > 0) {
            for (TradeConfirm confirm : list) {
                if (tradeAccos.indexOf(confirm.getTradeAcco()) == -1) {
                    tradeAccos += confirm.getTradeAcco() + ",";
                }
            }
            tradeAccos = tradeAccos.substring(0, tradeAccos.length() - 1);
        }
        String tradeAcco = "交易账号：" + tradeAccos + "\n";
        String tradeTime = "交易时间段：" + beginDate.replaceAll("-", "") + " ~ " + endDate.replaceAll("-", "") + "\n\n";
        Paragraph graph = new Paragraph(accoName + account + tradeAcco + tradeTime, contentFont);

        PdfPTable table = new PdfPTable(13);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle = {"申请日期", "确认日期", "基金", "销售商", "申请编号", "单位净值",
                "业务类别", "申请金额", "确认金额", "申请份额", "确认份额", "手续费", "确认信息"};

        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        for (int i = 0; i < list.size(); i++) {
            TradeConfirm tradeConfirm = list.get(i);
            table.addCell(new Paragraph(tradeConfirm.getDate(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getConfirmDate(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getFundName(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getAgencyName(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getRequestno(), contentFont));
            table.addCell(new Paragraph(df2.format(Double.parseDouble(tradeConfirm.getNetValue())), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getBusinFlag(), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getBalance().equals("--") ? "0" : tradeConfirm.getBalance())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getConfirmBalance().equals("--") ? "0" : tradeConfirm.getConfirmBalance())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getShares().equals("--") ? "0" : tradeConfirm.getShares())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getConfirmShares().equals("--") ? "0" : tradeConfirm.getConfirmShares())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getSxf().equals("--") ? "0" : tradeConfirm.getSxf())), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getStatus(), contentFont));
        }

        //body
        Element[] addContent = new Element[2];
        addContent[0] = graph;
        addContent[1] = table;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf("tradePDF " + beginDate.replaceAll("-", "") + "~" + endDate.replaceAll("-", "") + ".pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出交易确认pdf");

        return null;
    }

    //交易确认英文版pdf
    public ActionForward tradeConfirm_en(ActionMapping mapping, ActionForm form,
                                         HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
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

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo_en");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo_en();
            request.getSession().getServletContext().setAttribute("initSysInfo_en", sysInfo);
        }

        String queryCondition = request.getParameter("queryCondition");

        String fundCode = request.getParameter("fund");
        String businFlag = request.getParameter("businFlag");
        String agencyNo = request.getParameter("agency");
        String requestno = request.getParameter("requestno");

        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));

        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        //查询数据
        List<TradeConfirm> list = queryDao.queryTradeConfirm_en(fundAcco, queryCondition, fundCode, agencyNo, businFlag, requestno, beginDate, endDate, 0, 0, sysInfo, true, null);

        String userName = queryDao.queryUserName(fundAcco);    //户名
        String tradeNo = queryDao.queryTradeAcco(fundAcco); //交易账号

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "Transaction Confirmation";

        String accoName = "UserName：" + userName + "\n";
        String account = FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "en") + "\n";
        String tradeAccos = "";
        if (list != null && list.size() > 0) {
            for (TradeConfirm confirm : list) {
                if (tradeAccos.indexOf(confirm.getTradeAcco()) == -1) {
                    tradeAccos += confirm.getTradeAcco() + ",";
                }
            }
            tradeAccos = tradeAccos.substring(0, tradeAccos.length() - 1);
        }
        String tradeAcco = "Trade Number：" + tradeAccos + "\n";
        String tradeTime = "Trading Time：" + beginDate.replaceAll("-", "") + " ~ " + endDate.replaceAll("-", "") + "\n\n";
        Paragraph graph = new Paragraph(accoName + account + tradeAcco + tradeTime, contentFont);

        PdfPTable table = new PdfPTable(13);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(table.ALIGN_CENTER);
        String[] tableTitle = {"Application\n Date", "Confirmation\n Date", "Fund", "Distribution\n Channel", "Number", "NAV",
                "Transaction\n Type", "Applied Amount", "Confirmed\n Amount", "Applied\n Unit", "Confirmed Unit", "Fees", "Confirmation"};

        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        for (int i = 0; i < list.size(); i++) {
            TradeConfirm tradeConfirm = list.get(i);
            table.addCell(new Paragraph(tradeConfirm.getDate(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getConfirmDate(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getFundName(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getAgencyName(), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getRequestno(), contentFont));
            table.addCell(new Paragraph(df2.format(Double.parseDouble(tradeConfirm.getNetValue())), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getBusinFlag(), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getBalance().equals("--") ? "0" : tradeConfirm.getBalance())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getConfirmBalance().equals("--") ? "0" : tradeConfirm.getConfirmBalance())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getShares().equals("--") ? "0" : tradeConfirm.getShares())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getConfirmShares().equals("--") ? "0" : tradeConfirm.getConfirmShares())), contentFont));
            table.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getSxf().equals("--") ? "0" : tradeConfirm.getSxf())), contentFont));
            table.addCell(new Paragraph(tradeConfirm.getStatus(), contentFont));
            System.out.println(tradeConfirm.getStatus());
        }

        //body
        Element[] addContent = new Element[2];
        addContent[0] = graph;
        addContent[1] = table;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf_en("tradePDF " + beginDate.replaceAll("-", "") + "~" + endDate.replaceAll("-", "") + ".pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出交易确认pdf");

        return null;
    }

    //交易确认书下载
    public ActionForward confirmDetail(ActionMapping mapping, ActionForm form,
                                       HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
            fundAccos = queryDao.findFundAccoByCustNo(custno);
        } else {
            logger.info("获取客户编号失败 = " + custno);
            return null;
        }

        if (fundAccos == null || fundAccos.length() <= 0) {
            return null;
        }

        String fundAcco = FormatDateTool.stringFormat(fundAccos);
        System.out.println("交易确认书下载");

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo();
            request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
        }

        String cserialno = CheckData.c(request.getParameter("cno"));

        logger.info("TA确认号：" + cserialno);


        List<TradeConfirm> list = queryDao.queryConfirmDetail(cserialno, sysInfo);
        String fa = "'" + list.get(0).getFundAcco() + "'";
        String userName = queryDao.queryUserName(fa);    //户名
        //String tradeAcco = queryDao.queryTradeAcco(fa); //交易账号

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        String serialno = "0";
        if (list != null && list.size() > 0) {
            serialno = list.get(0).getSerialno();
            logger.info("交易确认书---申请流水号：" + serialno);
        }

        //title
        String titleStr = "上投摩根基金交易确认书";
        Element[] addContent = new Element[4];

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        //--->start
        Paragraph graph = new Paragraph("账户名称：" + userName + "\n" + FormatDateTool.accountCheck(list.get(0).getFundAcco(), "cn") + "\n交易账号：" + list.get(0).getTradeAcco() + "\n申请流水号：" + serialno + "\n确认流水号：" + cserialno + "\n\n" +
                "账户确认明细：\n\n", contentFont);
        addContent[0] = graph;
        //------>

        PdfPTable table_1 = new PdfPTable(8);
        table_1.setWidthPercentage(100);
        table_1.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle1 = {"基金代码", "基金名称", "申请日期", "确认日期", "交易类别", "收费方式", "申请金额", "申请份额"};

        for (String ttl : tableTitle1) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table_1.addCell(cell);
        }

        if (!list.isEmpty()) {
            TradeConfirm tradeConfirm = list.get(0);
            table_1.addCell(new Paragraph(tradeConfirm.getFundCode(), contentFont));
            table_1.addCell(new Paragraph(tradeConfirm.getFundName(), contentFont));
            table_1.addCell(new Paragraph(tradeConfirm.getDate(), contentFont));
            table_1.addCell(new Paragraph(tradeConfirm.getConfirmDate(), contentFont));
            table_1.addCell(new Paragraph(tradeConfirm.getBusinFlag(), contentFont));
            table_1.addCell(new Paragraph(tradeConfirm.getChargeType(), contentFont));
            table_1.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getBalance())), contentFont));
            table_1.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getShares())), contentFont));
        }
        addContent[1] = table_1;
        //--------->

        graph = new Paragraph("\n\n", contentFont);
        addContent[2] = graph;
        //------------>

        PdfPTable table_2 = new PdfPTable(8);
        table_2.setWidthPercentage(100);
        table_2.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle2 = {"确认结果", "确认金额", "确认份额", "成交净值", "手续费", "未付收益", "网点(销售商)", "备注"};

        for (String ttl : tableTitle2) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table_2.addCell(cell);
        }

        if (!list.isEmpty()) {
            TradeConfirm tradeConfirm = list.get(0);
            table_2.addCell(new Paragraph(tradeConfirm.getStatus(), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getConfirmBalance())), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getConfirmShares())), contentFont));
            table_2.addCell(new Paragraph(df2.format(Double.parseDouble(tradeConfirm.getNetValue())), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getSxf())), contentFont));
            logger.info("交易确认书---未付收益 前：" + tradeConfirm.getConfirmIncome());
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(tradeConfirm.getConfirmIncome())), contentFont));
            logger.info("交易确认书---未付收益 后：" + df.format(Double.parseDouble(tradeConfirm.getConfirmIncome())));
            table_2.addCell(new Paragraph(tradeConfirm.getAgencyName(), contentFont));
            table_2.addCell(new Paragraph(tradeConfirm.getMemo(), contentFont));
        }
        addContent[3] = table_2;
        //--------------->

        PdfTool pdfTool = new PdfTool();
        SecureRandom secureRandom = new SecureRandom();
        String surfix = String.valueOf(secureRandom.nextLong());
        pdfTool.writePdf("historyConfirmation" + surfix + ".pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出交易确认书");

        return null;
    }

    //红利发放确认书
    public ActionForward bonusConfirm(ActionMapping mapping, ActionForm form,
                                      HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
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

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo();
            request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
        }

        List initAgencyInfo = (List) sysInfo.get("initAgencyInfo");
        String cserialno = CheckData.c(request.getParameter("cno"));

        logger.info("TA确认号：" + cserialno);


        List list = queryDao.queryBonusConfirm(cserialno, sysInfo);
        String fa = "'" + ((Map) list.get(0)).get("C_FUNDACCO").toString() + "'";
        String userName = queryDao.queryUserName(fa);    //户名
        //String tradeAcco = queryDao.queryTradeAcco(fa); //交易账号

        List initFundInfo = (List) sysInfo.get("initFundInfo");

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "上投摩根基金红利发放确认书";
        Element[] addContent = new Element[4];

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        String space = " ";
        for (int i = 0; i < 15; i++) {
            space += " ";
        }

        //--->start
        Paragraph graph = new Paragraph("账户名称：" + userName + "\n" + FormatDateTool.accountCheck(((Map) list.get(0)).get("C_FUNDACCO").toString(), "cn") + "\n交易账号：" + ((Map) list.get(0)).get("C_TRADEACCO").toString() + "\n\n红利发放明细：\n\n", contentFont);
        addContent[0] = graph;
        //------>

        PdfPTable table_1 = new PdfPTable(7);
        table_1.setWidthPercentage(100);
        table_1.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle1 = {"基金代码", "基金名称", "确认编号", "权益登记日", "分红日", "红利转投确认日期", "交易类别"};

        for (String ttl : tableTitle1) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table_1.addCell(cell);
        }

        if (list != null && !list.isEmpty()) {
            HashMap map = (HashMap) list.get(0);
            table_1.addCell(new Paragraph(map.get("C_FUNDCODE") == null ? "" : map.get("C_FUNDCODE").toString(), contentFont));
            table_1.addCell(new Paragraph(map.get("C_FUNDCODE") == null ? "" : DictionaryChange.changeFundInfo(initFundInfo, map.get("C_FUNDCODE").toString()), contentFont));
            table_1.addCell(new Paragraph(map.get("c_cserialno".toUpperCase()) == null ? "" : map.get("c_cserialno".toUpperCase()).toString(), contentFont));
            table_1.addCell(new Paragraph(map.get("d_regdate".toUpperCase()) == null ? "" : map.get("d_regdate".toUpperCase()).toString(), contentFont));
            table_1.addCell(new Paragraph(map.get("d_date".toUpperCase()) == null ? "" : map.get("d_date".toUpperCase()).toString(), contentFont));
            table_1.addCell(new Paragraph(map.get("d_lastdate".toUpperCase()) == null ? "" : map.get("d_lastdate".toUpperCase()).toString(), contentFont));

            String flag = "";
            if (map.get("C_FLAG") != null) {
                if ("0".equals(map.get("C_FLAG").toString())) {
                    flag = "红利再投资";
                }
                if ("1".equals(map.get("C_FLAG").toString())) {
                    flag = "现金红利";
                }
            }

            table_1.addCell(new Paragraph(flag, contentFont));
        }
        addContent[1] = table_1;
        //--------->

        graph = new Paragraph("\n\n", contentFont);
        addContent[2] = graph;
        //------------>

        PdfPTable table_2 = new PdfPTable(10);
        table_2.setWidthPercentage(100);
        table_2.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle2 = {"可分红份额", "冻结金额", "冻结份额", "每份派息金额", "红利转投日净值", "现金红利", "红利转投份额", "币种", "业绩报酬", "网点号\n(销售商)"};

        for (String ttl : tableTitle2) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table_2.addCell(cell);
        }

        if (list != null && !list.isEmpty()) {
            HashMap map = (HashMap) list.get(0);
            table_2.addCell(new Paragraph(map.get("F_TOTALSHARE") == null ? "" : map.get("F_TOTALSHARE").toString(), contentFont));
            table_2.addCell(new Paragraph(map.get("F_FROZENBALANCE") == null ? "" : map.get("F_FROZENBALANCE").toString(), contentFont));
            table_2.addCell(new Paragraph(map.get("F_FROZENSHARES") == null ? "" : map.get("F_FROZENSHARES").toString(), contentFont));
            table_2.addCell(new Paragraph(map.get("F_UNITPROFIT") == null ? "" : map.get("F_UNITPROFIT").toString(), contentFont));
            table_2.addCell(new Paragraph(map.get("F_NETVALUE") == null ? "" : map.get("F_NETVALUE").toString(), contentFont));
            table_2.addCell(new Paragraph(map.get("F_REALBALANCE") == null ? "" : map.get("F_REALBALANCE").toString(), contentFont));
            table_2.addCell(new Paragraph(map.get("F_REALSHARES") == null ? "" : map.get("F_REALSHARES").toString(), contentFont));
            table_2.addCell(new Paragraph("人民币", contentFont));
            table_2.addCell(new Paragraph(map.get("F_STAMPTAX") == null ? "" : map.get("F_STAMPTAX").toString(), contentFont));
            table_2.addCell(new Paragraph(map.get("C_AGENCYNO") == null ? "" : DictionaryChange.changeAgency(initAgencyInfo, map.get("C_AGENCYNO").toString()), contentFont));
        }
        addContent[3] = table_2;
        //--------------->

        PdfTool pdfTool = new PdfTool();
        SecureRandom secureRandom = new SecureRandom();
        String surfix = String.valueOf(secureRandom.nextLong());
        pdfTool.writePdf("historyBonusConfirmation" + surfix + ".pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出红利发放确认书");

        return null;
    }

    //对账单查询
    public ActionForward checkAccount(ActionMapping mapping, ActionForm form,
                                      HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
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

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo();
            request.getSession().getServletContext().setAttribute("initSysInfo", sysInfo);
        }

        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));
        String taccoInfo = request.getParameter("requestno");
        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        List<BalanceInfo> balanceInfoList = queryDao.queryBalanceInfo(fundAcco, beginDate, endDate, sysInfo, taccoInfo);
        List<PeriodTrade> periodTradeList = queryDao.queryPeriodTrade(fundAcco, beginDate, endDate, sysInfo, taccoInfo);

        beginDate = beginDate.replaceAll("-", "");
        endDate = endDate.replaceAll("-", "");

        String userName = queryDao.queryUserName(fundAcco);    //户名

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "";
        Element[] addContent = new Element[6];

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        //--->start
        Paragraph graph = new Paragraph("对账单\nStatement of Account\n\n\n账户余额报告\nAccount Balance\n", contentFont);
        graph.setAlignment(Paragraph.ALIGN_CENTER);
        addContent[0] = graph;
        //------>

        graph = new Paragraph("账户名称：" + userName + "\n" + FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "cn") + "\n\n" +
                "截至" + endDate + "的投资组合\n" + "Portfolios by the time of " + endDate + "\n\n", contentFont);
        addContent[1] = graph;
        //--------->

        PdfPTable table_1 = new PdfPTable(7);
        table_1.setWidthPercentage(110);
        table_1.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);

        String[] tableTitle1 = {"基金名称\n Fund Name", "基金余额(份)\n Units Held",
                "单位净值(元)\n NAV per\nUnit(RMB)", "当前市值(元)\n Market\n Value(RMB)",
                "分红方式\n Dividend\n PaymentWay", "未付收益\nUnpaid\nDistributions\n (RMB)",
                "交易渠道\n Transaction\n  Channel"};

        for (String ttl : tableTitle1) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table_1.addCell(cell);
        }

        for (int i = 0; i < balanceInfoList.size(); i++) {
            BalanceInfo balanceInfo = balanceInfoList.get(i);
            logger.info("基金余额信息导出PDF--->基金名称：" + balanceInfo.getFundName() + "--->表TSHARECURRENTS-->字段c_sharetype：[" + balanceInfo.getShareClass() + "]");
            /** by thy: 2010-03-11
             if(balanceInfo.getShareClass()!=null && !"".equals(balanceInfo.getShareClass().trim())) {
             table_1.addCell(new Paragraph(balanceInfo.getFundName() + "(" + balanceInfo.getShareClass() + "类客户)", contentFont));
             }
             else {
             table_1.addCell(new Paragraph(balanceInfo.getFundName(), contentFont));
             }
             */
            table_1.addCell(new Paragraph(balanceInfo.getFundName(), contentFont));
            table_1.addCell(new Paragraph(df.format(Double.parseDouble(balanceInfo.getUsableShares())), contentFont));
            table_1.addCell(new Paragraph(df2.format(Double.parseDouble(balanceInfo.getNetValue())), contentFont));
            table_1.addCell(new Paragraph(df.format(
                    Double.parseDouble(balanceInfo.getTotalValue()) +
                            Double.parseDouble(balanceInfo.getIncome())
            ), contentFont));
            table_1.addCell(new Paragraph(balanceInfo.getBonusType(), contentFont));
            table_1.addCell(new Paragraph(df.format(Double.parseDouble(balanceInfo.getIncome())), contentFont));
            table_1.addCell(new Paragraph(balanceInfo.getAgencyno(), contentFont));
        }

        addContent[2] = table_1;
        //------------>

        graph = new Paragraph("\n\n交易对账单\nTransaction Account\n", contentFont);
        graph.setAlignment(Paragraph.ALIGN_CENTER);
        addContent[3] = graph;
        //--------------->

        graph = new Paragraph(beginDate + " 至 " + endDate + " 期内完成的交易: \n" +
                "Transaction conducted from begindate " + beginDate + " to " + endDate + "\n\n", contentFont);
        addContent[4] = graph;
        //------------------>

        PdfPTable table_2 = new PdfPTable(9);
        table_2.setWidthPercentage(110);
        table_2.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);

        String[] tableTitle2 = {"确认日期\n Value Date", "基金名称\n Fund Name",
                "交易渠道\n Transaction\n Channel", "交易类型\n Transaction\n Type",
                "成交份额\n(份)\n No. of Units Transacted",
                "单位净值\n(元)\n NAV per\nUnit\n(RMB)", "兑付/未付\n 收益(元)\n Paid/Unpaid\n Distributions\n (RMB)",
                "手续费(元)\n Handing\n Charges\n(RMB)", "清算金额\n(元)\nNet\nConsideration\n(RMB)"};

        for (String ttl : tableTitle2) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table_2.addCell(cell);
        }

        for (int i = 0; i < periodTradeList.size(); i++) {
            PeriodTrade periodTrade = periodTradeList.get(i);
            table_2.addCell(new Paragraph(periodTrade.getConfirmDate(), contentFont));
            table_2.addCell(new Paragraph(periodTrade.getFundName(), contentFont));
            table_2.addCell(new Paragraph(periodTrade.getAgencyno(), contentFont));
            table_2.addCell(new Paragraph(periodTrade.getBusinFlag(), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(periodTrade.getConfirmShares())), contentFont));
            table_2.addCell(new Paragraph(df2.format(Double.parseDouble(periodTrade.getNetValue())), contentFont));
            table_2.addCell(new Paragraph(df2.format(Double.parseDouble(periodTrade.getIncome())), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(periodTrade.getSxf())), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(periodTrade.getConfirmBalance())), contentFont));
        }
        addContent[5] = table_2;
        //--------------------->end

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf("comparePDF " + beginDate.replaceAll("-", "") + "~" + endDate.replaceAll("-", "") + ".pdf", response, titleStr, titleFont, contentFont, addContent);


        return null;
    }

    //对账单查询英文版pdf
    public ActionForward checkAccount_en(ActionMapping mapping, ActionForm form,
                                         HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
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

        HashMap sysInfo = (HashMap) request.getSession().getServletContext().getAttribute("initSysInfo_en");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo_en();
            request.getSession().getServletContext().setAttribute("initSysInfo_en", sysInfo);
        }

        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));
        String taccoInfo = request.getParameter("requestno");
        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        List<BalanceInfo> balanceInfoList = queryDao.queryBalanceInfo_en(fundAcco, beginDate, endDate, sysInfo, taccoInfo);
        List<PeriodTrade> periodTradeList = queryDao.queryPeriodTrade_en(fundAcco, beginDate, endDate, sysInfo, taccoInfo);

        beginDate = beginDate.replaceAll("-", "");
        endDate = endDate.replaceAll("-", "");

        String userName = queryDao.queryUserName(fundAcco);    //户名

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        //title
        String titleStr = "";
        Element[] addContent = new Element[6];

        DecimalFormat df = new DecimalFormat(",##0.00");
        DecimalFormat df2 = new DecimalFormat(",##0.0000");

        //--->start
        Paragraph graph = new Paragraph("Statement of Account\n\n\nAccount Balance\n", contentFont);
        graph.setAlignment(Paragraph.ALIGN_CENTER);
        addContent[0] = graph;
        //------>

        graph = new Paragraph("UserName：" + userName + "\n" + FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "en") + "\n\n" +
                "Portfolios by the time of " + endDate + "\n\n", contentFont);
        addContent[1] = graph;
        //--------->

        PdfPTable table_1 = new PdfPTable(7);
        table_1.setWidthPercentage(110);
        table_1.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle1 = {"Fund Name", "Units Held",
                "NAV per\nUnit\n(RMB)", "Market Value\n(RMB)",
                "Dividend\n PaymentWay", "Unpaid\n Distributions\n(RMB)",
                "Transaction\n Channel"};

        for (String ttl : tableTitle1) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table_1.addCell(cell);
        }

        for (int i = 0; i < balanceInfoList.size(); i++) {
            BalanceInfo balanceInfo = balanceInfoList.get(i);
            logger.info("基金余额信息导出PDF--->基金名称：" + balanceInfo.getFundName() + "--->表TSHARECURRENTS-->字段c_sharetype：[" + balanceInfo.getShareClass() + "]");
            /** by thy: 2010-03-11
             if(balanceInfo.getShareClass()!=null && !"".equals(balanceInfo.getShareClass().trim())) {
             table_1.addCell(new Paragraph(balanceInfo.getFundName() + "(" + balanceInfo.getShareClass() + "类客户)", contentFont));
             }
             else {
             table_1.addCell(new Paragraph(balanceInfo.getFundName(), contentFont));
             }
             */
            logger.info("***********+++++" + balanceInfo.getFundName() + balanceInfo.getUsableShares());
            table_1.addCell(new Paragraph(balanceInfo.getFundName(), contentFont));
            table_1.addCell(new Paragraph(df.format(Double.parseDouble(balanceInfo.getUsableShares())), contentFont));
            table_1.addCell(new Paragraph(df2.format(Double.parseDouble(balanceInfo.getNetValue())), contentFont));
            table_1.addCell(new Paragraph(df.format(
                    Double.parseDouble(balanceInfo.getTotalValue()) +
                            Double.parseDouble(balanceInfo.getIncome())
            ), contentFont));
            table_1.addCell(new Paragraph(balanceInfo.getBonusType(), contentFont));
            table_1.addCell(new Paragraph(df.format(Double.parseDouble(balanceInfo.getIncome())), contentFont));
            table_1.addCell(new Paragraph(balanceInfo.getAgencyno(), contentFont));
        }

        addContent[2] = table_1;
        //------------>

        graph = new Paragraph("\n\nTransaction Account\n", contentFont);
        graph.setAlignment(Paragraph.ALIGN_CENTER);
        addContent[3] = graph;
        //--------------->

        graph = new Paragraph("Transaction conducted from begindate " + beginDate + " to " + endDate + "\n\n", contentFont);
        addContent[4] = graph;
        //------------------>

        PdfPTable table_2 = new PdfPTable(9);
        table_2.setWidthPercentage(110);
        table_2.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle2 = {"Value Date", "Fund Name",
                "Transaction\n Channel", "Transaction\n Type",
                "No. of Units\n Transacted",
                "NAV perUnit(RMB)", "Paid/Unpaid\n Distributions\n (RMB)",
                "Handing Charges\n (RMB)", "Net\nConsideration\n(RMB)"};

        for (String ttl : tableTitle2) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table_2.addCell(cell);
        }

        for (int i = 0; i < periodTradeList.size(); i++) {
            PeriodTrade periodTrade = periodTradeList.get(i);
            table_2.addCell(new Paragraph(periodTrade.getConfirmDate(), contentFont));
            table_2.addCell(new Paragraph(periodTrade.getFundName(), contentFont));
            table_2.addCell(new Paragraph(periodTrade.getAgencyno(), contentFont));
            table_2.addCell(new Paragraph(periodTrade.getBusinFlag(), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(periodTrade.getConfirmShares())), contentFont));
            table_2.addCell(new Paragraph(df2.format(Double.parseDouble(periodTrade.getNetValue())), contentFont));
            table_2.addCell(new Paragraph(df2.format(Double.parseDouble(periodTrade.getIncome())), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(periodTrade.getSxf())), contentFont));
            table_2.addCell(new Paragraph(df.format(Double.parseDouble(periodTrade.getConfirmBalance())), contentFont));
        }
        addContent[5] = table_2;
        //--------------------->end

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf_en("comparePDF" + beginDate.replaceAll("-", "") + "~" + endDate.replaceAll("-", "") + ".pdf", response, titleStr, titleFont, contentFont, addContent);


        return null;
    }

    //未付收益
    public ActionForward unincome(ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;
        String tradeacco = null;
        if (custno != null && custno.length() > 0) {
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

        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));

        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        List list = queryDao.queryUnincome(fundAcco, beginDate, endDate, 0, 0, true, tradeacco);
        String userName = queryDao.queryUserName(fundAcco);    //户名

        String space = " ";
        for (int i = 0; i < 32; i++) {
            space += " ";
        }

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        Paragraph graph = new Paragraph("账户名称：" + userName + "\n" + FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "cn") + "\n\n", contentFont);

        //title
        String titleStr = "上投摩根基金未付收益流水书";

        PdfPTable table = new PdfPTable(14);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle = {"交易账号\nTrading Account\nNo.", "客户类型\nClient\nType",
                "基金名称\nFund Name", "确认日期\nDeal Confirmation Date", "未付收益\nCumulative Unpaid Distributions",
                "冻结未付收益\nCumulative Unpaid Distributions under Freeze", "份额余额\nUnits Held", "冻结份额余额\nUnits Held under Freeze", "当天新增收益\nIncome Distributions\nDeclared for the day",
                "销售商\nSales Agent", "电子邮件\nE-mail Address", "地址\nAddress", "联系电话\nTelephone No.", "联系人\nContact person"};

        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");

        //数据
        for (int i = 0; i < list.size(); i++) {
            Map map = (Map) list.get(i);

            String custType = map.get("C_CUSTTYPE").toString();
            String income = map.get("F_INCOME") == null ? "0" : map.get("F_INCOME").toString();
            String frozenIncome = map.get("F_FROZENINCOME") == null ? "0" : map.get("F_FROZENINCOME").toString();
            String realShares = map.get("F_REALSHARES") == null ? "0" : map.get("F_REALSHARES").toString();
            String frozenShares = map.get("F_LASTFREEZESHARE") == null ? "0" : map.get("F_LASTFREEZESHARE").toString();
            String f_newincome = map.get("F_NEWINCOME") == null ? "0" : map.get("F_NEWINCOME").toString();
            String confirmDate = map.get("D_CDATE") == null ? "--" : DateTools.getDateString((Date) map.get("D_CDATE"));

            income = df.format(Double.parseDouble(income));
            frozenIncome = df.format(Double.parseDouble(frozenIncome));
            realShares = df.format(Double.parseDouble(realShares));
            frozenShares = df.format(Double.parseDouble(frozenShares));
            f_newincome = df.format(Double.parseDouble(f_newincome));

            table.addCell(new Paragraph(map.get("C_TRADEACCO").toString(), contentFont));
            table.addCell(new Paragraph(custType, contentFont));
            table.addCell(new Paragraph(map.get("C_FUNDNAME").toString(), contentFont));
            table.addCell(new Paragraph(confirmDate, contentFont));
            table.addCell(new Paragraph(income, contentFont));
            table.addCell(new Paragraph(frozenIncome, contentFont));
            table.addCell(new Paragraph(realShares, contentFont));
            table.addCell(new Paragraph(frozenShares, contentFont));
            table.addCell(new Paragraph(f_newincome, contentFont));

            table.addCell(new Paragraph(map.get("C_AGENCYNAME") == null ? "" : map.get("C_AGENCYNAME").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_EMAIL") == null ? "" : map.get("C_EMAIL").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_ADDRESS") == null ? "" : map.get("C_ADDRESS").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_PHONE") == null ? "" : map.get("C_PHONE").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_CONTACT") == null ? "" : map.get("C_CONTACT").toString(), contentFont));
        }

        //body
        Element[] addContent = new Element[2];
        addContent[0] = graph;
        addContent[1] = table;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf("unincomePDF " + ".pdf", response, titleStr, titleFont, contentFont, addContent);

        logger.info("基金账号：" + fundAcco + "---导出未付收益pdf");

        return null;
    }

    //未付收益英文版pdf
    public ActionForward unincome_en(ActionMapping mapping, ActionForm form,
                                     HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        //判断登陆,根据客户编号查询用户所有的基金、股东、中登账号
        String custno = ((UserInfo) request.getSession().getAttribute(com.trs.ids.QueryActor.LOGIN_FLAG)).getCustomNo();
        String fundAccos = null;

        if (custno != null && custno.length() > 0) {
            fundAccos = queryDao.findFundAccoByCustNo(custno);
        } else {
            logger.info("获取客户编号失败 = " + custno);
            return null;
        }

        if (fundAccos == null || fundAccos.length() <= 0) {
            return null;
        }
        String fundAcco = FormatDateTool.stringFormat(fundAccos);
        String tradeacco = null;
        String beginDate = CheckData.c(request.getParameter("begindate"));
        String endDate = CheckData.c(request.getParameter("enddate"));

        // 系统查询所需公共信息
        HashMap sysInfo = (HashMap) request.getSession().getServletContext()
                .getAttribute("initSysInfo_en");
        if (sysInfo == null) {
            sysInfo = initSysInfoDao.getInitSysInfo_en();
            request.getSession().getServletContext().setAttribute(
                    "initSysInfo_en", sysInfo);
        }

        List initFundInfos = (List) sysInfo.get("initFundInfo");

        if (!Validate.date10Validate(beginDate) || !Validate.date10Validate(endDate)) {
            logger.warn("基金账号：" + fundAcco + "不合法的日期格式");
            return null;
        }

        List list = queryDao.queryUnincome_en(fundAcco, beginDate, endDate, 0, 0, true, tradeacco);
        String userName = queryDao.queryUserName(fundAcco);    //户名

        String space = " ";
        for (int i = 0; i < 32; i++) {
            space += " ";
        }

        //------------------>go pdf
        // 标题字体
        BaseFont bfTitle = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfTitle, 10, Font.NORMAL);
        // 内容字体
        BaseFont bfContent = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font contentFont = new Font(bfContent, 10, Font.NORMAL);

        Paragraph graph = new Paragraph("UserName：" + userName + "\n" + FormatDateTool.accountCheck(FormatDateTool.stringFormat1(fundAcco), "en") + "\n\n", contentFont);

        //title
        String titleStr = "Not Paying Income Water Book";

        PdfPTable table = new PdfPTable(14);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        String[] tableTitle = {"Trading\n Account\nNo.", "Client\nType",
                "Fund Name", "Deal \nConfirmation\n Date", "Cumulative Unpaid\nDistributions",
                "Cumulative Unpaid\nDistributions under Freeze", "Units Held", "Units Held\n under Freeze", "Income Distributions\nDeclared for the day",
                "Sales\n Agent", "E-mail\n Address", "Address", "Telephone No.", "Contact person"};

        for (String ttl : tableTitle) {
            PdfPCell cell = new PdfPCell(new Paragraph(ttl, contentFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        DecimalFormat df = new DecimalFormat(",##0.00");

        //数据
        for (int i = 0; i < list.size(); i++) {
            Map map = (Map) list.get(i);

            String custType = map.get("C_CUSTTYPE").toString();
            String income = map.get("F_INCOME") == null ? "0" : map.get("F_INCOME").toString();
            String frozenIncome = map.get("F_FROZENINCOME") == null ? "0" : map.get("F_FROZENINCOME").toString();
            String realShares = map.get("F_REALSHARES") == null ? "0" : map.get("F_REALSHARES").toString();
            String frozenShares = map.get("F_LASTFREEZESHARE") == null ? "0" : map.get("F_LASTFREEZESHARE").toString();
            String f_newincome = map.get("F_NEWINCOME") == null ? "0" : map.get("F_NEWINCOME").toString();
            String confirmDate = map.get("D_CDATE") == null ? "--" : DateTools.getDateString((Date) map.get("D_CDATE"));
            String fundName = DictionaryChange.changeFundInfo_en(initFundInfos, map.get("C_FUNDCODE").toString());

            income = df.format(Double.parseDouble(income));
            frozenIncome = df.format(Double.parseDouble(frozenIncome));
            realShares = df.format(Double.parseDouble(realShares));
            frozenShares = df.format(Double.parseDouble(frozenShares));
            f_newincome = df.format(Double.parseDouble(f_newincome));

            table.addCell(new Paragraph(map.get("C_TRADEACCO").toString(), contentFont));
            table.addCell(new Paragraph(custType, contentFont));
            table.addCell(new Paragraph(fundName, contentFont));
            table.addCell(new Paragraph(confirmDate, contentFont));
            table.addCell(new Paragraph(income, contentFont));
            table.addCell(new Paragraph(frozenIncome, contentFont));
            table.addCell(new Paragraph(realShares, contentFont));
            table.addCell(new Paragraph(frozenShares, contentFont));
            table.addCell(new Paragraph(f_newincome, contentFont));

            table.addCell(new Paragraph(map.get("C_AGENCYNAME") == null ? "" : map.get("C_AGENCYNAME").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_EMAIL") == null ? "" : map.get("C_EMAIL").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_ADDRESS") == null ? "" : map.get("C_ADDRESS").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_PHONE") == null ? "" : map.get("C_PHONE").toString(), contentFont));
            table.addCell(new Paragraph(map.get("C_CONTACT") == null ? "" : map.get("C_CONTACT").toString(), contentFont));
        }

        //body
        Element[] addContent = new Element[2];
        addContent[0] = graph;
        addContent[1] = table;

        PdfTool pdfTool = new PdfTool();
        pdfTool.writePdf_en("unincomePDF " + ".pdf", response, titleStr, titleFont, contentFont, addContent);
        logger.info("**************************");
        logger.info("基金账号：" + fundAcco + "---未付收益英文版pdf");

        return null;
    }

    public void setInitSysInfoDao(InitSysInfoDao initSysInfoDao) {
        this.initSysInfoDao = initSysInfoDao;
    }

    public void setQueryDao(QueryDao queryDao) {
        this.queryDao = queryDao;
    }
}

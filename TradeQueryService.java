package com.cifm.one.trade.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cifm.one.account.service.CustomerService;
import com.cifm.one.act.entity.BankChannel;
import com.cifm.one.common.model.CustomerInfo;
import com.cifm.one.common.model.Result;
import com.cifm.one.common.model.ResultOb;
import com.cifm.one.common.utils.*;
import com.cifm.one.fund.service.FundNavService;
import com.cifm.one.hs.api.*;
import com.cifm.one.hs.entity.cashTreasure.HsMoneyFundFixConvert;
import com.cifm.one.hs.entity.cashTreasure.HsMoneyFundFixPreCheck;
import com.cifm.one.hs.entity.cashTreasure.HsMoneyShareQuery;
import com.cifm.one.hs.entity.dcQuery.*;
import com.cifm.one.hs.entity.fixedInverstment.HsChangeFixInvestState;
import com.cifm.one.hs.entity.fixedInverstment.HsFixInvestQuery;
import com.cifm.one.hs.entity.query.*;
import com.cifm.one.hs.entity.query.HsDcCustInfoQuery;
import com.cifm.one.hs.entity.trade.*;
import com.cifm.one.sys.api.ParaApi;
import com.cifm.one.sys.entity.Para;
import com.cifm.one.trade.entity.cache.fund.FundInfoExt;
import com.cifm.one.trade.model.DsInfo;
import com.cifm.one.trade.model.buy.BuyFormVM;
import com.cifm.one.trade.model.cash.CashTreasureForm;
import com.cifm.one.trade.model.query.FixBean;
import com.cifm.one.trade.model.query.FixRecord;
import com.cifm.one.trade.model.query.TradeRecord;
import com.cifm.one.trade.model.share.*;
import com.cifm.one.web.service.AppSessionService;
import com.cifm.one.web.utils.BusyErrorCode;
import com.cifm.one.web.utils.SessionCacheType;
import com.cifm.one.web.utils.WebTools;
import com.google.gson.JsonObject;
import com.hundsun.fund.fund.model.query.*;
import com.hundsun.fund.fund.model.trade.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by 王文婷
 * since  2017/2/22.
 * 交易查询
 */
@Service
@CacheConfig(cacheNames = "tradeQuery")
public class TradeQueryService {

    private static Logger logger = LoggerFactory.getLogger(TradeQueryService.class);

    @Autowired
    private EcQueryApi ecQueryApi;
    @Autowired
    private ParaApi paraApi;
    @Autowired
    private EcDcQueryApi ecDcQueryApi; //数据中心查询接口
    @Autowired
    private EcTradeFundApi ecTradeFundApi;
    @Autowired
    private EcCashTreasureApi ecCashTreasureApi; //查询现金宝接口
    @Autowired
    private CommonService commonService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private AppSessionService appSessionService;
    @Autowired
    private CashTreasureService cashTreasureService;
    @Autowired
    private FundInfoCacheService fundInfoCacheService;
    @Autowired
    private TradeCusInfoManageService tradeCusInfoManageService;
    @Autowired
    private TradePurchaseApplyService tradePurchaseApplyService;
    @Autowired
    private EcAccountApi ecAccountApi;
    @Autowired
    private TradeCusInfoManageService tcs;

    @Autowired
    private FundNavService fundNavService;
    @Autowired
    private FundInfoService fundInfoService;


    @CacheEvict(allEntries = true)
    public void clearCache() {

    }

    /**
     * 按照确认日期降序排序
     */
    private void sort(List<DcBonus> list) {
        Collections.sort(list, new Comparator<DcBonus>() {
            @Override
            public int compare(DcBonus o1, DcBonus o2) {
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                try {
                    Date dt1 = format.parse(o1.getAffirm_date());
                    Date dt2 = format.parse(o2.getAffirm_date());
                    if (dt1.getTime() < dt2.getTime()) {
                        return 1;
                    } else if (dt1.getTime() > dt2.getTime()) {
                        return -1;
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }

    /**
     * 按照确认日期降序排序
     */
    private void sortConfirm(List<DcTradeConfirm> list) {
        Collections.sort(list, new Comparator<DcTradeConfirm>() {
            @Override
            public int compare(DcTradeConfirm o1, DcTradeConfirm o2) {
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                try {
                    Date dt1 = format.parse(o1.getApply_date());
                    Date dt2 = format.parse(o2.getApply_date());
                    if (dt1.getTime() < dt2.getTime()) {
                        return 1;
                    } else if (dt1.getTime() > dt2.getTime()) {
                        return -1;
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }

    /**
     * 份额查询
     */
    public ResultOb<HsDcShareQuery> shareQueryInfo(DsInfo dsInfo, String offundtype, String fund_code) {
        String dcClientId = this.getDcClientId(dsInfo);
        if (StringUtils.isNotBlank(dcClientId)) {
            return ecQueryApi.dcShareQuery(dcClientId, null, offundtype, fund_code);
        } else {
            logger.error(BusyErrorCode.MY_SHARE + ":shareQueryInfo:dsInfo=" + dsInfo);
            ResultOb<HsDcShareQuery> resultOb = new ResultOb<>();
            Tools.setErrorMessage(resultOb, "error");
            return resultOb;
        }
    }

    //D408
    public ResultOb<HsDcShareDetailQuery> shareDetailQueryInfo(String fund_code,
                                                               String accountno,
                                                               String agencytype,
                                                               String offundType) {
        return ecQueryApi.dcSharDetaileQuery(accountno, agencytype, offundType, fund_code);
    }

    //S403直销份额查询 返回资金方式
    public ResultOb<HsShareQuery> dsShareQuery(String client_id, String fund_code, String ofundType) {
        return ecQueryApi.shareQueryInfo(client_id, fund_code, null, ofundType);
    }

    public String getDcClientId(DsInfo dsInfo) {
        // 获取数据中心客户编号
        String dcClientId;
        if (customerService.isOnlyAgent(dsInfo.getSource())) {
            dcClientId = dsInfo.getDcClientId();
        } else {
            if (StringUtils.startsWith(dsInfo.getFundAccount(), "*")) {
                dcClientId = this.dcCustInfoQuery(dsInfo.getIdType(), dsInfo.getIdNo());
            } else {
                // 根据基金账号查询数据中心客户ID
                if (StringUtils.isNotBlank(dsInfo.getFundAccount())) {
                    dcClientId = this.dcCustInfoQuery(dsInfo.getFundAccount());
                } else {
                    dcClientId = "";
                }
            }
            if (StringUtils.isNotBlank(dcClientId)) {
                dsInfo.setDcClientId(dcClientId);
                if (StringUtils.isNotBlank(dsInfo.getSid())) {
                    DsInfo dsInfoCache = appSessionService.getDsInfo(dsInfo.getSid());
                    if (StringUtils.isBlank(dsInfoCache.getDcClientId())) {
                        logger.error("dcClientId is blank. sid=" + dsInfo.getSid() + ",dcClientId=" + dcClientId);
                        dsInfoCache.setDcClientId(dcClientId);
                        appSessionService.setDsInfoSession(dsInfo.getSid(), dsInfoCache);
                    }
                }
            }
        }
        return dcClientId;
    }

    @Cacheable
    public JSONObject test(String cacheUnit) {
        JSONObject jsonObject = new JSONObject();
        JSONObject json = new JSONObject();
        json.put("1", "a");
        json.put("2", "b");
        jsonObject.put("name", "value");
        jsonObject.put("abc", "123");
        jsonObject.put("obj", json);
        return jsonObject;
    }


    /**
     * 我的资产查询
     */
    @Cacheable
    public ShareInfoBean shareQuery(String dcClientId, String fundAccount, String custId, String cacheUnit) {

        logger.info(String.format("dcClientId:\r\n%s", dcClientId));

        ShareInfoBean shareInfoBean = new ShareInfoBean();

        Map<String, Object> typeMap = new HashMap<>();
        shareInfoBean.setTypeDistribute(typeMap);

        List<DcShare> dcShares = new ArrayList<>();
        List<DcInvestProfitFundacco> dcProfits = new ArrayList<>();
        Integer threadCount = 0, timeout = 60;
        long beg = System.currentTimeMillis();
        Map<String, Object> theadMap = new HashMap<>();
        if (StringUtils.isNotBlank(dcClientId)) {
            ++threadCount;
            //查询数据中心直销资产D407
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<DcShare> temp = ecQueryApi.dcShareQuery(dcClientId, null, null, null).getData().getResults();
                    if (temp != null) {
                        dcShares.addAll(temp);
                    }
                    theadMap.put("0", true);
                }
            }).start();
        } else {
            logger.warn(String.format("query my asserts error: %s", dcClientId));
        }
        if (StringUtils.isNotBlank(fundAccount)) {
            ++threadCount;
            //查询数据中心直销资产D407
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<DcInvestProfitFundacco> temp = ecDcQueryApi.dcInvestProfitFundaccoQuery(fundAccount, null, null, null).getData().getResults();
                    if (temp != null) {
                        dcProfits.addAll(temp);
                    }
                    theadMap.put("1", true);
                }
            }).start();
        } else {
            logger.warn(String.format("query my profit error: %s", fundAccount));
        }
        List<MoneyShareQuery> shareList = null;
        if (StringUtils.isNotBlank(custId)) {
            shareList = ecCashTreasureApi.shareQuery(custId, null).getData().getMoneyShareQuerys();
        } else {
            logger.warn(String.format("query share error: %s", custId));
        }
        while (theadMap.size() < threadCount && (System.currentTimeMillis() - beg) < timeout * 1000) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //===================处理数据中心资产========================
        if (dcShares != null && !dcShares.isEmpty()) {
            //总的美元市值
            double totalDollarValue = Double.parseDouble(shareInfoBean.getTotalDollarvalue()),
                    //人民币市值
                    totalMarketValue = Double.parseDouble(shareInfoBean.getTotalMarketvalue()),
                    //普通资产人民币市值
                    normalTotalMarketValue = Double.parseDouble(shareInfoBean.getNormalTotalmarketvalue()),
                    //普通资产美元市值
                    normalTotalDollarValue = Double.parseDouble(shareInfoBean.getNormalTotalDollarmarketvalue()),
                    //专户资产人民币市值
                    spMarketValue = Double.parseDouble(shareInfoBean.getSpMarketvalue()),
                    //专户资产美元市值
                    spDollarMarketValue = Double.parseDouble(shareInfoBean.getSpDollarMarketvalue());

            Map<String, Double> typeAmountMap = new HashMap<>(),
                    typeAmountMapUs = new HashMap<>();

            final String SPEC_ELEC = "专户理财",
                    STOC_PROD = "偏股产品",
                    BOND_PROD = "偏债产品",
                    QDII_FUND = "QDII基金",
                    ABRO_PROD = "海外产品",
                    MUTU_FUND = "互认基金",
                    CASH_MANA = "现金管理";

            for (DcShare shareShowBean : dcShares) {
                final String fundCode = shareShowBean.getFund_code(),
                        ofundType = shareShowBean.getOfund_type();
                String typeAmountKey = null;
                Double totalWorthValue = Double.valueOf(shareShowBean.getTotal_worth_value()),
                        worthValue = Double.valueOf(shareShowBean.getWorth_value());
                if ("2".equals(ofundType)) {
                    typeAmountKey = SPEC_ELEC;
                } else {
                    //todo 按照类别分类
                    List<Map<String, Object>> fundManagerInfo = fundNavService.getFundManagerInfo(fundCode);
                    if (null != fundManagerInfo && fundManagerInfo.size() > 0) {
                        Map<String, Object> manager = fundManagerInfo.get(0);
                        if (null != manager && null != manager.get("FUND_TYPE")) {
                            String fund_type = manager.get("FUND_TYPE").toString().replaceAll("[()]", "");
                            if (fund_type.contains("股") || fund_type.contains("指数")) {
                                typeAmountKey = STOC_PROD;
                            } else if (fund_type.contains("债")) {
                                typeAmountKey = BOND_PROD;
                            } else if (fund_type.contains("QD")) {
                                if ("美元".equals(shareShowBean.getMoney_type())) {
                                    typeAmountKey = QDII_FUND;
                                } else {
                                    typeAmountKey = ABRO_PROD;
                                }
                            } else if (fund_type.contains("互认")) {
                                if ("美元".equals(shareShowBean.getMoney_type())) {
                                    typeAmountKey = MUTU_FUND;
                                } else {
                                    typeAmountKey = ABRO_PROD;
                                }
                            } else if (fund_type.contains("货币")) {
                                typeAmountKey = CASH_MANA;
                            }
                        }
                    }
                }
                //如果是美元
                if ("美元".equals(shareShowBean.getMoney_type())) {
                    //如果是专户基金//产品类型
                    if ("2".equals(shareShowBean.getOfund_type())) {
                        spDollarMarketValue = spDollarMarketValue + Double.parseDouble(shareShowBean.getWorth_value());
                    } else {
                        normalTotalDollarValue = normalTotalDollarValue + Double.parseDouble(shareShowBean.getWorth_value());
                    }
                    totalDollarValue = totalDollarValue + Double.parseDouble(shareShowBean.getTotal_worth_value());
                    if (null != typeAmountKey) {
                        typeAmountMapUs.put(typeAmountKey, totalWorthValue + (typeAmountMapUs.containsKey(typeAmountKey) ? typeAmountMapUs.get(typeAmountKey) : 0.00));
                    }
                } else {
                    //专户取数据中心 || 子公司专户产品 取数据中心
                    if ("2".equals(shareShowBean.getOfund_type()) || ("2".equals(shareShowBean.getOfund_type()) && StringUtils.equals("2JA", shareShowBean.getAgency()))) {
                        spMarketValue += worthValue;
                        totalMarketValue += worthValue;
                    } else {//普通基金人民币市值（含现金宝）
                        normalTotalMarketValue += totalWorthValue;
                        totalMarketValue += totalWorthValue;
                    }
                    if (null != typeAmountKey) {
                        typeAmountMap.put(typeAmountKey, totalWorthValue + (typeAmountMap.containsKey(typeAmountKey) ? typeAmountMap.get(typeAmountKey) : 0.00));
                    }
                }

            }
            List<Map<String, Object>> typeRates = new ArrayList<>(), typeRatesUs = new ArrayList<>();
            for (Map.Entry<String, Double> entry : typeAmountMapUs.entrySet()) {
                Map<String, Object> rate = new HashMap<>();
                rate.put("name", entry.getKey());
                rate.put("value", NumberUtils.format00(entry.getValue() * 100 / totalDollarValue));
                typeRatesUs.add(rate);
            }

            for (Map.Entry<String, Double> entry : typeAmountMap.entrySet()) {
                Map<String, Object> rate = new HashMap<>();
                rate.put("name", entry.getKey());
                rate.put("value", NumberUtils.format00(entry.getValue() * 100 / totalMarketValue));
                typeRates.add(rate);
            }
            typeMap.put("rmbDistribute", typeRates);
            typeMap.put("usdDistribute", typeRatesUs);

            shareInfoBean.setTotalMarketvalue(NumberUtils.format00(totalMarketValue));
            shareInfoBean.setTotalDollarvalue(NumberUtils.format00(totalDollarValue));
            shareInfoBean.setNormalTotalmarketvalue(NumberUtils.format00(normalTotalMarketValue));
            shareInfoBean.setNormalTotalDollarmarketvalue(NumberUtils.format00(normalTotalDollarValue));
            shareInfoBean.setSpMarketvalue(NumberUtils.format00(spMarketValue));
            shareInfoBean.setSpDollarMarketvalue(NumberUtils.format00(spDollarMarketValue));
        }
        //人民币资产昨日收益、人民币资产总收益、现金宝昨日收益、现金宝总收益
        if (dcProfits != null && !dcProfits.isEmpty()) {
            DcInvestProfitFundacco profit = dcProfits.get(0);
            shareInfoBean.setRmbYestodayProfit(profit.getSession_income());//人民币资产昨日总收益
            shareInfoBean.setRmbTotalProfit(profit.getAll_income());//人民币资产总收益
        }
        if (shareList != null && !shareList.isEmpty()) {
            double moneyYestodayProfit = 0.0, moneyTotalProfit = 0.0;
            for (MoneyShareQuery money : shareList) {
                moneyYestodayProfit += Double.parseDouble(money.getToday_income());
                moneyTotalProfit += Double.parseDouble(money.getAccum_income());
            }
            shareInfoBean.setMoneyYestodayProfit(NumberUtils.format00(moneyYestodayProfit));//现金宝昨日收益
            shareInfoBean.setMoneyTotalProfit(NumberUtils.format00(moneyTotalProfit));//现金宝总收益
        }
        return shareInfoBean;
    }

    /**
     * 处理份额
     */
    private ShareShowBean dealwith(DcShare dcShare,
                                   Map<String, ShareQuery> dsShareMap,
                                   boolean redeem,
                                   Map<String, Boolean> redeemAbleFundMap,
                                   Map<String, HsHisdayProfit> fundYestodayIncomeMap,
                                   Map<String, String> fundIncomeMap,
                                   Map<String, Para> fundParaMap) {
        //缓存中获取基金信息
        FundInfoExt fundInfoExt = fundInfoCacheService.getFundInfoExt(dcShare.getFund_code());
        //封装数据
        ShareShowBean entity = new ShareShowBean();
        entity.setAgencyname(dcShare.getAgency_name());
        entity.setAgencytype(dcShare.getAgency_type());
        entity.setBalancecoin(dcShare.getMoney_type());
        entity.setBankacco(dcShare.getBank_account());
        entity.setBankdetailname(dcShare.getBank_name());
        entity.setBankname(commonService.getBankName(dcShare.getBank_no()));
        entity.setBankno(dcShare.getBank_no());
        entity.setSharetype(dcShare.getShare_type());
        entity.setFundcode(dcShare.getFund_code());
        entity.setFundname(dcShare.getFund_name());
        entity.setTradeacco(dcShare.getTrade_acco());
        entity.setMelonmethod(dcShare.getAuto_buy());
        entity.setMelonmethodstr(dcShare.getAuto_buy_name());
        entity.setFundtype(dcShare.getFund_property());
        entity.setFundtypeStr(commonService.getFundShareTypeName(dcShare.getFund_property()));
        //万份收益
        entity.setHfincomeratio(fundInfoExt.getPerMyriadIncome());
        //七日年化
        entity.setIncomeratio(fundInfoExt.getFundCurrRatio());
        entity.setDeclarestate(fundInfoExt.getDeclareState());
        entity.setFundstatus(fundInfoExt.getFundStatus());
        //未付收益
        entity.setUnpaidincome(dcShare.getNopay_income());
        //定投状态
        entity.setValuagrstate(fundInfoExt.getValuagrState());
        entity.setTransState(fundInfoExt.getTransState());
        //最新净值 来自公布的净值
        if (StringUtil.isNotBlank(fundInfoExt.getNetValue())) {
            entity.setPernetvalue(NumberUtils.format(fundInfoExt.getNetValue()));
        } else {
            entity.setPernetvalue("0.0000");
        }

        //净值日期 来自公布的净值日期
        entity.setNavdate(fundInfoExt.getNavDate());
        //可用份额
        entity.setUsableremainshare(dcShare.getEnable_shares());
        entity.setTfreezeremainshare(dcShare.getFrozen_shares());

        //设置基金陪伴信息
        entity.setAccompanyInfo(fundInfoService.getAccompanyInfo(fundParaMap, dcShare.getFund_code()));
        // 市值
        entity.setMarketvalue(NumberUtils.format00(dcShare.getTotal_worth_value()));
        // 净值日期不一致
//        if (!StringUtils.equals(fundInfoExt.getNavDate(), shareShowBean.getNew_hq_date())) {
//            if (shareShowBean.getEnable_shares() != null && fundInfoExt.getNetValue() != null) {
//                String result = NumberUtils.format00(Double.parseDouble(shareShowBean.getEnable_shares()) *
//                        Double.parseDouble(fundInfoExt.getNetValue()));
//                entity.setMarketvalue(result);
//            }
//        }

        entity.setMarketvalue(NumberUtils.format00(dcShare.getTotal_worth_value()));
        //昨日收益
        if (null != fundYestodayIncomeMap && fundYestodayIncomeMap.get(dcShare.getFund_code()) != null) {
            HsHisdayProfit res = fundYestodayIncomeMap.get(dcShare.getFund_code());
            entity.setYestodayIncome(NumberUtils.format00(res.getToday_income()));
        }
        //持仓收益
        if (null != fundIncomeMap && fundIncomeMap.get(dcShare.getFund_code()) != null) {
            String sum = fundIncomeMap.get(dcShare.getFund_code()).toString();
            entity.setShareIncome(NumberUtils.format00(sum));
        }
        // 直销资产
        ShareQuery shareQuery = dsShareMap.get(dcShare.getFund_code() + "." + dcShare.getTrade_acco());
        //2 为专户
        if ("2".equals(dcShare.getOfund_type())) {
            //计算
            if (dcShare.getEnable_shares() != null && fundInfoExt.getNetValue() != null) {
                String result = NumberUtils.format00(Double.parseDouble(dcShare.getEnable_shares()) *
                        Double.parseDouble(fundInfoExt.getNetValue()));
                entity.setMarketvalue(result);
            }
        }
        //专户取直销数据
        if ("2".equals(dcShare.getOfund_type()) && shareQuery != null) {
            entity.setNavdate(shareQuery.getNet_value_date());
            if (StringUtil.isNotBlank(shareQuery.getNet_value())) {
                entity.setPernetvalue(NumberUtils.format(shareQuery.getNet_value()));
            } else {
                entity.setPernetvalue("0.0000");
            }
            entity.setMarketvalue(NumberUtils.format00(shareQuery.getWorth_value()));
        }
        //专户取数据中心数据
        if ("2".equals(dcShare.getOfund_type())) {
            entity.setNavdate(dcShare.getNew_hq_date());
            if (StringUtil.isNotBlank(dcShare.getNet_value())) {
                entity.setPernetvalue(NumberUtils.format(dcShare.getNet_value()));
            } else {
                entity.setPernetvalue("0.0000");
            }
            entity.setMarketvalue(NumberUtils.format00(dcShare.getWorth_value()));
            String log = "专户净值log:dc:navDate=" + dcShare.getNew_hq_date() + ",nav=" + dcShare.getNet_value()
                    + ",mv=" + dcShare.getWorth_value();
            if (shareQuery != null) {
                log += ":ds:navDate=" + shareQuery.getNet_value_date() + ",nav=" + shareQuery.getNet_value()
                        + ",mv=" + shareQuery.getWorth_value();
            }
            log += ":net:navDate=" + fundInfoExt.getNavDate() + ",nav=" + fundInfoExt.getNetValue();
            logger.error(log);
        }
        entity.setCurrentremainshare(dcShare.getEnable_shares());
        // 认购状态
        entity.setSubscribestate(fundInfoExt.getSubscribeState());
        if (StringUtil.isNotBlank(fundInfoExt.getNavTotal())) {
            entity.setTotalnetvalue(NumberUtils.format(fundInfoExt.getNavTotal()));
        } else {
            entity.setTotalnetvalue("0.0000");
        }
        entity.setRedeemstate(fundInfoExt.getRedeemState());
        // 赎回状态,fundstatus为暂停交易
//        if (StringUtils.equals("0", fundInfoExt.getRedeemState())
//                && StringUtils.equals("1", fundInfoExt.getDeclareState()) && redeem) {
        if (StringUtils.equals("0", fundInfoExt.getRedeemState())
                && !StringUtils.equals("5", entity.getFundstatus())
                && redeem) {
            entity.setRedeemstate("1");
        }
        // 列表赎回状态
        if (StringUtils.equals("0", fundInfoExt.getRedeemState())
                && !StringUtils.equals("1", entity.getRedeemstate())
                && !StringUtils.equals("5", entity.getFundstatus())
                && redeemAbleFundMap.get(dcShare.getFund_code()) != null) {
            entity.setRedeemstate("1");
        }

        //直销代销
        if (StringUtil.isNotBlank(dcShare.getAgency()) && "237".equals(dcShare.getAgency()) &&
                shareQuery != null && !"E".equals(shareQuery.getCapital_mode())) {
            //直销
            entity.setAgencytype("0");
        } else {
            //代销资产
            entity.setAgencytype("1");
        }
        //根据基金代码和交易账号唯一确认一份资产信息  取得资金方式
        if (shareQuery != null && StringUtil.isNotBlank(shareQuery.getCapital_mode())) {
            entity.setCapitalmode(shareQuery.getCapital_mode());
            //str
            entity.setCapitalmodestr(commonService.getCapitalModeName(shareQuery.getCapital_mode()));
        }
        // 根据交易账号查询客户银行账号的资金方式
        if (StringUtil.isNotBlank(dcShare.getTrade_acco())) {
            ResultOb<HsBankAccountQuery> res = ecQueryApi.bankAccountQueryByTradeAcco(dcShare.getTrade_acco());
            if (res.isSuccess() && null != res.getData() && res.getData().getBankAccountQuerys().size() > 0) {
                BankAccountQueryBean bankAccountQuery = res.getData().getBankAccountQuerys().get(0);
                entity.setCapitalmode(bankAccountQuery.getCapital_mode());
                //str
                entity.setCapitalmodestr(commonService.getCapitalModeName(bankAccountQuery.getCapital_mode()));
            }
        }
        //美元基金特殊处理转换定投状态
        //判断是否是美元基金
        List<Para> paras = paraApi.findByKey("app_fund_us");
        if (null != paras && paras.size() > 0) {
            for (Para para : paras) {
                if (dcShare.getFund_code().equals(para.getCode())) {
                    //不可转换
                    entity.setTransState("0");
                    //不能定投
                    entity.setValuagrstate("0");
                }
            }
        }
        return entity;
    }

    /**
     * 获取购买表单数据
     * 1.根据基金代码查询基金信息 基金名称，风险等级 基金的交易限额
     * 2.客户银行卡列表 || D402 客户交易账号查询
     * 3.客户的现金宝账户
     * 4.客户的风险评测信息
     * 5.支持工行,建行,民生,浦发,通联
     * 6.浦发特殊处理
     * 7.上投货币不支持通联
     * 8.定投时显示现金宝份额为0的账户，认申购则不显示
     */
    public ResultOb getBuyForm(boolean fix, String fund_code, String warn_channel, DsInfo dsInfo, HttpServletRequest request) {
        BuyFormVM formViewModel = new BuyFormVM();
        //************************支付银行卡********************************//
        //查询客户银行账号//支付银行卡
        Map<String, List<BankAccountQueryBean>> bankResult = tradeCusInfoManageService.bankAccountQueryForBuy(dsInfo.getCustInfo().getClient_id(), request);


        //=====================现金宝账户=====================================\\
        ResultOb<HsMoneyShareQuery> hsMoneyShareQueryResultOb = ecCashTreasureApi.shareQuery(dsInfo.getCid(), null);
        List<MoneyShareQuery> aboveZero = new ArrayList<>();
        List<MoneyShareQuery> moneyShareQuerys = new ArrayList<>();
        if (hsMoneyShareQueryResultOb.isSuccess() && hsMoneyShareQueryResultOb.getData() != null
                && hsMoneyShareQueryResultOb.getData().getMoneyShareQuerys() != null) {
            moneyShareQuerys = hsMoneyShareQueryResultOb.getData().getMoneyShareQuerys();
            for (MoneyShareQuery m : moneyShareQuerys) {
                if (Double.parseDouble(m.getEnable_shares()) > 0) {
                    aboveZero.add(m);
                }
            }

        }
        List<MoneyShareBean> moneylist = new ArrayList<>();

        //过滤可用份额为0的现金宝账户

        if (!fix) {
            moneyShareQuerys = aboveZero;
        }

        if (moneyShareQuerys != null && moneyShareQuerys.size() > 0) {
            for (MoneyShareQuery bean : moneyShareQuerys) {

                MoneyShareBean moneyBean = new MoneyShareBean();
                Tools.copyProperties(moneyBean, bean);
                BankChannel bankChannel = commonService.getBankChannel(bean.getBank_no(), bean.getCapital_mode());
                logger.debug("bankChannel=" + JSON.toJSONString(bankChannel));
                if (null != bankChannel) {
                    moneyBean.setAlias(bankChannel.getAlias());
                    moneyBean.setBankName(bankChannel.getBankName());
                    logger.info("======================bankname=============================" + commonService.getBankName(bankChannel.getBankNo()));
                    moneyBean.setBankInfo(commonService.getBankName(bankChannel.getBankNo())
                            + "[尾号" + bean.getBank_account().substring(bean.getBank_account().length() - 4)
                            + "] " + commonService.getCapitalModeName(bean.getCapital_mode()));
                }


                // 工行,建行,民生,浦发,通联
                if (StringUtils.equals("4", bean.getCapital_mode())
                        || StringUtils.equals("B", bean.getCapital_mode())
                        || StringUtils.equals("J", bean.getCapital_mode())
                        || StringUtils.equals("F", bean.getCapital_mode())
                        || StringUtils.equals("M", bean.getCapital_mode())
                        || StringUtils.equals("1", bean.getCapital_mode())) {
                    // 上投货币不支持通联
                    if (StringUtils.equals("370010", fund_code)) {
                        if (!StringUtils.equals("M", (moneyBean.getCapital_mode()))) {
                            moneylist.add(moneyBean);
                        }
                    } else {
                        moneylist.add(moneyBean);
                    }
                }
            }
        }
//====================================现金宝 END=====================

        //************************基金信息********************************//


        FundInfoExt fm = fundInfoCacheService.getFundInfoExt(fund_code);
        String businFlag = "";  //业务代码  根据基金状态来判定
        String cashFlag = "036";  //现金宝定投其他基金 (其实做的默认是转换业务)
        if (fix) {
            businFlag = "090"; //定期定额申购协议签订
        } else {
            //或者通过基金的认申购状态来判断
            if ("1".equals(fm.getDeclareState())) {  //申购状态
                businFlag = "022";
            }
            if ("1".equals(fm.getSubscribeState())) {  //认购状态  boolean
                businFlag = "020";
            }
        }
        String shareType = fm.getShareType();
        //  基金风险等级   缓存中获取//基金风险等级名称
        String riskStr = fm.getOfundRisklevel();
        //基金名称
        String fundName = fm.getFundName();
        if (fund_code.equals("370010")) {
            fundName = "上投货币A";
        }
        // 客户风险信息
        String invest_risk_tolerance = dsInfo.getRiskInfo().getInvest_risk_tolerance();
        formViewModel.setInvest_risk_tolerance(invest_risk_tolerance);

        //最低风险承受能力
        formViewModel.setLowest_risk_tolerance(dsInfo.getRiskInfo().getLowest_risk_tolerance());

        //交易限制查询 s435
        HsTradeLimitQuery limitresult1 = new HsTradeLimitQuery();
        HsTradeLimitQuery limitresult = ecDcQueryApi.tradeLimitQuery(fund_code, null, shareType, businFlag, null).getData();
        if (StringUtil.isNotBlank(cashFlag)) {
            limitresult1 = ecDcQueryApi.tradeLimitQuery("000857", null, shareType, cashFlag, fund_code).getData();//现金宝
        }

        String firstTradeAcco = dsInfo.getTradeAccount();
        //TODO 风险评估,金额固定为10000
        ResultOb<HsRiskmatchResult> riskmatch = tradePurchaseApplyService.riskmatch(fund_code, businFlag, shareType,
                "10000", firstTradeAcco, null, null, null, null);


        //********************警示内容查询******************
        ResultOb<HsCautionqueryResult> cautionquery = tradePurchaseApplyService.cautionquery(null, null, warn_channel);
        logger.debug("警示内容查询=" + JSON.toJSONString(cautionquery));
        this.setForm(formViewModel, cautionquery, invest_risk_tolerance, fundName, fm.getOfundRisklevelName());

        List<BankAccountQueryBean> bankCardsApp = new ArrayList<>();
        if (StringUtil.isNotBlank(businFlag)) {
            bankCardsApp = bankResult.get(businFlag);
            // 上投货币不支持通联
            if (StringUtils.equals("370010", fund_code)) {
                for (int i = 0; i < bankCardsApp.size(); i++) {
                    if (StringUtils.equals("M", (bankCardsApp.get(i).getCapital_mode()))) {
                        bankCardsApp.remove(bankCardsApp.get(i));
                    }
                }
            }
        }

        this.setFormViewModel(formViewModel, fund_code, bankCardsApp, moneylist, riskStr, invest_risk_tolerance,
                limitresult, limitresult1, fm, businFlag, fundName, shareType, riskmatch.getData(), dsInfo);
        ResultOb<BuyFormVM> rb = new ResultOb<>();
        if (!"ETS-5BP0000".equals(riskmatch.getCode())) {
            Tools.setSuccessMessage(rb, "error");
        } else {
            Tools.setSuccessMessage(rb, "OK");
        }
        rb.setData(formViewModel);
        return rb;
    }


    private void setFormViewModel(BuyFormVM formViewModel, String fund_code, List<BankAccountQueryBean> bankCardsApp,
                                  List<MoneyShareBean> moneylist, String riskStr, String invest_risk_tolerance,
                                  HsTradeLimitQuery limitresult, HsTradeLimitQuery limitresult1, FundInfoExt fm,
                                  String businFlag, String fundName, String shareType, HsRiskmatchResult data, DsInfo dsInfo) {


        //************************设置formViewModel********************************//
        formViewModel.setBusinflag(businFlag);
        formViewModel.setFundcode(fund_code);
        formViewModel.setFundname(fundName);


        //美元申购的基金列表
        List<Para> paras = paraApi.findByKey("app_fund_us");
        Map<String, Para> map = new HashMap();
        for (Para para : paras) {
            map.put(para.getCode(), para);
        }
        // 美元资金
        if (map.containsKey(fund_code)) {
            Para para = map.get(fund_code);
            List<BankAccountQueryBean> usBankList = new ArrayList<>();
            for (BankAccountQueryBean bankAccountQueryBean : bankCardsApp) {
                bankAccountQueryBean.setDayMaxValue(para.getReserved3());//单笔限额
                bankAccountQueryBean.setOnceMaxValue(para.getReserved4());//单日交易限额
                if (StringUtils.equals("4", bankAccountQueryBean.getCapital_mode())) {
                    usBankList.add(bankAccountQueryBean);
                }
            }
            formViewModel.setBankCards(usBankList);
            formViewModel.setMoneyCards(new ArrayList<MoneyShareBean>());
            formViewModel.setMoneyType("USD");
        } else {//人民币
            formViewModel.setBankCards(bankCardsApp); //支付银行卡信息
            formViewModel.setMoneyCards(moneylist);  //设置支付的现金宝信息
            formViewModel.setMoneyType("CNY");
        }
        formViewModel.setInvest_risk_tolerance(invest_risk_tolerance);
        formViewModel.setFundRisk(riskStr);  //基金风险等级
        formViewModel.setFundRiskName(commonService.getOfundRiskLevelName(riskStr)); //基金风险等级名称
        //交易限制
        if (limitresult != null) {
            formViewModel.setAddvalue(limitresult.getAdd_value());
            formViewModel.setLow_value(limitresult.getLow_value());
            formViewModel.setHighvalue(limitresult.getHigh_value());
            formViewModel.setLimitMsg(limitresult.getMessage());
        }
//        //现金宝交易限制
        if (limitresult1 != null) {
            formViewModel.setCashaddvalue(limitresult1.getAdd_value());
            if ("020".equals(businFlag)) {
                formViewModel.setCashlow_value("100");
            } else {
                formViewModel.setCashlow_value(limitresult1.getLow_value());
            }

            formViewModel.setCashhighvalue(limitresult1.getHigh_value());
            formViewModel.setCashlimitMsg(limitresult1.getMessage());
        }
        formViewModel.setShareType(shareType);
        formViewModel.setTacode(fm.getTaCode());
        formViewModel.setRiskmatch(data);
        formViewModel.setIdType(dsInfo.getIdType());
        formViewModel.setIdNo(dsInfo.getIdNo());
    }


    private Map<String, HsHisdayProfit> getFundYestodayIncomeMap(String cid, String bdate, String edate) {
        Map<String, HsHisdayProfit> map = new HashMap<>();
        String yesterday = DateTime.getYesterday(new Date());
        ResultOb<HsHisdayProfitQueryResult> resultOb = ecQueryApi.hisProfitQuery(cid, yesterday, yesterday, null, "3", null, null);
        if (null != resultOb && null != resultOb.getData() && null != resultOb.getData().getResults() && resultOb.getData().getResults().size() > 0) {
            List<HsHisdayProfit> results = resultOb.getData().getResults();
            for (HsHisdayProfit fund : results) {
                map.put(fund.getFund_code(), fund);
            }
        }
        logger.info("昨日收益查询============================" + JSON.toJSONString(map));
        return map;
    }

    /**
     * @param resultOb
     * @param flag     是否不合并
     * @return
     */
    public Map<String, Object> getFundIncomeMap(ResultOb<DcFloatingProfitAndLossByClientIdQueryResult> resultOb, boolean flag) {
        Map<String, Object> mincome = new HashMap<>();
        Double sum = 0.00;
        if (null != resultOb && null != resultOb.getData() && null != resultOb.getData().getResults() && resultOb.getData().getResults().size() > 0) {
            List<DcFloatingProfitAndLossByClientId> results = resultOb.getData().getResults();
            for (int i = 0; i < results.size(); i++) {
                DcFloatingProfitAndLossByClientId fundIncome = results.get(i),
                        nextFundIncome = results.get(i + 1);
                if (!flag) { //列表页面相同基金合并收益
                    if (i == results.size() - 1) {
                        break;
                    }
                    if (fundIncome.getFund_code().equals(nextFundIncome.getFund_code())) {
                        sum = Double.parseDouble(fundIncome.getGain_balance()) + Double.parseDouble(nextFundIncome.getGain_balance());
                        mincome.put(fundIncome.getFund_code(), sum);
                    }
                } else {
                    mincome.put(fundIncome.getFund_code() + "-" + fundIncome.getTrade_acco(), fundIncome.getGain_balance());
                }
            }
        }
        return mincome;
    }

    /**
     * 资产列表查询
     *
     * @param offundtype
     * @param cacheUnit
     * @return
     */
    @Cacheable
    public List<JSONObject> shareDetailList(String clientId, String dcClientId, String offundtype, String cacheUnit) {
        logger.info(cacheUnit);
        List<JSONObject> sharelist = new ArrayList<>();
        // 所有可赎回的基金
        //todo 昨日收益
//        Map<String, HsHisdayProfit> fundYestodayIncomeMap = this.getFundYestodayIncomeMap(dcClientId, DateTime.getYesterday(new Date()), DateTime.getYesterday(new Date()));
        Map<String, HsHisdayProfit> fundYestodayIncomeMap = null;
        //持仓收益列表
        Map<String, String> fundIncomeMap = new HashMap<>();
        List<DcShare> dcShares = null;

        if (StringUtils.isNotBlank(dcClientId)) {
            // 根据offundtype查询数据中心所有资产
            dcShares = ecQueryApi.dcShareQuery(dcClientId, null, offundtype, null).getData().getResults();  //D407
        }

        //基金陪伴信息
        Map<String, Para> fundParaMap = fundInfoService.getAllFundCompanyInfo();
        // 数据中心的全部资产
        if (dcShares != null && !dcShares.isEmpty()) {
            // 查询全部
            Map<String, DcShare> mergeFundShareMap = new HashMap<>();
            DcShare sameFundShare = null;
            for (DcShare shareShowBean : dcShares) {
                if ((sameFundShare = mergeFundShareMap.get(shareShowBean.getFund_code())) != null) {
                    sameFundShare.setTotal_worth_value(String.valueOf(new BigDecimal(sameFundShare.getTotal_worth_value()).add(new BigDecimal(shareShowBean.getTotal_worth_value()))));
                    sameFundShare.setEnable_shares(String.valueOf(new BigDecimal(sameFundShare.getEnable_shares()).add(new BigDecimal(shareShowBean.getEnable_shares()))));
                } else {
                    mergeFundShareMap.put(shareShowBean.getFund_code(), shareShowBean);
                }
            }
            for (Map.Entry<String, DcShare> entry : mergeFundShareMap.entrySet()) {
                // 处理基金信息
                DcShare dcShare = entry.getValue();
                final String fundcode = dcShare.getFund_code();
                //缓存中获取基金信息
                FundInfoExt fundInfoExt = fundInfoCacheService.getFundInfoExt(fundcode);
                JSONObject entity = new JSONObject();
                //设置基金陪伴信息
                entity.put("accompanyInfo", fundInfoService.getAccompanyInfo(fundParaMap, fundcode));
                entity.put("balancecoin", dcShare.getMoney_type());
                entity.put("fundcode", fundcode);
                entity.put("fundname", dcShare.getFund_name());
                entity.put("fundtype", dcShare.getFund_property());
                //万份收益
                entity.put("hfincomeratio", fundInfoExt.getPerMyriadIncome());
                //七日年化
                entity.put("incomeratio", fundInfoExt.getFundCurrRatio());
                entity.put("marketvalue", NumberUtils.format00(dcShare.getTotal_worth_value()));
                entity.put("navdate", fundInfoExt.getNavDate());
                //最新净值 来自公布的净值
                if (StringUtil.isNotBlank(fundInfoExt.getNetValue())) {
                    entity.put("pernetvalue", NumberUtils.format(fundInfoExt.getNetValue()));
                } else {
                    entity.put("pernetvalue", "0.0000");
                }
                sharelist.add(entity);
            }
        }
        return sharelist;
    }

    @Cacheable
    public List<JSONObject> shareDetailNew(final String clientId, final String dcClientId, final String fundCode, String offundtype, String cacheUnit) {
        logger.info(cacheUnit);
        List<JSONObject> sharelist = new ArrayList<>();
        // 查询用户直销资产
        Map<String, ShareQuery> dsShareMap = new HashMap<>();
        //todo 昨日收益
//        Map<String, HsHisdayProfit> fundYestodayIncomeMap = this.getFundYestodayIncomeMap(dcClientId, DateTime.getYesterday(new Date()), DateTime.getYesterday(new Date()));
        Map<String, HsHisdayProfit> fundYestodayIncomeMap = null;
        //持仓收益列表
        List<ShareQuery> dsShares = new ArrayList<>();
        List<DcShare> dcShares = new ArrayList<>();
        Boolean redeem = false;

        Integer threadCount = 0, timeout = 60;
        long beg = System.currentTimeMillis();
        Map<String, Object> threadMap = new HashMap<>();
        if (StringUtils.isNotBlank(dcClientId)) {
            // 根据offundtype查询数据中心所有资产
            ++threadCount;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<DcShare> temp = ecQueryApi.dcShareQuery(dcClientId, null, offundtype, fundCode).getData().getResults();
                    if (temp != null) {
                        dcShares.addAll(temp);
                    }
                    threadMap.put("2", true);
                }
            }).start();
        }
        if (StringUtils.isNotBlank(clientId)) {
            ++threadCount;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<ShareQuery> temp = ecQueryApi.shareQueryInfo(clientId, null, null, null).getData().getShareQuerys();
                    if (temp != null) {
                        dsShares.addAll(temp);
                    }
                    threadMap.put("1", true);
                }
            }).start();
            List<RedeemListQuery> redeemFunds = ecTradeFundApi.redeemListQuery(clientId, fundCode).getData().getRedeemListQuerys();
            redeem = redeemFunds != null && !redeemFunds.isEmpty();
        }
        while (threadMap.size() < threadCount && (System.currentTimeMillis() - beg) < timeout * 1000) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (dsShares != null) {
            for (ShareQuery shareQuery : dsShares) {
                dsShareMap.put(shareQuery.getFund_code() + "." + shareQuery.getTrade_acco(), shareQuery);
            }
        }

        // 数据中心的全部资产
        if (dcShares != null && !dcShares.isEmpty()) {
            // 按基金代码查询
            for (DcShare dcShare : dcShares) {
                // 直销资产
                ShareQuery shareQuery = dsShareMap.get(dcShare.getFund_code() + "." + dcShare.getTrade_acco());
                //缓存中获取基金信息
                FundInfoExt fundInfoExt = fundInfoCacheService.getFundInfoExt(dcShare.getFund_code());
                //封装数据
                JSONObject entity = new JSONObject();
                entity.put("agencyname", dcShare.getAgency_name());
                entity.put("agencytype", dcShare.getAgency_type());
                entity.put("balancecoin", dcShare.getMoney_type());
                entity.put("bankacco", dcShare.getBank_account());
                entity.put("bankname", commonService.getBankName(dcShare.getBank_no()));
                entity.put("bankno", dcShare.getBank_no());
                //根据基金代码和交易账号唯一确认一份资产信息  取得资金方式
                if (shareQuery != null && StringUtil.isNotBlank(shareQuery.getCapital_mode())) {
                    entity.put("capitalmode", shareQuery.getCapital_mode());
                    entity.put("capitalmodestr", commonService.getCapitalModeName(shareQuery.getCapital_mode()));
                }

                //直销代销
                if (StringUtil.isNotBlank(dcShare.getAgency()) && "237".equals(dcShare.getAgency()) &&
                        shareQuery != null && !"E".equals(shareQuery.getCapital_mode())) {
                    //直销
                    entity.put("agencytype", "0");
                } else {
                    //代销资产
                    entity.put("agencytype", "1");
                }


                // 根据交易账号查询客户银行账号的资金方式
                if (StringUtil.isNotBlank(dcShare.getTrade_acco())) {
                    ResultOb<HsBankAccountQuery> res = ecQueryApi.bankAccountQueryByTradeAcco(dcShare.getTrade_acco());
                    if (res.isSuccess() && null != res.getData() && res.getData().getBankAccountQuerys().size() > 0) {
                        BankAccountQueryBean bankAccountQuery = res.getData().getBankAccountQuerys().get(0);
                        entity.put("capitalmode", bankAccountQuery.getCapital_mode());
                        entity.put("capitalmodestr", commonService.getCapitalModeName(bankAccountQuery.getCapital_mode()));
                    }
                }
                entity.put("currentremainshare", dcShare.getEnable_shares());
                entity.put("declarestate", fundInfoExt.getDeclareState());
                entity.put("fundcode", dcShare.getFund_code());
                entity.put("fundname", dcShare.getFund_name());
                entity.put("fundtype", dcShare.getFund_property());
                entity.put("fundtypeStr", commonService.getFundShareTypeName(dcShare.getFund_property()));
                entity.put("hfincomeratio", fundInfoExt.getPerMyriadIncome());
                entity.put("incomeratio", fundInfoExt.getFundCurrRatio());
                entity.put("marketvalue", NumberUtils.format00(dcShare.getTotal_worth_value()));
                entity.put("melonmethod", dcShare.getAuto_buy());
                entity.put("melonmethodstr", dcShare.getAuto_buy_name());
                if (StringUtil.isNotBlank(fundInfoExt.getNetValue())) {
                    entity.put("pernetvalue", NumberUtils.format(fundInfoExt.getNetValue()));
                } else {
                    entity.put("pernetvalue", "0.0000");
                }
                entity.put("redeemstate", fundInfoExt.getRedeemState());
                // todo 请老师在看一下
                if (StringUtils.equals("0", fundInfoExt.getRedeemState())
                        && !StringUtils.equals("5", fundInfoExt.getFundStatus())
                        && redeem) {
                    entity.put("redeemstate", "1");
                }
                entity.put("sharetype", dcShare.getShare_type());
                entity.put("subscribestate", fundInfoExt.getSubscribeState());
                if (StringUtil.isNotBlank(fundInfoExt.getNavTotal())) {
                    entity.put("totalnetvalue", NumberUtils.format(fundInfoExt.getNavTotal()));
                } else {
                    entity.put("totalnetvalue", "0.0000");
                }
                entity.put("tradeacco", dcShare.getTrade_acco());
                entity.put("transState", fundInfoExt.getTransState());
                entity.put("valuagrstate", fundInfoExt.getValuagrState());

                //美元基金特殊处理转换定投状态
                //判断是否是美元基金
                List<Para> paras = paraApi.findByKey("app_fund_us");
                if (null != paras && paras.size() > 0) {
                    for (Para para : paras) {
                        if (dcShare.getFund_code().equals(para.getCode())) {
                            //不可转换
                            entity.put("transState", "0");
                            //不能定投
                            entity.put("valuagrstate", "0");
                        }
                    }
                }
                entity.put("unpaidincome", dcShare.getNopay_income());
                sharelist.add(entity);
            }
        }
        return sharelist;
    }

    /**
     * 资产详情查询 shareDetail @wangwt
     */
    @Cacheable
    public ResultOb<ShareShowBean> shareDetail(DsInfo dsInfo, String fund_code, String offundtype) {
        ResultOb<ShareShowBean> resultOb = new ResultOb<>();
        List<ShareShowBean> sharelist = new ArrayList<>();
        resultOb.setItems(sharelist);
        Tools.setSuccessMessage(resultOb, "success");

        // 查询用户直销资产
        Map<String, ShareQuery> dsShareMap = new HashMap<>();
        // 所有可赎回的基金
        Map<String, Boolean> redeemAbleFundMap = new HashMap<>();
        //todo 昨日收益
//        Map<String, HsHisdayProfit> fundYestodayIncomeMap = this.getFundYestodayIncomeMap(dcClientId, DateTime.getYesterday(new Date()), DateTime.getYesterday(new Date()));
        Map<String, HsHisdayProfit> fundYestodayIncomeMap = null;
        //持仓收益列表
        Map<String, String> fundIncomeMergeMap = new HashMap<>();
        Map<String, String> fundIncomeDetailMap = new HashMap<>();

        List<ShareQuery> dsShares = null;
        List<RedeemListQuery> redeemFunds = null;
        List<DcShare> dcShares = null;
        List<DcFloatingProfitAndLossByClientId> fundProfits = null;
        final String clientId = dsInfo.getCid(),
                dcClientId = this.getDcClientId(dsInfo);

        if (StringUtils.isNotBlank(clientId)) {
            dsShares = ecQueryApi.shareQueryInfo(clientId, null, null, null).getData().getShareQuerys();
            redeemFunds = ecTradeFundApi.redeemListQuery(clientId, fund_code).getData().getRedeemListQuerys();
        }
        if (StringUtils.isNotBlank(dcClientId)) {
            // 根据offundtype查询数据中心所有资产
            dcShares = ecQueryApi.dcShareQuery(dcClientId, null, offundtype, fund_code).getData().getResults();  //D407
            fundProfits = ecQueryApi.floatIncome(dcClientId, DateTime.getDateStr8(new Date())).getData().getResults();
        }

        if (dsShares != null) {
            for (ShareQuery shareQuery : dsShares) {
                dsShareMap.put(shareQuery.getFund_code() + "." + shareQuery.getTrade_acco(), shareQuery);
            }
        }
        // 用作处理基金是否可赎回
        boolean redeem = false;
        if (redeemFunds != null && !redeemFunds.isEmpty()) {
            redeem = StringUtils.isNotBlank(fund_code);
            for (RedeemListQuery redeemListQuery : redeemFunds) {
                redeemAbleFundMap.put(redeemListQuery.getFund_code(), true);
            }
        }

        if (fundProfits != null && !fundProfits.isEmpty()) {
            String sameFundProfit = null;
            for (DcFloatingProfitAndLossByClientId fundProfit : fundProfits) {
                final String fundCode = fundProfit.getFund_code();
                fundIncomeDetailMap.put(fundCode + "-" + fundProfit.getTrade_acco(), fundProfit.getGain_balance());
                if ((sameFundProfit = fundIncomeMergeMap.get(fundCode)) != null) {
                    fundIncomeMergeMap.put(fundCode, String.valueOf(Double.parseDouble(sameFundProfit) + Double.parseDouble(fundIncomeMergeMap.get(fundCode))));
                } else {
                    fundIncomeMergeMap.put(fundCode, fundProfit.getGain_balance());
                }
            }
        }

        //基金陪伴信息
        Map<String, Para> fundParaMap = fundInfoService.getAllFundCompanyInfo();
        // 数据中心的全部资产
        if (dcShares != null && !dcShares.isEmpty()) {
            // 查询全部
            if (StringUtils.isBlank(fund_code)) {
                Map<String, DcShare> mergeFundShareMap = new HashMap<>();
                DcShare sameFundShare = null;
                for (DcShare shareShowBean : dcShares) {
                    if ((sameFundShare = mergeFundShareMap.get(shareShowBean.getFund_code())) != null) {
                        sameFundShare.setTotal_worth_value(String.valueOf(new BigDecimal(sameFundShare.getTotal_worth_value()).add(new BigDecimal(shareShowBean.getTotal_worth_value()))));
                        sameFundShare.setEnable_shares(String.valueOf(new BigDecimal(sameFundShare.getEnable_shares()).add(new BigDecimal(shareShowBean.getEnable_shares()))));
                    } else {
                        mergeFundShareMap.put(shareShowBean.getFund_code(), shareShowBean);
                    }
                }
                for (Map.Entry<String, DcShare> entry : mergeFundShareMap.entrySet()) {
                    // 处理基金信息
                    DcShare dcShare = entry.getValue();
                    ShareShowBean entity = this.dealwith(dcShare, dsShareMap, redeem, redeemAbleFundMap, fundYestodayIncomeMap, fundIncomeMergeMap, fundParaMap);
                    sharelist.add(entity);
                }
            } else {
                // 按基金代码查询
                for (DcShare dcShare : dcShares) {
                    ShareShowBean entity = this.dealwith(dcShare, dsShareMap, redeem, redeemAbleFundMap, fundYestodayIncomeMap, fundIncomeDetailMap, fundParaMap);
                    sharelist.add(entity);
                }
            }
        }
        return resultOb;
    }

    public Map<String, DcShare> setMres(Map<String, DcShare> mres, DcShare shareShowBean) {

        if (mres.containsKey(shareShowBean.getFund_code())) {
            DcShare dcShare = mres.get(shareShowBean.getFund_code());
            BigDecimal worth = new BigDecimal(dcShare.getTotal_worth_value());
            BigDecimal newWorth = new BigDecimal(shareShowBean.getTotal_worth_value());
            BigDecimal worthSum = worth.add(newWorth);
            dcShare.setTotal_worth_value(String.valueOf(worthSum));

            BigDecimal share = new BigDecimal(dcShare.getEnable_shares());
            BigDecimal newShare = new BigDecimal(shareShowBean.getEnable_shares());
            BigDecimal sumShare = share.add(newShare);
            dcShare.setEnable_shares(String.valueOf(sumShare));
        } else {
            mres.put(shareShowBean.getFund_code(), shareShowBean);
        }
        return mres;
    }

    /**
     * 查询可转换出基金列表查询(T413)
     */
    List<ShareShowBean> setConvertStatus(List<ShareShowBean> sharelist, String clientId) {
        //可转换出基金列表
        List<ConvertOutListResult.ConvertOut> results =
                tradePurchaseApplyService.convertOut(clientId).getData().getResults();

        for (ConvertOutListResult.ConvertOut result : results) {
            for (ShareShowBean shareShowBean : sharelist) {
                if (result.getFund_code().equals(shareShowBean.getFundcode())) {
                    shareShowBean.setTransState("1");
                }
            }
        }
        return sharelist;
    }

    /**
     * 普通定投协议查询(L410)如果传递了协议号则查询特定协议详情
     */
    public ResultOb<FixBean> fixInvestQuery(String id_kind_gb,
                                            String id_no,
                                            String request_num,
                                            String request_pageno,
                                            String scheduled_protocol_id) {

        ResultOb<FixBean> resultOb = new ResultOb<>();
        List<FixBean> newlist = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("00");
        ResultOb<HsFixInvestQuery> resultOb1 = ecQueryApi.fixInvestQuery(id_kind_gb, id_no, request_num,
                request_pageno, scheduled_protocol_id);
        if (resultOb1.isSuccess() && resultOb1.getData() != null && resultOb1.getData().getFixInvestQuery() != null) {
            List<FixInvestQuery> fixInvestQuery = resultOb1.getData().getFixInvestQuery();
            FundInfoExt fundInfoExt;
            for (FixInvestQuery fix : fixInvestQuery) {
                //过滤掉组合定投计划
                if (!StringUtils.isNotBlank(fix.getComb_code())) {
                    FixBean fixBean = new FixBean();
                    if (StringUtils.isNotBlank(fix.getOppo_fund_code()) && "000857".equals(fix.getFund_code())) {
                        fixBean.setType("现金宝定投");
                        fixBean.setFundcode(fix.getOppo_fund_code());
                        fixBean.setFromFundCode(fix.getFund_code());
                        fixBean.setApplysum(fix.getApply_share());
                        fixBean.setSharetype(fix.getOppo_share_type());
                        fixBean.setFromfundsharetype(fix.getShare_type());
                    } else {
                        fixBean.setType("定投");
                        fixBean.setFundcode(fix.getFund_code());
                        fixBean.setApplysum(fix.getApply_sum());
                        fixBean.setSharetype(fix.getShare_type());
                    }
                    fundInfoExt = fundInfoCacheService.getFundInfoExt(fixBean.getFundcode());
                    if (StringUtils.isNotBlank(scheduled_protocol_id)) {
                        //交易限制查询 s435
                        HsTradeLimitQuery limitresult = ecDcQueryApi.tradeLimitQuery(fundInfoExt.getFundCode(), null, fundInfoExt.getShareType(), fix.getFund_busin_code(), null).getData();
                        if (limitresult != null) {
                            fixBean.setLow_value(limitresult.getLow_value());
                            fixBean.setAddvalue(limitresult.getAdd_value());
                            fixBean.setHighvalue(limitresult.getHigh_value());
                        }
                    }

                    if (fundInfoExt != null) {
                        if ("370010".equals(fix.getFund_code())) {
                            fixBean.setFundname("上投货币");
                        } else {
                            fixBean.setFundname(fundInfoExt.getFundName());
                        }
                    }
                    fixBean.setBankacco(fix.getBank_account());
                    fixBean.setBankserial(fix.getBank_no());
                    // todo 恒生API的BUG
                    //fixBean.setBankname(fix.getBank_name());
                    fixBean.setBankname(fix.getBank_no() == null ? "--" : commonService.getBankName(fix.getBank_no()));
                    fixBean.setCapitalmode(fix.getCapital_mode());
                    fixBean.setCapitalmodestr(commonService.getCapitalModeName(fix.getCapital_mode()));
                    fixBean.setCycleunit(fix.getProtocol_period_unit());  //协议周期单位
                    fixBean.setJyrq(df.format(Integer.parseInt(fix.getProtocol_fix_day()))); //格式化保留两位小数
                    fixBean.setJyzq(fix.getTrade_period()); //交易周期
                    fixBean.setNextdate(fix.getNext_fixrequest_date());
                    fixBean.setXybm(fix.getProtocol_name());
                    fixBean.setXyh(fix.getScheduled_protocol_id());
                    fixBean.setYwdm(fix.getFund_busin_code());
                    fixBean.setTotalsuccsum(fix.getTotal_succ_sum());
                    fixBean.setTotalsucctime(fix.getTotal_succ_time());
                    fixBean.setTradeacco(fix.getTrade_acco());
                    fixBean.setState(fix.getScheduled_protocol_state());
                    fixBean.setFailTimes(fix.getFail_times());
                    newlist.add(fixBean);
                }
            }
        }
        resultOb.setItems(newlist);
        Tools.setSuccessMessage(resultOb, "success");
        return resultOb;
    }

    /**
     * 定投申购协议新增或修改(L411)
     */
    public ResultOb fixDeclare(FixDeclareBean param, DsInfo dsInfo) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMM");
//        //获得首次交易年月份
        String firstmonth = sf.format(new Date());
        if ("1".equals(param.getCapsource())) {
            //现金宝定投预校验
            ResultOb<HsMoneyFundFixPreCheck> hsMoneyFundFixPreCheckResultOb =
                    ecCashTreasureApi.fundFixInvestPreCheck("3", param.getBuyMoneyFundcode(),
                            param.getBuyMoneyShareType(),
                            param.getTradeacco(), param.getApplysum(),
                            param.getFundcode(), param.getSharetype());

            if ("ETS-5BP0000".equals(hsMoneyFundFixPreCheckResultOb.getHsCode())) {
                //现金宝定投
                ResultOb<HsMoneyFundFixConvert> hsMoneyFundFixConvertResultOb = ecCashTreasureApi.fundFixInvestConvert(param.getApplysum(),
                        firstmonth, param.getJyrq(), "0",
                        param.getPassword(), param.getCycleunit(),
                        param.getTradeacco(), param.getJyzq(),
                        param.getFundcode(), param.getSharetype(), param.getProtocolid());
                if ("DS-TR120102".equals(hsMoneyFundFixConvertResultOb.getHsCode())) {
                    hsMoneyFundFixConvertResultOb.setCode("0102");
                    return hsMoneyFundFixConvertResultOb;
                }
                if ("ETS-5BP0000".equals(hsMoneyFundFixConvertResultOb.getHsCode())) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    //下单成功，查询定投记录
                    ResultOb<HsTradeApplyQuery> hsTradeApplyQueryResultOb = ecQueryApi.tradeApplyQuery(dsInfo.getCid(), null, null, null, null,
                            "1", "999", null, hsMoneyFundFixConvertResultOb.getData().getScheduled_protocol_id(), null);
                    if ("ETS-5BP0000".equals(hsTradeApplyQueryResultOb.getHsCode()) && null != hsTradeApplyQueryResultOb.getData()) {
                        List<TradeApplyQuery> tradeApplyQuerys = hsTradeApplyQueryResultOb.getData().getTradeApplyQuerys();
                        map.put("record", tradeApplyQuerys);
                        hsMoneyFundFixConvertResultOb.setInfo(map);
                    }
                }
                return hsMoneyFundFixConvertResultOb;
            } else {
                return hsMoneyFundFixPreCheckResultOb;
            }
        } else {
            //普通定投
            ResultOb<HsFixDeclare> hsFixDeclareResultOb = ecQueryApi.fixDeclare(param.getApplysum(), param.getJyrq(),
                    param.getFundcode(), param.getPassword(),
                    param.getCycleunit(), param.getSharetype(),
                    param.getTradeacco(), param.getJyzq(),
                    firstmonth, param.getProtocolid());
            if ("DS-TR120102".equals(hsFixDeclareResultOb.getHsCode())) {
                hsFixDeclareResultOb.setCode("0102");
                return hsFixDeclareResultOb;
            }
            if ("ETS-5BP0000".equals(hsFixDeclareResultOb.getHsCode())) {
                Map<String, Object> map = new HashMap<String, Object>();
                //下单成功，查询定投记录
                ResultOb<HsTradeApplyQuery> hsTradeApplyQueryResultOb = ecQueryApi.tradeApplyQuery(dsInfo.getCid(), null, null, null, null,
                        "1", "999", null, hsFixDeclareResultOb.getData().getScheduled_protocol_id(), null);
                if (hsTradeApplyQueryResultOb != null && "ETS-5BP0000".equals(hsTradeApplyQueryResultOb.getHsCode()) && hsTradeApplyQueryResultOb.getData() != null) {
                    List<TradeApplyQuery> tradeApplyQuerys = hsTradeApplyQueryResultOb.getData().getTradeApplyQuerys();
                    map.put("record", tradeApplyQuerys);
                    hsFixDeclareResultOb.setInfo(map);
                }
            }
            return hsFixDeclareResultOb;
        }
    }

    /**
     * ·
     * 定投协议状态变更(L409)
     */
    public ResultOb<HsChangeFixInvestState> changeFixInvestState(String trade_acco,
                                                                 String scheduled_protocol_state,
                                                                 String scheduled_protocol_id, String password) {

        //查询用户的交易密码
//        String password = tradeCusInfoManageService.tradePassword(trade_acco).getData();
        String pwd = "";
        if (StringUtil.isNotBlank(password)) {
            pwd = ecAccountApi.pwd(password).getData().getPassword();//加密密码
        }
        return ecQueryApi.changeFixInvestState(pwd, scheduled_protocol_state, trade_acco, scheduled_protocol_id);
    }

    /**
     * 交易申请查询(S404)   数据中心(D404)
     * 查询数据中心和直销交易申请，通过申请编号合并查询结果
     */
    public ResultOb<TradeRecord> tradeQuery(String client_id, String startdate,
                                            String enddate, String allot_no,
                                            String requestsize, String fundcode,
                                            String scheduled_protocol_id,
                                            String requestno, String fund_busin_code,
                                            String queryFundType) { //1.只查询专户，默认查公募
        //直销交易申请
        ResultOb<HsTradeApplyQuery> hsTradeApplyQueryResultOb =
                ecQueryApi.tradeApplyQuery(client_id, startdate, enddate,
                        fund_busin_code, allot_no,
                        requestno, requestsize, fundcode,
                        scheduled_protocol_id, queryFundType);
        List<TradeApplyQuery> tradeApplyQuerys = hsTradeApplyQueryResultOb.getData().getTradeApplyQuerys();
        List<TradeRecord> newList = new ArrayList<>();
        if (tradeApplyQuerys != null && tradeApplyQuerys.size() > 0) {
            Map withdrawmap = this.queryWithdrawMap(client_id);  //可撤单列表
            for (TradeApplyQuery tr : tradeApplyQuerys) {
                TradeRecord tradeRecord = new TradeRecord();
                if (withdrawmap.get(tr.getAllot_no()) != null) {   //如果可撤单列表中有该申请编号
                    tradeRecord.setWithdrawstat("1");
                } else {
                    tradeRecord.setWithdrawstat("0");
                }
                tradeRecord.setBankno(tr.getBank_no());
                tradeRecord.setFundcode(tr.getFund_code());
                tradeRecord.setFundname(tr.getFund_name());
                tradeRecord.setXyh(tr.getScheduled_protocol_id());
                tradeRecord.setAccepttime(tr.getAccept_time());
                tradeRecord.setAgencyno("0"); //直销
                tradeRecord.setAgencyname("上投摩根");
                tradeRecord.setApplydate(tr.getApply_date());
                tradeRecord.setApplyserial(tr.getOriginal_appno()); //原申请单号
                tradeRecord.setApplyshare(tr.getShares());
                tradeRecord.setApplysum(tr.getBalance());
                tradeRecord.setBalancecoin(tr.getMoney_type());
                tradeRecord.setBankacco(tr.getBank_account());
                tradeRecord.setBusinflag(tr.getFund_busin_code());
                tradeRecord.setCapitalmode(tr.getCapital_mode());
                tradeRecord.setConfirmflag(tr.getConfirm_flag());
                tradeRecord.setKkstat(tr.getDeduct_status());
                tradeRecord.setTradeacco(tr.getTrade_acco()); //交易账号
                tradeRecord.setApplyserial(tr.getAllot_no()); //申请编号
                tradeRecord.setOriginalappno(tr.getOriginal_appno()); //原始申请单号
                tradeRecord.setTargetfundcode(tr.getTarget_fund_code()); //对方基金代

                if ("029".equals(tr.getFund_busin_code())) { //如果是修改分红方式
                    if ("0".equals(tr.getAuto_buy())) {//红利再投资
                        tradeRecord.setBeforemelond("1");
                    } else if ("1".equals(tr.getAuto_buy())) {
                        tradeRecord.setBeforemelond("0");
                    }
                }
                tradeRecord.setAutobuy(tr.getAuto_buy()); //分红方式
                tradeRecord.setTargetfundname(StringUtil.isNotBlank(tr.getTarget_fund_code()) ? fundInfoCacheService.getFundInfoExt(tr.getTarget_fund_code()).getFundName() : "--");
                //因为发现接口返回的业务代码有的是代码有的是业务名称
                if (StringUtils.isNumeric(tr.getFund_busin_code())) {
                    tradeRecord.setBusinflagStr(tr.getFund_busin_code() == null ? "--" :
                            commonService.getApplyBusinName(tr.getFund_busin_code()));
                } else {
                    tradeRecord.setBusinflagStr(tr.getFund_busin_code());
                }

                tradeRecord.setKkstat(tr.getDeduct_status());
                tradeRecord.setKkstatstr(commonService.getApplyCheckName(tr.getDeduct_status()));
                String pageSize = "1";
                // 认购
                if ("020".equals(tr.getFund_busin_code())) {
                    pageSize = "2";
                }
                // 根据申请编号查询 s416
                List<TradeConfirmQuery> results = ecQueryApi.tradeConfirm(tr.getAllot_no(), "1", pageSize,
                        null, null, null).getData().getTradeConfirmQuerys();//S416
                if (results != null && results.size() > 0) {
                    TradeConfirmQuery tradeConfirm = null;
                    if ("020".equals(tr.getFund_busin_code()) && results.size() > 1) {
                        for (TradeConfirmQuery confirmQuery : results) {
                            if (StringUtils.equals("130", confirmQuery.getFund_busin_code())) {
                                tradeConfirm = confirmQuery;
                            }
                        }
                        if (tradeConfirm == null) {
                            tradeConfirm = results.get(0);
                        }
                    } else {
                        tradeConfirm = results.get(0);
                    }
                    if (tradeConfirm != null) {
                        tradeRecord.setNetvalue(tradeConfirm.getNet_value());
                        tradeRecord.setTradeconfirmshare(tradeConfirm.getTrade_confirm_type());
                        tradeRecord.setSxf(tradeConfirm.getFare_sx());
                        tradeRecord.setConfirmdate(tradeConfirm.getAffirm_date());
                        tradeRecord.setNavdate(tradeConfirm.getAffirm_date()); //净值日期为确认日期？
                        tradeRecord.setAutobuy(tradeConfirm.getAuto_buy()); //分红方式
                    }
                }
                tradeRecord.setConfirmstat(commonService.getTradeFlagName(tr.getConfirm_flag()));   //交易确认标志
                tradeRecord.setBankname(commonService.getBankName(tr.getBank_no()));  //银行名称
                tradeRecord.setCapitalmodestr(commonService.getCapitalModeName(tr.getCapital_mode())); //资金方式
                newList.add(tradeRecord);
            }
            // 原始交易业务类型 根据原始申请单号查询交易申请
            for (TradeRecord tradeRecord : newList) {
                if (StringUtil.isNotBlank(tradeRecord.getOriginalappno())) {
                    ResultOb<HsTradeApplyQuery> hsTradeApplyQueryResultOb1 = ecQueryApi.tradeApplyQuery(null, null, null,
                            null, tradeRecord.getOriginalappno(),
                            null, null, null,
                            null, null);
                    TradeApplyQuery tradeApplyQuery = new TradeApplyQuery();
                    if (null != hsTradeApplyQueryResultOb1 && null != hsTradeApplyQueryResultOb1.getData()
                            && null != hsTradeApplyQueryResultOb1.getData().getTradeApplyQuerys() &&
                            hsTradeApplyQueryResultOb1.getData().getTradeApplyQuerys().size() > 0) {

                        tradeApplyQuery = hsTradeApplyQueryResultOb1.getData().getTradeApplyQuerys().get(0);
                        tradeRecord.setOriginalcode(tradeApplyQuery.getFund_busin_code());
                        if (StringUtils.isNumeric(tradeRecord.getBusinflag())) {
                            tradeRecord.setOriginalstr(tradeApplyQuery.getFund_busin_code() == null ? "--" :
                                    commonService.getApplyBusinName(tradeApplyQuery.getFund_busin_code()));
                        } else {
                            tradeRecord.setOriginalstr(tradeApplyQuery.getFund_busin_code());
                        }
                    }
                }
            }

            ResultOb<TradeRecord> rb = new ResultOb<>();
            rb.setItems(newList);
            rb.setTotal(Long.valueOf(hsTradeApplyQueryResultOb.getData().getTotal_count()));
            Tools.setSuccessMessage(rb, "OK");
            return rb;
        } else {
            return new ResultOb<>();
        }
    }

    public Integer getTotalPage(String requestsize, String total) {
        //计算总页数
        int totolPages = 0;
        if (Integer.valueOf(requestsize) >= 0 && Integer.valueOf(total) > 0) {
            if (Integer.valueOf(total) % Integer.valueOf(requestsize) == 0) {
                totolPages = Integer.valueOf(total) / Integer.valueOf(requestsize);
            } else {
                totolPages = Integer.valueOf(total) / Integer.valueOf(requestsize) + 1;
            }
        }
        return totolPages;
    }


    private void confirmType(String ofundType) {
        if (StringUtils.isNotBlank(ofundType)) {
            if ("0".equals(ofundType)) {
                ofundType = "1";
            }
            if ("1".equals(ofundType)) {
                ofundType = "2";
            }
            if ("2".equals(ofundType)) {
                ofundType = "";
            }
        }
    }


    /**
     * 分红信息查询(兑付查询)(D406)
     */
    public ResultOb<HsDcBonusQuery> dcBonusQuery(String startdate, String enddate,
                                                 String pageno, String pagesize,
                                                 String accono, String fundcode, String ofundType) {
        //判断ofundType
        this.confirmType(ofundType);
        ResultOb<HsDcBonusQuery> DcBonusQueryResultOb = ecDcQueryApi.dcBonusQuery(accono, startdate, enddate, "1", "1", fundcode, ofundType);
        String total_count = DcBonusQueryResultOb.getData().getTotal_count(); //总记录数
        String totalpages = "";
        if (StringUtil.isNotBlank(pagesize)) {
            if (Integer.valueOf(total_count) % Integer.valueOf(pagesize) == 0) {
                totalpages = String.valueOf((Integer.valueOf(total_count)) / (Integer.valueOf(pagesize)));//总页数
            } else {
                totalpages = String.valueOf((Integer.valueOf(total_count)) / (Integer.valueOf(pagesize)) + 1);
            }
            pageno = String.valueOf(Integer.valueOf(totalpages) - (Integer.valueOf(pageno) - 1)); //页码
        }
        ResultOb<HsDcBonusQuery> hsDcBonusQueryResultOb = ecDcQueryApi.dcBonusQuery(accono, startdate, enddate, pageno, pagesize, fundcode, ofundType);
        List<DcBonus> results = hsDcBonusQueryResultOb.getData().getResults();
        if (results != null && results.size() > 0) {
            this.sort(results);
            hsDcBonusQueryResultOb.getData().setResults(results);
        }
        return hsDcBonusQueryResultOb;
    }

    /**
     * DC交易确认查询 D405
     */
    public Map<String, Object> dcTradeConfirmQuery(String startdate,
                                                   String enddate,
                                                   String pageno,
                                                   String pagesize,
                                                   String fundacco,
                                                   String fundcode,
                                                   String agencyType,
                                                   String fund_busin_code,
                                                   String ofundType) {  //1 公募，2.专户 空 所有
        String s = this.dcCustInfoQuery(fundacco);
        Map<String, Object> map = new HashMap<>();
        List<TradeRecord> newList = new ArrayList<>();
        this.confirmType(ofundType);
        if (StringUtils.isNotBlank(s)) {
            ResultOb<HsDcTradeConfirmQuery> hsDcTradeConfirmQueryResultOb =
                    ecDcQueryApi.dcTradeConfirmQuery(s, fund_busin_code, startdate,
                            enddate, pageno, pagesize, fundcode, agencyType, ofundType);
            List<DcTradeConfirm> results = hsDcTradeConfirmQueryResultOb.getData().getResults();
            if (results != null && results.size() > 0) {
                sortConfirm(results);
            }

            if (results != null) {
                for (DcTradeConfirm result : results) {
                    TradeRecord tradeRecord = new TradeRecord();
                    tradeRecord.setFundcode(result.getFund_code()); //
                    tradeRecord.setFundname(result.getFund_name());
                    tradeRecord.setApplydate(result.getApply_date());
                    tradeRecord.setApplydate(result.getApply_date());
                    tradeRecord.setAgencyno("1"); //代销
                    tradeRecord.setAgencyname(result.getAgency_name());
                    tradeRecord.setApplyshare(result.getApply_share());
                    tradeRecord.setApplysum(result.getApply_sum());
                    tradeRecord.setBalancecoin(result.getMoney_type());
                    tradeRecord.setBusinflag(result.getBusin_name());
                    tradeRecord.setNetvalue(result.getNet_value());
                    tradeRecord.setTradeconfirmshare(result.getTrade_confirm_type());
                    tradeRecord.setSxf(result.getFare_sx());
                    tradeRecord.setConfirmdate(result.getAffirm_date());
                    tradeRecord.setBusinflag(result.getBusin_flag()); //业务代码
                    tradeRecord.setTradeacco(result.getTrade_acco());// 交易账号
                    tradeRecord.setApplyserial(result.getAllot_no()); //申请编号
                    tradeRecord.setFromfundcode(result.getFrom_fund_code()); //转出产品代码
                    tradeRecord.setFromfundcodename(result.getFrom_fund_name());
                    tradeRecord.setTargetfundcode(result.getOppo_fund_code());
                    tradeRecord.setTargetfundname(result.getOppo_fund_name()); //转入产品代码
                    if ("50".equals(result.getBusin_flag())) {
                        tradeRecord.setBusinflagStr("基金成立");
                    } else if ("13".equals(result.getBusin_flag())) {
                        tradeRecord.setBusinflagStr("基金转换出");
                    } else if ("16".equals(result.getBusin_flag())) {
                        tradeRecord.setBusinflagStr("基金转换入");
                    } else {
                        tradeRecord.setBusinflagStr(result.getBusin_flag() != null ? commonService.getConfirmCheckName(result.getBusin_flag()) : "--");
                    }
                    if ("0".equals(result.getApply_share()) && "0".equals(result.getApply_sum())) {
                        logger.info("=============allotno==================>>>" + result.getAllot_no());
                        ResultOb<HsTradeApplyQuery> recordResultOb =
                                ecQueryApi.tradeApplyQuery(null, null, null,
                                        null, result.getAllot_no(),
                                        null, null, null,
                                        null, null);
                        logger.info("============recordResultOb==========>>>" + recordResultOb);
                        HsTradeApplyQuery data = recordResultOb.getData();
                        if (data != null && data.getTradeApplyQuerys() != null && data.getTradeApplyQuerys().size() > 0) {
                            TradeApplyQuery tq = data.getTradeApplyQuerys().get(0);
                            tradeRecord.setApplyshare(tq.getShares());
                            tradeRecord.setApplysum(tq.getBalance());
                        }
                    }
                    newList.add(tradeRecord);
                }
            }

            map.put("totalpage", hsDcTradeConfirmQueryResultOb.getData().getTotal_page());
            map.put("result", newList);
            return map;
        } else {
            logger.error("======================数据中心客户编号为空=========================================");
        }
        return new HashMap<>();
    }


    /**
     * 交易申请查询(D404)
     */
    public ResultOb tradeResultQueryDC(String startdate, String enddate,
                                       String pageno, String pagesize,
                                       String fund_busin_code, String fundacco, String ofundType) {

        String s = this.dcCustInfoQuery(fundacco);
        return ecDcQueryApi.dcTradeApplyQuery(s, startdate, fund_busin_code, enddate, pageno, pagesize, ofundType);

    }


    /**
     * 查询可撤单列表
     */
    private Map queryWithdrawMap(String client_id) {
        Map<String, Object> withdrawmap = new HashMap<>();
        ResultOb<HsWithdrawListQuery> hsWithdrawListQueryResultOb =
                tradePurchaseApplyService.withdrawListQuery(client_id);
        List<WithdrawListQuery> withdrawResult = hsWithdrawListQueryResultOb.getData().getWithdrawSetQuerys();

        if (withdrawResult != null && withdrawResult.size() > 0) {
            int size = withdrawResult.size();
            for (int i = 0; i < size; i++) {
                WithdrawListQuery wq = withdrawResult.get(i);
                withdrawmap.put(wq.getAllot_no(), withdrawResult);  //申请编号为key,对象为value存入map
            }
        }
        return withdrawmap;
    }


    /**
     * 客户信息查询(D401)
     */
    private String dcCustInfoQuery(String acco_no) {
        ResultOb<HsDcCustInfoQuery> hsCustInfoQueryResultOb = ecDcQueryApi.custInfoQueryByAccoNo(acco_no, "2");
        if (hsCustInfoQueryResultOb.getData() != null) {
            List<DcCustInfo> results = hsCustInfoQueryResultOb.getData().getResults();
            if (results != null && results.size() > 0) {
                DcCustInfo dcCustInfo = results.get(0);
                return dcCustInfo.getCust_no();   //数据中心客户编号
            }
        }
        logger.error(BusyErrorCode.DC_CUSTOMER + ":数据中心客户信息查询失败:acco_no=" + acco_no
                + ":code=" + hsCustInfoQueryResultOb.getHsCode()
                + ":message=" + hsCustInfoQueryResultOb.getMessage()
                + ":data=" + hsCustInfoQueryResultOb.getData());
        return "";
    }

    private String dcCustInfoQuery(String idType, String idNo) {
        ResultOb<HsDcCustInfoQuery> hsCustInfoQueryResultOb = ecDcQueryApi.custInfoQuery(idType, idNo);
        List<DcCustInfo> results = hsCustInfoQueryResultOb.getData().getResults();
        if (results != null) {
            DcCustInfo dcCustInfo = results.get(0);
            return dcCustInfo.getCust_no();   //数据中心客户编号
        }
        logger.error(BusyErrorCode.DC_CUSTOMER + ":数据中心客户信息查询失败:idNo=" + idNo
                + ":idType=" + idType
                + ":code=" + hsCustInfoQueryResultOb.getHsCode()
                + ":message=" + hsCustInfoQueryResultOb.getMessage()
                + ":data=" + hsCustInfoQueryResultOb.getData().getResults());
        return "";
    }

    /**
     * 交易记录业务方法
     */
    public Result tradeQueryByCode(DsInfo dsInfo, String startdate, String enddate,
                                   String callingcode, String allot_no,
                                   String pageno, String applyrecordno,
                                   String fundcode,
                                   String scheduledprotocolid, String queryFundType) throws ParseException {

        //将endDate 设置为当天所属工作日的交易
//        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
//        String work_date = ecQueryApi.workDateQuery(enddate, "2",
//                "0").getData().getWork_date();
//
//        enddate = work_date;
        if (StringUtils.isBlank(applyrecordno)) {
            applyrecordno = "10";
        }
        Result result = new Result();
        Map<String, Object> map = new HashMap<>();
//        dsInfo.setFundAccount("371000000045");
//        dsInfo.setCid("48");
        if ("13".equals(callingcode) || "16".equals(callingcode) || "50".equals(callingcode)) {
            //查询交易确认（基金转换入，转换出，基金成立）
            Map<String, Object> dcmap = this.dcTradeConfirmQuery(startdate, enddate, pageno,
                    applyrecordno, dsInfo.getFundAccount(), fundcode, null, callingcode, queryFundType);
            map.put("totalPage", dcmap.get("totalpage"));
            map.put("result", dcmap.get("result"));
        } else if ("999".equals(callingcode)) {
            //分红列表
            ResultOb<HsDcBonusQuery> hsDcBonusQueryResultOb = this.dcBonusQuery(startdate, enddate, pageno,
                    applyrecordno, dsInfo.getFundAccount(), fundcode, queryFundType);
            map.put("totalPage", hsDcBonusQueryResultOb.getData().getTotal_page());
            map.put("result", hsDcBonusQueryResultOb.getData().getResults());
        } else if ("confirm".equals(callingcode)) {//S416交易确认查询
            ResultOb<TradeRecord> recordResultOb = this.doConfirm(dsInfo, pageno, applyrecordno, startdate, enddate, queryFundType, fundcode);
            map.put("totalPage", this.getTotalPage(applyrecordno, recordResultOb.getTotal().toString()));
            map.put("result", recordResultOb.getItems());
        } else {
            //代销交易
            Map<String, Object> dcmap = this.dcTradeConfirmQuery(startdate, enddate,
                    pageno, applyrecordno, dsInfo.getFundAccount(), fundcode, "1", callingcode, queryFundType);
            //直销交易
            ResultOb<TradeRecord> recordResultOb = this.tradeQuery(dsInfo.getCid(), startdate, enddate, allot_no,
                    applyrecordno, fundcode, scheduledprotocolid, pageno, callingcode, queryFundType);
            logger.info("================recordOB==================================" + JsonUtils.toJson(recordResultOb));
            if (recordResultOb.getTotal() != null) {
                map.put("totalPage", this.getTotalPage(applyrecordno, recordResultOb.getTotal().toString()));
            } else {
                map.put("totalPage", "0");
            }
            map.put("result", recordResultOb.getItems());
            map.put("dcShare", dcmap.get("result")); //代销交易明细
        }
        result.setData(map);
        Tools.setSuccessMessage(result, "success");
        return result;
    }

    //处理份额确认查询
    private ResultOb<TradeRecord> doConfirm(DsInfo dsInfo, String pageno, String applyrecordno, String startdate, String enddate, String ofundType, String fundcode) {
        String dcClientId = this.getDcClientId(dsInfo);
        logger.debug("########交易确认查询########");
        ResultOb<HsDcTradeConfirmQuery> hsDcTradeConfirmQueryResultOb =
                ecDcQueryApi.dcTradeConfirmQuery(dcClientId, null, startdate, enddate, pageno, applyrecordno, fundcode, null, ofundType);
        List<DcTradeConfirm> results = hsDcTradeConfirmQueryResultOb.getData().getResults();
        List<TradeRecord> newList = new ArrayList<>();
        if (results != null && results.size() > 0) {
            for (DcTradeConfirm tr : results) {
                TradeRecord tradeRecord = new TradeRecord();
                tradeRecord.setFundcode(tr.getFund_code());
                tradeRecord.setFundname(tr.getFund_name());
                tradeRecord.setAgencyno("0"); //直销
                tradeRecord.setAgencyname("上投摩根");
                tradeRecord.setApplydate(tr.getAffirm_date()); //权宜之计
                tradeRecord.setApplyserial(tr.getAllot_no()); //申请编号
                tradeRecord.setApplyshare(tr.getApply_share()); //交易份额
                tradeRecord.setApplysum(tr.getApply_sum()); //交易金额
                tradeRecord.setBalancecoin(tr.getMoney_type());
                tradeRecord.setBusinflag(tr.getBusin_flag());
                tradeRecord.setBusinflagStr(tr.getBusin_name());
                tradeRecord.setTradeacco(tr.getTrade_acco()); //交易账号
                tradeRecord.setApplyserial(tr.getAllot_no()); //申请编号
                tradeRecord.setSharetype(tr.getShare_type());
                tradeRecord.setSxf(tr.getFare_sx()); //交易手续费
                tradeRecord.setNetvalue(tr.getNet_value()); //成交净值
                tradeRecord.setNavdate(tr.getAffirm_date());
                tradeRecord.setConfirmstat(tr.getTrade_status_name()); //交易状态
                tradeRecord.setFromfundcodename(tr.getFrom_fund_code());
                tradeRecord.setFromfundcodename(tr.getFrom_fund_name());
                tradeRecord.setTargetfundcode(tr.getOppo_fund_code());
                tradeRecord.setTargetfundname(tr.getOppo_fund_name());
                tradeRecord.setBalancecoin(tr.getMoney_type());
                tradeRecord.setTradeconfirmshare(tr.getTrade_confirm_type());
                tradeRecord.setTradeconfirmbalance(tr.getTrade_confirm_balance());
                tradeRecord.setConfirmdate(tr.getAffirm_date()); //确认日期
                logger.debug("fundCode=" + tr.getFund_code() + ",手续费=" + tr.getFare_sx());
                if ("07".equals(tr.getBusin_flag())) { //如果是修改分红方式  数据中心分红业务代码为07
                    if ("0".equals(tr.getAuto_buy())) {//红利再投资
                        tradeRecord.setBeforemelond("1");
                    } else if ("1".equals(tr.getAuto_buy())) {
                        tradeRecord.setBeforemelond("0");
                    }
                }
                tradeRecord.setAutobuy(tr.getAuto_buy()); //分红方式
                newList.add(tradeRecord);
            }
        }
        ResultOb<TradeRecord> rb = new ResultOb<>();
        rb.setItems(newList);
        rb.setTotal(Long.valueOf(hsDcTradeConfirmQueryResultOb.getData().getTotal_count()));
        Tools.setSuccessMessage(rb, "OK");
        return rb;
    }

    //可赎回列表
    public ResultOb redeemQuery(String cid, String fundcode) {
        RedeemForm redeemForm = new RedeemForm();
        String subbankacco = "-";
        String payment = "-";
        String bankname = "-";
        String shareType = "";
        String fund_name = "";
        ResultOb<HsRedeemListQueryResult> hsRedeemListQueryResultResultOb = ecTradeFundApi.redeemListQuery(cid, fundcode);
        List<RedeemListQuery> redeemListQuerys = hsRedeemListQueryResultResultOb.getData().getRedeemListQuerys();
        List<HsReddemListQuery> result = new ArrayList<>();
        for (RedeemListQuery rq : redeemListQuerys) {
            if (Double.parseDouble(rq.getWorth_value()) > 0) {
                HsReddemListQuery query = new HsReddemListQuery();
                Tools.copyProperties(query, rq);
                result.add(query);
            }
        }

        logger.info("=====================result.size===================" + result.size());
        for (HsReddemListQuery info : result) {
            fund_name = info.getFund_name();
            shareType = info.getShare_type();
            if (StringUtil.isNotBlank(info.getBank_account())) {
                subbankacco = info.getBank_account().substring(info.getBank_account().length() - 4); //后四位
            }
            if (StringUtil.isNotBlank(info.getBank_no())) {
                bankname = commonService.getBankName(info.getBank_no());
                info.setBankname(bankname);
            }
            if (StringUtil.isNotBlank(info.getCapital_mode())) {
                payment = commonService.getCapitalModeName(info.getCapital_mode());
                info.setCapitalmodename(payment);
            }
            info.setPaystr(bankname + "[尾号" + subbankacco + "] " + payment);
        }
        //交易限制查询 s435
        HsTradeLimitQuery limitresult = ecDcQueryApi.tradeLimitQuery(fundcode, null, shareType, "024", null).getData();
        logger.info("================================================================" + limitresult);
        //交易限制
        if (limitresult != null) {
            redeemForm.setAddvalue(limitresult.getAdd_value());
            redeemForm.setLow_value(limitresult.getLow_value());
            redeemForm.setHighvalue(limitresult.getHigh_value());
            redeemForm.setLimitMsg(limitresult.getMessage());
        }
        redeemForm.setFundcode(fundcode);
        redeemForm.setFundname(fund_name);
        redeemForm.setRedeemListQuerys(result);
        ResultOb<RedeemForm> rb = new ResultOb<>();
        rb.setData(redeemForm);
        Tools.setSuccessMessage(rb, "OK");
        return rb;
    }

    //定投记录 and 现金宝转出记录
    public Result fixRecord(String cid, String scheduledprotocolid, String begindate, String enddate) {
        Map<String, Object> recoredmap = new HashMap<>();
        List<FixRecord> fixrecord = new ArrayList<>();
        List<FixRecord> cashcovert = new ArrayList<>();
        //根据协议号查询直销所有定投记录
        List<TradeApplyQuery> items = ecQueryApi.tradeApplyQuery(cid, begindate, enddate, null, null,
                "1", "999", null, scheduledprotocolid, null).getData().getTradeApplyQuerys();
        if (items != null) {
            //定投记录传递业务代码039 和定投协议号   筛选
            for (TradeApplyQuery bean : items) {
                //根据申请编号查询交易详情
                List<TradeConfirmQuery> results =
                        ecQueryApi.tradeConfirm(bean.getAllot_no(), "1", "2", null, null, null).getData().getTradeConfirmQuerys();
                if (results != null && results.size() > 0) {
                    // TradeConfirmQuery tradeResultQuery = results.get(0);
                    for (TradeConfirmQuery tradeResultQuery : results) {
                        if ("039".equals(bean.getFund_busin_code()) && "1".equals(bean.getConfirm_flag())
                                && "2".equals(bean.getDeduct_status())) {
                            //银行卡定投记录
                            if (tradeResultQuery != null) {
                                fixrecord.add(this.combine(tradeResultQuery, bean));
                            }
                        }
                        if ("985".equals(bean.getFund_busin_code())
                                && (StringUtils.isNotBlank(bean.getTarget_fund_code())
                                && "000857".equals(bean.getFund_code())) && "2".equals(bean.getDeduct_status())
                                && "1".equals(bean.getConfirm_flag())) {
                            if (tradeResultQuery != null) {
                                cashcovert.add(this.combine(tradeResultQuery, bean));
                            }
                        }
                    }
                }
            }
        }
        recoredmap.put("fixrecord", fixrecord);
        recoredmap.put("cashcovert", cashcovert);  //该定投现金宝转出记录
        //无需考虑代销  代销记录肯定查不到了啦
        Result result = new Result();
        result.setData(recoredmap);
        Tools.setSuccessMessage(result, "success");
        return result;
    }


    //拼装
    public FixRecord combine(TradeConfirmQuery tradeResultQuery, TradeApplyQuery bean) {
        FixRecord tradeRecord = new FixRecord();
        tradeRecord.setFundcode(tradeResultQuery.getFund_code());
        tradeRecord.setFundname(tradeResultQuery.getFund_name());
        tradeRecord.setCapitalmode(bean.getCapital_mode()); //资金方式
        tradeRecord.setCapitalmodestr(bean.getCapital_mode() == null ? "--" :
                commonService.getCapitalModeName(bean.getCapital_mode()));
        tradeRecord.setApplydate(tradeResultQuery.getApply_date());
        tradeRecord.setApplytime(tradeResultQuery.getAccept_time());
        tradeRecord.setConfirmdate(tradeResultQuery.getAffirm_date()); //确认日期
        tradeRecord.setBankacco(tradeResultQuery.getBank_account());
        tradeRecord.setBankname(commonService.getBankName(bean.getBank_no()));
        tradeRecord.setSxf(tradeResultQuery.getFare_sx());
        tradeRecord.setTradeconfirmshare(tradeResultQuery.getTrade_confirm_type());
        tradeRecord.setNetvalue(tradeResultQuery.getNet_value());
        tradeRecord.setCallingcode(tradeResultQuery.getFund_busin_code());
        tradeRecord.setTradeconfirmbalance(tradeResultQuery.getTrade_confirm_balance());
        tradeRecord.setApplysum(bean.getBalance()); //发生金额
        tradeRecord.setConfirmflag(bean.getConfirm_flag()); //交易记录的确认标志
        tradeRecord.setOppofundcode(tradeResultQuery.getTarget_fund_code());
        tradeRecord.setOppfundname(tradeResultQuery.getTarget_fund_name());
        return tradeRecord;
    }

    /**
     * S407交易结果查询
     */
    public ResultOb tradeResultQuery(String businflag, String cid, String alltno) {
        return ecQueryApi.tradeResultQuery(businflag, cid, alltno);
    }


    public Map riskStr(String risknum) {
        String str = "";
        String product = "";
        Map<String, String> map = new HashMap<>();
        switch (risknum) {
            case "1":
                str = "保守型";
                product = "较低风险";
                map.put("investRisk", str);
                map.put("productRisk", product);
                break;
            case "2":
                str = "平衡型";
                product = "较低风险";
                map.put("investRisk", str);
                map.put("productRisk", product);
                break;
            case "3":
                str = "稳健型";
                product = "中等风险";
                map.put("investRisk", str);
                map.put("productRisk", product);
                break;
            case "4":
                str = "积极性";
                product = "较高风险";
                map.put("investRisk", str);
                map.put("productRisk", product);
                break;
            case "5":
                str = "进取型";
                product = "较高风险";
                map.put("investRisk", str);
                map.put("productRisk", product);
                break;
            default:
                str = "";
        }
        return map;
    }

    public void setForm(WarningBean formViewModel, ResultOb<HsCautionqueryResult> cautionquery, String invest_risk_tolerance, String fundName, String fundRisk) {
        Map<String, String> map = this.riskStr(invest_risk_tolerance);//客户风险等级
        if (cautionquery.isSuccess() && cautionquery.getData() != null) {
            List<ICautionQuerys> items = cautionquery.getData().getResults();
            for (ICautionQuerys bean : items) {
                if ("A".equals(bean.getCaution_type())) {
                    formViewModel.setWarningANo(bean.getCaution_id());
                    // {4} 客户风险等级  {5} 基金风险等级
                    String str = bean.getCaution_content().replace("{4}", map.get("investRisk")).replace("{5}", map.get("productRisk"));
                    formViewModel.setWarningAContent(str);
                } else if ("B".equals(bean.getCaution_type())) {
                    formViewModel.setWarningBNo(bean.getCaution_id());
                    formViewModel.setWarningBContent(bean.getCaution_content());
                } else if ("C".equals(bean.getCaution_type())) {
                    formViewModel.setWarningCNo(bean.getCaution_id());
                    String str = bean.getCaution_content().replace("{0}", fundName).replace("{1}", fundRisk);
                    formViewModel.setWarningCContent(str);
                } else if ("D".equals(bean.getCaution_type())) {
                    formViewModel.setWarningDNo(bean.getCaution_id());
                    formViewModel.setWarningDContent(bean.getCaution_content());
                } else if ("E".equals(bean.getCaution_type())) {
                    formViewModel.setWarningENo(bean.getCaution_id());
                    formViewModel.setWarningEContent(bean.getCaution_content());
                }
            }
        }
    }

    public Result dcInvestProfitDetailCustomQuery(HttpServletRequest request, String bdate, String endDate, String fundCode) {
        DsInfo dsInfo = WebTools.getDsInfo(request);
        List<DcInvestProfitDetailCustom> list = new ArrayList<>();
        Result result = new Result();
        String dcClientId = this.getDcClientId(dsInfo);

        if (StringUtil.isNotBlank(dcClientId)) {
            ResultOb<HsDcInvestProfitDetailCustomQuery> resultOb =
                    ecDcQueryApi.dcInvestProfitDetailCustomQuery(bdate, endDate, fundCode, dcClientId);
            logger.info("===============reslutOb==================" + JSON.toJSONString(resultOb));
            if (resultOb != null & resultOb.getData().getResults() != null && resultOb.getData().getResults().size() > 0) {
                if (StringUtil.isNotBlank(fundCode)) {
                    Map<String, DcInvestProfitDetailCustom> map = new HashedMap();
                    List<DcInvestProfitDetailCustom> results = resultOb.getData().getResults();
                    for (DcInvestProfitDetailCustom dc : results) {
                        map.put(dc.getFund_code(), dc);
                    }
                    DcInvestProfitDetailCustom bean = map.get(fundCode);
                    if (null != bean) {
                        list.add(bean);
                    }
                } else {
                    list = resultOb.getData().getResults();
                }
            }
        }
        result.setItems(list);
        Tools.setSuccessMessage(result, "success");
        return result;
    }

}

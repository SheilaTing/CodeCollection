package com.cifm.dc.server.api.trade;

import com.cifm.dc.server.api.model.base.ResultModel;
import com.cifm.dc.server.api.model.base.ResultOb;
import com.cifm.dc.server.api.model.base.UserInfo;
import com.cifm.dc.server.common.interceptor.Right;
import com.cifm.dc.server.common.web.FileMeta;
import com.cifm.dc.server.entity.account.User;
import com.cifm.dc.server.entity.sys.Attach;
import com.cifm.dc.server.entity.trade.Trade;
import com.cifm.dc.server.service.sys.AttachService;
import com.cifm.dc.server.service.sys.MyMessageSource;
import com.cifm.dc.server.service.trade.TradeService;
import com.cifm.dc.server.utils.*;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

/**
 * @author 陈平
 * @version 1.0
 * @since 2016/11/1
 */
@Controller
@RequestMapping(value = "/api/trade")
public class TradeUploadController {

    protected final Logger LOGGER = LoggerFactory.getLogger(TradeUploadController.class);

    @Autowired
    private AttachService attachService;
    @Autowired
    private TradeService tradeService;
    @Autowired
    private MyMessageSource message;

    @ApiOperation(value = "文件上传", tags = "file")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "code", value = "编码", paramType = "query", required = true),
            @ApiImplicitParam(name = "refType", value = "关联类型,trade/user", paramType = "query", required = true),
            @ApiImplicitParam(name = "refId", value = "关联主键", paramType = "query", required = true)
    })
    @ResponseBody
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
//    @Right("cifmUser")
    public ResultModel upload(@RequestParam String code,
                              @RequestParam String refType,
                              @RequestParam String refId,
                              MultipartHttpServletRequest request) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Iterator<String> itr = request.getFileNames();
        MultipartFile mpf;
        FileMeta fileMeta;
        UserInfo userInfo = Tools.getUserInfo(request);
        LOGGER.debug("code=" + code);
        if (StringUtils.equals("trade", refType)) {
            Trade trade = tradeService.findOne(refId);
            if (trade == null) {
                return Tools.getError("交易不存在");
            }
        }
        if (StringUtils.equals("user", refType)) {
            User user = AppCache.userGet(refId);
            if (user == null) {
                return Tools.getError("用户不存在");
            }
        }
        ResultOb<FileMeta> resultOb = new ResultOb<>();
        while (itr.hasNext()) {
            mpf = request.getFile(itr.next());
            LOGGER.debug(mpf.getOriginalFilename() + " uploaded! ");
            fileMeta = new FileMeta();
            fileMeta.setFileName(mpf.getOriginalFilename());
            fileMeta.setFileSize(mpf.getSize() / 1024 + " Kb");
            fileMeta.setFileType(mpf.getContentType());
            LOGGER.debug("file size=" + fileMeta.getFileSize());
            if ((mpf.getSize() / 1024) > 10240) {
                resultOb.setData(fileMeta);
                Tools.setErrorMessage(resultOb, "太大");
                return resultOb;
            }
            // Constants.FILE_PATH: public static final String FILE_PATH = File.separator + "static" + File.separator + "upload" + File.separator
            try {
                String relativePath = Constants.FILE_PATH + File.separator + sdf.format(new Date());
                relativePath = StringUtils.replace(relativePath, File.separator, "/");
                fileMeta.setRelativePath(relativePath);
                String savePath = getFolder(request);
                LOGGER.debug("savePath=" + savePath);
                fileMeta.setAbsolutePath(savePath);
                String fileName = this.getRandomName(mpf.getOriginalFilename());
                fileMeta.setNewName(fileName);
                InputStream is = new ByteArrayInputStream(mpf.getBytes());
                boolean isImage = PicUtils.isImage(is);
                LOGGER.debug("isImage=" + isImage);
                if (isImage) {
                    FileCopyUtils.copy(mpf.getBytes(), new FileOutputStream(savePath + File.separator + fileName));
                    fileMeta.setSuccess(true);
                    Attach attach = FileMeta.getFromFileMeta(fileMeta);
                    attach.setCuser(userInfo.getUid());
                    attach.setCtime(new Date());
                    attach.setStat(1);
                    attach.setCode(code);
                    attach.setRefType(refType);
                    attach.setRefId(refId);
                    Tools.saveBase(attach, userInfo.getUid());
                    attach = attachService.save(attach);
                    fileMeta.setId(attach.getId());
                    LOGGER.debug("attachId=" + attach.getId());
                    resultOb.setData(fileMeta);
                    Tools.setSuccessMessage(resultOb, message.getMessage(MessageUtils.opSuccess[1]));
                } else {
                    Tools.setErrorMessage(resultOb, "不是图片");
                }
            } catch (IOException e) {
                LOGGER.error("", e);
                fileMeta.setSuccess(false);
                Tools.setErrorMessage(resultOb, message.getMessage(MessageUtils.opError[1]));
            }
        }
        return resultOb;
    }

    @ApiOperation(value = "文件下载", tags = "file")
    @ApiImplicitParam(name = "id", value = "附件主键", paramType = "query", required = true)
    @RequestMapping(value = "download", method = RequestMethod.GET)
//    @Right("cifmUser")
    public ResponseEntity<byte[]> download(@RequestParam(value = "id") String id) throws IOException {
//        Attach attach = attachService.findOne(id);
        Attach attach = attachService.findByRefId(id);
        if (attach != null) {
            String filePath = attach.getAbsolutePath() + File.separator + attach.getNewName();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", URLEncoder.encode(attach.getName(), "UTF-8"));
            File file = new File(filePath);
            return new ResponseEntity<>(FileUtils.readFileToByteArray(file), headers, HttpStatus.CREATED);
        } else {
            return null;
        }
    }

    private String getFolder(HttpServletRequest request) {
        String physicalPath = getPhysicalPath(request);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String path = physicalPath + File.separator + sdf.format(new Date());
        File dir = new File(path);
        if (!dir.exists()) {
            boolean bool = dir.mkdirs();
            LOGGER.debug("getFolder=" + bool);
        }
        return path;
    }

    private String getPhysicalPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String realPath = request.getSession().getServletContext().getRealPath(servletPath);
        LOGGER.debug("realPath=" + realPath);
        return new File(realPath).getParentFile().getParentFile().getParent() + Constants.FILE_PATH;
    }

    public String getRandomName(String fileName) {
        SecureRandom random = new SecureRandom();
        return "" + random.nextInt(10000) + System.currentTimeMillis() + "." + getFileExt(fileName);
    }

    /**
     * 得到指定文件的扩展名
     *
     * @param filePathName 文件名
     * @return 如“jpg”、“png”、“gif”
     */
    public String getFileExt(String filePathName) {
        String value;
        int start;
        int end;
        if (filePathName == null) {
            return null;
        }
        // "."的ASCII码是46
        start = filePathName.lastIndexOf(46) + 1;
        end = filePathName.length();
        value = filePathName.substring(start, end);
        if (filePathName.lastIndexOf(46) > 0) {
            return value;
        } else {
            return "";
        }
    }
}

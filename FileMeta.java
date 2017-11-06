package com.cifm.dc.server.common.web;

import com.cifm.dc.server.entity.sys.Attach;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * @author 陈平
 * @version 1.0
 * @since 16/11/1
 */
public class FileMeta implements Serializable {

    private String id;
    private Boolean success;
    private String fileName;
    private Long size;
    private String fileSize;
    private String fileType;
    private String newName;
    private String relativePath;
    private String absolutePath;
    private String comm1;
    private String comm2;
    private String comm3;

    @JsonIgnore
    private byte[] bytes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getComm1() {
        return comm1;
    }

    public void setComm1(String comm1) {
        this.comm1 = comm1;
    }

    public String getComm2() {
        return comm2;
    }

    public void setComm2(String comm2) {
        this.comm2 = comm2;
    }

    public String getComm3() {
        return comm3;
    }

    public void setComm3(String comm3) {
        this.comm3 = comm3;
    }

    public static Attach getFromFileMeta(FileMeta fileMeta) {
        Attach attach = new Attach();
        attach.setName(fileMeta.getFileName());
        attach.setAttachSize(fileMeta.getSize());
        attach.setFileSize(fileMeta.getFileSize());
        attach.setFileType(fileMeta.getFileType());
        attach.setNewName(fileMeta.getNewName());
        attach.setAbsolutePath(fileMeta.getAbsolutePath());
        attach.setRelativePath(fileMeta.getRelativePath());
        attach.setComm1(fileMeta.getComm1());
        attach.setComm2(fileMeta.getComm2());
        attach.setComm3(fileMeta.getComm3());
        return attach;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}

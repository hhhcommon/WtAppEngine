package com.woting.dataanal.gather.API.persis.pojo;

import java.sql.Timestamp;

import com.spiritdata.framework.core.model.BaseObject;

/**
 * Api数据收集持久化对象，与数据库中ld_API表对应
 * @author wh
 */
public class ApiLogPo extends BaseObject {
    private static final long serialVersionUID=-5047650691581766045L;

    private String id; //任务组id
    private String reqUrl; //请求的URL
    private String method; //请求方式是POST/GET/DEL等
    private String reqParam; //请求中的参数，形式为JSON
    private String apiName; //Api名称
    private int ownerType; //任务组所对应的所有者类型（1=注册用户;2=非注册用户(session);3=系统生成）
    private String ownerId; //所有者标识（可能是用户id，也可能是SessionID，也可能是'Sys'）
    private int deviceType=0; //设备类型；1App;2WoTing;3Pc
    private String deviceId; //设备ID(移动端是IMEI,PC是SessionId)
    private String deviceClass; //设备型号
    private String exploreName; //浏览器名称：若是PC的请求
    private String exploreVer; //浏览器型号：若是PC的请求
    private String objType; //api的分类，按模块
    private String objId; //访问实体的ID
    private int dealFlag=0; //处理过程0正在处理1处理成功2处理失败
    private String returnData; //返回数据，以JSON形式
    private Timestamp beginTime; //开始处理时间
    private Timestamp endTime; //结束处理时间

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id=id;
    }
    public String getReqUrl() {
        return reqUrl;
    }
    public void setReqUrl(String reqUrl) {
        this.reqUrl=reqUrl;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method=method;
    }
    public String getReqParam() {
        return reqParam;
    }
    public void setReqParam(String reqParam) {
        this.reqParam=reqParam;
    }
    public String getApiName() {
        return apiName;
    }
    public void setApiName(String apiName) {
        this.apiName=apiName;
    }
    public int getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(int ownerType) {
        this.ownerType=ownerType;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(String ownerId) {
        this.ownerId=ownerId;
    }
    public int getDeviceType() {
        return deviceType;
    }
    public void setDeviceType(int deviceType) {
        this.deviceType=deviceType;
    }
    public String getDeviceId() {
        return deviceId;
    }
    public void setDeviceId(String deviceId) {
        this.deviceId=deviceId;
    }
    public String getDeviceClass() {
        return deviceClass;
    }
    public void setDeviceClass(String deviceClass) {
        this.deviceClass=deviceClass;
    }
    public String getExploreName() {
        return exploreName;
    }
    public void setExploreName(String exploreName) {
        this.exploreName=exploreName;
    }
    public String getExploreVer() {
        return exploreVer;
    }
    public void setExploreVer(String exploreVer) {
        this.exploreVer=exploreVer;
    }
    public String getObjType() {
        return objType;
    }
    public void setObjType(String objType) {
        this.objType=objType;
    }
    public String getObjId() {
        return objId;
    }
    public void setObjId(String objId) {
        this.objId=objId;
    }
    public int getDealFlag() {
        return dealFlag;
    }
    public void setDealFlag(int dealFlag) {
        this.dealFlag=dealFlag;
    }
    public String getReturnData() {
        return returnData;
    }
    public void setReturnData(String returnData) {
        this.returnData=returnData;
    }
    public Timestamp getBeginTime() {
        return beginTime;
    }
    public void setBeginTime(Timestamp beginTime) {
        this.beginTime=beginTime;
    }
    public Timestamp getEndTime() {
        return endTime;
    }
    public void setEndTime(Timestamp endTime) {
        this.endTime=endTime;
    }
}
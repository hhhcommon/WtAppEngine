package com.woting.version.core.model;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.spiritdata.framework.core.model.BaseObject;
import com.spiritdata.framework.util.DateUtils;
import com.spiritdata.framework.util.StringUtils;

public class Version extends BaseObject {
    private static final long serialVersionUID=-1670331511245273735L;

    private int id; //版本ID(UUID)自动增加的顺序号
    private String appName; //应用名称，这里的App不单单值手机应用
    private String version; //版本号，此版本号的规则由程序通过正则表达式进行处理
    private String verMemo; //版本描述，可以是一段html
    private String bugMemo; //版本bug修改情况描述，可以是一段html
    private int pubFlag; //发布状态：1=已发布；0=未发布；此状态用于今后扩展，目前只有1
    private String apkFile; //版本发布物的访问Url,目前仅针对apk
    private int apkSize; //版本发布物尺寸大小，是字节数,目前仅针对apk
    private int isCurVer; //是否是当前版本，0不是，1是
    private Timestamp pubTime; //发布时间
    private Timestamp CTime; //创建时间
    private Timestamp lmTime; //最后修改时间
    private String extHisPatchInfo; //历史版本的修改信息,删除版本时，用此保存被删除版本的说明

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id=id;
    }
    public String getAppName() {
        return appName;
    }
    public void setAppName(String appName) {
        this.appName=appName;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version=version;
    }
    public String getVerMemo() {
        return verMemo;
    }
    public void setVerMemo(String verMemo) {
        this.verMemo=verMemo;
    }
    public String getBugMemo() {
        return bugMemo;
    }
    public void setBugMemo(String bugMemo) {
        this.bugMemo=bugMemo;
    }
    public int getPubFlag() {
        return pubFlag;
    }
    public void setPubFlag(int pubFlag) {
        this.pubFlag=pubFlag;
    }
    public String getApkFile() {
        return apkFile;
    }
    public void setApkFile(String apkFile) {
        this.apkFile=apkFile;
    }
    public int getApkSize() {
        return apkSize;
    }
    public void setApkSize(int apkSize) {
        this.apkSize=apkSize;
    }
    public int getIsCurVer() {
        return isCurVer;
    }
    public void setIsCurVer(int isCurVer) {
        this.isCurVer=isCurVer;
    }
    public Timestamp getPubTime() {
        return pubTime;
    }
    public void setPubTime(Timestamp pubTime) {
        this.pubTime=pubTime;
    }
    public Timestamp getCTime() {
        return CTime;
    }
    public void setCTime(Timestamp CTime) {
        this.CTime=CTime;
    }
    public Timestamp getLmTime() {
        return lmTime;
    }
    public void setLmTime(Timestamp lmTime) {
        this.lmTime=lmTime;
    }
    public String getExtHisPatchInfo() {
        return extHisPatchInfo;
    }
    public void setExtHisPatchInfo(String extHisPatchInfo) {
        this.extHisPatchInfo=extHisPatchInfo;
    }

    /**
     * 转换为为App所用的显示Map
     * @return
     */
    public Map<String, Object> toViewMap4App() {
        Map<String, Object> retM=new HashMap<String, Object>();
        if (StringUtils.isNullOrEmptyOrSpace(this.getAppName())) return null;
        if (StringUtils.isNullOrEmptyOrSpace(this.getVersion())) return null;

        retM.put("AppName", this.getAppName());
        retM.put("Version", this.getVersion());
        if (!StringUtils.isNullOrEmptyOrSpace(this.getVerMemo())) retM.put("Descn", this.getVerMemo());
        if (!StringUtils.isNullOrEmptyOrSpace(this.getBugMemo())) retM.put("BugPatch", this.getBugMemo());
        String _s=(new DecimalFormat("0.00")).format(((float)this.apkSize)/(2<<19));
        if (_s.endsWith(".00")) _s=_s.substring(0, _s.length()-3);
        if (_s.endsWith("0")&&_s.indexOf(".")!=-1) _s=_s.substring(0, _s.length()-1);
        retM.put("ApkSize", _s+" m");
        if (this.getPubTime()!=null) {
            _s=DateUtils.convert2LocalStr("yyyy年MM月dd日 HH时mm分", new Date(this.getPubTime().getTime()));
            retM.put("PubTime", _s);
        }
        return retM;
    }

    /**
     * 转换为为显示所用的Map
     * @return
     */
    public Map<String, String> toViewMap4View() {
        Map<String, String> retM=new HashMap<String, String>();
        if (StringUtils.isNullOrEmptyOrSpace(this.getAppName())) return null;
        if (StringUtils.isNullOrEmptyOrSpace(this.getVersion())) return null;

        retM.put("VerId", this.getId()+"");
        retM.put("AppName", this.getAppName());
        retM.put("Version", this.getVersion());
        if (!StringUtils.isNullOrEmptyOrSpace(this.getVerMemo())) retM.put("Descn", this.getVerMemo());
        if (!StringUtils.isNullOrEmptyOrSpace(this.getBugMemo())) retM.put("BugPatch", this.getBugMemo());
        retM.put("PubFlag", this.getPubFlag()+"");
        retM.put("StoreFile", this.getApkFile());
        retM.put("IsCur", ""+(this.getIsCurVer()==1));//是否是当前版本
        String _s=(new DecimalFormat("0.00")).format(((float)this.apkSize)/(2<<19));
        if (_s.endsWith(".00")) _s=_s.substring(0, _s.length()-3);
        if (_s.endsWith("0")&&_s.indexOf(".")!=-1) _s=_s.substring(0, _s.length()-1);
        retM.put("ApkSize", _s+" m");
        if (this.getPubTime()!=null) {
            _s=DateUtils.convert2LocalStr("yyyy年MM月dd日 HH时mm分", new Date(this.getPubTime().getTime()));
            retM.put("PubTime", _s);
        }
        if (this.getCTime()!=null) {
            _s=DateUtils.convert2LocalStr("yyyy年MM月dd日 HH时mm分", new Date(this.getCTime().getTime()));
            retM.put("CreateTime", _s);
        }
        if (this.getLmTime()!=null) {
            _s=DateUtils.convert2LocalStr("yyyy年MM月dd日 HH时mm分", new Date(this.getLmTime().getTime()));
            retM.put("LastModifyTime", _s);
        }
        if (!StringUtils.isNullOrEmptyOrSpace(this.getExtHisPatchInfo())) retM.put("HisPathInfo", this.getExtHisPatchInfo());
        return retM;
    }
}
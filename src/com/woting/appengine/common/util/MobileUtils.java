package com.woting.appengine.common.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.FileNameUtils;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.appengine.mobile.model.MobileKey;
import com.woting.appengine.mobile.model.MobileParam;
import com.woting.appengine.mobile.push.model.Message;
import com.woting.appengine.mobile.session.mem.SessionMemoryManage;
import com.woting.appengine.mobile.session.model.MobileSession;
import com.woting.passport.UGA.service.GroupService;
import com.woting.passport.login.persistence.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;

/**
 * 移动的公共处理
 * @author wh
 */
public abstract class MobileUtils {
    /**
     * 从输入流获得Json参数
     * @param req 请求内容
     * @return 返回参数
     */
    public static Map<String, Object> getDataFromRequest(ServletRequest req) {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader((ServletInputStream)req.getInputStream(), "UTF-8");
            br = new BufferedReader(isr);
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line=br.readLine())!=null) sb.append(line);
            line = sb.toString();
            if (line!=null&&line.length()>0) {
                return (Map<String, Object>)JsonUtils.jsonToObj(sb.toString(), Map.class);
            }
            System.out.println(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isr!=null) try {isr.close();} catch(Exception e) {}
            if (br!=null) try {br.close();} catch(Exception e) {}
        }
        return null;
    }

    /**
     * 从输入流获得Json参数
     * @param req 请求内容
     * @return 返回参数
     */
    public static Map<String, Object> getDataFromRequestParam(ServletRequest req) {
        Map<String, Object> retM = new HashMap<String, Object>();
        Enumeration<String> enu=req.getParameterNames();
        while(enu.hasMoreElements()) {
            String name=(String)enu.nextElement();
            retM.put(name, req.getParameter(name));
        }
        return retM;
    }

    /**
     * 按类型保存图片
     * @param req
     * @return
     * @throws IOException 
     * @throws UnsupportedEncodingException
     */
    public static Map<String, Object> saveTypePictrue(HttpServletRequest req, String ownerId) throws UnsupportedEncodingException, IOException {
        String appOSPath = ((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent();
        Map<String, Object> retM = new HashMap<String, Object>();
        String fType = req.getParameter("PType");
        String extName = req.getParameter("ExtName");
        String errMessage = "";
        if (StringUtils.isNullOrEmptyOrSpace(fType)) {
            errMessage+=","+"无法获取文件类型，不能保存文件";
        }
        if (StringUtils.isNullOrEmptyOrSpace(extName)) {
            errMessage+=","+"无法获取文件扩展名，不能保存文件";
        }
        if (StringUtils.isNullOrEmptyOrSpace(ownerId)) {
            errMessage+=","+"无法获取文所有者Id，不能保存文件";
        }
        if (!StringUtils.isNullOrEmpty(errMessage)) {
            retM.put("rType", "err");
            retM.put("Message", errMessage.substring(1));
            return retM;
        }
        if (fType.equals("Portrait")) {//保存头像，不包括数据库存储
            //获得文件名称
            String bigUri = "asset\\members\\"+ownerId+"\\Portrait\\bigImg"+(extName.startsWith(".")?extName:"."+extName);
            if (MobileUtils.savePicture(req,  FileNameUtils.concatPath(appOSPath, bigUri))) {
                retM.put("rType", "ok");
                retM.put("bigUri", bigUri);
                retM.put("miniUri",bigUri);
            } else {
                retM.put("rType", "err");
            }
            return retM;
        } else if (fType.equals("Others")) {
            retM.put("rType", "err");
            retM.put("Message", "未知的文件处理类型，无法处理");
            return retM;
        } else {
            retM.put("rType", "err");
            retM.put("Message", "未知的文件处理类型，无法处理");
            return retM;
        }
    }

    /**
     * 保存文件
     * @param isr 输入流
     * @param fileName 保存的文件名称
     * @return
     */
    private static boolean savePicture(HttpServletRequest req, String fileName) {
        DataInputStream in = null;
        DataOutputStream out = null;
        FileOutputStream fos = null;
        try {
            in = new DataInputStream((ServletInputStream)req.getInputStream());
            String filePath = FileNameUtils.getFilePath(fileName);
            File dir = new File(filePath);
            dir.mkdirs();
            fos = new FileOutputStream(fileName);
            out = new DataOutputStream(fos);
            byte[] buffer = new byte[4096];
            int count = 0;
            while ((count=in.read(buffer))>0) {
                out.write(buffer, 0, count);
            }
            //处理空文件？
            out.close();
            in.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos!=null) try {fos.close();} catch(Exception e) {}
            if (out!=null) try {out.close();} catch(Exception e) {}
            if (in!=null) try {in.close();} catch(Exception e) {}
        }
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            List<FileItem> items = upload.parseRequest(req);
            System.out.println(items);
            return false;
        } catch (FileUploadException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static MobileParam getMobileParam(Map<String, Object> m) {
        if (m==null||m.size()==0) return null;
        MobileParam mp = new MobileParam();
        mp.setImei((String)m.get("IMEI"));
        mp.setMType((String)m.get("MobileType"));
        mp.setGps((String)m.get("GPS"));
        mp.setUserId((String)m.get("UserId"));
        mp.setScreenSize((String)m.get("ScreenSize"));
        mp.setSessionId((String)m.get("SessionId"));
        mp.setMachine((String)m.get("Machine"));
        if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getMType())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getGps())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getScreenSize())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getMachine())) {
            return null;
        } else {
            return mp;
        }
    }

    /**
     * 得到最新的版本号
     */
    public static String getLastVersion() {
        //应该从某一个文件中读取最新的版本号
        return ((CacheEle<String>)SystemCache.getCache(WtAppEngineConstants.APP_VERSION)).getContent();
    }

    /**
     * 判断UserId是否合法
     */
    public static boolean isValidUserId(String userId) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return false;
        return userId.length()==12;
    }

    /**
     * 从一个Map获得移动的Key
     * @param m
     * @return
     */
    public static MobileKey getMobileKey(Map<String, Object> m) {
        if (m==null||m.size()==0) return null;
        String _temp=""+m.get("IMEI");
        if (StringUtils.isNullOrEmptyOrSpace(_temp)) return null;
        MobileKey ret = new MobileKey();
        ret.setMobileId(_temp);
        _temp=""+m.get("UserId");
        ret.setUserId(_temp);
        return ret;
    }

    /**
     * 从Message的消息发送地址，获得MobileKey
     * @param m 消息数据
     * @return 若合规，返回正常的MobileKey，否则返回空
     */
    //还有问题，没有做全部的解析，先这样
    public static MobileKey getMobileKey(Message m) {
        if (m==null||StringUtils.isNullOrEmptyOrSpace(m.getFromAddr())) return null;
        String _temp, _temp2;
        _temp=m.getFromAddr();
        if (_temp.charAt(0)!='{'||_temp.charAt(_temp.length()-1)!='}') return null;
        _temp=_temp.substring(1, _temp.length()-1);
        String[] ss=_temp.split("@@");
        if (ss.length==1) {//只有IMEI可能
            _temp=ss[0];
            if (_temp.charAt(0)=='('&&_temp.charAt(_temp.length()-1)==')') {
                _temp=_temp.substring(1, _temp.length()-1);
                ss=_temp.split("||");
                if (ss.length==1) return null;
                _temp=ss[0];
                MobileKey mk = new MobileKey();
                mk.setMobileId(_temp);
                mk.setUserId(_temp);
                return mk;
            } else return null;
        } else {
            _temp=ss[0]; _temp2=ss[1];
            MobileKey mk = new MobileKey();
            //获得IMEI
            if (_temp2.charAt(0)=='('&&_temp2.charAt(_temp2.length()-1)==')') {
                _temp2=_temp2.substring(1, _temp2.length()-1);
                ss=_temp2.split("\\u007C\\u007C");
                if (ss.length==1) return null;
                _temp2=ss[0];
                mk.setMobileId(_temp2);
            } else return null;
            //获得userId
            if (_temp.charAt(0)=='('&&_temp.charAt(_temp.length()-1)==')') {
                _temp=_temp.substring(1, _temp.length()-1);
                ss=_temp.split("\\u007C\\u007C");
                if (ss.length==1) {
                    mk.setUserId(mk.getMobileId());
                } else {
                    _temp=ss[0];
                    mk.setUserId(_temp);
                }
            }
            return mk;
        }
    }

    public static String getAddr(MobileKey mk) {
        String ret="";
        if (mk.isMobile()) {
            ret="{()@@("+mk.getMobileId()+"||M)}";
        } else {
            ret="{("+mk.getUserId()+"||wt)@@("+mk.getMobileId()+"||M)}";
        }
        return ret;
    }

    public static String getVersion() {
        String version="0.0.0.0.0000";
        Properties prop = new Properties();
        FileInputStream in = null;
        try {
            String appPfileName= ((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent()+"/WEB-INF/app.properties";
            in = new FileInputStream(appPfileName);
            prop.load(in);
            version = prop.getProperty("appVersion").trim()+","+prop.getProperty("publishTime").trim()+","+prop.getProperty("patchInfo").trim();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {in.close();} catch(Exception _e){} finally{in=null;}
        }
        return version;
    }

    /**
     * 为消息服务，处理客户端的连接
     * @param m 消息
     * @param type 类型：1=注册，第一次进入；2=仅绑定
     * @return 返回Map，情况如下：1=若参数整体无意义，则返回空；
     *   ReturnType="2001", Msg="未获得IMEI无法处理";
     */
    public static Map<String, Object> dealMobileLinked(Message m, int type) {
        //若参数整体无意义，则返回空
        if (m==null) return null;
        return MobileUtils._dealMobileLinked(MobileUtils.getMobileKey(m), type);
    }

    /**
     * 为Web服务，处理客户端的连接
     * @param m Web服务收到的客户端所传过来的数据的Map
     * @return 返回Map，情况如下：1=若参数整体无意义，则返回空
     *   ReturnType="2001", Msg="未获得IMEI无法处理";
     */
    public static Map<String, Object> dealMobileLinked(Map<String, Object> m, int type) {
        if (m==null||m.size()==0) return null;
        return MobileUtils._dealMobileLinked(MobileUtils.getMobileKey(m), type);
    }

    private static Map<String, Object> _dealMobileLinked(MobileKey mKey, int type) {
        Map<String,Object> map=new HashMap<String, Object>();
        if (mKey==null||StringUtils.isNullOrEmptyOrSpace(mKey.getMobileId())) {//若无IMEI
            map.put("ReturnType", "2001");
            map.put("Msg", "未获得IMEI无法处理");
        } else if (mKey.isMobile()) {//若仅仅是设备
            if (type==1) {
                //看之前是否在此设备上有人登录过
                ServletContext sc = (ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
                if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                    MobileUsedService muService = (MobileUsedService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("muService");
                    MobileUsedPo mu=muService.getUsedInfo(mKey.getMobileId());
                    
                } else {
                }
            } else {
                
            }
            //SessionMemoryManage.getInstance().addOneSession(ms);
        } else if (mKey.isUser()) {//是用户
            MobileSession ms=SessionMemoryManage.getInstance().getSession(mKey);
            if (ms==null) {
                ms=new MobileSession(mKey);
            }
        }
        return map;
    }
}
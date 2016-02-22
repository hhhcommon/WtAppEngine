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
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.login.persistence.pojo.MobileUsedPo;
import com.woting.passport.login.service.MobileUsedService;

/**
 * 移动的公共处理
 * @author wh
 */
public abstract class MobileUtils {
    /**
     * 从输入POST流获得参数
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
     * 从Web参数获得Json参数
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

    public static MobileParam getMobileParam(Map<String, Object> m) {
        if (m==null||m.size()==0) return null;

        MobileParam mp = new MobileParam();

        Object o=m.get("IMEI");
        String __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setImei(__tmp);
        o=m.get("PCDType");
        __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setPCDType(__tmp);
        o=m.get("MobileClass");
        __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setMClass(__tmp);
        o=m.get("GPS-longitude");
        __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setGpsLongitude(__tmp);
        o=m.get("GPS-Latitude");
        __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setGpsLatitude(__tmp);
        o=m.get("ScreenSize");
        __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setScreenSize(__tmp);
        o=m.get("SessionId");
        __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setSessionId(__tmp);
        o=m.get("UserId");
        __tmp=o==null?"":(String)o;
        if (!StringUtils.isNullOrEmptyOrSpace(__tmp)) mp.setUserId(__tmp);

        if (StringUtils.isNullOrEmptyOrSpace(mp.getImei())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getPCDType())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getMClass())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getGpsLongitude())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getGpsLatitude())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getScreenSize())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getSessionId())&&
            StringUtils.isNullOrEmptyOrSpace(mp.getUserId())) {
            return null;
        } else {
            return mp;
        }
    }

    /*
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

        Object o=m.get("IMEI");
        String __tmp=o==null?"":(String)o;
        if (StringUtils.isNullOrEmptyOrSpace(__tmp)) return null;

        MobileKey ret = new MobileKey();
        ret.setMobileId(__tmp);
        o=m.get("PCDType");
        __tmp=o==null?"1":(String)o;
        try {ret.setPCDType(Integer.parseInt(__tmp));} catch(Exception e) {}
        o=m.get("UserId");
        __tmp=o==null?"":(String)o;
        if ("".equals(__tmp)) {
            o=m.get("SessionId");
            __tmp=o==null?"":(String)o;
        }
        ret.setUserId(__tmp);
        return ret;
    }

    /**
     * 根据MobileKey生成地址
     * @param mk 
     * @return 地址字符串
     */
    public static String getAddr(MobileKey mk) {
        String ret="";
        if (mk.isMobile()) {
            ret="{()@@("+mk.getMobileId()+"::"+mk.getPCDType()+"||M)}";
        } else {
            ret="{("+mk.getUserId()+"||wt)@@("+mk.getMobileId()+"::"+mk.getPCDType()+"||M)}";
        }
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
        _temp=ss[0]; _temp2=ss[1];
        MobileKey mk = new MobileKey();
        //获得IMEI
        if (_temp2.charAt(0)=='('&&_temp2.charAt(_temp2.length()-1)==')') {
            _temp2=_temp2.substring(1, _temp2.length()-1);
            ss=_temp2.split("\\u007C\\u007C");
            if (ss.length==1) return null;
            _temp2=ss[0];
            String[] ss2=_temp2.split("::");
            mk.setMobileId(ss2[0]);
            mk.setPCDType(1);
            if (ss2.length==2) {
                try {mk.setPCDType(Integer.parseInt(ss2[1]));} catch(Exception e) {};
            }
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
        } else mk.setUserId(mk.getMobileId());
        return mk;
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
     * @param type 类型：1=注册，第一次进入；2=仅绑定
     * @return 返回Map，情况如下：1=若参数整体无意义，则返回空
     *   ReturnType="2001", Msg="未获得IMEI无法处理";
     */
    public static Map<String, Object> dealMobileLinked(Map<String, Object> m, int type) {
        if (m==null||m.size()==0) return null;
        return MobileUtils._dealMobileLinked(MobileUtils.getMobileKey(m), type);
    }

    private static Map<String, Object> _dealMobileLinked(MobileKey mKey, int type) {
        MobileKey _mKey=new MobileKey();
        _mKey.setMobileId(mKey.getMobileId());
        _mKey.setPCDType(mKey.getPCDType());
        _mKey.setUserId(mKey.getUserId());
        Map<String,Object> map=new HashMap<String, Object>();
        if (_mKey==null||StringUtils.isNullOrEmptyOrSpace(_mKey.getMobileId())) {//若无IMEI
            map.put("ReturnType", "2001");
            map.put("Msg", "未获得IMEI无法处理");
        } else {
            if (type==1) {
                if (_mKey.isMobile()) {//找到上次登录的情况
                    try {
                        ServletContext sc=(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
                        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                            MobileUsedService muService = (MobileUsedService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("mobileUsedService");
                            MobileUsedPo mu=muService.getUsedInfo(_mKey.getMobileId(), _mKey.getPCDType());
                            if (mu!=null&&mu.getStatus()==1) {//上次登录
                                _mKey.setUserId(mu.getUserId());//修改mKey
                            }
                        }
                    } catch(Exception e) {
                    }
                }
            }

            boolean msExist=true;//缓存是否存在
            MobileSession ms=SessionMemoryManage.getInstance().getSession(_mKey);
            if (ms==null) {
                msExist=false;
                ms=new MobileSession(_mKey);
            }
            ms.access();

            boolean needLogin=false;
            UserPo u=null;
            if (_mKey.isUser()) {
                try {
                    u=(UserPo)ms.getAttribute("user");
                } catch(Exception e) {}
                if (mKey.isMobile()&&type==1&&(u==null||!_mKey.getUserId().equals(u.getUserId()))) {//实现自动登录
                    try {
                        ServletContext sc=(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent();
                        if (WebApplicationContextUtils.getWebApplicationContext(sc)!=null) {
                            UserService userService = (UserService)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("userService");
                            u=userService.getUserById(_mKey.getUserId());
                            if (u!=null) ms.addAttribute("user", u);
                        }
                    } catch(Exception e) {}
                }
                String userIdInMem=u==null?"":u.getUserId();
                if (!_mKey.getUserId().equals(userIdInMem)) needLogin=true;//需要登录
            }

            //准备返回值
            boolean needInsertMs=false;
            if (mKey.isMobile()&&_mKey.isUser()) {
                if (u!=null) {
                    map.put("ReturnType", "1001");
                    map.put("Msg", "成功自动登录");
                    needInsertMs=true;
                } else {
                    map.put("ReturnType", "1002");
                    map.put("Msg", "不能找到相应的用户");
                    MobileSession _ms=SessionMemoryManage.getInstance().getSession(mKey);
                    if (_ms==null) {
                        _ms=new MobileSession(mKey);
                        _ms.access();
                        SessionMemoryManage.getInstance().addOneSession(_ms);
                    }
                }
            } else if (mKey.isMobile()&&_mKey.isMobile()) {
                map.put("ReturnType", "1002");
                map.put("Msg", "设备成功绑定");
                needInsertMs=true;
            } else {
                if (u!=null) {
                    map.put("ReturnType", "1001");
                    map.put("Msg", "成功获得Session");
                    needInsertMs=true;
                } else {
                    map.put("ReturnType", "2002");
                    map.put("Msg", "不能找到相应的用户");
                }
            }

            if (needLogin) {
                map.put("ReturnType", "2003");
                map.put("Msg", "请先登录");
            }
            if (!msExist&&needInsertMs) SessionMemoryManage.getInstance().addOneSession(ms);
            ms.access();

            map.put("MobileSession", ms);
        }
        return map;
    }

    //=============图片处理
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
    /*
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

    //==============版本号
    /**
     * 得到最新的版本号
     */
    public static String getLastVersion() {
        //应该从某一个文件中读取最新的版本号
        return ((CacheEle<String>)SystemCache.getCache(WtAppEngineConstants.APP_VERSION)).getContent();
    }
    /**
     * 从配置文件中获得版本号
     * @return
     */
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
}
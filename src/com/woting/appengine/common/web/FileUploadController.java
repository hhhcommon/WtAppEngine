package com.woting.appengine.common.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.web.AbstractFileUploadController;
import com.spiritdata.framework.util.FileNameUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.UGA.persistence.pojo.GroupPo;
import com.woting.passport.UGA.persistence.pojo.UserPo;
import com.woting.passport.UGA.service.GroupService;
import com.woting.passport.UGA.service.UserService;
import com.woting.passport.mobile.MobileParam;
import com.woting.passport.mobile.MobileUDKey;
import com.woting.passport.session.SessionService;

@Controller
public class FileUploadController extends AbstractFileUploadController {
    @Resource
    private UserService userService;
    @Resource
    private GroupService groupService;
    @Resource(name="redisSessionService")
    private SessionService sessionService;

    @Override
    public Map<String, Object> afterUploadOneFileOnSuccess(Map<String, Object> m, Map<String, Object> a, Map<String, Object> p) {
        String tempFileName=m.get("storeFilename")+"";
        Map<String,Object> retmap=new HashMap<String, Object>();
        Map<String,Object> datamap=new HashMap<String, Object>();
        //0-获取参数
        String userId="";
        if (p==null||p.size()==0) {
            datamap.put("ReturnType", "0000");
            datamap.put("Message", "无法获取需要的参数");
        } else {
            MobileUDKey mUdk=MobileParam.build(m).getUserDeviceKey();
            Map<String, Object> retM=sessionService.dealUDkeyEntry(mUdk, "fileUpload");
            if ((retM.get("ReturnType")+"").equals("2001")) {
                datamap.put("ReturnType", "0000");
                datamap.put("Message", "无法获取设备Id(IMEI)");
            } else if ((retM.get("ReturnType")+"").equals("2003")) {
                datamap.put("ReturnType", "200");
                datamap.put("Message", "需要登录");
            } else {
                datamap.putAll(mUdk.toHashMapAsBean());
            }
            if (datamap.get("ReturnType")==null&&StringUtils.isNullOrEmptyOrSpace(userId)) {
                datamap.put("ReturnType", "1002");
                datamap.put("Message", "无法获取用户Id，不能保存图片");
            }
        }
        if (datamap.get("ReturnType")==null){
            String fType=p.get("FType")+"";
            String extName=p.get("ExtName")+"";
            if (fType.equals("UserP")) {//========================================处理用户头像
                try {
                    String appPath=(SystemCache.getCache(FConstants.APPOSPATH)==null?"":((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent());
                    String bigImgFileName = "asset"+File.separatorChar+"members"+File.separatorChar+userId+File.separatorChar+"Portrait"+File.separatorChar+"bigImg"+SequenceUUID.getUUIDSubSegment(2)+(extName.startsWith(".")?extName:"."+extName);
                    FileUtils.copyFile(new File(tempFileName), new File(FileNameUtils.concatPath(appPath,bigImgFileName)));
                    //图片文件缩略存储
                    //文件保存到数据库中
                    UserPo u=(UserPo)userService.getUserById(userId);
                    u.setPortraitBig(bigImgFileName);
                    u.setPortraitMini(bigImgFileName);
                    userService.updateUser(u);
                    datamap.put("ReturnType", "1001");
                    datamap.put("PortraitBig", bigImgFileName);
                    datamap.put("PortraitMini", bigImgFileName);
                } catch (IOException e) {
                    datamap.put("ReturnType", "1003");
                    datamap.put("Message", "文件转存失败:"+e.getMessage());
                    e.printStackTrace();
                }
            } else if (fType.equals("GroupP")) {
                String groupId=p.get("GroupId")+"";
                GroupPo g=groupService.getGroupById(groupId);
                if (g==null) {
                    datamap.put("ReturnType", "1002");
                    datamap.put("Message", "根据Id无法获取用户组，不能保存图片");
                } else {
                    try {
                        String bigImgFileName=((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent();
                        bigImgFileName = "asset"+File.separatorChar+"group"+File.separatorChar+groupId+File.separatorChar+"Portrait"+File.separatorChar+"bigImg"+SequenceUUID.getUUIDSubSegment(2)+(extName.startsWith(".")?extName:"."+extName);
                        FileUtils.copyFile(new File(tempFileName), new File(FileNameUtils.concatPath(((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent(),bigImgFileName)));
                        //图片文件缩略存储
                        //文件保存到数据库中
                        g.setGroupImg(bigImgFileName);
                        groupService.updateGroup(g);
                        datamap.put("ReturnType", "1001");
                        datamap.put("GroupImg", bigImgFileName);
                    } catch (IOException e) {
                        datamap.put("ReturnType", "1003");
                        datamap.put("Message", "文件转存失败:"+e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        //删除缓存文件
        try {
            FileUtils.forceDelete(new File(tempFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        m.clear();
        m.putAll(datamap);
        retmap.put("success", "TRUE");
        retmap.put("onFaildBreak", "FALSE");
        return retmap;
    }
}
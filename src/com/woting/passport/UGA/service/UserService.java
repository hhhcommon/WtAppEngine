package com.woting.passport.UGA.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.spiritdata.framework.UGA.UgaUserService;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.service.DictService;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.thirdlogin.ThirdLoginUtils;
import com.woting.passport.thirdlogin.persis.po.ThirdUserPo;

public class UserService implements UgaUserService {
    @Resource(name="defaultDAO")
    private MybatisDAO<UserPo> userDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<ThirdUserPo> thirdUserDao;
    @Resource
    private DictService dictService;

    @PostConstruct
    public void initParam() {
        userDao.setNamespace("WT_USER");
        thirdUserDao.setNamespace("THIRDUSRE");
    }

    @Override
    @SuppressWarnings("unchecked")
    public UserPo getUserByLoginName(String loginName) {
        try {
            return userDao.getInfoObject("getUserByLoginName", loginName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据绑定手机号，获得用户信息
     * @param userNum 用户号码
     * @return 用户信息
     */
    public UserPo getUserByPhoneNum(String phoneNum) {
        try {
            return userDao.getInfoObject("getUserByPhoneNum", phoneNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UserPo getUserById(String userId) {
        try {
            return userDao.getInfoObject("getUserById", userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<UserPo> getUserByIds(List<String> userIds) {
        try {
            String whereStr="";
            if (userIds!=null&&userIds.size()>0) {
                for (String id: userIds) {
                    whereStr+=" or id='"+id+"'";
                }
            }
            Map<String, String> param=new HashMap<String, String>();
            if (!StringUtils.isNullOrEmptyOrSpace(whereStr)) param.put("whereByClause", whereStr.substring(4));
            param.put("orderByClause", "cTime desc");
            return userDao.queryForList("getListByWhere", param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建用户成功返回1，否则返回0
     */
    public int insertUser(UserPo user) {
        int i=0;
        try {
            if (StringUtils.isNullOrEmptyOrSpace(user.getUserId())) {
                user.setUserId(SequenceUUID.getUUIDSubSegment(4));
            }
            userDao.insert("insertUser", user);
            i=1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * 更新用户
     * @param userInfo 用户信息
     * @return Map信息:
     *   ReturnType    Message
     *   1001          OKFields  NOFields
     *   1002          更新错误
     */
    public Map<String, Object> updateUser(Map<String, Object> userInfo) {
        if (userInfo.get("UserId")==null) return null;

        @SuppressWarnings("unchecked")
        CacheEle<_CacheDictionary> cache=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT));
        _CacheDictionary cd=cache.getContent();

        Map<String, Object> retMap=new HashMap<String, Object>();
        try {
            UserPo up=this.getUserById(""+userInfo.get("UserId"));
            if (up==null) return null;
            Map<String, String> noMap=new HashMap<String, String>();
            String OKFields="";

            //1-昵称NickName
            if (userInfo.get("nickName")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("nickName")+"")) {
                OKFields+=",NickName";
            }
            //2-用户签名UserSign
            if (userInfo.get("userSign")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("userSign")+"")) {
                OKFields+=",UserSign";
            }
            int flag=0;
            //3-性别Sex
            if (userInfo.get("sex")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("sex")+"")) {
                DictRefRes drr=new DictRefRes();
                drr.setResId(up.getUserId());
                drr.setResTableName("plat_User");
                drr.setRefName("性别");
                DictModel sexDm=cd.getDictModelById("8");
                drr.setDm(sexDm);
                @SuppressWarnings("unchecked")
                TreeNode<DictDetail> sexNode=(TreeNode<DictDetail>)sexDm.dictTree.findNode(userInfo.get("sex")+"");
                if (sexNode==null) {
                    noMap.put("Sex", "用户性别编码错误");
                } else {
                    drr.setDd(sexNode);
                    flag=dictService.bindDictRef(drr, 1);
                    if (flag!=1) noMap.put("Sex", "用户性别保存错误");
                    else OKFields+=",Sex";
                }
            }
            //4-地区Region
            if (userInfo.get("region")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("region")+"")) {
                DictRefRes drr=new DictRefRes();
                drr.setResId(up.getUserId());
                drr.setResTableName("plat_User");
                drr.setRefName("地区");
                DictModel regionDm=cd.getDictModelById("2");
                drr.setDm(regionDm);
                @SuppressWarnings("unchecked")
                TreeNode<DictDetail> regionNode=(TreeNode<DictDetail>)regionDm.dictTree.findNode(userInfo.get("region")+"");
                if (regionNode==null) {
                    noMap.put("Region", "地区编码错误");
                } else {
                    drr.setDd(regionNode);
                    flag=dictService.bindDictRef(drr, 1);
                    if (flag!=1) noMap.put("Region", "地区保存错误");
                    else OKFields+=",Region";
                }
            }
            //5-生日Birthday
            if (userInfo.get("birthday")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("birthday")+"")) {
                OKFields+=",Birthday";
            }
            //6-星座StarSign
            if (userInfo.get("starSign")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("starSign")+"")) {
                OKFields+=",StarSign";
            }
            //7-星座StarSign
            if (userInfo.get("starSign")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("starSign")+"")) {
                OKFields+=",StarSign";
            }
            //8-邮件Email
            if (userInfo.get("email")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("email")+"")) {
                userInfo.put("mailAddress", userInfo.get("email"));
                userInfo.remove("email");
                OKFields+=",Email";
            }
            //9-主页Homepage
            if (userInfo.get("homePage")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("homePage")+"")) {
                OKFields+=",Homepage";
            }
            //10-用户号
            if (userInfo.get("userNum")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("userNum")+"")) {
                flag=this.decideUserNum(userInfo.get("userId")+"", userInfo.get("userNum")+"");
                if (flag==1) OKFields+=",UserNum";
                else {
                    if (flag==2) {
                        noMap.put("UserNum", "该用户号已存在用户号");
                    } else if (flag==3) {
                        noMap.put("UserNum", "用户号重复");
                    } else {
                        noMap.put("UserNum", "未知问题:"+userInfo.get("userNum"));
                    }
                    userInfo.remove("userNum");
                }
            }
            //11-用户密码
            if (userInfo.get("password")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("password")+"")) {
                OKFields+=",Password";
            }
            //12-手机号码
            if (userInfo.get("phoneNum")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("phoneNum")+"")) {
                userInfo.put("mainPhoneNum", userInfo.get("phoneNum"));
                userInfo.remove("phoneNum");
                OKFields+=",PhoneNum";
            }
            //13-用户大图标
            if (userInfo.get("portraitBig")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("portraitBig")+"")) {
                OKFields+=",PortraitBig";
            }
            //14-用户小
            if (userInfo.get("portraitMini")!=null&&!StringUtils.isNullOrEmptyOrSpace(userInfo.get("portraitMini")+"")) {
                OKFields+=",PortraitMini";
            }
            //更新
            userDao.update(userInfo);//用户信息

            retMap.put("ReturnType", "1001");
            retMap.put("OkFields", OKFields.substring(1));
            retMap.put("NoFields", noMap);
        } catch (Exception e) {
            e.printStackTrace();
            retMap.put("ReturnType", "T");
            retMap.put("Message", StringUtils.getAllMessage(e));
        }
        return retMap;
    }

    /**
     * 判断用户号是否合法
     * @param userId 用户信息
     * @param userNum 用户号
     * @return 1=成功;2=该用户号已存在用户号;3=用户号重复;-1参数错误
     */
    public int decideUserNum(String userId, String userNum) {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return -1;
        if (StringUtils.isNullOrEmptyOrSpace(userNum)) return -1;
        UserPo uPo=userDao.getInfoObject("getUserByNum", userNum);
        if (uPo!=null) return 3;
        uPo=userDao.getInfoObject("getUserById", userId);
        if (!StringUtils.isNullOrEmptyOrSpace(uPo.getUserNum())) return 2;
        return 0;
    }

    /**
     * 获得组成员，为创建用户组使用
     * @param Members 组成员id，用逗号隔开
     * @return 组成员类表
     */
    public List<UserPo> getMembers4BuildGroup(String members) {
        try {
            List<UserPo> ul=userDao.queryForList("getMembers", members);
            return ul;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据用户号码获得用户信息
     * @param userNum 用户号码
     * @return 用户信息
     */
    public UserPo getUserByNum(String userNum) {
        return userDao.getInfoObject("getUserByNum", userNum);
    }

    /**
     * 第三方登录
     * @param thirdType 第三方登录类型
     * @param tuserId 第三方中的用户唯一标识
     * @param tuserName 第三方中的用户名称
     * @param tuserImg 第三方用户的头像
     * @param tuserData 第三方用户的其他信息
     * @return Map 当成功则有如下内容：
     *   count——登录的次数
     *   userInfo——用户信息
     */
    public Map<String, Object> thirdLogin(String thirdType, String tuserId, String tuserName, String tuserImg, Map<String, Object> tuserData) {
        Map<String, Object> r=new HashMap<String, Object>();
        //查看是否已经存在了
        ThirdUserPo tuPo=thirdUserDao.getInfoObject("getInfoByThirdUserId", tuserId);
        UserPo uPo=null;

        if (tuPo==null) {
            //若没有存在：1-加入新的用户
            uPo=new UserPo();
            uPo.setUserId(SequenceUUID.getUUIDSubSegment(4));
            uPo.setLoginName(tuserName);
            uPo.setUserState(1);
            uPo.setUserType(1);
            uPo.setPortraitBig(tuserImg);
            uPo.setPortraitMini(tuserImg);
            uPo.setUserName(tuserName);
            uPo.setPassword("--"+uPo.getUserId());
            ThirdLoginUtils.fillUserInfo(uPo, thirdType, tuserData);
            //若没有存在：2-建立关联关系
            tuPo=new ThirdUserPo();
            tuPo.setId(SequenceUUID.getUUIDSubSegment(4));
            tuPo.setUserId(uPo.getUserId());
            tuPo.setThirdUserId(tuserId);
            tuPo.setThirdLoginType(thirdType);
            tuPo.setThirdLoginCount(1);
            tuPo.setThirdUserInfo(JsonUtils.objToJson(tuserData));
            //保存到数据库
            thirdUserDao.insert(tuPo);
            this.insertUser(uPo);
        } else {
            //若存在：加一次登录数
            Map<String, Object> paramM=new HashMap<String, Object>();
            paramM.put("id", tuPo.getId());
            tuPo.setThirdLoginCount(tuPo.getThirdLoginCount()+1);
            paramM.put("thirdLoginCount", tuPo.getThirdLoginCount());
            thirdUserDao.update(paramM);
            uPo=userDao.getInfoObject("getUserById", tuPo.getUserId());
        }
        if (uPo!=null) r.put("userInfo", uPo);
        if (tuPo!=null) r.put("count", tuPo.getThirdLoginCount());
       
        return r;
    }
}
package com.woting.passport.preference.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.persis.po.DictRefResPo;
import com.woting.cm.core.dict.service.DictService;

@Service
public class PreferenceService {
    @Resource(name="defaultDAO")
    private MybatisDAO<DictRefResPo> dictRefDao;
    @Resource
    DictService dictService;

    @PostConstruct
    public void initParam() {
        dictRefDao.setNamespace("A_DREFRES");
    }

    /**
     * 得到所有的偏好信息
     * @return 偏好树
     */
    public TreeNode<? extends TreeNodeBean> getPreference() throws CloneNotSupportedException {
        DictModel pModel=dictService.getDictModelById("6");
        TreeNode<? extends TreeNodeBean> ret=pModel.dictTree.clone();
        //删除掉偏好下面的所有子结点
        if (ret.getChildren()==null||ret.getChildren().isEmpty()) return null;
        for (TreeNode<? extends TreeNodeBean> tn: ret.getChildren()) {
            tn.setChildren(null);
        }
        //找到下级偏好
        List<DictRefRes> drrl=null;
        for (TreeNode<? extends TreeNodeBean> tn: ret.getChildren()) {
            drrl=null;
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("resId", tn.getId());
            param.put("refName", "偏好对应分类");
            param.put("resTableName", "plat_DictD::6");
            drrl=dictService.getDictRefs(param);
            if (drrl!=null&&!drrl.isEmpty()) {
                for (DictRefRes drr: drrl) {
                    if (drr.getDd()!=null) {
                        tn.addChild(drr.getDd());
                    }
                }
            }
        }
        return ret;
    }

    /**
     * 得到用户偏好信息
     * @param userId 用户Id
     * @return
     * @throws CloneNotSupportedException 
     */
    public TreeNode<? extends TreeNodeBean> getUserPreference(String userId) throws CloneNotSupportedException {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;
        
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("resId", userId);
        param.put("resTableName", "plat_User");
        param.put("refName", "偏好设置-喜欢");
        List<DictRefRes> drrl=dictService.getDictRefs(param);
        if (drrl!=null&&!drrl.isEmpty()) {
            DictDetail _t=new DictDetail();
            _t.setId("=1");
            _t.setMId("=1");
            _t.setNodeName("个人["+userId+"]偏好树");
            _t.setIsValidate(1);
            _t.setParentId(null);
            _t.setOrder(1);
            _t.setBCode("root");
            TreeNode<? extends TreeNodeBean> root=new TreeNode<DictDetail>(_t);

            for (DictRefRes drr: drrl) {
                if (drr.getDd()!=null) root.addChild(drr.getDd());
            }
            if (root.getChildren()!=null&&!root.getChildren().isEmpty()) {
                return root;
            }
        }
        return null;
    }

    /**
     * 偏好设置
     * @param objId 用户Id，或IMEI
     * @param prefStr 偏好字符串
     * @param flag =1是用户; =2是IMEI
     * @param isOnlyCata =1仅包括分类(这是默认值); =2包括分类和偏好类型
     * @return 保存成功返回1，失败返回0
     */
    @SuppressWarnings("unchecked")
    public int setPreference(String objId, String prefStr, int flag, int isOnlyCata) {
        if (StringUtils.isNullOrEmptyOrSpace(prefStr)||StringUtils.isNullOrEmptyOrSpace(objId)||(flag!=1&&flag!=2)) return 0;

        DictModel pModel=dictService.getDictModelById("6");
        DictModel cModel=dictService.getDictModelById("3");
        CacheEle<_CacheChannel> cacheC=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL));
        _CacheChannel cc=cacheC.getContent();
        List<String> prefIds=new ArrayList<String>();
        List<String> cataIds=new ArrayList<String>();

        String[] oneArray=prefStr.split(",");
        for (String s:oneArray) {
            String[] fields=s.trim().split("::");
            if (fields.length==2) {//分类和偏好类型都设置
                if (fields[0].trim().equals("6")) {
                    if (pModel.dictTree!=null&&pModel.dictTree.findNode(fields[1])!=null) {
                        prefIds.add(fields[1].trim());
                    }
                }
                if (fields[0].trim().equals("-1")) {
                    if ((TreeNode<TreeNodeBean>)cc.channelTree.findNode(fields[1])!=null) {
                        prefIds.add(fields[1].trim());
                    }
                }
                if (fields[0].trim().equals("3")) {
                    if (cModel.dictTree!=null&&cModel.dictTree.findNode(fields[1])!=null) {
                        cataIds.add(fields[1].trim());
                    }
                }
            }
        }
        Map<String, Object> param=new HashMap<String, Object>();
        String setUpdateWhere="";
        if (flag==1) {//是用户
            if (isOnlyCata==2) { //包括分类和偏好
                //取消已经选择的
                param.clear();
                param.put("resTableName", "plat_User");
                param.put("resId", objId);
                List<DictRefResPo> existList=dictRefDao.queryForList("queryPrefList", param);
                if (existList!=null&&!existList.isEmpty()) {
                    param.put("refName", "偏好设置-取消");
                    param.put("dictMid", "3");
                    dictRefDao.update("cancelPref", param);
                    param.put("dictMid", "6");
                    dictRefDao.update("cancelPref", param);
                    if (!prefIds.isEmpty()) {
                        for (int i=prefIds.size()-1; i>=0; i--) {
                            boolean needDel=false;
                            for (DictRefResPo drrPo: existList) {
                                if (drrPo.getDictDid().equals(prefIds.get(i))) {
                                    setUpdateWhere+=" or id='"+drrPo.getId()+"'";
                                    needDel=true;
                                    break;
                                }
                            }
                            if (needDel) prefIds.remove(i);
                        }
                    }
                    if (!cataIds.isEmpty()) {
                        for (int i=cataIds.size()-1; i>=0; i--) {
                            boolean needDel=false;
                            for (DictRefResPo drrPo: existList) {
                                if (drrPo.getDictDid().equals(cataIds.get(i))) {
                                    setUpdateWhere+=" or id='"+drrPo.getId()+"'";
                                    needDel=true;
                                    break;
                                }
                            }
                            if (needDel) cataIds.remove(i);
                        }
                    }
                }
                //更新现有的
                if (!StringUtils.isNullOrEmptyOrSpace(setUpdateWhere)) {
                    param.clear();
                    param.put("refName", "偏好设置-喜欢");
                    param.put("whereStr", setUpdateWhere.substring(4));
                    dictRefDao.update("changeToLike", param);
                }
                //插入新的
                if (!prefIds.isEmpty()||!cataIds.isEmpty()) {
                    param.clear();
                    param.put("resTableName", "plat_User");
                    param.put("resId", objId);
                    if (!prefIds.isEmpty()) {
                        for (String prefId: prefIds) {
                            param.put("id", SequenceUUID.getPureUUID());
                            param.put("refName", "偏好设置-喜欢");
                            param.put("dictMid", "6");
                            param.put("dictDid", prefId);
                            dictRefDao.insert(param);
                        }
                    }
                    if (!cataIds.isEmpty()) {
                        for (String cataId: cataIds) {
                            param.put("id", SequenceUUID.getPureUUID());
                            param.put("refName", "偏好设置-喜欢");
                            param.put("dictMid", "3");
                            param.put("dictDid", cataId);
                            dictRefDao.insert(param);
                        }
                    }
                }
            } else { //仅包括分类
                //取消已经选择的
                param.clear();
                param.put("resTableName", "plat_User");
                param.put("resId", objId);
                param.put("dictMid", "3");
                List<DictRefResPo> existList=dictRefDao.queryForList(param);
                if (existList!=null&&!existList.isEmpty()) {
                    param.put("refName", "偏好设置-取消");
                    dictRefDao.update("cancelPref", param);
                    if (!cataIds.isEmpty()) {
                        for (int i=cataIds.size()-1; i>=0; i--) {
                            boolean needDel=false;
                            for (DictRefResPo drrPo: existList) {
                                if (drrPo.getDictDid().equals(cataIds.get(i))) {
                                    setUpdateWhere+=" or id='"+drrPo.getId()+"'";
                                    needDel=true;
                                    break;
                                }
                            }
                            if (needDel) cataIds.remove(i);
                        }
                    }
                }
                //更新现有的
                if (!StringUtils.isNullOrEmptyOrSpace(setUpdateWhere)) {
                    param.clear();
                    param.put("refName", "偏好设置-喜欢");
                    param.put("whereStr", setUpdateWhere.substring(4));
                    dictRefDao.update("changeToLike", param);
                }
                //插入新的
                if (!cataIds.isEmpty()) {
                    for (String cataId: cataIds) {
                        param.put("id", SequenceUUID.getPureUUID());
                        param.put("refName", "偏好设置-喜欢");
                        param.put("dictMid", "3");
                        param.put("dictDid", cataId);
                        dictRefDao.insert(param);
                    }
                }
            }
            return 1;
        } else { //是IMEI
            //取消已经选择的
            param.clear();
            param.put("resTableName", "Device");
            param.put("resId", objId);
            param.put("dictMid", "6");
            List<DictRefResPo> existList=dictRefDao.queryForList(param);
            if (existList!=null&&!existList.isEmpty()) {
                param.put("refName", "偏好设置-取消");
                dictRefDao.update("cancelPref", param);
                if (!prefIds.isEmpty()) {
                    for (int i=prefIds.size()-1; i>=0; i--) {
                        boolean needDel=false;
                        for (DictRefResPo drrPo: existList) {
                            if (drrPo.getDictDid().equals(prefIds.get(i))) {
                                setUpdateWhere+=" or id='"+drrPo.getId()+"'";
                                needDel=true;
                                break;
                            }
                        }
                        if (needDel) prefIds.remove(i);
                    }
                }
            }
            //更新现有的
            if (!StringUtils.isNullOrEmptyOrSpace(setUpdateWhere)) {
                param.clear();
                param.put("refName", "偏好设置-喜欢");
                param.put("whereStr", setUpdateWhere.substring(4));
                dictRefDao.update("changeToLike", param);
            }
            //插入新的
            if (!prefIds.isEmpty()) {
                for (String prefId: prefIds) {
                    param.put("id", SequenceUUID.getPureUUID());
                    param.put("refName", "偏好设置-喜欢");
                    param.put("dictMid", "6");
                    param.put("dictDid", prefId);
                    dictRefDao.insert(param);
                }
            }
            return 1;
        }
    }
}
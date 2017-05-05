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
    public TreeNode<? extends TreeNodeBean> getUserPreference(String userId, int fromType) throws CloneNotSupportedException {
        if (StringUtils.isNullOrEmptyOrSpace(userId)) return null;
        
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("resId", userId);
        param.put("resTableName", fromType==1?"plat_User":"device");
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

        //过滤喜欢的内容，把当前设置需要的找出来
        CacheEle<_CacheChannel> cacheC=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL));
        _CacheChannel cc=cacheC.getContent();
        List<String> okPrefStrList=new ArrayList<String>();

        DictModel dModel=null;
        String[] oneArray=prefStr.split(",");
        for (String s:oneArray) {
            String[] fields=s.trim().split("::");
            if (fields.length==2) {
                if (fields[0].trim().equals("-1")) {
                    if ((TreeNode<TreeNodeBean>)cc.channelTree.findNode(fields[1])!=null) {
                        int i=0;
                        for (;i<okPrefStrList.size(); i++) if (okPrefStrList.get(i).equals(s)) break;
                        if (i==okPrefStrList.size()) okPrefStrList.add(s);
                    }
                } else {
                    dModel=dictService.getDictModelById(fields[0]);
                    if (dModel!=null&&dModel.dictTree!=null&&dModel.dictTree.findNode(fields[1])!=null) {
                        int i=0;
                        for (;i<okPrefStrList.size(); i++) if (okPrefStrList.get(i).equals(s)) break;
                        if (i==okPrefStrList.size()) okPrefStrList.add(s);
                    }
                }
            }
        }
        Map<String, Object> param=new HashMap<String, Object>();
        //删除所有的以前的喜欢
        param.put("resTableName", flag==1?"plat_User":"device");
        param.put("resId", objId);
        param.put("refName", "偏好设置-喜欢");
        dictRefDao.delete("delPreference", param);
        //插入新的喜欢
        for (String okPrefStr: okPrefStrList) {
            String[] fields=okPrefStr.trim().split("::");
            if (fields.length==2) {
                param.put("id", SequenceUUID.getPureUUID());
                param.put("dictMid", fields[0]);
                param.put("dictDid", fields[1]);
                dictRefDao.insert(param);
            }
        }
        return 1;
    }
}
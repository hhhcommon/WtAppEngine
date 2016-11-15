package com.woting.passport.preference.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.service.DictService;

@Service
public class PreferenceService {
    @Resource
    DictService dictService;
    /**
     * 得到所有的偏好信息
     * @return 偏好树
     */
    public TreeNode<? extends TreeNodeBean> getPreference() throws CloneNotSupportedException {
        DictModel pModel=dictService.getDictModelById("6");
        TreeNode<? extends TreeNodeBean> ret=pModel.dictTree.clone();
        //删除掉偏好下面的所有子结点
        for (TreeNode<? extends TreeNodeBean> tn: ret.getChildren()) {
            tn.setChildren(null);
        }
        //找到下级偏好
        List<DictRefRes> drrl=null;
        for (TreeNode<? extends TreeNodeBean> tn: ret.getChildren()) {
            drrl=null;
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("resId", tn.getId());
            param.put("resTableName", "plat_DictD::6");
            drrl=dictService.getDictRefs(param);
            if (drrl!=null&&!drrl.isEmpty()) {
                for (DictRefRes drr: drrl) {
                    tn.addChild(drr.getDd());
                }
            }
        }
        return ret;
    }

    /**
     * 得到用户偏好信息
     * @param userId 用户Id
     * @return
     */
    public TreeNode<? extends TreeNodeBean> getUserPreference(String userId) {
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
                root.addChild(drr.getDd());
            }
        }
        return null;
    }

    /**
     * 偏好设置
     * @param objId 用户Id，或IMEI
     * @param preferenceStr 偏好字符串
     * @param flag =1是用户; =2是IMEI
     * @return 保存成功返回1，失败返回0
     */
    public int setPreference(String objId, String preferenceStr, int flag) {
        if (flag==1) {//是IMEI的方式
            //先修改为不喜欢
            
            
        } else if (flag==2) {
            
        }
        return 0;
    }
}
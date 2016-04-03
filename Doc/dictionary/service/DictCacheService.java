package com.woting.dictionary.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.common.model.Owner;
import com.woting.dictionary.model.DictDetail;
import com.woting.dictionary.model.DictMaster;
import com.woting.dictionary.model.DictModel;
import com.woting.dictionary.model._CacheDictionary;
import com.woting.exceptionC.Wtcm1000CException;

@Service
public class DictCacheService {
    @Resource
    private DictService dictService;

    /**
     * 加载所有者字典信息到Session。构造过程会启动另一个线程处理
     * @param ownerId 所有者Id,UserId或SessionId
     * @param ownerType 所有者类型
     * @param session session
     */
    public _CacheDictionary loader() {
        Owner o=new Owner();
        o.setOwnerType(0);
        o.setOwnerId("0");
        _CacheDictionary _cd = new _CacheDictionary(o);

        try {
            //字典组列表
            _cd.dmList = dictService.getDictMListSys();

            //组装dictModelMap
            if (_cd.dmList!=null&&_cd.dmList.size()>0) {
                //Map主对应关系
                for (DictMaster dm: _cd.dmList) {
                    _cd.dictModelMap.put(dm.getId(), new DictModel(dm));
                }

                //构造单独的字典树
                List<DictDetail> templ = new ArrayList<DictDetail>();
                String tempDmId = "";
                _cd.ddList = dictService.getDictDListSys();//字典项列表，按照层级结果，按照排序的广度遍历树
                if (_cd.ddList!=null&&_cd.ddList.size()>0) {
                    for (DictDetail dd: _cd.ddList) {
                        if (tempDmId.equals(dd.getMId())) templ.add(dd);
                        else {
                            buildDictTree(templ, _cd);
                            templ.clear();
                            templ.add(dd);
                            tempDmId=dd.getMId();
                        }
                    }
                    //最后一个记录的后处理
                    buildDictTree(templ, _cd);
                }
            }
            return _cd;
        } catch(Exception e) {
            throw new Wtcm1000CException("加载Session中的字典信息", e);
        }
    }


    /**
     * 以ddList为数据源(同一字典组的所有字典项的列表)，构造所有者字典数据中的dictModelMap中的dictModel对象中的dictTree
     * @param ddList 同一字典组的所有字典项的列表
     * @param od 所有者字典数据
     */
    private void buildDictTree(List<DictDetail> ddList, _CacheDictionary cd) {
        if (ddList.size()>0) {//组成树
            DictModel dModel = cd.dictModelMap.get(ddList.get(0).getMId());
            if (dModel!=null) {
                DictDetail _t = new DictDetail();
                _t.setId(dModel.getId());
                _t.setMId(dModel.getId());
                _t.setNodeName(dModel.getDmName());
                _t.setIsValidate(1);
                _t.setParentId(null);
                _t.setOrder(1);
                _t.setBCode("root");
                TreeNode<? extends TreeNodeBean> root = new TreeNode<DictDetail>(_t);

                Map<String, Object> m = TreeUtils.convertFromList(ddList);
                root.setChildren((List<TreeNode<? extends TreeNodeBean>>)m.get("forest"));
                dModel.dictTree = (TreeNode<DictDetail>)root;
                //暂不处理错误记录
            }
        }
    }

    public Map<String, Object> getForest() {
        List<TreeNode<? extends TreeNodeBean>> forestList = new ArrayList<TreeNode<? extends TreeNodeBean>>();
        Map<String, Object> retM = new HashMap<String, Object>();
        return retM;
    }
}
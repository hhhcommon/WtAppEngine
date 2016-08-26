package com.woting.cm.core.dict.mem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.spiritdata.framework.core.model.tree.TreeNode;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictMaster;
import com.woting.cm.core.dict.model.DictModel;

/**
 * “字典数据”内容管理资源库的所有字典信息按结构进行存储。
 * 
 * @author wh
 */
public class _CacheDictionary {
    public Map<String, DictModel> dictModelMap; //所有者字典模型的数据集合
    public List<DictMaster> dmList = null; //所有者字典组列表
    public List<DictDetail> ddList = null; //所有者字典项列表

    /**
     * 构造所有者处理单元
     */
    public _CacheDictionary() {
        dictModelMap=new ConcurrentHashMap<String, DictModel>();
    }

    /**
     * 根据Id得到字典模式
     * @param dictMId 字典组Id
     * @return 元数据信息
     */
    public DictModel getDictModelById(String dictMid) {
        if (dictModelMap==null) return null;
        return dictModelMap.get(dictMid);
    }

    /**
     * 根据Id得到字典模式
     * @param dictMId 字典组Id
     * @return 元数据信息
     */
    public DictDetail getDictDetail(String dictMid, String dictDid) {
        if (dictModelMap==null) return null;
        DictModel dm=dictModelMap.get(dictMid);
        if (dm==null) return null;
        TreeNode<DictDetail> ddTn=(TreeNode<DictDetail>)dm.dictTree.findNode(dictDid);
        if (ddTn==null) return null;
        return ddTn.getTnEntity();
    }
}
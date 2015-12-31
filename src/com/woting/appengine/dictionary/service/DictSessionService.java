package com.woting.appengine.dictionary.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.web.SessionLoader;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.appengine.WtAppEngineConstants;
import com.woting.appengine.common.model.Owner;
import com.woting.appengine.common.util.SessionUtils;
import com.woting.appengine.dictionary.model.DictDetail;
import com.woting.appengine.dictionary.model.DictMaster;
import com.woting.appengine.dictionary.model.DictModel;
import com.woting.appengine.dictionary.model._OwnerDictionary;

/**
 * 所有者字典信息Session处理服务。
 * 字典信息存放在_OwnerDictionary中，这个类中存放了登录用户的字典数据信息。
 * 包括：<br/>
 * 1-Session和持久化存储同步的相关操作。<br/>
 * 2-通过线程方式进行加载，并放入缓存或Session。<br/>
 * @author wh
 */
public class DictSessionService implements SessionLoader {
    @Resource
    private DictService dictService;
    public DictService getDictService() {
        return dictService;
    }

    /**
     * 加载所有者字典信息到Session。构造过程会启动另一个线程处理
     * @param ownerId 所有者Id,UserId或SessionId
     * @param ownerType 所有者类型
     * @param session session
     */
    @Override
    public void loader(HttpSession session) throws Exception {
        _OwnerDictionary _od = new _OwnerDictionary(SessionUtils.getOwner(session));
        session.removeAttribute(WtAppEngineConstants.SESSION_OWNER_DICT);
        session.setAttribute(WtAppEngineConstants.SESSION_OWNER_DICT, _od);
        //启动加载线程
        Thread_LoadData lm = new Thread_LoadData(session, this);
        Thread t = new Thread(lm);
        t.start();
    }

    /**
     * 装载并检查数据，先检查是否已经装载，若没有装载则进行装载。并返回装载的内容
     * @param session 
     * @throws Excepiton InterruptedException
     */
    public _OwnerDictionary loadcheckData(HttpSession session) throws Exception {
        _OwnerDictionary _od = (_OwnerDictionary)session.getAttribute(WtAppEngineConstants.SESSION_OWNER_DICT);
        if (_od==null) {
            loader(session);
            _od = (_OwnerDictionary)session.getAttribute(WtAppEngineConstants.SESSION_OWNER_DICT);
            while (!_od.isLoadSuccess()) {
                Thread.sleep(100);
                _od = (_OwnerDictionary)session.getAttribute(WtAppEngineConstants.SESSION_OWNER_DICT);
            }
        }
        return _od;
    }
}

class Thread_LoadData implements Runnable {
    private HttpSession session;
    private DictSessionService caller;

    public Thread_LoadData(HttpSession session, DictSessionService caller) {
        this.caller = caller;
        this.session = session;
    }

    @Override
    public void run() {
        _OwnerDictionary _od = (_OwnerDictionary)session.getAttribute(WtAppEngineConstants.SESSION_OWNER_DICT);
        Owner owner = _od.getOwner();
        _od.dictModelMap = new ConcurrentHashMap<String, DictModel>();

        DictService dictService = caller.getDictService();
        try {
            //字典组列表
            _od.dmList = dictService.getDictMListByOwnerId(owner.getOwnerId());
            //字典项列表，按照层级结果，按照排序的广度遍历树
            if (_od.dmList!=null&&_od.dmList.size()>0) _od.ddList = dictService.getDictDListByOwnerId(owner.getOwnerId());

            //组装dictModelMap
            if (_od.dmList!=null&&_od.dmList.size()>0) {
                //Map主对应关系
                for (DictMaster dm: _od.dmList) {
                    if (dm.getOwner().equals(owner)) { //过滤掉不可用的数据
                        _od.dictModelMap.put(dm.getId(), new DictModel(dm));
                    }
                }
                //构造单独的字典树
                List<DictDetail> templ = new ArrayList<DictDetail>();
                String tempDmId = "";
                if (_od.ddList!=null&&_od.ddList.size()>0) {
                    for (DictDetail dd: _od.ddList) {
                        if (tempDmId.equals(dd.getMId())) templ.add(dd);
                        else {
                            buildDictTree(templ, _od);
                            templ.clear();
                            templ.add(dd);
                            tempDmId=dd.getMId();
                        }
                    }
                    //最后一个记录的后处理
                    buildDictTree(templ, _od);
                }
            }
        } finally {
            _od.setLoadSuccess();
        }
    }

    /**
     * 以ddList为数据源(同一字典组的所有字典项的列表)，构造所有者字典数据中的dictModelMap中的dictModel对象中的dictTree
     * @param ddList 同一字典组的所有字典项的列表
     * @param od 所有者字典数据
     */
    private void buildDictTree(List<DictDetail> ddList, _OwnerDictionary od) {
        if (ddList.size()>0) {//组成树
            DictModel dModel = od.dictModelMap.get(ddList.get(0).getMId());
            if (dModel!=null) {
                DictDetail _t = new DictDetail();
                _t.setId(dModel.getId());
                _t.setMId(dModel.getId());
                _t.setNodeName(dModel.getDmName());
                _t.setIsValidate(1);
                _t.setParentId(null);
                _t.setOrder(1);
                _t.setBCode("root");
                TreeNode<DictDetail> root = new TreeNode<DictDetail>(_t);

                Map<String, Object> m = TreeUtils.convertFromList(ddList);
                root.setChildren((List<TreeNode<DictDetail>>)m.get("forest"));
                dModel.dictTree = root;
                //暂不处理错误记录
            }
        }
    }
}
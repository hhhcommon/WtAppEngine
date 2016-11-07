package com.woting.cm.core.dict.service;

import java.sql.Timestamp;
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
import com.spiritdata.framework.core.model.Page;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictMaster;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.persis.po.DictDetailPo;
import com.woting.cm.core.dict.persis.po.DictMasterPo;
import com.woting.cm.core.dict.persis.po.DictRefResPo;
import com.woting.exceptionC.Wtcm0301CException;
import com.woting.exceptionC.Wtcm1000CException;

@Service
public class DictService {
    private static final Object updateLock=new Object();
    @Resource(name="defaultDAO")
    private MybatisDAO<DictMasterPo> dictMDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<DictDetailPo> dictDDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<DictRefResPo> dictRefDao;
    
    @PostConstruct
    public void initParam() {
        dictMDao.setNamespace("A_DMASTER");
        dictDDao.setNamespace("A_DDETAIL");
        dictRefDao.setNamespace("A_DREFRES");
    }

    //一、以下是字典相关的操作
    /**
     * 加载字典信息
     */
    public _CacheDictionary loadCache() {
        _CacheDictionary _cd=new _CacheDictionary();

        try {
            //字典组列表
            Map<String, Object> param=new HashMap<String, Object>();
//            param.put("ownerType", "100");
//            param.put("sortByClause", "id");
            List<DictMasterPo> dmpol=dictMDao.queryForList(param);
            if (dmpol==null||dmpol.size()==0) return null;
            List<DictMaster> dml=new ArrayList<DictMaster>();
            for (DictMasterPo dmp: dmpol) {
                DictMaster dm=new DictMaster();
                dm.buildFromPo(dmp);
                dml.add(dm);
            }
            _cd.dmList =(dml.size()==0?null:dml);

            //组装dictModelMap
            if (_cd.dmList!=null&&_cd.dmList.size()>0) {
                //Map主对应关系
                for (DictMaster dm: _cd.dmList) {
                    DictModel dModel=new DictModel(dm);
                    DictDetail _t=new DictDetail();
                    _t.setId(dModel.getId());
                    _t.setMId(dModel.getId());
                    _t.setNodeName(dModel.getDmName());
                    _t.setIsValidate(1);
                    _t.setParentId(null);
                    _t.setOrder(1);
                    _t.setBCode("root");
                    TreeNode<? extends TreeNodeBean> root=new TreeNode<DictDetail>(_t);
                    dModel.dictTree=(TreeNode<DictDetail>)root;
                    _cd.dictModelMap.put(dm.getId(), dModel);
                }

                //构造单独的字典树
                String tempDmId="";
                param.put("ownerId", "cm");
                param.put("ownerType", "100");
                int i=1;
                Page<DictDetailPo> ddPage=dictDDao.pageQuery("getListByOnwerDemo", param, i++, 10000);
                List<DictDetailPo> ddpol=new ArrayList<DictDetailPo>();
                boolean hasDD=!ddPage.getResult().isEmpty();
                //分页处理
                while (hasDD) {
                    ddpol.addAll(ddPage.getResult());
                    ddPage=dictDDao.pageQuery("getListByOnwerDemo", param, i++,10000);
                    hasDD=!ddPage.getResult().isEmpty();
                }
                if (ddpol==null||ddpol.size()==0) return _cd;
                List<DictDetail> ddl=new ArrayList<DictDetail>();
                for (DictDetailPo ddp: ddpol) {
                    DictDetail dd=new DictDetail();
                    dd.buildFromPo(ddp);
                    ddl.add(dd);
                }
                _cd.ddList=(ddl.size()==0?null:ddl);//字典项列表，按照层级结果，按照排序的广度遍历树

                List<DictDetail> templ=new ArrayList<DictDetail>();
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
            //处理空树
            return _cd;
        } catch(Exception e) {
            e.printStackTrace();
            throw new Wtcm1000CException("加载Session中的字典信息", e);
        }
    }
    /*
     * 以ddList为数据源(同一字典组的所有字典项的列表)，构造所有者字典数据中的dictModelMap中的dictModel对象中的dictTree
     * @param ddList 同一字典组的所有字典项的列表
     * @param od 所有者字典数据
     */
    private void buildDictTree(List<DictDetail> ddList, _CacheDictionary cd) {
        if (ddList.size()>0) {//组成树
            DictModel dModel=cd.dictModelMap.get(ddList.get(0).getMId());
            if (dModel!=null) {
                Map<String, Object> m=TreeUtils.convertFromList(ddList);
                dModel.dictTree.setChildren((List<TreeNode<? extends TreeNodeBean>>)m.get("forest"));
                //暂不处理错误记录
            }
        }
    }

    /**
     * 新增字典项，加入数据库
     * @param dd 字典项信息
     */
    public void addDictDetail(DictDetail dd) {
        try {
            DictDetailPo newDdp=dd.convert2Po();
            dictDDao.insert(newDdp);
        } catch(Exception e) {
            throw new Wtcm0301CException("新增字典项", e);
        }
    }

    /**
     * 绑定字典与资源的关系
     * @param drr 字典资源关系
     * @return 新增的字典项Id；1-成功；2-未找到字典组；3-未找到父亲结点；
     */
    public int bindDictRef(DictRefRes drr) {
        try {
            CacheEle<_CacheDictionary> cache=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT));
            _CacheDictionary cd=cache.getContent();
            DictRefResPo newDrrPo=drr.convert2Po();
            DictModel dm=cd.dictModelMap.get(newDrrPo.getDictMid());
            if (dm==null||dm.dictTree==null) return 2;
            TreeNode<DictDetail> dd=(TreeNode<DictDetail>)dm.dictTree.findNode(newDrrPo.getDictDid());
            if (dd==null) return 3;

            return dictRefDao.insert(newDrrPo);
        } catch(Exception e) {
        }
        return 0;
    }

    /**
     * 获得字典资源关系
     * @param resTableName 资源分类(资源表名称)
     * @param resId 资源Id
     * @return 资源关系列表
     */
    public List<DictRefRes> getDictRefs(String resTableName, String resId) {
        List<DictRefRes> ret=new ArrayList<DictRefRes>();
        try {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("resTableName", resTableName);
            param.put("resId", resId);
            List<DictRefResPo> l=dictRefDao.queryForList(param);
            if (l==null||l.isEmpty()) return null;

            CacheEle<_CacheDictionary> cache=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT));
            _CacheDictionary cd=cache.getContent();
            for (DictRefResPo drrPo: l) {
                DictRefRes drr=new DictRefRes();
                drr.buildFromPo(drrPo);
                DictModel dm=cd.dictModelMap.get(drrPo.getDictMid());
                drr.setDm(dm);
                TreeNode<DictDetail> dd=(TreeNode<DictDetail>)dm.dictTree.findNode(drrPo.getDictDid());
                drr.setDd(dd);
                ret.add(drr);
            }
        } catch(Exception e) {
        }
        return ret.isEmpty()?null:ret;
    }

    /**
     * 加入新字典项，同时处理字典缓存
     * @param dd 字典项信息
     * @return 新增的字典项Id；2-未找到字典组；3-未找到父亲结点；4-名称重复，同级重复；5-bCode重复，某分类下重复
     */
    public String insertDictDetail(DictDetail dd) {
        CacheEle<_CacheDictionary> cache=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT));
        _CacheDictionary cd=cache.getContent();
        DictModel dm=cd.dictModelMap.get(dd.getMId());
        if (dm==null||dm.dictTree==null) return "2";
        if (dm.getDdByBCode(dd.getBCode())!=null) return "5";

        TreeNode<DictDetail> parentNode=dm.dictTree;
        if (!StringUtils.isNullOrEmptyOrSpace(dd.getParentId())) {//父节点为空
            parentNode=(TreeNode<DictDetail>)parentNode.findNode(dd.getParentId());
            if (parentNode==null) return "3";
        }
        if (!parentNode.isLeaf()) {
            List<TreeNode<? extends TreeNodeBean>> cl=parentNode.getChildren();
            for (TreeNode<? extends TreeNodeBean> cn: cl) {
                if (cn.getNodeName().equals(dd.getNodeName())) return "4";
            }
        }

        //插入字典项
        dd.setId(SequenceUUID.getUUIDSubSegment(0));
        dd.setCTime(new Timestamp(System.currentTimeMillis()));
        try {
            //数据库
            DictDetailPo newDdp=dd.convert2Po();
            dictDDao.insert(newDdp);
            //缓存
            TreeNode<DictDetail> nd=new TreeNode<DictDetail>(dd);
            parentNode.addChild(nd);
            return dd.getId();
        } catch(Exception e) {
            return "err:"+e.getMessage();
        }
    }

    /**
     * 修改字典项，同时处理字典缓存
     * @param dd 字典项信息
     * @return 1-修改成功；2-未找到字典组；3-对应的结点未找到；4-名称重复，同级重复；5-bCode重复，某分类下重复；6-与原信息相同，不必修改
     */
    public int updateDictDetail(DictDetail dd) {
        CacheEle<_CacheDictionary> cache=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT));
        _CacheDictionary cd=cache.getContent();
        synchronized (updateLock) {
            DictModel dm=cd.dictModelMap.get(dd.getMId());

            if (dm==null||dm.dictTree==null) return 2;
            TreeNode<DictDetail> myInTree=(TreeNode<DictDetail>)dm.dictTree.findNode(dd.getId());
            if (myInTree==null) return 3;

            if ((dd.getNodeName()==null||(dd.getNodeName()!=null&&dd.getNodeName().equals(myInTree.getTnEntity().getNodeName())))
              &&myInTree.getTnEntity().getOrder()==dd.getOrder()
              &&(dd.getAliasName()==null||(dd.getAliasName()!=null&&dd.getAliasName().equals(myInTree.getTnEntity().getAliasName())))
              &&myInTree.getTnEntity().getIsValidate()==dd.getIsValidate()
              &&(dd.getBCode()==null||(dd.getBCode()!=null&&dd.getBCode().equals(myInTree.getTnEntity().getBCode())))
              &&(dd.getDesc()==null||(dd.getDesc()!=null&&dd.getDesc().equals(myInTree.getTnEntity().getDesc())))
              &&(dd.getParentId()==null||(dd.getParentId()!=null&&dd.getParentId().equals(myInTree.getTnEntity().getParentId())))
              ) {
              return 6;
            }

            if (dm.getDdByBCode(dd.getBCode())!=null&&!(myInTree.getTnEntity().getBCode().equals(dd.getBCode()))) return 5;

            boolean changeFather=false;
            TreeNode<DictDetail> parentNode=dm.dictTree;
            if (!StringUtils.isNullOrEmptyOrSpace(dd.getParentId())) {//父节点不为空
                //看看是否要更换所属父节点
                if (!dd.getParentId().equals("0")) parentNode=(TreeNode<DictDetail>)parentNode.findNode(dd.getParentId());
                if (parentNode!=null) changeFather=!myInTree.getTnEntity().getParentId().equals(dd.getParentId());
            } else {
                parentNode=(TreeNode<DictDetail>)myInTree.getParent();
            }
            if (!StringUtils.isNullOrEmptyOrSpace(dd.getNodeName())) {
                if (parentNode!=null&&!parentNode.isLeaf()) {
                    List<TreeNode<? extends TreeNodeBean>> cl=parentNode.getChildren();
                    for (TreeNode<? extends TreeNodeBean> cn: cl) {
                        if (cn.getNodeName().equals(dd.getNodeName())&&!cn.getId().equals(dd.getId())) return 4;
                    }
                }
            }
            //修改字典项
            try {
                //数据库
                dictDDao.update(dd.convert2Po());
                //缓存
                if (dd.getNodeName()!=null&&!dd.getNodeName().equals(myInTree.getTnEntity().getNodeName()))  myInTree.getTnEntity().setNodeName(dd.getNodeName());
                if (myInTree.getTnEntity().getOrder()!=dd.getOrder()) myInTree.getTnEntity().setOrder(dd.getOrder());
                if (dd.getAliasName()!=null&&!dd.getAliasName().equals(myInTree.getTnEntity().getAliasName())) myInTree.getTnEntity().setAliasName(dd.getAliasName());
                if (myInTree.getTnEntity().getIsValidate()!=dd.getIsValidate()) myInTree.getTnEntity().setIsValidate(dd.getIsValidate());
                if (dd.getBCode()!=null&&!dd.getBCode().equals(myInTree.getTnEntity().getBCode())) myInTree.getTnEntity().setBCode(dd.getBCode());
                if (dd.getDesc()!=null&&!dd.getDesc().equals(myInTree.getTnEntity().getDesc())) myInTree.getTnEntity().setDesc(dd.getDesc());
                if (changeFather) {
                    TreeNode<? extends TreeNodeBean> delNode=myInTree.getParent().removeChild(myInTree.getId());
                    parentNode.addChild(delNode);
                }
                return 1;
            } catch(Exception e) {
                throw new Wtcm0301CException("新增字典项", e);
            }
        }
    }

    /**
     * 删除字典项，同时处理字典缓存
     * @param dd 字典项信息
     * @param force 是否强制删除
     * @return "1"成功删除,"2"未找到相应的结点,"3::因为什么什么关联信息的存在而不能删除"
     */
    public String delDictDetail(DictDetail dd, boolean force) {
        CacheEle<_CacheDictionary> cache=((CacheEle<_CacheDictionary>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT));
        _CacheDictionary cd=cache.getContent();
        synchronized (updateLock) {
            DictModel dm=cd.dictModelMap.get(dd.getMId());
            if (dm==null||dm.dictTree==null) return "2";
            TreeNode<DictDetail> myInTree=(TreeNode<DictDetail>)dm.dictTree.findNode(dd.getId());
            if (myInTree==null) return "2";

            List<TreeNodeBean> ddl=myInTree.getAllBeansList();
            //检查是否有相关信息，注意是递归查找
            String inStr="";
            String inStr2="";
            for (TreeNodeBean _dd:ddl) {
                inStr+=" or id='"+_dd.getId()+"'";
                inStr2+=" or (dictMid='"+dd.getMId()+"' and dictDid='"+_dd.getId()+"')";
            }
            inStr=inStr.substring(4);
            inStr2=inStr2.substring(4);
            int count=dictRefDao.getCount("existRefDict", inStr2);
            if (count>0&&!force) return "3::由于有关联信息存在，不能删除";
            else {
                //删除关联
                dictRefDao.execute("delByDicts", inStr2);
                //删除本表
                dictDDao.delete("delByIds", inStr);
                //处理缓存
                myInTree.getParent().removeChild(myInTree.getId());
                return "1";
            }
        }
    }
}
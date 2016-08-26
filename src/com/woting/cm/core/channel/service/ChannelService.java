package com.woting.cm.core.channel.service;

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
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.WtAppEngineConstants;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.channel.model.Channel;
import com.woting.cm.core.channel.model.ChannelAsset;
import com.woting.cm.core.channel.persis.po.ChannelAssetPo;
import com.woting.cm.core.channel.persis.po.ChannelPo;
import com.woting.cm.core.common.model.Owner;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.exceptionC.Wtcm0201CException;
import com.woting.exceptionC.Wtcm1000CException;

@Service
public class ChannelService {
    @Resource(name="defaultDAO")
    private MybatisDAO<ChannelPo> channelDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<ChannelAssetPo> channelAssetDao;

    private _CacheChannel _cc=null;

    @PostConstruct 
    public void initParam() {
        channelDao.setNamespace("A_CHANNEL");
        channelAssetDao.setNamespace("A_CHANNELASSET");
        _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
    }

    /**
     * 加载栏目结构信息
     */
    public _CacheChannel loadCache() {
        _CacheChannel _cc=null;
        //初始化_cc
        _cc=new _CacheChannel();
        Channel _c=new Channel();
        _c.setId("0");
        Owner o=new Owner();
        o.setOwnerType(100);
        o.setOwnerId("cm");
        _c.setOwner(o);
        _c.setChannelName("栏目根");
        _c.setIsValidate(1);
        _c.setParentId(null);
        _c.setOrder(1);
        TreeNode<Channel> root=new TreeNode<Channel>(_c);
        _cc.channelTree=(TreeNode<Channel>)root;
        _cc.channelTreeMap=new HashMap<String, TreeNode<Channel>>();

        try {
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("ownerType", "100");
            param.put("sortByClause", "sort");
            List<ChannelPo> cpol=channelDao.queryForList(param);
            if (cpol!=null&&!cpol.isEmpty()) {
                List<Channel> cl=new ArrayList<Channel>();
                for (ChannelPo cpo: cpol) {
                    Channel c=new Channel();
                    c.buildFromPo(cpo);
                    cl.add(c);
                }
                Map<String, Object> m=TreeUtils.convertFromList(cl);
                //构造树
                _cc.channelTree.setChildren((List<TreeNode<? extends TreeNodeBean>>)m.get("forest"));
                //构造对应表
                for (ChannelPo cpo: cpol) {
                    TreeNode<Channel> findNode=(TreeNode<Channel>)_cc.channelTree.findNode(cpo.getId());
                    if (findNode!=null) {
                        _cc.channelTreeMap.put(cpo.getId(), findNode);
                    }
                }
            }
            return _cc;
        } catch(Exception e) {
            e.printStackTrace();
            throw new Wtcm1000CException("加载内存中的栏目结构信息失败", e);
        }
    }

    public void add(Channel c) {
        channelDao.insert(c.convert2Po());
    }

    public void publish(ChannelAsset ca) {
        channelAssetDao.insert(ca.convert2Po());
    }

    /**
     * 判断一个内容资源是否已经发布了
     * @param assetType 资源类型
     * @param assetId 资源Id
     * @return true已经发布了，false未发布
     */
    public boolean isPub(String assetType, String assetId) {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("assetType", assetType);
        param.put("assetId", assetId);
        param.put("flowFlag", "2");
        if (assetType.equals("wt_MediaAsset")) {
            return (channelAssetDao.getCount("belongSeqPubCount", param)>0);
        } else return (channelAssetDao.getCount(param)>0);
    }

    /**
     * 获得某内容列表中的内容，所对应的发布栏目信息（这里是正式发布的信息）
     * @param assetList 内容列表，列表中是Map，Map中包括两个字段 assetType,assetId
     * @return 该资源是否发布了，返回值包括三个字段assetType,assetId,isPub，其中isPub=1是已发布，否则是未发布
     */
    public List<Map<String, Object>> getPubChannelList(List<Map<String, Object>> assetList) {
        if (assetList==null||assetList.isEmpty()) return null;
        //拼Sql
        String sql="";
        for (Map<String, Object> asset: assetList) {
            sql+="or (assetType='"+asset.get("resTableName")+"' and assetId='"+asset.get("resId")+"')";
        }
        if (sql.length()>0) sql=sql.substring(3);
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("whereSql", sql);
        List<ChannelAssetPo> pubChannels=channelAssetDao.queryForListAutoTranform("pubChannels", param);
        if (pubChannels==null||pubChannels.isEmpty()) return null;

        List<Map<String, Object>> ret=new ArrayList<Map<String, Object>>();
        if (_cc==null) _cc=(SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)==null?null:((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL)).getContent());
        for (ChannelAssetPo caPo: pubChannels) {
            Map<String, Object> one=caPo.toHashMap();
            if (_cc!=null) {
                TreeNode<Channel> _c=(TreeNode<Channel>)_cc.channelTree.findNode(caPo.getChannelId());
                if (_c!=null) one.put("channelName", _c.getNodeName());
            }
            ret.add(one);
        }
        return ret;
    }

    /**
     * 加入新栏目，同时处理栏目缓存
     * @param c 字典项信息
     * @return 新增的栏目Id；2-未找到父亲结点；3-名称重复，同级重复;
     */
    public String insertChannel(Channel c) {
        CacheEle<_CacheChannel> cache=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL));
        _CacheChannel cc=cache.getContent();
        if (cc==null||cc.channelTree==null) return "2";

        TreeNode<Channel> parentNode=cc.channelTree;
        if (!StringUtils.isNullOrEmptyOrSpace(c.getParentId())) {//父节点为空
            parentNode=(TreeNode<Channel>)parentNode.findNode(c.getParentId());
            if (parentNode==null) return "2";
        }
        if (!parentNode.isLeaf()) {
            List<TreeNode<? extends TreeNodeBean>> cl=parentNode.getChildren();
            for (TreeNode<? extends TreeNodeBean> cn: cl) {
                if (cn.getNodeName().equals(c.getNodeName())) return "3";
            }
        }

        //插入字典项
        c.setId(SequenceUUID.getUUIDSubSegment(0));
        c.setCTime(new Timestamp(System.currentTimeMillis()));
        try {
            //数据库
            channelDao.insert(c.convert2Po());
            //缓存
            TreeNode<Channel> nd=new TreeNode<Channel>(c);
            parentNode.addChild(nd);
            return c.getId();
        } catch(Exception e) {
            return "err:"+e.getMessage();
        }
    }

    /**
     * 修改栏目信息，同时处理栏目缓存
     * @param c
     * @return 1-修改成功；2-对应的结点未找到；3-名称重复，同级重复；4-与原信息相同，不必修改
     */
    public int updateChannel(Channel c) {
        CacheEle<_CacheChannel> cache=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_CHANNEL));
        _CacheChannel cc=cache.getContent();
        if (cc==null||cc.channelTree==null) return 2;

        TreeNode<Channel> myInTree=(TreeNode<Channel>)cc.channelTree.findNode(c.getId());
        if (myInTree==null) return 2;

        if ((c.getNodeName()==null||(c.getNodeName()!=null&&c.getNodeName().equals(myInTree.getTnEntity().getNodeName())))
          &&myInTree.getTnEntity().getOrder()==c.getOrder()
          &&(c.getContentType()==null||(c.getContentType()!=null&&c.getContentType().equals(myInTree.getTnEntity().getContentType())))
          &&myInTree.getTnEntity().getIsValidate()==c.getIsValidate()
          &&(c.getOwner()==null||(c.getOwner()!=null&&c.getOwner().equals(myInTree.getTnEntity().getOwner())))
          &&(c.getDescn()==null||(c.getDescn()!=null&&c.getDescn().equals(myInTree.getTnEntity().getDescn())))
          &&(c.getParentId()==null||(c.getParentId()!=null&&c.getParentId().equals(myInTree.getTnEntity().getParentId())))
          ) {
          return 4;
        }

        boolean changeFather=false;
        TreeNode<Channel> parentNode=cc.channelTree;
        if (!StringUtils.isNullOrEmptyOrSpace(c.getParentId())) {//父节点为空
            //看看是否要更换所属父节点
            changeFather=!myInTree.getTnEntity().getParentId().equals(c.getParentId());
            parentNode=(TreeNode<Channel>)parentNode.findNode(c.getParentId());
        } else {
            parentNode=(TreeNode<Channel>)myInTree.getParent();
        }
        String compareName=StringUtils.isNullOrEmptyOrSpace(c.getNodeName())?myInTree.getNodeName():c.getNodeName();
        if (parentNode!=null&&!parentNode.isLeaf()) {
            List<TreeNode<? extends TreeNodeBean>> cl=parentNode.getChildren();
            for (TreeNode<? extends TreeNodeBean> cn: cl) {
                if (cn.getNodeName().equals(compareName)&&!cn.getId().equals(c.getId())) return 3;
            }
        }

        //修改字典项
        try {
            //数据库
            channelDao.update(c.convert2Po());
            //缓存
            if (changeFather) {
                if (c.getNodeName()==null) c.setNodeName(myInTree.getTnEntity().getNodeName());
                if (c.getOwner()==null) c.setOwner(myInTree.getTnEntity().getOwner());
                if (c.getIsValidate()==0) c.setIsValidate(myInTree.getTnEntity().getIsValidate());
                if (c.getOrder()==0) c.setOrder(myInTree.getTnEntity().getOrder());
                if (c.getContentType()==null) c.setContentType(myInTree.getTnEntity().getContentType());
                if (c.getDescn()==null) c.setDescn(myInTree.getTnEntity().getDescn());
                TreeNode<Channel> nd=new TreeNode<Channel>(c);

                myInTree.getParent().removeChild(myInTree.getId());
                parentNode.addChild(nd);
            } else {
                if (c.getNodeName()!=null&&!c.getNodeName().equals(myInTree.getTnEntity().getNodeName()))  myInTree.getTnEntity().setNodeName(c.getNodeName());
                if (c.getOwner()!=null&&!c.getOwner().equals(myInTree.getTnEntity().getOwner())) myInTree.getTnEntity().setOwner(c.getOwner());
                if (myInTree.getTnEntity().getIsValidate()!=c.getIsValidate()) myInTree.getTnEntity().setIsValidate(c.getIsValidate());
                if (myInTree.getTnEntity().getOrder()!=c.getOrder()) myInTree.getTnEntity().setOrder(c.getOrder());
                if (c.getContentType()!=null&&!c.getContentType().equals(myInTree.getTnEntity().getContentType())) myInTree.getTnEntity().setContentType(c.getContentType());
                if (c.getDescn()!=null&&!c.getDescn().equals(myInTree.getTnEntity().getDescn())) myInTree.getTnEntity().setDescn(c.getDescn());
            }
            return 1;
        } catch(Exception e) {
            throw new Wtcm0201CException("修改栏目", e);
        }
    }

    /**
     * 删除栏目，同时处理字典缓存
     * @param dd 字典项信息
     * @param force 是否强制删除
     * @return "1"成功删除,"2"未找到相应的结点,"3::因为什么什么关联信息的存在而不能删除"
     */
    public String delChannel(Channel c, boolean force) {
        CacheEle<_CacheChannel> cache=((CacheEle<_CacheChannel>)SystemCache.getCache(WtAppEngineConstants.CACHE_DICT));
        _CacheChannel cc=cache.getContent();
        if (cc==null||cc.channelTree==null) return "2";
        TreeNode<Channel> myInTree=(TreeNode<Channel>)cc.channelTree.findNode(c.getId());
        if (myInTree==null) return "2";

        List<TreeNodeBean> cl=myInTree.getAllBeansList();
        //检查是否有相关信息，注意是递归查找
        String inStr="";
        String inStr2="";
        for (TreeNodeBean _c:cl) {
            inStr+=" or id='"+_c.getId()+"'";
            inStr2+=" or channelId='"+_c.getId()+"')";
        }
        inStr=inStr.substring(4);
        inStr2=inStr2.substring(4);
        int count=channelAssetDao.getCount("existRefChannel", inStr2);
        if (count>0&&!force) return "3::由于有关联信息存在，不能删除";
        else {
            //删除关联
            channelAssetDao.execute("delByChannels", inStr2);
            //删除本表
            channelDao.delete("delByIds", inStr);
            //处理缓存
            myInTree.getParent().removeChild(myInTree.getId());
            return "1";
        }
    }
}
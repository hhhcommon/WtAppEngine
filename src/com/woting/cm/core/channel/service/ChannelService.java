package com.woting.cm.core.channel.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.cm.core.channel.mem._CacheChannel;
import com.woting.cm.core.channel.model.Channel;
import com.woting.cm.core.channel.model.ChannelAsset;
import com.woting.cm.core.channel.persis.po.ChannelAssetPo;
import com.woting.cm.core.channel.persis.po.ChannelPo;
import com.woting.cm.core.common.model.Owner;
import com.woting.exceptionC.Wtcm1000CException;

@Service
public class ChannelService {
    @Resource(name="defaultDAO")
    private MybatisDAO<ChannelPo> channelDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<ChannelAssetPo> channelAssetDao;

    @PostConstruct
    public void initParam() {
        channelDao.setNamespace("A_CHANNEL");
        channelAssetDao.setNamespace("A_CHANNELASSET");
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
            throw new Wtcm1000CException("加载Session中的栏目结构信息失败", e);
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
        return (channelDao.getCount(param)>0);
    }

    /**
     * 判断一个列表中的内容是否被发布了
     * @param assetList 被判断的资源列表，列表中是Map，Map中包括两个字段 assetType,assetId
     * @return 该资源是否发布了，返回值包括三个字段assetType,assetId,isPub，其中isPub=1是已发布，否则是未发布
     */
    public List<Map<String, Object>> isPubList(List<Map<String, Object>> assetList) {
        if (assetList==null||assetList.isEmpty()) return null;
        //拼Sql
        String sql="";
        for (Map<String, Object> asset: assetList) {
            sql+="or (assetType='"+asset.get("assetType")+"' and assetId='"+asset.get("assetId")+"')";
        }
        sql=sql.substring(3);
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("whereSql", sql);
        return channelDao.queryForListAutoTranform("isPubAssets", param);
    }
}
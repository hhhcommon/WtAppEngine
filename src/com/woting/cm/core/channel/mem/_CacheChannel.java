package com.woting.cm.core.channel.mem;

import java.util.Map;

import com.spiritdata.framework.core.model.tree.TreeNode;
import com.woting.cm.core.channel.model.Channel;

public class _CacheChannel {
    public Map<String, TreeNode<Channel>> channelTreeMap; //所有者栏目结构的对应表
    public TreeNode<Channel> channelTree;
}
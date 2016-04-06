package com.woting.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;

public abstract class TreeUtils {

    public static TreeNode<? extends TreeNodeBean> getLevelTree(TreeNode<? extends TreeNodeBean> t, int level) {
        TreeNode<? extends TreeNodeBean> ret=null;
        try {
            ret=t.clone();
            if (level>0) {
                if (!ret.isLeaf()) {
                    for (TreeNode<? extends TreeNodeBean> tn: ret.getChildren()) {
                        TreeUtils.cutLevel(tn, level-1);
                    }
                }
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static void cutLevel(TreeNode<? extends TreeNodeBean> t, int level) {
        if (!t.isLeaf()) {
            if (level==0) {
                (t.getChildren()).clear();
            } else {
                for (TreeNode<? extends TreeNodeBean> tn: t.getChildren()) {
                    TreeUtils.cutLevel(tn, level-1);
                }
            }
        }
    }

    public static List<TreeNode<? extends TreeNodeBean>> getDeepList(TreeNode<? extends TreeNodeBean> t) {
        if (t==null) return null;
        List<TreeNode<? extends TreeNodeBean>> ret=new ArrayList<TreeNode<? extends TreeNodeBean>>();
        if (!t.isLeaf()) {
            for (TreeNode<? extends TreeNodeBean> _t: t.getChildren()) {
                ret.add(_t);
                List<TreeNode<? extends TreeNodeBean>> _r=getDeepList(_t);
                if (_r!=null) ret.addAll(_r);
            }
            return ret;
        } else {
            return null;
        }
    }
}
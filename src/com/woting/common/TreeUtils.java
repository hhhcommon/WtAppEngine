package com.woting.common;

import java.util.ArrayList;
import java.util.Collection;
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
                        TreeUtils.cutLevel(tn, level--);
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
                t.setChildren(null);
            } else {
                for (TreeNode<? extends TreeNodeBean> tn: t.getChildren()) {
                    TreeUtils.cutLevel(tn, level--);
                }
            }
        }
    }
}
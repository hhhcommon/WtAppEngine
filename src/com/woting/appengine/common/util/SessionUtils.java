package com.woting.appengine.common.util;

import javax.servlet.http.HttpSession;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.UGA.UgaUser;
import com.woting.appengine.common.model.Owner;

/**
 * Session处理的通用方法，有：<br/>
 * 1-获得ownerId
 * @author wh
 */
public abstract class SessionUtils {

    /**
     * 从Session中获得所有者对象
     * @param session
     * @return 所有者对象，若Session为空，返回null
     */
    public static Owner getOwner(HttpSession session) {
        if (session==null) return null;
        Owner ret = new Owner();
        ret.setOwnerId(session.getId());
        ret.setOwnerType(2);
        UgaUser user = (UgaUser)session.getAttribute(FConstants.SESSION_USER);
        if (user!=null) {
            ret.setOwnerId(user.getUserId());
            ret.setOwnerType(1);
        }
        return ret;
    }
}
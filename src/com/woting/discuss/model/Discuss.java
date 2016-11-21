package com.woting.discuss.model;

import java.util.HashMap;
import java.util.Map;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.cm.core.media.MediaType;
import com.woting.discuss.persis.po.DiscussPo;

/**
 * 反馈意见信息<br/>
 * 包括意见的反馈列表
 * @author wh
 */
public class Discuss extends DiscussPo implements ModelSwapPo {
    private static final long serialVersionUID = 1020093563227522687L;


    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof DiscussPo)) throw new Plat0006CException("Po对象不是AppOpinionPo的实例，无法从此对象构建字典组对象！");
        DiscussPo _po = (DiscussPo)po;
        this.setId(_po.getId());
        this.setUserId(_po.getUserId());
        this.setResTableName(_po.getResTableName());
        this.setResId(_po.getResId());
        this.setDiscuss(_po.getDiscuss());
        this.setCTime(_po.getCTime());
    }
    @Override
    public Object convert2Po() {
        DiscussPo ret = new DiscussPo();
        if (StringUtils.isNullOrEmptyOrSpace(this.getId())) ret.setId(SequenceUUID.getUUIDSubSegment(4));
        else ret.setId(this.getId());
        ret.setUserId(this.getUserId());
        ret.setResTableName(this.getResTableName());
        ret.setResId(this.getResId());
        ret.setDiscuss(this.getDiscuss());
        ret.setCTime(this.getCTime());
        return ret;
    }

    public Map<String, Object> toHashMap4Mobile() {
        Map<String, Object> retM = new HashMap<String, Object>();
        if (!StringUtils.isNullOrEmptyOrSpace(this.id)) retM.put("Id", this.id);
        if (!StringUtils.isNullOrEmptyOrSpace(this.userId)) retM.put("UserId", this.userId);
        if (!StringUtils.isNullOrEmptyOrSpace(this.resTableName)) retM.put("MediaType", MediaType.buildByTabName(this.resTableName).getTypeName());
        if (!StringUtils.isNullOrEmptyOrSpace(this.resId)) retM.put("ContentId", this.resId);
        if (!StringUtils.isNullOrEmptyOrSpace(this.discuss)) retM.put("Discuss", this.discuss);
        if (this.CTime!=null) retM.put("Time", this.CTime.getTime());
        return retM;
    }
}
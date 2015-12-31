package com.woting.appengine.appopinion.model;

import java.util.ArrayList;
import java.util.List;

import com.spiritdata.framework.core.model.ModelSwapPo;
import com.spiritdata.framework.exceptionC.Plat0006CException;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.appopinion.persistence.pojo.AppOpinionPo;
import com.woting.appengine.appopinion.persistence.pojo.AppReOpinionPo;

/**
 * 反馈意见信息<br/>
 * 包括意见的反馈列表
 * @author wh
 */
public class AppOpinion extends AppOpinionPo implements  ModelSwapPo {
    private static final long serialVersionUID = 1020093563227522687L;
    private List<AppReOpinionPo> reList;
    public List<AppReOpinionPo> getReList() {
        return reList;
    }
    public void setReList(List<AppReOpinionPo> reList) {
        this.reList = reList;
    }

    public void addOneRe(AppReOpinionPo re) {
        if (reList==null) reList = new ArrayList<AppReOpinionPo>();
        for (AppReOpinionPo _re: this.reList) {
            if (!re.getOpinionId().equals(this.getId())) return;
            if (_re.getId().equals(re.getId())) return;
        }
        this.reList.add(re);
    }

    @Override
    public void buildFromPo(Object po) {
        if (po==null) throw new Plat0006CException("Po对象为空，无法从空对象得到概念/逻辑对象！");
        if (!(po instanceof AppOpinionPo)) throw new Plat0006CException("Po对象不是AppOpinionPo的实例，无法从此对象构建字典组对象！");
        AppOpinionPo _po = (AppOpinionPo)po;
        this.setId(_po.getId());
        this.setImei(_po.getImei());
        this.setUserId(_po.getUserId());
        this.setOpinion(_po.getOpinion());
        this.setCTime(_po.getCTime());
    }
    @Override
    public Object convert2Po() {
        AppOpinionPo ret = new AppOpinionPo();
        if (StringUtils.isNullOrEmptyOrSpace(this.getId())) ret.setId(SequenceUUID.getUUIDSubSegment(4));
        else ret.setId(this.getId());
        ret.setImei(this.getImei());
        ret.setUserId(this.getUserId());
        ret.setOpinion(this.getOpinion());
        ret.setCTime(this.getCTime());
        return ret;
    }
}
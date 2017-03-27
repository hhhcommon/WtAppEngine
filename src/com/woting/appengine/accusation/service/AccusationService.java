package com.woting.appengine.accusation.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.accusation.persis.po.ContentAccusationPo;
import com.woting.cm.core.broadcast.persis.po.BroadcastPo;
import com.woting.cm.core.media.MediaType;
import com.woting.cm.core.media.persis.po.MediaAssetPo;
import com.woting.cm.core.media.persis.po.SeqMediaAssetPo;
import com.woting.passport.mobile.MobileUDKey;

@Lazy(true)
@Service
public class AccusationService {
    @Resource(name="defaultDAO")
    private MybatisDAO<BroadcastPo> bcDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<MediaAssetPo> mediaAssetDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<SeqMediaAssetPo> seqMediaAssetDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<ContentAccusationPo> accusationPo;

    @PostConstruct
    public void initParam() {
        bcDao.setNamespace("A_BROADCAST");
        mediaAssetDao.setNamespace("A_MEDIA");
        seqMediaAssetDao.setNamespace("A_MEDIA");
        accusationPo.setNamespace("ACCUSATION");
    }

    /**
     * 喜欢或取消喜欢某个内容
     * @param mediaType 内容类型
     * @param contentId 内容Id
     * @param selReasons 选择性原因,用逗号隔开,例：3244e3234e23444352245::侵权,234::违法
     * @param inputReason 输入原因，100个汉字以内
     * @param mUdk 用户标识，可以是登录用户，也可以是手机设备
     * @return 若成功返回1；用户标识为空，返回-1；若内容Id为空返回-2；若mediaType不合法，返回-3；若得不到原因，返回-4；若内容不存在，返回-5；若选择性原因不合法，返回-6
     */
    public int accuseContent(String mediaType, String contentId, String selReasons, String inputReason, MobileUDKey mUdk) {
        if (mUdk==null)  return -1;
        if (StringUtils.isNullOrEmptyOrSpace(contentId))  return -2;
        if (MediaType.buildByTypeName(mediaType.toUpperCase())==MediaType.ERR)  return -3;
        if (StringUtils.isNullOrEmptyOrSpace(selReasons)&&StringUtils.isNullOrEmptyOrSpace(inputReason)) return -4;

        //检查内容是否存在
        Object c=null;
        switch (MediaType.buildByTypeName(mediaType.toUpperCase())) {
        case RADIO:
            c=bcDao.queryForListAutoTranform("getListByWhere", "a.id='"+contentId+"'");
            break;
        case AUDIO:
            c=mediaAssetDao.getInfoObject("getMaInfoById", contentId);
            break;
        case SEQU:
            c=seqMediaAssetDao.getInfoObject("getSmaInfoById", contentId);
            break;
        default:
            break;
        }
        if (c==null) return -5;

        boolean illegle=false;
        if (!StringUtils.isNullOrEmptyOrSpace(selReasons)) {
            String[] sp=selReasons.split(",");
            for (int i=0; i<sp.length; i++) {
                if (sp[i].indexOf("::")==-1) {
                    illegle=true;
                    break;
                }
            }
        }
        if (illegle) return -6;

        Map<String, Object> param=new HashMap<String, Object>();
        param.put("id", SequenceUUID.getPureUUID());
        param.put("resTableName", MediaType.buildByTypeName(mediaType.toUpperCase()).getTabName());
        param.put("resId", contentId);
        param.put("selReasons", selReasons);
        param.put("inputReason", inputReason);
        param.put("userId", mUdk.isUser()?mUdk.getUserId():"="+mUdk.getDeviceId());

        accusationPo.insert(param);
        return 1;
    }
}
package com.woting.cm.core.subscribe.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.woting.cm.core.channel.service.ChannelService;
import com.woting.cm.core.subscribe.persis.po.SubscribePo;
import com.woting.passport.mobile.MobileUDKey;


public class SubscribeService {
	@Resource(name = "dataSource")
	private DataSource DataSource;
	@Resource(name="defaultDAO")
    private MybatisDAO<SubscribePo> subscribeDao;
	@Resource
	private ChannelService channelService;
	
	@PostConstruct
    public void initParam() {
		subscribeDao.setNamespace("A_SUBSCRIBE");
	}
	
	/**
     * 订阅或取消订阅某个内容
     * @param contentId 内容Id
     * @param flag 操作类型:=1订阅；=0取消订阅，默认=1
     * @param mk 用户标识，可以是登录用户，也可以是手机设备
     * @return 若成功返回1；若是订阅：0=所指定的专辑不存在，2=已经订阅了此专辑；若是取消订阅：-1=还未订阅此专辑；
     */
	public int Subscribe(String contentId, int flag, MobileUDKey mUdk) {
		Map<String, Object> param=new HashMap<String, Object>();
        param.put("resTableName", "wt_SeqMediaAsset");
        param.put("resId", contentId);
        if (!channelService.isPub("wt_SeqMediaAsset", contentId)) return 0;
		if (flag==1) {
            if (mUdk.isUser()) {
                param.put("ownerType", "201");
                param.put("ownerId", mUdk.getUserId());
            } else {
                param.put("ownerType", "202");
                param.put("ownerId", mUdk.getDeviceId());
            }
            param.put("sId", contentId);
            if (subscribeDao.getCount(param)>0) return 2;
            param.put("id", SequenceUUID.getPureUUID());
            subscribeDao.insert(param);
		} else {
			if (isOrNoSubscribe(contentId, mUdk)) {
				delSubscribes(contentId, mUdk);
			} else return -1;
		}
		return 1;
	}
	
	/**
	 * 
	 * @param contentId
	 * @param mUdk
	 * @return =true已订阅;=false为订阅
	 */
	public boolean isOrNoSubscribe(String contentId, MobileUDKey mUdk) {
		Map<String, Object> param=new HashMap<String, Object>();
		param.put("sId", contentId);
		if (mUdk.isUser()) {
            param.put("ownerType", "201"); //登录用户
            param.put("ownerId", mUdk.getUserId());
        } else {
            param.put("ownerType", "202"); //手机设备
            param.put("ownerId", mUdk.getDeviceId());
        }
        if (subscribeDao.getCount(param)>0)
        	return true;
		return false;
	}
	
	public void delSubscribes(String contentIds, MobileUDKey mUdk) {
		Map<String, Object> param=new HashMap<String, Object>();
		String[] sids = contentIds.split(",");
		String orSql = "";
		for (String id : sids) {
			orSql += " or sId = '"+id+"'";
		}
		orSql = orSql.substring(3);
		orSql = " ( "+orSql+" )";
		param.put("whereByClause", orSql);
		if (mUdk.isUser()) {
            param.put("ownerType", "201"); //登录用户
            param.put("ownerId", mUdk.getUserId());
        } else {
            param.put("ownerType", "202"); //手机设备
            param.put("ownerId", mUdk.getDeviceId());
        }
		subscribeDao.delete(param);
	}
	
	public List<Map<String, Object>> getSubscribeList(int pageSize, int page, int sortType, MobileUDKey mUdk) {
		List<Map<String, Object>> retLs = new ArrayList<>();
		List<Map<String, Object>> ls = new ArrayList<>();
		List<Map<String, Object>> ls2 = new ArrayList<>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT res.* FROM"
					+ " (SELECT ss.*,cha.pubTime from (SELECT s.* from (SELECT sma.id,sma.smaTitle,sma.smaImg,ma.id maId,ma.maTitle,sub.cTime from wt_UserSubscribe sub"
					+ " LEFT JOIN wt_SeqMediaAsset sma ON sub.sId = sma.id LEFT JOIN wt_SeqMA_Ref smaf ON smaf.sId = sma.id LEFT JOIN wt_MediaAsset ma ON ma.id = smaf.mId"
					+ " where ";
			if (mUdk.isUser()) {
	            sql += " ownerType = '201' and ownerId = '"+mUdk.getUserId()+"' ";
	        } else {
	        	sql += " ownerType = '202' and ownerId = '"+mUdk.getDeviceId()+"' ";
	        }
			sql += "  ORDER BY smaf.columnNum DESC,smaf.cTime DESC,ma.maTitle DESC) s"
					+ " GROUP BY s.id) ss,"
					+ " wt_ChannelAsset cha"
					+ " where ((cha.assetType = 'wt_SeqMediaAsset' and cha.assetId = ss.id) OR (cha.assetType = 'wt_MediaAsset' and cha.assetId = ss.maid)) AND cha.flowFlag = 2"
					+ " ORDER BY cha.pubTime ASC) res"
					+ " GROUP BY res.id "
					+ " ORDER BY ";
			if (sortType == 1) sql += " res.pubTime  DESC";
			else if (sortType == 2) sql += " res.pubTime  ASC";
			else if (sortType == 3) sql += " res.cTime DESC";
			else if (sortType == 4) sql += " res.cTime ASC"; 
			else if (sortType == 5) sql += " res.pubTime  DESC, res.cTime DESC";
			else if (sortType == 6) sql += " res.pubTime  ASC, res.cTime ASC";
			
			sql += " limit "+(page-1)*pageSize+","+pageSize;
			conn = DataSource.getConnection();
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			String smaids = "";
			while (rs != null && rs.next()) {
				Map<String, Object> m1 = new HashMap<>();
				m1.put("ContentSeqName", rs.getString("smaTitle"));
				m1.put("ContentSeqId", rs.getString("id"));
				m1.put("ContentMediaName", rs.getString("maTitle"));
				m1.put("ContentMediaId", rs.getString("maId"));
				m1.put("ContentSeqImg", rs.getString("smaImg"));
				m1.put("ContentPubTime", rs.getTimestamp("cTime").getTime());
				m1.put("UpdateCount", 0);
				smaids += " or reqParam LIKE '%"+rs.getString("id")+"%'";
				ls.add(m1);
			}
			
			if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            
            if (ls.size()>0) {
				smaids = smaids.substring(3);
				sql = "SELECT sub.id,COUNT(*) num FROM"
						+ " (SELECT ld.*,smaf.mId,smaf.cTime,cha.pubTime FROM (SELECT res.* FROM (SELECT (CASE locate('ContentId', lda.reqParam) WHEN 0 THEN '' ELSE SUBSTR(lda.reqParam, locate('ContentId', lda.reqParam)+12,32) END) id,"
						+ " lda.apiName,lda.ownerId,lda.beginTime FROM ld_API lda where lda.apiName = '4.2.2-content/getContentInfo' and reqParam LIKE '%SEQU%' AND( "
						+ smaids+" ) and lda.ownerId = '123' ORDER BY lda.beginTime DESC) res GROUP BY res.id) ld, wt_SeqMA_Ref smaf  LEFT JOIN wt_ChannelAsset cha ON cha.assetType = 'wt_MediaAsset' and cha.flowFlag = 2 and smaf.mId = cha.assetId"
						+ " where ld.id = smaf.sid and ld.beginTime < smaf.cTime"
						+ " GROUP BY smaf.mId) sub"
						+ " GROUP BY sub.id";
				if (sortType==5) sql += " ORDER BY num desc"; 
				else if (sortType==6) sql += " ORDER BY num asc";
				sql += " limit "+(page-1)*pageSize+","+pageSize;
				ps = conn.prepareStatement(sql);
				rs = ps.executeQuery();
				if (sortType>=1 && sortType <=4) {
					Map<String , Object> mnum = new HashMap<>();
					while (rs != null && rs.next()) {
						mnum.put(rs.getString("id"), rs.getString("num"));
					}
					for (Map<String, Object> m : ls) {
						if (mnum.containsKey(m.get("ContentSeqId"))) {
							m.put("UpdateCount", mnum.get(m.get("ContentSeqId")));
						}
					}
					retLs = ls;
				} else {
					while (rs != null && rs.next()) {
						Iterator<Map<String, Object>> it = ls.iterator();
						while (it.hasNext()) {
							Map<String, Object> m = it.next();
							if (m.get("ContentSeqId").equals(rs.getString("id"))) {
								m.put("UpdateCount",rs.getString("num"));
								ls2.add(m);
								it.remove();
							}
						}
					}
					ls2.addAll(ls);
					if (sortType == 6) {
						ls.clear();
						for (int i = ls2.size()-1; i >= 0; i--) {
							ls.add(ls2.get(i));
						}
						ls2 = ls;
					}
					retLs = ls2;
				}
				if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
	            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
			}
            
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (rs!=null) try {rs.close();rs=null;} catch(Exception e) {rs=null;} finally {rs=null;};
            if (ps!=null) try {ps.close();ps=null;} catch(Exception e) {ps=null;} finally {ps=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
		}
		return retLs;
	}
}

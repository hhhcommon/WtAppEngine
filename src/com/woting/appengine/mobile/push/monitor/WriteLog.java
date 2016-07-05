package com.woting.appengine.mobile.push.monitor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.springframework.web.context.support.WebApplicationContextUtils;

import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.util.JsonUtils;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.mobile.push.mem.PushMemoryManage;

/**
 * 日志写数据库
 * @author wanghui
 */
public class WriteLog extends Thread {
    @Resource
    private DataSource dataSource;
    private Connection conn=null;
    private PreparedStatement insertMsg=null;
    private PreparedStatement insertAudio=null;

    private String logStr="";
    private String msgStr="";
    private String msgType="";
    private String fromAddress="";
    private String toAddress="";
    private long msgTime=0l;
    private Map<String, Object> msgMap=null;

    private long sendTime=0l;
    private int seqNo=0;
    private String audioType="";
    private String talkId="";
    private String audioStr="";

    private String tempStr="";
    
    private PushMemoryManage pmm=PushMemoryManage.getInstance();

    public void run() {
        try {
            ServletContext sc=(SystemCache.getCache(FConstants.SERVLET_CONTEXT)==null?null:(ServletContext)SystemCache.getCache(FConstants.SERVLET_CONTEXT).getContent());
            dataSource=(DataSource)WebApplicationContextUtils.getWebApplicationContext(sc).getBean("dataSource");
            boolean dbOk=preparedDb();
            while (true) {
                if (!dbOk) dbOk=preparedDb();
                else {
                    try {
                        logStr=pmm.logQueue.poll();
                        if (!StringUtils.isNullOrEmptyOrSpace(logStr)) {//写数据库
                            //解析消息
                            tempStr=logStr.substring(0, logStr.indexOf("::"));
                            msgTime=Long.parseLong(tempStr);
                            tempStr=logStr.substring(logStr.indexOf("::")+2);
                            msgType=tempStr.substring(0, tempStr.indexOf("::"));
                            msgStr=tempStr.substring(tempStr.indexOf("::")+2);
                            tempStr=msgStr.substring(0, msgStr.indexOf("::"));
                            msgStr=msgStr.substring(msgStr.indexOf("::")+2);
                            if (msgType.equals("send")) {
                                fromAddress="Server";
                                toAddress="undefined";
                                if (!tempStr.equals("NULL")) {
                                    toAddress=tempStr;

                                    tempStr=msgStr.substring(0, msgStr.indexOf("::"));
                                    toAddress+="_"+tempStr;
                                    msgStr=msgStr.substring(msgStr.indexOf("::")+2);

                                    tempStr=msgStr.substring(0, msgStr.indexOf("::"));
                                    toAddress+="_"+tempStr;
                                    msgStr=msgStr.substring(msgStr.indexOf("::")+2);
                                }
                            } else {
                                fromAddress="undefined";
                                toAddress="Server";
                                if (!tempStr.equals("NULL")) {
                                    fromAddress=tempStr;

                                    tempStr=msgStr.substring(0, msgStr.indexOf("::"));
                                    fromAddress+="_"+tempStr;
                                    msgStr=msgStr.substring(msgStr.indexOf("::")+2);

                                    tempStr=msgStr.substring(0, msgStr.indexOf("::"));
                                    fromAddress+="_"+tempStr;
                                    msgStr=msgStr.substring(msgStr.indexOf("::")+2);
                                }
                            }
                            msgMap=(Map<String, Object>)JsonUtils.jsonToObj(msgStr, Map.class);

                            insertMsg.setString(1, SequenceUUID.getUUIDSubSegment(4));
                            insertMsg.setString(2, msgType);
                            insertMsg.setLong(3, msgTime);
                            insertMsg.setString(4, fromAddress);
                            insertMsg.setString(5, toAddress);
                            insertMsg.setString(6, msgStr);
                            insertMsg.executeUpdate();
                            //音频包处理
                            if (msgMap.get("BizType").equals("AUDIOFLOW")) {
                                sendTime=Long.parseLong(msgMap.get("SendTime")+"");
                                seqNo=Integer.parseInt(((Map)msgMap.get("Data")).get("SeqNum")+"");
                                audioType=msgMap.get("CmdType")+"";
                                talkId=((Map)msgMap.get("Data")).get("TalkId")+"";
                                audioStr=((Map)msgMap.get("Data")).get("AudioData")+"";

                                insertAudio.setString(1, SequenceUUID.getUUIDSubSegment(4));
                                insertAudio.setString(2, msgType);
                                insertAudio.setLong(3, msgTime);
                                insertAudio.setLong(4, sendTime);
                                insertAudio.setString(5, fromAddress);
                                insertAudio.setString(6, toAddress);
                                insertAudio.setInt(7, seqNo);
                                insertAudio.setString(8, audioType);
                                insertAudio.setString(9, talkId);
                                insertAudio.setString(10, audioStr);
                                insertAudio.executeUpdate();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        dbOk=false;
                    }
                }
                try { sleep(10); } catch (InterruptedException e) {};//给10毫秒的延迟
            }
        } finally {
            if (insertMsg!=null) try {insertMsg.close();insertMsg=null;} catch(Exception e) {insertMsg=null;} finally {insertMsg=null;};
            if (insertAudio!=null) try {insertAudio.close();insertAudio=null;} catch(Exception e) {insertAudio=null;} finally {insertAudio=null;};
            if (conn!=null) try {conn.close();conn=null;} catch(Exception e) {conn=null;} finally {conn=null;};
        }
    }

    private boolean preparedDb() {
        try {
            conn=dataSource.getConnection();
            insertMsg=conn.prepareStatement("insert into ld_Message values(?, ?, ?, ?, ?, ?)");
            insertAudio=conn.prepareStatement("insert into ld_Audio values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            return true;
        } catch (Exception e) {
        }
        return false;
    }
}
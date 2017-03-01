package com.woting.push.socketclient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.FelEngineImpl;
import com.spiritdata.framework.FConstants;
import com.spiritdata.framework.core.cache.CacheEle;
import com.spiritdata.framework.core.cache.SystemCache;
import com.spiritdata.framework.jsonconf.JsonConfig;
import com.spiritdata.framework.util.StringUtils;
import com.woting.push.config.Config;

/**
 * Socket连接客户端配置信息
 * @author wanghui
 */
public class SocketClientConfig implements Config {
    protected Logger logger=LoggerFactory.getLogger(this.getClass());

    private String serverType; //本服务器的类型
    private String serverName; //本服务器的名称
    public String getServerType() {
        return serverType;
    }
    public void setServerType(String serverType) {
        this.serverType=serverType;
    }
    public String getServerName() {
        return serverName;
    }
    public void setServerName(String serverName) {
        this.serverName=serverName;
    }

    private String ip; //服务器端——ip地址
    private int port;  //服务器端——端口号
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip=ip;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port=port;
    }

    private long intervalBeat; //心跳信号发送时间间隔
    private long intervalCheckSocket; //检查Socket连接状况的时间间隔
    private long expireTime; //过期时间，多长时间未收到服务器消息，就认为连接中断了，这个时间要大于心跳和检查时间
    private long stopDelay; //多长时间后还未停止，则强行停止

    public long getIntervalBeat() {
        return intervalBeat;
    }
    public void setIntervalBeat(long intervalBeat) {
        this.intervalBeat=intervalBeat;
    }
    public long getIntervalCheckSocket() {
        return intervalCheckSocket;
    }
    public void setIntervalCheckSocket(long intervalCheckSocket) {
        this.intervalCheckSocket=intervalCheckSocket;
    }
    public long getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(long expireTime) {
        this.expireTime=expireTime;
    }
    public long getStopDelay() {
        return stopDelay;
    }
    public void setStopDelay(long stopDelay) {
        this.stopDelay=stopDelay;
    }

    private int[] checkGotoP;
    private int[] checkGotoN;
    protected List<String> reConnStrategy; //重连策略

    public List<String> getReConnStrategy() {
        return reConnStrategy;
    }
    public void setReConnStrategy(List<String> reConnStrategy) {
        this.reConnStrategy=reConnStrategy;
    }
    /**
     * 获得重连接的间隔时间，和下一个策略号
     * @param index 策略号
     * @return 重连接的间隔时间 and 下一个策略号，中间用"::"隔开
     */
    public String getReConnectIntervalTimeAndNextIndex(int index) {
        checkGotoP=new int[reConnStrategy.size()];
        for (int i=0; i<checkGotoP.length; i++) checkGotoP[i]=-1;
        checkGotoN=new int[reConnStrategy.size()];
        for (int i=0; i<checkGotoN.length; i++) checkGotoN[i]=-1;
        return _getReConnectIntervalTimeAndNextIndex(index);
    }

    private String logPath;

    public String getLogPath() {
        return logPath;
    }
    public void setLogPath(String logPath) {
        this.logPath=logPath;
    }
    
    /*
     * 获得重连接的间隔时间，和下一个策略号
     * @param index 策略号
     * @return 重连接的间隔时间 and 下一个策略号，中间用"::"隔开
     */
    private String _getReConnectIntervalTimeAndNextIndex(int index) {
        if (index==-1||this.reConnStrategy==null||this.reConnStrategy.size()==0) return ("-1::60000");//默认重连间隔一分钟
        int _index=(index>=this.reConnStrategy.size()?this.reConnStrategy.size()-1:index);
        String ways=this.reConnStrategy.get(_index);
        String[] _ws=ways.split("::");
        if (_ws[0].equals("INTE")) return _index+1+"::"+_ws[1];
        else if (_ws[0].equals("GOTO")) {//跳转到相应的
            int _gotoIndex=Integer.parseInt(_ws[1]);
            checkGotoP[getLastGotoIndex(checkGotoP)]=index;
            checkGotoN[getLastGotoIndex(checkGotoN)]=_gotoIndex;
            if (isLoopGoto()) return ("-1::60000"); //死循环了，返回默认值
            else return _getReConnectIntervalTimeAndNextIndex(_gotoIndex);
        } else {//出现问题，则到第一行
            return _getReConnectIntervalTimeAndNextIndex(0);
        }
    }
    private int getLastGotoIndex(int[] il) {
        for (int i=0; i<il.length; i++) if (il[i]==-1) return i;
        return -1;
    }
    private boolean isLoopGoto() {
    	for (int i=0; i<checkGotoN.length; i++) {
    		if (checkGotoN[i]!=-1) {
    			for (int j=0; j<checkGotoP.length; j++) {
    				if (checkGotoP[j]!=-1&&checkGotoN[i]==checkGotoP[j]) return true;
    			}
    		}
    	}
        return false;
    }

    public static SocketClientConfig loadConfig() {
        //生成默认
        SocketClientConfig scc=new SocketClientConfig();
        scc.setIp("localhost");
        scc.setPort(16789);
        scc.setIntervalBeat(23*1000);
        scc.setIntervalCheckSocket(19*1000);
        scc.setExpireTime(30*60*1000);
        scc.setStopDelay(10*1000);
        List<String> reConnStrategy=new ArrayList<String>();
        reConnStrategy.add("INTE::500");
        reConnStrategy.add("INTE::1000");
        reConnStrategy.add("INTE::3000");
        reConnStrategy.add("INTE::6000");
        reConnStrategy.add("GOTO::0");
        scc.setReConnStrategy(reConnStrategy);
        scc.setLogPath(null);

        //读取配置文件
        JsonConfig jc=null;
        try {
            String configFileName=(SystemCache.getCache(FConstants.APPOSPATH)==null?"":((CacheEle<String>)(SystemCache.getCache(FConstants.APPOSPATH))).getContent());
            configFileName+="WEB-INF"+File.separator+"app.jconf";
            jc=new JsonConfig(configFileName);
            scc.logger.info("配置文件信息={}", jc.getAllConfInfo());
        } catch(Exception e) {
            scc.logger.info(StringUtils.getAllMessage(e));
            jc=null;
            e.printStackTrace();
        }
        if (jc!=null) {
            FelEngine fel=new FelEngineImpl();
            String tmpStr="";
            int tmpInt=-1;

            try {
                tmpStr=jc.getString("serverIdentify.serverType");
            } catch(Exception e) {tmpStr="";}
            if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) scc.setServerType(tmpStr);
            tmpStr="";
            try {
                tmpStr=jc.getString("serverIdentify.serverName");
            } catch(Exception e) {tmpStr="";}
            if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) scc.setServerName(tmpStr);
            tmpStr="";

            try {
                tmpStr=jc.getString("socketClient.ip");
            } catch(Exception e) {tmpStr="";}
            if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) scc.setIp(tmpStr);
            tmpStr="";

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.port"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setPort(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.intervalBeat"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setIntervalBeat(tmpInt);
            tmpInt=-1;
            
            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.intervalCheckSocket"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setIntervalCheckSocket(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.expireTime"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setExpireTime(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.stopDelay"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) scc.setStopDelay(tmpInt);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("socketClient.reConnStrategy#size"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) {
                List<String> _reConnStrategy=new ArrayList<String>();
                for (int i=0; i<tmpInt; i++) {
                    try {
                        tmpStr=jc.getString("socketClient.reConnStrategy["+i+"]");
                    } catch(Exception e) {tmpStr="";}
                    if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) _reConnStrategy.add(tmpStr);
                }
                if (!_reConnStrategy.isEmpty()) {
                    scc.setReConnStrategy(_reConnStrategy);
                }
            }
            tmpStr="";

            try {
                tmpStr=jc.getString("logPath");
            } catch(Exception e) {tmpStr="";}
            if (!StringUtils.isNullOrEmptyOrSpace(tmpStr)) scc.setLogPath(tmpStr);
        }

        return scc;
    }
}
package com.woting.appengine.common.web;

import java.util.ArrayList;
import java.util.List;

import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.FelEngineImpl;
import com.spiritdata.framework.jsonconf.JsonConfig;
import com.spiritdata.framework.util.StringUtils;
import com.woting.appengine.content.ContentConfig;
import com.woting.passport.UGA.PasswordConfig;
import com.woting.push.socketclient.SocketClientConfig;

public abstract class ConfigLoadUtils {
    private final static FelEngine fel=new FelEngineImpl();


    public static SocketClientConfig getSocketClientConfig(JsonConfig jc) {
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

        if (jc!=null) {
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

    public static ContentConfig getContentConfig(JsonConfig jc) {
        //生成默认
        ContentConfig cc=new ContentConfig();

        //设置默认值
        cc.setUseRedis(true);
        cc.setResident(false);
        cc.setExpiredTime(30*60*60);

        if (jc!=null) {
            int tmpInt=-1;
            try {
                tmpInt=(int)fel.eval(jc.getString("contentRedisCfg.useRedis"));
            } catch(Exception e) {tmpInt=-1;}
            cc.setUseRedis(tmpInt==1);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("contentRedisCfg.isResident"));
            } catch(Exception e) {tmpInt=-1;}
            cc.setResident(tmpInt==1);
            tmpInt=-1;

            try {
                tmpInt=(int)fel.eval(jc.getString("contentRedisCfg.expiredTime"));
            } catch(Exception e) {tmpInt=-1;}
            if (tmpInt!=-1) cc.setExpiredTime(tmpInt);
            tmpInt=-1;
        }
        return cc;
    }

    public static PasswordConfig getPasswordConfig(JsonConfig jc) {
        //生成默认
        PasswordConfig pc=new PasswordConfig();

        //设置默认值
        pc.setEncryption(false);
        pc.setReEncryption(false);

        if (jc!=null) {
            int tmpInt=-1;
            try {
                tmpInt=(int)fel.eval(jc.getString("passwordCfg.isEncryption"));
            } catch(Exception e) {tmpInt=-1;}
            pc.setEncryption(tmpInt==1);
            tmpInt=-1;
            try {
                tmpInt=(int)fel.eval(jc.getString("passwordCfg.reEncryption"));
            } catch(Exception e) {tmpInt=-1;}
            pc.setReEncryption(tmpInt==1);
            tmpInt=-1;
        }
        return pc;
    }
}
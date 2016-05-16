package com.woting.appengine.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;

import com.spiritdata.framework.util.JsonUtils;

public abstract class RequestUtils {
    /**
     * 从Request获得所有参数，包括流中的json参数和？后的参数，若重复以？后的参数为准
     * @param req 请求内容
     * @return 返回参数
     */
    public static Map<String, Object> getDataFromRequest(ServletRequest req) {
        Map<String, Object> retM=new HashMap<String, Object>();
        //1-从数据流中获得参数：这种参数必须是json格式的
        InputStreamReader isr=null;
        BufferedReader br=null;
        try {
            isr=new InputStreamReader((ServletInputStream)req.getInputStream(), "UTF-8");
            br=new BufferedReader(isr);
            String line=null;
            StringBuilder sb=new StringBuilder();
            while ((line=br.readLine())!=null) sb.append(line);
            line=sb.toString().trim();
            if (line!=null&&line.length()>0) {
                try {
                    retM=(Map<String, Object>)JsonUtils.jsonToObj(line, Map.class);
                } catch(Exception e) {
                    retM=getDataFromUrlQueryStr(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isr!=null) try {isr.close();} catch(Exception e) {}
            if (br!=null) try {br.close();} catch(Exception e) {}
        }
        //2-从?参数中读取数据
        if (retM==null) retM=new HashMap<String, Object>();
        Enumeration<String> enu=req.getParameterNames();
        while(enu.hasMoreElements()) {
            String name=(String)enu.nextElement();
            retM.put(name, req.getParameter(name));
        }
        if (retM==null||retM.isEmpty()) return null;
        return retM;
    }

    private static Map<String, Object> getDataFromUrlQueryStr(String queryStr) throws UnsupportedEncodingException {
        Map<String, Object> retM=new HashMap<String, Object>();
        String pkvs[]=queryStr.split("&");
        for (String kv: pkvs) {
            String kvD[]=kv.trim().split("=");
            if (kvD.length==2) retM.put(URLDecoder.decode(kvD[0],"UTF-8"), URLDecoder.decode(kvD[1],"UTF-8"));
        }
        return retM;
    }
}
package com.woting.appengine.searchcrawler.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.woting.appengine.searchcrawler.model.Festival;
import com.woting.appengine.searchcrawler.model.Station;
import com.woting.appengine.searchcrawler.utils.DataTransform;


public class ThreadService {

    DataTransform dataT = new DataTransform();

    /**
     * 使用多线程在三平台查询内容
     * @param constr 三平台查询的内容
     * @return
     */
    public Map<String, Object> threadService(String constr,int resultType,int pageType){
        Map<String, Object> maps = new HashMap<String,Object>();
        ExecutorService pool = Executors.newFixedThreadPool(4);    //线程池大小3
        QingTingService qt = new QingTingService(constr);        //创建蜻蜓搜索线程
        KaoLaService kl = new KaoLaService(constr);                //创建考拉搜索线程
        XiMaLaYaService xm = new XiMaLaYaService(constr);        //创建喜马拉雅搜索线程
        LocalService ls = new LocalService(constr, resultType, pageType);
        Future<Map<String, Object>> kl_s = pool.submit(kl);        //开启考拉线程
        Future<Map<String, Object>> qt_s = pool.submit(qt);        //开启蜻蜓线程
        Future<Map<String, Object>> xm_s = pool.submit(xm);        //开启喜马拉雅线程
        Future<Map<String, Object>> ls_s = pool.submit(ls);
        try {
            maps.put("KL", kl_s.get()==null?null:kl_s.get());        //考拉搜索返回Map，get()等待线程执行完并返回结果
            maps.put("QT", qt_s.get()==null?null:qt_s.get());        //蜻蜓搜索返回Map
            maps.put("XMLY", xm_s.get()==null?null:xm_s.get());        //喜马拉雅搜索返回Map
            maps.put("LS", ls_s.get()==null?null:ls_s.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        pool.shutdown();                    //线程关闭
        return maps;
    }
    
    /**
        * 考拉，蜻蜓和喜马拉雅三平台搜索
        * @param searchStr
        * @param resultType
        * @param pageType
        * @return
        */
       public Map<String, Object> searchWebAndLocal(String searchStr, int resultType, int pageType) {
           Map<String, Object> map = new HashMap<String,Object>();
           int count = 0;
           String __s[]=searchStr.split(",");
           String _s[]=new String[__s.length];
           for (int i=0; i<__s.length; i++) _s[i]=__s[i].trim();       
           { //敏感词处理
               
           }
           if(resultType==0&&pageType==0){
               //按照0::0处理
               for(int i = 0;i<_s.length;i++){
                    Map<String, Object> maps = threadService(_s[i],resultType,pageType);                //开启多线程查询
                    Map<String, Object> mapkl = (Map<String, Object>) maps.get("KL");                //得到考拉搜索返回结果
                    Map<String, Object> mapqt = (Map<String, Object>) maps.get("QT");                //得到蜻蜓搜索返回结果
                    Map<String, Object> mapxmly = (Map<String, Object>) maps.get("XMLY");            //得到喜马拉雅搜索返回结果
                    Map<String, Object>    maplocal = (Map<String, Object>) maps.get("LS")==null?null:(Map<String, Object>) maps.get("LS");        //本地数据库搜索
                    List<Festival> list_kl_festival = (List<Festival>) mapkl.get("KL_F");        //取出考拉搜索的节目信息
                    List<Station> list_kl_station = (List<Station>) mapkl.get("KL_S");            //取出考拉搜索的频道信息
                    List<Festival> list_qt_festival = (List<Festival>) mapqt.get("QT_F");        //取出蜻蜓搜索的节目信息
                    List<Station> list_qt_station = (List<Station>) mapqt.get("QT_S");            //取出蜻蜓搜索的频道信息
                    List<Festival> list_xmly_festival = (List<Festival>) mapxmly.get("XMLY_F");    //取出喜马拉雅搜索的节目信息
                    List<Station> list_xmly_station = (List<Station>) mapxmly.get("XMLY_S");    //取出喜马拉雅搜索的频道信息
                    List<Station> liststation = new ArrayList<Station>();        //合并搜索到的频道信息
                    List<Festival> listfestival = new ArrayList<Festival>();    //合并搜索到的节目信息
                    if(list_kl_station!=null)liststation.addAll(list_kl_station);
                    if(list_qt_station!=null)liststation.addAll(list_qt_station);
                    if(list_kl_festival!=null)listfestival.addAll(list_kl_festival);
                    if(list_qt_festival!=null)listfestival.addAll(list_qt_festival);
                    if(list_xmly_station!=null)liststation.addAll(list_xmly_station);
                    if(list_xmly_festival!=null)listfestival.addAll(list_xmly_festival);
                    List<Map<String, Object>> listall = dataT.datas2Audio(listfestival,liststation,0);    //数据信息转换        0为PageType信息，暂定ResultType为0        
                    if(maplocal!=null){
                        List<Map<String, Object>> listlocal = (List<Map<String,Object>>)maplocal.get("List");
                        for (Map<String, Object> maplocalfes : listlocal) {
                            listall.add(maplocalfes);
                        }
                        count += listall.size();
                    }
                    map.put("AllCount", listfestival.size()+liststation.size()+count);
                    map.put("List", listall);    
                    map.put("ResultType", resultType);
                  }
           }else{
                   for(int i = 0;i<_s.length;i++){
                        Map<String, Object> maps = threadService(_s[i],resultType,pageType);                //开启多线程查询
                        map = (Map<String, Object>) maps.get("LS");        //本地数据库搜索  
                      }
               }
           return map;
    }
}

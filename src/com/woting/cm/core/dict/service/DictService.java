package com.woting.cm.core.dict.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.core.model.Page;
import com.spiritdata.framework.core.model.tree.TreeNode;
import com.spiritdata.framework.core.model.tree.TreeNodeBean;
import com.spiritdata.framework.util.TreeUtils;
import com.woting.cm.core.dict.mem._CacheDictionary;
import com.woting.cm.core.dict.model.DictDetail;
import com.woting.cm.core.dict.model.DictMaster;
import com.woting.cm.core.dict.model.DictModel;
import com.woting.cm.core.dict.model.DictRefRes;
import com.woting.cm.core.dict.persis.po.DictDetailPo;
import com.woting.cm.core.dict.persis.po.DictMasterPo;
import com.woting.cm.core.dict.persis.po.DictRefResPo;
import com.woting.exceptionC.Wtcm0301CException;
import com.woting.exceptionC.Wtcm1000CException;

@Service
public class DictService {
    @Resource(name="defaultDAO")
    private MybatisDAO<DictMasterPo> dictMDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<DictDetailPo> dictDDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<DictRefResPo> dictRefDao;
    
    @PostConstruct
    public void initParam() {
        dictMDao.setNamespace("A_DMASTER");
        dictDDao.setNamespace("A_DDETAIL");
        dictRefDao.setNamespace("A_DREFRES");
    }

    //一、以下是字典相关的操作
    /**
     * 加载字典信息
     */
    public _CacheDictionary loadCache() {
        _CacheDictionary _cd=new _CacheDictionary();

        try {
            //字典组列表
            Map<String, Object> param=new HashMap<String, Object>();
            param.put("ownerType", "100");
            param.put("sortByClause", "id");
            List<DictMasterPo> dmpol=dictMDao.queryForList(param);
            if (dmpol==null||dmpol.size()==0) return null;
            List<DictMaster> dml=new ArrayList<DictMaster>();
            for (DictMasterPo dmp: dmpol) {
                DictMaster dm=new DictMaster();
                dm.buildFromPo(dmp);
                dml.add(dm);
            }
            _cd.dmList =(dml.size()==0?null:dml);

            //组装dictModelMap
            if (_cd.dmList!=null&&_cd.dmList.size()>0) {
                //Map主对应关系
                for (DictMaster dm: _cd.dmList) {
                    DictModel dModel=new DictModel(dm);
                    DictDetail _t=new DictDetail();
                    _t.setId(dModel.getId());
                    _t.setMId(dModel.getId());
                    _t.setNodeName(dModel.getDmName());
                    _t.setIsValidate(1);
                    _t.setParentId(null);
                    _t.setOrder(1);
                    _t.setBCode("root");
                    TreeNode<? extends TreeNodeBean> root=new TreeNode<DictDetail>(_t);
                    dModel.dictTree=(TreeNode<DictDetail>)root;
                    _cd.dictModelMap.put(dm.getId(), dModel);
                }

                //构造单独的字典树
                String tempDmId="";
                param.put("ownerId", "cm");
                param.put("ownerType", "100");
                int i=1;
                Page<DictDetailPo> ddPage=dictDDao.pageQuery("getListByOnwerDemo", param, i++, 10000);
                List<DictDetailPo> ddpol=new ArrayList<DictDetailPo>();
                boolean hasDD=!ddPage.getResult().isEmpty();
                //分页处理
                while (hasDD) {
                    ddpol.addAll(ddPage.getResult());
                    ddPage=dictDDao.pageQuery("getListByOnwerDemo", param, i++,10000);
                    hasDD=!ddPage.getResult().isEmpty();
                }
                if (ddpol==null||ddpol.size()==0) return _cd;
                List<DictDetail> ddl=new ArrayList<DictDetail>();
                for (DictDetailPo ddp: ddpol) {
                    DictDetail dd=new DictDetail();
                    dd.buildFromPo(ddp);
                    ddl.add(dd);
                }

                List<DictDetail> templ=new ArrayList<DictDetail>();
                _cd.ddList=(ddl.size()==0?null:ddl);//字典项列表，按照层级结果，按照排序的广度遍历树
                if (_cd.ddList!=null&&_cd.ddList.size()>0) {
                    for (DictDetail dd: _cd.ddList) {
                        if (tempDmId.equals(dd.getMId())) templ.add(dd);
                        else {
                            buildDictTree(templ, _cd);
                            templ.clear();
                            templ.add(dd);
                            tempDmId=dd.getMId();
                        }
                    }
                    //最后一个记录的后处理
                    buildDictTree(templ, _cd);
                }
            }
            //处理空树
            return _cd;
        } catch(Exception e) {
            e.printStackTrace();
            throw new Wtcm1000CException("加载Session中的字典信息", e);
        }
    }
    /*
     * 以ddList为数据源(同一字典组的所有字典项的列表)，构造所有者字典数据中的dictModelMap中的dictModel对象中的dictTree
     * @param ddList 同一字典组的所有字典项的列表
     * @param od 所有者字典数据
     */
    private void buildDictTree(List<DictDetail> ddList, _CacheDictionary cd) {
        if (ddList.size()>0) {//组成树
            DictModel dModel=cd.dictModelMap.get(ddList.get(0).getMId());
            if (dModel!=null) {
                Map<String, Object> m=TreeUtils.convertFromList(ddList);
                dModel.dictTree.setChildren((List<TreeNode<? extends TreeNodeBean>>)m.get("forest"));
                //暂不处理错误记录
            }
        }
    }

    /**
     * 新增字典项，加入数据库
     * @param dd 字典项信息
     */
    public void addDictDetail(DictDetail dd) {
        try {
            DictDetailPo newDdp=dd.convert2Po();
            dictDDao.insert(newDdp);
        } catch(Exception e) {
            throw new Wtcm0301CException(e);
        }
    }

    /**
     * 绑定字典与资源的关系
     * @param dd 字典项信息
     */
    public void bindDictRef(DictRefRes drr) {
        try {
            DictRefResPo newDrrPo=drr.convert2Po();
            dictRefDao.insert(newDrrPo);
        } catch(Exception e) {
        }
    }
}
package com.woting.passport.useralias.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.spiritdata.framework.core.dao.mybatis.MybatisDAO;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.passport.UGA.persis.pojo.UserPo;
import com.woting.passport.useralias.mem.UserAliasMemoryManage;
import com.woting.passport.useralias.model.UserAliasKey;
import com.woting.passport.useralias.persis.pojo.UserAliasPo;

@Service
public class UserAliasService {
    private UserAliasMemoryManage uamm=UserAliasMemoryManage.getInstance();

    @Resource(name="defaultDAO")
    private MybatisDAO<UserPo> userDao;
    @Resource(name="defaultDAO")
    private MybatisDAO<UserAliasPo> userAliasDao;

    @PostConstruct
    public void initParam() {
        userAliasDao.setNamespace("WT_USERALIAS");
        userDao.setNamespace("WT_USER");
    }

    /**
     * 从数据库加载所有用户别名信息
     * @return
     */
    public List<UserAliasPo> getAllAlias() {
        return userAliasDao.queryForList();
    }

    /**
     * 保存用户别名，包括新增和修改
     * @return -1别名或别名描述为空,-2主用户不存在;-3别名用户不存在,1新增成功;2修改成功;0不需要保存
     */
    public int save(UserAliasPo uaPo) {
        if (StringUtils.isNullOrEmptyOrSpace(uaPo.getAliasName())&&StringUtils.isNullOrEmptyOrSpace(uaPo.getAliasDescn())) {
            return -1;
        }
        UserPo mainUser, aliasUser;
        mainUser=userDao.getInfoObject("getUserById", uaPo.getMainUserId());
        if (mainUser==null) return -2;
        aliasUser=userDao.getInfoObject("getUserById", uaPo.getAliasUserId());
        if (aliasUser==null) return -3;

        
        //检查内存中是否存在
        UserAliasPo _uaPo = uamm.getOneUserAlias(uaPo.getAliasKey());
        if (_uaPo==null) {//加入，插入
            if (StringUtils.isNullOrEmptyOrSpace(uaPo.getAliasName())) {
                uaPo.setAliasName(aliasUser.getLoginName());//用户名称???
            }
            uamm.addOneUserAlias(uaPo);
            uaPo.setId(SequenceUUID.getUUIDSubSegment(4));
            userAliasDao.insert(uaPo);
            return 1;
        }
        //修改
        if (uaPo.equals(_uaPo)) return 0;
        else {
            if (StringUtils.isNullOrEmptyOrSpace(uaPo.getAliasName())) uaPo.setAliasName(_uaPo.getAliasName());
            uamm.addOneUserAlias(uaPo);
            userAliasDao.update("updateByKey", uaPo);
            return 2;
        }
    }

    /**
     * 删除某一别名
     * @param uaPo
     */
    public void del(UserAliasKey uak) {
        uamm.delUserAlias(uak);
        userAliasDao.delete("deleteByEntity", uak.toHashMap());
    }

    /**
     * 删除某一用户群组下的所有别名
     * @param uaPo
     */
    public void delAliasInGroup(String groupId) {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("typeId", groupId);
        userAliasDao.delete("deleteByEntity", param);
        uamm.delAliasInOneGroup(groupId);
    }

    /**
     * 删除某一用户群组下，某一用户的别名
     * @param uaPo
     */
    public void delUserAliasInGroup(String groupId, String userId) {
        Map<String, Object> param=new HashMap<String, Object>();
        param.put("typeId", groupId);
        param.put("mainUserId", userId);
        userAliasDao.delete("deleteByEntity", param);
        param.clear();
        param.put("typeId", groupId);
        param.put("aliasUserId", userId);
        userAliasDao.delete("deleteByEntity", param);

        uamm.delUserAliasInGroup(groupId, userId);
    }
}
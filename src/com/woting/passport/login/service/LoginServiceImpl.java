package com.woting.passport.login.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.spiritdata.framework.UGA.UgaUser;
import com.spiritdata.framework.component.login.service.LoginService;

public class LoginServiceImpl implements LoginService {

    @Override
    public Map<String, Object> afterUserLoginOk(UgaUser arg0, HttpServletRequest arg1) {
        return null;
    }

    @Override
    public Map<String, Object> beforeUserLogin(HttpServletRequest arg0) {
        return null;
    }

    @Override
    public Map<String, Object> onLogout(HttpServletRequest arg0) {
        return null;
    }
}
package com.woting.appengine.common.web;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;

import com.spiritdata.framework.core.web.AbstractFileUploadController;

@Controller
public class FileUploadController extends AbstractFileUploadController{
    @Override
    public Map<String, Object> afterUploadOneFileOnSuccess(Map<String, Object> m, Map<String, Object> a, Map<String, Object> p) {
        System.out.println(m.toString());
        return null;
    }

    @Override
    public void afterUploadAllFiles(List<Map<String, Object>> fl, Map<String, Object> a, Map<String, Object> p) {
        //System.out.println(fl.toString());
    }
}

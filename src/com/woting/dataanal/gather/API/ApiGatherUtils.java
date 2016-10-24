package com.woting.dataanal.gather.API;

import java.sql.Timestamp;
import javax.servlet.http.HttpServletRequest;
import com.spiritdata.framework.util.SequenceUUID;
import com.spiritdata.framework.util.StringUtils;
import com.woting.dataanal.gather.API.persis.pojo.ApiLogPo;

public abstract class ApiGatherUtils {
    public static ApiLogPo buildApiLogDataFromRequest(HttpServletRequest request) {
        ApiLogPo alPo=new ApiLogPo();
        alPo.setId(SequenceUUID.getPureUUID());
        alPo.setMethod(request.getMethod());
        String tempStr=request.getQueryString();
        tempStr=request.getRequestURL()+(StringUtils.isNullOrEmptyOrSpace(tempStr)?"":"?"+tempStr);
        alPo.setReqUrl(tempStr);
        alPo.setBeginTime(new Timestamp(System.currentTimeMillis()));
        return alPo;
    }
}
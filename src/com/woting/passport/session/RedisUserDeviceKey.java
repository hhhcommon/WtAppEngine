package com.woting.passport.session;

import com.woting.redis.session.RedisLoginData;

public class RedisUserDeviceKey extends UserDeviceKey implements RedisLoginData {

    @Override
    public String getLockKey() {
        return null;
    }

}
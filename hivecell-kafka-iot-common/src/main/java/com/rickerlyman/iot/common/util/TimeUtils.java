package com.rickerlyman.iot.common.util;

import java.time.Instant;

public class TimeUtils {

    public static final long PT1S = 1000;

    public static Long now(){
        return Instant.now().toEpochMilli();
    }

}

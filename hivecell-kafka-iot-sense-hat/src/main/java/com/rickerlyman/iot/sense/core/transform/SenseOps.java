package com.rickerlyman.iot.sense.core.transform;

import com.rickerlyman.iot.schema.SenseCommand;
import com.rickerlyman.iot.schema.SenseReadingOutput;

import java.util.Collections;

public class SenseOps {

    public static Iterable<SenseCommand> alert(String key, SenseReadingOutput value) {
        if (value.getTemperature() > 30) {
            return Collections.singletonList(new SenseCommand(value.getSensorId(), "beep"));
        } else {
            return Collections.emptyList();
        }
    }
}

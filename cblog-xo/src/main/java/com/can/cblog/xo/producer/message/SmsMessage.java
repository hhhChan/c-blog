package com.can.cblog.xo.producer.message;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author ccc
 */
@Data
@Accessors(chain = true)
public class SmsMessage {
    public static final String TOPIC = "SMS_MESSAGE";

    private Map<String, String> map;
}

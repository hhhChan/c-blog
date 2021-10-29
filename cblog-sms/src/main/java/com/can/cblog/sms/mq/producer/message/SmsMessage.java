package com.can.cblog.sms.mq.producer.message;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ccc
 */
@Data
@Accessors(chain = true)
public class SmsMessage {
    public static final String TOPIC = "SMS_MESSAGE";

}

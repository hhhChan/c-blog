package com.can.cblog.sms.mq.producer.message;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ccc
 */
@Data
@Accessors(chain = true)
public class MailMessage {
    public static final String TOPIC = "Mail_MESSAGE";

    private String subject;

    private String receiver;

    private String text;
}

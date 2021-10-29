package com.can.cblog.sms.mq.producer.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author ccc
 */
@Data
@Accessors(chain = true)
public class BlogMessage {

    public static final String TOPIC = "BLOG_MESSAGE";

    private Map<String, String> map;
}

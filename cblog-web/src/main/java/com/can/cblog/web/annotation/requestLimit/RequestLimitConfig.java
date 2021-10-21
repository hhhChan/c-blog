package com.can.cblog.web.annotation.requestLimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @author ccc
 */
@RefreshScope
@ConfigurationProperties(prefix = "request-limit")
@Component
@Data
public class RequestLimitConfig {

    /**
     * 是否开启请求限制
     */
    private Boolean start;

    /**
     * 允许访问的数量
     */
    private int amount;

    /**
     * 时间段
     */
    private long time;
}

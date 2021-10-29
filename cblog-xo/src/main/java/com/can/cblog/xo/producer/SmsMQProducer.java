package com.can.cblog.xo.producer;

import com.can.cblog.xo.producer.message.BlogMessage;
import com.can.cblog.xo.producer.message.MailMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ccc
 */
@Component
@Slf4j
// 后续优化下，考虑下一致性
public class SmsMQProducer {

    @Autowired
    private RocketMQTemplate template;

    public void sendBlogMessage(BlogMessage message) {
        try {
            SendResult sendResult = template.syncSend(BlogMessage.TOPIC, message);
            if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                log.error("[sendBlogMessage][消息({}) 发送更新消息失败，结果为({})]", message, sendResult);
            }
        } catch (Throwable throwable) {
            log.error("[sendBlogMessage][消息({}) 发送更新消息失败，发生异常]", message, throwable);
        }
    }

    public void sendMailMessage(MailMessage message) {
        try {
            SendResult sendResult = template.syncSend(MailMessage.TOPIC, message);
            if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                log.error("[sendMailMessage][消息({}) 发送更新消息失败，结果为({})]", message, sendResult);
            }
        } catch (Throwable throwable) {
            log.error("[sendMailMessage][消息({}) 发送更新消息失败，发生异常]", message, throwable);
        }
    }


}

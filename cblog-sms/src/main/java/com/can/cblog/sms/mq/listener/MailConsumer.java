package com.can.cblog.sms.mq.listener;

import com.can.cblog.sms.mq.producer.message.MailMessage;
import com.can.cblog.sms.util.SendMailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author ccc
 */
@Service
@RocketMQMessageListener(
        topic = MailMessage.TOPIC,
        consumerGroup = "blog-consumer-group-" + MailMessage.TOPIC
)
@Slf4j
public class MailConsumer implements RocketMQListener<MailMessage> {

    @Autowired
    private SendMailUtils sendMailUtils;

    @Override
    public void onMessage(MailMessage message) {
        if (!Objects.isNull(message)) {
            sendMailUtils.sendEmail(message.getSubject(), message.getReceiver(), message.getText());
        }
    }
}

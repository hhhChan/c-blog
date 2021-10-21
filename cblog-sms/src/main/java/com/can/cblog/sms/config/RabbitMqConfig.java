package com.can.cblog.sms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置文件【可用于自动生成队列和交换机】
 * @author ccc
 */
@Configuration
public class RabbitMqConfig {

    public static final String CBLOG_BLOG = "cblog.blog";
    public static final String CBLOG_EMAIL = "cblog.email";
    public static final String CBLOG_SMS = "cblog.sms";
    public static final String EXCHANGE_DIRECT = "exchange.direct";
    public static final String ROUTING_KEY_BLOG = "cblog.blog";
    public static final String ROUTING_KEY_EMAIL = "cblog.email";
    public static final String ROUTING_KEY_SMS = "cblog.sms";

    /**
     * 声明交换机
     */
    @Bean(EXCHANGE_DIRECT)
    public Exchange EXCHANGE_DIRECT() {
        // 声明路由交换机，durable:在rabbitmq重启后，交换机还在
        return ExchangeBuilder.directExchange(EXCHANGE_DIRECT).durable(true).build();
    }

    /**
     * 声明Blog队列
     *
     * @return
     */
    @Bean(CBLOG_BLOG)
    public Queue CBLOG_BLOG() {
        return new Queue(CBLOG_BLOG);
    }

    /**
     * 声明Email队列
     *
     * @return
     */
    @Bean(CBLOG_EMAIL)
    public Queue CBLOG_EMAIL() {
        return new Queue(CBLOG_EMAIL);
    }

    /**
     * 声明SMS队列
     *
     * @return
     */
    @Bean(CBLOG_SMS)
    public Queue CBLOG_SMS() {
        return new Queue(CBLOG_SMS);
    }

    /**
     * cblog.blog 队列绑定交换机，指定routingKey
     *
     * @param queue
     * @param exchange
     * @return
     */
    @Bean
    public Binding BINDING_QUEUE_INFORM_BLOG(@Qualifier(CBLOG_BLOG) Queue queue, @Qualifier(EXCHANGE_DIRECT) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_BLOG).noargs();
    }

    /**
     * cblog.mail 队列绑定交换机，指定routingKey
     *
     * @param queue
     * @param exchange
     * @return
     */
    @Bean
    public Binding BINDING_QUEUE_INFORM_EMAIL(@Qualifier(CBLOG_EMAIL) Queue queue, @Qualifier(EXCHANGE_DIRECT) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_EMAIL).noargs();
    }

    /**
     * cblog.sms 队列绑定交换机，指定routingKey
     *
     * @param queue
     * @param exchange
     * @return
     */
    @Bean
    public Binding BINDING_QUEUE_INFORM_SMS(@Qualifier(CBLOG_SMS) Queue queue, @Qualifier(EXCHANGE_DIRECT) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_SMS).noargs();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}


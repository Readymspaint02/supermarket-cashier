package com.zmj.gbs_commerce_system.config;

/**
 * ============================================================
 * 【MQ-01】RabbitMQConfig - 消息队列配置
 * ============================================================
 * 
 * 文件作用：
 * 配置RabbitMQ消息队列，包括交换机、队列、绑定关系。
 * 核心功能：订单支付成功后，异步处理会员积分。
 * 
 * 技术原理：
 * - TopicExchange：主题交换机，支持通配符路由
 * - Queue：消息队列，存储消息
 * - Binding：绑定关系，交换机如何路由消息到队列
 * - Routing Key：路由键，决定消息去哪个队列
 * 
 * 消息流转流程：
 * 1. 订单支付成功 → 发送消息到 order.exchange 交换机
 * 2. 交换机根据 Routing Key（order.paid）路由消息
 * 3. 消息到达 order.paid.queue 队列
 * 4. 消费者监听队列，取出消息处理
 * 5. 处理会员积分：查询会员 → 计算积分 → 更新数据库 → 刷新缓存
 * 
 * 面试考点：
 * - Q1：RabbitMQ的核心组件有哪些？
 *   A1：Producer（生产者）、Consumer（消费者）、Exchange（交换机）、
 *       Queue（队列）、Binding（绑定）、Routing Key（路由键）。
 * 
 * - Q2：Exchange的类型有哪些？为什么用Topic？
 *   A2：- Direct：精确匹配Routing Key
 *       - Topic：通配符匹配，*匹配一个单词，#匹配多个（推荐）
 *       - Fanout：广播到所有队列
 *       - Headers：根据消息头匹配（不常用）
 *       我们用Topic因为灵活，可以根据业务扩展更多队列。
 * 
 * - Q3：为什么队列要持久化（new Queue(name, true)）？
 *   A3：持久化后，RabbitMQ重启消息不会丢失。
 *       true表示持久化，false表示不持久化。
 * 
 * - Q4：消费者配置3-10个并发，为什么？
 *   A4：- concurrentConsumers=3：初始3个消费者线程
 *       - maxConcurrentConsumers=10：最多10个线程
 *       - 根据消息量动态调整，提高处理效率
 * 
 * - Q5：prefetchCount=1是什么意思？
 *   A5：每个消费者一次只取1条消息，处理完再取下一条。
 *       避免某个消费者抢占大量消息导致其他消费者空闲，
 *       实现负载均衡。
 * 
 * - Q6：如何保证消息不丢失？
 *   A6：1. 队列持久化：new Queue(name, true)
 *       2. 消息持久化：MessageProperties.PERSISTENT_TEXT_PLAIN
 *       3. 生产者确认：confirm回调机制
 *       4. 消费者手动ACK：处理成功才确认
 * 
 * 关联文件：
 * - mq/consumer/MemberPointsConsumer.java（积分消费者）
 * - mq/producer/OrderPaidProducer.java（消息生产者）
 * - service/impl/OrderServiceImpl.java（发送消息）
 * 
 * 参考文档：
 * - 梳理项目.md 3.4 RabbitMQ异步处理模块
 * - 项目难点讲解.txt 难点三：RabbitMQ异步处理会员积分
 * ============================================================
 */

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 【MQ-01-常量定义】交换机、队列、路由键名称
    public static final String ORDER_EXCHANGE = "order.exchange";           // 订单交换机
    public static final String ORDER_PAID_QUEUE = "order.paid.queue";       // 订单支付队列
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";       // 订单支付路由键

    public static final String MEMBER_POINTS_QUEUE = "member.points.queue"; // 会员积分队列
    public static final String MEMBER_POINTS_ROUTING_KEY = "member.points"; // 会员积分路由键

    // 【MQ-01-交换机】TopicExchange - 主题交换机
    // 面试考点：为什么用Topic不用Direct？
    // 答：Topic支持通配符，更灵活。例如：order.* 可以匹配 order.paid、order.refund
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    // 【MQ-01-队列】orderPaidQueue - 订单支付队列
    // 面试考点：第二个参数true是什么意思？
    // 答：true表示持久化，RabbitMQ重启后队列和消息不会丢失
    @Bean
    public Queue orderPaidQueue() {
        return new Queue(ORDER_PAID_QUEUE, true); // true = 持久化
    }

    // 【MQ-01-队列】memberPointsQueue - 会员积分队列
    @Bean
    public Queue memberPointsQueue() {
        return new Queue(MEMBER_POINTS_QUEUE, true); // true = 持久化
    }

    // 【MQ-01-绑定】orderPaidBinding - 绑定订单支付队列到交换机
    // 作用：交换机收到 Routing Key = "order.paid" 的消息，路由到此队列
    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue())
                .to(orderExchange())
                .with(ORDER_PAID_ROUTING_KEY); // Routing Key = "order.paid"
    }

    // 【MQ-01-绑定】memberPointsBinding - 绑定会员积分队列到交换机
    @Bean
    public Binding memberPointsBinding() {
        return BindingBuilder.bind(memberPointsQueue())
                .to(orderExchange())
                .with(MEMBER_POINTS_ROUTING_KEY); // Routing Key = "member.points"
    }

    // 【MQ-01-消息转换器】JSON消息转换器
    // 作用：将Java对象序列化为JSON发送，接收时反序列化为Java对象
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 【MQ-01-生产者】RabbitTemplate - 消息发送模板
    // 作用：发送消息到RabbitMQ
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter()); // 设置JSON转换器
        return template;
    }

    // 【MQ-01-消费者配置】消费者容器工厂
    // 面试考点：并发消费者配置？
    // 答：concurrentConsumers=3：初始3个线程
    //    maxConcurrentConsumers=10：最多10个线程
    //    prefetchCount=1：一次只取1条消息，实现负载均衡
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);           // 连接工厂
        factory.setMessageConverter(jsonMessageConverter());        // JSON转换器
        factory.setConcurrentConsumers(3);                          // 初始并发数：3
        factory.setMaxConcurrentConsumers(10);                      // 最大并发数：10
        factory.setPrefetchCount(1);                                // 预取数量：1
        return factory;
    }
}
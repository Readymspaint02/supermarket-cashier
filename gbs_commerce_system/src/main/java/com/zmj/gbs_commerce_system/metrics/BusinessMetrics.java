package com.zmj.gbs_commerce_system.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter orderCreateCounter;
    private final Counter orderSuccessCounter;
    private final Counter orderFailCounter;

    private final Counter loginSuccessCounter;
    private final Counter loginFailCounter;
    private final Counter faceLoginSuccessCounter;
    private final Counter faceLoginFailCounter;

    private final Counter asrCallCounter;
    private final Counter asrSuccessCounter;
    private final Counter asrFailCounter;

    private final Counter agentQueryCounter;

    private final Counter paymentSuccessCounter;
    private final Counter paymentFailCounter;

    private final ConcurrentHashMap<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.orderCreateCounter = Counter.builder("business_order_created")
                .description("订单创建总数")
                .tag("type", "create")
                .register(meterRegistry);

        this.orderSuccessCounter = Counter.builder("business_order_success")
                .description("订单成功数")
                .tag("type", "success")
                .register(meterRegistry);

        this.orderFailCounter = Counter.builder("business_order_fail")
                .description("订单失败数")
                .tag("type", "fail")
                .register(meterRegistry);

        this.loginSuccessCounter = Counter.builder("business_login_success")
                .description("登录成功次数")
                .tag("type", "password")
                .register(meterRegistry);

        this.loginFailCounter = Counter.builder("business_login_fail")
                .description("登录失败次数")
                .tag("type", "password")
                .register(meterRegistry);

        this.faceLoginSuccessCounter = Counter.builder("business_face_login_success")
                .description("人脸登录成功次数")
                .tag("type", "face")
                .register(meterRegistry);

        this.faceLoginFailCounter = Counter.builder("business_face_login_fail")
                .description("人脸登录失败次数")
                .tag("type", "face")
                .register(meterRegistry);

        this.asrCallCounter = Counter.builder("business_asr_call")
                .description("语音识别调用次数")
                .tag("type", "call")
                .register(meterRegistry);

        this.asrSuccessCounter = Counter.builder("business_asr_success")
                .description("语音识别成功次数")
                .tag("type", "success")
                .register(meterRegistry);

        this.asrFailCounter = Counter.builder("business_asr_fail")
                .description("语音识别失败次数")
                .tag("type", "fail")
                .register(meterRegistry);

        this.agentQueryCounter = Counter.builder("business_agent_query")
                .description("AI助手查询次数")
                .tag("type", "query")
                .register(meterRegistry);

        this.paymentSuccessCounter = Counter.builder("business_payment_success")
                .description("支付成功次数")
                .tag("type", "success")
                .register(meterRegistry);

        this.paymentFailCounter = Counter.builder("business_payment_fail")
                .description("支付失败次数")
                .tag("type", "fail")
                .register(meterRegistry);

        Gauge.builder("business_order_amount_total", () -> getGaugeValue("order_amount"))
                .description("订单总金额")
                .register(meterRegistry);

        Gauge.builder("business_cache_hit_rate", () -> getGaugeValue("cache_hit_rate"))
                .description("缓存命中率")
                .register(meterRegistry);

        Gauge.builder("business_active_users", () -> getGaugeValue("active_users"))
                .description("活跃用户数")
                .register(meterRegistry);
    }

    private AtomicLong getOrCreateGauge(String name) {
        return gaugeValues.computeIfAbsent(name, k -> {
            AtomicLong value = new AtomicLong(0);
            return value;
        });
    }

    private double getGaugeValue(String name) {
        AtomicLong value = gaugeValues.get(name);
        return value != null ? value.doubleValue() : 0.0;
    }

    public void incrementOrderCreate() {
        orderCreateCounter.increment();
    }

    public void incrementOrderSuccess() {
        orderSuccessCounter.increment();
    }

    public void incrementOrderFail() {
        orderFailCounter.increment();
    }

    public void addOrderAmount(double amount) {
        AtomicLong total = getOrCreateGauge("order_amount");
        total.addAndGet((long) (amount * 100));
    }

    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void incrementLoginFail() {
        loginFailCounter.increment();
    }

    public void incrementFaceLoginSuccess() {
        faceLoginSuccessCounter.increment();
    }

    public void incrementFaceLoginFail() {
        faceLoginFailCounter.increment();
    }

    public void incrementAsrCall() {
        asrCallCounter.increment();
    }

    public void incrementAsrSuccess() {
        asrSuccessCounter.increment();
    }

    public void incrementAsrFail() {
        asrFailCounter.increment();
    }

    public void incrementAgentQuery() {
        agentQueryCounter.increment();
    }

    public void incrementPaymentSuccess() {
        paymentSuccessCounter.increment();
    }

    public void incrementPaymentFail() {
        paymentFailCounter.increment();
    }

    public void setCacheHitRate(double rate) {
        AtomicLong value = getOrCreateGauge("cache_hit_rate");
        value.set((long) (rate * 100));
    }

    public void setActiveUsers(long count) {
        AtomicLong value = getOrCreateGauge("active_users");
        value.set(count);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, String metricName) {
        sample.stop(meterRegistry.timer(metricName));
    }
}
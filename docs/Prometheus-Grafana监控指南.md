# Prometheus + Grafana 监控系统完全指南

> 本文档基于智慧超市收银系统实战，结合面试教练深度分析，涵盖概念、原理、实战、面试考点。

---

## 一、概念解析：它们是什么？

### 1.1 Prometheus 是什么？

**一句话定义**：Prometheus 是一款开源的**时序数据库 + 监控系统**，专门用于采集和存储指标数据。

**核心特性**：

| 特性 | 说明 |
|------|------|
| **Pull 模型** | Prometheus 主动拉取指标，而非应用推送（避免推送风暴） |
| **时序数据库** | 专门存储带时间戳的数据，查询效率极高 |
| **多维数据模型** | 指标名 + 标签（Label）组合 |
| **PromQL** | 强大的查询语言，支持聚合、分位数、rate计算 |
| **告警支持** | Alertmanager 支持邮件、钉钉、企业微信告警 |

### 1.2 Grafana 是什么？

**一句话定义**：Grafana 是一款开源的**可视化平台**，用于将 Prometheus 等数据源的数据以图表形式展示。

**核心特性**：
- 多数据源支持（Prometheus、MySQL、Elasticsearch、InfluxDB等）
- 丰富的可视化组件（折线图、仪表盘、热力图等）
- 告警规则配置
- Dashboard 模板导入/导出

---

## 二、为什么需要监控？（面试必问）

### 2.1 没有监控的痛点

```
场景：用户反馈"系统很慢"

❌ 没有监控：
开发：我本地测试挺快的啊，你网络是不是不好？
运维：服务器 CPU 30%，应该没问题
用户：......（丢失信任）

✅ 有监控：
开发：查 Prometheus 面板，发现订单查询接口 P99 耗时 800ms
     定位 SQL 慢查询，添加索引后 P99 降至 50ms
     监控面板验证效果，告用户"已优化"
```

### 2.2 监控的核心价值（降维打击）

| 维度 | 普通回答 | 降维打击回答 |
|------|----------|--------------|
| **发现问题** | 用户反馈系统慢 | 通过监控面板主动发现P99异常，在用户感知前修复 |
| **定位问题** | 看日志排查 | 通过链路追踪+指标关联，3分钟定位根因 |
| **验证效果** | 本地测试通过 | 线上监控面板验证，P99从800ms降至50ms |
| **成本优化** | 无 | 发现Redis内存浪费，调整TTL后节省30%内存 |

---

## 三、监控指标分类

### 3.1 四大黄金信号（Google SRE）

| 指标 | 说明 | 监控方法 |
|------|------|----------|
| **Latency（延迟）** | 请求响应时间 | P50/P95/P99 分位数 |
| **Traffic（流量）** | 系统吞吐量 | QPS（每秒请求数） |
| **Errors（错误）** | 请求失败率 | 4xx/5xx 错误比例 |
| **Saturation（饱和度）** | 资源使用率 | CPU、内存、磁盘、连接数 |

### 3.2 指标类型（Prometheus 四种类型）

| 类型 | 说明 | 示例 |
|------|------|------|
| **Counter（计数器）** | 只增不减的累计值 | 订单总数、请求总数 |
| **Gauge（仪表）** | 可增可减的瞬时值 | 当前内存、活跃连接数 |
| **Histogram（直方图）** | 分布统计 | 响应时间分布（P50/P95/P99） |
| **Summary（摘要）** | 分位数统计 | 类似 Histogram，服务端计算 |

---

## 四、项目实战：智慧超市收银系统监控

### 4.1 技术架构

```
Spring Boot 应用 (8081)
├── BusinessMetrics (自定义埋点)
│   ├── 订单创建数 Counter
│   ├── 订单金额 Gauge
│   ├── 登录成功数 Counter
│   └── 语音识别调用数 Counter
├── Micrometer (指标采集)
│   ├── JVM 内存/CPU/线程
│   ├── HTTP 请求 QPS/延迟
│   └── 数据库连接池
└── /actuator/prometheus
        │
        ▼
Prometheus (9090)
├── 15秒抓取一次指标
├── 存储到时序数据库
└── PromQL 查询
        │
        ▼
Grafana (3000)
├── Dashboard: 智慧超市监控面板
└── 告警规则: P99>500ms 告警
```

### 4.2 业务埋点详解

#### 4.2.1 自定义业务指标（BusinessMetrics.java）

```java
@Component
public class BusinessMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter orderCreateCounter;
    private final AtomicLong orderAmount = new AtomicLong(0);
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 注册 Counter
        this.orderCreateCounter = Counter.builder("business_order_created")
            .description("订单创建总数")
            .tag("type", "create")
            .register(meterRegistry);
        
        // 注册 Gauge
        Gauge.builder("business_order_amount_total", orderAmount, AtomicLong::get)
            .description("订单总金额")
            .register(meterRegistry);
    }
    
    public void incrementOrderCreate() {
        orderCreateCounter.increment();
    }
    
    public void addOrderAmount(double amount) {
        orderAmount.addAndGet((long)(amount * 100));
    }
}
```

#### 4.2.2 埋点位置

| 业务场景 | 埋点位置 | 指标名 |
|----------|----------|--------|
| 订单创建 | OrderController.checkout() | `business_order_created_total` |
| 登录成功 | AuthController.login() | `business_login_success_total` |
| 人脸登录 | AuthController.faceLogin() | `business_face_login_success_total` |
| 语音识别 | AsrController.recognize() | `business_asr_call_total` |

### 4.3 配置详解

#### application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: gbs_commerce_system
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
```

#### prometheus.yml

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'gbs_commerce_system'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']
```

---

## 五、使用指南

### 5.1 快速启动（Docker）

```bash
# 启动 Prometheus
docker run -d --name prometheus -p 9090:9090 \
  -v ./prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

# 启动 Grafana
docker run -d --name grafana -p 3000:3000 \
  grafana/grafana
```

### 5.2 Grafana 配置步骤

1. **添加数据源**：URL = `http://prometheus:9090`
2. **导入 Dashboard**：Upload JSON file → 选择 `grafana-dashboard.json`
3. **配置告警**：Alert Rules → P99 > 500ms 触发告警

### 5.3 常用 PromQL 查询

```promql
# 订单创建速率（每分钟）
rate(business_order_created_total[1m]) * 60

# P95 响应时间
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM 堆内存使用
jvm_memory_used_bytes{area="heap"}
```

---

## 六、监控方案对比（2026年市场分析）

### 6.1 主流监控产品对比

| 产品 | 类型 | 优点 | 缺点 | 适用场景 | 2026竞争力 |
|------|------|------|------|----------|------------|
| **Prometheus + Grafana** | 开源 | 生态丰富、社区活跃、成本低 | 需自建、配置复杂 | 中小企业、云原生 | ⭐⭐⭐⭐⭐ |
| **Datadog** | 商业 | 功能全面、一键部署 | 价格昂贵（$15/主机/月） | 大型企业 | ⭐⭐⭐⭐ |
| **阿里云 ARMS** | 商业 | 与阿里云集成、免部署 | 仅限阿里云、贵 | 阿里云用户 | ⭐⭐⭐ |
| **SkyWalking** | 开源 | 链路追踪强 | 存储压力大 | 微服务架构 | ⭐⭐⭐⭐ |
| **Zabbix** | 开源 | 老牌、功能全 | 界面老旧 | 传统运维 | ⭐⭐⭐ |
| **VictoriaMetrics** | 开源 | 兼容Prometheus、性能强 | 生态不如Prometheus | 大规模监控 | ⭐⭐⭐⭐ |

### 6.2 2026年 AI 冲击下的竞争力分析

| 变化 | 说明 |
|------|------|
| **异常检测智能化** | 不再依赖固定阈值，AI 学习正常模式自动发现异常 |
| **根因分析自动化** | AI 关联多维数据，自动生成排查报告 |
| **预测性运维** | 基于历史数据预测资源瓶颈，提前扩容 |
| **成本优化** | AI 分析资源使用，给出降本建议 |

**Prometheus + Grafana 应对策略**：
- Grafana Cloud 已集成 AI 功能
- 可对接 LLM 进行智能分析
- 开源生态优势：社区快速迭代

---

## 七、面试考点（毒舌拷打）

### 7.1 基础层

**Q1：你们项目用了什么监控方案？**

```
❌ 普通回答：
我们用了 Prometheus + Grafana 监控系统。

✅ 降维打击回答：
我们项目采用了 Prometheus + Grafana 监控方案。
- Prometheus 负责采集 Spring Boot 应用的 Micrometer 指标
- 我们做了业务埋点，比如订单创建数、登录成功数等
- Grafana 上配置了 P99 延迟告警，超过 500ms 会触发
- 通过监控面板，我们发现过订单查询接口慢查询，优化后 P99 从 800ms 降到 50ms
```

### 7.2 进阶层

**Q2：你们监控了哪些指标？为什么要监控 P99？**

```
回答：
我们监控了四类指标：

1. 系统指标：JVM 内存、CPU、GC 次数（自动采集）
2. 接口指标：QPS、P50/P95/P99 延迟、错误率（Micrometer 自动）
3. 业务指标：订单创建数、登录成功数、语音识别调用数（自定义埋点）
4. 中间件指标：Redis 缓存命中率、MySQL 连接数（Exporter）

为什么监控 P99：
- P99 代表 99% 用户的体验
- P99 高说明有 1% 的用户体验很差
- 对于金融、交易场景，P99 比 P50 更重要
```

### 7.3 毒舌拷打层（极端场景）

**Q3：你们设置了告警，那如果 Prometheus 挂了怎么办？**

```
❌ 回答：呃... 没想过...

✅ 回答：
这是个好问题。我们有两层保障：

1. Prometheus 高可用方案：
   - 部署两个 Prometheus 实例，互为主备
   - 或者用 Thanos 实现联邦集群

2. 基础监控兜底：
   - 使用云厂商的监控（阿里云 ARMS、AWS CloudWatch）
   - 服务器 CPU/内存 基础告警不依赖 Prometheus
```

**Q4：你说监控了订单数，那数据量和 Prometheus 内存占用怎么权衡？**

```
回答：
这涉及到 Prometheus 的成本优化：

1. 控制标签维度：
   - 不要用高基数标签（如 userId、orderId）
   - 我们只保留 type、status 等低基数标签

2. 合理设置采集间隔：
   - 默认 15s，非核心服务可以设为 60s

3. 数据保留策略：
   - 本地存储保留 15 天
   - 长期存储用 Thanos 或 VictoriaMetrics
```

---

## 八、最佳实践

### 8.1 命名规范

```promql
# 推荐格式：<namespace>_<name>_<unit>
business_order_created_total
business_order_amount_total
jvm_memory_used_bytes
http_server_requests_seconds
```

### 8.2 标签设计原则

```java
// ❌ 错误：高基数标签（userId 可能百万级）
Counter.builder("api_requests")
    .tag("userId", userId)  // 爆炸！
    .register(meterRegistry);

// ✅ 正确：低基数标签
Counter.builder("api_requests")
    .tag("method", "GET")
    .tag("status", "200")
    .tag("endpoint", "/api/orders")
    .register(meterRegistry);
```

---

## 九、面试加分项总结

1. **有监控思维**：不是等用户反馈，而是主动发现问题
2. **能讲数据来源**：不是"本地测试"，而是"监控面板显示"
3. **懂成本优化**：考虑内存、标签基数、采集频率
4. **会极端场景**：监控挂了怎么办、数据量大了怎么办

---

## 附录

### A. 指标端点

- Prometheus 指标：`http://localhost:8081/api/actuator/prometheus`
- 健康检查：`http://localhost:8081/api/actuator/health`

### B. 文件清单

| 文件 | 说明 |
|------|------|
| `prometheus.yml` | Prometheus 配置文件 |
| `grafana-dashboard.json` | Grafana Dashboard 模板 |
| `BusinessMetrics.java` | 业务指标埋点类 |

### C. 参考链接

- Prometheus 官网：https://prometheus.io
- Grafana 官网：https://grafana.com
- Micrometer 文档：https://micrometer.io
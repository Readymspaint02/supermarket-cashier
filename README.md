# 智慧超市多模态智能体副屏

基于 Spring Boot 3 + Vue 3 的超市收银副屏 AI 助手系统，支持语音/人脸交互。

## 项目介绍

本项目是一款超市收银副屏 AI 助手，通过语音识别和人脸识别替代传统收银流程，解决会员核验、订单播报等场景效率问题。

**核心亮点：**
- 多模态交互（语音 + 人脸）
- 异步解耦（RabbitMQ）
- 缓存优化（Redis）
- 无状态认证（Shiro + JWT）
- 分布式锁防超卖（Redisson）

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|---|---|---|
| Spring Boot | 3.4.3 | 基础框架 |
| MyBatis-Plus | 3.5.7 | ORM框架 |
| Shiro | 1.13.0 | 权限认证 |
| JWT | 0.11.5 | Token方案 |
| Redis | - | 缓存、分布式锁 |
| RabbitMQ | - | 消息队列 |
| Redisson | 3.27.2 | 分布式锁 |

### 前端
| 技术 | 说明 |
|---|---|
| Vue 3 | 前端框架 |
| Element Plus | UI组件库 |
| Axios | HTTP请求 |

### AI服务
| 服务 | 功能 |
|---|---|
| 华为云 ASR | 语音转文字 |
| 百度人脸识别 | 刷脸支付 |
| DeepSeek | AI对话 |

## 核心功能

### 1. 认证授权（Shiro + JWT）
- 无状态Token认证
- 权限粒度到按钮级别
- Token有效期2小时

### 2. 订单管理
- 事务控制保证数据一致性
- 分布式锁 + 乐观锁防超卖
- 订单号生成（时间戳 + 随机数）

### 3. 异步处理（RabbitMQ）
- 订单支付后异步处理会员积分
- 响应时间从 2000ms 降至 200ms

### 4. 缓存优化（Redis）
- Cache-Aside 模式
- TTL 随机化防雪崩
- 分布式锁防击穿
- 布隆过滤器防穿透

### 5. AI集成
- 华为 ASR 语音识别
- 百度人脸识别刷脸支付

## 项目结构

```
supermarket-cashier/
├── gbs_commerce_system/          # 后端项目
│   ├── src/main/java/
│   │   └── com/zmj/gbs_commerce_system/
│   │       ├── config/           # 配置类
│   │       ├── controller/       # 控制器
│   │       ├── service/          # 业务层
│   │       ├── mapper/           # MyBatis Mapper
│   │       ├── entity/           # 实体类
│   │       └── utils/            # 工具类
│   └── src/test/                 # 单元测试
│
├── gbs_commerce_system_ui/       # 前端项目
│   └── src/
│       ├── views/                # 页面组件
│       ├── components/           # 公共组件
│       └── api/                  # API接口
│
└── docker-compose.yml            # Docker编排
```

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.12+

### 后端启动

```bash
git clone https://github.com/Readymspaint02/ArtPick-.git
cd supermarket-cashier/gbs_commerce_system
mvn clean package -DskipTests
java -jar target/gbs_commerce_system-0.0.1-SNAPSHOT.jar
```

### 前端启动

```bash
cd gbs_commerce_system_ui
npm install
npm run dev
```

### Docker部署

```bash
cp .env.example .env
docker-compose up -d --build
```

## 在线体验

- 演示地址：https://ryo.jufu.vip
- GitHub：https://github.com/Readymspaint02/ArtPick-

## 单元测试

```bash
cd gbs_commerce_system
mvn test
```

**测试覆盖：**
- 订单号生成验证
- 商品下架校验
- 库存不足校验
- 库存扣减成功/失败
- 乐观锁冲突测试
- 订单金额计算
- 积分计算

## 面试考点

| 模块 | 常见问题 |
|---|---|
| Shiro + JWT | Token过期处理？为什么要查数据库？ |
| Redis | 缓存穿透/击穿/雪崩？分布式锁实现？ |
| RabbitMQ | 消息丢失怎么办？消息积压处理？ |
| 事务 | @Transactional什么时候失效？如何防超卖？ |

## 作者

**朱铭杰**
- 邮箱：3500498546@qq.com
- 个人博客：https://iloveartwork.com

## License

MIT License
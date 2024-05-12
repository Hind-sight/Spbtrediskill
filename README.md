# spike-system
秒杀系统
# SpringBoot+MySql+Redis+RabbitMQ+Jmeter秒杀系统
技术栈：SpringBoot, MySql, Redis, RabbitMQ,Jmeter,Docekr

# Guide

项目参考自  
[入门基础必备，使用Idea实现SpringBoot+Mysql+Redis+RabbitMQ+Jmeter模拟实现高并发秒杀](https://blog.csdn.net/weixin_44951037/article/details/103672888)

# 服务端环境搭建

本项目服务端在华为云服务器上进行部署，Linux系统环境中，基于Docker进行MySql,Redis和RabbitMQ相关容器部署。

## 操作系统

Ubuntu 20.04

## 安全组配置

开放端口

mysql: 3306

redis: 6379	

rabbitmq: 5672 15672	#5672为rabbitmq服务端端口，15672为rabbitmq提供网页端端口。	

## Docker容器安装

容器部署安装参考文档

[springboot+redis+rabbitmq实现模拟秒杀系统(附带docker安装mysql，rabbitmq，redis教程)-阿里云开发者社区 (aliyun.com)](https://developer.aliyun.com/article/877571)

### MySQL工具安装

```shell
docker pull mysql    # 下载mysql镜像
docker images        # 查看下载的镜像
docker run --name mysql -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root 容器ID  # 使用mysql镜像制作容器并运行
# 镜像ID为IMAGE ID字段对应的值，-e MYSQL_ROOT_PASSWORD=数据库密码
docker ps -a         # 查看运行中的容器
```

容器启动后，使用本地的Navicat连接服务器Docker中的MySql

### Redis工具安装

```shell
docker pull redis   # 下载redis镜像
docker run --name myredis -d -p 6379:6379 容器ID   # 使用redis镜像制作容器并运行
docker ps -a         # 查看运行中的容器
```

容器启动后，使用本地的RedisPlus连接服务器Docker中的Redis

### RabbitMQ工具安装

```shell
docker pull rabbitmq   # 下载rabbitmq镜像
docker run -d --name rabbit -p 15672:15672 -p 5672:5672 容器ID    # 使用rabbitmq镜像制作容器并运行
docker exec -it 容器ID   /bin/bash      # 进入rabbitmq容器
rabbitmq-plugins enable rabbitmq_management   # 安装rabbitmq可视化网页管理平台
#  通过ip:15672访问 登录：用户名密码都为guest
```

容器启动后，通过云服务器公网ip:15672访问

# 应用层接口实现

### Controller
/put  : 上架 "watch"商品10个
```java
@RequestMapping("/put")
    	String put(@RequestParam String orderName, @RequestParam Long count)
```
/sec  : 秒杀购买商品  
```java
    @RequestMapping("/sec")  
    	String sec(String userName, String orderName)
```
### 项目结构
			│  MainSystemApplication.java
			│
			├─config
			│      MyRabbitMQConfig.java
			│      RedisConfig.java
			│
			├─controller
			│      SecController.java
			│
			├─pojo
			│      Stock.java
			│      Order.java
			│
			├─mapper
			│      OrderMapper.java
			│      StockMapper.java
			│
			└─service│
	                 ├─impl
	                 │       OrderServiceImpl
	                 │       StockServiceImpl
	                 │
	                 ├─MQOrderService.java
	                 ├─MQStockService.java
	                 ├─RedisService.java
	                 ├─OrderService.java
	                 └─StockService.java

### 说明
Stock代表库存，Order代表订单。当一个购买行为发生时，应用会先查询购买物品的库存是否足够，如果足够，则将库存减一，并生成一个订单数据到数据库保存，最后返回购买成功的消息给用户

### START
首先利用idea创建springboot项目时勾选\web\redis\RabbitMQ\MySql\，pom依赖如下  

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.8.1</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt</artifactId>
            <version>0.7.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
        <dependency>
            <groupId>tk.mybatis</groupId>
            <artifactId>mapper-spring-boot-starter</artifactId>
            <version>2.0.3-beta1</version>
        </dependency>
        <dependency>
            <groupId>tk.mybatis</groupId>
            <artifactId>mapper</artifactId>
            <version>4.0.0</version>
        </dependency>
    </dependencies>

首先要创建两个实体类到pojo包下
然后创建config包，配置RabbitMQ和Redis.
    RabbitMQ这里涉及到交换机、队列、路由键的概念，需要搜索RabbitMQ基础教程了解一下
    ---
    Redis的配置主要是序列化的配置，应该不配置也可以的。但是配置后查看的时候更美观，而且性能会更好更简洁。
然后到Service层，逻辑是
    RedisService提供增加库存、查询库存服务，增加库存时需要调用StockService库存到数据库。
    ---
    MQOrderService用于消费队列中的订单消息，创建新订单保存到数据库。只需要使用@RabbitMQListener即可实现监听
同理，监听库存消息并修改数据库值
最后一个类，controller，提供接口。购买逻辑是直接调用redisService提供的方法，实现操作 库存-1，返回该结果，如果结果>=0说明库存充足，发送创建新订单和库存-1消息给RabbitMQ，然后直接返回购买成功的结果给用户即可；如果库存不足，直接返回购买失败即可。使用RabbitMQ的好处是，只需要关心是否有库存，然后简单的发送消息之后就不用再管了，同时做到了削峰的好处
最后可以运行起来，先put一个商品进去，再利用Jmeter进行压力测试，Jmeter的使用可以参考本文参考的文章。

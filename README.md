# spike-system
秒杀系统
# SpringBoot+JPA+MySql+Redis秒杀系统
技术栈：SpringBoot, MySql, Redis, RabbitMQ, JPA,(lombok)
## Controller
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
## Guide
项目参考自  
[入门基础必备，使用Idea实现SpringBoot+Mysql+Redis+RabbitMQ+Jmeter模拟实现高并发秒杀](https://blog.csdn.net/weixin_44951037/article/details/103672888)  
简化了原项目，将tkmybatis替换成JPA。并将一些比较复杂的操作尽可能简化。

### 项目结构
			│  MainSystemApplication.java
			│
			├─config
			│      MyRabbitConfig.java
			│      MyRedisConfig.java
			│
			├─controller
			│      Test.java
			│
			├─dao
			│      StockRe.java
			│      TOrderRe.java
			│
			├─domain
			│      Stock.java
			│      TOrder.java
			│
			└─service
					MQOrderService.java
					MQStockService.java
					RedisService.java

### 说明
stock代表库存，TORder代表订单。当一个购买行为发生时，应用会先查询购买物品的库存是否足够，如果足够，则将库存减一，并生成一个订单数据到数据库保存，最后返回购买成功的消息给用户

### START
首先利用idea创建springboot项目时勾选lombok\web\redis\RabbitMQ\MySql\JPA，生成的pom依赖如下  

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
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
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
    </dependencies>

首先要创建两个实体类到domain包下  

	package com.chunmiao.mainsystem.domain;

	import lombok.Data;
	
	import javax.persistence.Entity;
	import javax.persistence.GeneratedValue;
	import javax.persistence.Id;
	import java.io.Serializable;
	
	/**
	 * 库存实体类
	 */
	@Data
	@Entity
	public class Stock implements Serializable {
	    @Id
	    @GeneratedValue
	    private Long id;
	
	    private String name;
	
	    //货品库存数量
	    private Long stock;
	
	}

----

	package com.chunmiao.mainsystem.domain;
	
	import lombok.AllArgsConstructor;
	import lombok.Builder;
	import lombok.Data;
	import lombok.NoArgsConstructor;
	
	import javax.persistence.Entity;
	import javax.persistence.GeneratedValue;
	import javax.persistence.Id;
	import java.io.Serializable;
	
	/**
	 * 订单实体类
	 */
	@Data
	@Entity
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public class TOrder implements Serializable{
	    @Id
	    @GeneratedValue
	    private Long id;
	
	    private String orderName;
	
	    private String orderUser;
	
	}

---
然后创建jpa的Repository到dao包下。
jpa实现crud的操作只需要继承JPARepository接口即可自动提供find\sava\delete实现类给用户使用。  

	package com.chunmiao.mainsystem.dao;
	
	import com.chunmiao.mainsystem.domain.TOrder;
	import org.springframework.data.jpa.repository.JpaRepository;
	import org.springframework.stereotype.Repository;
	
	@Repository
	public interface TOrderRe extends JpaRepository<TOrder,Long> {
	}
---
由于JPA默认只提供findById的操作，如果想通过别的字段查询需要自己提供接口，当然，JPA也会为这个接口自动提供实现类  

	package com.chunmiao.mainsystem.dao;
	
	import com.chunmiao.mainsystem.domain.Stock;
	import org.springframework.data.jpa.repository.JpaRepository;
	import org.springframework.stereotype.Repository;
	
	@Repository
	public interface StockRe extends JpaRepository<Stock,Long> {
	
	    Stock findByName(String name);
	
	}



然后创建config包，配置RabbitMQ和Redis.
RabbitMQ这里涉及到交换机、队列、路由键的概念，需要搜索RabbitMQ基础教程了解一下  
  
	package com.chunmiao.mainsystem.config;
	
	import org.springframework.amqp.core.*;
	import org.springframework.context.annotation.Bean;
	import org.springframework.context.annotation.Configuration;
	
	@Configuration
	public class MyRabbitConfig {
	
	    public final static String TORDER_EXCHANG = "TORDER_EXCHANG";
	
	    public final static String TORDER_QUEUE = "TORDER_QUEUE";
	
	    public final static String TORDER_ROUTING_KEY = "TORDER_ROUTING_KEY";
	
	    public final static String STOCK_EXCHANG = "STOCK_EXCHANG";
	
	    public final static String STOCK_QUEUE = "STOCK_QUEUE";
	
	    public final static String STOCK_ROUTING_KEY = "STOCK_ROUTING_KEY";
	
	    /**
	     * 订单消息
	     * 1.创建交换机
	     * 2.创建队列
	     * 3.通过路由键绑定交换机和队列
	     */
	    @Bean
	    public Exchange getTOrderExchang() {
	        return ExchangeBuilder.directExchange(TORDER_EXCHANG).build();
	    }
	
	    @Bean
	    public Queue getTOrderQueue() {
	        return QueueBuilder.nonDurable(TORDER_QUEUE).build();
	    }
	
	    @Bean
	    public Binding bindTOrder() {
	        return BindingBuilder.bind(getTOrderQueue()).to(getTOrderExchang()).with(TORDER_ROUTING_KEY).noargs();
	    }
	
	    /**
	     * 库存消息
	     * 1.创建交换机
	     * 2.创建队列
	     * 3.通过路由键绑定交换机和队列
	     */
	    @Bean
	    public Exchange getStockExchange() {
	        return ExchangeBuilder.directExchange(STOCK_EXCHANG).build();
	    }
	
	    @Bean
	    public Queue getStockQueue() {
	        return QueueBuilder.nonDurable(STOCK_QUEUE).build();
	    }
	
	    @Bean
	    public Binding bindStock() {
	        return BindingBuilder.bind(getStockQueue()).to(getStockExchange()).with(STOCK_ROUTING_KEY).noargs();
	    }
	
	}

---
Redis的配置主要是序列化的配置，应该不配置也可以的。但是配置后查看的时候更美观，而且性能会更好更简洁。  

	package com.chunmiao.mainsystem.config;
	
	import org.springframework.context.annotation.Bean;
	import org.springframework.context.annotation.Configuration;
	import org.springframework.data.redis.connection.RedisConnectionFactory;
	import org.springframework.data.redis.core.RedisTemplate;
	import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
	import org.springframework.data.redis.serializer.StringRedisSerializer;
	
	@Configuration
	public class MyRedisConfig {
	
	    @Bean
	    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
	        RedisTemplate<String, Object> re = new RedisTemplate<>();
	        re.setConnectionFactory(redisConnectionFactory);
	        re.setKeySerializer(new StringRedisSerializer());
	        re.setValueSerializer(new Jackson2JsonRedisSerializer<>(Long.class));   // 不能用generic的Serializer，有存Long取Integer的bug
	        re.afterPropertiesSet();
	        return re;
	    }
	
	
	}


---

然后到Service层，逻辑是RedisService提供增加库存、查询库存服务，增加库存时需要调用StockRe保存增加库存到数据库。  

	package com.chunmiao.mainsystem.service;
	
	import com.chunmiao.mainsystem.dao.StockRe;
	import com.chunmiao.mainsystem.domain.Stock;
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.data.redis.core.BoundValueOperations;
	import org.springframework.data.redis.core.RedisTemplate;
	import org.springframework.stereotype.Service;
	
	/**
	 * stock信息缓存到redis
	 */
	@Service
	public class RedisService {
	    @Autowired
	    private RedisTemplate<String, Object> redisTemplate;
	    @Autowired
	    private StockRe stockRe;
	
	    public void put(String key, Long value) {
	        BoundValueOperations<String, Object> bp = redisTemplate.boundValueOps(key);
	        Long count = (Long) bp.get();
	        if ( count!= null){
	            count = count >= 0 ? count + value : value;
	        } else count = value;
	        bp.set(count);
	
	        Stock stock = stockRe.findByName(key);
	        if (stock == null) {
	            stock = new Stock();
	            stock.setName(key);
	            stock.setStock(0l);
	        }
	        long l = stock.getStock() + value;
	        stock.setStock(l);
	        stockRe.save(stock);
	    }
		// 返回当前商品库存-1的结果，如果库存小于0时直接返回，这样调用它的类就知道已经没有库存了
	    public Long decrBy(String key) {
	        BoundValueOperations<String, Object> bp = redisTemplate.boundValueOps(key);
	        Long count = (Long) bp.get();
	        if (count == null) return -1l;
	        if (count >= 0) {
	            count--;
	            bp.set(count);
	        }
	        return count;
	    }
	}

---
MQOrderService用于消费队列中的订单消息，创建新订单保存到数据库。只需要使用@RabbitMQListener即可实现监听  

	package com.chunmiao.mainsystem.service;
	
	import com.chunmiao.mainsystem.dao.TOrderRe;
	import com.chunmiao.mainsystem.domain.TOrder;
	import org.springframework.amqp.rabbit.annotation.RabbitListener;
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.stereotype.Service;
	
	import static config.com.spbtrediskill.secondskill.MyRabbitConfig.TORDER_QUEUE;
	import static config.com.spbtrediskill.secondskill.MyRabbitConfig.STOCK_QUEUE;
	
	/**
	 * 从MQ中拿消息，创建一个新订单到数据库
	 */
	@Service
	public class MQOrderService {
	    @Autowired
	    private TOrderRe orderRe;
	
	    @RabbitListener(queues = TORDER_QUEUE)
	    public void saveOrder(TOrder order) {
	        System.out.println("创建新订单");
	        orderRe.save(order);
	    }
	}
---
同理，监听库存消息并修改数据库值  

	package com.chunmiao.mainsystem.service;
	
	import com.chunmiao.mainsystem.dao.StockRe;
	import com.chunmiao.mainsystem.domain.Stock;
	import org.springframework.amqp.rabbit.annotation.RabbitListener;
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.stereotype.Service;
	
	import static config.com.spbtrediskill.secondskill.MyRabbitConfig.STOCK_QUEUE;
	
	/**
	 * 从MQ拿消息，使库存减少一个
	 */
	@Service
	public class MQStockService {
	    @Autowired
	    private StockRe stockRe;
	
	    @RabbitListener(queues = STOCK_QUEUE)
	    public void decrStock(String orderName) {
	        System.out.println("减少数据库的库存");
	        Stock stock = stockRe.findByName(orderName);
	        if (stock!= null) {
	            stock.setStock(stock.getStock() - 1);
	            stockRe.save(stock);
	        }
	    }
	}
---
最后一个类，controller，提供接口。购买逻辑是直接调用redisService提供的方法，实现操作 库存-1，返回该结果，如果结果>=0说明库存充足，发送创建新订单和库存-1消息给RabbitMQ，然后直接返回购买成功的结果给用户即可；如果库存不足，直接返回购买失败即可。使用RabbitMQ的好处是，只需要关心是否有库存，然后简单的发送消息之后就不用再管了，同时做到了削峰的好处  

	package com.chunmiao.mainsystem.controller;
	
	
	import com.chunmiao.mainsystem.domain.TOrder;
	import service.com.spbtrediskill.secondskill.RedisService;
	import org.springframework.amqp.rabbit.core.RabbitTemplate;
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.web.bind.annotation.RequestMapping;
	import org.springframework.web.bind.annotation.RequestParam;
	import org.springframework.web.bind.annotation.RestController;
	
	import static config.com.spbtrediskill.secondskill.MyRabbitConfig.*;
	
	@RestController
	public class Test {
	    @Autowired
	    private RedisService redisService;
	
	    @Autowired
	    private RabbitTemplate rabbitTemplate;
	
	    @RequestMapping("/put")
	    String put(@RequestParam String orderName, @RequestParam Long count) {
	        redisService.put(orderName, count);
	        return "上架商品\n" + orderName + ":" + count;
	    }
	
	    @RequestMapping("/sec")
	    String sec(String userName, String orderName) {
	        String msg = "秒杀用户：" + userName + "\n" + "秒杀商品： " + orderName;
	        System.out.println("\n---------------------------------------------");
	        System.out.println("秒杀用户：" + userName + "\n" + "秒杀商品： " + orderName);
	        Long count = redisService.decrBy(orderName);
	        // 秒杀成功
	        System.out.println("当前商品数量为： " + (count + 1));
	        if (count >= 0) {
	            System.out.println("库存充足");
	            // 创建新订单
	            rabbitTemplate.convertAndSend(TORDER_EXCHANG,TORDER_ROUTING_KEY,
	                    TOrder.builder()
	                            .orderName(orderName)
	                            .orderUser(userName)
	                            .build());
	            // 创建库存-1消息
	            rabbitTemplate.convertAndSend(STOCK_EXCHANG,STOCK_ROUTING_KEY,orderName);
	            System.out.println("秒杀成功");
	            msg +=  "成功";
	        } else {
	            System.out.println("库存不足");
	            msg += "失败";
	        }
	        return msg;
	    }
	}

最后可以运行起来，先put一个商品进去，再利用Jmeter进行压力测试，Jmeter的使用可以参考本文参考的文章。

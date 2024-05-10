package com.spbtrediskill.secondskill;

import com.spbtrediskill.secondskill.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import tk.mybatis.spring.annotation.MapperScan;
@SpringBootApplication
@MapperScan("com.spbtrediskill.secondskill.mapper")
public class SecondskillApplication implements ApplicationRunner{
    public static void main(String[] args) {
        SpringApplication.run(SecondskillApplication.class, args);
    }
    @Autowired
    private RedisService redisService;
    /**
     * redis初始化商品的库存量和信息
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        redisService.put("watch", 10, 20);
    }
}

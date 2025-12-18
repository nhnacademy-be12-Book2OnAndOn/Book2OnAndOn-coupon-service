package com.example.book2onandoncouponservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients
public class Book2onandoncouponserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Book2onandoncouponserviceApplication.class, args);
    }

}

package com.example.book2onandoncouponservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication
@EnableDiscoveryClient
public class Book2onandoncouponserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Book2onandoncouponserviceApplication.class, args);
    }

}

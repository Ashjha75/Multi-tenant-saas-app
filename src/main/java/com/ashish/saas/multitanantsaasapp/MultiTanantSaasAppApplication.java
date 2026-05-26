package com.ashish.saas.multitanantsaasapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MultiTanantSaasAppApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(MultiTanantSaasAppApplication.class, args);
        System.out.println("Project is working fine : 🥳✨");
    }

}

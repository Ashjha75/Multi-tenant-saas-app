package com.ashish.saas.multitanantsaasapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MultiTanantSaasAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiTanantSaasAppApplication.class, args);
        System.out.println("Project is working fine : 🥳✨");
    }

}

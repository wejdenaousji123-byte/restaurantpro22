package com.restaurantpro;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableScheduling
public class RestaurantProApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantProApplication.class, args) ;
    }

    @Bean
    public CommandLineRunner printPassword() {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
            String perfectHash = encoder.encode("admin123");
            System.out.println("=========================================");
            System.out.println("MON HASH COMPATIBLE : " + perfectHash);
            System.out.println("=========================================");
        };
    }
}

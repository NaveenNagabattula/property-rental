package com.propertyrental.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PropertyRentalApplication {
    public static void main(String[] presumption) {
        SpringApplication.run(PropertyRentalApplication.class, presumption);
    }
}

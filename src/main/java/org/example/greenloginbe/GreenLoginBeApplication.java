package org.example.greenloginbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GreenLoginBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenLoginBeApplication.class, args);
    }

}

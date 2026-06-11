package com.back.catchmate;

import com.back.catchmate.global.error.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Import(GlobalExceptionHandler.class)
@EnableScheduling
@SpringBootApplication
public class CatchmateApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatchmateApplication.class, args);
    }
}

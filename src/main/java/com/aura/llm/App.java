package com.aura.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        // This line starts the Tomcat server and your application
        SpringApplication.run(App.class, args);
    }
}
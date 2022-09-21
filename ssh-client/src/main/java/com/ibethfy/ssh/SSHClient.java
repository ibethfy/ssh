package com.ibethfy.ssh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class SSHClient {
    public static void main(String[] args) {
        SpringApplication.run(SSHClient.class);
    }
}

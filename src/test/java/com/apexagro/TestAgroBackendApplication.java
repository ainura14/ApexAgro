package com.apexagro;

import org.springframework.boot.SpringApplication;

public class TestAgroBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(AgroBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

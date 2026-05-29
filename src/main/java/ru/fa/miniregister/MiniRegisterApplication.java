package ru.fa.miniregister;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа приложения MiniRegister.
 * Spring Boot автоматически конфигурирует все компоненты.
 */
@SpringBootApplication
public class MiniRegisterApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniRegisterApplication.class, args);
    }
}

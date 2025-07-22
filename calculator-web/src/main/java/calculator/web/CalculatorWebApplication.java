package calculator.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 计算器Web应用主类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"calculator.web", "calculator.service", "calculator.api"})
public class CalculatorWebApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CalculatorWebApplication.class, args);
    }
} 
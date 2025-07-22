package calculator.service;

import org.springframework.stereotype.Service;
import calculator.api.CalculatorService;
import calculator.api.CalculatorServiceImpl;

/**
 * 高级计算器服务
 * 提供更复杂的计算功能
 */
@Service
public class AdvancedCalculatorService {
    
    private final CalculatorService calculatorService;
    
    public AdvancedCalculatorService() {
        this.calculatorService = new CalculatorServiceImpl();
    }
    
    /**
     * 计算平均值
     */
    public double average(double... numbers) {
        if (numbers.length == 0) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (double number : numbers) {
            sum = calculatorService.add(sum, number);
        }
        
        return calculatorService.divide(sum, numbers.length);
    }
    
    /**
     * 计算平方
     */
    public double square(double x) {
        return calculatorService.multiply(x, x);
    }
    
    /**
     * 计算立方
     */
    public double cube(double x) {
        return calculatorService.multiply(calculatorService.multiply(x, x), x);
    }
    
    /**
     * 计算阶乘
     */
    public double factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("阶乘不能为负数");
        }
        if (n == 0 || n == 1) {
            return 1.0;
        }
        
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result = calculatorService.multiply(result, i);
        }
        return result;
    }
    
    /**
     * 获取基础计算器服务
     */
    public CalculatorService getCalculatorService() {
        return calculatorService;
    }
} 
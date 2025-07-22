package calculator.api;

import calculator.core.Calculator;

/**
 * 计算器服务接口
 */
public interface CalculatorService {
    
    /**
     * 加法运算
     */
    double add(double x, double y);
    
    /**
     * 减法运算
     */
    double subtract(double x, double y);
    
    /**
     * 乘法运算
     */
    double multiply(double x, double y);
    
    /**
     * 除法运算
     */
    double divide(double x, double y);
} 
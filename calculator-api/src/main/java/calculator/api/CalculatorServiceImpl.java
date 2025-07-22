package calculator.api;

import calculator.core.Calculator;

/**
 * 计算器服务实现类
 */
public class CalculatorServiceImpl implements CalculatorService {
    
    @Override
    public double add(double x, double y) {
        return Calculator.add(x, y);
    }
    
    @Override
    public double subtract(double x, double y) {
        return Calculator.subtract(x, y);
    }
    
    @Override
    public double multiply(double x, double y) {
        return Calculator.multiply(x, y);
    }
    
    @Override
    public double divide(double x, double y) {
        return Calculator.divide(x, y);
    }
} 
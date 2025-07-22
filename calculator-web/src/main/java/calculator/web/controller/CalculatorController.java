package calculator.web.controller;

import calculator.service.AdvancedCalculatorService;
import calculator.api.CalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 计算器REST API控制器
 */
@RestController
@RequestMapping("/api/calculator")
public class CalculatorController {
    
    private final AdvancedCalculatorService advancedCalculatorService;
    private final CalculatorService calculatorService;
    
    @Autowired
    public CalculatorController(AdvancedCalculatorService advancedCalculatorService) {
        this.advancedCalculatorService = advancedCalculatorService;
        this.calculatorService = advancedCalculatorService.getCalculatorService();
    }
    
    /**
     * 基础加法运算
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Map<String, Double> request) {
        double x = request.get("x");
        double y = request.get("y");
        double result = calculatorService.add(x, y);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "add");
        response.put("x", x);
        response.put("y", y);
        response.put("result", result);
        return response;
    }
    
    /**
     * 基础减法运算
     */
    @PostMapping("/subtract")
    public Map<String, Object> subtract(@RequestBody Map<String, Double> request) {
        double x = request.get("x");
        double y = request.get("y");
        double result = calculatorService.subtract(x, y);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "subtract");
        response.put("x", x);
        response.put("y", y);
        response.put("result", result);
        return response;
    }
    
    /**
     * 基础乘法运算
     */
    @PostMapping("/multiply")
    public Map<String, Object> multiply(@RequestBody Map<String, Double> request) {
        double x = request.get("x");
        double y = request.get("y");
        double result = calculatorService.multiply(x, y);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "multiply");
        response.put("x", x);
        response.put("y", y);
        response.put("result", result);
        return response;
    }
    
    /**
     * 基础除法运算
     */
    @PostMapping("/divide")
    public Map<String, Object> divide(@RequestBody Map<String, Double> request) {
        double x = request.get("x");
        double y = request.get("y");
        double result = calculatorService.divide(x, y);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "divide");
        response.put("x", x);
        response.put("y", y);
        response.put("result", result);
        return response;
    }
    
    /**
     * 计算平均值
     */
    @PostMapping("/average")
    public Map<String, Object> average(@RequestBody Map<String, double[]> request) {
        double[] numbers = request.get("numbers");
        double result = advancedCalculatorService.average(numbers);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "average");
        response.put("numbers", numbers);
        response.put("result", result);
        return response;
    }
    
    /**
     * 计算平方
     */
    @PostMapping("/square")
    public Map<String, Object> square(@RequestBody Map<String, Double> request) {
        double x = request.get("x");
        double result = advancedCalculatorService.square(x);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "square");
        response.put("x", x);
        response.put("result", result);
        return response;
    }
    
    /**
     * 计算立方
     */
    @PostMapping("/cube")
    public Map<String, Object> cube(@RequestBody Map<String, Double> request) {
        double x = request.get("x");
        double result = advancedCalculatorService.cube(x);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "cube");
        response.put("x", x);
        response.put("result", result);
        return response;
    }
    
    /**
     * 计算阶乘
     */
    @PostMapping("/factorial")
    public Map<String, Object> factorial(@RequestBody Map<String, Integer> request) {
        int n = request.get("n");
        double result = advancedCalculatorService.factorial(n);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "factorial");
        response.put("n", n);
        response.put("result", result);
        return response;
    }
    
    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Calculator service is running");
        return response;
    }
} 
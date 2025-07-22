# 多模块项目测试指南

## 项目结构验证

✅ **已完成的多模块结构：**

```
java-gradle-cicd-example/
├── build.gradle                    # 根项目构建配置
├── settings.gradle                 # 项目设置，定义子模块
├── calculator-core/                # 核心计算模块
│   ├── build.gradle
│   └── src/main/java/calculator/core/
│       └── Calculator.java         # 基础计算器类
├── calculator-api/                 # API接口模块
│   ├── build.gradle
│   └── src/main/java/calculator/api/
│       ├── CalculatorService.java      # 服务接口
│       └── CalculatorServiceImpl.java  # 服务实现
├── calculator-service/             # 服务层模块
│   ├── build.gradle
│   └── src/main/java/calculator/service/
│       └── AdvancedCalculatorService.java  # 高级计算服务
└── calculator-web/                 # Web应用模块
    ├── build.gradle
    └── src/main/java/calculator/web/
        ├── CalculatorWebApplication.java    # Spring Boot主类
        └── controller/
            └── CalculatorController.java    # REST API控制器
```

## 模块依赖关系验证

✅ **依赖关系：**

- calculator-web → calculator-service → calculator-api → calculator-core
- calculator-web → calculator-api (直接依赖，用于类型引用)

## 构建测试

✅ **构建成功：**

```bash
gradle build
```

✅ **测试成功：**

```bash
gradle test
```

## 功能验证

### 1. 基础计算功能

- calculator-core 模块提供基础的四则运算
- 通过 calculator-api 模块暴露服务接口
- calculator-service 模块提供高级计算功能

### 2. Web API功能

- calculator-web 模块提供REST API
- 支持基础运算和高级运算
- 使用Spring Boot框架

## 运行测试

### 启动Web应用

```bash
gradle :calculator-web:bootRun
```

### 测试API端点

应用启动后，可以通过以下端点测试：

1. **健康检查：**
   
   ```
   GET http://localhost:8080/api/calculator/health
   ```

2. **基础运算：**
   
   ```json
   POST http://localhost:8080/api/calculator/add
   {
       "x": 10,
       "y": 5
   }
   ```

3. **高级运算：**
   
   ```json
   POST http://localhost:8080/api/calculator/average
   {
       "numbers": [1, 2, 3, 4, 5]
   }
   ```

## 模块独立性测试

### 单独构建模块

```bash
gradle :calculator-core:build
gradle :calculator-api:build
gradle :calculator-service:build
gradle :calculator-web:build
```

### 模块间依赖验证

- calculator-api 可以独立编译，依赖 calculator-core
- calculator-service 可以独立编译，依赖 calculator-api
- calculator-web 可以独立编译，依赖 calculator-service 和 calculator-api

## 项目特点

✅ **多模块架构：** 清晰的分层结构
✅ **依赖管理：** 使用Gradle的project依赖
✅ **测试覆盖：** 每个模块都有独立的测试
✅ **Spring Boot集成：** Web模块使用Spring Boot
✅ **构建工具：** 使用Gradle进行构建和依赖管理
✅ **代码质量：** 集成JaCoCo代码覆盖率分析 
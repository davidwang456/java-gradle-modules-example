# Java Gradle 多模块项目示例

这是一个基于Gradle的多模块Java项目，展示了模块间的依赖关系。

## 项目结构

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

## 模块依赖关系

```
calculator-web
    ↓ 依赖
calculator-service
    ↓ 依赖
calculator-api
    ↓ 依赖
calculator-core
```

## 构建和运行

### 构建整个项目
```bash
./gradlew build
```

### 构建特定模块
```bash
./gradlew :calculator-core:build
./gradlew :calculator-api:build
./gradlew :calculator-service:build
./gradlew :calculator-web:build
```

### 运行Web应用
```bash
./gradlew :calculator-web:bootRun
```

### 运行测试
```bash
./gradlew test
```

## API端点

启动Web应用后，可以通过以下端点访问计算器功能：

- `GET /api/calculator/health` - 健康检查
- `POST /api/calculator/add` - 加法运算
- `POST /api/calculator/subtract` - 减法运算
- `POST /api/calculator/multiply` - 乘法运算
- `POST /api/calculator/divide` - 除法运算
- `POST /api/calculator/average` - 计算平均值
- `POST /api/calculator/square` - 计算平方
- `POST /api/calculator/cube` - 计算立方
- `POST /api/calculator/factorial` - 计算阶乘

### 示例请求

**加法运算：**
```json
POST /api/calculator/add
{
    "x": 10,
    "y": 5
}
```

**计算平均值：**
```json
POST /api/calculator/average
{
    "numbers": [1, 2, 3, 4, 5]
}
```

## 技术栈

- **构建工具**: Gradle
- **Java版本**: 17
- **Web框架**: Spring Boot 3.2.0
- **测试框架**: JUnit 4
- **代码覆盖率**: JaCoCo

## 模块说明

### calculator-core
核心计算模块，包含基础的数学运算功能。不依赖其他模块。

### calculator-api
API接口模块，定义了计算器服务的接口和基础实现。依赖calculator-core模块。

### calculator-service
服务层模块，提供高级计算功能（如平均值、平方、立方、阶乘等）。依赖calculator-api模块。

### calculator-web
Web应用模块，提供REST API接口。依赖calculator-service模块，使用Spring Boot框架。

## 开发指南

1. 每个模块都有独立的测试
2. 模块间的依赖通过Gradle的project依赖管理
3. 使用JaCoCo进行代码覆盖率分析
4. 支持Maven仓库发布

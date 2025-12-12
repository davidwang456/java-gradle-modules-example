# Gradle 多模块项目私有仓库与发布配置指南

本指南介绍如何通过 Gradle 的 `--init-script` 机制，动态配置私有仓库依赖拉取与发布，适用于多模块项目，安全支持 CI/CD 场景。

---

## 一、动态 private-maven-init.gradle 脚本

将以下内容保存为 `private-maven-init.gradle`，或在 CI 脚本中动态生成：

```groovy
allprojects {
    repositories {
        maven {
            name = "public-proxy"
            url = "https://example.repo.com/repository/public-proxy/"
            credentials {
                username = "${System.getenv('MAVEN_USERNAME')}"
                password = "${System.getenv('MAVEN_PASSWORD')}"
            }
        }
        mavenCentral()
    }
}

subprojects {
    afterEvaluate { project ->
        if (project.plugins.hasPlugin('maven-publish')) {
            publishing {
                repositories {
                    maven {
                        // 判断版本是SNAPSHOT还是RELEASE，写入不同的repo
                        def repoType = project.version.toString().endsWith("SNAPSHOT") ? "snapshots" : "releases"
                        def repoName = "${rootProject.name}-${repoType}"
                        name = repoName
                        url = "https://example.repo.com/repository/${project.name}/"
                        credentials {
                            username = "${System.getenv('MAVEN_USERNAME')}"
                            password = "${System.getenv('MAVEN_PASSWORD')}"
                        }
                    }
                }
            }
        }
    }
}
```

---

## 二、在 GitLab CI 中动态生成脚本

在 `.gitlab-ci.yml` 的每个 job 里添加如下 shell 片段：

```bash
cat <<EOF > private-maven-init.gradle
allprojects {
    repositories {
        maven {
            name = "public-proxy"
            url = "https://example.repo.com/repository/public-proxy/"
            credentials {
                username = "\${MAVEN_USERNAME}"
                password = "\${MAVEN_PASSWORD}"
            }
        }
        mavenCentral()
    }
}

subprojects {
    afterEvaluate { project ->
        if (project.plugins.hasPlugin('maven-publish')) {
            publishing {
                repositories {
                    maven {
                        def repoType = project.version.toString().endsWith("SNAPSHOT") ? "snapshots" : "releases"
                        def repoName = "\${rootProject.name}-\${repoType}"
                        name = repoName
                        url = "https://example.repo.com/repository/\${project.name}/"
                        credentials {
                            username = "\${MAVEN_USERNAME}"
                            password = "\${MAVEN_PASSWORD}"
                        }
                    }
                }
            }
        }
    }
}
EOF
```

---

## 三、CI/CD 调用方式

在所有需要用到私有仓库的 gradle 命令后加上 `--init-script private-maven-init.gradle`，如：

```bash
gradle build --init-script private-maven-init.gradle
gradle publish --init-script private-maven-init.gradle
gradle test jacocoTestReport --init-script private-maven-init.gradle
```

---

## 四、环境变量安全说明

- 请在 GitLab CI/CD 的变量设置中配置 `MAVEN_USERNAME` 和 `MAVEN_PASSWORD`，不要将敏感信息写入源码。
- 该方案不会污染项目源码，适合多环境、临时切换、CI/CD 场景。

---

如需进一步定制或有特殊需求，请联系开发负责人。 

---

## 五、CI中上传 all-jars.zip 到 Nexus 示例

假设参数：
- Nexus 地址：`https://nexus.example.com`
- 仓库名：`my-raw-repo`
- 目标目录：`ci-artifacts/`
- 文件名：`all-jars.zip`
- 用户名/密码：`${NEXUS_USERNAME}` / `${NEXUS_PASSWORD}`（建议用 CI/CD 变量）

### curl 上传命令

```bash
curl -u "${NEXUS_USERNAME}:${NEXUS_PASSWORD}" \
  --upload-file all-jars.zip \
  "https://nexus.example.com/repository/my-raw-repo/ci-artifacts/all-jars.zip"
```

### 在 .gitlab-ci.yml 中的用法

```yaml
upload:
  stage: deploy
  script:
    - echo "=== 上传 all-jars.zip 到 Nexus ==="
    - |
      curl -u "${NEXUS_USERNAME}:${NEXUS_PASSWORD}" \
        --upload-file all-jars.zip \
        "https://nexus.example.com/repository/my-raw-repo/ci-artifacts/all-jars.zip"
```

### 说明
- `-u "${NEXUS_USERNAME}:${NEXUS_PASSWORD}"`：使用 CI/CD 变量传递认证信息，安全可靠。
- `--upload-file all-jars.zip`：指定要上传的文件。
- URL 末尾的 `ci-artifacts/all-jars.zip`：指定上传到仓库的哪个目录和文件名。
- 如果你用的是 maven 类型仓库，URL 结构会不同（通常需要 groupId、artifactId、version 等路径）。

如需适配 maven 仓库或有特殊路径/命名需求，请补充说明！ 

---

## 六、CI中上传 all-jars.zip 到 Maven 类型仓库示例

如果你用的是 Maven 类型仓库，上传路径必须符合 Maven 坐标规则（groupId/artifactId/version/文件名）。

### curl 上传命令

假设参数：
- Nexus 地址：`https://nexus.example.com`
- 仓库名：`maven-releases`
- groupId：`com.example`
- artifactId：`myapp`
- version：`1.0.0`
- 文件名：`myapp-1.0.0.zip`
- 用户名/密码：`${NEXUS_USERNAME}` / `${NEXUS_PASSWORD}`

命令如下：

```bash
curl -u "${NEXUS_USERNAME}:${NEXUS_PASSWORD}" \
  --upload-file all-jars.zip \
  "https://nexus.example.com/repository/maven-releases/com/example/myapp/1.0.0/myapp-1.0.0.zip"
```

### 在 .gitlab-ci.yml 中的用法

```yaml
upload:
  stage: deploy
  script:
    - |
      GROUP=$(grep '^group=' gradle.properties | cut -d'=' -f2 | tr -d '\r' | tr '.' '/')
      ARTIFACT=$(grep '^artifactName=' gradle.properties | cut -d'=' -f2 | tr -d '\r')
      VERSION=$(grep '^artifactVersion=' gradle.properties | cut -d'=' -f2 | tr -d '\r')
      REPO="maven-releases"
      FILE="${ARTIFACT}-${VERSION}.zip"
      mv all-jars.zip "$FILE"
      echo "${REPO} ${GROUP} ${ARTIFACT} ${VERSION} ${FILE}"
      echo "=== 上传 $FILE 到 Nexus Maven 仓库 ==="
      curl -u "${NEXUS_USERNAME}:${NEXUS_PASSWORD}" \
        --upload-file "$FILE" \
        "https://nexus.example.com/repository/${REPO}/${GROUP}/${ARTIFACT}/${VERSION}/${FILE}"
```

### 说明
- groupId 必须用 `/` 分隔
- artifactId、version、文件名要和路径一致
- 这种方式适合上传自定义产物（如 zip），标准 jar/pom 建议用 Gradle 的 publish 任务

如需进一步定制或有特殊需求，请联系开发负责人。 

---

## 七、Maven 指定单元测试目录配置指南

在 Maven 中，默认的单元测试目录是 `src/test/java`。如果你的测试代码不在这个目录，或者你想指定其他测试目录，可以通过以下几种方式实现：

### 1. 推荐做法：在 pom.xml 中配置 build-helper-maven-plugin

在你的 `pom.xml` 中添加如下配置：

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>build-helper-maven-plugin</artifactId>
      <version>3.2.0</version>
      <executions>
        <execution>
          <id>add-test-source</id>
          <phase>generate-test-sources</phase>
          <goals>
            <goal>add-test-source</goal>
          </goals>
          <configuration>
            <sources>
              <source>your/custom/test/dir</source>
            </sources>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

这样，`mvn test` 就会自动把 `your/custom/test/dir` 作为测试源码目录。

### 2. 通过 maven-surefire-plugin includes 配置

在 `pom.xml` 里配置：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>3.2.5</version>
  <configuration>
    <includes>
      <include>your/custom/test/dir/**/*.java</include>
    </includes>
  </configuration>
</plugin>
```

### 3. 只运行指定目录下的测试（命令行）

如果你只是想运行某个目录下的测试，可以用 surefire 的 includes：

```bash
mvn -Dtest=**/your/custom/test/dir/** test
```

但这种方式不如上面插件方式通用。

### 4. 常见问题排查

如果 `mvn test` 没有执行子模块的单元测试，可能的原因：

1. **子模块未被聚合到父模块的 `<modules>` 中**
2. **子模块的 `pom.xml` 没有 `<packaging>jar</packaging>`**
3. **子模块没有 `src/test/java` 目录或没有任何测试类**
4. **测试类/方法命名不规范**（默认只执行以 `Test` 结尾的类、以 `test` 开头的方法）
5. **子模块被 `<skipTests>true</skipTests>` 或 `-DskipTests` 跳过**
6. **子模块的 `pom.xml` 没有继承父模块的 surefire 配置**

### 总结

- **推荐**：用 `build-helper-maven-plugin` 在 pom.xml 里添加自定义测试目录。
- **不推荐**：直接用命令行参数指定测试源码目录（Maven原生不支持）。
- **只想运行某目录下的测试**：用 surefire 的 includes 配置。

如需具体配置示例或有特殊目录结构，请补充说明！ 

---

## 八、pom.xml 中 build 标签位置说明

在 `pom.xml` 文件中，`<build>` 标签应该放在以下位置：

### 1. 标准位置（推荐）

`<build>` 标签应该放在 `<project>` 根标签内，通常位于 `<dependencies>` 之后：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>My Project</name>
    <description>My project description</description>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <!-- build 标签放在这里 -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>3.2.0</version>
                <executions>
                    <execution>
                        <id>add-test-source</id>
                        <phase>generate-test-sources</phase>
                        <goals>
                            <goal>add-test-source</goal>
                        </goals>
                        <configuration>
                            <sources>
                                <source>your/custom/test/dir</source>
                            </sources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    
</project>
```

### 2. 推荐的标签顺序

在 `<project>` 内的标准顺序通常是：

1. `<modelVersion>`
2. `<groupId>`, `<artifactId>`, `<version>`, `<packaging>`
3. `<name>`, `<description>`, `<url>`
4. `<properties>`
5. `<dependencies>`
6. **`<build>`** ← 放在这里
7. `<profiles>`
8. `<modules>` (如果是父模块)
9. `<dependencyManagement>`
10. `<pluginManagement>`

### 3. 注意事项

- `<build>` 标签**不能**放在 `<dependencies>` 内部
- `<build>` 标签**不能**放在 `<properties>` 内部
- `<build>` 标签**不能**放在其他标签内部
- `<build>` 标签必须直接放在 `<project>` 标签下

### 4. 多模块项目

在多模块项目中，父模块和子模块都可以有自己的 `<build>` 标签，位置相同。

**总结**：`<build>` 标签应该放在 `<project>` 根标签内，通常位于 `<dependencies>` 之后，其他配置标签之前。

如需具体配置示例或有特殊需求，请补充说明！ 

---

## 九、在 GitLab CI 中定义全局 build.gradle 配置

在 GitLab CI 中，可以通过多种方式定义全局的 Gradle 配置，让所有 stage 共享相同的构建配置，避免在每个 job 中重复定义。

### 方法一：使用全局变量定义 Gradle 命令（推荐）

这是最简单直接的方式，通过定义全局变量统一管理 Gradle 命令：

```yaml
image: ubuntu:22.04

variables:
  GRADLE_HOME: "/opt/gradle-8.14"
  PATH: "$GRADLE_HOME/bin:$PATH"
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  NEXUS_URL: ${NEXUS_URL}
  NEXUS_USERNAME: ${NEXUS_USERNAME}
  NEXUS_PASSWORD: ${NEXUS_PASSWORD}
  # 定义全局的 gradle 命令，包含 init script
  GRADLE_CMD: "gradle --init-script ci-init.gradle"

before_script:
  - chmod +x $GRADLE_HOME/bin/gradle
  - gradle --version
  # 创建全局的 init.gradle 脚本
  - |
    cat > ci-init.gradle << 'EOF'
    allprojects {
        repositories {
            // 阿里云 Maven 仓库
            maven { url = 'https://maven.aliyun.com/repository/public' }
            maven { url = 'https://maven.aliyun.com/repository/spring' }
            maven { url = 'https://maven.aliyun.com/repository/google' }
            // 私有仓库配置
            maven {
                url = System.getenv('NEXUS_URL') ?: 'https://your-private-repo.com/repository/maven-releases/'
                credentials {
                    username = System.getenv("NEXUS_USERNAME") ?: project.findProperty("nexusUsername")
                    password = System.getenv("NEXUS_PASSWORD") ?: project.findProperty("nexusPassword")
                }
            }
            mavenCentral()
        }
    }
    EOF

stages:
  - build
  - test
  - package
  - deploy

cache:
  paths:
    - .gradle/
    - build/

build:
  stage: build
  script:
    - $GRADLE_CMD build -x test --no-daemon
    - echo "=== 构建的JAR包列表 ==="
    - find . -path "*/build/libs/*.jar" -type f -exec ls -lh {} \;
    - echo "=== Gradle构建产物信息 ==="
    - gradle properties | grep -E "(name|version|group)" || true
    - echo "=== 构建完成 ==="

test:
  stage: test
  script:
    - $GRADLE_CMD test jacocoTestReport --no-daemon
  artifacts:
    paths:
      - "build/reports/"
      - "*/build/reports/"
      - "**/build/reports/"
    expire_in: 1 week

package:
  stage: package
  script:
    - $GRADLE_CMD assemble --no-daemon
    - echo "=== 查找所有 JAR 文件 ==="
    - find . -path "*/build/libs/*.jar" -type f
    - echo "=== 打包所有 JAR 文件为 all-jars.zip ==="
    - find . -path "*/build/libs/*.jar" -type f -print0 | xargs -0 zip all-jars.zip
    - ls -lh all-jars.zip
  artifacts:
    paths:
      - all-jars.zip
    expire_in: 1 week

deploy:
  stage: deploy
  script:
    - $GRADLE_CMD publish --no-daemon
  only:
    - main
  when: manual
```

### 方法二：使用 extends 定义基础 job 模板

使用 GitLab CI 的 `extends` 功能定义基础配置，所有 job 继承该配置：

```yaml
image: ubuntu:22.04

variables:
  GRADLE_HOME: "/opt/gradle-8.14"
  PATH: "$GRADLE_HOME/bin:$PATH"
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  NEXUS_URL: ${NEXUS_URL}
  NEXUS_USERNAME: ${NEXUS_USERNAME}
  NEXUS_PASSWORD: ${NEXUS_PASSWORD}

before_script:
  - chmod +x $GRADLE_HOME/bin/gradle
  - gradle --version
  # 创建全局的 init.gradle 脚本
  - |
    cat > ci-init.gradle << 'EOF'
    allprojects {
        repositories {
            maven { url = 'https://maven.aliyun.com/repository/public' }
            maven { url = 'https://maven.aliyun.com/repository/spring' }
            maven { url = 'https://maven.aliyun.com/repository/google' }
            maven {
                url = System.getenv('NEXUS_URL') ?: 'https://your-private-repo.com/repository/maven-releases/'
                credentials {
                    username = System.getenv("NEXUS_USERNAME") ?: project.findProperty("nexusUsername")
                    password = System.getenv("NEXUS_PASSWORD") ?: project.findProperty("nexusPassword")
                }
            }
            mavenCentral()
        }
    }
    EOF

stages:
  - build
  - test
  - package
  - deploy

cache:
  paths:
    - .gradle/
    - build/

# 定义基础 job 模板
.gradle_base:
  before_script:
    - chmod +x $GRADLE_HOME/bin/gradle
    - gradle --version
  script:
    - gradle --init-script ci-init.gradle

build:
  extends: .gradle_base
  stage: build
  script:
    - gradle --init-script ci-init.gradle build -x test --no-daemon
    - echo "=== 构建的JAR包列表 ==="
    - find . -path "*/build/libs/*.jar" -type f -exec ls -lh {} \;
    - echo "=== Gradle构建产物信息 ==="
    - gradle properties | grep -E "(name|version|group)" || true
    - echo "=== 构建完成 ==="

test:
  extends: .gradle_base
  stage: test
  script:
    - gradle --init-script ci-init.gradle test jacocoTestReport --no-daemon
  artifacts:
    paths:
      - "build/reports/"
      - "*/build/reports/"
      - "**/build/reports/"
    expire_in: 1 week

package:
  extends: .gradle_base
  stage: package
  script:
    - gradle --init-script ci-init.gradle assemble --no-daemon
    - echo "=== 查找所有 JAR 文件 ==="
    - find . -path "*/build/libs/*.jar" -type f
    - echo "=== 打包所有 JAR 文件为 all-jars.zip ==="
    - find . -path "*/build/libs/*.jar" -type f -print0 | xargs -0 zip all-jars.zip
    - ls -lh all-jars.zip
  artifacts:
    paths:
      - all-jars.zip
    expire_in: 1 week

deploy:
  extends: .gradle_base
  stage: deploy
  script:
    - gradle --init-script ci-init.gradle publish --no-daemon
  only:
    - main
  when: manual
```

### 方法三：使用 YAML Anchors

使用 YAML anchors 定义可复用的配置片段：

```yaml
image: ubuntu:22.04

variables:
  GRADLE_HOME: "/opt/gradle-8.14"
  PATH: "$GRADLE_HOME/bin:$PATH"
  GRADLE_OPTS: "-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2048m"
  NEXUS_URL: ${NEXUS_URL}
  NEXUS_USERNAME: ${NEXUS_USERNAME}
  NEXUS_PASSWORD: ${NEXUS_PASSWORD}

# 定义全局的 init.gradle 脚本创建步骤
.gradle_init: &gradle_init
  - |
    cat > ci-init.gradle << 'EOF'
    allprojects {
        repositories {
            maven { url = 'https://maven.aliyun.com/repository/public' }
            maven { url = 'https://maven.aliyun.com/repository/spring' }
            maven { url = 'https://maven.aliyun.com/repository/google' }
            maven {
                url = System.getenv('NEXUS_URL') ?: 'https://your-private-repo.com/repository/maven-releases/'
                credentials {
                    username = System.getenv("NEXUS_USERNAME") ?: project.findProperty("nexusUsername")
                    password = System.getenv("NEXUS_PASSWORD") ?: project.findProperty("nexusPassword")
                }
            }
            mavenCentral()
        }
    }
    EOF

# 定义通用的 gradle 命令前缀
.gradle_cmd: &gradle_cmd
  - gradle --init-script ci-init.gradle

before_script:
  - chmod +x $GRADLE_HOME/bin/gradle
  - gradle --version
  - *gradle_init

stages:
  - build
  - test
  - package
  - deploy

cache:
  paths:
    - .gradle/
    - build/

build:
  stage: build
  script:
    - *gradle_cmd
    - gradle --init-script ci-init.gradle build -x test --no-daemon
    - echo "=== 构建的JAR包列表 ==="
    - find . -path "*/build/libs/*.jar" -type f -exec ls -lh {} \;
    - echo "=== Gradle构建产物信息 ==="
    - gradle properties | grep -E "(name|version|group)" || true
    - echo "=== 构建完成 ==="

test:
  stage: test
  script:
    - *gradle_cmd
    - gradle --init-script ci-init.gradle test jacocoTestReport --no-daemon
  artifacts:
    paths:
      - "build/reports/"
      - "*/build/reports/"
      - "**/build/reports/"
    expire_in: 1 week

package:
  stage: package
  script:
    - *gradle_cmd
    - gradle --init-script ci-init.gradle assemble --no-daemon
    - echo "=== 查找所有 JAR 文件 ==="
    - find . -path "*/build/libs/*.jar" -type f
    - echo "=== 打包所有 JAR 文件为 all-jars.zip ==="
    - find . -path "*/build/libs/*.jar" -type f -print0 | xargs -0 zip all-jars.zip
    - ls -lh all-jars.zip
  artifacts:
    paths:
      - all-jars.zip
    expire_in: 1 week

deploy:
  stage: deploy
  script:
    - *gradle_cmd
    - gradle --init-script ci-init.gradle publish --no-daemon
  only:
    - main
  when: manual
```

### 方案对比

| 方法 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| 方法一（全局变量） | 简单直接，易于维护 | 灵活性稍低 | 快速实现，统一配置 |
| 方法二（extends） | 结构清晰，易于维护 | 配置稍复杂 | 需要多个 job 共享配置 |
| 方法三（Anchors） | 简洁，复用性好 | YAML 语法要求高 | 简单项目，需要复用配置片段 |

### 推荐方案

**推荐使用方法一（全局变量）**，原因：
1. ✅ **简单直接**：只需定义一个变量，所有 job 自动使用
2. ✅ **易于维护**：修改配置只需在一个地方
3. ✅ **自动继承**：所有 job 自动继承 `before_script` 中的 init script 创建
4. ✅ **统一管理**：通过 `$GRADLE_CMD` 变量统一调用 Gradle 命令

### 关键点说明

1. **init script 的创建**：在 `before_script` 中动态创建 `ci-init.gradle`，确保所有 job 都能使用
2. **环境变量使用**：通过 `System.getenv()` 读取 CI/CD 环境变量，安全可靠
3. **配置复用**：通过全局变量或 extends 机制，避免在每个 job 中重复配置
4. **缓存配置**：`.gradle/` 和 `build/` 目录的缓存可以加速后续构建

### 注意事项

- 确保在 GitLab CI/CD 变量设置中配置了 `NEXUS_URL`、`NEXUS_USERNAME`、`NEXUS_PASSWORD`
- init script 中的单引号 `'EOF'` 可以防止变量在创建脚本时被展开
- 如果需要在 init script 中使用环境变量，使用 `System.getenv()` 而不是 `${}` 语法

如需进一步定制或有特殊需求，请联系开发负责人。
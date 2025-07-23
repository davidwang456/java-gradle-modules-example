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
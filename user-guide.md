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
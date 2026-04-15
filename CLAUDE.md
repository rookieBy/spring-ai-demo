# 项目配置

## Maven 配置

**Maven 路径**: `D:\software_install\maven\apache-maven-3.9.11\bin\mvn`

**本地仓库**: `D:\software_install\maven\maven-repository-3.9.11`

**WSL 访问路径**:
- Maven: `/mnt/d/software_install/maven/apache-maven-3.9.11/bin/mvn`
- 本地仓库: `/mnt/d/software_install/maven/maven-repository-3.9.11`

**项目级配置**: `settings.xml` (已配置本地仓库路径)

**构建命令**:

```bash
# 使用 settings.xml 指定本地仓库
mvn compile -s settings.xml -pl newsay-server-ai-llm -am
mvn package -s settings.xml -DskipTests -pl newsay-server-ai-launcher -am

# 或通过 MAVEN_OPTS 环境变量
export MAVEN_OPTS="-Dmaven.repo.local=/mnt/d/software_install/maven/maven-repository-3.9.11"
mvn compile -pl newsay-server-ai-llm -am
```

## Java 版本

需要 Java 21

---

## Git Worktrees

工作树目录: `.worktrees/`

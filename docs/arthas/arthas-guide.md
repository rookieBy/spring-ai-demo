# Arthas 监控 Spring Boot 实战指南

## 1. Arthas 简介

Arthas 是阿里巴巴开源的 Java 诊断工具，无需重启 JVM 即可实时查看应用运行状态，支持在线热修复。

**官网:** https://arthas.aliyun.com/

## 2. 环境信息

| 项目 | 值 |
|------|-----|
| Arthas 位置 | `/opt/arthas/arthas-boot.jar` |
| WSL IP | `172.20.187.163` |
| Arthas Web Console 端口 | `8563` |
| Arthas Telnet 端口 | `9999` |
| 应用端口 | `38010` |

## 3. 快速启动

### 3.1 启动 Spring Boot 应用

```bash
cd /root/coding_plan/newsay-server-ai/current

# 使用 38010 端口启动
nohup java -Xms512m -Xmx1024m -XX:+UseG1GC \
    -jar newsay-server-ai-launcher-1.0.0.jar \
    --server.port=38010 > app.log 2>&1 &

echo $!  # 记录 PID
```

### 3.2 附加 Arthas 到 Java 进程

```bash
cd /opt/arthas

# 获取 Java 进程 PID
ps aux | grep java

# 启动 Arthas（配置远程访问和认证）
java -jar arthas-boot.jar <PID> \
    --target-ip 0.0.0.0 \
    --telnet-port 9999 \
    --http-port 8563 \
    --username admin \
    --password admin123 &
```

### 3.3 关键参数说明

| 参数 | 说明 |
|------|------|
| `--target-ip 0.0.0.0` | 允许从任意 IP 访问 |
| `--http-port 8563` | Arthas Web Console 端口 |
| `--telnet-port 9999` | Telnet 连接端口 |
| `--username` | 设置用户名 |
| `--password` | 设置密码 |

## 4. 远程访问 Arthas

### 4.1 Web Console 访问

在 **Windows 浏览器**中打开：

```
http://172.20.187.163:8563
```

登录信息：
- 用户名：`admin`
- 密码：`admin123`

### 4.2 Telnet 访问

```bash
telnet 172.20.187.163 9999
```

## 5. 防火墙配置（Windows）

确保 Windows 防火墙允许以下端口：

```powershell
# 使用 PowerShell（管理员权限）
New-NetFirewallRule -DisplayName "Arthas HTTP" -Direction Inbound -Protocol TCP -LocalPort 8563 -Action Allow
New-NetFirewallRule -DisplayName "Arthas Telnet" -Direction Inbound -Protocol TCP -LocalPort 9999 -Action Allow
```

## 6. 常用命令

### 6.1 查看 Dashboard（实时监控面板）

```bash
dashboard
```

显示内容包括：线程、内存、GC、运行环境等信息。

### 6.2 查看 JVM 信息

```bash
jvm
```

### 6.3 查看线程状态

```bash
# 查看所有线程
thread

# 查看阻塞线程
thread -b

# 查看死锁
thread -n

# 查看指定线程
thread <thread-id>
```

### 6.4 查看内存信息

```bash
# 查看内存使用情况
memory

# 查看对象统计
classloader
```

### 6.5 查看 GC 情况

```bash
# 查看 GC 详情
gc

# 监控 GC 频率（持续输出）
monitor -c 5 com.example.* *  # 每 5 秒输出
```

### 6.6 导出堆内存

```bash
# 导出堆内存到文件
heapdump /tmp/heapdump.hprof
```

导出的文件可以使用 `jhat` 或 VisualVM 分析。

## 7. 关闭 Arthas

```bash
# 在 Arthas 控制台输入
quit
# 或
exit
```

完全停止：
```bash
pkill -f arthas-boot
```

## 8. 常见问题

### Q: 浏览器访问显示 "Domains, protocols and ports must match"

A: 这是浏览器的安全策略。请确保使用 `http://172.20.187.163:8563` 而非 `localhost` 或 `127.0.0.1`。

### Q: 返回 401 未授权

A: 确保启动 Arthas 时设置了用户名密码，并使用正确的凭据登录。

### Q: 端口被占用

A: 先关闭占用端口的进程，或使用其他端口：
```bash
--http-port 8564
--telnet-port 9998
```

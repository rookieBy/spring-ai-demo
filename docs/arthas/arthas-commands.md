# Arthas 命令详解 - JVM 与线程排查指南

## 目录

- [JVM 相关命令](#jvm-相关命令)
- [线程相关命令](#线程相关命令)
- [排查案例](#排查案例)

---

## JVM 相关命令

### 1. dashboard - 实时监控面板

**命令：**
```bash
dashboard
```

**输出信息：**
- **线程区域**：RUNNING、BLOCKED、WAITING、TIMED_WAITING 线程数量
- **内存区域**：堆内存使用情况（Eden、Survivor、Old）
- **GC 区域**：Young GC、Full GC 次数和耗时
- **运行环境**：加载类数、线程总数、CPU 使用率

**适用场景：**
- 快速了解系统整体运行状态
- 发现异常（GC 突然增多、线程数暴涨）

---

### 2. jvm - JVM 状态详细信息

**命令：**
```bash
jvm
```

**输出内容：**

| 字段 | 说明 |
|------|------|
| `THREADS` | 当前线程数（peak、历史峰值） |
| `CLASSES` | 已加载类数量 |
| `HEAP MEMORY` | 堆内存使用情况（Used/Committed/Max） |
| `NO HEAP MEMORY` | 非堆内存（Metaspace、Code Cache 等） |
| `GC` | 各垃圾收集器的 GC 次数和耗时 |

**关键指标解读：**

- **BLOCKED 线程数过多**：可能存在锁竞争
- **Full GC 次数不为 0 或持续增长**：内存压力、内存泄漏
- **加载类数量持续增长**：可能存在字节码增强或类加载泄漏

---

### 3. memory - 内存详细信息

**命令：**
```bash
memory
```

**输出内容：**
```
 Memory                       used      total      max       usage
 heap                        256M      480M       1024M     25.00%
 g1 old generation            100M      200M       1024M     9.77%
 g1 eden space                150M      150M       -         0.00%
 g1 survivor space             6M        6M        -         0.00%
 nonheap                     52M        60M        -         86.67%
 metaspace                   40M        45M        -         88.89%
 codecache                   8M         10M        -         80.00%
```

**诊断建议：**
- `heap` 使用率持续 > 80%：检查内存配置或存在内存泄漏
- `metaspace` 使用率 > 90%：需要增加 `-XX:MaxMetaspaceSize`
- `codecache` 使用率 > 90%：JIT 编译受影响，添加 `-XX:ReservedCodeCacheSize`

---

### 4. gc - GC 详细分析

**命令：**
```bash
gc
```

**输出内容：**
```
 gc                    count         time
 Young GC               120           1.2s
 Full GC                 5            2.3s
```

**持续监控 GC（推荐）：**
```bash
# 每 5 秒输出一次 GC 统计
monitor -c 5 com.sun.management.GarbageCollectorMXBean * --expand false
```

**GC 频繁排查思路：**

1. **Young GC 频繁**
   - 原因：Eden 区太小，对象分配速率高
   - 解决：增大 `-Xmn` 或 `-XX:NewSize`

2. **Full GC 频繁**
   - 原因：老年代内存不足、内存泄漏、大对象直接进入老年代
   - 排查步骤：
     ```bash
     # 1. 查看对象分布
     heapdump /tmp/heap.hprof

     # 2. 使用 MAT 分析堆文件
     # 3. 查找内存泄漏（长期存活对象持续增长）

     # 4. 查看是否有多余的 Full GC
     jstat -gcutil <pid> 1000
     ```

3. **G1 GC 调优参数**
   ```
   -XX:MaxGCPauseMillis=200    # 目标停顿时间
   -XX:G1HeapRegionSize=16M     # Region 大小
   -XX:InitiatingHeapOccupancyPercent=45  # 触发 Mixed GC 阈值
   ```

---

### 5. heapdump - 堆内存转储

**命令：**
```bash
# 导出到指定文件
heapdump /tmp/heapdump_$(date +%Y%m%d_%H%M%S).hprof

# 只导出活对象（默认）
heapdump --live /tmp/heap_live.hprof
```

**分析工具：**
- [Eclipse MAT](https://www.eclipse.org/mat/) - 内存分析
- [VisualVM](https://visualvm.github.io/) - Java 监控工具
- `jhat` - JDK 自带堆分析工具

```bash
# 使用 jhat 分析
jhat /tmp/heapdump.hprof
# 然后浏览器访问 http://localhost:7000
```

---

## 线程相关命令

### 1. thread - 线程状态分析

**命令：**
```bash
# 查看所有线程
thread

# 查看线程状态统计
thread -s

# 查看指定线程详情
thread <thread-id>

# 查看阻塞线程
thread -b

# 查看死锁
thread -n

# 查看 CPU 使用最高的线程
thread -n 5
```

**线程状态说明：**

| 状态 | 说明 | 正常情况 |
|------|------|----------|
| RUNNING | 正在执行 | 波动 |
| BLOCKED | 等待获取监视器锁 | 偶发 |
| WAITING | 无限期等待 | 偶发 |
| TIMED_WAITING | 指定时间等待 | 偶发 |

**异常线程状态诊断：**

#### 案例 1：大量线程处于 BLOCKED 状态

```bash
# 1. 查看阻塞线程
thread -b

# 输出示例：
"pool-1-thread-10" Id=35 BLOCKED on java.lang.Object@7a5d5c4e
    owned by "pool-1-thread-1" Id=25

# 2. 查看持有锁的线程
thread 25

# 3. 查看锁信息
monitor -c 5 -b com.example.MyService process
```

**排查步骤：**
1. `thread -b` 找出阻塞链
2. 查看持有锁的线程在做什么
3. 分析锁竞争原因
4. 优化锁粒度或使用并发包

#### 案例 2：大量线程处于 WAITING/TIMED_WAITING

```bash
# 查看等待中的线程
thread --state WAITING

# 查看限时等待
thread --state TIMED_WAITING
```

**常见原因：**
- `LockSupport.park()` - 线程池等待任务
- `Object.wait()` - 等待条件变量
- `Thread.sleep()` - 代码中存在 sleep
- 数据库/网络 I/O 阻塞

#### 案例 3：死锁检测

```bash
# 方法 1：使用 thread -n
thread -n

# 方法 2：使用 jstack
jstack <pid>

# 输出示例：
Found one Java-level deadlock:
"pool-1-thread-2":
  waiting for monitor entry to 0x00000000c5c5c5c0
  which is held by "pool-1-thread-1"
"pool-1-thread-1":
  waiting for monitor entry to 0x00000000c5c5c5d0
  which is held by "pool-1-thread-2"
```

**解决死锁：**
1. 确认死锁线程 ID
2. 如果无法自动恢复，杀掉线程或重启应用
3. 分析代码中的锁获取顺序
4. 使用 `ReentrantLock` 并设置超时

---

### 2. jstack - 线程堆栈信息

**命令：**
```bash
# 导出完整线程堆栈
jstack <pid>

# 导出到文件
jstack <pid> > /tmp/thread_dump.txt
```

**输出格式解析：**

```
"pool-1-thread-10" #35 prio=5 os_prio=0 tid=0x00007f8c4c012800 nid=0x7f3a waiting on condition [0x00007f8c3c5d5000]
   java.lang.Thread.State: WAITING (parking)
    at sun.misc.Unsafe.park(Native Method)
    at java.util.concurrent.locks.LockSupport.park(LockSupport.java:304)
    at java.util.concurrent.FutureTask.awaitDone(FutureTask.java:358)
```

**关键信息：**
- `nid` - Native 线程 ID（用于关联操作系统线程）
- `waiting on condition` - 等待原因
- `parking` - LockSupport 暂停

**CPU 占用高排查：**
```bash
# 1. 找出 CPU 占用最高的线程
top -Hp <pid>

# 2. 转换为 16 进制
printf '%x\n' <thread-id>

# 3. 在 jstack 输出中搜索该线程 ID
jstack <pid> | grep -A 20 <nid-in-hex>
```

---

### 3. monitor - 方法调用监控

**命令：**
```bash
# 监控方法调用（每 5 秒输出）
monitor -c 5 com.example.Service methodName

# 监控所有方法
monitor -c 5 com.example.*
```

**输出内容：**
```
 timestamp         class           method        total  success  fail  avg(ms)  fail-rate
 2024-01-01 12:00  Service         process       1000    999      1     25.00      0.10%
```

---

## 排查案例

### 案例 1：线程池饥饿（Thread Pool Starvation）

**症状：**
- 请求堆积
- 线程池线程全部处于 WAITING 状态
- CPU 使用率低

**排查步骤：**
```bash
# 1. 查看线程状态分布
thread -s

# 2. 查看阻塞线程
thread -b

# 3. 查看等待队列
jstack <pid> | grep "pool-.*ThreadPool"
```

**解决方案：**
- 增加线程池大小
- 检查任务是否阻塞在 I/O 或数据库
- 使用异步处理

---

### 案例 2：内存泄漏

**症状：**
- 内存使用量持续上升
- Full GC 越来越频繁
- Old 区内存持续增长

**排查步骤：**
```bash
# 1. 多次 heapdump 对比
heapdump /tmp/heap1.hprof
# 等待一段时间或执行可疑操作
heapdump /tmp/heap2.hprof

# 2. 使用 MAT 对比
# 打开两个堆文件，使用 Histogram 对比

# 3. 查看对象增长
jmap -histo <pid> | head -50
```

**典型泄漏场景：**
- 集合类未清理（Map 持续 put）
- 监听器未注销
- 静态集合持有对象引用
- ThreadLocal 未清理

---

### 案例 3：G1 GC 频繁 Young GC

**诊断：**
```bash
# 1. 查看 GC 情况
gc

# 2. 持续监控
jstat -gcutil <pid> 1000

# 3. 查看年轻代配置
jinfo -flag NewSize <pid>
jinfo -flag MaxNewSize <pid>
```

**解决：**
```
# 增加年轻代大小
-XX:NewSize=512m -XX:MaxNewSize=512m

# 或调整 G1 参数
-XX:G1NewSizePercent=30
-XX:G1MaxNewSizePercent=40
```

---

### 案例 4：死锁

**症状：**
- 应用无响应
- 线程全部等待

**排查：**
```bash
# 1. 快速检测死锁
thread -n

# 2. 完整线程 dump
jstack <pid> > /tmp/thread_dump.log

# 3. 分析死锁线程的锁依赖
```

**代码修复：**
```java
// 错误：锁顺序不一致
// 线程 A 先锁 lockA 再锁 lockB
// 线程 B 先锁 lockB 再锁 lockA

// 修复：统一锁顺序
// 所有线程都先锁 lockA 再锁 lockB

// 或使用 ReentrantLock 尝试获取
if (lockA.tryLock() && lockB.tryLock()) {
    try {
        // do work
    } finally {
        lockB.unlock();
        lockA.unlock();
    }
}
```

---

## 常用命令速查表

| 目的 | 命令 |
|------|------|
| 实时监控面板 | `dashboard` |
| JVM 状态 | `jvm` |
| 内存详情 | `memory` |
| GC 状态 | `gc` |
| 所有线程 | `thread` |
| 阻塞线程 | `thread -b` |
| 死锁检测 | `thread -n` |
| 线程堆栈 | `jstack <pid>` |
| 堆内存导出 | `heapdump /tmp/dump.hprof` |
| 方法监控 | `monitor -c 5 class method` |
| 反编译类 | `jad class.Name` |
| 查看类加载器 | `classloader` |

## 参考链接

- [Arthas 官方文档](https://arthas.aliyun.com/doc/)
- [Arthas 命令列表](https://arthas.aliyun.com/doc/commands.html)
- [JVM 调优指南](https://www.oracle.com/technetwork/java/javaee/overview/index.html)

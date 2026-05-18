# Arthas GC 排查面试题精讲

> 本文档整理自大厂实际面试场景，涵盖频繁 GC / Full GC 问题定位的思路、命令及底层原理。

---

## 题目一：Young GC 频繁如何排查？

### 面试官想考察什么？
- 候选人对 GC 分代算法的理解
- 是否掌握 Young GC 频繁的常见原因
- 是否熟练使用 Arthas 工具定位问题

### 常见原因
1. **Eden 区太小**，对象分配速率高
2. **Minor GC 对象晋升年龄太小**，导致频繁进入 Survivor 区甚至 Old 区
3. **业务代码创建了大量短生命周期对象**（如字符串拼接、流式 API 中间对象）
4. **大对象直接进入 Old 区**，挤压 Survivor 空间

### 使用 Arthas 排查步骤

```bash
# 1. 查看 GC 概况，确认 Young GC 频率
gc

# 2. 持续监控 GC，每 1 秒输出一次
monitor -c 1 com.sun.management.GarbageCollectorMXBean * --expand false

# 3. 查看内存分代情况
memory

# 4. 查看堆内存详情
jmap -heap <pid>

# 5. 导出堆文件，用 MAT 分析对象分布
heapdump /tmp/heap.hprof
```

### 排查思路

| 步骤 | 命令 | 看什么 |
|------|------|--------|
| 确认 GC 类型 | `gc` | Young GC count 是否持续增长 |
| 分析内存使用 | `memory` | Eden 区使用率是否接近 100% |
| 定位对象 | `heapdump` + MAT | 哪些对象占用最大 |
| 检查晋升 | `jstat -gcutil <pid> 1000` | Survivor 区是否能容纳 |

### 回答要点

> Young GC 频繁通常是因为 **Eden 区太小** 或 **对象分配速率太高**。首先通过 `gc` 命令确认是 Young GC 还是 Full GC，然后结合 `memory` 查看各代使用率。如果 Eden 区长期 > 90%，说明对象分配速率过高，需要分析代码中是否有 **对象创建热点**（如循环创建对象、字符串拼接），或考虑增大年轻代 `-Xmn`。

---

## 题目二：Full GC 频繁如何系统排查？

### 面试官想考察什么？
- 对 Full GC 触发条件的理解（老年代满、MetaSpace 满、FGC 显式调用等）
- 是否有完整的排查链路（从现象到根因）
- 能否将工具使用和问题定位串联成体系

### Full GC 触发原因

| 触发条件 | 原因 | 表现 |
|---------|------|------|
| Old 区空间不足 | 内存分配速率 > 回收速率 | Old 使用率高，Full GC 后不下降 |
| MetaSpace 满 | 类加载过多 | Non-heap 区持续增长 |
| System.gc() | 代码显式调用 | 时间点固定 |
| JNI/JNI 引用 | Native 代码持有 Java 对象 | 偶发 |

### Arthas 排查步骤

```bash
# 1. 确认 Full GC 频率
gc

# 2. 查看 JVM 完整状态
jvm

# 3. 查看内存详情
memory

# 4. 导出堆文件分析
heapdump /tmp/fullgc_heap_$(date +%Y%m%d_%H%M%S).hprof
```

### 用 jstat 辅助分析

```bash
# 每 1 秒输出一次 GC 统计
jstat -gcutil <pid> 1000

# 关键指标：
# - Old 区使用率持续 > 80%
# - Full GC 次数持续增长
# - GC 时间占用过高
```

### 常见场景及解决方案

**场景 A：内存泄漏**

```
表现：Full GC 后 Old 区从 95% -> 70%，下次稳定在 70%
说明：有 30% 是回收不掉的对象
排查：多次 heapdump，对比 Histogram，找持续增长的对象
常见泄漏：静态集合、ThreadLocal、监听器未注销
```

**场景 B：老年代空间分配不足**

```
表现：Old 区使用率稳步增长，最终触发 Full GC
排查：jmap -heap 查看 Old 区大小
解决：-Xmx 足够大，或调整 -XX:OldSize
```

**场景 C：MetaSpace 满**

```
表现：Full GC 扩展到 MetaSpace
排查：jvm 命令查看 CLASSES 加载数量
解决：增加 -XX:MaxMetaspaceSize，或排查是否有大量反射/代理类
```

### 回答要点

> Full GC 频繁的核心原因是**老年代（或 MetaSpace）空间不足且回收不掉**。排查思路是：`gc` 确认类型 → `jvm` + `memory` 看各代使用率 → `heapdump` 导出堆文件 → 用 MAT 分析保留对象。如果 Full GC 后内存不下降，基本是**内存泄漏**；如果 Old 区稳步上升后触发 GC，说明**内存分配速率超过回收能力**，需要优化代码或调整参数。

---

## 题目三：如何判断是内存泄漏还是内存分配过快？

### 面试官想考察什么？
- 能否区分两类表象相似但根因不同的问题
- 是否理解 GC 日志中"回收后内存是否归零"的含义
- 是否有量化的判断方法

### 判断方法：两次 heapdump 对比

```bash
# 第一次 dump（Full GC 后）
jmap -dump:live,format=b,file=heap1.hprof <pid>

# 等待一段时间（或做一轮业务操作）

# 第二次 dump（Full GC 后）
jmap -dump:live,format=b,file=heap2.hprof <pid>
```

用 MAT 打开两个文件，使用 **Histogram Compare** 功能：

| 对比结果 | 结论 |
|---------|------|
| 对象数量和大小完全相同 | 正常，没有泄漏 |
| 某些对象数量**线性增长** | 内存泄漏（集合在持续 add） |
| 某些对象大小不变但数量不变 | 正常（缓存池等） |
| Dump 时 Full GC 后内存下降明显 | 内存分配过快，非泄漏 |

### GC 回收后内存是否归位

```
内存泄漏特征：
  Full GC 后 Old 区从 95% -> 70%，下次稳定在 70%
  说明有 30% 是"正常"对象，70% 每次都回收不掉

内存分配过快特征：
  Full GC 后 Old 区从 95% -> 30%
  说明回收正常，但业务生产对象太快
```

### 回答要点

> 区分内存泄漏和分配过快，核心看 **Full GC 后内存是否归位**。如果每次 Full GC 后 Old 区使用率都降到一个稳定的低值，说明是分配过快；如果每次 GC 后都维持在高水位，说明有对象回收不掉。可以用 **两次 heapdump + MAT Histogram 对比** 看哪个类/集合的对象数量在持续增长。内存泄漏一定是某个**集合或静态变量在持续持有对象引用**。

---

## 题目四：G1 GC 延迟高如何排查？

### 面试官想考察什么？
- 对 G1 收集器特性的理解（Region、Mixed GC、Pause）
- 是否掌握 G1 特有的调优思路
- 能否说出 G1 和 CMS/Parallel 的核心区别

### G1 关键概念

| 概念 | 说明 |
|------|------|
| Region | G1 将堆分成 1MB~32MB 的小区域 |
| Young GC | 回收年轻代 Region |
| Mixed GC | 回收年轻代 + 部分老年代 Region |
| Humongous | 超过 Region 50% 的大对象，直接进入老年代 |
| Pause | GC 时所有应用线程停顿 |

### 排查命令

```bash
# 1. 查看 GC 详情
gc

# 2. 查看 GC 耗时分布
jstat -gccapacity <pid>

# 3. Arthas 中查看实时面板
dashboard

# 4. 查看 Young/Mixed GC 耗时
jstat -gcutil <pid> 1000
# 关注：YGC 时间 > 1s，MGC 时间 > 1s，FGC 时间 > 1s
```

### G1 调优参数

```bash
# 目标停顿时间（默认 200ms）
-XX:MaxGCPauseMillis=200

# 年轻代占比（默认 5%）
-XX:G1NewSizePercent=30

# 触发 Mixed GC 的老年代占比（默认 45%）
-XX:InitiatingHeapOccupancyPercent=45

# Region 大小（默认自动）
-XX:G1HeapRegionSize=16M
```

### 常见延迟原因及解决

| 问题 | 表现 | 解决方案 |
|------|------|---------|
| Old 区空间不足触发 Mixed GC 时间长 | MGC 耗时 > 500ms | 增加 `-XX:G1ReservePercent` |
| 大量 Humongous 对象 | 大对象直接入 Old 区 | 避免大对象，或增大 Region size |
| Mixed GC 回收老年代过多 | Old 区很大时触发停顿长 | 降低 `InitiatingHeapOccupancyPercent` |
| 应用线程被 GC 阻塞 | GC 时间波动大 | 减少 GC 次数（增大堆或调年轻代占比） |

### 回答要点

> G1 的特点是 **停顿可控**（通过 MaxGCPauseMillis）和 **Region 化**。延迟高通常是 **Mixed GC 回收的 Region 太多**。先用 `jstat -gcutil` 看哪类 GC 耗时最长：如果是 YGC 慢，考虑增大年轻代或减少对象分配；如果是 MGC/FGC 慢，降低老年代触发阈值或增加预留空间。Humongous 对象（大对象直接入 Old 区）是 G1 的性能杀手，需要通过 `G1HeapRegionSize` 调优。

---

## 题目五：线上 Full GC 导致应用卡顿，如何用 Arthas 快速定位？

### 面试官想考察什么？
- 是否有过线上排查和应急的经验
- 是否理解 **Stop-The-World** 对业务的影响
- 能否在高压下快速定位问题

### 场景描述

> 某服务每隔几分钟出现一次 RT 毛刺，P99 延迟突然飙高，GC 日志显示 Full GC 耗时 3-5 秒，如何快速定位？

### 排查步骤

**Step 1：快速确认（30 秒内）**

```bash
# 登录 Arthas，查看实时面板
dashboard

# 重点关注：
# - 线程数是否暴涨
# - 内存各代使用率
# - GC 次数和耗时
```

**Step 2：定位 GC 线程**

```bash
# 查看所有线程状态
thread

# 查看当前 CPU 使用最高的线程
thread -n 5

# 查看阻塞线程
thread -b
```

**Step 3：GC 详情**

```bash
# 确认 Full GC
gc

# 如果 Full GC 频繁，立刻导出堆文件（Full GC 后对象少）
heapdump /tmp/urgent_$(date +%Y%m%d_%H%M%S).hprof
```

**Step 4：分析堆文件**

```bash
# 导出后用 MAT 打开
# 重点看：
# 1. Histogram - 按对象大小排序，找最大的对象
# 2. Leak Suspects - MAT 自动分析泄漏嫌疑
# 3. Dominator Tree - 找引用链

# 常见元凶：
# - HashMap/ArrayList 持续 put 导致
# - 数据库 ResultSet 未关闭
# - ThreadLocal 未清理
```

### 完整排查链路

```
1. dashboard（全局感知）→ 确认是 GC 问题
2. gc（GC 详情）→ 确认 Full GC 频率和耗时
3. memory（内存详情）→ 确认 Old 区满了
4. heapdump（堆文件）→ 保留现场
5. jstack <pid> > /tmp/thread.log（线程快照）→ 分析线程状态
6. MAT 分析堆文件 → 定位具体对象
7. 代码走查 → 找到泄漏点
```

### 无法导出堆文件时的替代方案

```bash
# 没有堆文件时的替代方案：

# 1. 查看类加载情况（类泄漏）
classloader

# 2. 查看实例数量
jmap -histo <pid> | head -50

# 3. 持续观察（如果 GC 还在发生）
watch com.example.Service methodName '{params, returnObj}' -x 2
```

### 回答要点

> 线上 Full GC 导致卡顿，第一步是 **确认问题**：通过 `dashboard` 快速判断是 GC 问题还是业务线程问题。如果是 GC 问题，立刻 `heapdump` 保留现场（Full GC 后对象最少），然后用 MAT 分析。如果是生产环境无法 dump，用 `jmap -histo` 实时看哪些对象最多，结合 `classloader` 查类泄漏。**永远记住：保留现场 > 分析问题**，不要在排查时重启应用。

---

## 附加：面试加分项

### 加分话题 1：GC 日志分析

```bash
# 开启 GC 日志
-XX:+UseGCLogFileRotation -Xlog:gc*:file=/tmp/gc.log:time,uptime:filecount=5,filesize=10M

# 分析工具：GCViewer、GCeasy（在线）
```

### 加分话题 2：JVM 调优口诀

> **Minor GC 频繁调年轻代，Full GC 频繁调老年代，MetaSpace 满加上限，GC 时间长加堆内存或换收集器。**

### 加分话题 3：不同收集器的选择

| 场景 | 推荐收集器 |
|------|-----------|
| 吞吐量优先（后台批处理） | Parallel GC |
| 低延迟优先（在线业务） | G1 / ZGC |
| 超大堆（> 64G） | ZGC / Shenandoah |
| 常规 Web 应用 | G1（默认） |

---

## 参考命令速查

| 场景 | 命令 |
|------|------|
| 确认 GC 类型和频率 | `gc` |
| 查看内存各代使用 | `memory` |
| JVM 完整状态 | `jvm` |
| 实时监控面板 | `dashboard` |
| 导出堆文件 | `heapdump /tmp/dump.hprof` |
| 查看线程堆栈 | `jstack <pid>` |
| 查看线程状态 | `thread` / `thread -b` / `thread -n` |
| 方法调用监控 | `monitor -c 5 class method` |
| 反编译类 | `jad class.Name` |
| 类加载器查看 | `classloader` |
| JVM 参数查看 | `jinfo <pid>` |

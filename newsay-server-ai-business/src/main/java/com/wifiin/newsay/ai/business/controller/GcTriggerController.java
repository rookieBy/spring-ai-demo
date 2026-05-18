package com.wifiin.newsay.ai.business.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * GC Trigger Controller - 用于触发不同类型的 GC，方便通过 Arthas dashboard 观察 GC 过程。
 *
 * JVM 参数建议（512MB 堆）:
 * -Xms512m -Xmx512m
 * -XX:NewSize=200m -XX:MaxNewSize=200m
 * -XX:SurvivorRatio=2
 * -XX:OldSize=312m -XX:MaxOldSize=312m
 * -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M
 *
 * 内存布局（SurvivorRatio=2 时）:
 * - Young Gen: 200MB (Eden=100MB, S0=50MB, S1=50MB)
 * - Old Gen: 312MB
 *
 * 观察步骤:
 * 1. 调用 /young 多次（建议 6 次），观察 Young GC 和对象逐渐进入 Survivor 再进入 Old Gen
 * 2. 调用 /old 快速填充老年代，观察 Old GC
 * 3. 调用 /full 触发 Full GC
 */
@RestController
@RequestMapping("/api/gc/trigger")
public class GcTriggerController {

    /**
     * 保持老年代对象的引用，持续累积直到触发 Full GC
     */
    private static final List<byte[]> OLD_GEN_HOLDER = new ArrayList<>();

    /**
     * 触发 Young GC - 分配临时对象
     * 每次调用分配约 20MB，在方法结束后成为垃圾，等待 Young GC 回收
     */
    @GetMapping("/young")
    public String triggerYoungGC() {
        for (int i = 0; i < 20; i++) {
            byte[] temp = new byte[1024 * 1024]; // 1MB × 20 = 20MB
        }
        return "Young GC triggered: allocated 20MB temporary objects for Young GC observation";
    }

    /**
     * 触发 Old GC - 分配对象直接进入老年代
     * 保留引用防止回收，持续占用老年代空间
     */
    @GetMapping("/old")
    public String triggerOldGC() {
        byte[] bigObject = new byte[10 * 1024 * 1024]; // 10MB
        OLD_GEN_HOLDER.add(bigObject);
        return "Old GC triggered: allocated 10MB object into old generation, held in list (total: " + OLD_GEN_HOLDER.size() + " objects)";
    }

    /**
     * 触发 Full GC - 持续分配直到老年代满
     */
    @GetMapping("/full")
    public String triggerFullGC() {
        int count = 0;
        try {
            // 持续分配 10MB 对象填满老年代（~312MB / 10MB ≈ 31 次）
            for (int i = 0; i < 100; i++) {
                byte[] largeObject = new byte[10 * 1024 * 1024]; // 10MB
                OLD_GEN_HOLDER.add(largeObject);
                count++;
            }
        } catch (OutOfMemoryError e) {
            OLD_GEN_HOLDER.clear();
            System.gc();
            return "Full GC triggered: allocated " + count + " × 10MB objects, caught OOM, called System.gc()";
        }
        return "Full GC: allocated " + count + " × 10MB objects in old generation";
    }

    /**
     * 清理已累积的老年代对象，释放内存
     */
    @GetMapping("/cleanup")
    public String cleanup() {
        int size = OLD_GEN_HOLDER.size();
        OLD_GEN_HOLDER.clear();
        return "Cleaned up " + size + " objects from old generation holder";
    }
}

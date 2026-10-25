package com.ticket.ops.probe.annotation;

import com.ticket.ops.probe.model.ProbeType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注入被监控应用的注解探针。
 * <p>
 * 使用方式：在 12306 的 Controller / Service / Mapper 方法上标注，
 * 引入 ops-probe-sdk 后由自动装配生效，采集异常绝不会影响业务方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpsProbe {

    /** 探针类型，默认按方法特征自动推断 */
    ProbeType value() default ProbeType.AUTO;

    /** SpEL 表达式，提取业务标识（订单号 / 车次号 / 用户 ID） */
    String bizKey() default "";

    /** 采样率 1~100，100 表示全量 */
    int sampleRate() default 100;

    /** 仅 MEMORY_PRESSURE 有效：是否记录入参 / 返回值大小 */
    boolean recordSize() default false;

    /** 参数脱敏开关，默认开启 */
    boolean desensitize() default true;
}

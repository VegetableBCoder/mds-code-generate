package com.mds.demo.common;

/**
 * 主键生成器（插件生成 model-service 时引用它）。
 *
 * <p>契约只有一条：{@code nextId()} 返回 long 雪花 ID。插件只对「主键字段名 id 且类型 Long」的表注入。
 */
public interface IdGenerator {

    long nextId();
}

package com.mds.codegen.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置里的一个实体：要生成哪个实体、它的枚举列对应哪个枚举类。
 *
 * <pre>
 *   entities:
 *     - name: AppUser
 *       enums:
 *         status: "com.example.demo.domain.user.UserStatusEnum"
 * </pre>
 */
public final class EntitySpec {

    /** 实体简单名（必须与 entity 类名一致）。 */
    public String name = "";

    /** 字段名 → 枚举全限定名。 */
    public final Map<String, String> enums = new LinkedHashMap<>();
}

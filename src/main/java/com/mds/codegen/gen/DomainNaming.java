package com.mds.codegen.gen;

import com.mds.codegen.config.CodegenConfig;

/**
 * model-service 的包定位。
 *
 * <p>是否按域分包由 {@code output.byDomain} **一个开关**决定：
 * <ul>
 *   <li>关闭（默认）：{@code model} / {@code model.impl}</li>
 *   <li>开启：{@code model.<域>} / {@code model.<域>.impl}，域默认取表名下划线前的部分
 *       （{@code app_user} → {@code app}），可用 {@code entities[].domain} 覆盖</li>
 * </ul>
 * 不再单独维护 domain / domainMapping 两套配置。
 */
public final class DomainNaming {

    private DomainNaming() {
    }

    /** 该实体所属的域；不分域时返回空串。 */
    public static String domainOf(String tableName, String entityName, CodegenConfig cfg) {
        if (!cfg.output.byDomain) {
            return "";
        }
        String override = cfg.entityDomains.get(entityName);
        if (override != null && !override.isBlank()) {
            return override;
        }
        int underscore = tableName.indexOf('_');
        return underscore > 0 ? tableName.substring(0, underscore) : tableName;
    }

    public static String servicePackage(String domain, CodegenConfig cfg) {
        return domain.isBlank() ? cfg.output.modelPackage : cfg.output.modelPackage + "." + domain;
    }

    public static String implPackage(String domain, CodegenConfig cfg) {
        String base = servicePackage(domain, cfg);
        return cfg.output.implSubPackage.isBlank() ? base : base + "." + cfg.output.implSubPackage;
    }
}

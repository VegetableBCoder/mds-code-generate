package com.mds.codegen.gen;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.config.EntitySpec;
import com.mds.codegen.model.DaoModel.Table;
import java.util.List;
import java.util.Map;

/**
 * 部件生成器 SPI。
 *
 * <p>输入只有「表模型 + 该实体的配置 + 枚举映射」，不碰文件系统——
 * 落盘统一由 {@link SourceWriter} 负责，覆盖策略、dry-run 都只有一处实现。
 */
public interface Generator {

    /** 部件标识，与 {@code parts} 一致；一个部件可以产出多个文件。 */
    String part();

    /**
     * @param table 表模型
     * @param spec  该实体的配置（含枚举映射）
     * @param cfg   全局配置
     * @return 生成的文件（0..n）
     */
    List<GeneratedFile> generate(Table table, EntitySpec spec, CodegenConfig cfg);
}

package com.mds.codegen.read;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.model.DaoModel.Table;
import java.util.List;

/**
 * 元数据读取 SPI：把 MBG 产物读成 {@link Table}。
 */
public interface MetadataReader {

    /** 唯一标识。 */
    String id();

    /** 读全部候选表（用于报错时列出可选项）。 */
    List<Table> readAll(CodegenConfig cfg, ReadContext ctx);

    /** 只读指定的一个实体（找不到就抛 {@link ReaderException}）。 */
    default Table readOne(CodegenConfig cfg, ReadContext ctx, String entitySimpleName) {
        List<Table> all = readAll(cfg, ctx);
        return all.stream()
                .filter(t -> t.entitySimpleName().equals(entitySimpleName))
                .findFirst()
                .orElseThrow(() -> new ReaderException("找不到实体 " + entitySimpleName
                        + "；可选的实体：" + all.stream().map(Table::entitySimpleName).sorted().toList()));
    }
}

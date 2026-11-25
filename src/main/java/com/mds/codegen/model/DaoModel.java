package com.mds.codegen.model;

import java.util.List;

/**
 * 读取层产出的内存模型，是生成器的**唯一输入**。
 *
 * <p>三层信息分别来自三处：
 * <ul>
 *   <li>{@link Bean} ← 实体（字段名 + Java 类型）</li>
 *   <li>{@link Column} ← {@code <X>DynamicSqlSupport}（列名 + Java 类型 + JDBC 类型）</li>
 *   <li>{@link Table#primaryKeyField()} ← {@code <X>Mapper} 的 {@code ...ByPrimaryKey} 方法</li>
 * </ul>
 *
 * <p>模型**只承载结构，不承载任何生成策略**（包名、后缀、域、枚举都在配置/生成器里）。
 * 这样换读取来源（源码 / 字节码）时生成器完全不用动。
 */
public final class DaoModel {

    private DaoModel() {
    }

    /** 一个持久化实体。 */
    public record Bean(String packageName, String simpleName, List<Field> fields) {

        public String qualifiedName() {
            return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        }

        public Field field(String name) {
            return fields.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
        }
    }

    /** 实体的一个字段。 */
    public record Field(String name, TypeRef type) {
    }

    /** 表列定义（来自 {@code SqlColumn<T>}）。 */
    public record Column(String fieldName, String columnName, TypeRef javaType, String jdbcType) {
    }

    /**
     * 一张表的完整信息。
     *
     * @param primaryKeyField 主键字段名；推不出时为 {@code null}
     * @param mapperQualifiedName mapper 全限定名（生成 model-service 时直接用，不做字符串替换）
     * @param supportQualifiedName DynamicSqlSupport 全限定名
     */
    public record Table(Bean bean,
                        String tableName,
                        List<Column> columns,
                        String primaryKeyField,
                        String mapperQualifiedName,
                        String supportQualifiedName) {

        public String beanQualifiedName() {
            return bean.qualifiedName();
        }

        public String entitySimpleName() {
            return bean.simpleName();
        }

        public Column primaryKey() {
            if (primaryKeyField == null) {
                return null;
            }
            return columns.stream()
                    .filter(c -> c.fieldName().equals(primaryKeyField))
                    .findFirst()
                    .orElse(null);
        }

        public Column column(String fieldName) {
            return columns.stream().filter(c -> c.fieldName().equals(fieldName)).findFirst().orElse(null);
        }
    }
}

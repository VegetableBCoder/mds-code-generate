package com.mds.codegen.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件配置（从 YAML 装载）。
 *
 * <h2>设计原则：只留"这个项目自己的信息"</h2>
 * 代码风格（Lombok / MapStruct / Spring / Optional）是**固定的**，不做开关——
 * 目标场景就是 Java 17+ + Lombok + MapStruct + Spring，多一个开关就多一处能配错的地方。
 *
 * <p>所以这里只有四块：从哪读（{@code source}）、往哪写（{@code output}）、
 * 要生成什么名字（{@code naming}）、处理哪些实体（{@code entities}）。
 */
public final class CodegenConfig {

    public final Source source = new Source();
    public final Output output = new Output();
    public final Naming naming = new Naming();
    public final Write write = new Write();

    /**
     * 要处理的实体清单。
     *
     * <p>不动"空 = 全部"：只有列在这里的实体才会被生成。
     * 每条可以带自己的枚举映射（字段名 → 枚举类全限定名）。
     */
    public final List<EntitySpec> entities = new ArrayList<>();

    /** 主键生成器全限定名；留空 = 不生成主键。 */
    public String idGenerator = "";

    /** 实际生效的配置文件路径（仅日志/诊断用）。 */
    public String configFile = "";

    /** 模块目录，相对路径都以它为基准。 */
    public String projectDir = "";

    // ------------------------------------------------------------------ 分节

    /** 元数据来源。 */
    public static final class Source {
        /** 实体包名。 */
        public String entityPackage = "";

        /** mapper 包名（{@code <X>Mapper} 与 {@code <X>DynamicSqlSupport} 都在这一层）。 */
        public String mapperPackage = "";

        /**
         * 编译后的 class 根目录。
         *
         * <p>留空 = Maven 的 {@code project.build.outputDirectory}。
         * 只有"读别人 jar 里的类"时才需要显式给。
         */
        public String classesRoot = "";
    }

    /** 输出位置与包名。 */
    public static final class Output {
        /** 生成根目录；默认放 target 下，Maven 会自动当源码根。 */
        public String dir = "target/generated-sources/codegen";

        public String dtoPackage = "";
        public String convertPackage = "";
        public String modelPackage = "";

        /** model-service 的实现子包。 */
        public String implSubPackage = "impl";

        /**
         * 是否按域分子包。
         *
         * <p>{@code true}：{@code model.<域>} / {@code model.<域>.impl}，域取表名下划线前的部分
         * （{@code app_user} → {@code app}）；需要覆盖时在实体条目上写 {@code domain:}。
         * {@code false}（默认）：平铺，{@code model} / {@code model.impl}。
         */
        public boolean byDomain = false;
    }

    /** 写盘策略。 */
    public static final class Write {
        /** {@code overwrite} | {@code skip} | {@code failOnDrift}。 */
        public String policy = "failOnDrift";
    }

    /** 命名后缀。 */
    public static final class Naming {
        public String dtoSuffix = "Dto";
        public String convertSuffix = "Converter";
        public String serviceSuffix = "ModelService";
        public String implSuffix = "ModelServiceImpl";
    }

    // ------------------------------------------------------------------ 装载

    /**
     * 从 SnakeYAML 解析出来的 Map 装载。
     *
     * <p>未知键直接报错——配置项拼错时静默忽略，是最难查的一类问题。
     */
    public static CodegenConfig fromMap(java.util.Map<String, Object> root) {
        CodegenConfig c = new CodegenConfig();
        if (root == null) {
            return c;
        }
        for (java.util.Map.Entry<String, Object> e : root.entrySet()) {
            switch (e.getKey()) {
                case "source" -> readSource(c.source, e.getValue());
                case "output" -> readOutput(c.output, e.getValue());
                case "naming" -> readNaming(c.naming, e.getValue());
                case "write" -> readWrite(c.write, e.getValue());
                case "entities" -> readEntities(c, e.getValue());
                case "idGenerator" -> c.idGenerator = str(e.getValue(), "");
                default -> throw new IllegalArgumentException(
                        unknown("顶层", e.getKey(), "source / output / naming / write / idGenerator / entities"));
            }
        }
        c.validate();
        return c;
    }

    private static void readSource(Source s, Object v) {
        for (java.util.Map.Entry<String, Object> e : map(v, "source").entrySet()) {
            switch (e.getKey()) {
                case "entityPackage" -> s.entityPackage = str(e.getValue(), s.entityPackage);
                case "mapperPackage" -> s.mapperPackage = str(e.getValue(), s.mapperPackage);
                case "classesRoot" -> s.classesRoot = str(e.getValue(), s.classesRoot);
                default -> throw new IllegalArgumentException(
                        unknown("source", e.getKey(), "entityPackage / mapperPackage / classesRoot"));
            }
        }
    }

    private static void readOutput(Output o, Object v) {
        for (java.util.Map.Entry<String, Object> e : map(v, "output").entrySet()) {
            switch (e.getKey()) {
                case "dir" -> o.dir = str(e.getValue(), o.dir);
                case "dtoPackage" -> o.dtoPackage = str(e.getValue(), o.dtoPackage);
                case "convertPackage" -> o.convertPackage = str(e.getValue(), o.convertPackage);
                case "modelPackage" -> o.modelPackage = str(e.getValue(), o.modelPackage);
                case "implSubPackage" -> o.implSubPackage = str(e.getValue(), o.implSubPackage);
                case "byDomain" -> o.byDomain = bool(e.getValue(), o.byDomain);
                default -> throw new IllegalArgumentException(
                        unknown("output", e.getKey(),
                                "dir / dtoPackage / convertPackage / modelPackage / implSubPackage / byDomain"));
            }
        }
    }

    private static void readNaming(Naming n, Object v) {
        for (java.util.Map.Entry<String, Object> e : map(v, "naming").entrySet()) {
            switch (e.getKey()) {
                case "dtoSuffix" -> n.dtoSuffix = str(e.getValue(), n.dtoSuffix);
                case "convertSuffix" -> n.convertSuffix = str(e.getValue(), n.convertSuffix);
                case "serviceSuffix" -> n.serviceSuffix = str(e.getValue(), n.serviceSuffix);
                case "implSuffix" -> n.implSuffix = str(e.getValue(), n.implSuffix);
                default -> throw new IllegalArgumentException(
                        unknown("naming", e.getKey(),
                                "dtoSuffix / convertSuffix / serviceSuffix / implSuffix"));
            }
        }
    }

    private static void readWrite(Write w, Object v) {
        for (java.util.Map.Entry<String, Object> e : map(v, "write").entrySet()) {
            switch (e.getKey()) {
                case "policy" -> w.policy = str(e.getValue(), w.policy);
                default -> throw new IllegalArgumentException(
                        unknown("write", e.getKey(), "policy"));
            }
        }
    }

    private static void readEntities(CodegenConfig c, Object v) {
        if (!(v instanceof List<?> list)) {
            throw new IllegalArgumentException("entities 应该是列表");
        }
        for (Object item : list) {
            java.util.Map<String, Object> m = map(item, "entities[]");
            EntitySpec spec = new EntitySpec();
            for (java.util.Map.Entry<String, Object> e : m.entrySet()) {
                switch (e.getKey()) {
                    case "name" -> spec.name = str(e.getValue(), "");
                    case "enums" -> spec.enums.putAll(stringMap(e.getValue(), "entities[].enums"));
                    case "domain" -> c.entityDomains.put(spec.name, str(e.getValue(), ""));
                    default -> throw new IllegalArgumentException(
                            unknown("entities[]", e.getKey(), "name / enums / domain"));
                }
            }
            if (spec.name.isBlank()) {
                throw new IllegalArgumentException("entities[] 里的每一项都必须有 name");
            }
            c.entities.add(spec);
        }
    }

    /** 实体名 → 域的覆盖（{@code entities[].domain}）。 */
    public final java.util.Map<String, String> entityDomains = new java.util.LinkedHashMap<>();

    // ------------------------------------------------------------------ 校验

    private void validate() {
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("entities 不能为空：请列出要生成的实体");
        }
        if (!write.policy.equals("overwrite") && !write.policy.equals("skip")
                && !write.policy.equals("failOnDrift")) {
            throw new IllegalArgumentException(
                    "write.policy 只能是 overwrite / skip / failOnDrift，实际: " + write.policy);
        }
        if (!output.byDomain) {
            return;
        }
        // 按域分包时，域从表名推导，无法在这里校验；生成时会把实际用的包打出来
    }

    // ------------------------------------------------------------------ 链式补充

    public CodegenConfig withFile(java.nio.file.Path file) {
        this.configFile = file == null ? "" : file.toString();
        return this;
    }

    public CodegenConfig withProjectDir(java.nio.file.Path dir) {
        this.projectDir = dir == null ? "" : dir.toString();
        return this;
    }

    /** 按实体简单名找配置（找不到返回 null）。 */
    public EntitySpec entity(String name) {
        return entities.stream().filter(e -> e.name.equals(name)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ 工具

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Object> map(Object v, String where) {
        if (v == null) {
            return java.util.Map.of();
        }
        if (!(v instanceof java.util.Map<?, ?> m)) {
            throw new IllegalArgumentException("配置项 " + where + " 应该是映射，实际: " + v.getClass().getSimpleName());
        }
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        m.forEach((k, val) -> out.put(String.valueOf(k), val));
        return out;
    }

    private static java.util.Map<String, String> stringMap(Object v, String where) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String, Object> e : map(v, where).entrySet()) {
            if (e.getValue() != null && !String.valueOf(e.getValue()).isBlank()) {
                out.put(e.getKey(), String.valueOf(e.getValue()).trim());
            }
        }
        return out;
    }

    /**
     * 未知键的报错文案。
     *
     * <p>单独点名几个"曾经存在、后来删掉"的键——照着旧文档抄配置的人会直接看到原因，
     * 而不是只看到"未知配置项"然后自己猜。
     */
    private static String unknown(String section, String key, String validKeys) {
        String hint = switch (section + "." + key) {
            case "output.domainPackage" ->
                    "；这个键已移除（枚举骨架生成功能取消后它没有用途了），删掉即可";
            case "enum.valueType", "enum.accessor", "enum.factory" ->
                    "；enum 整节已移除：枚举现在必须已存在，在 entities[].enums 里配全限定名";
            case "source.beanPattern", "source.entityDir", "source.mapperDir" ->
                    "；这个键已移除：实体由 entities 精确指定，目录按包名推导";
            case "output.domain", "output.domainMapping" ->
                    "；这个键已移除：改用 output.byDomain + entities[].domain";
            case "style.lombok", "style.mapstruct", "style.springService", "style.dtoBuilder",
                 "style.findByIdReturns" ->
                    "；style 整节已移除：Lombok / MapStruct / Spring / Optional 都是固定口径";
            case "parts", "enumMapping" ->
                    "；这个键的位置变了：parts 已移除（总是全量），enumMapping 移到 entities[].enums";
            default -> "";
        };
        return "未知配置项 " + section + "." + key + "（合法键：" + validKeys + "）" + hint;
    }

    private static String str(Object v, String dflt) {
        if (v == null) {
            return dflt;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? dflt : s;
    }

    private static boolean bool(Object v, boolean dflt) {
        if (v == null) {
            return dflt;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v).trim());
    }
}
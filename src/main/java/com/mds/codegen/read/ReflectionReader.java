package com.mds.codegen.read;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.model.DaoModel.Bean;
import com.mds.codegen.model.DaoModel.Column;
import com.mds.codegen.model.DaoModel.Table;
import com.mds.codegen.model.DaoModel.Field;
import com.mds.codegen.model.TypeRef;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 加载工程自己的类，用反射读元数据。
 *
 * <p>这是唯一的读取实现。相比解析源码/字节码，它更简单也更准：
 * <ul>
 *   <li>表名、列名、JDBC 类型直接调 {@code tableName()} / {@code column.name()} / {@code column.jdbcType()}；</li>
 *   <li>列 Java 类型读 {@code SqlColumn<T>} 字段的泛型实参；</li>
 *   <li>主键读 mapper 上 MBG 写好的 {@code @Result(id = true).property()}，不用反推合成 lambda；</li>
 *   <li>实体有没有 getter/setter 一看就知道（源码方案看不见 Lombok 生成的成员）。</li>
 * </ul>
 *
 * <h2>前提</h2>
 * 被读的工程必须**先编译**（{@code target/classes} 存在）。插件通过
 * {@code project.getCompileClasspathElements()} 拼出 classpath，再建一个隔离的
 * {@link URLClassLoader}——父加载器用平台加载器，避免插件自身的依赖干扰被读工程。
 *
 * <h2>副作用</h2>
 * 读表定义会触发 {@code <X>DynamicSqlSupport} 的静态初始化（MBG 产物里只是构造表对象，
 * 不连库、不读配置）。其它类一概用 {@code initialize=false} 加载。
 */
public final class ReflectionReader implements MetadataReader {

    private static final String SUPPORT_SUFFIX = "DynamicSqlSupport";
    private static final String MAPPER_SUFFIX = "Mapper";

    @Override
    public String id() {
        return "reflection";
    }

    @Override
    public List<Table> readAll(CodegenConfig cfg, ReadContext ctx) {
        if (ctx.classpath().isEmpty()) {
            throw new ReaderException("classpath 为空：无法加载被读工程的类。请确认插件运行在 Maven 里"
                    + "（会取 project.getCompileClasspathElements()）");
        }
        Path classesRoot = cfg.source.classesRoot.isBlank() ? ctx.classesDir() : ctx.resolve(cfg.source.classesRoot);
        Path entityDir = classesRoot.resolve(cfg.source.entityPackage.replace('.', '/'));
        Path mapperDir = classesRoot.resolve(cfg.source.mapperPackage.replace('.', '/'));

        if (!Files.isDirectory(classesRoot)) {
            throw new ReaderException("编译输出目录不存在: " + classesRoot + "\n"
                    + "  本插件读的是**已编译的 class**，不是源码。先在 IDE 里 Build 一次"
                    + "（或执行 mvn compile），再点 codegen:generate。");
        }
        if (!Files.isDirectory(entityDir)) {
            throw new ReaderException(packageMissing("实体", cfg.source.entityPackage, entityDir, classesRoot));
        }
        if (!Files.isDirectory(mapperDir)) {
            throw new ReaderException(packageMissing("mapper", cfg.source.mapperPackage, mapperDir, classesRoot));
        }

        List<URL> urls = new ArrayList<>();
        for (Path p : ctx.classpath()) {
            try {
                urls.add(p.toUri().toURL());
            } catch (IOException e) {
                throw new ReaderException("classpath 项无法转 URL: " + p, e);
            }
        }
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new),
                ClassLoader.getPlatformClassLoader())) {
            List<Table> out = new ArrayList<>();
            for (Path beanClass : listClassFiles(entityDir).stream()
                    .sorted(Comparator.comparing(Path::toString)).toList()) {
                String simple = simpleNameOf(beanClass);
                if (simple.contains("$") || simple.endsWith(SUPPORT_SUFFIX)
                        || simple.endsWith(MAPPER_SUFFIX) || simple.equals("package-info")) {
                    continue;
                }
                Path supportFile = mapperDir.resolve(simple + SUPPORT_SUFFIX + ".class");
                Path mapperFile = mapperDir.resolve(simple + MAPPER_SUFFIX + ".class");
                if (!Files.isRegularFile(supportFile) || !Files.isRegularFile(mapperFile)) {
                    continue;                       // 不是 MBG 生成的表（例如手写行视图）
                }
                Table table = readOne(loader, cfg, simple);
                if (table != null) {
                    out.add(table);
                }
            }
            return out;
        } catch (IOException e) {
            throw new ReaderException("关闭 classloader 失败", e);
        }
    }

    private Table readOne(ClassLoader loader, CodegenConfig cfg, String simple) {
        String entityFqn = qualified(cfg.source.entityPackage, simple);
        String mapperFqn = qualified(cfg.source.mapperPackage, simple + MAPPER_SUFFIX);
        String supportFqn = qualified(cfg.source.mapperPackage, simple + SUPPORT_SUFFIX);

        Class<?> entityClass = load(loader, entityFqn, false);
        Class<?> mapperClass = load(loader, mapperFqn, false);
        Class<?> supportClass = load(loader, supportFqn, true);   // 需要静态初始化才能拿到表实例

        Bean bean = new Bean(cfg.source.entityPackage, simple, readFields(entityClass));
        Object table = findTableInstance(supportClass, simple);
        if (table == null) {
            throw new ReaderException("在 " + supportFqn + " 里找不到表定义字段"
                    + "（期望有一个静态字段持有 <X>DynamicSqlSupport$<X> 的实例）");
        }
        String tableName = invokeString(table, "tableName");
        if (tableName == null) {
            throw new ReaderException("表实例 " + table.getClass().getName() + " 没有 tableName() 返回值");
        }
        List<Column> columns = readColumns(table);
        if (columns.isEmpty()) {
            throw new ReaderException("表 " + tableName + " 没读到任何 SqlColumn 字段（" + supportFqn + "）");
        }
        String pk = readPrimaryKey(mapperClass, columns, simple);
        return new Table(bean, tableName, columns, pk, mapperFqn, supportFqn);
    }

    /**
     * 包目录不存在时的诊断。
     *
     * <p>最典型的错法是**抄了示例配置**（{@code com.example.demo.xxx}）而没改成本项目包名。
     * 所以这里去 classesRoot 下扫一遍，把实际存在的包列出来，让人一眼看出该填什么。
     */
    private String packageMissing(String what, String configured, Path expected, Path classesRoot) {
        StringBuilder sb = new StringBuilder();
        sb.append(what).append(" class 目录不存在: ").append(expected).append('\n');
        sb.append("  当前 ").append(what.equals("实体") ? "source.entityPackage" : "source.mapperPackage")
                .append(" = \"").append(configured).append("\"\n");

        List<String> candidates = existingPackages(classesRoot);
        if (candidates.isEmpty()) {
            sb.append("  编译输出目录里没有任何包 —— 先 Build 一次（mvn compile）\n");
        } else {
            sb.append("  实际存在的包（取前若干个）：\n");
            candidates.stream().limit(12).forEach(c -> sb.append("    - ").append(c).append('\n'));
            sb.append("  如果上面有你要的，把配置里的包名改成它；")
                    .append("如果是示例配置里的 com.example.demo 占位值，说明还没改成本项目包名。\n");
        }
        return sb.toString();
    }

    /** 编译输出目录里实际存在的包（只扫两层，够定位 entity/mapper 这类约定包）。 */
    private List<String> existingPackages(Path classesRoot) {
        Path base = Path.of(".");
        List<String> out = new ArrayList<>();
        try (var stream = Files.walk(classesRoot, 6)) {
            for (Path p : stream.filter(Files::isDirectory).toList()) {
                if (p.equals(classesRoot)) {
                    continue;
                }
                List<String> segs = new ArrayList<>();
                for (Path part : classesRoot.relativize(p)) {
                    segs.add(part.toString());
                }
                String pkg = String.join(".", segs);
                if (!pkg.isBlank() && out.size() < 200) {
                    out.add(pkg);
                }
            }
        } catch (IOException e) {
            return List.of();
        }
        out.sort(String::compareTo);
        return out;
    }

    // ------------------------------------------------------------------ 实体

    private List<Field> readFields(Class<?> entityClass) {
        List<Field> out = new ArrayList<>();
        for (java.lang.reflect.Field f : entityClass.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || f.isSynthetic()) {
                continue;
            }
            out.add(new Field(f.getName(), toTypeRef(f.getGenericType(), f.getName())));
        }
        return out;
    }

    // ------------------------------------------------------------------ 表与列

    /** 找持有表实例的静态字段：类型上是 {@code <X>} 内部类，或有 {@code tableName()} 方法。 */
    private Object findTableInstance(Class<?> supportClass, String simple) {
        Object fallback = null;
        for (java.lang.reflect.Field f : supportClass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
                continue;
            }
            Object value;
            try {
                f.setAccessible(true);
                value = f.get(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                continue;                       // 有的静态字段可能初始化失败，跳过
            }
            if (value == null || value instanceof CharSequence || value instanceof Number) {
                continue;
            }
            boolean namedLikeTable = value.getClass().getSimpleName().equals(simple);
            boolean looksLikeTable = hasTableMethods(value.getClass());
            if (namedLikeTable && looksLikeTable) {
                return value;
            }
            if (looksLikeTable && fallback == null) {
                fallback = value;
            }
        }
        return fallback;
    }

    private boolean hasTableMethods(Class<?> c) {
        try {
            c.getMethod("tableName");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private List<Column> readColumns(Object table) {
        List<Column> out = new ArrayList<>();
        for (java.lang.reflect.Field f : table.getClass().getDeclaredFields()) {
            if (!f.getType().getName().equals("org.mybatis.dynamic.sql.SqlColumn")) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object column = f.get(table);
                if (column == null) {
                    continue;
                }
                String columnName = invokeString(column, "name");
                String jdbcType = invokeOptionalName(column, "jdbcType");
                TypeRef javaType = columnJavaType(f, column);
                out.add(new Column(f.getName(), columnName, javaType, jdbcType));
            } catch (ReflectiveOperationException | RuntimeException e) {
                throw new ReaderException("读列 " + f.getName() + " 失败: " + e, e);
            }
        }
        return out;
    }

    /**
     * 列 Java 类型：优先 {@code SqlColumn<T>} 的泛型实参（MBG 产物一定有），
     * 退到 {@code column.javaType()}（只有显式传了才非空——实测 MBG 产物这里是 empty）。
     */
    private TypeRef columnJavaType(java.lang.reflect.Field field, Object column) {
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 1) {
            Type arg = pt.getActualTypeArguments()[0];
            if (!(arg instanceof java.lang.reflect.TypeVariable<?>)) {
                return toTypeRef(arg, field.getName());
            }
        }
        Optional<?> viaApi = invokeOptional(column, "javaType");
        if (viaApi.isPresent() && viaApi.get() instanceof Class<?> c) {
            return TypeRef.of(c.getName());
        }
        throw new ReaderException("列 " + field.getName() + " 读不出 Java 类型（泛型实参缺失且 javaType() 为空）");
    }

    // ------------------------------------------------------------------ 主键

    /**
     * 主键：读 MBG 写在 {@code selectMany} 上的 {@code @Results} 里 {@code @Result(id = true).property()}。
     *
     * <p>这是声明式的——比"反推合成 lambda 里唯一引用的列"稳得多。
     * 读不到时退化为 {@code ByPrimaryKey} 方法的唯一形参类型匹配（仅当唯一匹配），再不行返回 {@code null}。
     */
    private String readPrimaryKey(Class<?> mapperClass, List<Column> columns, String simple) {
        String fromResults = readPkFromResults(mapperClass);
        if (fromResults != null) {
            return fromResults;
        }
        return readPkFromMethodParameter(mapperClass, columns);
    }

    /**
     * 从 {@code @Results(value = {@Result(column=..., property=..., id=true)})} 里取 property。
     *
     * <p>用反射调注解的方法而不是强类型引用，是为了**不把 mybatis 变成插件的编译期依赖**。
     */
    private String readPkFromResults(Class<?> mapperClass) {
        for (java.lang.reflect.Method method : mapperClass.getDeclaredMethods()) {
            for (java.lang.annotation.Annotation annotation : method.getAnnotations()) {
                if (!annotation.annotationType().getName().equals("org.apache.ibatis.annotations.Results")) {
                    continue;
                }
                Object[] results = (Object[]) invokeAnnotationMethod(annotation, "value");
                if (results == null) {
                    continue;
                }
                for (Object result : results) {
                    Object id = invokeAnnotationMethod(result, "id");
                    if (Boolean.TRUE.equals(id)) {
                        Object property = invokeAnnotationMethod(result, "property");
                        if (property instanceof String s) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Object invokeAnnotationMethod(Object annotation, String name) {
        if (!(annotation instanceof java.lang.annotation.Annotation ann)) {
            return null;
        }
        try {
            return ann.annotationType().getMethod(name).invoke(ann);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** 兜底：{@code deleteByPrimaryKey} / {@code selectByPrimaryKey} 的唯一形参，按类型唯一匹配列。 */
    private String readPkFromMethodParameter(Class<?> mapperClass, List<Column> columns) {
        for (java.lang.reflect.Method method : mapperClass.getDeclaredMethods()) {
            String name = method.getName();
            if (!name.equals("deleteByPrimaryKey") && !name.equals("selectByPrimaryKey")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) {
                continue;
            }
            String typeName = params[0].getName();
            List<Column> hits = columns.stream()
                    .filter(c -> c.javaType().qualifiedName().equals(typeName))
                    .toList();
            if (hits.size() == 1) {
                return hits.get(0).fieldName();
            }
            throw new ReaderException("mapper " + mapperClass.getName() + " 的 " + name
                    + " 形参类型是 " + typeName + "，但表里有 " + hits.size()
                    + " 个同类型列，无法确定主键；请在配置里显式指定 primaryKey");
        }
        return null;
    }

    // ------------------------------------------------------------------ 工具

    private Class<?> load(ClassLoader loader, String fqn, boolean initialize) {
        try {
            return Class.forName(fqn, initialize, loader);
        } catch (ClassNotFoundException e) {
            throw new ReaderException("找不到类 " + fqn + "（classpath 是否完整？是否已编译？）", e);
        }
    }

    private String invokeString(Object target, String method) {
        Object v = invoke(target, method);
        return v instanceof String s ? s : null;
    }

    private String invokeOptionalName(Object target, String method) {
        Optional<?> o = invokeOptional(target, method);
        return o.map(String::valueOf).orElse("");
    }

    private Optional<?> invokeOptional(Object target, String method) {
        Object v = invoke(target, method);
        return v instanceof Optional<?> o ? o : Optional.empty();
    }

    private Object invoke(Object target, String method) {
        try {
            var m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new ReaderException("调用 " + target.getClass().getName() + "." + method + "() 失败: " + e, e);
        }
    }

    /** 反射 {@link Type} → {@link TypeRef}。 */
    static TypeRef toTypeRef(Type type, String where) {
        if (type instanceof Class<?> c) {
            if (c.isArray()) {
                return TypeRef.of(toTypeRef(c.getComponentType(), where).qualifiedName() + "[]");
            }
            return TypeRef.of(c.getName());
        }
        if (type instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            String rawName = raw instanceof Class<?> c ? c.getName() : raw.getTypeName();
            List<TypeRef> args = new ArrayList<>();
            for (Type arg : pt.getActualTypeArguments()) {
                args.add(toTypeRef(arg, where));
            }
            return new TypeRef(rawName, args);
        }
        if (type instanceof java.lang.reflect.TypeVariable<?> || type instanceof java.lang.reflect.WildcardType) {
            return TypeRef.of("java.lang.Object");
        }
        throw new ReaderException("不支持的类型 " + type + "（位置：" + where + "）");
    }

    private static String simpleNameOf(Path classFile) {
        String name = classFile.getFileName().toString();
        return name.substring(0, name.length() - ".class".length());
    }

    private static String qualified(String pkg, String simple) {
        return pkg.isEmpty() ? simple : pkg + "." + simple;
    }

    private static List<Path> listClassFiles(Path dir) {
        try (var stream = Files.walk(dir)) {
            return stream.filter(p -> p.toString().endsWith(".class")).toList();
        } catch (IOException e) {
            throw new ReaderException("遍历目录失败: " + dir, e);
        }
    }
}
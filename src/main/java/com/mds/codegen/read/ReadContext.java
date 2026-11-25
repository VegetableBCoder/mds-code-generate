package com.mds.codegen.read;

import java.nio.file.Path;
import java.util.List;

/**
 * 读取上下文。
 *
 * @param projectDir  模块目录（配置里的相对路径以它为基准）
 * @param classesDir  构建输出目录（{@code target/classes}）
 * @param sourceDir   源码根（{@code src/main/java}）——枚举骨架写这里，因为之后由人长期维护
 * @param classpath   被读工程的编译期 classpath（已含 {@code classesDir}）
 */
public record ReadContext(Path projectDir, Path classesDir, Path sourceDir, List<Path> classpath) {

    public ReadContext(Path projectDir, Path classesDir, List<Path> classpath) {
        this(projectDir, classesDir, projectDir.resolve("src/main/java"), classpath);
    }

    public ReadContext {
        classpath = List.copyOf(classpath);
    }

    public Path resolve(String maybeRelative) {
        if (maybeRelative == null || maybeRelative.isBlank()) {
            return projectDir;
        }
        Path p = Path.of(maybeRelative);
        return p.isAbsolute() ? p : projectDir.resolve(p);
    }
}

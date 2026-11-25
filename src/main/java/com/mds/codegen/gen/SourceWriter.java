package com.mds.codegen.gen;

import com.mds.codegen.config.CodegenConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 落盘：**覆盖策略的唯一实现处**。
 *
 * <table>
 *   <tr><td>{@code overwrite}</td><td>无条件覆盖</td></tr>
 *   <tr><td>{@code skip}</td><td>已存在就跳过</td></tr>
 *   <tr><td>{@code failOnDrift}</td><td>已存在且内容不同 → 失败（CI 友好）；相同 → 跳过</td></tr>
 * </table>
 *
 * <p><b>没有 merge</b>：不做"生成物里留手写区"那套。需要加自定义方法就直接写进实现类，
 * 之后靠 git 对比/回滚，或把策略换成 {@code skip}。少一个合并机制，就少一类事故。
 */
public final class SourceWriter {

    /** 一次写盘的结果。 */
    public record WriteResult(List<Path> written, List<Path> skipped, List<Path> drifted) {

        public int total() {
            return written.size() + skipped.size() + drifted.size();
        }
    }

    private final Path outputDir;
    private final CodegenConfig cfg;

    public SourceWriter(Path outputDir, CodegenConfig cfg) {
        this.outputDir = outputDir;
        this.cfg = cfg;
    }

    public WriteResult writeAll(List<GeneratedFile> files) throws IOException {
        List<Path> written = new ArrayList<>();
        List<Path> skipped = new ArrayList<>();
        List<Path> drifted = new ArrayList<>();
        for (GeneratedFile file : files) {
            Path target = outputDir.resolve(file.relativePath());
            String content = file.content();
            if (Files.isRegularFile(target)) {
                String existing = Files.readString(target, StandardCharsets.UTF_8);
                switch (cfg.write.policy) {
                    case "overwrite" -> {
                        // 直接往下写
                    }
                    case "skip" -> {
                        skipped.add(target);
                        continue;
                    }
                    case "failOnDrift" -> {
                        if (existing.equals(content)) {
                            skipped.add(target);
                        } else {
                            drifted.add(target);
                        }
                        continue;
                    }
                    default -> throw new IllegalStateException("未知 write.policy: " + cfg.write.policy);
                }
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
            written.add(target);
        }
        return new WriteResult(written, skipped, drifted);
    }

}
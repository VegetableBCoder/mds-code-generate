package com.mds.codegen;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.gen.EnumSkeletonGenerator;
import com.mds.codegen.config.ConfigLoader;
import com.mds.codegen.config.EntitySpec;
import com.mds.codegen.gen.ConverterGenerator;
import com.mds.codegen.gen.DomainNaming;
import com.mds.codegen.gen.DtoGenerator;
import com.mds.codegen.gen.GeneratedFile;
import com.mds.codegen.gen.Generator;
import com.mds.codegen.gen.ServiceImplGenerator;
import com.mds.codegen.gen.ServiceInterfaceGenerator;
import com.mds.codegen.gen.SourceWriter;
import com.mds.codegen.model.DaoModel.Column;
import com.mds.codegen.model.DaoModel.Field;
import com.mds.codegen.model.DaoModel.Table;
import com.mds.codegen.read.ClasspathProbe;
import com.mds.codegen.read.ReadContext;
import com.mds.codegen.read.ReflectionReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * 代码生成：读 MBG（{@code MyBatis3DynamicSql}）产物，生成 DTO / converter / model-service。
 *
 * <p>处理哪些实体**写在配置文件里**（{@code entities}），命令行只需要
 * {@code mvn ...:generate} 一条命令，不用每次带 {@code -Dcodegen.entity}。
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * 编译期 classpath（由 Maven 求值，会触发依赖解析）。
     *
     * <p>不用 {@code project.getCompileClasspathElements()}：直接调 goal 时它可能只返回输出目录。
     */
    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> compileClasspathElements;

    /**
     * 配置文件路径；不配则按 {@link ConfigLoader#DEFAULT_LOCATIONS} 顺序查找。
     *
     * <p>{@code -Dcodegen.config=...} 优先（见 {@link #execute()}）。
     */
    @Parameter(property = "codegen.config")
    private File configFile;

    /** 只处理配置里的某一个实体（可选，用于临时单独重生成一个）。 */
    @Parameter(property = "codegen.entity")
    private String entity;

    /** 覆盖写盘策略：overwrite / skip / failOnDrift。 */
    @Parameter(property = "codegen.writePolicy")
    private String writePolicy;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path projectDir = project.getBasedir().toPath();
        CodegenConfig cfg = loadConfig(projectDir);
        applyOverrides(cfg, projectDir);

        ReadContext ctx = readContext(projectDir);
        logConfig(cfg, ctx);

        ReflectionReader reader = new ReflectionReader();
        List<EntitySpec> targets = selectEntities(cfg);
        List<GeneratedFile> files = new ArrayList<>();

        for (EntitySpec spec : targets) {
            Table table;
            try {
                table = reader.readOne(cfg, ctx, spec.name);
            } catch (RuntimeException e) {
                throw new MojoExecutionException("读实体 " + spec.name + " 失败：" + e.getMessage(), e);
            }
            logTable(table, cfg);
            try {
                enumSkeletons(spec, table, ctx);
            } catch (IOException e) {
                throw new MojoExecutionException("生成枚举骨架失败：" + e.getMessage(), e);
            }
            if (!spec.enums.isEmpty()) {
                spec.enums.forEach((k, v) -> getLog().info("  枚举 " + k + " -> " + v));
            }
            try {
                for (Generator gen : List.of(new DtoGenerator(), new ConverterGenerator(),
                        new ServiceInterfaceGenerator(), new ServiceImplGenerator())) {
                    files.addAll(gen.generate(table, spec, cfg));
                }
            } catch (RuntimeException e) {
                throw new MojoExecutionException("生成 " + spec.name + " 失败：" + e.getMessage(), e);
            }
        }

        if (files.isEmpty()) {
            getLog().warn("没有生成任何文件");
            return;
        }

        Path out = ctx.resolve(cfg.output.dir);
        try {
            SourceWriter.WriteResult result = new SourceWriter(out, cfg).writeAll(files);
            for (Path p : result.written()) {
                getLog().info("  写入 " + projectDir.relativize(p));
            }
            for (Path p : result.skipped()) {
                getLog().info("  跳过（内容一致） " + projectDir.relativize(p));
            }
            if (!result.drifted().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("以下文件与生成结果不一致（write.policy=").append(cfg.write.policy).append("）：\n");
                for (Path p : result.drifted()) {
                    sb.append("  ").append(projectDir.relativize(p)).append('\n');
                }
                sb.append("确认要覆盖请加 -Dcodegen.writePolicy=overwrite。");
                throw new MojoFailureException(sb.toString());
            }
            getLog().info("完成：写入 " + result.written().size() + " 个文件，跳过 "
                    + result.skipped().size() + " 个 → " + out);
        } catch (IOException e) {
            throw new MojoExecutionException("写文件失败：" + e.getMessage(), e);
        }

        registerSourceRoot(out);
    }

    /**
     * 配置里指定了枚举类、但 classpath 上找不到时，生成骨架。
     *
     * <p>这是"枚举可以自己生成"的唯一入口：配一个还不存在的全限定名，插件补骨架，
     * 成员留给人填。已存在的枚举**绝不覆盖**。
     */
    private List<GeneratedFile> enumSkeletons(EntitySpec spec, Table table, ReadContext ctx) throws IOException {
        List<GeneratedFile> out = new ArrayList<>();
        for (var e : spec.enums.entrySet()) {
            String field = e.getKey();
            String fqn = e.getValue();
            if (ClasspathProbe.exists(ctx, fqn)) {
                continue;                       // 已存在，直接用
            }
            Column column = table.column(field);
            if (column == null) {
                throw new IllegalStateException("实体 " + table.entitySimpleName() + " 没有字段 " + field
                        + "（enums 里的键必须是字段名）；实际字段："
                        + table.bean().fields().stream().map(Field::name).toList());
            }
            Path target = ctx.sourceDir().resolve(fqn.replace('.', '/') + ".java");
            Path shown = ctx.projectDir().relativize(target);
            if (Files.exists(target)) {
                getLog().info("  枚举 " + fqn + " 已存在（" + shown + "），不覆盖；"
                        + "若编译报错请先 Build 让 class 生效");
                continue;
            }
            // 骨架写进源码树：之后由人补成员、长期维护，放 target 会被 clean 掉
            GeneratedFile skeleton = EnumSkeletonGenerator.skeleton(fqn, column);
            Files.createDirectories(target.getParent());
            Files.writeString(target, skeleton.content());
            getLog().info("  枚举 " + fqn + " 不存在 -> 已生成骨架 " + shown + "（请补成员后重新 Build）");
        }
        return out;
    }

    // ------------------------------------------------------------------ 实体选择

    /** 默认生成配置里的全部实体；{@code -Dcodegen.entity} 只用来临时挑一个。 */
    private List<EntitySpec> selectEntities(CodegenConfig cfg) {
        if (entity == null || entity.isBlank()) {
            return cfg.entities;
        }
        EntitySpec spec = cfg.entity(entity.trim());
        if (spec == null) {
            throw new IllegalArgumentException("配置的 entities 里没有 " + entity
                    + "；已配置：" + cfg.entities.stream().map(e -> e.name).toList());
        }
        return List.of(spec);
    }

    /** 把生成目录登记为编译源根，当轮即可参与编译。 */
    private void registerSourceRoot(Path out) {
        try {
            String path = out.toAbsolutePath().toString();
            if (!project.getCompileSourceRoots().contains(path)) {
                project.addCompileSourceRoot(path);
            }
        } catch (RuntimeException e) {
            getLog().warn("登记编译源根失败：" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ 配置

    private CodegenConfig loadConfig(Path projectDir) throws MojoExecutionException {
        try {
            String fromCli = System.getProperty("codegen.config");
            if (fromCli != null && !fromCli.isBlank()) {
                Path p = Path.of(fromCli);
                Path resolved = p.isAbsolute() ? p : projectDir.resolve(p);
                if (!Files.isRegularFile(resolved)) {
                    throw new MojoExecutionException("-Dcodegen.config 指向的文件不存在: " + resolved);
                }
                return ConfigLoader.load(resolved, projectDir, projectProps());
            }
            if (configFile != null && configFile.isFile()) {
                return ConfigLoader.load(configFile.toPath(), projectDir, projectProps());
            }
            for (String location : ConfigLoader.DEFAULT_LOCATIONS) {
                Path p = projectDir.resolve(location);
                if (Files.isRegularFile(p)) {
                    return ConfigLoader.load(p, projectDir, projectProps());
                }
            }
            throw new MojoExecutionException("找不到配置文件。请在模块目录放 codegen.yaml，"
                    + "或用 -Dcodegen.config=<路径> 指定。已查找：" + ConfigLoader.DEFAULT_LOCATIONS);
        } catch (IOException e) {
            throw new MojoExecutionException("读配置失败：" + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("配置有误：" + e.getMessage(), e);
        }
    }

    private Map<String, String> projectProps() {
        Map<String, String> props = new HashMap<>(project.getProperties().stringPropertyNames().stream()
                .collect(HashMap::new, (m, k) -> m.put(k, project.getProperties().getProperty(k)), HashMap::putAll));
        props.put("project.basedir", project.getBasedir().getAbsolutePath());
        props.put("project.build.directory", project.getBuild().getDirectory());
        props.put("project.build.outputDirectory", project.getBuild().getOutputDirectory());
        props.put("project.build.sourceDirectory", project.getBuild().getSourceDirectory());
        props.put("project.version", project.getVersion());
        props.put("project.groupId", project.getGroupId());
        props.put("project.artifactId", project.getArtifactId());
        return props;
    }

    private void applyOverrides(CodegenConfig cfg, Path projectDir) {
        if (writePolicy != null && !writePolicy.isBlank()) {
            cfg.write.policy = writePolicy.trim();
        }
        if (cfg.projectDir.isBlank()) {
            cfg.projectDir = projectDir.toString();
        }
    }

    private ReadContext readContext(Path projectDir) throws MojoExecutionException {
        Path classesDir = Path.of(project.getBuild().getOutputDirectory());
        List<Path> classpath = new ArrayList<>();
        for (String element : compileClasspathElements == null ? List.<String>of() : compileClasspathElements) {
            if (element != null && !element.isBlank()) {
                classpath.add(Path.of(element));
            }
        }
        if (classpath.size() <= 1) {
            try {
                for (String element : project.getCompileClasspathElements()) {
                    Path p = Path.of(element);
                    if (!classpath.contains(p)) {
                        classpath.add(p);
                    }
                }
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("解析编译期 classpath 失败：" + e.getMessage(), e);
            }
        }
        if (!classpath.contains(classesDir)) {
            classpath.add(0, classesDir);
        }
        return new ReadContext(projectDir, classesDir, classpath);
    }

    // ------------------------------------------------------------------ 日志

    private void logConfig(CodegenConfig cfg, ReadContext ctx) {
        getLog().info("配置来源：" + (cfg.configFile.isBlank() ? "（pom 内联）" : cfg.configFile));
        getLog().info("  实体=" + cfg.entities.stream().map(e -> e.name).toList()
                + "  write.policy=" + cfg.write.policy
                + "  idGenerator=" + (cfg.idGenerator.isBlank() ? "（无）" : cfg.idGenerator));
        getLog().info("  classesDir=" + ctx.classesDir() + "  classpath 项=" + ctx.classpath().size());
        getLog().info("  output.dir=" + cfg.output.dir + "  dto=" + cfg.output.dtoPackage
                + "  model=" + cfg.output.modelPackage + (cfg.output.byDomain ? ".<域>" : ""));
    }

    private void logTable(Table t, CodegenConfig cfg) {
        var pk = t.primaryKey();
        String domain = DomainNaming.domainOf(t.tableName(), t.entitySimpleName(), cfg);
        getLog().info("实体 " + t.entitySimpleName() + "（表 " + t.tableName() + "，列 " + t.columns().size()
                + "，主键 " + (pk == null ? "推不出" : pk.fieldName() + " : " + pk.javaType())
                + (domain.isBlank() ? "" : "，域 " + domain) + "）");
    }
}
package com.mds.codegen.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * 从 YAML 装载 {@link CodegenConfig}，并把 ${...} 占位符替换成本次构建的实际值。
 *
 * <p>占位符只做一层字符串替换：未定义的占位符原样保留（而不是抛错），
 * 这样用户把 Maven 属性写错时，报错信息里能直接看到没被替换的那段。
 */
public final class ConfigLoader {

    /** 默认查找顺序（相对模块目录）。 */
    public static final List<String> DEFAULT_LOCATIONS = List.of(
            "codegen/entities.yaml",
            "codegen.yaml",
            "src/main/resources/codegen.yaml");

    private ConfigLoader() {
    }

    /**
     * 读 YAML 文件。
     *
     * @param file       配置文件
     * @param projectDir 模块目录（相对路径解析基准）
     * @param props      可替换的键值（Maven 项目属性）
     */
    public static CodegenConfig load(Path file, Path projectDir, Map<String, String> props) throws IOException {
        return fromText(Files.readString(file), projectDir, props).withFile(file);
    }

    /** 读 YAML 文本（便于测试与 pom 内联）。 */
    public static CodegenConfig fromText(String text, Path projectDir, Map<String, String> props) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        String substituted = substitute(text, props);
        Object root = yaml.load(substituted);
        if (root != null && !(root instanceof Map)) {
            throw new IllegalArgumentException("配置文件根节点必须是映射");
        }
        Map<String, Object> map = root == null ? Map.of() : asMap(root);
        return CodegenConfig.fromMap(map).withProjectDir(projectDir);
    }

    /** 读 YAML（流形式）。 */
    public static CodegenConfig fromStream(InputStream in, Path projectDir, Map<String, String> props)
            throws IOException {
        return fromText(new String(in.readAllBytes(), StandardCharsets.UTF_8), projectDir, props);
    }

    /** 把 ${key} 替换成 props 里的值；未定义的保持原样。 */
    public static String substitute(String text, Map<String, String> props) {
        String open = String.valueOf((char) 36) + "{";
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf(open, i);
            if (start < 0) {
                out.append(text, i, text.length());
                break;
            }
            int end = text.indexOf('}', start + 2);
            if (end < 0) {
                out.append(text, i, text.length());
                break;
            }
            out.append(text, i, start);
            String key = text.substring(start + 2, end).trim();
            String value = props.get(key);
            out.append(value != null ? value : text.substring(start, end + 1));
            i = end + 1;
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object root) {
        return (Map<String, Object>) root;
    }
}

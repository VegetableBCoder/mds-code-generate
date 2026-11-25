package com.mds.codegen.gen;

/**
 * 一份生成结果（尚未落盘）。
 *
 * @param packageName 包名
 * @param simpleName  简单名
 * @param content     完整源码
 */
public record GeneratedFile(String packageName, String simpleName, String content) {

    public String relativePath() {
        return packageName.replace('.', '/') + "/" + simpleName + ".java";
    }

    public String fqcn() {
        return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
    }
}

package com.mds.codegen.model;

import java.util.List;

/**
 * 一个 Java 类型的引用（从源码或字节码读出来的最小信息）。
 *
 * @param qualifiedName 全限定名；基本类型写 {@code int}，数组写 {@code byte[]} / {@code Foo[]}
 * @param typeArguments 泛型实参（{@code List<String>} 的 {@code String}）
 */
public record TypeRef(String qualifiedName, List<TypeRef> typeArguments) {

    public static TypeRef of(String qualifiedName) {
        return new TypeRef(qualifiedName, List.of());
    }

    public boolean isString() {
        return "java.lang.String".equals(qualifiedName);
    }

    public boolean isShort() {
        return "java.lang.Short".equals(qualifiedName) || "short".equals(qualifiedName);
    }

    public boolean isLong() {
        return "java.lang.Long".equals(qualifiedName) || "long".equals(qualifiedName);
    }

    public boolean isByteArray() {
        return "byte[]".equals(qualifiedName);
    }

    public boolean isCollection() {
        return "java.util.List".equals(qualifiedName)
                || "java.util.Collection".equals(qualifiedName)
                || "java.util.Set".equals(qualifiedName);
    }

    @Override
    public String toString() {
        if (typeArguments.isEmpty()) {
            return qualifiedName;
        }
        return qualifiedName + "<"
                + String.join(", ", typeArguments.stream().map(TypeRef::toString).toList()) + ">";
    }
}

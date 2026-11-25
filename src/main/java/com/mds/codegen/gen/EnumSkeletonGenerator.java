package com.mds.codegen.gen;

import com.mds.codegen.model.DaoModel.Column;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

/**
 * 枚举**骨架**生成器：配置里给了枚举全限定名，但 classpath 上找不到它时，生成一个骨架。
 *
 * <h2>只出骨架，成员由人补</h2>
 * 成员（取值 + 文案）来自 DDL 的 CHECK 约束，插件不读 DDL，所以留空。
 * 生成器只保证 {@code value} 字段 + {@code fromValue} 方法这套约定——
 * converter 里会调 {@code fromValue} / {@code getValue}，骨架必须对得上。
 *
 * <h2>取值类型从实体列推导</h2>
 * 不单独配 valueType：列是 Short 就生成 {@code short value}，Integer 就生成 {@code int value}。
 * 这样配置与真实列类型不可能对不上。
 */
public final class EnumSkeletonGenerator {

    private EnumSkeletonGenerator() {
    }

    /**
     * @param fqn    枚举全限定名（来自配置）
     * @param column 该字段在表里的列（用来推导取值类型与生成注释）
     */
    public static GeneratedFile skeleton(String fqn, Column column) {
        String simple = fqn.substring(fqn.lastIndexOf('.') + 1);
        String pkg = fqn.contains(".") ? fqn.substring(0, fqn.lastIndexOf('.')) : "";
        ClassName self = ClassName.get(pkg, simple);
        TypeName valueType = valueTypeOf(column);

        TypeSpec.Builder type = TypeSpec.enumBuilder(simple)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("$L。\n", simple)
                .addJavadoc("\n<p>对应列 $L（$L）。取值与 DDL 的 CHECK 约束一一对应，一旦定下不可改。\n",
                        column.columnName(), column.jdbcType())
                .addJavadoc("\n<p>**骨架**：成员由人补，形如 {@code ACTIVE((short) 1)}；补完 Build 一次即可。\n")
                .addJavadoc("\n<p>已存在的枚举不会被覆盖，本文件只在这个类缺失时生成一次。\n")
                .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "Getter")).build())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "RequiredArgsConstructor")).build())
                .addEnumConstant("", TypeSpec.anonymousClassBuilder("").build())
                .addField(FieldSpec.builder(valueType, "value", Modifier.PRIVATE, Modifier.FINAL)
                        .addJavadoc("落库值（显式常量，绝不用 ordinal）。\n")
                        .build());

        type.addMethod(MethodSpec.methodBuilder("fromValue")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(self)
                .addJavadoc("从落库值还原；认不出的值抛错。\n")
                .addParameter(valueType, "value")
                .beginControlFlow("for ($T item : values())", self)
                .beginControlFlow("if (item.value == value)")
                .addStatement("return item")
                .endControlFlow()
                .endControlFlow()
                .addStatement("throw new $T($S + value)", IllegalArgumentException.class,
                        "未知的 " + simple + " 取值: ")
                .build());

        String content = JavaFile.builder(pkg, type.build())
                .skipJavaLangImports(true)
                .build()
                .toString();
        return new GeneratedFile(pkg, simple, content);
    }

    /** 列类型 → 枚举取值类型。 */
    static TypeName valueTypeOf(Column column) {
        String name = column.javaType().qualifiedName();
        return switch (name) {
            case "java.lang.Short", "short" -> TypeName.SHORT;
            case "java.lang.Integer", "int" -> TypeName.INT;
            case "java.lang.Long", "long" -> TypeName.LONG;
            case "java.lang.String" -> ClassName.get(String.class);
            default -> throw new IllegalStateException("枚举列 " + column.fieldName() + " 的类型是 " + name
                    + "，不支持生成枚举（只支持 short / int / long / String）");
        };
    }
}
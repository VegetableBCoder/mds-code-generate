package com.mds.codegen.gen;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.config.EntitySpec;
import com.mds.codegen.model.DaoModel.Column;
import com.mds.codegen.model.DaoModel.Table;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * MapStruct converter 生成器（entity ↔ dto）。
 *
 * <p>枚举转换写成双向的两个 default 方法：业务层拿到枚举，落库时取回原始值。
 * 枚举自身需要提供 {@code fromValue(short)} 与 {@code getValue()}。
 */
public final class ConverterGenerator implements Generator {

    @Override
    public String part() {
        return "convert";
    }

    @Override
    public List<GeneratedFile> generate(Table table, EntitySpec spec, CodegenConfig cfg) {
        String entityName = table.entitySimpleName();
        String dtoName = entityName + cfg.naming.dtoSuffix;
        String converterName = entityName + cfg.naming.convertSuffix;
        ClassName entity = ClassName.bestGuess(table.beanQualifiedName());
        ClassName dto = ClassName.get(cfg.output.dtoPackage, dtoName);

        TypeSpec.Builder type = TypeSpec.interfaceBuilder(converterName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("$L 的 entity ↔ dto 转换（MapStruct，实现由注解处理器生成）。\n", entityName)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.mapstruct", "Mapper"))
                        .addMember("componentModel", "$S", "spring")
                        .build());

        type.addMethod(MethodSpec.methodBuilder("toDto")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(dto)
                .addParameter(entity, "entity")
                .build());
        type.addMethod(MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(entity)
                .addJavadoc("反向转换：DTO → 实体（落库用）。\n")
                .addAnnotation(AnnotationSpec.builder(
                                ClassName.get("org.mapstruct", "InheritInverseConfiguration"))
                        .addMember("name", "$S", "toDto")
                        .build())
                .addParameter(dto, "dto")
                .build());

        for (Column col : table.columns()) {
            String enumFqn = spec.enums.get(col.fieldName());
            if (enumFqn == null) {
                continue;
            }
            ClassName enumType = ClassName.bestGuess(enumFqn);
            String suffix = Naming.capitalize(col.fieldName());
            TypeName valueType = TypeMapper.toTypeName(col.javaType(), "枚举列 " + col.fieldName());

            type.addMethod(MethodSpec.methodBuilder("map" + suffix)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(enumType)
                    .addJavadoc("$L：库存值 → $L。未知值抛错。\n", col.columnName(), enumType.simpleName())
                    .addParameter(valueType, "value")
                    .addStatement("return value == null ? null : $T.fromValue(value)", enumType)
                    .build());
            type.addMethod(MethodSpec.methodBuilder("map" + suffix + "Value")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(valueType)
                    .addJavadoc("$L：$L → 库存值。\n", col.columnName(), enumType.simpleName())
                    .addParameter(enumType, "value")
                    .addStatement("return value == null ? null : value.getValue()")
                    .build());
        }

        String content = JavaFile.builder(cfg.output.convertPackage, type.build())
                .skipJavaLangImports(true)
                .build()
                .toString();
        return List.of(new GeneratedFile(cfg.output.convertPackage, converterName, content));
    }
}

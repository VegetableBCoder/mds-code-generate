package com.mds.codegen.gen;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.config.EntitySpec;
import com.mds.codegen.model.DaoModel.Column;
import com.mds.codegen.model.DaoModel.Table;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/**
 * model-service **实现**生成器。
 *
 * <h2>主键生成</h2>
 * 只有当配置了 {@code idGenerator} **且**主键是 {@code id : Long} 时，才在 insert 前调
 * {@code idGenerator.nextId()}。其它形态（业务主键）由调用方给。
 *
 * <h2>加自定义方法</h2>
 * 直接写进这个类，之后靠 git 对比/回滚；不想再被覆盖就把 {@code write.policy} 设为 {@code skip}。
 * {@code mapper} / {@code converter} / {@code idGenerator} 都是 {@code protected}。
 */
public final class ServiceImplGenerator implements Generator {

    @Override
    public String part() {
        return "model";
    }

    @Override
    public List<GeneratedFile> generate(Table table, EntitySpec spec, CodegenConfig cfg) {
        Column pk = ServiceInterfaceGenerator.requirePrimaryKey(table);
        String domain = DomainNaming.domainOf(table.tableName(), table.entitySimpleName(), cfg);
        String entityName = table.entitySimpleName();
        String dtoSimple = entityName + cfg.naming.dtoSuffix;
        String serviceName = entityName + cfg.naming.serviceSuffix;
        String implName = entityName + cfg.naming.implSuffix;
        String servicePkg = DomainNaming.servicePackage(domain, cfg);
        String implPkg = DomainNaming.implPackage(domain, cfg);

        ClassName dto = ClassName.get(cfg.output.dtoPackage, dtoSimple);
        ClassName entity = ClassName.bestGuess(table.beanQualifiedName());
        ClassName mapper = ClassName.bestGuess(table.mapperQualifiedName());
        ClassName support = ClassName.bestGuess(table.supportQualifiedName());
        ClassName converter = ClassName.get(cfg.output.convertPackage, entityName + cfg.naming.convertSuffix);
        ClassName service = ClassName.get(servicePkg, serviceName);
        ClassName sqlBuilder = ClassName.bestGuess("org.mybatis.dynamic.sql.SqlBuilder");
        TypeName pkType = TypeMapper.toTypeName(pk.javaType(), "主键 " + pk.fieldName());
        TypeName pkBoxed = ServiceInterfaceGenerator.box(pkType);
        boolean generateId = shouldGenerateId(pk, cfg);
        ClassName idGenerator = generateId ? ClassName.bestGuess(cfg.idGenerator) : null;

        TypeSpec.Builder type = TypeSpec.classBuilder(implName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(service)
                .addJavadoc("$L 的实现。\n", serviceName)
                .addJavadoc("\n<p>领域方法可直接加在本类里；重生成前用 git 对比/回滚，"
                        + "或把 write.policy 设为 skip。\n")
                .addAnnotation(AnnotationSpec.builder(
                        ClassName.get("org.springframework.stereotype", "Service")).build())
                .addField(FieldSpec.builder(mapper, "mapper", Modifier.PROTECTED, Modifier.FINAL).build())
                .addField(FieldSpec.builder(converter, "converter", Modifier.PROTECTED, Modifier.FINAL).build());
        if (generateId) {
            type.addField(FieldSpec.builder(idGenerator, "idGenerator",
                    Modifier.PROTECTED, Modifier.FINAL).build());
        }

        MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mapper, "mapper")
                .addParameter(converter, "converter");
        if (generateId) {
            ctor.addParameter(idGenerator, "idGenerator")
                    .addStatement("this.idGenerator = idGenerator");
        }
        ctor.addStatement("this.mapper = mapper")
                .addStatement("this.converter = converter");
        type.addMethod(ctor.build());

        MethodSpec.Builder insert = MethodSpec.methodBuilder("insert")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .addParameter(dto, "dto");
        if (generateId) {
            insert.beginControlFlow("if (dto.$L() == null)", Naming.getter(pk.fieldName()))
                    .addStatement("dto.$L(idGenerator.nextId())", Naming.setter(pk.fieldName()))
                    .endControlFlow();
        }
        insert.addStatement("mapper.insert(converter.toEntity(dto))");
        type.addMethod(insert.build());

        MethodSpec.Builder batch = MethodSpec.methodBuilder("insertBatch")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), dto), "dtos");
        if (generateId) {
            batch.beginControlFlow("for ($T dto : dtos)", dto)
                    .beginControlFlow("if (dto.$L() == null)", Naming.getter(pk.fieldName()))
                    .addStatement("dto.$L(idGenerator.nextId())", Naming.setter(pk.fieldName()))
                    .endControlFlow()
                    .endControlFlow();
        }
        batch.addStatement("mapper.insertMultiple(dtos.stream().map(converter::toEntity).toList())");
        type.addMethod(batch.build());

        type.addMethod(MethodSpec.methodBuilder("findById")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), dto))
                .addParameter(pkType, "id")
                .addStatement("return mapper.selectByPrimaryKey(id).map(converter::toDto)")
                .build());

        type.addMethod(MethodSpec.methodBuilder("findByIds")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), dto))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Collection.class), pkBoxed), "ids")
                .beginControlFlow("if (ids == null || ids.isEmpty())")
                .addStatement("return $T.of()", List.class)
                .endControlFlow()
                .addStatement("return mapper.select(c -> c.where($T.$L, $T.isIn(ids))).stream()"
                        + ".map(converter::toDto).toList()", support, pk.fieldName(), sqlBuilder)
                .build());

        for (String[] pair : new String[][]{
                {"updateById", "updateByPrimaryKey"},
                {"updateSelectiveById", "updateByPrimaryKeySelective"}}) {
            type.addMethod(MethodSpec.methodBuilder(pair[0])
                    .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.INT)
                    .addParameter(pkType, "id")
                    .addParameter(dto, "dto")
                    .addStatement("$T entity = converter.toEntity(dto)", entity)
                    .addStatement("entity.$L(id)", Naming.setter(pk.fieldName()))
                    .addStatement("return mapper.$L(entity)", pair[1])
                    .build());
        }

        type.addMethod(MethodSpec.methodBuilder("deleteById")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(pkType, "id")
                .addStatement("return mapper.deleteByPrimaryKey(id)")
                .build());
        type.addMethod(MethodSpec.methodBuilder("deleteByIds")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Collection.class), pkBoxed), "ids")
                .beginControlFlow("if (ids == null || ids.isEmpty())")
                .addStatement("return 0")
                .endControlFlow()
                .addStatement("return mapper.delete(c -> c.where($T.$L, $T.isIn(ids)))",
                        support, pk.fieldName(), sqlBuilder)
                .build());

        String content = JavaFile.builder(implPkg, type.build())
                .skipJavaLangImports(true)
                .build()
                .toString();
        return List.of(new GeneratedFile(implPkg, implName, content));
    }

    /**
     * 是否自动生成主键：配了 idGenerator **且**主键字段名是 {@code id} **且**类型是 {@code Long}。
     */
    static boolean shouldGenerateId(Column pk, CodegenConfig cfg) {
        return cfg.idGenerator != null && !cfg.idGenerator.isBlank()
                && "id".equals(pk.fieldName())
                && pk.javaType().isLong();
    }
}

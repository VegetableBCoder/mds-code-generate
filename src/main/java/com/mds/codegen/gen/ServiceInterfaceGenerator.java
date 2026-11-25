package com.mds.codegen.gen;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.config.EntitySpec;
import com.mds.codegen.model.DaoModel.Column;
import com.mds.codegen.model.DaoModel.Table;
import com.palantir.javapoet.ClassName;
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
 * model-service **接口**生成器。
 *
 * <p>基线 CRUD 八个方法：{@code insert / insertBatch / findById / findByIds /
 * updateById / updateSelectiveById / deleteById / deleteByIds}。
 * {@code findById} **固定返回 {@code Optional}**——强制调用方处理"查不到"。
 */
public final class ServiceInterfaceGenerator implements Generator {

    @Override
    public String part() {
        return "model";
    }

    @Override
    public List<GeneratedFile> generate(Table table, EntitySpec spec, CodegenConfig cfg) {
        Column pk = requirePrimaryKey(table);
        String domain = DomainNaming.domainOf(table.tableName(), table.entitySimpleName(), cfg);
        String entityName = table.entitySimpleName();
        String dtoSimple = entityName + cfg.naming.dtoSuffix;
        String serviceName = entityName + cfg.naming.serviceSuffix;
        String servicePkg = DomainNaming.servicePackage(domain, cfg);

        ClassName dto = ClassName.get(cfg.output.dtoPackage, dtoSimple);
        TypeName pkType = TypeMapper.toTypeName(pk.javaType(), "主键 " + pk.fieldName());
        TypeName pkBoxed = box(pkType);
        TypeName optionalDto = ParameterizedTypeName.get(ClassName.get(Optional.class), dto);

        TypeSpec.Builder type = TypeSpec.interfaceBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("$L 的 model-service。\n", entityName)
                .addJavadoc("\n<p>按主键的基线 CRUD；带业务条件的领域操作另写接口或直接写在实现类里。\n");

        type.addMethod(MethodSpec.methodBuilder("insert")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("单条插入。\n")
                .addParameter(dto, "dto")
                .build());
        type.addMethod(MethodSpec.methodBuilder("insertBatch")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("批量插入（不分批）。\n")
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), dto), "dtos")
                .build());
        type.addMethod(MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("按主键查单个。\n")
                .returns(optionalDto)
                .addParameter(pkType, "id")
                .build());
        type.addMethod(MethodSpec.methodBuilder("findByIds")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("按主键批量查。\n")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), dto))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Collection.class), pkBoxed), "ids")
                .build());
        type.addMethod(MethodSpec.methodBuilder("updateById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("按主键写**全部业务列**（主键除外）。\n")
                .returns(TypeName.INT)
                .addParameter(pkType, "id")
                .addParameter(dto, "dto")
                .build());
        type.addMethod(MethodSpec.methodBuilder("updateSelectiveById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("按主键只写**非 null 列**。\n")
                .returns(TypeName.INT)
                .addParameter(pkType, "id")
                .addParameter(dto, "dto")
                .build());
        type.addMethod(MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("按主键删除，返回影响行数。\n")
                .returns(TypeName.INT)
                .addParameter(pkType, "id")
                .build());
        type.addMethod(MethodSpec.methodBuilder("deleteByIds")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("按主键批量删除，返回影响行数。\n")
                .returns(TypeName.INT)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Collection.class), pkBoxed), "ids")
                .build());

        String content = JavaFile.builder(servicePkg, type.build())
                .skipJavaLangImports(true)
                .build()
                .toString();
        return List.of(new GeneratedFile(servicePkg, serviceName, content));
    }

    static TypeName box(TypeName t) {
        return t.isPrimitive() ? t.box() : t;
    }

    static Column requirePrimaryKey(Table table) {
        Column pk = table.primaryKey();
        if (pk == null) {
            throw new IllegalStateException("表 " + table.tableName()
                    + " 的主键推不出来（mapper 上没有 @Result(id = true)）。"
                    + "生成 model-service 需要主键；请先修好 MBG 产物，或只生成 dto/convert。");
        }
        return pk;
    }
}

package com.mds.codegen.gen;

import com.mds.codegen.config.CodegenConfig;
import com.mds.codegen.config.EntitySpec;
import com.mds.codegen.model.DaoModel.Field;
import com.mds.codegen.model.DaoModel.Table;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * DTO 生成器。
 *
 * <pre>
 *   &#64;Data &#64;NoArgsConstructor &#64;AllArgsConstructor &#64;Builder
 *   public class BuildDto {
 *       private String apkSha256;
 *       private BuildParseStatusEnum parseStatus;   // 枚举列换成枚举类型
 *   }
 * </pre>
 *
 * <p>Lombok 是**固定**的（不做开关）：目标场景就是 Lombok 项目，少一个开关少一处能配错的地方。
 */
public final class DtoGenerator implements Generator {

    @Override
    public String part() {
        return "dto";
    }

    @Override
    public List<GeneratedFile> generate(Table table, EntitySpec spec, CodegenConfig cfg) {
        String dtoName = table.entitySimpleName() + cfg.naming.dtoSuffix;
        TypeSpec.Builder type = TypeSpec.classBuilder(dtoName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("$L 的 DTO。\n", table.entitySimpleName())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "Data")).build())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "NoArgsConstructor")).build())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "AllArgsConstructor")).build())
                .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "Builder")).build());

        for (Field f : table.bean().fields()) {
            type.addField(FieldSpec.builder(fieldType(table, f, spec), f.name(), Modifier.PRIVATE).build());
        }

        String content = JavaFile.builder(cfg.output.dtoPackage, type.build())
                .skipJavaLangImports(true)
                .build()
                .toString();
        return List.of(new GeneratedFile(cfg.output.dtoPackage, dtoName, content));
    }

    private TypeName fieldType(Table table, Field f, EntitySpec spec) {
        String enumFqn = spec.enums.get(f.name());
        if (enumFqn != null) {
            return ClassName.bestGuess(enumFqn);
        }
        return TypeMapper.toTypeName(f.type(), table.entitySimpleName() + "." + f.name());
    }
}

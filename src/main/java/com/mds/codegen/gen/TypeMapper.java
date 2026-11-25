package com.mds.codegen.gen;

import com.mds.codegen.model.TypeRef;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link TypeRef}（读取层产出的类型）→ JavaPoet 的 {@link TypeName}。
 *
 * <p>基本类型与常见 JDK 类型走白名单；其余按普通类处理（实体、枚举、业务类型都能过）。
 * 唯一拒绝的是"名字里带尖括号"的嵌套泛型——那说明上游解析出了问题，不该由这里猜。
 */
public final class TypeMapper {

    private static final Map<String, TypeName> PRIMITIVES = Map.of(
            "int", TypeName.INT,
            "long", TypeName.LONG,
            "short", TypeName.SHORT,
            "byte", TypeName.BYTE,
            "boolean", TypeName.BOOLEAN,
            "char", TypeName.CHAR,
            "float", TypeName.FLOAT,
            "double", TypeName.DOUBLE,
            "void", TypeName.VOID);

    private TypeMapper() {
    }

    /** 转换；不认识的抛 {@link IllegalArgumentException} 并说明是哪个字段。 */
    public static TypeName toTypeName(TypeRef ref, String where) {
        String name = ref.qualifiedName();
        if ("byte[]".equals(name)) {
            return ArrayTypeName.of(TypeName.BYTE);
        }
        TypeName primitive = PRIMITIVES.get(name);
        if (primitive != null) {
            return primitive;
        }
        if (name.endsWith("[]")) {
            return ArrayTypeName.of(toTypeName(TypeRef.of(name.substring(0, name.length() - 2)), where));
        }
        if (ref.isCollection() || name.equals("java.util.Map")) {
            if (ref.typeArguments().isEmpty()) {
                throw new IllegalArgumentException("集合类型缺少类型实参：" + ref + "（位置：" + where + "）");
            }
            TypeName[] args = ref.typeArguments().stream()
                    .map(a -> toTypeName(a, where)).toArray(TypeName[]::new);
            return ParameterizedTypeName.get(ClassName.bestGuess(name), args);
        }
        if (!ref.typeArguments().isEmpty()) {
            TypeName[] args = ref.typeArguments().stream()
                    .map(a -> toTypeName(a, where)).toArray(TypeName[]::new);
            return ParameterizedTypeName.get(ClassName.bestGuess(name), args);
        }
        if (name.contains("<")) {
            throw new IllegalArgumentException("不支持的嵌套泛型：" + ref + "（位置：" + where + "）");
        }
        return ClassName.bestGuess(name);
    }

    /** 把类型递归展开成全限定名列表（便于收集 import）。 */
    public static List<String> referencedClassNames(TypeRef ref) {
        List<String> out = new ArrayList<>();
        collect(ref, out);
        return out;
    }

    private static void collect(TypeRef ref, List<String> out) {
        String name = ref.qualifiedName();
        if (!PRIMITIVES.containsKey(name) && !"byte[]".equals(name) && !name.endsWith("[]")) {
            out.add(name);
        }
        for (TypeRef arg : ref.typeArguments()) {
            collect(arg, out);
        }
    }
}

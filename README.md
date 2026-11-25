# mds-code-generate

读 **MyBatis Generator（`targetRuntime = MyBatis3DynamicSql`）** 产出的
`entity` / `mapper` / `DynamicSqlSupport`，生成 DTO / MapStruct converter / model-service。

## 用法（IDEA 里点一下就行）

1. **配置**：模块根目录放 `codegen.yaml`（逐项说明见 [codegen.yaml](codegen.yaml)）；
2. **Build**：IDEA 里 Build 一次（或 `mvn compile`）——插件读的是**已编译的 class**；
3. **生成**：IDEA Maven 面板 → ~Lifecycle~ 之外的 ~Plugins~ → ~codegen~ → **~codegen:generate`**
   （或命令行 ~mvn codegen:generate`）。

> 第 2 步是硬要求：插件不看源码，看 `target/classes` 里的类。
> IDEA 的 Build 会把 class 编出来，所以"Build 一次 → 点 generate"就够。

实体清单写在 yaml 里，**不需要每次带 `-Dcodegen.entity`**。

## 生成物

| 生成物 | 说明 |
|---|---|
| `dto/<X>Dto.java` | `@Data @NoArgsConstructor @AllArgsConstructor @Builder` |
| `convert/<X>Converter.java` | `@Mapper(componentModel = "spring")` + 枚举双向转换 |
| `model/[<域>/]<X>ModelService.java` | 基线 CRUD 接口，`findById` 固定返回 `Optional` |
| `model/[<域>/]impl/<X>ModelServiceImpl.java` | 实现（`@Service`，字段 `protected`） |

全部写到 `output.dir`（默认 `target/generated-sources/codegen`）。

## 最小配置

```yaml
source:
  entityPackage: "com.example.demo.entity"     # ← 改成你自己项目的包名
  mapperPackage: "com.example.demo.mapper"

output:
  dtoPackage: "com.example.demo.dto"
  convertPackage: "com.example.demo.convert"
  modelPackage: "com.example.demo.model"
  byDomain: false

idGenerator: "com.example.demo.common.IdGenerator"   # 可选

entities:
  - name: AppUser
    enums:
      status: "com.example.demo.domain.user.UserStatusEnum"
```

> ⚠️ 上面全是 `com.example.demo` 占位包名。抄过去必须改成**本项目实际包名**，
> 否则报 `实体 class 目录不存在: .../com/example/demo/entity`。

## 生成到真实代码里（提交进 git）

**只改一个配置**：

```yaml
output:
  dir: "src/main/java"
```

其它都不用动。区别：

| | `target/generated-sources/codegen`（默认） | `src/main/java` |
|---|---|---|
| 是否进 git | 否 | **是** |
| `mvn clean` | 删掉，重跑插件再生成 | 不删 |
| 每次构建跑插件 | 要 | **不用**，改表/配置才重跑 |
| 还要 build-helper | 要 | **不要** |

> `write.policy` 建议保持 `failOnDrift`：生成结果与现有文件不一致时先失败，
> `git diff` 确认后再用 `-Dcodegen.writePolicy=overwrite` 覆盖一次。
> 这样别人的手改不会被静默冲掉。

## 枚举：两条路径

在 `entities[].enums` 里写 **字段名 → 枚举全限定名**，插件按"这个类存不存在"自己分：

| 情况 | 插件行为 |
|---|---|
| 枚举**已存在** | 直接用：DTO 字段换成枚举类型，converter 生成双向 `mapXxx` / `mapXxxValue` |
| 枚举**不存在** | 在 `src/main/java` 下生成骨架，只含 `value` 字段 + `fromValue()`，**成员由人补** |

_注意：骨架写在源码树（不是 target）——它之后由人长期维护，放 target 会被 clean 掉。已存在的枚举绝不覆盖。_

### 走一遍

```yaml
entities:
  - name: AppUser
    enums:
      status: "com.example.demo.domain.user.UserStatusEnum"   # 还不存在？
```

点 `codegen:generate`：

```
[INFO]   枚举 com.example.demo.domain.user.UserStatusEnum 不存在 -> 已生成骨架 src/main/java/.../UserStatusEnum.java（请补成员后重新 Build）
[INFO]   写入 .../AppUserDto.java
```

生成的骨架：

```java
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum {
  ;                                    // ← 补成员：NORMAL((short) 1), LOCKED((short) 2);

  private final short value;           // ← 类型从实体列推导（SMALLINT → short）

  public static UserStatusEnum fromValue(short value) {
    for (UserStatusEnum item : values()) {
      if (item.value == value) return item;
    }
    throw new IllegalArgumentException("未知的 UserStatusEnum 取值: " + value);
  }
}
```

补完成员再 Build 一次就通了。**再点 generate 会把枚举认作已存在**，不会重复生成：

```
[INFO]   完成：写入 0 个文件，跳过 4 个
```

> 取值类型（`short` / `int` / `long` / `String`）从实体列类型推导，不单独配置。

## 固定项（不做开关）

| 项 | 值 |
|---|---|
| Lombok | `@Data @NoArgsConstructor @AllArgsConstructor @Builder` |
| MapStruct | `@Mapper(componentModel = "spring")` |
| Spring | 实现类 `@Service` |
| `findById` | 固定返回 `Optional` |
| 枚举 | 配置里给全限定名；不存在时生成骨架（见下） |

理由：目标场景就一种技术栈。多一个开关就多一处能配错的地方。

## 为什么报错（最常见的两种）

| 现象 | 原因 | 解决 |
|---|---|---|
| `实体 class 目录不存在: .../com/example/demo/entity` | 抄了示例配置，占位包名没改 | 改成项目实际包名（报错里会列出实际存在的包） |
| 同上，但包名是对的 | 还没编译 | 先 Build / `mvn compile` |
| `未知配置项 output.xxx` | 用了已移除的旧键 | 报错里会写明替代方案 |

## 命令行覆盖项

| 参数 | 作用 |
|---|---|
| `-Dcodegen.config` | 配置文件路径（优先于 pom 的 configFile） |
| `-Dcodegen.entity` | 只处理某一个实体（调试用，可选） |
| `-Dcodegen.writePolicy` | `overwrite / skip / failOnDrift` |

## 设计取舍

1. **读取方式：加载类后反射**。要求先编译；换来元数据绝对准确——
   主键读 `@Result(id = true)`，列类型读 `SqlColumn<T>` 的泛型实参。
2. **实体清单写在配置文件**，命令行默认不带参数。
3. **写盘策略只有一处实现**（`SourceWriter`）：`overwrite / skip / failOnDrift`。
4. **未知配置项直接报错**，并指明替代方案。

## 加自定义方法

直接写进生成的实现类，之后靠 git 对比/回滚；不想被覆盖就把 `write.policy` 设为 `skip`。
`mapper` / `converter` / `idGenerator` 都是 `protected`。

## 仓库约定

- `target/`、`.idea/`、`*.iml` 不进仓库（见 [.gitignore](.gitignore)）；
- **数据库口令不进仓库**：用 `DB_PASSWORD` 环境变量提供，见 [it/README.md](it/README.md)。

## 模块

```
codegen-maven-plugin/
├── codegen.yaml                     # 配置示例（逐项注释）
├── docs/research-access-layer-plugin.md
├── src/main/java/com/mds/codegen/
│   ├── CodegenMojo.java
│   ├── config/                      # YAML 装载 + 校验
│   ├── model/                       # Table / Bean / Column / TypeRef
│   ├── read/                        # ReflectionReader
│   └── gen/                         # 4 个生成器 + SourceWriter
└── it/                              # 链路验证（MBG + 本插件 + 真实 MySQL + 集成测试）
```

## 已知边界

- **只支持 MBG 的 `MyBatis3DynamicSql` 产物形态**；
- **主键必须单字段**：联合主键直接报错；
- 自动生成主键的条件是「主键字段名 = `id` **且** 类型 = `Long`」。
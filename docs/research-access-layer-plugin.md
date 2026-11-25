# access-layer-plugin 调研报告

> 文档位置：`docs/research-access-layer-plugin.md`（本仓工作区）。下文行号均指 `access-layer-plugin/src/main/java/...`。

> 对象：`/usr/local/projects/agent/auto-test/codegen/access-layer-plugin`（Maven 插件，2639 行 Java + 109 行 pom，0 个测试）
> 目的：搞清它如何「读 MyBatis Dynamic SQL 产物 → 生成 DTO / 枚举 / Converter / ModelService」，并评估改造成**通用代码生成插件**的差距。
> 方式：通读全部源码 + 在真实工程 `business-server-core` 上实跑 generate / scanOnly 各分支（记录见 §8）。

---

## 1. 结论摘要

一个 `maven-plugin`（`com.autotest:access-layer-plugin:1.0.0-SNAPSHOT`，goal `generate`），
**不连数据库、不读 DDL、不读源码**，只解析已编译的 `.class`（ASM），把 MyBatis Generator
（`targetRuntime=MyBatis3DynamicSql`）产出的三件套读成内存表模型，再用 JavaPoet 生成 4 类文件：

| 输入（MBG 产物） | 输出（本插件生成） |
|---|---|
| `entity/<X>.class` | `dto/<X>Dto.java`（Lombok） |
| `mapper/<X>DynamicSqlSupport.class` + 内部类 | `convert/<X>Converter.java`（MapStruct） |
| `mapper/<X>Mapper.class` | `domain/<域>/<枚举>Enum.java`（骨架） |
| | `model/<域>/<X>ModelService.java` + `impl/<X>ModelServiceImpl.java` |

实测 17 张表全通：17 表 / 12 个枚举列 / 17 DTO + 17 Converter + 34 model 文件。

**通用化判定**：分层干净（read / model / config / gen 四包），**可以抽出来**；但离"通用"还差三块硬骨头：

1. **输入契约钉死在 MBG + Dynamic SQL 2.0.0 的产物形态上**（类名后缀、内部类、合成 lambda 名、`SqlColumn` 描述符）；
2. **生成物形态全是硬编码模板**（Lombok、MapStruct、Spring `@Service`、`Optional`、`insertMultiple`、`IdGenerator`），无模板/SPI 层；
3. **配置只有一张平铺 properties，没有 per-entity 结构**，且 3 个配置项声明了却没接线，1 处后缀硬编码。

外加实测发现 2 个**会破坏现有代码**的缺陷（§8.4 / §8.5）。

---

## 2. 目录结构

```
codegen/access-layer-plugin/
├── pom.xml                      # packaging=maven-plugin，goalPrefix=autotest
├── mvnw / .mvn/                 # 自带 wrapper（Maven 3.9.16）
└── src/main/java/com/autotest/codegen/
    ├── GenerateMojo.java        (376)  唯一 Mojo：编排 + 配置覆盖 + 日志
    ├── config/
    │   ├── PluginConfig.java    (362)  properties 装载、XML/-D 覆盖、默认值
    │   └── DomainMapping.java   ( 59)  「表名 → 域子包」，17 条默认映射写死
    ├── read/
    │   ├── DaoScanner.java      (123)  目录扫描 + 三件套关联 + 过滤
    │   └── BytecodeReader.java  (470)  ASM 解析（字段/注解/合成 lambda/常量池）
    ├── model/
    │   └── DaoModel.java        (112)  record 模型：Bean/Field/Column/Table/TypeRef
    └── gen/
        ├── TypeMapper.java      ( 98)  TypeRef → JavaPoet TypeName
        ├── EnumIndex.java       (175)  枚举索引 + 候选名推导
        ├── DtoGenerator.java    (120)
        ├── ConverterGenerator.java (150)
        ├── EnumGenerator.java   (196)
        └── ModelServiceGenerator.java (398)
```

`src/test/java` 为空。依赖面很小：`maven-plugin-api`/`maven-core`（provided）、`asm 9.10.1`、
`com.palantir.javapoet 0.5.0`、`lombok`（provided）——**没有** mybatis / spring / mapstruct，因为只读字节码、只写源码。

---

## 3. 端到端流程

```
GenerateMojo.execute()                                        GenerateMojo.java:162
 │
 ├─① 读配置 PluginConfig.fromProperties(configFile, projectDir)        :166
 │         默认 access-layer.properties（不存在则全走内置默认）
 ├─② 覆盖   overrideAll(cfg)  只覆盖"显式给了"的 pom <configuration> / -D  :172, :317
 ├─③ 定位   classesRoot = ${project.build.outputDirectory}
 │         entityDir = classesRoot/<basePackage>/<entityPackage>        :178
 │         mapperDir = classesRoot/<basePackage>/<mapperPackage>        :180
 │         outputDir = <classesRoot 父>/generated-sources/autotest       :183
 ├─④ 扫描   DaoScanner.scan(...) → List<Table>                          :187
 ├─⑤ 过滤   generateBeans（实体简单名白名单；空=全部）                    :198
 ├─⑥ 枚举   EnumIndex.build(<classesRoot>/<domainPackage>, ...)         :231
 │         + 每表 resolveEnums(table, index) → Map<字段名, 枚举FQN>      :236
 ├─⑦ 生成   按 generateParts（空=全部）：dto / convert / enum / model     :225
 └─⑧ 登记   project.addCompileSourceRoot(out)                           :306
```

命令：`../mvnw com.autotest:access-layer-plugin:1.0.0-SNAPSHOT:generate`（可加 `-Dautotest.codegen.scanOnly=true`）。

---

## 4. 读取层：三个 class 拼成一张表

`DaoScanner.scan` 按 **MBG 命名约定**关联，不反射、不加载类：

| 目标 | 来源 class | 解析内容 | 实现 |
|---|---|---|---|
| 实体 | `entity/<X>.class` | 字段名 + Java 类型（含泛型/数组） | `BytecodeReader.readFields` :39 |
| 表名 | `mapper/<X>DynamicSqlSupport$<X>.class`（**内部类**） | `AliasableSqlTable` 构造器里的 `ldc` | `readSupport` :212 |
| 列名 + JDBC 类型 | 同上 | `column(String, JDBCType)` 的常量对，按**调用顺序** | 同上 |
| 列 Java 类型 | 内部类静态字段 | `SqlColumn<T>` 签名里的 T | `readSupportColumns` :278 |
| **主键字段** | `mapper/<X>Mapper.class` | `lambda$selectByPrimaryKey$0` / `lambda$deleteByPrimaryKey$0` 里的 `getstatic ...DynamicSqlSupport.<列>` | `readPrimaryKeyField` :165 |

隐式 ABI 两条（MBG 改版即可能崩）：
- 「列名与 `SqlColumn` 字段**按位对齐**」——实测调用顺序 == 字段声明顺序；
- 「主键只能从合成 lambda 里取」——按类型匹配必然歧义（`Long` 主键与 `Long applicationId` 同型）。

实测 17 张表主键全部推出：13 张 `Long id`，另有 `app_config.applicationId`、`build.apkSha256`、
`device.uuid`、`lease.leaseId` 四种非 `id`/非 `Long` 形态——正是"不能写死 `long id`"的实证。

字节码坑（源码注释已记录，均属实）：`byte[]` 描述符是 `[B` 不能当类名；泛型变量 `T` 退化为 `Object`；
表名只在内部类里；主键不能按类型猜。

> 待改进：`readSupport` 的 `visitMethod` 注释写"只看构造器"，实现**不判断方法名**（:222），靠巧合工作。

---

## 5. 内存模型

```java
record Table(Bean bean, String tableName, List<Column> columns, String primaryKeyField)
record Bean(String packageName, String simpleName, List<Field> fields)
record Field(String name, TypeRef type)
record Column(String fieldName, String columnName, TypeRef javaType, String jdbcType)
record TypeRef(String qualifiedName, List<TypeRef> typeArguments)
```

**关键点**：`Table` 只承载结构，不承载任何生成策略（包名/后缀/域/枚举）。这个切分对通用化有利：
只要能把任意来源（DDL / JPA / 源码 / 其它 ORM 产物）映射成 `Table`，生成器就能复用。

`TypeRef` 约定：原始类型写 `int`、包装类型写 `java.lang.Long`、数组 `byte[]`/`Foo[]`、集合带 `typeArguments`。
`TypeMapper.toTypeName` 有白名单但**已放宽**为"不认识的按普通类 `ClassName.bestGuess`"（:72-79），
好处是任意业务类型能过，代价是错类型不会提前报错。

---

## 6. 配置体系

优先级：**内置默认值 < access-layer.properties < pom `<configuration>` < -D**。
所有 Mojo 参数默认 `null`，只有"显式给了"才覆盖；`override` 判据是"非 null 且非空集合/Map/非空白串"，
因为 Maven 会给 List/Map 参数注入**空集合**（不这么判会静默清空 properties 里的列表）。

配置项 31 个（`PluginConfig`:38-131），实测状态：

| 状态 | 配置项 |
|---|---|
| 生效 | base/entity/mapper package、entity/mapper dir、bean.pattern、table.filter、skip.tables、generate.beans、generate.parts、output.dir、四个输出包、model.impl.subpackage、enum.dir、enum.source.dir、convert/service/impl 后缀、dto.builder、scan.only、enum.jackson、enum.with.label、enum.map（**仅 表.列 键**）、domain.map |
| 部分生效 | `access.dto.suffix` —— 只在 DTO 生成器生效，model 里写死 `+"Dto"+`（`ModelServiceGenerator`:98） |
| 未接线 | `access.overwrite.policy`（全仓仅声明/装载/打印 3 处）、`access.id.generator.class`（另有硬编码常量）、`access.enum.code.start`（完全未使用） |

**结构性缺陷**（通用化核心障碍）：
1. **没有 per-entity 块**：只有 `generate.beans`（实体名）与 `table.filter`（表名正则）两个扁平白名单，无法给单个实体单独覆盖域/包/枚举/后缀/主键策略。
2. **默认值写死项目信息**：`basePackage=com.autotest.business`、四个包名、`IdGenerator` 全限定名、`DomainMapping` 的 17 条映射。
3. properties 的列表项只能 `;`/`,` 切分，还有"含 `(` 或 `|` 就不按逗号切"的正则启发式。
4. `enumMapping` 只支持 `<表>.<列>`，文档声称的 `<实体>.<字段>` **不生效**（§8.3）。
5. 无配置校验：拼错的 part 静默产出 0 文件；缺域映射要到生成 model 时才炸。

---

## 7. 四类生成物

### 7.1 DTO（`DtoGenerator`）

```java
@Data @NoArgsConstructor @AllArgsConstructor     // dtoBuilder=true 时追加 @Builder
public class BuildDto {
  private String apkSha256;
  private BuildParseStatusEnum parseStatus;       // 枚举列替换类型
  private OffsetDateTime uploadedAt;
}
```

- 字段与实体**一一对应，不裁剪**（:55-60 说明理由：DTO 要承载完整数据库操作，凭据是否外露属业务判断）；
- 枚举列由 `Map<字段名, 枚举FQN>` 命中则换类型（:113-119）；
- `@Builder` 默认关：Lombok 给嵌套 Builder 标 `@Generated`，会被本项目 ArchUnit 分层规则判违规（实测 67 处）。

### 7.2 Converter（`ConverterGenerator`）

```java
@Mapper(componentModel = "spring")
public interface BuildConverter {
  BuildDto toDto(Build entity);
  @InheritInverseConfiguration(name = "toDto") Build toEntity(BuildDto dto);
  default BuildParseStatusEnum mapParseStatus(Short value) { ... fromValue(value) }
  default Short mapParseStatusValue(BuildParseStatusEnum value) { ... getValue() }
}
```

枚举转换只写在 converter，双向两个 default 方法；未知库存值**抛错不吞**。项目约定味最重的一块：
`fromValue`/`getValue` 方法名与 `short`/`Short` 类型全写死。

### 7.3 枚举（`EnumGenerator`）

- **只出骨架：不带成员，已存在绝不覆盖**（判重同时看输出目录与 `src/main/java`，注释说实测踩过重复生成 12 个空枚举）；
- 骨架 = `short value`（可选 `@JsonValue`）+ `VALUE_MAP` + `fromValue` 抛错 + 可选 `label/fromLabel` + 可选 `@JsonCreator fromJson(String)`；
- 成员留空，因为取值来自 DDL 的 CHECK，而插件**不读 DDL**。

枚举识别（`EnumIndex`:138-161）是最"巧"的一块，候选按优先级：
`build.parse_status → BuildParseStatusEnum`；`task.task_type → TaskTypeEnum`（列名首词==表名末词时去重）；
再退到"仅列名"、"仅表名"；每个基名先试带 `Enum` 后缀再试不带。实测 12/12 唯一命中。

> 注意：`EnumIndex` 的 `ambiguous` 字段**从未写入**，注释承诺的"命中 >1 报错"没实现，`resolve` 直接返回第一个命中。
> 索引只按**简单名**建，跨包同名枚举会静默取"后注册的那个"。

### 7.4 ModelService（`ModelServiceGenerator`，最复杂）

接口 8 个基线方法：`insert / insertBatch / findById / findByIds / updateById / updateSelectiveById / deleteById / deleteByIds`。

实现要点：
- `@Service` + 构造器注入 `mapper`、`converter`、(条件) `idGenerator`；
- 仅当「主键字段名 == `id` 且类型 == `java.lang.Long`」才注入 `idGenerator.nextId()`（:372-376）；
- `findByIds` / `deleteByIds` 空集合短路 + `mapper.select/delete` + `SqlBuilder.isIn`；
- 更新委托 MBG 的 `updateByPrimaryKey` / `updateByPrimaryKeySelective`（主键进 WHERE、不进 SET）。

包定位：`model.<域>` + `.impl`，域来自 `DomainMapping`，缺失直接抛错不猜。
`mapper`/`support` 全限定名靠**字符串替换**推出（:377-385）：
`bean.packageName().replace(".entity", ".mapper")`——只对 `…entity → …mapper` 的目录结构成立。

**合并写盘（关键机制）**：`write()`（:318-337）+ `extractHandwritten()`（:344-364）：
生成文本后，若目标文件已存在且含哨兵 `// ==================== 以下为手写`，
就把哨兵之后（到最后一个 `}` 之前）的正文原样拼回生成文件末尾。
**实测该机制只能保住函数体，保不住 import / 字段 / 构造器**（§8.4）。

---

## 8. 实测记录（本次实际执行）

环境：JDK 25.0.5、Maven 3.9.16（wrapper）、`-o` 离线、插件已在本地库。

### 8.1 scanOnly（全量）
`扫到 17 张表（全部推出主键）/ 识别到 12 个枚举列`，与 README 描述一致。

### 8.2 全量生成到临时目录
`-Dautotest.codegen.outputDir=/tmp/codegen-all -Dautotest.codegen.generateParts=dto,enum,convert,model`
→ 17 DTO + 17 converter + 34 model 文件，枚举骨架 0 个（12 个都已存在），BUILD SUCCESS 11.6s。

### 8.3 枚举映射的 `实体.字段` 形式不生效
`access.enum.map.Build.parseStatus=...` → 无任何输出；改成 `access.enum.map.build.parse_status=...` → 正常生成骨架。

### 8.4 合并策略会破坏手写代码（高危）
把真实的 `ApplicationModelService(Impl)` 拷到临时目录、原样重生成 model 部分：
手写块（list / findVisibleById / create / bindPackage / rename / softDelete / unbindAllPackages）**都在**，
但它们引用的 `applicationMapper`（5 处）、`appPackageMapper`（3 处）、`AppPackage`、
`AppPackageDynamicSqlSupport`、`BusinessException`、`isEqualTo`、`isNull`、`@Transactional`、
`OffsetDateTime` **全部没有声明/import**。

根因：`write()` 用固定模板重新生成 import 段与字段/构造器，只把哨兵后的**函数体**拼回。
→ 手写方法一旦引用生成块之外的东西，重生成后**必然编译失败**。接口的类级 Javadoc 也会被覆盖。

### 8.5 `failOnDrift` 未实现 → 静默覆盖（高危）
生成 `AccountDto` → 追加一行 `// 人工修改` → 再跑一次 → BUILD SUCCESS，文件被无条件覆写（md5 变化，手改消失）。
`overwritePolicy` 全仓只有声明/读取/打印三处，**没有任何地方用它决策写盘**。

### 8.6 其他
- `-Dautotest.codegen.enumMapping=...`（Map 参数）无法从命令行传：Maven 报 "Cannot assign configuration entry ... to property of type java.util.Map"。只能走 properties 或 pom。
- `generateParts` 写错（如 `dtoo`）不报错，静默 0 文件。
- `entity/` 下 19 个 java 但只扫到 17 张表：`DeviceListRow`（无 Support/Mapper）与 `package-info` 被正确跳过。

---

## 9. 通用化差距清单

| # | 问题 | 证据 | 建议 |
|---|---|---|---|
| **P0-1** | 输入契约钉死 MBG+DynamicSQL 2.0.0 | `*DynamicSqlSupport` 内部类、`Lorg/mybatis/dynamic/sql/SqlColumn;`、`lambda$*ByPrimaryKey$0`、`@Mapper` | 抽 `MetadataReader` SPI，现有实现降级为 `reader-mbg-bytecode` |
| **P0-2** | 生成物形态硬编码 | Lombok/MapStruct/Spring、`Optional`、`insertMultiple`、`isIn`、`fromValue` | ①配置开关（lombok/optional/注解）②模板外置（FreeMarker 或 JavaPoet「模板类」） |
| **P0-3** | 无 per-entity 配置 | 只有扁平 `generate.beans` | `defaults + entities[]`（§10.3） |
| **P0-4** | 项目信息写死 | 默认包名、`DomainMapping.DEFAULTS`、`ID_GENERATOR` 常量 | 全部外置；缺省值由 `basePackage` 推导 |
| **P1-1** | `overwritePolicy` 未实现 | §8.5 | 实现 overwrite/skip/failOnDrift/merge 四策略 + `check` 模式 |
| **P1-2** | 合并策略不自洽 | §8.4 | 改"分离"：生成接口 + 生成抽象基类，手写类继承；或生成到独立子包 |
| **P1-3** | 两轮编译 | `@Mojo` 在 `GENERATE_SOURCES` 却读 class | 读源码（JavaParser）/ 读 DDL / 绑 `process-classes` + 二次编译 |
| **P1-4** | `dtoSuffix` 硬编码 `Dto` | :98 | 传参修掉 |
| **P1-5** | `idGeneratorClass` 未接线 | :48 | 传参修掉；主键策略化 |
| **P1-6** | 枚举索引只按简单名 | `EnumIndex`:88 | 包路径+简单名双索引，冲突报错 |
| **P1-7** | `enumMapping` 键形式与文档不符 | §8.3 | 支持两种键；未知键报警 |
| **P1-8** | 包名推导靠 `replace(".entity",".mapper")` | :377-385 | 显式配置，不要字符串替换 |
| **P2** | `generateParts` 不校验 / `readSupport` 不校验方法名 / 错误路径全量重扫 / 无 dry-run / 0 测试 / 重复 import | — | 顺手清 |

---

## 10. 目标形态建议

### 10.1 "通过 Maven 依赖引入"的三种落法（必须先选清）

| 方式 | 消费方要写什么 | 何时跑 | 是否算"依赖" |
|---|---|---|---|
| **A. 插件（推荐）** | `<build><plugins><plugin><goal>generate</goal>` | 绑生命周期或显式调用 | 构件是 plugin，声明在 `<plugins>`，**不是** `<dependencies>` |
| B. 插件依赖 | `<plugin><dependencies>` 加你的 jar | 由宿主插件加载 | 严格意义的"插件依赖" |
| C. Build Extension | `.mvn/extensions.xml` 加一段 | 自动挂生命周期 | 是（`<extensions>`），无需改 pom，但要 `META-INF/maven/extension.xml`，侵入构建模型 |

推荐 **A 为主 + C 可选**。Maven 插件的 `<dependencies>` **不是**给消费者加业务依赖用的；
若目标是"加一行依赖就自动生成"，必须走 C，否则就是 A——这点要先对齐，否则做出来与预期不符。

### 10.2 模块切分

```
mds-codegen/
├── codegen-core/                  普通 jar：模型 + 配置 + 写盘策略 + 模板 + SPI
│   ├── model/       Table/Bean/Column/TypeRef（沿用 DaoModel，去掉 MBG 特定字段）
│   ├── config/      CodegenConfig（defaults + entities[]）、校验
│   ├── reader/      MetadataReader 接口 + Registry
│   ├── template/    CodeTemplate 接口 + 变量上下文 + FreeMarker 适配
│   ├── writer/      SourceWriter（4 种覆盖策略 + check 模式）
│   └── gen/         内置生成器（dto/enum/converter/model）
├── codegen-reader-mbg-bytecode/    ASM 读 MBG 产物（现 BytecodeReader/DaoScanner 迁入）
├── codegen-reader-source/          JavaParser 读源码（解两轮编译）
├── codegen-reader-ddl/             DDL 解析（枚举取值 / CHECK / 表注释）
├── codegen-maven-plugin/           GenerateMojo / ScanMojo / CheckMojo
└── codegen-template-default/       默认模板 resources/templates/*.ftl
```

SPI 只需要两个：

```java
public interface MetadataReader {
    String id();                                   // "mbg-bytecode" / "source" / "ddl"
    boolean supports(CodegenConfig cfg);
    List<Table> read(ReadContext ctx);
}

public interface SourceWriter {
    WriteResult write(Path outDir, String pkg, String simpleName, String content, Policy policy);
}
```

"换 ORM" = 加一个 reader jar；"换代码风格" = 加一个模板 jar；插件本体不动。

### 10.3 配置结构（满足"项目内配置文件指定要处理的实体"）

YAML 优于 properties（嵌套、列表、注释）：

```yaml
version: 1
defaults:
  reader: mbg-bytecode
  source:
    classesDir: ${project.build.outputDirectory}
    entityPackage: com.mds.demo.entity
    mapperPackage: com.mds.demo.mapper
    pattern: ".*"
  output:                                  # {domain} 等为占位符
    root: ${project.build.directory}/generated-sources/codegen
    dto: "com.mds.demo.dto"
    converter: "com.mds.demo.convert"
    model: "com.mds.demo.model.{domain}"
    modelImpl: "com.mds.demo.model.{domain}.impl"
    enum: "com.mds.demo.domain.{domain}"
  naming:
    dtoSuffix: Dto
    converterSuffix: Converter
    serviceSuffix: ModelService
    implSuffix: ModelServiceImpl
  style:
    lombok: true
    mapstruct: true
    springService: true
    dtoBuilder: false
    findByIdReturns: optional        # optional | nullable
  enum:
    valueType: short
    accessor: getValue
    factory: fromValue
    withLabel: false
    jackson: false
  write:
    policy: mergeBySentinel          # overwrite | skip | failOnDrift | mergeBySentinel
    sentinel: "// ---- 手写区开始 ----"

entities:                                # 白名单：不写 = 不生成
  - table: build
    entity: Build
    domain: build
    enums: { parse_status: com.mds.demo.domain.build.BuildParseStatusEnum }
    parts: [dto, converter, enum]
    key: { field: apkSha256, strategy: caller }      # caller | snowflake | db
  - table: account
    entity: Account
    domain: identity
    parts: [dto, converter, model]
    key: { field: id, strategy: snowflake }
```

要点：`entities[]` 白名单比"空=全部"更安全；每个实体可覆盖任意 defaults 键；
键用 `table`+`entity`，不再依赖包名里必须有 `.entity`；默认文件 `codegen/entities.yaml`。

### 10.4 需要参数化掉的硬编码

| 现在写死 | 应变成 |
|---|---|
| `@Data @NoArgsConstructor @AllArgsConstructor` | `style.lombok` + 模板 |
| `@Mapper(componentModel="spring")` | `style.mapstruct` + componentModel |
| `@Service` + 构造器注入 | `style.springService` / 模板 |
| `Optional<Dto> findById` | `style.findByIdReturns` |
| `mapper.insertMultiple(...)` | 探测 mapper class 是否真有该方法，而不是假设 |
| `getId()==null → idGenerator.nextId()` | `key.strategy` + 可插拔 KeyGeneratorSpec |
| 枚举 `short/getValue/fromValue` | `enum.valueType/accessor/factory` |
| `model.<域>.impl` 布局 | `output.model` 模板串 |
| 哨兵字符串 | `write.sentinel` |

### 10.5 落地顺序

1. **止血**（半天）：`overwritePolicy` 生效、`dtoSuffix`/`idGeneratorClass` 接线、`generateParts` 校验、
   `enumMapping` 双键、`ambiguous` 报错——这些在当前工程里就会咬人。
2. **拆包**（1–2 天）：`codegen-core` + `codegen-reader-mbg-bytecode`，行为不变（用 17 张表做逐字节回归基线）。
3. **换配置**（1–2 天）：properties → YAML `defaults+entities[]`，保留旧键兼容映射。
4. **模板化**（3–5 天）：四个生成器的 TypeSpec 组装抽成模板；先用"JavaPoet 模板类"，需要用户自定义再上 FreeMarker。
5. **加 reader**（按需）：`source` 优先（解两轮编译），`ddl` 次之（解枚举取值）。
6. **CI 门禁**：`check` goal（生成到临时目录 + 逐字节比对，漂移退出 1）——现有工程已用 shell 脚本做类似事。

---

## 11. 风险与开放问题

1. **"通用"边界**：支持"任意 ORM"还是"任意 MyBatis Dynamic SQL 项目"？前者要 3 个以上 reader；
   后者只需把 MBG 特定常量（`SUPPORT_SUFFIX`、`MAPPER_SUFFIX`、`@Mapper` 注解名、`SqlColumn` 包名）参数化。**建议先做后者**。
2. **枚举取值从哪来**：现在"骨架 + 人补"。若读 DDL 的 `CHECK (x IN (1,2,3))` 就能连成员一起生成，但会推翻"不读 DDL"的前提，需确认。
3. **合并 vs 分离**：哨兵合并在 Java 里天然别扭（import/字段跨区）。**推荐分离**：
   `XxxModelService`（生成接口）+ `AbstractXxxModelService`（生成实现）+ 手写 `XxxModelServiceImpl extends`；
   或生成到 `…generated` 子包、手写类实现生成接口。代价是文件变多、包结构变化。
4. **两轮编译**：读字节码的固有代价。可绑 `process-classes` + 追加一个 `maven-compiler-plugin` 执行做到单轮；
   根本解法是改读源码/DDL。
5. **坐标与命名**：现 `com.autotest:access-layer-plugin` / goalPrefix `autotest`；
   通用化建议 `com.mds.codegen:codegen-maven-plugin` / goalPrefix `codegen`。
6. **版本兼容**：`maven.compiler.release=25`、ASM 9.10.1；`SqlColumn<T>` 是**字符串前缀匹配**，
   mybatis-dynamic-sql 换包名就静默 0 列——要加"扫到 0 列/字段名对不上"的显式报错。

---

## 12. 附：本次实测产物

- 全量生成（不落源码树）：`/tmp/codegen-all`
- 指定实体生成：`/tmp/codegen-probe`（Build + Account）
- 枚举骨架：`/tmp/codegen-enum/com/autotest/business/domain/build/ProbeXEnum.java`
- 合并破坏验证：`/tmp/codegen-merge`（Application 重生成结果，可与源码 diff）

复现示例：

```bash
cd /usr/local/projects/agent/auto-test/business-server/business-server-core
../mvnw com.autotest:access-layer-plugin:1.0.0-SNAPSHOT:generate -Dautotest.codegen.scanOnly=true
../mvnw com.autotest:access-layer-plugin:1.0.0-SNAPSHOT:generate \
    -Dautotest.codegen.outputDir=/tmp/codegen-all \
    -Dautotest.codegen.generateParts=dto,enum,convert,model
```

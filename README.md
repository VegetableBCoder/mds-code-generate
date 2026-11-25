# mds-code-generate

把 **MyBatis Generator 生成的数据访问层**，再往前推一步：

```
MBG 产物（entity / mapper / DynamicSqlSupport）
        │
        ▼  codegen-maven-plugin
DTO  ─  MapStruct converter  ─  枚举  ─  model-service 接口 + 实现
```

这样从"能访问数据库"到"能写业务代码"之间的那些样板，不用再手敲一遍。

## 功能

| 生成物 | 内容 |
|---|---|
| `XxxDto` | `@Data @NoArgsConstructor @AllArgsConstructor @Builder`；枚举列换成枚举类型 |
| `XxxConverter` | MapStruct `@Mapper(componentModel = "spring")`；entity ↔ dto，枚举双向转换 |
| `XxxModelService` | 8 个基线 CRUD 接口，`findById` 固定返回 `Optional` |
| `XxxModelServiceImpl` | `@Service` 实现；`mapper` / `converter` / `idGenerator` 为 `protected` |
| 枚举骨架 | 只在配置的枚举还不存在时生成一次，成员由人补 |

基线 CRUD 是：`insert`、`insertBatch`、`findById`、`findByIds`、
`updateById`、`updateSelectiveById`、`deleteById`、`deleteByIds`。

## 用法

### 1. 引入插件

```xml
<plugin>
  <groupId>com.mds.codegen</groupId>
  <artifactId>codegen-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</plugin>
```

### 2. 写配置

模块根目录放 `codegen.yaml`，逐项说明见 [codegen.yaml](codegen.yaml)。最小可用：

```yaml
source:
  entityPackage: "com.example.demo.entity"
  mapperPackage: "com.example.demo.mapper"

output:
  dtoPackage: "com.example.demo.dto"
  convertPackage: "com.example.demo.convert"
  modelPackage: "com.example.demo.model"

entities:
  - name: AppUser
    enums:
      status: "com.example.demo.domain.user.UserStatusEnum"
```

> ⚠️ `com.example.demo.*` 是占位，改成你自己项目的包名。

### 3. 跑

**IDEA**：先 Build 一次，Maven 面板 → `Plugins` → `codegen` → **`codegen:generate`**。

**命令行**：

```bash
mvn compile                                  # 必须先编译，见下
mvn codegen:generate                         # 按配置生成全部实体
mvn compile                                  # 把生成物编译进来
```

> **为什么必须先 compile**：插件读的是 `target/classes` 里的**字节码**，不是源码。
> 所以"生成"必然发生在一次编译之后；第二次 compile 是把生成物带进来。

只处理**一个实体**（调试用）：`mvn codegen:generate -Dcodegen.entity=AppUser`。

## 主要配置

| 配置 | 说明 |
|---|---|
| `source.entityPackage` / `source.mapperPackage` | MBG 产物所在包 |
| `output.dir` | 生成到哪，默认 `target/generated-sources/codegen` |
| `output.byDomain` | `false` 平铺；`true` 按 `entities[].domain` 分 `model.<域>` |
| `entities[].enums` | 字段名 → 枚举全限定名 |
| `idGenerator` | 主键生成器全限定名，**只要 `long nextId()`**，不必实现任何接口 |
| `write.policy` | `overwrite` / `skip` / `failOnDrift`（默认） |
| `naming.*` | 各类文件后缀，一般不用改 |

**生成进源码树**（提交进 git）：把 `output.dir` 改成 `"src/main/java"` 就行，其它不动。
这样 `mvn clean` 不会删它，每次构建也不用跑插件。

## 实现方法（简要）

### 读取：加载字节码 + 反射

不解析源码，也不用 ASM 手写 visitor——用隔离的 `URLClassLoader`
（父加载器取 `getPlatformClassLoader()`，避免插件自身依赖干扰）把 MBG 的 class 加载进来，
直接读它们的反射信息。

这样做的好处是**元数据取的是 MBG 自己的意图，不用猜**：

| 要什么 | 从哪读 |
|---|---|
| 表名 | `DynamicSqlSupport` 里 `SqlTable.tableName()` |
| 列名 | 同上的 `SqlColumn.name()` |
| 列的 Java 类型 | `SqlColumn<T>` 的**泛型实参**（MBG 产物的 `javaType()` 恒为空） |
| 主键 | mapper 上 MBG 写的 `@Result(id = true).property()`，不反推合成 lambda |

代价就是那个"先 compile"的约束。

### 生成：JavaPoet

每个部件一个 `Generator` 实现（`dto` / `convert` / `model`），
输入统一是"表模型 + 该实体配置"，输出 `GeneratedFile`，**不碰文件系统**。

### 落盘：一处策略

`SourceWriter` 统一处理 `overwrite` / `skip` / `failOnDrift`。
默认 `failOnDrift`：内容不一致就失败并列出文件，正好可以当"生成物是否被人手改过"的 CI 探针。

### 包结构

```
src/main/java/com/mds/codegen/
├── CodegenMojo.java          唯一的 Mojo：装载配置 → 读元数据 → 调生成器 → 落盘
├── config/                   YAML 装载与校验（未知键直接报错）
├── read/                     ReflectionReader / ClasspathProbe / ReadContext
├── model/                    Table / Bean / Column / TypeRef
└── gen/                      各部件生成器 + SourceWriter + DomainNaming
```

## 枚举

在 `entities[].enums` 里写 **字段名 → 枚举全限定名**，插件按该类存不存在自己分：

| 情况 | 行为 |
|---|---|
| **已存在** | 直接用：DTO 字段换成枚举，converter 生成 `mapXxx` / `mapXxxValue` |
| **不存在** | 在 `src/main/java` 下生成骨架（`value` + `fromValue`），成员由人补 |

骨架写源码树而不是 target——它之后由人长期维护。已存在的枚举绝不覆盖，骨架也只生成一次。

取值类型从实体列推导（`SMALLINT → short`、`INT → int`、`BIGINT → long`、`VARCHAR → String`），
不需要单独配置。

## 加自定义方法

直接写进生成的实现类，靠 **git 对比 / 回滚**；不想再被覆盖就把 `write.policy` 设为 `skip`。
`mapper` / `converter` / `idGenerator` 都是 `protected`。

## 固定项（不做开关）

Lombok（`@Data` / `@Builder` 等）、MapStruct、Spring `@Service`、`findById` 返回 `Optional`
都是写死的。目标场景就一种技术栈，多一个开关就多一处能配错的地方、多一条要测的分支。

## 常见报错

| 现象 | 原因 | 解决 |
|---|---|---|
| `实体 class 目录不存在` | 没编译，或包名写错 | 先 Build；包名不对时报错会列出实际存在的包 |
| `未知配置项 output.xxx` | 用了已移除的旧键 | 报错里写明替代方案 |

## 已知边界

- 只支持 MBG 的 `MyBatis3DynamicSql` 产物形态；
- 主键必须单字段，联合主键直接报错；
- 自动生成主键的条件：主键字段名是 `id` **且** 类型是 `Long`。

## 仓库

- `it/`：链路验证工程（真实 MySQL + MBG + 本插件 + 集成测试），见 [it/README.md](it/README.md)；
- `docs/research-access-layer-plugin.md`：对原 `access-layer-plugin` 的调研；
- `target/`、`.idea/`、`*.iml` 不进仓库；数据库口令由 `DB_PASSWORD` 环境变量提供。
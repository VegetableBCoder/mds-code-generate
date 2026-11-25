# codegen-it：链路验证工程

**单模块**：真实 MySQL 建表 → MyBatis Generator 生成 DAO → **codegen-maven-plugin** 生成
DTO / converter / model-service → 编译 → 集成测试。

## 目录

```
it/
├── db/schema.sql                 # 建库建表（codegen_test：app_user / order_record / device）
├── run.sh                        # 一键跑完整链路
└── demo-dao/
    ├── generatorConfig.xml       # MBG 配置（MyBatis3DynamicSql）
    ├── codegen.yaml              # 插件配置（实体清单 + 枚举映射）
    ├── src/main/java/com/mds/demo/
    │   ├── entity/  mapper/      # MBG 生成（DAO 层，本插件的输入）
    │   └── domain/ common/       # 手写：枚举 + IdGenerator（雪花）
    └── src/it/java/.../CodegenIntegrationTest.java   # 端到端集成测试（-Pit 才编）
```

## 三张表刻意覆盖的主键/类型形态

| 表 | 主键 | 插件行为 |
|---|---|---|
| `app_user` | `id BIGINT` | **注入 `IdGenerator.nextId()`**，insert 前生成雪花 ID 并回填 DTO |
| `order_record` | `order_no VARCHAR(40)` | 主键由调用方给，不注入 |
| `device` | `uuid CHAR(36)` | 同上（且类型不是 Long） |

枚举列：`app_user.status` / `order_record.state` / `device.kind` 都是 `SMALLINT`，
DTO 侧换成对应枚举（配置里给全限定名），converter 生成双向 `mapXxx` / `mapXxxValue`。

本例 `byDomain: true`，按 `entities[].domain` 分到 user / order / device；
**一个小微服务不需要分域时把 `byDomain` 设成 `false`** 即可平铺。

## 数据库口令

**不进仓库**。用环境变量提供：

```bash
DB_PASSWORD='你的口令' bash run.sh
```

不设的话脚本会交互式询问。口令经 `-Ddb.password` 传给 Maven，再过滤进
`target/generatorConfig.xml`（`target/` 是构建产物，不进仓库），
集成测试那边则走 `application.yml` 的 `${DB_PASSWORD}`。

## 怎么跑

```bash
bash run.sh              # 建表 + MBG + 编译 + 生成 + 编译 + 测试
bash run.sh --no-db      # 库已建好时
```

分步：

```bash
./mvnw -f it/pom.xml -pl demo-dao process-resources          # 过滤 generatorConfig.xml
./mvnw -f it/pom.xml -pl demo-dao org.mybatis.generator:mybatis-generator-maven-plugin:1.4.2:generate
./mvnw -f it/pom.xml -pl demo-dao compile                    # 第一次：产出 DAO 的 class
./mvnw -f it/pom.xml -pl demo-dao com.mds.codegen:codegen-maven-plugin:1.0.0-SNAPSHOT:generate
./mvnw -f it/pom.xml -pl demo-dao compile                    # 第二次：编译生成物
./mvnw -f it/pom.xml -pl demo-dao test -Pit -Dtest=CodegenIntegrationTest
```

## 两个必须知道的坑（本工程已踩并解决）

### 1. MBG 不解析 Maven 占位符，要靠资源过滤

`generatorConfig.xml` 里写 `connectionURL="${db.url}"` 是没用的——MBG 把它当普通字符串。
正确做法：把配置过滤到 `target/` 再交给它（见 `demo-dao/pom.xml` 的 `<resources>`）。

顺带一个坑：过滤后 `&` 会原样进 XML，所以 pom 里的 JDBC URL 要写 `&amp;amp;`。

### 2. 生成目录要被登记成源码根

插件读的是**已编译**的类（反射），所以生成动作只能在第一次 compile 之后；
它运行时虽然会 `addCompileSourceRoot`，但那只在**同一次** Maven 会话内有效。
第二次编译要能看见生成物，必须用 `build-helper-maven-plugin` 把
`target/generated-sources/codegen` 加进源码根。

## 集成测试覆盖了什么

真实 MySQL，4 个用例：

1. **AppUser CRUD**：insert 后断言 DTO 拿到雪花主键 → findById → 枚举往返 → updateSelectiveById
   → findByIds → deleteById；
2. **DTO `@Builder`**：builder 构造后断言字段；
3. **OrderRecord**：业务主键由调用方给 → insert / findById / updateById / deleteById；
4. **Device**：`CHAR(36)` 主键 → insert / findById / 枚举 / deleteById。

跑通即证明：**MBG 产物 → 插件生成物 → MapStruct 实现（注解处理器）→ Spring 装配 → 真实 SQL** 闭环。

## 结果

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
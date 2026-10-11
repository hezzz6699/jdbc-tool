# JDBC 工具类 JDBCTool 的设计与开发

> Java 课程作业一：基于 **ORM 思想 + Java 反射 + JDBC** 实现一个通用数据库访问工具类 `JDBCTool`，
> 并使用它对 `DateTest` 数据库中的「学生表」和「学院表」完成增、删、改、查操作。
> 项目使用 **Maven** 构建，使用 **Git** 进行版本管理。

---

## 一、作业目标对照

| 作业目标 | 实现位置 |
| --- | --- |
| 理解 ORM（对象关系映射）基本原理 | `JDBCTool` 类头注释、`@Table/@Column/@Id` 注解 |
| 掌握反射在对象创建与属性赋值中的应用 | `JDBCTool#newInstance`、`#setFieldValue`、`#resultSetToList` |
| 使用 JDBC 完成增删改查 | `JDBCTool#save/#update/#delete/#getOneById` |
| 实现四个（+1）核心方法 | `JDBCTool` 中 `resultSetToList / save / update / delete / getOneById` |
| 定义数据库 `DateTest` 与两张表 | `src/main/resources/schema.sql` |
| 用 JDBCTool 操作两张表 | `com.example.jdbc.DemoMain`、`com.example.jdbc.JDBCToolTest` |
| Maven 构建 | `pom.xml` |
| Git 版本管理 | 仓库根目录 `.git`、`.gitignore` |

---

## 二、技术栈

- **JDK 17**
- **Maven 3.9**（构建 / 依赖管理）
- **JDBC** + **Java 反射（Reflection）**
- **H2 2.2**（嵌入式数据库，MySQL 兼容模式，开箱即用）
- **MySQL Connector/J 8.4**（可选，切换真实 MySQL 时使用）
- **JUnit 5**（单元测试）
- **Git**（版本管理）

---

## 三、目录结构

```
jdbc-tool
├── pom.xml                          # Maven 配置
├── README.md
├── .gitignore
├── docs/                            # 运行截图、作业答案
└── src
    ├── main
    │   ├── java/com/example/jdbc
    │   │   ├── annotation           # ORM 注解
    │   │   │   ├── Table.java       #   表名注解
    │   │   │   ├── Column.java      #   列名注解
    │   │   │   └── Id.java          #   主键注解
    │   │   ├── entity               # 实体类（映射数据库表）
    │   │   │   ├── Student.java     #   学生表 student
    │   │   │   └── College.java     #   学院表 college
    │   │   ├── util
    │   │   │   ├── JDBCTool.java    # ★ 核心工具类（五个方法 + 辅助方法）
    │   │   │   └── DBUtil.java      # 数据库连接 / 初始化工具
    │   │   └── DemoMain.java        # 演示程序（控制台运行效果）
    │   └── resources
    │       ├── db.properties        # 数据库配置（默认 H2，可切 MySQL）
    │       └── schema.sql           # 建表脚本
    └── test/java/com/example/jdbc
        └── JDBCToolTest.java        # 五个方法的单元测试
```

---

## 四、数据库设计（数据库名：DateTest）

### 1. 学院表 college

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | INT | 主键，自增 |
| name | VARCHAR(50) | 学院名称 |
| code | VARCHAR(20) | 学院代码 |

### 2. 学生表 student

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | INT | 主键，自增 |
| name | VARCHAR(50) | 姓名 |
| major | VARCHAR(50) | 专业 |
| age | INT | 年龄 |
| enroll_time | DATE | 入学时间 |
| graduated | BOOLEAN | 是否毕业 |
| tuition | DECIMAL(10,2) | 学费 |

建表脚本见 [`src/main/resources/schema.sql`](src/main/resources/schema.sql)。

---

## 五、JDBCTool API 说明

| 方法 | 说明 |
| --- | --- |
| `static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz)` | 把 `ResultSet` 映射为对象列表：反射创建对象 → 按列名匹配字段 → 类型转换后赋值 |
| `static <T> int save(T obj, Connection connection)` | 反射读取对象非空字段，动态拼装 `INSERT`，返回受影响行数 |
| `static <T> int update(T obj, Connection connection)` | 根据 `@Id` 主键，动态拼装 `UPDATE`（只更新非空字段） |
| `static <T> int delete(T obj, Connection connection)` | 根据 `@Id` 主键拼装 `DELETE` |
| `static <T> T getOneById(String id, Class<T> clazz, Connection connection)` | 按主键查询单条记录并映射为对象，不存在返回 `null` |

**ORM 映射规则**

- 类 → 表：由 `@Table(name="...")` 指定，缺省时「类名驼峰转下划线小写」；
- 字段 → 列：由 `@Column(name="...")` 指定，缺省时「字段名驼峰转下划线小写」（如 `enrollTime` → `enroll_time`）；
- 主键：由 `@Id` 标注的字段决定，缺省时取名为 `id` 的字段。

配套的辅助方法：`getTableName`、`getColumnName`、`getFields`、`getPrimaryKeyField`、
`findFieldByColumn`、`getFieldValue`、`setFieldValue`、`convert`、`toSqlValue`、`camelToSnake`。

---

## 六、运行方式

### 环境要求

- JDK 17
- Maven 3.6+
- （可选）MySQL 8，若不想安装可直接使用内置 H2

### 1. 编译并运行单元测试

```bash
mvn clean test
```

### 2. 运行演示程序（打印增删改查全过程）

```bash
mvn exec:java
```

### 3. 打包为可执行 jar 并运行

```bash
mvn clean package
java -jar target/jdbc-tool-1.0.0.jar
```

> 默认使用 H2 嵌入式数据库（MySQL 兼容模式），**无需安装任何数据库服务即可直接运行**。

### 4. 切换到 MySQL（可选）

1. 在 MySQL 中创建数据库：

   ```sql
   CREATE DATABASE DateTest DEFAULT CHARSET utf8mb4;
   ```

2. 切换到 `DateTest` 库并执行 `src/main/resources/schema.sql` 建表；
3. 打开 `src/main/resources/db.properties`，注释掉 H2 配置、启用 MySQL 配置并填写账号密码；
4. 重新运行 `mvn exec:java` 即可。

---

## 七、运行效果

运行截图见 [`docs/`](docs/) 目录。

---

## 八、Git 提交记录

```bash
git log --oneline
```

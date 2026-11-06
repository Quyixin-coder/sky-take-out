# 员工模块 CRUD 总结

## 一、先记住整体路线

```text
网页
→ Nginx
→ Controller 接收请求
→ Service 处理业务
→ Mapper 操作数据库
→ MySQL
→ 返回 Result
→ 网页显示结果
```

员工模块的核心文件：

```text
Controller
sky-server/src/main/java/com/sky/controller/admin/EmployeeController.java

Service 接口
sky-server/src/main/java/com/sky/service/EmployeeService.java

Service 实现
sky-server/src/main/java/com/sky/service/impl/EmployeeServiceImpl.java

Mapper
sky-server/src/main/java/com/sky/mapper/EmployeeMapper.java

复杂 SQL
sky-server/src/main/resources/mapper/EmployeeMapper.xml
```

## 二、员工模块接口表

### 登录

```text
POST /admin/employee/login
```

作用：

```text
验证账号密码，生成 JWT
```

### 分页查询员工

```text
GET /admin/employee/page?page=1&pageSize=10&name=
```

作用：

```text
查询第几页员工，每页多少条
```

### 新增员工

```text
POST /admin/employee
```

作用：

```text
向 employee 表插入一行
```

### 根据编号查询员工

```text
GET /admin/employee/{id}
```

作用：

```text
查询一个员工的完整信息，通常用于编辑前回显
```

### 修改员工

```text
PUT /admin/employee
```

作用：

```text
根据 DTO 中的 id 修改已有员工
```

### 启用或禁用员工

```text
POST /admin/employee/status/{status}?id={id}
```

示例：

```text
POST /admin/employee/status/0?id=2
```

作用：

```text
status=1 启用
status=0 禁用
```

## 三、四个固定角色

### 1. Controller：前台服务员

职责：

```text
接收请求
读取参数
调用 Service
返回 Result
```

固定模板：

```java
@RestController
@RequestMapping("/admin/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/page")
    public Result<PageResult> page(EmployeePageQueryDTO dto) {
        PageResult result = employeeService.pageQuery(dto);
        return Result.success(result);
    }
}
```

固定写法：

- `@RestController`：这是网页接口类。
- `@RequestMapping`：这一组接口的公共地址。
- `@Autowired`：请 Spring 自动注入 Service。
- `Result.success(...)`：返回统一成功结果。

自己写的内容：

- 接口地址：例如 `/page`。
- 请求方法：`GET`、`POST`、`PUT`。
- 方法名：例如 `page`、`save`、`update`。
- 调用哪个 Service 方法。

### 2. Service：业务经理

职责：

```text
判断业务规则
补全默认字段
调用 Mapper
组织返回结果
```

固定模板：

```java
@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    public PageResult pageQuery(EmployeePageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery(dto);
        return new PageResult(page.getTotal(), page.getResult());
    }
}
```

固定写法：

- `@Service`：交给 Spring 管理。
- `implements`：实现 Service 接口。
- `@Autowired`：自动获得 Mapper。
- `PageHelper.startPage`：准备分页。
- `new PageResult(...)`：打包总数和当前页数据。

自己写的内容：

- 新增时默认密码是什么。
- 新员工默认启用还是禁用。
- 哪些情况需要抛出异常。
- 调用哪些 Mapper 方法。
- 多个数据库操作是否需要事务。

### 3. Mapper：仓库管理员

职责：

```text
执行查询、新增、修改、删除 SQL
```

简单 SQL 使用注解：

```java
@Mapper
public interface EmployeeMapper {

    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    @Insert("insert into employee (...) values (...)")
    void insert(Employee employee);
}
```

复杂 SQL 使用 XML：

```text
sky-server/src/main/resources/mapper/EmployeeMapper.xml
```

固定写法：

- `@Mapper`：告诉 MyBatis 接管接口。
- `@Select`：查询。
- `@Insert`：新增。
- `@Update`：修改。
- `@Delete`：删除。
- `#{}`：安全放入方法参数。

自己写的内容：

- 查询哪张表。
- 使用哪些字段。
- WHERE 条件、排序和分页。
- 是否需要联表查询。

### 4. MySQL：最终仓库

员工数据保存位置：

```text
数据库：sky_take_out
数据表：employee
本地端口：3307
```

重要字段：

| 字段 | 含义 |
|---|---|
| `id` | 员工编号 |
| `name` | 姓名 |
| `username` | 登录账号 |
| `password` | MD5 密码指纹 |
| `phone` | 手机号 |
| `sex` | 性别 |
| `id_number` | 身份证号 |
| `status` | 1启用，0禁用 |
| `create_time` | 创建时间 |
| `update_time` | 修改时间 |
| `create_user` | 创建人编号 |
| `update_user` | 修改人编号 |

## 四、DTO、Entity、VO

### DTO：进门的数据

前端提交的数据：

```text
EmployeeDTO
```

主要字段：

```text
id、name、username、phone、sex、idNumber
```

用途：

```text
接收新增和修改表单
```

### Entity：数据库档案

数据库一行的 Java 表示：

```text
Employee
```

用途：

```text
包含密码、状态、时间和创建人等完整字段
```

### VO：出门的数据

返回浏览器的数据：

```text
EmployeeLoginVO
```

主要字段：

```text
id、userName、name、token
```

用途：

```text
登录成功后返回需要展示的信息和 JWT
```

记忆：

```text
DTO 进门
Entity 入库
VO 出门
```

## 五、每个 CRUD 操作做了什么

### 查询列表

```text
GET /admin/employee/page
→ Controller.page
→ Service.pageQuery
→ PageHelper.startPage
→ Mapper.pageQuery
→ XML select
→ MySQL 返回员工
→ PageResult
→ Result.success
```

SQL：

```sql
SELECT * FROM employee
WHERE name LIKE ?
ORDER BY create_time DESC
LIMIT ?;
```

### 新增

```text
POST /admin/employee
→ Controller.save
→ Service.save
→ DTO 转 Entity
→ 补密码、状态、时间、创建人
→ Mapper.insert
→ INSERT SQL
→ MySQL 多一行
→ Result.success
```

### 修改

```text
PUT /admin/employee
→ Controller.update
→ Service.update
→ DTO 转 Entity
→ 补修改时间、修改人
→ Mapper.update
→ UPDATE SQL
→ MySQL 修改指定 id
→ Result.success
```

SQL 核心必须带 id：

```sql
UPDATE employee
SET name = ?, phone = ?, update_time = ?, update_user = ?
WHERE id = ?;
```

### 启用禁用

```text
POST /admin/employee/status/0?id=2
→ Controller.startOrStop
→ Service.startOrStop
→ Service 创建只包含 id、status、updateTime 的对象
→ Mapper.update
→ UPDATE employee ... WHERE id=2
→ status 变成 0
```

后续登录时：

```text
status=1 → 可以登录
status=0 → 账号被锁定
```

## 六、员工登录与 CRUD 的区别

登录是查询业务，但还多了一步身份认证：

```text
查询员工
→ 检查员工是否存在
→ 检查密码指纹
→ 检查账号状态
→ 生成 JWT
→ 返回 EmployeeLoginVO
```

JWT 用于后续后台请求：

```text
浏览器保存 token
→ 每次请求携带 token
→ JwtTokenAdminInterceptor 验证
→ BaseContext 保存当前员工 id
```

## 七、公共框架能力

### PageHelper

负责分页：

```java
PageHelper.startPage(page, pageSize);
```

它会自动生成总数查询和 LIMIT。

### JWT 拦截器

负责：

```text
检查登录通行证
获取当前员工编号
```

### AutoFillAspect

负责自动填：

```text
create_time
update_time
create_user
update_user
```

### GlobalExceptionHandler

负责把业务异常转换成：

```json
{
  "code": 0,
  "msg": "错误提示"
}
```

### Result

统一返回格式：

```java
code   1成功，0失败
msg    错误提示
data   真正返回到数据
```

## 八、固定模板和自己代码

### 基本不用自己想

```text
@RestController
@Service
@Mapper
@Autowired
@GetMapping
@PostMapping
@PutMapping
@RequestBody
@PathVariable
Result.success
PageHelper.startPage
MyBatis XML 标签
```

### 每个业务自己决定

```text
接口地址
请求方法
方法名称
需要哪些参数
业务判断
默认字段
查询哪张表
SQL 条件
排序方式
返回哪些字段
```

## 九、遇到新模块时的阅读顺序

### 第一步：找 Controller

找：

```text
@RequestMapping 类地址
@GetMapping / @PostMapping / @PutMapping 方法地址
Controller 调用了哪个 Service 方法
```

### 第二步：找 Service

找：

```text
业务规则
调用哪个 Mapper
是否需要事务
返回什么数据
```

### 第三步：找 Mapper 和 SQL

找：

```text
操作哪张表
WHERE 条件
INSERT / UPDATE / DELETE
是否联表
```

### 第四步：去 Navicat 验证

查看操作前后的数据变化。

## 十、常见错误

| 现象 | 常见原因 |
|---|---|
| 401 | 没带 JWT 或 token 过期 |
| 405 | 前端 `POST`，后端要求 `GET`，请求方法不匹配 |
| 500 | SQL 语法错误、数据库异常或空指针 |
| 新增后没数据 | 事务没提交、SQL 条件错误或插入失败 |
| 修改错人 | `WHERE` 条件不正确 |
| XML 启动或查询报错 | 在 XML/SQL 中使用了错误的 `//` 注释 |
| 分页总数不对 | PageHelper 调用后没有紧跟分页 SQL |

## 十一、最终记忆框架

```text
一个请求
→ Controller 接收
→ Service 决定
→ Mapper 执行
→ MySQL 存储
→ Result 返回

一个对象
→ DTO 进门
→ Entity 入库
→ VO 出门

一套模板
→ 注解做标记
→ Spring 管对象
→ MyBatis 管 SQL
→ PageHelper 管分页
→ JWT 管身份
```

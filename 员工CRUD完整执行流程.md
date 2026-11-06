# 员工模块完整代码执行流程

这份文档按“浏览器实际发出请求”的顺序，完整讲解员工模块。

先看懂下面的整体模型，再看每个功能。

## 一、整体运行模型

```text
浏览器页面
→ Nginx（80端口）
→ Spring Boot Controller（8080端口）
→ Service
→ Mapper
→ MySQL（项目专用3307端口）
→ Controller
→ Nginx
→ 浏览器
```

后台请求有一条公共规则：

```text
浏览器请求 /api/...
→ Nginx 转发为 /admin/...
→ Spring Boot 接收
```

例如：

```text
浏览器：POST /api/employee
Nginx： POST /admin/employee
```

除了登录接口，后台 `/admin/**` 请求都要先经过 JWT 拦截器。

---

# 二、先认识代码里的基本角色

## 1. 类、对象和方法

类可以理解成模板：

```java
Employee
```

对象是根据模板创建出来的一份真实数据：

```java
Employee employee = new Employee();
```

方法是对象能够执行的动作：

```java
employee.getName();
employee.setName("测试员工");
```

## 2. DTO、Entity、VO

### DTO：前端送来的数据

```text
sky-pojo/src/main/java/com/sky/dto/EmployeeDTO.java
sky-pojo/src/main/java/com/sky/dto/EmployeeLoginDTO.java
sky-pojo/src/main/java/com/sky/dto/EmployeePageQueryDTO.java
```

用途：

```text
接收网页填写和提交的数据
```

### Entity：数据库中的完整一行

```text
sky-pojo/src/main/java/com/sky/entity/Employee.java
```

用途：

```text
表示 employee 表中的一条员工记录
```

### VO：返回网页的数据

```text
sky-pojo/src/main/java/com/sky/vo/EmployeeLoginVO.java
```

用途：

```text
只返回网页需要展示的字段
```

记忆：

```text
DTO 进门
Entity 入库
VO 出门
```

## 3. 三层代码的职责

| 层 | 角色 | 负责 |
|---|---|---|
| Controller | 前台 | 接收请求、调用 Service、返回 Result |
| Service | 经理 | 业务判断、补字段、调用 Mapper |
| Mapper | 仓库管理员 | 执行 SQL |

## 4. 固定注解

```text
@RestController  接收网页请求并返回 JSON
@RequestMapping  声明这一组接口的公共地址
@Service         声明业务对象
@Mapper          声明 MyBatis 数据库接口
@Autowired       由 Spring 自动注入另一个对象
@GetMapping      查询
@PostMapping     新增或执行动作
@PutMapping      修改
@DeleteMapping   删除
@RequestBody     把请求 JSON 转成 Java 对象
@PathVariable    读取 URL 路径中的参数
```

---

# 三、公共入口：JWT 拦截器

文件：

```text
sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java
```

检查范围在 `WebMvcConfiguration` 中设置：

```java
registry.addInterceptor(jwtTokenAdminInterceptor)
        .addPathPatterns("/admin/**")
        .excludePathPatterns("/admin/employee/login");
```

含义：

```text
所有 /admin/** 请求都要检查 JWT
只有 /admin/employee/login 不需要
```

拦截器关键代码：

```java
String token = request.getHeader(jwtProperties.getAdminTokenName());
Claims claims = JwtUtil.parseJWT(
        jwtProperties.getAdminSecretKey(),
        token
);
Long empId = Long.valueOf(
        claims.get(JwtClaimsConstant.EMP_ID).toString()
);
BaseContext.setCurrentId(empId);
```

执行过程：

```text
从请求头读取 token
→ 验证签名和有效期
→ 取出员工 id
→ 保存到 BaseContext
→ 请求继续进入 Controller
```

如果验证失败：

```text
返回 HTTP 401
不会进入 Controller
```

---

# 四、登录

## 请求

```text
POST /api/employee/login
```

Nginx 后：

```text
POST /admin/employee/login
```

请求 JSON：

```json
{
  "username": "admin",
  "password": "123456"
}
```

## 1. Controller

文件：

```text
sky-server/src/main/java/com/sky/controller/admin/EmployeeController.java
```

代码：

```java
@PostMapping("/login")
public Result<EmployeeLoginVO> login(
        @RequestBody EmployeeLoginDTO employeeLoginDTO
) {
    Employee employee = employeeService.login(employeeLoginDTO);

    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtClaimsConstant.EMP_ID, employee.getId());

    String token = JwtUtil.createJWT(
            jwtProperties.getAdminSecretKey(),
            jwtProperties.getAdminTtl(),
            claims
    );

    EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
            .id(employee.getId())
            .userName(employee.getUsername())
            .name(employee.getName())
            .token(token)
            .build();

    return Result.success(employeeLoginVO);
}
```

执行顺序：

```text
把 JSON 转成 EmployeeLoginDTO
→ 调用 Service 验证登录
→ 得到 Employee
→ 把员工 id 放进 JWT
→ 生成 token
→ 创建 EmployeeLoginVO
→ Result.success 包装返回
```

## 2. Service

文件：

```text
sky-server/src/main/java/com/sky/service/impl/EmployeeServiceImpl.java
```

代码：

```java
public Employee login(EmployeeLoginDTO employeeLoginDTO) {
    String username = employeeLoginDTO.getUsername();
    String password = employeeLoginDTO.getPassword();

    Employee employee = employeeMapper.getByUsername(username);

    if (employee == null) {
        throw new AccountNotFoundException(
                MessageConstant.ACCOUNT_NOT_FOUND
        );
    }

    String md5 = DigestUtils.md5DigestAsHex(
            password.getBytes()
    );

    if (!md5.equals(employee.getPassword())) {
        throw new PasswordErrorException(
                MessageConstant.PASSWORD_ERROR
        );
    }

    if (Objects.equals(
            employee.getStatus(),
            StatusConstant.DISABLE
    )) {
        throw new AccountLockedException(
                MessageConstant.ACCOUNT_LOCKED
        );
    }

    return employee;
}
```

检查顺序：

```text
员工是否存在
→ 密码指纹是否一致
→ status 是否为 0
```

## 3. Mapper

代码：

```java
@Select("select * from employee where username = #{username}")
Employee getByUsername(String username);
```

MySQL 实际执行：

```sql
SELECT *
FROM employee
WHERE username = 'admin';
```

## 4. 返回

成功：

```json
{
  "code": 1,
  "data": {
    "id": 1,
    "userName": "admin",
    "name": "管理员",
    "token": "eyJ..."
  }
}
```

失败：

```json
{
  "code": 0,
  "msg": "密码错误"
}
```

---

# 五、分页查询

## 请求

```text
GET /api/employee/page?page=1&pageSize=10&name=
```

Nginx 后：

```text
GET /admin/employee/page
```

## 1. Controller

```java
@GetMapping("/page")
public Result<PageResult> page(
        EmployeePageQueryDTO employeePageQueryDTO
) {
    PageResult result = employeeService.pageQuery(
            employeePageQueryDTO
    );
    return Result.success(result);
}
```

查询参数自动放进 DTO：

```text
page = 1
pageSize = 10
name = null
```

## 2. Service

```java
public PageResult pageQuery(EmployeePageQueryDTO dto) {
    PageHelper.startPage(
            dto.getPage(),
            dto.getPageSize()
    );

    Page<Employee> page =
            employeeMapper.pageQuery(dto);

    long total = page.getTotal();
    List<Employee> result = page.getResult();

    return new PageResult(total, result);
}
```

执行过程：

```text
PageHelper 设置第几页和每页条数
→ Mapper 执行查询
→ PageHelper 自动增加 count 查询
→ PageHelper 自动增加 LIMIT
→ Page 包含总数和当前页列表
→ PageResult 返回总数和列表
```

## 3. Mapper XML

文件：

```text
sky-server/src/main/resources/mapper/EmployeeMapper.xml
```

代码：

```xml
<select id="pageQuery"
        resultType="com.sky.entity.Employee">
    select * from employee
    <where>
        <if test="name != null and name != ''">
            and name like concat('%',#{name},'%')
        </if>
    </where>
    order by create_time desc
</select>
```

最终 SQL 接近：

```sql
SELECT *
FROM employee
WHERE name LIKE ?
ORDER BY create_time DESC
LIMIT ?;
```

## 4. 返回

```json
{
  "code": 1,
  "data": {
    "total": 3,
    "records": [
      {
        "id": 3,
        "name": "测试员工2"
      }
    ]
  }
}
```

---

# 六、新增员工

## 请求

```text
POST /api/employee
```

Nginx 后：

```text
POST /admin/employee
```

请求 JSON：

```json
{
  "name": "测试员工",
  "username": "test01",
  "phone": "13800000001",
  "sex": "1",
  "idNumber": "110101199001010011"
}
```

## 1. Controller

```java
@PostMapping
public Result save(
        @RequestBody EmployeeDTO employeeDTO
) {
    employeeService.save(employeeDTO);
    return Result.success();
}
```

## 2. Service

```java
public void save(EmployeeDTO employeeDTO) {
    Employee employee = new Employee();
    BeanUtils.copyProperties(employeeDTO, employee);

    employee.setStatus(StatusConstant.ENABLE);

    employee.setPassword(
            DigestUtils.md5DigestAsHex(
                    PasswordConstant.DEFAULT_PASSWORD.getBytes()
            )
    );

    LocalDateTime now = LocalDateTime.now();
    employee.setCreateTime(now);
    employee.setUpdateTime(now);

    Long id = BaseContext.getCurrentId();
    employee.setCreateUser(id);
    employee.setUpdateUser(id);

    employeeMapper.insert(employee);
}
```

网页字段：

```text
name
username
phone
sex
idNumber
```

Service 补的字段：

```text
password   = MD5(123456)
status     = 1
createTime = 当前时间
updateTime = 当前时间
createUser = 当前管理员id
updateUser = 当前管理员id
```

## 3. Mapper

```java
@Insert(
    "insert into employee " +
    "(name, username, password, phone, sex, id_number, " +
    "create_time, update_time, create_user, update_user, status) " +
    "values " +
    "(#{name},#{username},#{password},#{phone},#{sex}," +
    "#{idNumber},#{createTime},#{updateTime}," +
    "#{createUser},#{updateUser},#{status})"
)
void insert(Employee employee);
```

最终 SQL：

```sql
INSERT INTO employee (
    name, username, password, phone, sex, id_number,
    create_time, update_time, create_user, update_user, status
) VALUES (
    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
);
```

MyBatis 日志：

```text
Preparing: insert into employee ...
Parameters: 测试员工, test01, e10adc..., ...
Updates: 1
```

`Updates: 1` 表示数据库新增了 1 行。

---

# 七、根据 id 查询员工

## 请求

```text
GET /api/employee/2
```

Nginx 后：

```text
GET /admin/employee/2
```

## Controller

```java
@GetMapping("/{id}")
public Result<Employee> getById(
        @PathVariable("id") Long id
) {
    Employee employee = employeeService.getById(id);
    return Result.success(employee);
}
```

`@PathVariable` 表示从 URL 路径中读取参数：

```text
URL 中的 2 → id 变量
```

## Service

```java
public Employee getById(Long id) {
    return employeeMapper.getById(id);
}
```

## Mapper

```java
@Select("select * from employee where id = #{id}")
Employee getById(Long id);
```

SQL：

```sql
SELECT *
FROM employee
WHERE id = 2;
```

---

# 八、修改员工

## 请求

```text
PUT /api/employee
```

Nginx 后：

```text
PUT /admin/employee
```

请求 JSON：

```json
{
  "id": 2,
  "name": "测试员工-已修改",
  "username": "test01",
  "phone": "13800000001",
  "sex": "1",
  "idNumber": "110101199001010011"
}
```

修改时必须携带 `id`。

## 1. Controller

```java
@PutMapping
public Result update(
        @RequestBody EmployeeDTO employeeDTO
) {
    employeeService.update(employeeDTO);
    return Result.success();
}
```

## 2. Service

```java
public void update(EmployeeDTO employeeDTO) {
    Employee employee = new Employee();
    BeanUtils.copyProperties(employeeDTO, employee);

    employee.setUpdateTime(LocalDateTime.now());
    employee.setUpdateUser(BaseContext.getCurrentId());

    employeeMapper.update(employee);
}
```

## 3. Mapper XML

```xml
<update id="update" parameterType="Employee">
    update employee
    <set>
        <if test="name != null">
            name = #{name},
        </if>
        <if test="username != null">
            username = #{username},
        </if>
        <if test="phone != null">
            phone = #{phone},
        </if>
        <if test="sex != null">
            sex = #{sex},
        </if>
        <if test="idNumber != null">
            id_number = #{idNumber},
        </if>
        <if test="updateTime != null">
            update_time = #{updateTime},
        </if>
        <if test="updateUser != null">
            update_user = #{updateUser}
        </if>
    </set>
    where id = #{id}
</update>
```

`<set>` 的作用：

```text
生成 SET
自动去掉最后一个多余逗号
```

`<if>` 的作用：

```text
只有字段不为 null 时才修改
```

最终 SQL：

```sql
UPDATE employee
SET
    name = ?,
    username = ?,
    phone = ?,
    sex = ?,
    id_number = ?,
    update_time = ?,
    update_user = ?
WHERE id = ?;
```

必须保留 `WHERE id = ?`，否则可能修改整张表。

---

# 九、启用和禁用员工

## 请求

当前项目前端发送：

```text
POST /api/employee/status/0?id=2
```

Nginx 后：

```text
POST /admin/employee/status/0?id=2
```

含义：

```text
status = 0  禁用
id = 2      修改 2 号员工
```

## 1. Controller

```java
@PostMapping("/status/{status}")
public Result startOrStop(
        @PathVariable("status") Integer status,
        Long id
) {
    employeeService.startOrStop(status, id);
    return Result.success();
}
```

参数来源：

```text
URL 路径 /status/0 中的 0 → status
查询参数 ?id=2 中的 2     → id
```

## 2. Service

```java
public void startOrStop(Integer status, Long id) {
    Employee employee = Employee.builder()
            .updateTime(LocalDateTime.now())
            .id(id)
            .status(status)
            .build();

    employeeMapper.update(employee);
}
```

创建的对象只有：

```text
id
status
updateTime
```

其他字段是 null，所以 Mapper 的动态 SQL 不会修改它们。

## 3. SQL

```sql
UPDATE employee
SET
    update_time = ?,
    update_user = ?,
    status = ?
WHERE id = ?;
```

状态约定：

```text
status = 1  启用
status = 0  禁用
```

登录时会检查：

```java
if (Objects.equals(
        employee.getStatus(),
        StatusConstant.DISABLE
)) {
    throw new AccountLockedException();
}
```

因此禁用后不能登录。

---

# 十、统一返回 Result

文件：

```text
sky-common/src/main/java/com/sky/result/Result.java
```

三个字段：

```java
private Integer code;
private String msg;
private T data;
```

成功：

```java
Result.success(data)
```

返回：

```json
{
  "code": 1,
  "msg": null,
  "data": "..."
}
```

失败：

```java
Result.error("错误提示")
```

返回：

```json
{
  "code": 0,
  "msg": "错误提示",
  "data": null
}
```

---

# 十一、常见业务异常

Service 发现业务错误时使用：

```java
throw new PasswordErrorException("密码错误");
```

全局异常处理器：

```text
sky-server/src/main/java/com/sky/handler/GlobalExceptionHandler.java
```

会把异常转换成：

```java
Result.error(ex.getMessage())
```

浏览器最终显示：

```text
密码错误
```

---

# 十二、五个功能对比

| 功能 | 请求 | Controller | Service | Mapper | SQL |
|---|---|---|---|---|---|
| 登录 | POST `/login` | `login` | `login` | `getByUsername` | SELECT |
| 列表 | GET `/page` | `page` | `pageQuery` | `pageQuery` | SELECT + LIMIT |
| 单个查询 | GET `/{id}` | `getById` | `getById` | `getById` | SELECT by id |
| 新增 | POST `/` | `save` | `save` | `insert` | INSERT |
| 修改 | PUT `/` | `update` | `update` | `update` | UPDATE |
| 禁用启用 | POST `/status/{status}` | `startOrStop` | `startOrStop` | `update` | UPDATE status |

员工模块没有物理删除功能，而是通过 `status=0` 禁用账号。

---

# 十三、固定模板和自己编写

## 固定模板

```text
@RestController
@RequestMapping
@Service
@Mapper
@Autowired
@GetMapping
@PostMapping
@PutMapping
@RequestBody
@PathVariable
Result.success
Result.error
PageHelper.startPage
BeanUtils.copyProperties
@Select
@Insert
@Update
@Delete
MyBatis XML 的 select/insert/update/delete/set/where/if
```

## 自己编写的业务

```text
接口地址
请求方法
方法名称
参数对象
需要检查哪些业务规则
默认密码和状态
调用哪个 Mapper
SQL 表名和字段
WHERE 条件
排序和分页
返回给网页的字段
```

---

# 十四、阅读一个新模块的顺序

```text
第一步：找 Controller
第二步：看请求方法和地址
第三步：找 Service 方法
第四步：看业务规则
第五步：找 Mapper 方法
第六步：Java 注解或 XML 中找 SQL
第七步：Navicat 验证数据变化
```

不要刚开始就逐行阅读整个项目。

每次只追一个功能：

```text
一个请求
一个 Controller 方法
一个 Service 方法
一个 Mapper 方法
一条 SQL
```

这就是最省力的阅读项目方法。

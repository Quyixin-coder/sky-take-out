# 苍穹外卖

基于 Spring Boot、MyBatis、MySQL、Redis、Vue 管理端和 uni-app 微信小程序的餐饮外卖全栈学习项目。

项目包含员工、分类、菜品、套餐、购物车、用户下单、支付、订单状态流转、催单、超时取消、营业状态和运营数据统计等完整业务。

## 技术栈

- Java 21
- Spring Boot 2.7
- Spring MVC
- MyBatis
- MySQL
- Redis
- JWT
- WebSocket
- PageHelper
- Apache POI
- Nginx
- Vue 管理端
- uni-app 微信小程序

## 项目结构

```text
sky-take-out
├── sky-common      公共常量、工具和异常
├── sky-pojo        Entity、DTO、VO
├── sky-server      Controller、Service、Mapper、配置和启动类
├── Web             Nginx 与管理员前端静态资源
├── WeiXinApp       微信小程序端
└── 本地启动.ps1    本地环境启动脚本
```

## 本地端口

| 服务 | 端口 |
|---|---:|
| Nginx | 80 |
| Spring Boot | 8080 |
| MySQL | 3307 |
| Redis | 6379 |

## 启动步骤

### 1. 初始化数据库

将以下 SQL 文件导入 MySQL：

```text
sky_take_out.sql
```

默认数据库名称：

```text
sky_take_out
```

本地教学数据库账号配置在：

```text
sky-server/src/main/resources/application-dev.yml
```

本地私密配置放在：

```text
sky-server/src/main/resources/application-local.yml
```

该文件已被 `.gitignore` 忽略，不应提交到 GitHub。

### 2. 启动基础服务

使用 PowerShell 执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start-infra.ps1 -DependenciesOnly
```

该命令只启动 MySQL、Redis 和 Nginx。

### 3. 启动后端

在 IDEA 中打开项目根目录并运行：

```text
sky-server/src/main/java/com/sky/SkyApplication.java
```

启动成功后访问：

```text
http://localhost:8080/doc.html
```

### 4. 打开管理员端

```text
http://localhost/
```

默认管理员账号：

```text
用户名：admin
密码：123456
```

### 5. 打开小程序

使用微信开发者工具打开：

```text
WeiXinApp
```

小程序接口地址使用局域网 IP。更换网络后需要根据当前电脑 IP 更新接口地址。

## 常用入口

```text
管理员后台：http://localhost/
接口文档：  http://localhost:8080/doc.html
顾客体验页：http://localhost/customer.html
接口测试页：http://localhost/user-test.html
```

## 学习文档

- [新手项目地图](新手项目地图.md)
- [员工 CRUD 总结](员工CRUD总结.md)
- [员工 CRUD 完整执行流程](员工CRUD完整执行流程.md)

## 说明

本项目用于学习和简历展示。微信 AppSecret、阿里云密钥和其他本地私密配置不得提交到 Git 仓库。

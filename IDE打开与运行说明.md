# 用 VS Code 或 IntelliJ IDEA 打开项目

## 第一次打开前

打开项目时一定选择这个根目录：

```text
E:\quyixin\2026-10-28\new-chat\outputs\sky-take-out
```

不要只打开 `sky-server` 子目录。根目录里有 `pom.xml`，Maven 会通过它认识三个后端模块。

## 使用 VS Code

1. 打开 VS Code，选择“文件 → 打开文件夹”，打开上面的根目录。
2. 在扩展商店安装 Java Extension Pack；VS Code 检测到推荐扩展时也可以直接安装。
3. 等待右下角 Maven / Java 项目加载完成。
4. 双击 `START-DEPENDENCIES.bat`，启动 MySQL、Redis 和 Nginx。
5. 按 `F5`，或者打开“运行和调试”，选择“运行 SkyApplication”。
6. 看到 `Started SkyApplication` 后，打开 <http://localhost/>。

如果按 F5 提示找不到主类，先执行一次：

```text
终端 → 运行任务 → Maven: 编译后端
```

## 使用 IntelliJ IDEA

1. 选择“Open”，打开上面的根目录。
2. 等待右下角的 Maven 导入完成。
3. 打开 `sky-server/src/main/java/com/sky/SkyApplication.java`。
4. 双击 `START-DEPENDENCIES.bat`，启动 MySQL、Redis 和 Nginx。
5. 点击 `SkyApplication` 类左侧的绿色三角，或者从运行配置中选择 `SkyApplication`，然后点击 Run。
6. 看到 `Started SkyApplication` 后，打开 <http://localhost/>。

如果 IDEA 提示 JDK 不匹配，在 `File → Project Structure → Project SDK` 中选择本机 JDK 21。IntelliJ IDEA 2022.2 版本较旧，如果界面不能正确识别 JDK 21，可以改用 JDK 17。

## 以后每天如何运行

方案一：不打开 IDE，直接双击 `START-PROJECT.bat`。

方案二：在 IDE 中学习和调试：

1. 双击 `START-DEPENDENCIES.bat`。
2. 在 VS Code 或 IDEA 中启动 `SkyApplication`。
3. 从 IDE 的停止按钮结束后端。
4. 双击 `STOP-DEPENDENCIES.bat`，结束数据库、Redis 和 Nginx。

## 常见问题

### 后端提示 8080 端口被占用

说明已经有一个后端正在运行。先停止旧的后端，或者在任务管理器中结束对应的 Java 进程。

### 网页打开但没有数据

依次检查：

1. MySQL 是否监听 3306。
2. Redis 是否监听 6379。
3. IDE 控制台是否出现 `Started SkyApplication`。
4. 是否已经运行 `IDE启动依赖.bat`。

### 修改代码后页面没有变化

后端 Java 代码必须在 IDE 中重新启动。前端是已经构建好的静态文件，修改 Java 后端后不需要重装前端。

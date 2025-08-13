# TestMachine 可执行文件构建说明

## 概述
本项目提供了将JavaFX应用程序打包成独立.exe可执行文件的完整解决方案，生成的exe文件包含Java运行时，无需目标机器安装Java环境。

## 前置要求

### 必需软件
1. **JDK 17或更高版本** - 必须包含jpackage工具
   - 推荐使用Oracle JDK 17+ 或 OpenJDK 17+
   - 验证：`java -version` 和 `jpackage --version`

2. **Maven 3.6+**
   - 验证：`mvn -version`

3. **Windows 10/11** (用于创建.exe文件)

### 验证环境
运行以下命令验证环境：
```bash
java -version
javac -version
jpackage --version
mvn -version
```

## 构建步骤

### 方法1：一键构建（推荐）
```bash
# 双击运行或在命令行执行
build-executable.bat
```

这将：
1. 清理并编译项目
2. 创建包含所有依赖的Fat JAR
3. 使用jpackage创建独立的.exe文件

### 方法2：分步构建

#### 步骤1：仅构建JAR
```bash
build-jar-only.bat
```

#### 步骤2：测试JAR
```bash
test-jar.bat
```

#### 步骤3：创建exe（如果JAR测试成功）
```bash
build-executable.bat
```

## 输出文件

### JAR文件
- 位置：`target/TestMachine-1.0-SNAPSHOT.jar`
- 大小：约 50-100MB（包含所有依赖）
- 运行：`java -jar target/TestMachine-1.0-SNAPSHOT.jar`

### EXE文件
- 位置：`target/dist/TestMachine.exe`
- 大小：约 150-300MB（包含Java运行时）
- 运行：直接双击或命令行执行

## 技术细节

### 打包技术
- **Maven Shade Plugin**: 创建Fat JAR，包含所有依赖
- **jpackage**: JDK内置工具，创建原生安装包和可执行文件

### 主要配置
- 主类：`com.del.testmechine.Launcher`
- JavaFX版本：17.0.1
- Java版本：17
- 编码：UTF-8

### 依赖项
- JavaFX Controls & FXML
- jSerialComm (串口通信)
- SLF4J + Logback (日志)

## 故障排除

### 常见问题

1. **jpackage命令未找到**
   - 确保使用JDK 17+（不是JRE）
   - 检查JAVA_HOME环境变量
   - 重新安装JDK并确保bin目录在PATH中

2. **编译失败**
   - 检查Java版本：`java -version`
   - 清理Maven缓存：`mvn clean`
   - 检查网络连接（Maven需要下载依赖）

3. **JAR运行失败**
   - 检查JavaFX模块是否正确
   - 验证主类路径
   - 查看控制台错误信息

4. **exe文件过大**
   - 这是正常的，因为包含了完整的Java运行时
   - 可以使用jlink创建自定义运行时来减小大小

5. **exe在其他电脑上无法运行**
   - 确保目标电脑是Windows 10/11
   - 检查是否有杀毒软件阻止
   - 尝试以管理员身份运行

### 调试技巧

1. **查看详细日志**
   ```bash
   # 在build-executable.bat中添加 --verbose 参数
   jpackage --verbose ...
   ```

2. **测试JAR文件**
   ```bash
   # 先确保JAR文件能正常运行
   java -jar target/TestMachine-1.0-SNAPSHOT.jar
   ```

3. **检查依赖**
   ```bash
   # 查看JAR文件内容
   jar -tf target/TestMachine-1.0-SNAPSHOT.jar
   ```

## 分发说明

### 单文件分发
生成的`TestMachine.exe`是一个独立的可执行文件，可以：
- 直接复制到其他Windows电脑运行
- 无需安装Java环境
- 包含所有必要的依赖和资源

### 注意事项
- exe文件较大（150-300MB）是正常的
- 首次启动可能较慢（解压运行时）
- 建议在目标环境测试兼容性

## 高级选项

### 自定义图标
在build-executable.bat中添加：
```bash
--icon "src/main/resources/app-icon.ico"
```

### 添加启动参数
```bash
--java-options "-Xmx2g -Duser.language=zh -Duser.country=CN"
```

### 创建安装程序
将`--type exe`改为`--type msi`可创建Windows安装程序。

@echo off
chcp 65001 >nul
echo ========================================
echo 快速构建JAR文件（跳过exe创建）
echo ========================================
echo.

echo 清理并编译...
call mvn clean compile
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo 创建Fat JAR...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo 打包失败！
    pause
    exit /b 1
)

echo.
echo ✅ JAR文件创建成功！
echo 文件位置: target\TestMachine-1.0-SNAPSHOT.jar
echo.
echo 可以使用以下命令运行：
echo java -jar target\TestMachine-1.0-SNAPSHOT.jar
echo.
echo 或者运行 test-jar.bat 来测试
echo.
pause

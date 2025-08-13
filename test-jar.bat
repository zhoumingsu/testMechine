@echo off
chcp 65001 >nul
echo ========================================
echo 测试JAR文件
echo ========================================
echo.

set JAR_NAME=TestMachine-1.0-SNAPSHOT.jar

if exist "target\%JAR_NAME%" (
    echo JAR文件存在，启动测试...
    echo 文件: target\%JAR_NAME%
    echo.
    java -jar "target\%JAR_NAME%"
) else (
    echo JAR文件不存在: target\%JAR_NAME%
    echo 请先运行 build-executable.bat 构建项目
    echo.
    pause
)

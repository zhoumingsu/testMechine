@echo off
chcp 65001 >nul
echo ========================================
echo 构建TestMachine独立可执行文件
echo ========================================
echo.

:: 设置变量
set APP_NAME=TestMachine
set APP_VERSION=1.0
set VENDOR=DEL Technology
set MAIN_CLASS=com.del.testmechine.Launcher
set JAR_NAME=TestMachine-1.0-SNAPSHOT.jar

echo 步骤1: 清理并编译项目...
call mvn clean compile
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo 步骤2: 创建Fat JAR...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo 打包失败！
    pause
    exit /b 1
)

echo.
echo 步骤3: 检查JAR文件...
if not exist "target\%JAR_NAME%" (
    echo 错误：JAR文件不存在 - target\%JAR_NAME%
    pause
    exit /b 1
)

echo JAR文件创建成功: target\%JAR_NAME%

echo.
echo 步骤4: 创建输出目录...
if not exist "target\dist" mkdir "target\dist"

echo.
echo 步骤5: 使用jpackage创建独立可执行文件...
echo 这可能需要几分钟时间，请耐心等待...

jpackage ^
    --type exe ^
    --name "%APP_NAME%" ^
    --app-version "%APP_VERSION%" ^
    --vendor "%VENDOR%" ^
    --input target ^
    --main-jar "%JAR_NAME%" ^
    --main-class "%MAIN_CLASS%" ^
    --dest "target\dist" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --java-options "-Djava.awt.headless=false" ^
    --win-console

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo 🎉 成功创建独立可执行文件！
    echo ========================================
    echo.
    echo 文件位置: target\dist\%APP_NAME%.exe
    echo 文件大小: 
    for %%A in ("target\dist\%APP_NAME%.exe") do echo %%~zA 字节
    echo.
    echo 该exe文件包含了Java运行时，可以在没有安装Java的电脑上直接运行。
    echo.
    echo 是否打开文件夹查看？(Y/N)
    set /p choice=
    if /i "%choice%"=="Y" (
        explorer "target\dist"
    )
) else (
    echo.
    echo ❌ jpackage执行失败
    echo 请检查：
    echo 1. 是否安装了JDK 17或更高版本
    echo 2. jpackage命令是否在PATH中
    echo 3. 是否有足够的磁盘空间
)

echo.
pause

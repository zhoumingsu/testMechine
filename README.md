# TestMachine

一个基于JavaFX的测试机器应用程序。

## 项目描述

这是一个使用JavaFX开发的桌面应用程序，用于测试机器控制和数据处理。

## 技术栈

- Java 17
- JavaFX 17.0.1
- Maven
- jSerialComm (串口通信)
- SLF4J + Logback (日志)

## 项目结构

```
testMechine/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── config/
├── logs/
├── target/
└── pom.xml
```

## 运行要求

- Java 17 或更高版本
- Maven 3.6 或更高版本

## 如何运行

1. 克隆项目到本地
2. 在项目根目录执行：
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

## 功能特性

- JavaFX图形界面
- 串口通信支持
- 日志记录
- 配置文件管理

## 许可证

本项目采用开源许可证。

package com.del.testmechine;

/**
 * JavaFX应用程序启动器
 * 用于解决模块化环境下的JavaFX启动问题和jpackage打包问题
 * 这个类不继承Application，避免模块路径问题
 */
public class Launcher {
    public static void main(String[] args) {
        // 设置JavaFX系统属性，确保在没有模块路径的环境下也能运行
        System.setProperty("javafx.preloader", "");
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        
        // 设置文件编码
        System.setProperty("file.encoding", "UTF-8");
        
        // 启动JavaFX应用程序
        HelloApplication.main(args);
    }
}

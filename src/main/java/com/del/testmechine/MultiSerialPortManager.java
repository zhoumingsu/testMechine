package com.del.testmechine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 多串口管理器，支持四个测试区域同时使用不同的串口
 */
public class MultiSerialPortManager {
    
    // 为每个测试区域维护独立的串口管理器
    private final Map<Integer, SerialPortManager> areaSerialManagers = new ConcurrentHashMap<>();
    
    // 测试区域到串口的映射
    private final Map<Integer, String> areaPortMapping = new ConcurrentHashMap<>();
    
    // 串口到测试区域的反向映射
    private final Map<String, Integer> portAreaMapping = new ConcurrentHashMap<>();
    
    // 全局日志回调
    private Consumer<String> globalLogCallback;
    
    // 区域数据接收回调 (areaNumber, data)
    private BiConsumer<Integer, String> areaDataReceivedCallback;
    
    public MultiSerialPortManager() {
        // 初始化六个测试区域的串口管理器
        for (int i = 1; i <= 6; i++) {
            areaSerialManagers.put(i, new SerialPortManager());
        }
    }
    
    /**
     * 设置全局日志回调
     */
    public void setGlobalLogCallback(Consumer<String> callback) {
        this.globalLogCallback = callback;
        
        // 为每个串口管理器设置日志回调
        for (Map.Entry<Integer, SerialPortManager> entry : areaSerialManagers.entrySet()) {
            int areaNumber = entry.getKey();
            SerialPortManager manager = entry.getValue();
            manager.setLogCallback(message -> {
                if (globalLogCallback != null) {
                    globalLogCallback.accept(String.format("[区域%d] %s", areaNumber, message));
                }
            });
        }
    }
    
    /**
     * 设置区域数据接收回调
     */
    public void setAreaDataReceivedCallback(BiConsumer<Integer, String> callback) {
        this.areaDataReceivedCallback = callback;
        
        // 为每个串口管理器设置数据接收回调
        for (Map.Entry<Integer, SerialPortManager> entry : areaSerialManagers.entrySet()) {
            int areaNumber = entry.getKey();
            SerialPortManager manager = entry.getValue();
            manager.setDataReceivedCallback(data -> {
                if (areaDataReceivedCallback != null) {
                    areaDataReceivedCallback.accept(areaNumber, data);
                }
            });
        }
    }
    
    /**
     * 获取可用的串口列表
     */
    public List<String> getAvailablePorts() {
        // 使用任意一个串口管理器来获取可用串口列表
        return areaSerialManagers.get(1).getAvailablePorts();
    }
    
    /**
     * 为指定测试区域连接串口
     */
    public boolean connectAreaPort(int areaNumber, String portName, int baudRate) {
        if (areaNumber < 1 || areaNumber > 6) {
            log("错误: 无效的测试区域编号: " + areaNumber);
            return false;
        }
        
        // 检查串口是否已被其他区域使用
        if (portAreaMapping.containsKey(portName)) {
            int occupiedArea = portAreaMapping.get(portName);
            if (occupiedArea != areaNumber) {
                log(String.format("错误: 串口 %s 已被测试区域%d使用", portName, occupiedArea));
                return false;
            }
        }
        
        SerialPortManager manager = areaSerialManagers.get(areaNumber);
        boolean connected = manager.connect(portName, baudRate);
        
        if (connected) {
            // 更新映射关系
            String oldPort = areaPortMapping.get(areaNumber);
            if (oldPort != null) {
                portAreaMapping.remove(oldPort);
            }
            
            areaPortMapping.put(areaNumber, portName);
            portAreaMapping.put(portName, areaNumber);
            
            log(String.format("测试区域%d成功连接串口: %s", areaNumber, portName));
        }
        
        return connected;
    }
    
    /**
     * 断开指定测试区域的串口连接
     */
    public boolean disconnectAreaPort(int areaNumber) {
        if (areaNumber < 1 || areaNumber > 6) {
            log("错误: 无效的测试区域编号: " + areaNumber);
            return false;
        }
        
        SerialPortManager manager = areaSerialManagers.get(areaNumber);
        boolean disconnected = manager.disconnect();
        
        if (disconnected) {
            // 清理映射关系
            String portName = areaPortMapping.remove(areaNumber);
            if (portName != null) {
                portAreaMapping.remove(portName);
            }
            
            log(String.format("测试区域%d串口连接已断开", areaNumber));
        }
        
        return disconnected;
    }
    
    /**
     * 检查指定测试区域的连接状态
     */
    public boolean isAreaConnected(int areaNumber) {
        if (areaNumber < 1 || areaNumber > 6) {
            return false;
        }
        
        SerialPortManager manager = areaSerialManagers.get(areaNumber);
        return manager.isConnected();
    }
    
    /**
     * 获取指定测试区域连接的串口名称
     */
    public String getAreaConnectedPort(int areaNumber) {
        if (areaNumber < 1 || areaNumber > 6) {
            return null;
        }
        
        return areaPortMapping.get(areaNumber);
    }
    
    /**
     * 向指定测试区域的串口发送数据
     */
    public boolean sendDataToArea(int areaNumber, String data) {
        if (areaNumber < 1 || areaNumber > 6) {
            log("错误: 无效的测试区域编号: " + areaNumber);
            return false;
        }
        
        SerialPortManager manager = areaSerialManagers.get(areaNumber);
        return manager.sendData(data);
    }
    
    /**
     * 向指定测试区域的串口发送数据（字节数组）
     */
    public boolean sendDataToArea(int areaNumber, byte[] data) {
        if (areaNumber < 1 || areaNumber > 6) {
            log("错误: 无效的测试区域编号: " + areaNumber);
            return false;
        }
        
        SerialPortManager manager = areaSerialManagers.get(areaNumber);
        return manager.sendData(data);
    }
    
    /**
     * 断开所有串口连接
     */
    public void disconnectAll() {
        for (int i = 1; i <= 6; i++) {
            disconnectAreaPort(i);
        }
        log("所有串口连接已断开");
    }
    
    /**
     * 获取所有区域的连接状态
     */
    public Map<Integer, Boolean> getAllConnectionStatus() {
        Map<Integer, Boolean> status = new ConcurrentHashMap<>();
        for (int i = 1; i <= 6; i++) {
            status.put(i, isAreaConnected(i));
        }
        return status;
    }
    
    /**
     * 获取所有区域的串口映射
     */
    public Map<Integer, String> getAllPortMappings() {
        return new ConcurrentHashMap<>(areaPortMapping);
    }
    
    /**
     * 检查是否有任何区域已连接
     */
    public boolean hasAnyConnection() {
        for (int i = 1; i <= 6; i++) {
            if (isAreaConnected(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取已连接的区域数量
     */
    public int getConnectedAreaCount() {
        int count = 0;
        for (int i = 1; i <= 6; i++) {
            if (isAreaConnected(i)) {
                count++;
            }
        }
        return count;
    }
    
    private void log(String message) {
        if (globalLogCallback != null) {
            globalLogCallback.accept(message);
        }
    }
}

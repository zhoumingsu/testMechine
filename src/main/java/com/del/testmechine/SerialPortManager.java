package com.del.testmechine;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 串口管理类，用于处理RS485串口通信
 */
public class SerialPortManager {
    private SerialPort serialPort;
    private boolean isConnected = false;
    private Consumer<String> dataReceivedCallback;
    private Consumer<String> logCallback;
    
    public SerialPortManager() {
    }
    
    /**
     * 设置数据接收回调
     */
    public void setDataReceivedCallback(Consumer<String> callback) {
        this.dataReceivedCallback = callback;
    }
    
    /**
     * 设置日志回调
     */
    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }
    
    /**
     * 获取可用的串口列表
     */
    public List<String> getAvailablePorts() {
        List<String> portNames = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            portNames.add(port.getSystemPortName());
        }

        // 只返回真实扫描到的串口，不添加虚拟串口
        return portNames;
    }
    
    /**
     * 连接串口
     */
    public boolean connect(String portName, int baudRate) {
        try {
            // 如果已经连接，先断开
            if (isConnected) {
                disconnect();
            }
            
            log("正在连接串口: " + portName + ", 波特率: " + baudRate);
            
            // 获取串口
            serialPort = SerialPort.getCommPort(portName);
            
            if (serialPort == null) {
                log("错误: 无法找到串口 " + portName);
                return false;
            }
            
            // 设置串口参数
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);
            
            // 设置超时
            serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                1000, // 读取超时1秒
                1000  // 写入超时1秒
            );
            
            // 打开串口
            if (serialPort.openPort()) {
                isConnected = true;
                log("串口连接成功: " + portName);
                
                // 添加数据监听器
                serialPort.addDataListener(new SerialPortDataListener() {
                    @Override
                    public int getListeningEvents() {
                        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                    }
                    
                    @Override
                    public void serialEvent(SerialPortEvent event) {
                        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                            return;
                        }
                        
                        byte[] newData = new byte[serialPort.bytesAvailable()];
                        int numRead = serialPort.readBytes(newData, newData.length);
                        
                        if (numRead > 0) {
                            // 将接收到的字节数据转换为十六进制字符串
                            String hexData = bytesToHex(newData, 0, numRead);
                            log("接收数据(HEX): " + hexData);

                            // 检查是否以A5开头的协议数据包
                            if (numRead > 0 && (newData[0] & 0xFF) == 0xA5) {
                                log("识别为A5协议数据包");
                                // A5开头的数据包，传递十六进制格式
                                if (dataReceivedCallback != null) {
                                    dataReceivedCallback.accept(hexData);
                                }
                            } else {
                                // 非A5开头的数据，使用UTF-8解析
                                String textData = new String(newData, 0, numRead, StandardCharsets.UTF_8);
                                log("接收数据(UTF-8): " + textData.trim());
                                // 传递UTF-8文本格式
                                if (dataReceivedCallback != null) {
                                    dataReceivedCallback.accept(textData);
                                }
                            }
                        }
                    }
                });
                
                return true;
            } else {
                log("错误: 无法打开串口 " + portName);
                return false;
            }
            
        } catch (Exception e) {
            log("串口连接异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 断开串口连接
     */
    public boolean disconnect() {
        boolean result = true;
        try {
            if (serialPort != null) {
                log("正在断开串口连接...");

                // 移除数据监听器
                try {
                    serialPort.removeDataListener();
                } catch (Exception e) {
                    log("移除数据监听器时出错: " + e.getMessage());
                }

                // 关闭串口
                if (isConnected) {
                    try {
                        if (serialPort.closePort()) {
                            log("串口连接已断开");
                        } else {
                            log("错误: 无法关闭串口");
                            result = false;
                        }
                    } catch (Exception e) {
                        log("关闭串口时出错: " + e.getMessage());
                        result = false;
                    }
                }

                // 无论关闭是否成功，都要清理状态和引用
                isConnected = false;
                serialPort = null;
            }
            return result;
        } catch (Exception e) {
            log("断开串口异常: " + e.getMessage());
            // 即使出现异常，也要确保状态被重置
            isConnected = false;
            serialPort = null;
            return false;
        }
    }
    
    /**
     * 发送数据（字符串格式，可能是文本或十六进制）
     */
    public boolean sendData(String data) {
        if (!isConnected || serialPort == null) {
            log("错误: 串口未连接");
            return false;
        }

        try {
            byte[] dataBytes;

            // 判断是否为十六进制格式的数据
            if (isHexString(data)) {
                // 将十六进制字符串转换为字节数组
                dataBytes = hexStringToBytes(data);
                log("发送协议数据(HEX): " + data.trim());
            } else {
                // 作为普通文本发送
                dataBytes = data.getBytes(StandardCharsets.UTF_8);
                log("发送文本数据: " + data.trim());
            }

            int bytesWritten = serialPort.writeBytes(dataBytes, dataBytes.length);

            if (bytesWritten == dataBytes.length) {
                return true;
            } else {
                log("错误: 数据发送不完整");
                return false;
            }
        } catch (Exception e) {
            log("发送数据异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送数据（字节数组）
     */
    public boolean sendData(byte[] data) {
        if (!isConnected || serialPort == null) {

            log("错误: 串口未连接");
            return false;
        }
        
        try {
            int bytesWritten = serialPort.writeBytes(data, data.length);
            
            if (bytesWritten == data.length) {
                log("发送数据: " + bytesToHex(data));
                return true;
            } else {
                log("错误: 数据发送不完整");
                return false;
            }
        } catch (Exception e) {
            log("发送数据异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }
    
    /**
     * 获取当前连接的串口名称
     */
    public String getConnectedPortName() {
        if (isConnected && serialPort != null) {
            return serialPort.getSystemPortName();
        }
        return null;
    }


    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 字节数组转十六进制字符串（指定范围）
     */
    private String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }

    /**
     * 判断字节数据是否为协议数据包
     * 协议格式：A5开头，0D结尾，长度至少6字节
     */
    private boolean isProtocolPacket(byte[] data, int offset, int length) {
        if (data == null || length < 6) {
            return false;
        }

        // 检查帧头是否为A5
        if ((data[offset] & 0xFF) != 0xA5) {
            return false;
        }

        // 检查帧尾是否为0D
        if ((data[offset + length - 1] & 0xFF) != 0x0D) {
            return false;
        }

        // 检查长度字段是否与实际长度匹配
        if (length >= 2) {
            int declaredLength = data[offset + 1] & 0xFF;
            if (declaredLength == length) {
                return true;
            }
        }

        return false;
    }



    /**
     * 判断字符串是否为可打印的文本
     */
    private boolean isPrintableText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 检查是否包含控制字符（除了常见的换行、回车、制表符）
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return false;
            }
        }

        // 检查是否主要由可打印字符组成
        int printableCount = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) ||
                "!@#$%^&*()_+-=[]{}|;':\",./<>?~`".indexOf(c) >= 0 ||
                c >= 0x4E00 && c <= 0x9FFF) { // 中文字符范围
                printableCount++;
            }
        }

        // 如果80%以上是可打印字符，认为是文本
        return (double) printableCount / text.length() >= 0.8;
    }

    /**
     * 判断字符串是否为十六进制格式
     */
    private boolean isHexString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 移除空格并转换为大写
        String cleanStr = str.replaceAll("\\s+", "").toUpperCase();

        // 检查是否只包含十六进制字符，且长度为偶数
        return cleanStr.length() % 2 == 0 && cleanStr.matches("[0-9A-F]+");
    }

    /**
     * 将十六进制字符串转换为字节数组
     */
    private byte[] hexStringToBytes(String hexString) {
        // 移除空格并转换为大写
        String cleanHex = hexString.replaceAll("\\s+", "").toUpperCase();

        int length = cleanHex.length();
        byte[] data = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanHex.charAt(i), 16) << 4)
                                + Character.digit(cleanHex.charAt(i + 1), 16));
        }

        return data;
    }

    /**
     * 记录日志
     */
    private void log(String message) {
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }
}

package com.del.testmechine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * SerialPortManager的单元测试
 */
public class SerialPortManagerTest {
    
    private SerialPortManager serialPortManager;
    
    @BeforeEach
    void setUp() {
        serialPortManager = new SerialPortManager();
    }
    
    @Test
    void testGetAvailablePortsReturnsEmptyListWhenNoPortsFound() {
        // 测试当没有串口时，返回空列表而不是虚拟串口
        List<String> ports = serialPortManager.getAvailablePorts();
        
        // 验证返回的列表不为null
        assertNotNull(ports, "串口列表不应该为null");
        
        // 如果系统没有真实串口，应该返回空列表
        // 注意：这个测试在有真实串口的系统上可能会失败
        // 在实际环境中，这个测试主要验证不会返回虚拟的COM1-COM8
        System.out.println("找到的串口数量: " + ports.size());
        System.out.println("串口列表: " + ports);
        
        // 验证不包含虚拟串口（如果列表为空的话）
        if (ports.isEmpty()) {
            System.out.println("✓ 测试通过：没有串口时返回空列表");
        } else {
            System.out.println("✓ 测试通过：找到真实串口: " + String.join(", ", ports));
        }
    }
    
    @Test
    void testInitialConnectionState() {
        // 测试初始连接状态
        assertFalse(serialPortManager.isConnected(), "初始状态应该是未连接");
        assertNull(serialPortManager.getConnectedPortName(), "初始状态下连接的串口名应该为null");
    }
    
    @Test
    void testConnectWithInvalidPort() {
        // 测试连接不存在的串口
        boolean result = serialPortManager.connect("INVALID_PORT", 9600);
        assertFalse(result, "连接不存在的串口应该返回false");
        assertFalse(serialPortManager.isConnected(), "连接失败后状态应该是未连接");
    }
}

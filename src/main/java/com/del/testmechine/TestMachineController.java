package com.del.testmechine;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMachineController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(TestMachineController.class);


    
    // 全局控制控件（保留在新布局中）
    @FXML private CheckBox autoModeCheckBox;
    @FXML private CheckBox singleTestCheckBox;
    @FXML private Button refreshPortsButton;

    // 状态监控
    @FXML private Label connectionStatusLabel;
    @FXML private Label statusLabel;
    @FXML private Label successCountLabel;
    @FXML private Label failureCountLabel;
    @FXML private Label versionLabel;
    
    // 六个测试区域的TextArea
    @FXML private TextArea testArea1;
    @FXML private TextArea testArea2;
    @FXML private TextArea testArea3;
    @FXML private TextArea testArea4;
    @FXML private TextArea testArea5;
    @FXML private TextArea testArea6;

    // 六个测试区域的按钮
    @FXML private Button testButton1;
    @FXML private Button testButton2;
    @FXML private Button testButton3;
    @FXML private Button testButton4;
    @FXML private Button testButton5;
    @FXML private Button testButton6;

    // 六个测试区域的串口选择控件
    @FXML private ComboBox<String> area1PortComboBox;
    @FXML private ComboBox<String> area2PortComboBox;
    @FXML private ComboBox<String> area3PortComboBox;
    @FXML private ComboBox<String> area4PortComboBox;
    @FXML private ComboBox<String> area5PortComboBox;
    @FXML private ComboBox<String> area6PortComboBox;

    // 六个测试区域的连接按钮
    @FXML private Button area1ConnectButton;
    @FXML private Button area2ConnectButton;
    @FXML private Button area3ConnectButton;
    @FXML private Button area4ConnectButton;
    @FXML private Button area5ConnectButton;
    @FXML private Button area6ConnectButton;

    // 六个测试区域的断开按钮
    @FXML private Button area1DisconnectButton;
    @FXML private Button area2DisconnectButton;
    @FXML private Button area3DisconnectButton;
    @FXML private Button area4DisconnectButton;
    @FXML private Button area5DisconnectButton;
    @FXML private Button area6DisconnectButton;

    // 六个测试区域的状态标签
    @FXML private Label area1StatusLabel;
    @FXML private Label area2StatusLabel;
    @FXML private Label area3StatusLabel;
    @FXML private Label area4StatusLabel;
    @FXML private Label area5StatusLabel;
    @FXML private Label area6StatusLabel;

    // 测试参数输入框和按钮 (1-10)
    @FXML private TextField param1Field;
    @FXML private Button testParam1Button;
    @FXML private TextField param2Field;
    @FXML private Button testParam2Button;
    @FXML private TextField param3Field;
    @FXML private Button testParam3Button;
    @FXML private TextField param4Field;
    @FXML private Button testParam4Button;
    @FXML private TextField param5Field;
    @FXML private Button testParam5Button;
    @FXML private TextField param6Field;
    @FXML private Button testParam6Button;
    @FXML private TextField param7Field;
    @FXML private Button testParam7Button;
    @FXML private TextField param8Field;
    @FXML private Button testParam8Button;
    @FXML private TextField param9Field;
    @FXML private Button testParam9Button;
    @FXML private TextField param10Field;
    @FXML private Button testParam10Button;

    // 测试参数输入框和按钮 (11-20)
    @FXML private TextField param11Field;
    @FXML private Button testParam11Button;
    @FXML private TextField param12Field;
    @FXML private Button testParam12Button;
    @FXML private TextField param13Field;
    @FXML private Button testParam13Button;
    @FXML private TextField param14Field;
    @FXML private Button testParam14Button;
    @FXML private TextField param15Field;
    @FXML private Button testParam15Button;
    @FXML private TextField param16Field;
    @FXML private Button testParam16Button;
    @FXML private TextField param17Field;
    @FXML private Button testParam17Button;
    @FXML private TextField param18Field;
    @FXML private Button testParam18Button;
    @FXML private TextField param19Field;
    @FXML private Button testParam19Button;
    @FXML private TextField param20Field;
    @FXML private Button testParam20Button;

    // 参数操作按钮
    @FXML private Button saveParamsButton;
    @FXML private Button loadParamsButton;

    // 数据和状态
    private ObservableList<String> testResults = FXCollections.observableArrayList(); // 保留原有的
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private volatile boolean isTestRunning = false;  // 使用volatile确保线程安全
    private final AtomicInteger successCount = new AtomicInteger(0);  // 使用AtomicInteger确保线程安全
    private final AtomicInteger failureCount = new AtomicInteger(0);  // 使用AtomicInteger确保线程安全

    // 下位机响应状态
    private volatile boolean waitingForResponse = false;  // 使用volatile确保线程安全
    private ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timeoutTask;

    // 测试区域状态
    private volatile int currentTestArea = 0;  // 当前正在测试的区域 (0表示无测试)
    private volatile boolean waitingForCommand4Response = false;  // 等待命令码4的响应
    private String currentTestType = "";  // 当前测试类型

    // 多串口管理器
    private MultiSerialPortManager multiSerialPortManager;

    // 测试区域状态管理器
    private TestAreaStateManager testAreaStateManager;

    // 保留原有的主串口管理器（用于兼容性）
    private SerialPortManager serialPortManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeMultiSerialPortManager();
        initializeTestAreaStateManager();
        initializeMainSerialPortManager(); // 保留原有功能
        initializeComponents();
        updateStatus("系统就绪");

        // 添加应用关闭钩子，确保资源被正确清理
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (Exception e) {
                logger.error("关闭资源时出错", e);
            }
        }));
    }

    private void initializeMultiSerialPortManager() {
        multiSerialPortManager = new MultiSerialPortManager();

        // 设置全局日志回调
        multiSerialPortManager.setGlobalLogCallback(this::logMessage);

        // 设置区域数据接收回调
        multiSerialPortManager.setAreaDataReceivedCallback((areaNumber, data) -> {
            Platform.runLater(() -> {
                String receivedData = data.trim();

                // 检查是否是协议数据包（以A5开头的十六进制数据）
                if (isProtocolPacket(receivedData)) {
                    logMessage(String.format("区域%d收到A5协议数据包: %s", areaNumber, receivedData));
                    // 解析协议数据包，传入区域编号
                    parseReceivedDataForArea(areaNumber, receivedData);
                } else {
                    // 处理UTF-8文本数据
                    String utf8Data = parseUTF8Data(receivedData);
                    logMessage(String.format("区域%d收到UTF-8文本数据: %s", areaNumber, utf8Data));

                    // 将UTF-8字符串结果显示在对应的测试区域
                    displayTestResultInArea(areaNumber, utf8Data, receivedData);

                    // 更新统计
                    updateTestAreaStatistics(utf8Data);

                    // 测试完成，重置该区域状态
                    testAreaStateManager.stopAreaTest(areaNumber);
                }
            });
        });
    }

    private void initializeTestAreaStateManager() {
        testAreaStateManager = new TestAreaStateManager();

        // 设置超时回调
        testAreaStateManager.setTimeoutCallback((areaNumber, reason) -> {
            Platform.runLater(() -> {
                addTestAreaMessage(areaNumber, "⏰ 超时: " + reason);
                addTestAreaMessage(areaNumber, "💡 请检查设备连接或重新测试");
                testAreaStateManager.stopAreaTest(areaNumber);
            });
        });
    }

    /**
     * 初始化主串口管理器（保留原有功能）
     */
    private void initializeMainSerialPortManager() {
        serialPortManager = new SerialPortManager();

        // 设置日志回调
        serialPortManager.setLogCallback(this::logMessage);

        // 设置数据接收回调（用于原有的测试功能）
        serialPortManager.setDataReceivedCallback(data -> {
            Platform.runLater(() -> {
                String receivedData = data.trim();

                // 检查是否是协议数据包（以A5开头的十六进制数据）
                if (isProtocolPacket(receivedData)) {
                    logMessage("主串口收到A5协议数据包: " + receivedData);
                    // 解析协议数据包
                    parseReceivedData(receivedData);
                } else {
                    // 处理UTF-8文本数据
                    String utf8Data = parseUTF8Data(receivedData);
                    logMessage("主串口收到UTF-8文本数据: " + utf8Data);

                    // 直接显示UTF-8文本响应到测试结果区域
                    addTestResult("下位机UTF-8响应: " + utf8Data);

                    // 如果正在测试中，尝试统计成功/失败结果
                    if (isTestRunning) {
                        updateTestResultStatistics(utf8Data);
                    }
                }
            });
        });
    }

    /**
     * 解析UTF-8数据
     * 如果数据是十六进制格式的UTF-8编码，则解析为字符串
     * 否则直接返回原始数据
     */
    private String parseUTF8Data(String data) {
        try {
            // 检查是否是十六进制格式的UTF-8数据
            if (isHexEncodedUTF8(data)) {
                return decodeHexUTF8(data);
            } else {
                // 直接返回原始字符串数据
                return data;
            }
        } catch (Exception e) {
            logger.warn("UTF-8数据解析失败: {}", e.getMessage());
            return data; // 解析失败时返回原始数据
        }
    }

    /**
     * 检查数据是否是十六进制编码的UTF-8数据
     */
    private boolean isHexEncodedUTF8(String data) {
        // 移除空格和换行符
        String cleanData = data.replaceAll("\\s+", "");

        // 检查是否全部是十六进制字符
        if (!cleanData.matches("^[0-9A-Fa-f]+$")) {
            return false;
        }

        // 检查长度是否为偶数（每个字节需要2个十六进制字符）
        return cleanData.length() % 2 == 0 && cleanData.length() > 0;
    }

    /**
     * 解码十六进制UTF-8数据为字符串
     */
    private String decodeHexUTF8(String hexData) {
        try {
            // 移除空格
            String cleanHex = hexData.replaceAll("\\s+", "");

            // 转换为字节数组
            byte[] bytes = new byte[cleanHex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int index = i * 2;
                bytes[i] = (byte) Integer.parseInt(cleanHex.substring(index, index + 2), 16);
            }

            // 使用UTF-8解码
            String result = new String(bytes, StandardCharsets.UTF_8);

            // 移除控制字符（如\r\n）
            return result.replaceAll("[\\r\\n\\x00-\\x1F\\x7F]", "").trim();

        } catch (Exception e) {
            logger.warn("十六进制UTF-8解码失败: {}", e.getMessage());
            return hexData; // 解码失败时返回原始数据
        }
    }

    private void initializeComponents() {
        // 初始化测试模式复选框的互斥逻辑
        setupTestModeCheckBoxes();

        // 初始化四个测试区域的TextArea
        initializeTestAreas();

        // 初始化四个测试区域的串口选择控件
        initializeAreaSerialPortControls();

        // 初始状态：未连接串口时禁用所有测试相关控件
        setTestingComponentsEnabled(false);
    }

    /**
     * 初始化六个测试区域的TextArea
     */
    private void initializeTestAreas() {
        // 设置六个测试区域为只读
        testArea1.setEditable(false);
        testArea1.setWrapText(true);
        testArea2.setEditable(false);
        testArea2.setWrapText(true);
        testArea3.setEditable(false);
        testArea3.setWrapText(true);
        testArea4.setEditable(false);
        testArea4.setWrapText(true);
        testArea5.setEditable(false);
        testArea5.setWrapText(true);
        testArea6.setEditable(false);
        testArea6.setWrapText(true);

        // 添加初始提示信息
        addTestAreaMessage(1, "📋 测试区域1已就绪");
        addTestAreaMessage(1, "💡 点击【测试1】按钮开始测试");
        addTestAreaMessage(1, "📊 UTF-8测试结果将显示在此区域");
        addTestAreaMessage(1, "");

        addTestAreaMessage(2, "📋 测试区域2已就绪");
        addTestAreaMessage(2, "💡 点击【测试2】按钮开始测试");
        addTestAreaMessage(2, "📊 UTF-8测试结果将显示在此区域");
        addTestAreaMessage(2, "");

        addTestAreaMessage(3, "📋 测试区域3已就绪");
        addTestAreaMessage(3, "💡 点击【测试3】按钮开始测试");
        addTestAreaMessage(3, "📊 UTF-8测试结果将显示在此区域");
        addTestAreaMessage(3, "");

        addTestAreaMessage(4, "📋 测试区域4已就绪");
        addTestAreaMessage(4, "💡 点击【测试4】按钮开始测试");
        addTestAreaMessage(4, "📊 UTF-8测试结果将显示在此区域");
        addTestAreaMessage(4, "");

        addTestAreaMessage(5, "📋 测试区域5已就绪");
        addTestAreaMessage(5, "💡 点击【测试5】按钮开始测试");
        addTestAreaMessage(5, "📊 UTF-8测试结果将显示在此区域");
        addTestAreaMessage(5, "");

        addTestAreaMessage(6, "📋 测试区域6已就绪");
        addTestAreaMessage(6, "💡 点击【测试6】按钮开始测试");
        addTestAreaMessage(6, "📊 UTF-8测试结果将显示在此区域");
        addTestAreaMessage(6, "");
    }

    /**
     * 初始化六个测试区域的串口选择控件
     */
    private void initializeAreaSerialPortControls() {
        // 获取可用串口列表
        List<String> availablePorts = multiSerialPortManager.getAvailablePorts();
        ObservableList<String> portList = FXCollections.observableArrayList(availablePorts);

        // 为每个测试区域设置串口选择下拉框
        area1PortComboBox.setItems(portList);
        area2PortComboBox.setItems(portList);
        area3PortComboBox.setItems(portList);
        area4PortComboBox.setItems(portList);
        area5PortComboBox.setItems(portList);
        area6PortComboBox.setItems(portList);

        // 初始化连接状态标签
        area1StatusLabel.setText("未连接");
        area2StatusLabel.setText("未连接");
        area3StatusLabel.setText("未连接");
        area4StatusLabel.setText("未连接");
        area5StatusLabel.setText("未连接");
        area6StatusLabel.setText("未连接");

        // 初始化按钮状态
        area1DisconnectButton.setDisable(true);
        area2DisconnectButton.setDisable(true);
        area3DisconnectButton.setDisable(true);
        area4DisconnectButton.setDisable(true);
        area5DisconnectButton.setDisable(true);
        area6DisconnectButton.setDisable(true);

        // 初始化测试按钮状态（需要连接串口后才能测试）
        testButton1.setDisable(true);
        testButton2.setDisable(true);
        testButton3.setDisable(true);
        testButton4.setDisable(true);
        testButton5.setDisable(true);
        testButton6.setDisable(true);

        logMessage("🔍 自动扫描串口: 找到 " + availablePorts.size() + " 个串口: " + String.join(", ", availablePorts));
    }

    private void setupTestModeCheckBoxes() {
        // 初始状态：未勾选任何模式时，所有控件都不可用
        updateControlsState();

        // 设置自动模式复选框的监听器
        autoModeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // 如果选中自动模式，取消单项测试
                singleTestCheckBox.setSelected(false);
            }
            updateControlsState();
        });

        // 设置单项测试复选框的监听器
        singleTestCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // 如果选中单项测试，取消自动模式
                autoModeCheckBox.setSelected(false);
            }
            updateControlsState();
        });
    }

    /**
     * 根据当前选择的测试模式更新所有控件的状态
     */
    private void updateControlsState() {
        boolean autoMode = autoModeCheckBox.isSelected();
        boolean singleTest = singleTestCheckBox.isSelected();
        boolean hasConnection = multiSerialPortManager.hasAnyConnection();

        if (!autoMode && !singleTest) {
            // 未勾选任何模式：所有输入框和按钮都不可用
            setParameterFieldsEnabled(false);
            setParameterTestButtonsEnabled(false);
            setTestAreaButtonsEnabled(false);
        } else if (autoMode) {
            // 勾选自动模式：输入框可用，参数测试按钮不可用，测试区域按钮可用（需要连接）
            setParameterFieldsEnabled(true);
            setParameterTestButtonsEnabled(false);
            setTestAreaButtonsEnabled(hasConnection);
        } else if (singleTest) {
            // 勾选单项测试：所有控件都可用（需要连接）
            setParameterFieldsEnabled(hasConnection);
            setParameterTestButtonsEnabled(hasConnection);
            setTestAreaButtonsEnabled(hasConnection);
        }
    }
    

    


    /**
     * 刷新串口列表（内部方法）
     */
    private void refreshSerialPorts() {
        // 刷新所有测试区域的串口列表
        refreshAllAreaPorts();

        // 提供反馈信息
        List<String> availablePorts = multiSerialPortManager.getAvailablePorts();
        if (availablePorts.isEmpty()) {
            logMessage("🔍 自动扫描串口: 未找到可用串口，请检查设备连接");
            updateStatus("未找到串口设备");
        } else {
            logMessage("🔍 自动扫描串口: 找到 " + availablePorts.size() + " 个串口: " + String.join(", ", availablePorts));
            updateStatus("找到 " + availablePorts.size() + " 个串口");
        }
    }



    @FXML
    private void onDisconnect() {
        // 使用真实的串口断开
        // 在新布局中，断开连接功能由各个测试区域独立管理
        logMessage("请使用各测试区域的断开按钮");
    }

    @FXML
    private void onStartTest() {
        if (isTestRunning) return;

        // 检查是否选择了测试模式
        boolean singleTest = singleTestCheckBox.isSelected();
        boolean autoMode = autoModeCheckBox.isSelected();

        if (!singleTest && !autoMode) {
            showTestModeSelectionAlert();
            return;
        }

        isTestRunning = true;

        String testMode = "基础功能测试";

        logMessage(String.format("开始测试 - 模式: %s, 自动模式: %s, 单项测试: %s",
            testMode, autoMode ? "是" : "否", singleTest ? "是" : "否"));

        // 重置计数器
        successCount.set(0);
        failureCount.set(0);
        updateCounters();

        // 自动模式和单项测试模式都需要等待下位机响应
        waitingForResponse = true;
        if (autoMode) {
            logMessage("自动模式：等待下位机响应...");
        } else {
            logMessage("单项测试模式：等待下位机响应...");
        }

        // 启动超时检测任务（5秒超时）
        timeoutTask = timeoutExecutor.schedule(() -> {
            if (waitingForResponse) {
                Platform.runLater(() -> {
                    showNoResponseAlert();
                    onStopTest();
                });
            }
        }, 5, TimeUnit.SECONDS);

        // 启动测试任务，等待下位机发送请求测试参数的数据包
        Task<Void> testTask = createTestTask(testMode, autoMode);
        executorService.submit(testTask);
    }
    
    @FXML
    private void onStopTest() {
        isTestRunning = false;
        waitingForResponse = false;

        // 取消超时任务
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }

        logMessage("测试已停止");
        updateStatus("测试已停止");
    }
    
    @FXML
    private void onReset() {
        if (isTestRunning) {
            onStopTest();
        }
        
        // 重置所有计数器和状态
        successCount.set(0);
        failureCount.set(0);
        updateCounters();

        testResults.clear();

        logMessage("系统已重置");
        updateStatus("系统就绪");
    }
    
    @FXML
    private void onClearLogs() {
        // 清空所有测试区域
        testArea1.clear();
        testArea2.clear();
        testArea3.clear();
        testArea4.clear();
        testResults.clear();

        // 重新添加初始提示信息
        addTestAreaMessage(1, "🧹 测试区域1已清空");
        addTestAreaMessage(1, "💡 点击【测试1】按钮开始新测试");

        addTestAreaMessage(2, "🧹 测试区域2已清空");
        addTestAreaMessage(2, "💡 点击【测试2】按钮开始新测试");

        addTestAreaMessage(3, "🧹 测试区域3已清空");
        addTestAreaMessage(3, "💡 点击【测试3】按钮开始新测试");

        addTestAreaMessage(4, "🧹 测试区域4已清空");
        addTestAreaMessage(4, "💡 点击【测试4】按钮开始新测试");

        logMessage("所有测试区域已清空");
    }




    
    private Task<Void> createTestTask(String testMode, boolean autoMode) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 持续运行测试，直到用户停止
                int currentTest = 1;
                while (isTestRunning) {
                    final int testNumber = currentTest;

                    Platform.runLater(() -> {
                        updateStatus(String.format("执行测试 %d", testNumber));
                    });

                    // 模拟测试执行
                    boolean testResult = executeTest(testMode, testNumber);

                    Platform.runLater(() -> {
                        if (testResult) {
                            successCount.incrementAndGet();
                            addTestResult(String.format("测试 %d: 成功 - %s",
                                testNumber, getCurrentTime()));
                        } else {
                            failureCount.incrementAndGet();
                            addTestResult(String.format("测试 %d: 失败 - %s",
                                testNumber, getCurrentTime()));
                        }
                        updateCounters();
                    });
                    
                    // 测试间隔
                    Thread.sleep(autoMode ? 500 : 1000);

                    currentTest++;
                }
                
                Platform.runLater(() -> {
                    if (isTestRunning) {
                        updateStatus("测试完成");
                        logMessage(String.format("测试完成 - 成功: %d, 失败: %d",
                            successCount.get(), failureCount.get()));
                        onStopTest();
                    }
                });
                
                return null;
            }
        };
    }
    
    private boolean executeTest(String testMode, int testNumber) {
        // 执行真实的测试逻辑
        try {
            logger.debug("执行测试: 模式={}, 测试编号={}", testMode, testNumber);

            // 检查串口连接状态
            if (!serialPortManager.isConnected()) {
                logger.warn("测试失败: 串口未连接");
                return false;
            }

            // 根据测试模式执行不同的测试逻辑
            switch (testMode) {
                case "基础功能测试":
                    return executeBasicFunctionTest(testNumber);
                case "性能压力测试":
                    return executePerformanceTest(testNumber);
                case "稳定性测试":
                    return executeStabilityTest(testNumber);
                case "兼容性测试":
                    return executeCompatibilityTest(testNumber);
                default:
                    logger.warn("未知的测试模式: {}", testMode);
                    return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("测试被中断", e);
            return false;
        } catch (Exception e) {
            logger.error("测试执行异常", e);
            return false;
        }
    }

    /**
     * 执行基础功能测试
     */
    private boolean executeBasicFunctionTest(int testNumber) throws InterruptedException {
        logger.debug("执行基础功能测试 #{}", testNumber);
        Thread.sleep(50); // 模拟测试时间

        // 这里应该实现真实的基础功能测试逻辑
        // 例如：发送测试命令，等待响应，验证结果

        return true; // 临时返回成功，实际应根据测试结果返回
    }

    /**
     * 执行性能压力测试
     */
    private boolean executePerformanceTest(int testNumber) throws InterruptedException {
        logger.debug("执行性能压力测试 #{}", testNumber);
        Thread.sleep(100); // 模拟较长的测试时间

        // 这里应该实现真实的性能测试逻辑

        return true; // 临时返回成功
    }

    /**
     * 执行稳定性测试
     */
    private boolean executeStabilityTest(int testNumber) throws InterruptedException {
        logger.debug("执行稳定性测试 #{}", testNumber);
        Thread.sleep(200); // 模拟更长的测试时间

        // 这里应该实现真实的稳定性测试逻辑

        return true; // 临时返回成功
    }

    /**
     * 执行兼容性测试
     */
    private boolean executeCompatibilityTest(int testNumber) throws InterruptedException {
        logger.debug("执行兼容性测试 #{}", testNumber);
        Thread.sleep(75); // 模拟测试时间

        // 这里应该实现真实的兼容性测试逻辑

        return true; // 临时返回成功
    }

    private void logMessage(String message) {
        // 使用日志框架记录系统日志
        logger.info(message);
    }
    
    private void addTestResult(String result) {
        // 检查是否已经在JavaFX线程中
        if (Platform.isFxApplicationThread()) {
            addTestResultInternal(result);
        } else {
            Platform.runLater(() -> addTestResultInternal(result));
        }
    }

    private void addTestResultInternal(String result) {
        // 保留原有的统一列表（如果需要的话）
        testResults.add(0, result);
        if (testResults.size() > 100) {
            testResults.remove(testResults.size() - 1);
        }

        // 如果有当前测试区域，将结果显示在对应区域
        if (currentTestArea > 0) {
            addTestAreaMessage(currentTestArea, "测试结果: " + result);
        } else {
            // 否则记录到日志
            logger.info("测试结果: {}", result);
        }
    }
    
    private void updateStatus(String status) {
        // 检查是否已经在JavaFX线程中
        if (Platform.isFxApplicationThread()) {
            statusLabel.setText("状态: " + status);
        } else {
            Platform.runLater(() -> statusLabel.setText("状态: " + status));
        }
    }
    
    private void updateCounters() {
        // 检查是否已经在JavaFX线程中
        if (Platform.isFxApplicationThread()) {
            successCountLabel.setText("成功: " + successCount.get());
            failureCountLabel.setText("失败: " + failureCount.get());
        } else {
            Platform.runLater(() -> {
                successCountLabel.setText("成功: " + successCount.get());
                failureCountLabel.setText("失败: " + failureCount.get());
            });
        }
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    public void shutdown() {
        isTestRunning = false;

        // 关闭执行器服务
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // 等待正在执行的任务完成，最多等待5秒
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭超时执行器服务
        if (timeoutExecutor != null && !timeoutExecutor.isShutdown()) {
            timeoutExecutor.shutdown();
            try {
                // 等待正在执行的任务完成，最多等待2秒
                if (!timeoutExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    timeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭测试区域状态管理器
        if (testAreaStateManager != null) {
            testAreaStateManager.shutdown();
        }

        // 断开所有串口连接
        if (multiSerialPortManager != null) {
            multiSerialPortManager.disconnectAll();
        }

        // 断开主串口连接
        if (serialPortManager != null) {
            serialPortManager.disconnect();
        }
    }

    // 参数测试方法 (1-10)
    @FXML
    private void onTestParam1() { testParameter(1, param1Field.getText()); }
    @FXML
    private void onTestParam2() { testParameter(2, param2Field.getText()); }
    @FXML
    private void onTestParam3() { testParameter(3, param3Field.getText()); }
    @FXML
    private void onTestParam4() { testParameter(4, param4Field.getText()); }
    @FXML
    private void onTestParam5() { testParameter(5, param5Field.getText()); }
    @FXML
    private void onTestParam6() { testParameter(6, param6Field.getText()); }
    @FXML
    private void onTestParam7() { testParameter(7, param7Field.getText()); }
    @FXML
    private void onTestParam8() { testParameter(8, param8Field.getText()); }
    @FXML
    private void onTestParam9() { testParameter(9, param9Field.getText()); }
    @FXML
    private void onTestParam10() { testParameter(10, param10Field.getText()); }

    // 参数测试方法 (11-20)
    @FXML
    private void onTestParam11() { testParameter(11, param11Field.getText()); }
    @FXML
    private void onTestParam12() { testParameter(12, param12Field.getText()); }
    @FXML
    private void onTestParam13() { testParameter(13, param13Field.getText()); }
    @FXML
    private void onTestParam14() { testParameter(14, param14Field.getText()); }
    @FXML
    private void onTestParam15() { testParameter(15, param15Field.getText()); }
    @FXML
    private void onTestParam16() { testParameter(16, param16Field.getText()); }
    @FXML
    private void onTestParam17() { testParameter(17, param17Field.getText()); }
    @FXML
    private void onTestParam18() { testParameter(18, param18Field.getText()); }
    @FXML
    private void onTestParam19() { testParameter(19, param19Field.getText()); }
    @FXML
    private void onTestParam20() { testParameter(20, param20Field.getText()); }

    // 参数操作方法
    @FXML
    private void onSaveParams() {
        saveCurrentParameters();
    }

    @FXML
    private void onLoadParams() {
        loadSavedParameters();
    }

    /**
     * 测试单个参数
     * @param paramNumber 参数编号
     * @param paramValue 参数值
     */
    private void testParameter(int paramNumber, String paramValue) {
        // 检查是否选择了测试模式
        boolean singleTest = singleTestCheckBox.isSelected();
        boolean autoMode = autoModeCheckBox.isSelected();

        if (!singleTest && !autoMode) {
            showTestModeSelectionAlert();
            return;
        }

        if (paramValue == null || paramValue.trim().isEmpty()) {
            logMessage("参数" + paramNumber + ": 请输入参数值");
            return;
        }

        if (!serialPortManager.isConnected()) {
            logMessage("参数" + paramNumber + ": 请先连接串口");
            return;
        }

        // 在新布局中不需要禁用测试控制按钮

        // 在后台线程中执行参数测试
        executorService.submit(() -> {
            try {
                logMessage("开始测试参数" + paramNumber + ": " + paramValue);

                // 构造测试命令 (这里使用示例格式，实际格式需要根据设备协议调整)
                String command = String.format("TEST_PARAM_%02d:%s", paramNumber, paramValue);

                // 发送命令到串口
                boolean sendSuccess = serialPortManager.sendData(command);

                if (!sendSuccess) {
                    logMessage("参数" + paramNumber + " 发送失败");
                    addTestResult("参数" + paramNumber + " 发送失败: " + paramValue);
                    return;
                }

                // 等待设备响应 (实际应用中应该读取设备响应)
                Thread.sleep(100); // 等待响应时间

                // TODO: 实现真实的设备响应读取和验证逻辑
                // 这里应该读取串口返回的数据并验证测试结果
                boolean success = validateParameterTestResult(paramNumber, paramValue);

                if (success) {
                    logMessage("参数" + paramNumber + " 测试成功: " + paramValue);
                    addTestResult("参数" + paramNumber + " 测试成功: " + paramValue);
                } else {
                    logMessage("参数" + paramNumber + " 测试失败: " + paramValue);
                    addTestResult("参数" + paramNumber + " 测试失败: " + paramValue);
                }

            } catch (Exception e) {
                logMessage("参数" + paramNumber + " 测试异常: " + e.getMessage());
            } finally {
                // 在新布局中不需要重新启用测试控制按钮
            }
        });
    }

    /**
     * 验证参数测试结果
     * TODO: 实现真实的参数测试结果验证逻辑
     */
    private boolean validateParameterTestResult(int paramNumber, String paramValue) {
        // 这里应该实现真实的验证逻辑
        // 例如：读取串口返回的数据，解析响应，验证参数是否设置成功

        logger.debug("验证参数{}测试结果: {}", paramNumber, paramValue);

        // 临时返回true，实际应根据设备响应进行验证
        return true;
    }

    /**
     * 设置所有测试相关组件的启用状态
     * @param enabled true为启用，false为禁用
     */
    private void setTestingComponentsEnabled(boolean enabled) {
        // 在新布局中，测试相关组件主要是参数输入框

        // 禁用/启用所有参数输入框
        param1Field.setDisable(!enabled);
        param2Field.setDisable(!enabled);
        param3Field.setDisable(!enabled);
        param4Field.setDisable(!enabled);
        param5Field.setDisable(!enabled);
        param6Field.setDisable(!enabled);
        param7Field.setDisable(!enabled);
        param8Field.setDisable(!enabled);
        param9Field.setDisable(!enabled);
        param10Field.setDisable(!enabled);
        param11Field.setDisable(!enabled);
        param12Field.setDisable(!enabled);
        param13Field.setDisable(!enabled);
        param14Field.setDisable(!enabled);
        param15Field.setDisable(!enabled);
        param16Field.setDisable(!enabled);
        param17Field.setDisable(!enabled);
        param18Field.setDisable(!enabled);
        param19Field.setDisable(!enabled);
        param20Field.setDisable(!enabled);

        // 禁用/启用所有参数测试按钮
        setParameterTestButtonsEnabled(enabled);
    }

    /**
     * 判断是否是协议数据包
     * @param data 接收到的数据字符串
     * @return true如果是协议数据包，false如果是文本响应
     */
    private boolean isProtocolPacket(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // 移除空格并转换为大写
        String cleanData = data.replaceAll("\\s+", "").toUpperCase();

        // 检查是否是以A5开头、0D结尾的十六进制数据包
        // 并且长度符合协议要求（至少12个字符，即6字节）
        return cleanData.length() >= 12 &&
               cleanData.startsWith("A5") &&
               cleanData.endsWith("0D") &&
               cleanData.matches("[0-9A-F]+"); // 只包含十六进制字符
    }

    /**
     * 解析下位机发送的数据包
     * @param data 接收到的数据字符串
     */
    private void parseReceivedData(String data) {
        try {
            // 移除空格并转换为大写
            String cleanData = data.replaceAll("\\s+", "").toUpperCase();

            // 检查是否是6字节的数据包（请求或响应）
            // 格式：A5 06 [CMD] [2B CRC] 0D
            if (cleanData.length() >= 12 && cleanData.startsWith("A5") && cleanData.endsWith("0D")) {
                // 解析数据包
                String frameHeader = cleanData.substring(0, 2);   // A5
                String length = cleanData.substring(2, 4);        // 06
                String command = cleanData.substring(4, 6);       // 命令
                String crcLow = cleanData.substring(6, 8);        // CRC低字节 (小端格式)
                String crcHigh = cleanData.substring(8, 10);      // CRC高字节
                String frameEnd = cleanData.substring(10, 12);    // 0D

                // 验证基本格式
                if ("A5".equals(frameHeader) && "06".equals(length) && "0D".equals(frameEnd)) {
                    // 验证CRC
                    byte[] packet = new byte[3];
                    packet[0] = (byte) 0xA5;
                    packet[1] = (byte) 0x06;
                    packet[2] = (byte) Integer.parseInt(command, 16);
                    int calculatedCRC = calculateCRC16(packet, 0, 3);
                    int receivedCRC = (Integer.parseInt(crcHigh, 16) << 8) | Integer.parseInt(crcLow, 16);

                    logMessage("CRC校验详情 - 数据: " + frameHeader + " " + length + " " + command);
                    logMessage("接收CRC(小端): 低字节=" + crcLow + ", 高字节=" + crcHigh + ", 组合=" + String.format("%04X", receivedCRC));
                    logMessage("计算CRC: " + String.format("%04X", calculatedCRC));

                    if (calculatedCRC == receivedCRC) {
                        // 根据命令类型处理
                        if ("02".equals(command)) {
                            logMessage("收到下位机请求测试参数命令，CRC校验通过");
                            handleRequestTestParameters();
                        } else if ("04".equals(command)) {
                            logMessage("收到下位机参数设置响应，CRC校验通过");
                            handleParameterSetResponse();
                        } else {
                            logMessage("收到未知命令: " + command);

                        }
                    } else {
                        logMessage("收到数据包，但CRC校验失败");
                        logMessage(String.format("命令: %s, 计算CRC: %04X, 接收CRC: %04X", command, calculatedCRC, receivedCRC));
                        logMessage("停止测试");
                        // CRC校验失败停止测试
                        if (isTestRunning) {
                            onStopTest();
                        }
                    }
                } else {
                    logMessage("收到数据包格式不匹配");
                }
            }
        } catch (Exception e) {
            logMessage("解析接收数据失败: " + e.getMessage());
        }
    }

    /**
     * 处理下位机请求测试参数的命令
     */
    private void handleRequestTestParameters() {
        // 收到下位机响应，取消超时任务
        waitingForResponse = false;
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }

        // 检查是否是测试区域测试
        if (currentTestArea > 0) {
            addTestAreaMessage(currentTestArea, "📨 下位机请求测试参数");
            addTestAreaMessage(currentTestArea, "📤 开始发送测试参数数据...");
            sendTestParametersData();
            return;
        }

        // 检查是否为自动模式或单项测试模式
        boolean autoMode = autoModeCheckBox.isSelected();
        boolean singleTest = singleTestCheckBox.isSelected();

        if (!autoMode && !singleTest) {
            logMessage("收到下位机请求测试参数命令，但当前未选择测试模式，忽略处理");
            return;
        }

        if (autoMode) {
            logMessage("自动模式：下位机请求测试参数，开始发送测试参数数据...");
        } else {
            logMessage("单项测试模式：下位机请求测试参数，开始发送测试参数数据...");
        }

        // 发送测试参数数据包
        sendTestParametersData();
    }

    /**
     * 处理下位机参数设置响应
     * 响应格式：A5 06 04 [2B CRC] 0D
     */
     private void handleParameterSetResponse() {
        logMessage("下位机确认参数设置成功");

        // 取消等待命令码4响应的状态
        waitingForCommand4Response = false;

        // 检查是否为测试区域测试
        if (currentTestArea > 0) {
            addTestAreaMessage(currentTestArea, "✅ 下位机确认参数设置成功");
            addTestAreaMessage(currentTestArea, "📤 发送校准电压数据给下位机...");
            sendFixedFormatDataToArea(currentTestArea);
            return;
        }

        // 检查是否为自动模式或单项测试模式
        boolean autoMode = autoModeCheckBox.isSelected();

        boolean singleTest = singleTestCheckBox.isSelected();

        if (!autoMode && !singleTest) {
            logMessage("收到下位机参数设置响应，但当前未选择测试模式，忽略后续处理");
            return;
        }

        if (autoMode) {
            logMessage("自动模式：发送固定格式数据给下位机");

        } else {
            logMessage("单项测试模式：发送固定格式数据给下位机");
        }

        // 发送固定格式的数据给下位机
        sendFixedFormatData();
    }

    /**
     * 发送固定格式数据给下位机
     * 协议格式：
     * 帧头(1B): A5
     * 长度(1B): 08
     * 帧含义(1B): 05
     * 电压校准值(2B): 40 1F (0.01V单位)
     * CRC校验(2B): MODBUS-CRC
     * 帧尾(1B): 0D
     */
    private void sendFixedFormatData() {
        if (!serialPortManager.isConnected()) {
            logMessage("串口未连接，无法发送固定格式数据");
            return;
        }

        try {
            // 构造8字节的数据包
            byte[] packet = new byte[8];
            int index = 0;

            // 1. 帧头 (1B): A5
            packet[index++] = (byte) 0xA5;

            // 2. 长度 (1B): 08
            packet[index++] = (byte) 0x08;

            // 3. 帧含义 (1B): 05
            packet[index++] = (byte) 0x05;

            // 4-5. 电压校准值 (2B): 40 1F
            // 固定值：0x1F40 = 8000，表示80.00V (单位：0.01V)
            packet[index++] = (byte) 0x40;  // 低字节
            packet[index++] = (byte) 0x1F;  // 高字节





            // 6-7. CRC校验 (2B) - MODBUS-CRC
            // 计算范围：从长度字段到电压校准值结束 (索引1-4)
            int crc = calculateCRC16(packet, 1, 4);
            packet[index++] = (byte) (crc & 0xFF);        // CRC低字节
            packet[index++] = (byte) ((crc >> 8) & 0xFF); // CRC高字节
            logMessage("命令码5 CRC写入(小端): 低字节=" + String.format("%02X", crc & 0xFF) +
                      ", 高字节=" + String.format("%02X", (crc >> 8) & 0xFF));

            // 8. 帧尾 (1B): 0D
            packet[index] = (byte) 0x0D;

            // 转换为十六进制字符串发送
            StringBuilder hexString = new StringBuilder();
            for (byte b : packet) {
                hexString.append(String.format("%02X ", b & 0xFF));
            }

            String command = hexString.toString().trim();
            serialPortManager.sendData(command);
            logMessage("发送固定格式数据包: " + command);
            logMessage("协议详情: 帧头=A5, 长度=08, 帧含义=05, 电压校准值=40 1F (80.00V), CRC=MODBUS, 帧尾=0D");

            // 发送完成后开始实际测试
            logMessage("固定格式数据已发送，开始执行测试...");
            startActualTesting();

        } catch (Exception e) {
            logMessage("发送固定格式数据失败: " + e.getMessage());
        }
    }

    /**
     * 开始实际的测试流程
     */
    private void startActualTesting() {
        logMessage("固定格式数据已发送，等待下位机测试结果...");
        logMessage("下位机正在执行测试，请等待测试结果返回");

        // 更新状态，表示正在等待下位机测试
        Platform.runLater(() -> {
            updateStatus("等待下位机测试结果");
        });

        // 不需要循环测试，测试结果会通过串口数据接收回调自动显示在界面上
        // 下位机会返回字符串类型的测试结果，这些结果会被 isProtocolPacket() 判断为非协议数据
        // 然后通过 addTestResult() 方法自动显示在测试结果区域
    }

    /**
     * 更新测试结果统计
     * 根据下位机返回的字符串内容判断测试成功或失败
     * @param receivedData 下位机返回的测试结果字符串
     */
    private void updateTestResultStatistics(String receivedData) {
        if (receivedData == null || receivedData.trim().isEmpty()) {
            return;
        }

        String data = receivedData.toLowerCase().trim();


        // 判断成功的关键词
        boolean isSuccess = data.contains("成功") || data.contains("通过") || data.contains("ok") ||
                           data.contains("pass") || data.contains("success") || data.contains("正常") ||
                           data.contains("合格");

        // 判断失败的关键词
        boolean isFailure = data.contains("失败") || data.contains("错误") || data.contains("异常") ||
                           data.contains("fail") || data.contains("error") || data.contains("ng") ||
                           data.contains("不合格");

        // 更新计数器
        if (isSuccess && !isFailure) {
            successCount.incrementAndGet();
            logMessage("检测到测试成功结果，成功计数+1");
        } else if (isFailure && !isSuccess) {
            failureCount.incrementAndGet();
            logMessage("检测到测试失败结果，失败计数+1");
        }
        // 如果既包含成功又包含失败关键词，或者都不包含，则不更新计数器

        // 更新界面显示
        updateCounters();
    }

    /**
     * 发送测试参数数据给下位机
     * 按照协议格式构造52字节的数据包
     * 自动模式：发送所有参数值
     * 单项测试模式：只发送有值的参数，其他位置填0
     */
    private void sendTestParametersData() {
        if (!serialPortManager.isConnected()) {
            logMessage("串口未连接，无法发送测试参数数据");
            return;
        }

        boolean isAutoMode = autoModeCheckBox.isSelected();

        try {
            // 构造52字节的数据包
            byte[] packet = new byte[52];
            int index = 0;

            // 1. 帧头 (1B)
            packet[index++] = (byte) 0xA5;

            // 2. 长度 (1B) - 数据域长度=42字节
            packet[index++] = (byte) 0x2A;

            // 3. 命令 (1B) - 下发参数
            packet[index++] = (byte) 0x03;

            // 4-7. 测试项 (4B) - 预留字段，全FF
            packet[index++] = (byte) 0xFF;
            packet[index++] = (byte) 0xFF;
            packet[index++] = (byte) 0xFF;
            packet[index++] = (byte) 0xFF;

            // 8-9. 电压校准值 (2B) - 从param1Field获取，单位：0.01V
            if (isAutoMode || hasParameterValue(param1Field)) {
                double voltageCalibrationV = getDoubleValueFromField(param1Field, 0.0);
                int voltageCalibration = (int) Math.round(voltageCalibrationV * 100); // V转换为0.01V
                writeInt16LittleEndian(packet, index, voltageCalibration);
            } else {
                writeInt16LittleEndian(packet, index, 0); // 单项测试模式下未测试的参数填0
            }
            index += 2;

            // 10-11. 电压校准范围 (2B) - 从param2Field获取，单位：0.01V
            if (isAutoMode || hasParameterValue(param2Field)) {
                double voltageRangeV = getDoubleValueFromField(param2Field, 0.0);

                int voltageRange = (int) Math.round(voltageRangeV * 100); // V转换为0.01V
                writeInt16LittleEndian(packet, index, voltageRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 12-16. 出厂日期 (5B) - 从param3Field获取，格式：年月日时分
            if (isAutoMode || hasParameterValue(param3Field)) {
                writeFactoryDate(packet, index, param3Field.getText());
            } else {
                // 填充5个字节的0
                for (int i = 0; i < 5; i++) {
                    packet[index + i] = 0;
                }
            }
            index += 5;

            // 17. 电池串数 (1B) - 从param4Field获取
            if (isAutoMode || hasParameterValue(param4Field)) {
                packet[index++] = (byte) getIntValueFromField(param4Field, 0);
            } else {
                packet[index++] = 0;
            }

            // 18-19. 产品功耗 (2B) - 从param5Field获取
            if (isAutoMode || hasParameterValue(param5Field)) {
                int productPower = getIntValueFromField(param5Field, 0);
                writeInt16LittleEndian(packet, index, productPower);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 20-21. 产品功耗范围 (2B) - 从param6Field获取
            if (isAutoMode || hasParameterValue(param6Field)) {
                int powerRange = getIntValueFromField(param6Field, 0);
                writeInt16LittleEndian(packet, index, powerRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);

            }
            index += 2;

            // 22-23. 充电老化时间 (2B) - 从param7Field获取
            if (isAutoMode || hasParameterValue(param7Field)) {
                int chargeAgingTime = getIntValueFromField(param7Field, 0);
                writeInt16LittleEndian(packet, index, chargeAgingTime);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 24-25. 最大充电电流 (2B) - 从param8Field获取，单位：0.1A
            if (isAutoMode || hasParameterValue(param8Field)) {
                double maxChargeCurrentA = getDoubleValueFromField(param8Field, 0.0);
                int maxChargeCurrent = (int) Math.round(maxChargeCurrentA * 10); // A转换为0.1A
                writeInt16LittleEndian(packet, index, maxChargeCurrent);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 26-27. 充电电流范围 (2B) - 从param9Field获取，单位：0.1A
            if (isAutoMode || hasParameterValue(param9Field)) {
                double chargeCurrentRangeA = getDoubleValueFromField(param9Field, 0.0);
                int chargeCurrentRange = (int) Math.round(chargeCurrentRangeA * 10); // A转换为0.1A
                writeInt16LittleEndian(packet, index, chargeCurrentRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 28-29. 最大充电温度 (2B) - 从param10Field获取，单位：0.1°C
            if (isAutoMode || hasParameterValue(param10Field)) {
                double maxChargeTempC = getDoubleValueFromField(param10Field, 0.0);
                int maxChargeTemp = (int) Math.round(maxChargeTempC * 10); // °C转换为0.1°C
                writeInt16LittleEndian(packet, index, maxChargeTemp);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 30-31. 充电温度范围 (2B) - 从param11Field获取，单位：0.1°C
            if (isAutoMode || hasParameterValue(param11Field)) {
                double chargeTempRangeC = getDoubleValueFromField(param11Field, 0.0);
                int chargeTempRange = (int) Math.round(chargeTempRangeC * 10); // °C转换为0.1°C
                writeInt16LittleEndian(packet, index, chargeTempRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 32-33. 最大充电均衡电流 (2B) - 从param12Field获取，单位：0.01A
            if (isAutoMode || hasParameterValue(param12Field)) {
                double maxChargeBalanceCurrentA = getDoubleValueFromField(param12Field, 0.0);
                int maxChargeBalanceCurrent = (int) Math.round(maxChargeBalanceCurrentA * 100); // A转换为0.01A
                writeInt16LittleEndian(packet, index, maxChargeBalanceCurrent);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 34-35. 充电均衡范围 (2B) - 从param13Field获取，单位：0.01A
            if (isAutoMode || hasParameterValue(param13Field)) {
                double chargeBalanceRangeA = getDoubleValueFromField(param13Field, 0.0);
                int chargeBalanceRange = (int) Math.round(chargeBalanceRangeA * 100); // A转换为0.01A
                writeInt16LittleEndian(packet, index, chargeBalanceRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 36-37. 放电老化时间 (2B) - 从param14Field获取
            if (isAutoMode || hasParameterValue(param14Field)) {
                int dischargeAgingTime = getIntValueFromField(param14Field, 0);
                writeInt16LittleEndian(packet, index, dischargeAgingTime);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 38-39. 最大放电电流 (2B) - 从param15Field获取，单位：0.1A
            if (isAutoMode || hasParameterValue(param15Field)) {
                double maxDischargeCurrentA = getDoubleValueFromField(param15Field, 0.0);
                int maxDischargeCurrent = (int) Math.round(maxDischargeCurrentA * 10); // A转换为0.1A
                writeInt16LittleEndian(packet, index, maxDischargeCurrent);

            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 40-41. 放电电流范围 (2B) - 从param16Field获取，单位：0.1A

         
            if (isAutoMode || hasParameterValue(param16Field)) {
                double dischargeCurrentRangeA = getDoubleValueFromField(param16Field, 0.0);
                int dischargeCurrentRange = (int) Math.round(dischargeCurrentRangeA * 10); // A转换为0.1A
                writeInt16LittleEndian(packet, index, dischargeCurrentRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 42-43. 最大放电温度 (2B) - 从param17Field获取，单位：0.1°C
            if (isAutoMode || hasParameterValue(param17Field)) {
                double maxDischargeTempC = getDoubleValueFromField(param17Field, 0.0);
                int maxDischargeTemp = (int) Math.round(maxDischargeTempC * 10); // °C转换为0.1°C
                writeInt16LittleEndian(packet, index, maxDischargeTemp);
            } else {
                writeInt16LittleEndian(packet, index, 0);

            }
            index += 2;

            // 44-45. 放电温度范围 (2B) - 从param18Field获取，单位：0.1°C
            if (isAutoMode || hasParameterValue(param18Field)) {
                double dischargeTempRangeC = getDoubleValueFromField(param18Field, 0.0);
                int dischargeTempRange = (int) Math.round(dischargeTempRangeC * 10); // °C转换为0.1°C
                writeInt16LittleEndian(packet, index, dischargeTempRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 46-47. 最大放电均衡电流 (2B) - 从param19Field获取，单位：0.01A
            if (isAutoMode || hasParameterValue(param19Field)) {
                double maxDischargeBalanceCurrentA = getDoubleValueFromField(param19Field, 0.0);
                int maxDischargeBalanceCurrent = (int) Math.round(maxDischargeBalanceCurrentA * 100); // A转换为0.01A
                writeInt16LittleEndian(packet, index, maxDischargeBalanceCurrent);
            } else {
                writeInt16LittleEndian(packet, index, 0);

            }
            index += 2;

            // 48-49. 放电均衡范围 (2B) - 从param20Field获取，单位：0.01A
            if (isAutoMode || hasParameterValue(param20Field)) {
                double dischargeBalanceRangeA = getDoubleValueFromField(param20Field, 0.0);
                int dischargeBalanceRange = (int) Math.round(dischargeBalanceRangeA * 100); // A转换为0.01A
                writeInt16LittleEndian(packet, index, dischargeBalanceRange);
            } else {
                writeInt16LittleEndian(packet, index, 0);
            }
            index += 2;

            // 49-50. CRC校验 (2B) - 计算范围：从A5帧头到数据结束，不包含CRC和帧尾
            int crc = calculateCRC16(packet, 0, 49); // 从索引0(A5)开始到索引48(数据结束)，共49字节

            // 调试信息：显示CRC计算的数据范围
            StringBuilder crcDataHex = new StringBuilder();
            for (int i = 0; i < 49; i++) {
                crcDataHex.append(String.format("%02X ", packet[i] & 0xFF));
            }
            logMessage("CRC计算数据范围(索引0-48，从A5到数据结束): " + crcDataHex.toString().trim());
            logMessage("计算得到的CRC16: " + String.format("%04X", crc));

            // MODBUS CRC16 小端格式 (低字节在前，高字节在后)
            packet[49] = (byte) (crc & 0xFF);        // CRC低字节 (索引49)
            packet[50] = (byte) ((crc >> 8) & 0xFF); // CRC高字节 (索引50)
            logMessage("CRC写入(小端): 低字节=" + String.format("%02X", crc & 0xFF) +
                      ", 高字节=" + String.format("%02X", (crc >> 8) & 0xFF));

            // 51. 帧尾 (1B)
            packet[51] = (byte) 0x0D;
            

            // 转换为十六进制字符串发送
            StringBuilder hexString = new StringBuilder();
            for (byte b : packet) {
                hexString.append(String.format("%02X ", b & 0xFF));
            }

            String command = hexString.toString().trim();
            serialPortManager.sendData(command);

            if (isAutoMode) {
                logMessage("发送测试参数数据包(自动模式-所有参数): " + command);
            } else {
                logMessage("发送测试参数数据包(单项测试模式-仅有值参数): " + command);
            }

            // 设置等待命令码4响应的状态
            waitingForCommand4Response = true;
            logMessage("等待下位机发送命令码04确认响应...");

        } catch (Exception e) {
            logMessage("发送测试参数数据失败: " + e.getMessage());
        }
    }

    /**
     * 计算CRC16校验码 (MODBUS-CRC算法)
     * @param data 数据数组
     * @param offset 起始偏移
     * @param length 数据长度
     * @return CRC16校验码
     */
    private int calculateCRC16(byte[] data, int offset, int length) {
        // 添加边界检查
        if (data == null) {
            throw new IllegalArgumentException("数据数组不能为null");
        }
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("偏移量和长度不能为负数");
        }
        if (offset + length > data.length) {
            throw new IllegalArgumentException("数据范围超出数组边界: offset=" + offset +
                ", length=" + length + ", array.length=" + data.length);
        }

        int crc = 0xFFFF;

        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc = crc >> 1;
                }
            }
        }

        return crc;
    }













    /**
     * 获取指定编号的参数字段
     */
    private TextField getParameterField(int paramNumber) {
        switch (paramNumber) {
            case 1: return param1Field;
            case 2: return param2Field;
            case 3: return param3Field;
            case 4: return param4Field;
            case 5: return param5Field;
            case 6: return param6Field;
            case 7: return param7Field;
            case 8: return param8Field;
            case 9: return param9Field;
            case 10: return param10Field;
            case 11: return param11Field;
            case 12: return param12Field;
            case 13: return param13Field;
            case 14: return param14Field;
            case 15: return param15Field;
            case 16: return param16Field;
            case 17: return param17Field;
            case 18: return param18Field;
            case 19: return param19Field;
            case 20: return param20Field;
            default: return null;
        }
    }







    /**
     * 检查参数字段是否有值
     * @param field 参数输入字段
     * @return true如果字段有非空值，false如果字段为空
     */
    private boolean hasParameterValue(TextField field) {
        // 添加空指针检查
        if (field == null) {
            return false;
        }

        try {
            String text = field.getText();
            return text != null && !text.trim().isEmpty();
        } catch (Exception e) {
            logMessage("检查参数值时发生异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 安全地将double值转换为int，防止溢出
     * @param value 要转换的double值
     * @param multiplier 乘数
     * @param min 最小值
     * @param max 最大值
     * @param paramName 参数名称（用于日志）
     * @return 转换后的int值
     */
    private int safeDoubleToInt(double value, double multiplier, int min, int max, String paramName) {
        try {
            // 先乘以倍数
            double scaledValue = value * multiplier;

            // 检查是否为有效数值
            if (Double.isNaN(scaledValue) || Double.isInfinite(scaledValue)) {
                logMessage(paramName + "数值无效(NaN或Infinite)，使用最小值: " + min);
                return min;
            }

            // 四舍五入
            long roundedValue = Math.round(scaledValue);

            // 检查是否在int范围内
            if (roundedValue < Integer.MIN_VALUE || roundedValue > Integer.MAX_VALUE) {
                logMessage(paramName + "转换后超出int范围，限制在[" + min + ", " + max + "]");
                return roundedValue < 0 ? min : max;
            }

            int intValue = (int) roundedValue;

            // 限制在指定范围内
            if (intValue < min) {
                logMessage(paramName + "值过小(" + intValue + ")，限制为最小值: " + min);
                return min;
            } else if (intValue > max) {
                logMessage(paramName + "值过大(" + intValue + ")，限制为最大值: " + max);
                return max;
            }

            return intValue;
        } catch (Exception e) {
            logMessage(paramName + "转换时发生异常: " + e.getMessage() + "，使用最小值: " + min);
            return min;
        }
    }

    /**
     * 显示测试模式选择提示
     */
    private void showTestModeSelectionAlert() {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("测试模式选择");
        alert.setHeaderText("请选择测试模式");
        alert.setContentText("请先勾选\"自动模式\"或\"单项测试\"中的一种模式，然后再开始测试。\n\n" +
                           "• 自动模式：执行完整的自动化测试流程\n" +
                           "• 单项测试：手动控制各个测试项目");
        alert.showAndWait();

        logMessage("请先选择测试模式（自动模式或单项测试）");
        
    }

    /**
     * 显示下位机无响应的弹框提示
     */
    private void showNoResponseAlert() {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("测试错误");
        alert.setHeaderText("下位机无响应");
        alert.setContentText("在指定时间内未收到下位机响应，测试已停止。\n请检查下位机连接状态。");
        alert.showAndWait();
        logMessage("下位机无响应，测试已停止");
    }

    /**
     * 设置所有参数测试按钮的启用状态
     * @param enabled true为启用，false为禁用
     */
    private void setParameterTestButtonsEnabled(boolean enabled) {
        testParam1Button.setDisable(!enabled);
        testParam2Button.setDisable(!enabled);
        testParam3Button.setDisable(!enabled);
        testParam4Button.setDisable(!enabled);
        testParam5Button.setDisable(!enabled);
        testParam6Button.setDisable(!enabled);
        testParam7Button.setDisable(!enabled);
        testParam8Button.setDisable(!enabled);
        testParam9Button.setDisable(!enabled);
        testParam10Button.setDisable(!enabled);
        testParam11Button.setDisable(!enabled);
        testParam12Button.setDisable(!enabled);
        testParam13Button.setDisable(!enabled);
        testParam14Button.setDisable(!enabled);
        testParam15Button.setDisable(!enabled);
        testParam16Button.setDisable(!enabled);
        testParam17Button.setDisable(!enabled);
        testParam18Button.setDisable(!enabled);
        testParam19Button.setDisable(!enabled);
        testParam20Button.setDisable(!enabled);
    }

    /**
     * 设置所有参数输入框的启用状态
     * @param enabled true为启用，false为禁用
     */
    private void setParameterFieldsEnabled(boolean enabled) {
        param1Field.setDisable(!enabled);
        param2Field.setDisable(!enabled);
        param3Field.setDisable(!enabled);
        param4Field.setDisable(!enabled);
        param5Field.setDisable(!enabled);
        param6Field.setDisable(!enabled);
        param7Field.setDisable(!enabled);
        param8Field.setDisable(!enabled);
        param9Field.setDisable(!enabled);
        param10Field.setDisable(!enabled);
        param11Field.setDisable(!enabled);
        param12Field.setDisable(!enabled);
        param13Field.setDisable(!enabled);
        param14Field.setDisable(!enabled);
        param15Field.setDisable(!enabled);
        param16Field.setDisable(!enabled);
        param17Field.setDisable(!enabled);
        param18Field.setDisable(!enabled);
        param19Field.setDisable(!enabled);
        param20Field.setDisable(!enabled);
    }

    /**
     * 设置所有测试区域按钮的启用状态
     * @param enabled true为启用，false为禁用
     */
    private void setTestAreaButtonsEnabled(boolean enabled) {
        testButton1.setDisable(!enabled);
        testButton2.setDisable(!enabled);
        testButton3.setDisable(!enabled);
        testButton4.setDisable(!enabled);
        testButton5.setDisable(!enabled);
        testButton6.setDisable(!enabled);
    }

    /**
     * 从TextField获取整数值
     * @param field 输入框
     * @param defaultValue 默认值
     * @return 整数值
     */
    private int getIntValueFromField(TextField field, int defaultValue) {
        // 添加空指针检查
        if (field == null) {
            logMessage("输入框为null，使用默认值: " + defaultValue);
            return defaultValue;
        }

        try {
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                return defaultValue;
            }

            int value = Integer.parseInt(text.trim());

            // 添加合理性检查 - 避免极端值
            if (value < -32768 || value > 32767) {
                logMessage("整数值超出合理范围(-32768到32767)，使用默认值: " + value);
                return defaultValue;
            }

            return value;
        } catch (NumberFormatException e) {
            logMessage("参数格式错误: " + field.getText() + "，使用默认值: " + defaultValue);
            return defaultValue;

        } catch (Exception e) {
            logMessage("获取整数值时发生异常: " + e.getMessage() + "，使用默认值: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * 从TextField获取浮点数值
     * @param field 输入框
     * @param defaultValue 默认值
     * @return 浮点数值
     */
    private double getDoubleValueFromField(TextField field, double defaultValue) {
        // 添加空指针检查
        if (field == null) {
            logMessage("输入框为null，使用默认值: " + defaultValue);
            return defaultValue;
        }

        try {
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                return defaultValue;
            }

            double value = Double.parseDouble(text.trim());

            // 添加合理性检查 - 避免极端值和无效值
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                logMessage("浮点数值无效(NaN或Infinite)，使用默认值: " + value);
                return defaultValue;
            }

            // 检查是否在合理范围内 (根据实际应用调整范围)
            if (value < -1000000 || value > 1000000) {
                logMessage("浮点数值超出合理范围(-1000000到1000000)，使用默认值: " + value);
                return defaultValue;
            }

            return value;
        } catch (NumberFormatException e) {
            logMessage("参数格式错误: " + field.getText() + "，使用默认值: " + defaultValue);
            return defaultValue;
        } catch (Exception e) {
            logMessage("获取浮点数值时发生异常: " + e.getMessage() + "，使用默认值: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * 以小端格式写入16位整数
     * @param buffer 缓冲区
     * @param offset 偏移量
     * @param value 值
     */
    private void writeInt16LittleEndian(byte[] buffer, int offset, int value) {
        // 检查缓冲区边界
        if (buffer == null) {
            throw new IllegalArgumentException("缓冲区不能为null");
        }
        if (offset < 0 || offset + 2 > buffer.length) {
            throw new IllegalArgumentException("缓冲区边界检查失败: offset=" + offset +
                ", buffer.length=" + buffer.length);
        }

        // 限制在16位无符号整数范围内 (0-65535)
        if (value < 0) {
            logMessage("数值为负数，设置为0: " + value);
            value = 0;
        } else if (value > 65535) {
            logMessage("数值超出16位范围，截断为65535: " + value);
            value = 65535;
        }

        buffer[offset] = (byte) (value & 0xFF);        // 低字节
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF); // 高字节
    }

    /**
     * 写入出厂日期
     * @param buffer 缓冲区
     * @param offset 偏移量
     * @param dateStr 日期字符串，格式：YYYYMMDDHHMM
     */
    private void writeFactoryDate(byte[] buffer, int offset, String dateStr) {
        // 检查缓冲区边界
        if (buffer == null) {
            throw new IllegalArgumentException("缓冲区不能为null");
        }
        if (offset < 0 || offset + 5 > buffer.length) {
            throw new IllegalArgumentException("缓冲区边界检查失败: offset=" + offset +
                ", buffer.length=" + buffer.length + ", 需要5个字节");
        }

        try {
            // 默认值：2024年1月1日0时0分
            int year = 2024;
            int month = 1;
            int day = 1;
            int hour = 0;
            int minute = 0;

            if (dateStr != null && !dateStr.trim().isEmpty()) {
                String cleanDate = dateStr.trim().replaceAll("[^0-9]", "");
                if (cleanDate.length() >= 12) {
                    year = Integer.parseInt(cleanDate.substring(0, 4));
                    month = Integer.parseInt(cleanDate.substring(4, 6));
                    day = Integer.parseInt(cleanDate.substring(6, 8));
                    hour = Integer.parseInt(cleanDate.substring(8, 10));
                    minute = Integer.parseInt(cleanDate.substring(10, 12));

                    // 验证数值范围
                    if (year < 2000 || year > 2255) {
                        logMessage("年份超出范围(2000-2255)，使用默认值: " + year);
                        year = 2024;
                    }
                    if (month < 1 || month > 12) {
                        logMessage("月份超出范围(1-12)，使用默认值: " + month);
                        month = 1;
                    }
                    if (day < 1 || day > 31) {
                        logMessage("日期超出范围(1-31)，使用默认值: " + day);
                        day = 1;
                    }
                    if (hour < 0 || hour > 23) {
                        logMessage("小时超出范围(0-23)，使用默认值: " + hour);
                        hour = 0;
                    }
                    if (minute < 0 || minute > 59) {
                        logMessage("分钟超出范围(0-59)，使用默认值: " + minute);
                        minute = 0;
                    }
                }
            }

            buffer[offset] = (byte) (year - 2000);  // 年份减去2000
            buffer[offset + 1] = (byte) month;      // 月
            buffer[offset + 2] = (byte) day;        // 日
            buffer[offset + 3] = (byte) hour;       // 时
            buffer[offset + 4] = (byte) minute;     // 分

        } catch (Exception e) {
            logMessage("出厂日期格式错误: " + dateStr + "，使用默认值");
            // 使用默认值：2024年1月1日0时0分
            buffer[offset] = (byte) 24;     // 2024年
            buffer[offset + 1] = (byte) 1;  // 1月
            buffer[offset + 2] = (byte) 1;  // 1日
            buffer[offset + 3] = (byte) 0;  // 0时
            buffer[offset + 4] = (byte) 0;  // 0分
        }
    }

    /**
     * 写入出厂序列号
     * @param buffer 缓冲区
     * @param offset 偏移量
     * @param serialStr 序列号字符串
     */
    private void writeSerialNumber(byte[] buffer, int offset, String serialStr) {
        // 检查缓冲区边界
        if (buffer == null) {
            throw new IllegalArgumentException("缓冲区不能为null");
        }
        if (offset < 0 || offset + 5 > buffer.length) {
            throw new IllegalArgumentException("缓冲区边界检查失败");
        }

        try {
            // 如果序列号为空或无效，使用默认值
            if (serialStr == null || serialStr.trim().isEmpty()) {
                // 默认序列号：00001
                for (int i = 0; i < 4; i++) {
                    buffer[offset + i] = 0;
                }
                buffer[offset + 4] = 1;
                return;
            }

            // 尝试解析序列号为数字
            long serialNumber = Long.parseLong(serialStr.trim());

            // 将序列号转换为5字节（40位）
            for (int i = 0; i < 5; i++) {
                buffer[offset + i] = (byte) ((serialNumber >> (i * 8)) & 0xFF);
            }

        } catch (NumberFormatException e) {
            // 如果不是数字，使用字符串的字节表示（截取前5字节）
            byte[] serialBytes = serialStr.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < 5; i++) {
                if (i < serialBytes.length) {
                    buffer[offset + i] = serialBytes[i];
                } else {
                    buffer[offset + i] = 0;
                }
            }
        }
    }

    /**
     * 保存当前测试参数到文件
     */
    private void saveCurrentParameters() {
        try {
            // 弹出对话框让用户输入文件名
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("保存测试参数");
            dialog.setHeaderText("请输入要保存的文件名");
            dialog.setContentText("文件名:");

            // 显示对话框并等待用户输入
            dialog.showAndWait().ifPresent(fileName -> {
                try {
                    // 处理文件名，确保不包含非法字符
                    String cleanFileName = fileName.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (cleanFileName.isEmpty()) {
                        cleanFileName = "test_parameters_" + System.currentTimeMillis();
                    }

                    // 确保文件名以.properties结尾
                    if (!cleanFileName.toLowerCase().endsWith(".properties")) {
                        cleanFileName += ".properties";
                    }

                    // 创建参数映射
                    Map<String, String> parameters = new HashMap<>();

                    // 参数名称映射
                    String[] paramNames = {
                        "电压校准值", "电压校准范围", "出厂日期", "电池串数", "产品功耗",
                        "产品功耗范围", "充电老化时间", "最大充电流", "充电电流范围", "最大充电温度",
                        "充电温度范围", "最大充电均衡电流", "充电均衡范围", "放电老化时间", "最大放电电流",
                        "放电电流范围", "最大放电温度", "放电温度范围", "最大放电均衡电流", "放电均衡范围"
                    };

                    TextField[] paramFields = {
                        param1Field, param2Field, param3Field, param4Field, param5Field,
                        param6Field, param7Field, param8Field, param9Field, param10Field,
                        param11Field, param12Field, param13Field, param14Field, param15Field,
                        param16Field, param17Field, param18Field, param19Field, param20Field
                    };

                    // 收集所有参数值并记录详细信息
                    logMessage("开始保存参数到文件: " + cleanFileName);
                    int savedCount = 0;

                    for (int i = 0; i < paramFields.length; i++) {
                        String paramKey = "param" + (i + 1);
                        String paramValue = paramFields[i].getText().trim();
                        parameters.put(paramKey, paramValue);

                        if (!paramValue.isEmpty()) {
                            logMessage(String.format("参数%d (%s): %s", i + 1, paramNames[i], paramValue));
                            savedCount++;
                        }
                    }

                    // 获取桌面路径
                    String userHome = System.getProperty("user.home");
                    File desktopDir = new File(userHome, "Desktop");
                    

                    // 如果Desktop目录不存在，尝试使用桌面的中文名称
                    if (!desktopDir.exists()) {
                        desktopDir = new File(userHome, "桌面");
                    }

                    // 如果还是不存在，使用用户主目录
                    if (!desktopDir.exists()) {
                        desktopDir = new File(userHome);
                        logMessage("桌面目录不存在，将保存到用户主目录: " + userHome);
                    }

                    // 创建测试参数专用文件夹
                    File paramDir = new File(desktopDir, "测试参数配置");
                    if (!paramDir.exists()) {
                        boolean created = paramDir.mkdirs();
                        if (created) {
                            logMessage("已创建测试参数文件夹: " + paramDir.getAbsolutePath());
                        } else {
                            logMessage("创建测试参数文件夹失败，使用桌面目录");
                            paramDir = desktopDir;
                        }
                    }

                    // 保存到文件
                    File paramFile = new File(paramDir, cleanFileName);
                    Properties props = new Properties();
                    props.putAll(parameters);

                    try (FileOutputStream fos = new FileOutputStream(paramFile)) {
                        props.store(fos, "Test Parameters - Saved at " + new Date() + " - File: " + cleanFileName);
                    }

                    logMessage("测试参数已保存到: " + paramFile.getAbsolutePath());
                    logMessage(String.format("共保存了 %d 个参数", savedCount));
                    addTestResult(String.format("参数保存成功 - 文件: %s (共%d个参数)", cleanFileName, savedCount));

                } catch (Exception e) {
                    logMessage("保存参数失败: " + e.getMessage());
                    addTestResult("参数保存失败: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            logMessage("保存参数失败: " + e.getMessage());

            addTestResult("参数保存失败: " + e.getMessage());
        }
    }

    /**
     * 从文件加载已保存的测试参数
     */
    private void loadSavedParameters() {
        try {
            // 获取桌面路径
            String userHome = System.getProperty("user.home");
            File targetDesktopDir = new File(userHome, "Desktop");

            // 如果Desktop目录不存在，尝试使用桌面的中文名称
            if (!targetDesktopDir.exists()) {
                targetDesktopDir = new File(userHome, "桌面");
            }

            // 如果还是不存在，使用用户主目录
            if (!targetDesktopDir.exists()) {
                targetDesktopDir = new File(userHome);
                logMessage("桌面目录不存在，将从用户主目录加载: " + userHome);
            }

            // 查找测试参数专用文件夹
            File paramDir = new File(targetDesktopDir, "测试参数配置");
            if (!paramDir.exists()) {
                logMessage("测试参数配置文件夹不存在: " + paramDir.getAbsolutePath());
                addTestResult("加载失败: 测试参数配置文件夹不存在");
                return;
            }

            final File finalParamDir = paramDir; // 创建final变量供lambda使用

            if (!finalParamDir.isDirectory()) {
                logMessage("测试参数配置路径不是文件夹: " + finalParamDir.getAbsolutePath());
                addTestResult("加载失败: 配置路径错误");
                return;
            }

            // 获取所有.properties文件
            File[] propertyFiles = finalParamDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".properties"));

            if (propertyFiles == null || propertyFiles.length == 0) {
                logMessage("测试参数配置文件夹中未找到任何参数文件");
                addTestResult("加载失败: 未找到参数文件");
                return;
            }

            // 创建文件选择对话框
            ChoiceDialog<String> dialog = new ChoiceDialog<>();
            dialog.setTitle("加载测试参数");
            dialog.setHeaderText("请选择要加载的参数文件");
            dialog.setContentText("文件:");

            // 添加文件选项
            for (File file : propertyFiles) {
                dialog.getItems().add(file.getName());
            }

            // 设置默认选择第一个文件
            if (!dialog.getItems().isEmpty()) {
                dialog.setSelectedItem(dialog.getItems().get(0));
            }

            // 显示对话框并等待用户选择
            dialog.showAndWait().ifPresent(selectedFileName -> {
                try {
                    File paramFile = new File(finalParamDir, selectedFileName);

                    Properties props = new Properties();
                    try (FileInputStream fis = new FileInputStream(paramFile)) {
                        props.load(fis);
                    }

                    // 加载参数到界面并记录详细信息
                    logMessage("开始加载参数文件: " + selectedFileName);

                    // 参数名称映射
                    String[] paramNames = {
                        "电压校准值", "电压校准范围", "出厂日期", "电池串数", "产品功耗",
                        "产品功耗范围", "充电老化时间", "最大充电流", "充电电流范围", "最大充电温度",
                        "充电温度范围", "最大充电均衡电流", "充电均衡范围", "放电老化时间", "最大放电电流",
                        "放电电流范围", "最大放电温度", "放电温度范围", "最大放电均衡电流", "放电均衡范围"
                    };

                    TextField[] paramFields = {
                        param1Field, param2Field, param3Field, param4Field, param5Field,
                        param6Field, param7Field, param8Field, param9Field, param10Field,
                        param11Field, param12Field, param13Field, param14Field, param15Field,
                        param16Field, param17Field, param18Field, param19Field, param20Field
                        
                    };

                    int loadedCount = 0;
                    for (int i = 0; i < paramFields.length; i++) {
                        String paramKey = "param" + (i + 1);
                        String paramValue = props.getProperty(paramKey, "");
                        paramFields[i].setText(paramValue);

                        if (!paramValue.isEmpty()) {
                            logMessage(String.format("参数%d (%s): %s", i + 1, paramNames[i], paramValue));
                            loadedCount++;
                        }
                    }

                    logMessage("测试参数加载完成: " + paramFile.getAbsolutePath());

                    logMessage(String.format("共加载了 %d 个参数", loadedCount));
                    addTestResult(String.format("参数加载成功 - 文件: %s (共%d个参数)", selectedFileName, loadedCount));

                } catch (Exception e) {
                    logMessage("加载参数失败: " + e.getMessage());
                    addTestResult("参数加载失败: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            logMessage("加载参数失败: " + e.getMessage());

            addTestResult("参数加载失败: " + e.getMessage());
        }
    }

    // ==================== 测试区域相关方法 ====================

    /**
     * 在指定测试区域显示测试结果
     * @param areaNumber 区域编号 (1-4)
     * @param utf8Data 解析后的UTF-8字符串数据
     * @param rawData 原始接收数据
     */
    private void displayTestResultInArea(int areaNumber, String utf8Data, String rawData) {
        addTestAreaMessage(areaNumber, "=== 测试结果 ===");

        // 显示解析后的UTF-8字符串
        if (utf8Data != null && !utf8Data.trim().isEmpty()) {
            addTestAreaMessage(areaNumber, "测试结果: " + utf8Data);
        }

        // 如果原始数据和解析数据不同，也显示原始数据
        if (!utf8Data.equals(rawData)) {
            addTestAreaMessage(areaNumber, "原始数据: " + rawData);
        }

        // 分析测试结果状态
        String resultStatus = analyzeTestResult(utf8Data);
        addTestAreaMessage(areaNumber, "结果状态: " + resultStatus);

        addTestAreaMessage(areaNumber, "==================");
    }

    /**
     * 分析测试结果状态
     * @param testData 测试数据
     * @return 结果状态描述
     */
    private String analyzeTestResult(String testData) {
        if (testData == null || testData.trim().isEmpty()) {
            return "无数据";
        }

        String data = testData.toLowerCase().trim();

        // 判断成功的关键词
        if (data.contains("成功") || data.contains("通过") || data.contains("ok") ||
            data.contains("pass") || data.contains("success") || data.contains("正常") ||
            data.contains("合格")) {
            return "✅ 测试通过";
        }

        // 判断失败的关键词
        if (data.contains("失败") || data.contains("错误") || data.contains("异常") ||
            data.contains("fail") || data.contains("error") || data.contains("ng") ||
            data.contains("不合格")) {
            return "❌ 测试失败";
        }

        // 默认为数据接收成功
        return "📄 数据接收完成";
    }

    /**
     * 向指定测试区域添加消息
     * @param areaNumber 区域编号 (1-6)
     * @param message 消息内容
     */
    private void addTestAreaMessage(int areaNumber, String message) {
        String timestamp = getCurrentTime();
        String formattedMessage = String.format("[%s] %s%n", timestamp, message);

        Platform.runLater(() -> {
            TextArea targetArea = getTestAreaByNumber(areaNumber);
            if (targetArea != null) {
                targetArea.appendText(formattedMessage);

                // 限制长度，防止内存溢出
                if (targetArea.getLength() > 50000) {
                    String currentText = targetArea.getText();
                    String newText = currentText.substring(currentText.length() / 2);
                    targetArea.setText("... (内容已截断) ...\n" + newText);
                }
            }
        });
    }

    /**
     * 根据区域编号获取对应的TextArea
     */
    private TextArea getTestAreaByNumber(int areaNumber) {
        switch (areaNumber) {
            case 1: return testArea1;
            case 2: return testArea2;
            case 3: return testArea3;
            case 4: return testArea4;
            case 5: return testArea5;
            case 6: return testArea6;
            default: return null;
        }
    }

    // 六个测试按钮的事件处理方法
    @FXML
    private void onTest1() {
        startAreaTest(1, "测试区域1");
    }

    @FXML
    private void onTest2() {
        startAreaTest(2, "测试区域2");
    }

    @FXML
    private void onTest3() {
        startAreaTest(3, "测试区域3");
    }

    @FXML
    private void onTest4() {
        startAreaTest(4, "测试区域4");
    }

    @FXML
    private void onTest5() {
        startAreaTest(5, "测试区域5");
    }

    @FXML
    private void onTest6() {
        startAreaTest(6, "测试区域6");
    }

    /**
     * 启动测试区域测试
     * @param areaNumber 区域编号
     * @param testType 测试类型
     */
    private void startAreaTest(int areaNumber, String testType) {
        // 检查该区域是否已在测试中
        if (testAreaStateManager.isAreaTesting(areaNumber)) {
            addTestAreaMessage(areaNumber, "⚠️ 警告: 测试区域" + areaNumber + "正在测试中");
            addTestAreaMessage(areaNumber, "💡 请等待当前测试完成后再试");
            return;
        }

        // 检查该区域的串口连接状态
        if (!multiSerialPortManager.isAreaConnected(areaNumber)) {
            addTestAreaMessage(areaNumber, "⚠️ 串口未连接，启用演示模式");
            addTestAreaMessage(areaNumber, "💡 连接真实设备可获得实际测试结果");
        }

        // 启动该区域的测试
        boolean started = testAreaStateManager.startAreaTest(areaNumber, testType);

        if (!started) {
            addTestAreaMessage(areaNumber, "❌ 无法启动测试");
            return;
        }

        addTestAreaMessage(areaNumber, "🚀 开始执行测试...");

        if (multiSerialPortManager.isAreaConnected(areaNumber)) {
            addTestAreaMessage(areaNumber, "📤 发送测试参数给下位机");
            addTestAreaMessage(areaNumber, "⏳ 等待下位机响应...");
        } else {
            addTestAreaMessage(areaNumber, "🎭 演示模式: 模拟测试流程");
        }

        // 启动测试任务
        Task<Void> testTask = createAreaTestTask(areaNumber, testType);
        executorService.submit(testTask);
    }

    /**
     * 创建测试区域测试任务
     */
    private Task<Void> createAreaTestTask(int areaNumber, String testType) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (multiSerialPortManager.isAreaConnected(areaNumber)) {
                    // 真实设备模式：等待下位机发送请求测试参数的数据包
                    // 这个等待会在parseReceivedDataForArea中的handleRequestTestParametersForArea方法中被处理

                    // 启动超时检测任务（10秒超时）
                    testAreaStateManager.setAreaTimeout(areaNumber, 10, "下位机响应超时");
                } else {
                    // 演示模式：模拟测试结果
                    Thread.sleep(2000); // 模拟等待时间
                    Platform.runLater(() -> {
                        if (testAreaStateManager.isAreaTesting(areaNumber)) {
                            // 模拟UTF-8测试结果
                            String[] sampleResults = {
                                "测试通过 - 电压值正常",
                                "电压校准成功 - 误差在允许范围内",
                                "设备响应正常 - 所有参数检测通过",
                                "参数设置完成 - 系统运行稳定",
                                "充电电流校准成功 - 精度达标",
                                "放电电流校准成功 - 性能优良"
                            };
                            String result = sampleResults[(areaNumber - 1) % sampleResults.length];
                            displayTestResultInArea(areaNumber, result, result);
                            updateTestAreaStatistics(result);

                            // 重置该区域状态
                            testAreaStateManager.stopAreaTest(areaNumber);
                        }
                    });
                }

                return null;
            }
        };
    }

    /**
     * 解析指定测试区域接收到的数据包
     * @param areaNumber 测试区域编号
     * @param data 接收到的数据字符串
     */
    private void parseReceivedDataForArea(int areaNumber, String data) {
        try {
            // 移除空格并转换为大写
            String cleanData = data.replaceAll("\\s+", "").toUpperCase();

            // 检查是否是6字节的数据包（请求或响应）
            // 格式：A5 06 [CMD] [2B CRC] 0D
            if (cleanData.length() >= 12 && cleanData.startsWith("A5") && cleanData.endsWith("0D")) {
                // 解析数据包
                String frameHeader = cleanData.substring(0, 2);   // A5
                String length = cleanData.substring(2, 4);        // 06
                String command = cleanData.substring(4, 6);       // 命令
                String crcLow = cleanData.substring(6, 8);        // CRC低字节 (小端格式)
                String crcHigh = cleanData.substring(8, 10);      // CRC高字节
                String frameEnd = cleanData.substring(10, 12);    // 0D

                // 验证基本格式
                if ("A5".equals(frameHeader) && "06".equals(length) && "0D".equals(frameEnd)) {
                    // 验证CRC
                    byte[] packet = new byte[3];
                    packet[0] = (byte) 0xA5;
                    packet[1] = (byte) 0x06;
                    packet[2] = (byte) Integer.parseInt(command, 16);
                    int calculatedCRC = calculateCRC16(packet, 0, 3);
                    int receivedCRC = (Integer.parseInt(crcHigh, 16) << 8) | Integer.parseInt(crcLow, 16);

                    if (calculatedCRC == receivedCRC) {
                        // 根据命令类型处理
                        if ("02".equals(command)) {
                            addTestAreaMessage(areaNumber, "收到下位机请求测试参数命令，CRC校验通过");
                            handleRequestTestParametersForArea(areaNumber);
                        } else if ("04".equals(command)) {
                            addTestAreaMessage(areaNumber, "收到下位机参数设置响应，CRC校验通过");
                            handleParameterSetResponseForArea(areaNumber);
                        } else {
                            addTestAreaMessage(areaNumber, "收到未知命令: " + command);
                        }
                    } else {
                        addTestAreaMessage(areaNumber, "收到数据包，但CRC校验失败");
                        testAreaStateManager.stopAreaTest(areaNumber);
                    }
                } else {
                    addTestAreaMessage(areaNumber, "收到数据包格式不匹配");
                }
            }
        } catch (Exception e) {
            addTestAreaMessage(areaNumber, "解析接收数据失败: " + e.getMessage());
        }
    }

    /**
     * 处理指定测试区域的请求测试参数命令
     */
    private void handleRequestTestParametersForArea(int areaNumber) {
        addTestAreaMessage(areaNumber, "下位机请求测试参数");
        addTestAreaMessage(areaNumber, "📤 发送测试参数数据给下位机...");
        sendTestParametersDataToArea(areaNumber);
    }

    /**
     * 处理指定测试区域的参数设置响应
     */
    private void handleParameterSetResponseForArea(int areaNumber) {
        addTestAreaMessage(areaNumber, "✅ 下位机确认参数设置成功");
        addTestAreaMessage(areaNumber, "📤 发送校准电压数据给下位机...");
        sendFixedFormatDataToArea(areaNumber);
    }

    /**
     * 向指定测试区域发送测试参数数据
     */
    private void sendTestParametersDataToArea(int areaNumber) {
        if (!multiSerialPortManager.isAreaConnected(areaNumber)) {
            addTestAreaMessage(areaNumber, "❌ 串口未连接，无法发送测试参数数据");
            return;
        }

        // 构造52字节的测试参数数据包（使用测试参数界面的真实数据）
        try {
            byte[] packet = new byte[52];
            int index = 0;

            // 1. 帧头 (1B)
            packet[index++] = (byte) 0xA5;

            // 2. 长度 (1B) - 数据域长度=42字节
            packet[index++] = (byte) 0x2A;

            // 3. 命令 (1B) - 下发参数
            packet[index++] = (byte) 0x03;

            // 4-7. 测试项 (4B) - 预留字段，全FF
            packet[index++] = (byte) 0xFF;
            packet[index++] = (byte) 0xFF;
            packet[index++] = (byte) 0xFF;
            packet[index++] = (byte) 0xFF;

            // 8-9. 电压校准值 (2B) - 从param1Field获取，单位：0.01V
            double voltageCalibrationV = getDoubleValueFromField(param1Field, 0.0);
            int voltageCalibration = (int) Math.round(voltageCalibrationV * 100);
            writeInt16LittleEndian(packet, index, voltageCalibration);
            index += 2;

            // 10-11. 电压校准范围 (2B) - 从param2Field获取，单位：0.01V
            double voltageRangeV = getDoubleValueFromField(param2Field, 0.0);
            int voltageRange = (int) Math.round(voltageRangeV * 100);
            writeInt16LittleEndian(packet, index, voltageRange);
            index += 2;

            // 12-16. 出厂日期 (5B) - 从param3Field获取
            writeFactoryDate(packet, index, param3Field.getText());
            index += 5;

            // 17-21. 出厂序列号 (5B) - 从param4Field获取
            writeSerialNumber(packet, index, param4Field.getText());
            index += 5;

            // 22-23. 充电电流校准值 (2B) - 从param5Field获取，单位：0.01A
            double chargeCurrentA = getDoubleValueFromField(param5Field, 0.0);
            int chargeCurrent = (int) Math.round(chargeCurrentA * 100);
            writeInt16LittleEndian(packet, index, chargeCurrent);
            index += 2;

            // 24-25. 充电电流校准范围 (2B) - 从param6Field获取，单位：0.01A
            double chargeCurrentRangeA = getDoubleValueFromField(param6Field, 0.0);
            int chargeCurrentRange = (int) Math.round(chargeCurrentRangeA * 100);
            writeInt16LittleEndian(packet, index, chargeCurrentRange);
            index += 2;

            // 26-27. 放电电流校准值 (2B) - 从param7Field获取，单位：0.01A
            double dischargeCurrentA = getDoubleValueFromField(param7Field, 0.0);
            int dischargeCurrent = (int) Math.round(dischargeCurrentA * 100);
            writeInt16LittleEndian(packet, index, dischargeCurrent);
            index += 2;

            // 28-29. 放电电流校准范围 (2B) - 从param8Field获取，单位：0.01A
            double dischargeCurrentRangeA = getDoubleValueFromField(param8Field, 0.0);
            int dischargeCurrentRange = (int) Math.round(dischargeCurrentRangeA * 100);
            writeInt16LittleEndian(packet, index, dischargeCurrentRange);
            index += 2;

            // 30-49. 其他参数 (20B) - 使用param9-param20的值
            // 简化处理：每个参数占2字节，共10个参数
            TextField[] remainingParams = {param9Field, param10Field, param11Field, param12Field, param13Field,
                                         param14Field, param15Field, param16Field, param17Field, param18Field};

            for (TextField paramField : remainingParams) {
                double paramValue = getDoubleValueFromField(paramField, 0.0);
                int intValue = (int) Math.round(paramValue * 100); // 转换为0.01单位
                writeInt16LittleEndian(packet, index, intValue);
                index += 2;
            }

            // 50-51. CRC校验 (2B)
            int crc = calculateCRC16(packet, 0, 49);
            packet[index++] = (byte) (crc & 0xFF);        // CRC低字节
            packet[index++] = (byte) ((crc >> 8) & 0xFF); // CRC高字节

            // 52. 帧尾 (1B): 0D
            packet[index] = (byte) 0x0D;

            // 发送数据包
            boolean success = multiSerialPortManager.sendDataToArea(areaNumber, packet);
            if (success) {
                addTestAreaMessage(areaNumber, "✅ 测试参数数据发送成功");
                addTestAreaMessage(areaNumber, String.format("📊 电压校准值: %.2fV, 校准范围: %.2fV",
                    voltageCalibrationV, voltageRangeV));
                testAreaStateManager.setAreaWaitingForCommand4Response(areaNumber, true);
            } else {
                addTestAreaMessage(areaNumber, "❌ 测试参数数据发送失败");
                testAreaStateManager.stopAreaTest(areaNumber);
            }
        } catch (Exception e) {
            addTestAreaMessage(areaNumber, "❌ 构造测试参数数据失败: " + e.getMessage());
            testAreaStateManager.stopAreaTest(areaNumber);
        }
    }

    /**
     * 向指定测试区域发送固定格式数据（校准电压）
     */
    private void sendFixedFormatDataToArea(int areaNumber) {
        if (!multiSerialPortManager.isAreaConnected(areaNumber)) {
            addTestAreaMessage(areaNumber, "❌ 串口未连接，无法发送校准电压数据");
            return;
        }

        try {
            // 构造8字节的数据包
            byte[] packet = new byte[8];
            int index = 0;

            // 1. 帧头 (1B): A5
            packet[index++] = (byte) 0xA5;

            // 2. 长度 (1B): 08
            packet[index++] = (byte) 0x08;

            // 3. 帧含义 (1B): 05
            packet[index++] = (byte) 0x05;

            // 4-5. 电压校准值 (2B): 40 1F
            // 固定值：0x1F40 = 8000，表示80.00V (单位：0.01V)
            packet[index++] = (byte) 0x40;  // 低字节
            packet[index++] = (byte) 0x1F;  // 高字节

            // 6-7. CRC校验 (2B) - MODBUS-CRC
            // 计算范围：从长度字段到电压校准值结束 (索引1-4)
            int crc = calculateCRC16(packet, 1, 4);
            packet[index++] = (byte) (crc & 0xFF);        // CRC低字节
            packet[index++] = (byte) ((crc >> 8) & 0xFF); // CRC高字节

            // 8. 帧尾 (1B): 0D
            packet[index] = (byte) 0x0D;

            // 转换为十六进制字符串发送
            StringBuilder hexString = new StringBuilder();
            for (byte b : packet) {
                hexString.append(String.format("%02X ", b & 0xFF));
            }

            String command = hexString.toString().trim();
            boolean success = multiSerialPortManager.sendDataToArea(areaNumber, command);

            if (success) {
                addTestAreaMessage(areaNumber, "📤 发送校准电压数据包");
                addTestAreaMessage(areaNumber, "📋 数据: " + command);
                addTestAreaMessage(areaNumber, "🔧 电压校准值: 80.00V");
                addTestAreaMessage(areaNumber, "⏳ 等待下位机测试结果...");
            } else {
                addTestAreaMessage(areaNumber, "❌ 发送校准电压数据失败");
                testAreaStateManager.stopAreaTest(areaNumber);
            }

        } catch (Exception e) {
            addTestAreaMessage(areaNumber, "❌ 发送校准电压数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新测试区域的统计信息
     */
    private void updateTestAreaStatistics(String receivedData) {
        String data = receivedData.toLowerCase().trim();

        // 判断成功的关键词
        boolean isSuccess = data.contains("成功") || data.contains("通过") || data.contains("ok") ||
                           data.contains("pass") || data.contains("success") || data.contains("正常") ||
                           data.contains("合格");

        // 判断失败的关键词
        boolean isFailure = data.contains("失败") || data.contains("错误") || data.contains("异常") ||
                           data.contains("fail") || data.contains("error") || data.contains("ng") ||
                
                           data.contains("不合格");

        // 更新计数器
        if (isSuccess && !isFailure) {
            successCount.incrementAndGet();

            addTestAreaMessage(currentTestArea, "检测到测试成功结果，成功计数+1");
        } else if (isFailure && !isSuccess) {
            failureCount.incrementAndGet();
            addTestAreaMessage(currentTestArea, "检测到测试失败结果，失败计数+1");
        }

        // 更新界面显示
        Platform.runLater(this::updateCounters);
    }

    // ==================== 测试区域串口连接方法 ====================

    @FXML
    private void onArea1Connect() {
        connectAreaPort(1, area1PortComboBox, area1ConnectButton, area1DisconnectButton, area1StatusLabel, testButton1);
    }

    @FXML
    private void onArea1Disconnect() {
        disconnectAreaPort(1, area1ConnectButton, area1DisconnectButton, area1StatusLabel, testButton1);
    }

    @FXML
    private void onArea2Connect() {
        connectAreaPort(2, area2PortComboBox, area2ConnectButton, area2DisconnectButton, area2StatusLabel, testButton2);
    }

    @FXML
    private void onArea2Disconnect() {
        disconnectAreaPort(2, area2ConnectButton, area2DisconnectButton, area2StatusLabel, testButton2);
    }

    @FXML
    private void onArea3Connect() {
        connectAreaPort(3, area3PortComboBox, area3ConnectButton, area3DisconnectButton, area3StatusLabel, testButton3);
    }

    @FXML
    private void onArea3Disconnect() {
        disconnectAreaPort(3, area3ConnectButton, area3DisconnectButton, area3StatusLabel, testButton3);
    }

    @FXML
    private void onArea4Connect() {
        connectAreaPort(4, area4PortComboBox, area4ConnectButton, area4DisconnectButton, area4StatusLabel, testButton4);
    }

    @FXML
    private void onArea4Disconnect() {
        disconnectAreaPort(4, area4ConnectButton, area4DisconnectButton, area4StatusLabel, testButton4);
    }

    @FXML
    private void onArea5Connect() {
        connectAreaPort(5, area5PortComboBox, area5ConnectButton, area5DisconnectButton, area5StatusLabel, testButton5);
    }

    @FXML
    private void onArea5Disconnect() {
        disconnectAreaPort(5, area5ConnectButton, area5DisconnectButton, area5StatusLabel, testButton5);
    }

    @FXML
    private void onArea6Connect() {
        connectAreaPort(6, area6PortComboBox, area6ConnectButton, area6DisconnectButton, area6StatusLabel, testButton6);
    }

    @FXML
    private void onArea6Disconnect() {
        disconnectAreaPort(6, area6ConnectButton, area6DisconnectButton, area6StatusLabel, testButton6);
    }

    /**
     * 连接测试区域的串口
     */
    private void connectAreaPort(int areaNumber, ComboBox<String> portComboBox,
                                Button connectButton, Button disconnectButton,
                                Label statusLabel, Button testButton) {
        String selectedPort = portComboBox.getValue();

        if (selectedPort == null || selectedPort.trim().isEmpty()) {
            addTestAreaMessage(areaNumber, "❌ 请先选择一个串口");
            return;
        }

        // 使用固定波特率9600
        int baudRate = 9600;

        boolean connected = multiSerialPortManager.connectAreaPort(areaNumber, selectedPort, baudRate);

        if (connected) {
            // 更新UI状态
            statusLabel.setText("已连接");
            statusLabel.setStyle("-fx-text-fill: green;");
            connectButton.setDisable(true);
            disconnectButton.setDisable(false);
            portComboBox.setDisable(true);
            // testButton的状态由updateControlsState统一管理

            addTestAreaMessage(areaNumber, "✅ 串口连接成功: " + selectedPort);
            addTestAreaMessage(areaNumber, "🚀 测试区域已就绪，可以开始测试");

            // 更新所有控件状态
            updateControlsState();
        } else {
            addTestAreaMessage(areaNumber, "❌ 串口连接失败: " + selectedPort);
        }
    }

    /**
     * 断开测试区域的串口
     */
    private void disconnectAreaPort(int areaNumber, Button connectButton, Button disconnectButton,
                                   Label statusLabel, Button testButton) {
        // 停止该区域的测试
        testAreaStateManager.stopAreaTest(areaNumber);

        boolean disconnected = multiSerialPortManager.disconnectAreaPort(areaNumber);

        if (disconnected) {
            // 更新UI状态
            statusLabel.setText("未连接");
            statusLabel.setStyle("-fx-text-fill: red;");
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            getAreaPortComboBox(areaNumber).setDisable(false);
            // testButton的状态由updateControlsState统一管理

            addTestAreaMessage(areaNumber, "🔌 串口连接已断开");

            // 更新所有控件状态
            updateControlsState();
        } else {
            addTestAreaMessage(areaNumber, "❌ 串口断开失败");
        }
    }

    /**
     * 根据区域编号获取对应的串口选择控件
     */
    private ComboBox<String> getAreaPortComboBox(int areaNumber) {
        switch (areaNumber) {
            case 1: return area1PortComboBox;
            case 2: return area2PortComboBox;
            case 3: return area3PortComboBox;
            case 4: return area4PortComboBox;
            case 5: return area5PortComboBox;
            case 6: return area6PortComboBox;
            default: return null;
        }
    }

    /**
     * 刷新所有测试区域的串口列表
     */
    public void refreshAllAreaPorts() {
        List<String> availablePorts = multiSerialPortManager.getAvailablePorts();
        ObservableList<String> portList = FXCollections.observableArrayList(availablePorts);

        // 保存当前选择
        String[] currentSelections = {
            area1PortComboBox.getValue(),
            area2PortComboBox.getValue(),
            area3PortComboBox.getValue(),
            area4PortComboBox.getValue(),
            area5PortComboBox.getValue(),
            area6PortComboBox.getValue()
        };

        // 更新所有下拉框
        area1PortComboBox.setItems(portList);
        area2PortComboBox.setItems(portList);
        area3PortComboBox.setItems(portList);
        area4PortComboBox.setItems(portList);
        area5PortComboBox.setItems(portList);
        area6PortComboBox.setItems(portList);

        // 恢复之前的选择（如果串口仍然可用）
        if (currentSelections[0] != null && availablePorts.contains(currentSelections[0])) {
            area1PortComboBox.setValue(currentSelections[0]);
        }
        if (currentSelections[1] != null && availablePorts.contains(currentSelections[1])) {
            area2PortComboBox.setValue(currentSelections[1]);
        }
        if (currentSelections[2] != null && availablePorts.contains(currentSelections[2])) {
            area3PortComboBox.setValue(currentSelections[2]);
        }
        if (currentSelections[3] != null && availablePorts.contains(currentSelections[3])) {
            area4PortComboBox.setValue(currentSelections[3]);
        }
        if (currentSelections[4] != null && availablePorts.contains(currentSelections[4])) {
            area5PortComboBox.setValue(currentSelections[4]);
        }
        if (currentSelections[5] != null && availablePorts.contains(currentSelections[5])) {
            area6PortComboBox.setValue(currentSelections[5]);
        }

        logMessage("🔄 刷新串口列表: 找到 " + availablePorts.size() + " 个串口: " + String.join(", ", availablePorts));
    }

    // ==================== 新布局的按钮处理方法 ====================

    @FXML
    private void onRefreshPorts() {
        refreshAllAreaPorts();
    }
}

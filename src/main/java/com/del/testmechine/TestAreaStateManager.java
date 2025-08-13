package com.del.testmechine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 测试区域状态管理器，支持多区域并发测试
 */
public class TestAreaStateManager {
    
    // 每个测试区域的状态
    private final Map<Integer, TestAreaState> areaStates = new ConcurrentHashMap<>();
    
    // 每个测试区域的超时任务
    private final Map<Integer, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();
    
    // 超时执行器服务
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(4);
    
    // 超时回调
    private TimeoutCallback timeoutCallback;

    // 全局日志回调
    private Consumer<String> globalLogCallback;
    
    public TestAreaStateManager() {
        // 初始化六个测试区域的状态
        for (int i = 1; i <= 6; i++) {
            areaStates.put(i, new TestAreaState());
        }
    }
    
    /**
     * 测试区域状态类
     */
    public static class TestAreaState {
        private volatile boolean isTestRunning = false;
        private volatile boolean waitingForResponse = false;
        private volatile boolean waitingForCommand4Response = false;
        private volatile String currentTestType = "";
        private volatile long testStartTime = 0;
        
        // Getters and Setters
        public boolean isTestRunning() { return isTestRunning; }
        public void setTestRunning(boolean testRunning) { this.isTestRunning = testRunning; }
        
        public boolean isWaitingForResponse() { return waitingForResponse; }
        public void setWaitingForResponse(boolean waiting) { this.waitingForResponse = waiting; }
        
        public boolean isWaitingForCommand4Response() { return waitingForCommand4Response; }
        public void setWaitingForCommand4Response(boolean waiting) { this.waitingForCommand4Response = waiting; }
        
        public String getCurrentTestType() { return currentTestType; }
        public void setCurrentTestType(String testType) { this.currentTestType = testType; }
        
        public long getTestStartTime() { return testStartTime; }
        public void setTestStartTime(long startTime) { this.testStartTime = startTime; }
        
        public void reset() {
            isTestRunning = false;
            waitingForResponse = false;
            waitingForCommand4Response = false;
            currentTestType = "";
            testStartTime = 0;
        }
    }
    
    /**
     * 超时回调接口
     */
    public interface TimeoutCallback {
        void onTimeout(int areaNumber, String reason);
    }
    
    /**
     * 设置超时回调
     */
    public void setTimeoutCallback(TimeoutCallback callback) {
        this.timeoutCallback = callback;
    }

    /**
     * 设置全局日志回调
     */
    public void setGlobalLogCallback(Consumer<String> callback) {
        this.globalLogCallback = callback;
    }
    
    /**
     * 开始测试区域测试
     */
    public boolean startAreaTest(int areaNumber, String testType) {
        if (areaNumber < 1 || areaNumber > 6) {
            return false;
        }
        
        TestAreaState state = areaStates.get(areaNumber);
        
        // 检查是否已在测试中
        if (state.isTestRunning()) {
            return false;
        }
        
        // 设置测试状态
        state.setTestRunning(true);
        state.setCurrentTestType(testType);
        state.setTestStartTime(System.currentTimeMillis());
        state.setWaitingForResponse(true);
        
        return true;
    }
    
    /**
     * 停止测试区域测试
     */
    public void stopAreaTest(int areaNumber) {
        if (areaNumber < 1 || areaNumber > 6) {
            return;
        }
        
        TestAreaState state = areaStates.get(areaNumber);
        state.reset();
        
        // 取消超时任务
        cancelTimeoutTask(areaNumber);
    }
    
    /**
     * 设置测试区域的超时任务
     */
    public void setAreaTimeout(int areaNumber, long timeoutSeconds, String reason) {
        if (areaNumber < 1 || areaNumber > 6) {
            return;
        }
        
        // 取消之前的超时任务
        cancelTimeoutTask(areaNumber);
        
        // 创建新的超时任务
        ScheduledFuture<?> timeoutTask = timeoutExecutor.schedule(() -> {
            TestAreaState state = areaStates.get(areaNumber);
            if (state.isTestRunning() && timeoutCallback != null) {
                timeoutCallback.onTimeout(areaNumber, reason);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
        
        timeoutTasks.put(areaNumber, timeoutTask);
    }
    
    /**
     * 取消测试区域的超时任务
     */
    public void cancelTimeoutTask(int areaNumber) {
        ScheduledFuture<?> task = timeoutTasks.remove(areaNumber);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }
    
    /**
     * 获取测试区域状态
     */
    public TestAreaState getAreaState(int areaNumber) {
        return areaStates.get(areaNumber);
    }
    
    /**
     * 检查测试区域是否正在测试
     */
    public boolean isAreaTesting(int areaNumber) {
        if (areaNumber < 1 || areaNumber > 6) {
            return false;
        }
        return areaStates.get(areaNumber).isTestRunning();
    }
    
    /**
     * 获取正在测试的区域数量
     */
    public int getActiveTestCount() {
        int count = 0;
        for (TestAreaState state : areaStates.values()) {
            if (state.isTestRunning()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取所有区域的测试状态
     */
    public Map<Integer, Boolean> getAllTestingStatus() {
        Map<Integer, Boolean> status = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, TestAreaState> entry : areaStates.entrySet()) {

            status.put(entry.getKey(), entry.getValue().isTestRunning());
        }
        return status;
    }
    
    /**
     * 停止所有测试
     */
    public void stopAllTests() {
        for (int i = 1; i <= 6; i++) {
            stopAreaTest(i);
        }
    }
    
    /**
     * 关闭状态管理器
     */
    public void shutdown() {
        // 停止所有测试
        stopAllTests();
        
        // 关闭超时执行器
        if (timeoutExecutor != null && !timeoutExecutor.isShutdown()) {
            timeoutExecutor.shutdown();
            try {
                if (!timeoutExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    timeoutExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 设置测试区域等待响应状态
     */
    public void setAreaWaitingForResponse(int areaNumber, boolean waiting) {
        if (areaNumber >= 1 && areaNumber <= 6) {
            areaStates.get(areaNumber).setWaitingForResponse(waiting);
        }
    }

    /**
     * 设置测试区域等待命令4响应状态
     */
    public void setAreaWaitingForCommand4Response(int areaNumber, boolean waiting) {
        if (areaNumber >= 1 && areaNumber <= 6) {
            areaStates.get(areaNumber).setWaitingForCommand4Response(waiting);
        }
    }

    /**
     * 检查测试区域是否等待响应
     */
    public boolean isAreaWaitingForResponse(int areaNumber) {
        if (areaNumber < 1 || areaNumber > 6) {
            return false;
        }
        return areaStates.get(areaNumber).isWaitingForResponse();
    }

    /**
     * 检查测试区域是否等待命令4响应
     */
    public boolean isAreaWaitingForCommand4Response(int areaNumber) {
        if (areaNumber < 1 || areaNumber > 6) {
            return false;
        }
        return areaStates.get(areaNumber).isWaitingForCommand4Response();
    }
    
    private void log(String message) {
        if (globalLogCallback != null) {
            globalLogCallback.accept(message);
        }
    }
}

package com.javaee.common;

import com.javaee.backend.common.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Result 统一响应结果测试")
class ResultTest {

    @Test
    @DisplayName("成功响应应包含正确状态码和数据")
    void testSuccessResponseWithData() {
        String testData = "test data";
        Result<String> result = Result.success(testData);
        
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals(testData, result.getData());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    @DisplayName("成功响应应支持自定义消息")
    void testSuccessResponseWithCustomMessage() {
        String customMessage = "查询成功";
        Integer testData = 42;
        
        Result<Integer> result = Result.success(customMessage, testData);
        
        assertEquals(200, result.getCode());
        assertEquals(customMessage, result.getMessage());
        assertEquals(testData, result.getData());
    }

    @Test
    @DisplayName("错误响应应包含错误码和消息")
    void testErrorResponseWithCodeAndMessage() {
        int errorCode = 404;
        String errorMessage = "资源不存在";
        
        Result<?> result = Result.error(errorCode, errorMessage);
        
        assertEquals(errorCode, result.getCode());
        assertEquals(errorMessage, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("默认错误响应应使用500状态码")
    void testDefaultErrorResponse() {
        String errorMessage = "服务器内部错误";
        
        Result<?> result = Result.error(errorMessage);
        
        assertEquals(500, result.getCode());
        assertEquals(errorMessage, result.getMessage());
    }

    @Test
    @DisplayName("时间戳应接近当前时间")
    void testTimestampIsCurrentTime() {
        long beforeCall = System.currentTimeMillis();
        Result<String> result = Result.success("data");
        long afterCall = System.currentTimeMillis();
        
        assertTrue(result.getTimestamp() >= beforeCall && 
                    result.getTimestamp() <= afterCall,
                "时间戳应该在方法调用前后之间");
    }

    @Test
    @DisplayName("成功响应的数据可以为null")
    void testSuccessWithNullData() {
        Result<Void> result = Result.success(null);
        
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("不同类型的数据应该正常工作")
    void testDifferentDataTypes() {
        Result<String> stringResult = Result.success("string");
        Result<Integer> intResult = Result.success(123);
        Result<Boolean> boolResult = Result.success(true);
        
        assertInstanceOf(String.class, stringResult.getData());
        assertInstanceOf(Integer.class, intResult.getData());
        assertInstanceOf(Boolean.class, boolResult.getData());
    }
}

package com.javaee.backend.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 自定义 List<String> 类型处理器
 * 处理数据库中 JSON 数组字符串与 Java List<String> 的转换
 */
@Component
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setString(i, "[]");
        } else {
            // 简单拼接：["tag1", "tag2"]
            String json = "\"" + String.join("\",\"", parameter) + "\"";
            ps.setString(i, "[" + json + "]");
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJsonArray(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJsonArray(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJsonArray(cs.getString(columnIndex));
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return Collections.emptyList();
        }
        // 去除方括号，分割字符串
        String content = json.replaceAll("[\\[\\]\"]", "").trim();
        if (content.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(content.split(","));
    }
}

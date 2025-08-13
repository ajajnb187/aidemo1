package org.example.toolmcp.tool;

// 导入Hutool工具类，用于字符串处理

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具类
 * 作为MCP服务暴露给AI模型调用的工具
 * 通过Pexels API实现网络图片搜索功能
 */
@Service // 标记为Spring服务，使其能被Spring容器管理
public class ImageSearchTool {

    // Pexels API密钥，用于API认证
    // 实际使用时需要从Pexels官网申请自己的API密钥
    private static final String API_KEY = "ivRN0cJ9xtggvfncztepZ1Ub8CEBeMeMMfYhiJtrijWdImqfy3r2Focl";

    // Pexels API的搜索接口地址
    private static final String API_URL = "https://api.pexels.com/v1/search";

    /**
     * 图片搜索工具方法
     * 被@Tool注解标记，使其能被AI模型调用
     * description提供了对该工具的描述，AI会根据这个描述决定何时使用此工具
     *
     * @param query 搜索关键词，被@ToolParam标记，提供参数描述
     * @return 返回逗号分隔的图片URL字符串
     */
    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            System.out.println("Searching image for: " + query);

            // 调用searchMediumImages方法获取图片URL列表，并用逗号连接成字符串返回
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            // 异常处理，返回错误信息
            return "Error search image: " + e.getMessage();
        }
    }

    /**
     * 搜索中等尺寸的图片列表
     * 实际调用Pexels API并处理响应的核心方法
     *
     * @param query 搜索关键词
     * @return 返回图片URL列表
     */
    public List<String> searchMediumImages(String query) {
        // 创建请求头Map，用于存放HTTP请求头信息
        Map<String, String> headers = new HashMap<>();
        // 添加Authorization请求头，值为API密钥，这是Pexels API的认证方式
        headers.put("Authorization", API_KEY);

        // 创建请求参数Map，用于存放HTTP请求参数
        Map<String, Object> params = new HashMap<>();
        // 添加query参数，即搜索关键词
        // 注：可以根据Pexels API文档添加更多参数，如page、per_page等
        params.put("query", query);

        // 使用Hutool的HttpUtil发送GET请求
        // 1. createGet创建GET请求对象
        // 2. addHeaders添加请求头
        // 3. form添加表单参数
        // 4. execute执行请求
        // 5. body获取响应体
        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        // 解析响应JSON
        return JSONUtil.parseObj(response) // 将响应字符串解析为JSONObject
                .getJSONArray("photos") // 获取photos数组
                .stream() // 转换为Java流处理
                .map(photoObj -> (JSONObject) photoObj) // 将每个元素转为JSONObject
                .map(photoObj -> photoObj.getJSONObject("src")) // 获取每个photo的src对象
                .map(photo -> photo.getStr("medium")) // 从src对象中获取medium字段值(图片URL)
                .filter(StrUtil::isNotBlank) // 过滤掉空URL
                .collect(Collectors.toList()); // 收集结果到List
    }
}

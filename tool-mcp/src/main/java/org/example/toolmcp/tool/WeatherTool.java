package org.example.toolmcp.tool;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.example.toolmcp.uity.cityCode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 天气查询工具
 * 在本地MCP服务器中直接调用阿里云百炼的Amap Maps API
 */
@Service
@Slf4j
public class WeatherTool {

    private final String baseurl = "http://t.weather.itboy.net/api/weather/city/";

    @Autowired
    private cityCode citycode;


    /**
     * 查询指定城市的天气信息
     */
    @Tool(description = "Query weather information for a specific city")
    public JSONObject queryWeather(@ToolParam(description = "City name") String cityName) {
        System.out.println("cityName:"+cityName);
        return callAmapMapsApi(citycode.getCityCode(cityName));
    }



    /**
     * 调用天气API
     */
    private JSONObject callAmapMapsApi(String cityCode) {
        String url = baseurl + cityCode;
        String response =HttpUtil.createGet(url)
                .form(cityCode)
                .execute()
                .body();

        log.info("response:{}", response);
        return new JSONObject(response);


    }
}



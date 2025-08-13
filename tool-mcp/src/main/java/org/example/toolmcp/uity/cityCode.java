package org.example.toolmcp.uity;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Data
public class cityCode {

    private HashMap<String,String> cityCodeMap = new HashMap<>();

    @PostConstruct
    public void init(){

        try {
            // 读取JSON文件内容
            String jsonStr = ResourceUtil.readUtf8Str("city_code.json");
            
            // 方式1：使用 TypeReference
            cityCodeMap = JSONUtil.toBean(jsonStr, new TypeReference<HashMap<String,String>>(){}, false);

        }
        catch (Exception e) {
            try {
                // 方式2：如果上面方法不行，使用parseObj
                String jsonStr = ResourceUtil.readUtf8Str("city_code.json");
                cityCodeMap = (HashMap<String,String>) JSONUtil.parseObj(jsonStr).toBean(HashMap.class);
            } catch (Exception e2) {
                // 方式3：最简单的方式
                try {
                    String jsonStr = ResourceUtil.readUtf8Str("city_code.json");
                    Map<String, Object> tempMap = JSONUtil.parseObj(jsonStr);
                    cityCodeMap = new HashMap<>();
                    tempMap.forEach((key, value) -> cityCodeMap.put(key, value.toString()));
                } catch (Exception e3) {
                    System.err.println("加载城市代码数据失败: " + e3.getMessage());
                    e3.printStackTrace();
                }
            }
        }

    }

    /**
     * 根据城市名模糊查询城市代码（智能匹配）
     * @param cityName 城市名称（支持模糊查询，智能处理"市"、"区"、"县"等后缀）
     * @return 匹配的城市和代码列表
     */
    public Map<String, String> fuzzySearchCity(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        String searchKey = cityName.trim();
        LinkedHashMap<String, String> results = new LinkedHashMap<>();
        
        // 1. 精确匹配
        if (cityCodeMap.containsKey(searchKey)) {
            results.put(searchKey, cityCodeMap.get(searchKey));
        }
        
        // 2. 智能匹配 - 处理城市名变体
        Set<String> searchVariants = generateSearchVariants(searchKey);
        
        for (Map.Entry<String, String> entry : cityCodeMap.entrySet()) {
            String cityInMap = entry.getKey();
            String cityCode = entry.getValue();
            
            // 避免重复添加已经精确匹配的结果
            if (results.containsKey(cityInMap)) {
                continue;
            }
            
            // 检查是否匹配
            if (isSmartMatch(searchKey, searchVariants, cityInMap)) {
                results.put(cityInMap, cityCode);
            }
        }
        
        return results;
    }
    
    /**
     * 生成搜索词的各种变体
     * @param searchKey 原始搜索词
     * @return 搜索词变体集合
     */
    private Set<String> generateSearchVariants(String searchKey) {
        Set<String> variants = new HashSet<>();
        variants.add(searchKey);
        
        // 去除常见后缀的变体
        String normalized = normalizeCityName(searchKey);
        if (!normalized.equals(searchKey)) {
            variants.add(normalized);
        }
        
        // 添加常见后缀的变体
        if (!searchKey.endsWith("市") && !searchKey.endsWith("区") && 
            !searchKey.endsWith("县") && !searchKey.endsWith("镇")) {
            variants.add(searchKey + "市");
            variants.add(searchKey + "区");
            variants.add(searchKey + "县");
        }
        
        return variants;
    }
    
    /**
     * 标准化城市名称（去除常见后缀）
     * @param cityName 城市名称
     * @return 标准化后的城市名称
     */
    private String normalizeCityName(String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            return cityName;
        }
        
        // 定义常见的城市后缀
        String[] suffixes = {"市", "区", "县", "镇", "旗", "盟", "州", "地区", "自治区", "自治州", "自治县"};
        
        for (String suffix : suffixes) {
            if (cityName.endsWith(suffix) && cityName.length() > suffix.length()) {
                return cityName.substring(0, cityName.length() - suffix.length());
            }
        }
        
        return cityName;
    }
    
    /**
     * 智能匹配判断
     * @param originalSearch 原始搜索词
     * @param searchVariants 搜索词变体
     * @param cityInMap JSON中的城市名
     * @return 是否匹配
     */
    private boolean isSmartMatch(String originalSearch, Set<String> searchVariants, String cityInMap) {
        // 1. 直接包含匹配
        if (cityInMap.contains(originalSearch)) {
            return true;
        }
        
        // 2. 搜索词变体匹配
        for (String variant : searchVariants) {
            if (cityInMap.contains(variant)) {
                return true;
            }
        }
        
        // 3. 反向匹配：JSON中城市名的标准化版本是否包含搜索词
        String normalizedCityInMap = normalizeCityName(cityInMap);
        if (!normalizedCityInMap.equals(cityInMap)) {
            if (normalizedCityInMap.contains(originalSearch) || 
                originalSearch.contains(normalizedCityInMap)) {
                return true;
            }
            
            // 检查搜索词变体是否匹配标准化的城市名
            for (String variant : searchVariants) {
                if (normalizedCityInMap.contains(variant) || 
                    variant.contains(normalizedCityInMap)) {
                    return true;
                }
            }
        }
        
        // 4. 标准化后的双向包含匹配
        String normalizedSearch = normalizeCityName(originalSearch);
        if (!normalizedSearch.equals(originalSearch)) {
            if (cityInMap.contains(normalizedSearch) || 
                normalizedCityInMap.contains(normalizedSearch)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取指定城市的城市代码（智能匹配）
     * @param cityName 城市名称（支持智能匹配，如"北京市"匹配"北京"，"梧州市"匹配"梧州"）
     * @return 城市代码，如果不存在返回null
     */
    public String getCityCode(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return null;
        }
        
        String searchKey = cityName.trim();
        
        // 1. 精确匹配
        if (cityCodeMap.containsKey(searchKey)) {
            return cityCodeMap.get(searchKey);
        }
        
        // 2. 智能后缀匹配 - 优先尝试去掉后缀的匹配
        String normalizedSearch = normalizeCityName(searchKey);
        if (!normalizedSearch.equals(searchKey) && cityCodeMap.containsKey(normalizedSearch)) {
            // 如"梧州市" -> "梧州"，直接匹配成功
            return cityCodeMap.get(normalizedSearch);
        }
        
        // 3. 反向匹配 - 如果搜索词没有后缀，尝试匹配有后缀的城市名
        if (normalizedSearch.equals(searchKey)) {
            // 生成可能的后缀变体
            String[] suffixes = {"市", "区", "县", "镇"};
            for (String suffix : suffixes) {
                String withSuffix = searchKey + suffix;
                if (cityCodeMap.containsKey(withSuffix)) {
                    return cityCodeMap.get(withSuffix);
                }
            }
        }
        
        // 4. 模糊匹配 - 使用智能模糊查询
        Map<String, String> fuzzyResults = fuzzySearchCity(searchKey);
        
        if (fuzzyResults.isEmpty()) {
            return null;
        }
        
        // 5. 从模糊匹配结果中选择最佳匹配
        // 优先级：标准化后完全匹配 > 包含匹配 > 第一个结果
        
        // 5.1 标准化后完全匹配
        for (Map.Entry<String, String> entry : fuzzyResults.entrySet()) {
            String cityInMap = entry.getKey();
            String normalizedCity = normalizeCityName(cityInMap);
            
            if (normalizedSearch.equals(normalizedCity)) {
                return entry.getValue();
            }
        }
        
        // 5.2 完全包含匹配（搜索词包含在城市名中，或城市名包含在搜索词中）
        for (Map.Entry<String, String> entry : fuzzyResults.entrySet()) {
            String cityInMap = entry.getKey();
            
            if (cityInMap.equals(normalizedSearch) || normalizedSearch.equals(cityInMap)) {
                return entry.getValue();
            }
        }
        
        // 5.3 返回第一个匹配结果
        return fuzzyResults.values().iterator().next();
    }

    /**
     * 获取所有城市列表
     * @return 所有城市的Map
     */
    public Map<String, String> getAllCities() {
        return new HashMap<>(cityCodeMap);
    }

    /**
     * 根据城市代码查找城市名称
     * @param code 城市代码
     * @return 城市名称，如果不存在返回null
     */
    public String getCityNameByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        return cityCodeMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(code.trim()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查城市是否存在（智能匹配）
     * @param cityName 城市名称（支持智能匹配，如"北京市"匹配"北京"）
     * @return 是否存在
     */
    public boolean isCityExists(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return false;
        }
        
        String searchKey = cityName.trim();
        
        // 1. 精确匹配
        if (cityCodeMap.containsKey(searchKey)) {
            return true;
        }
        
        // 2. 智能匹配
        Map<String, String> fuzzyResults = fuzzySearchCity(searchKey);
        return !fuzzyResults.isEmpty();
    }


}

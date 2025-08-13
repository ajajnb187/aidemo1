package org.example.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI诊断结构化响应
 * 用于友好展示病虫害诊断结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"diseaseInfo", "symptoms", "causes", "treatment", "preventions", "notes"})
public class DiagnosisResponse {
    
    /**
     * 病害基本信息
     */
    private DiseaseInfo diseaseInfo;
    
    /**
     * 症状特征
     */
    private List<String> symptoms;
    
    /**
     * 发病原因
     */
    private List<String> causes;
    
    /**
     * 治疗方案
     */
    private TreatmentPlan treatment;
    
    /**
     * 预防措施
     */
    private List<String> preventions;
    
    /**
     * 注意事项
     */
    private List<String> notes;
    
    /**
     * 病害基本信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonPropertyOrder({"name", "severity", "description", "weatherImpact"})
    public static class DiseaseInfo {
        /**
         * 病害名称
         */
        private String name;
        
        /**
         * 严重程度 (轻微/中等/严重)
         */
        private String severity;
        
        /**
         * 病害描述
         */
        private String description;
        
        /**
         * 天气影响分析
         */
        private String weatherImpact;
    }
    
    /**
     * 治疗方案
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonPropertyOrder({"urgentActions", "medications", "timing", "application"})
    public static class TreatmentPlan {
        /**
         * 紧急措施
         */
        private List<String> urgentActions;
        
        /**
         * 推荐药剂
         */
        private List<Medication> medications;
        
        /**
         * 用药时机
         */
        private String timing;
        
        /**
         * 施药方法
         */
        private String application;
    }
    
    /**
     * 药剂信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonPropertyOrder({"name", "dosage", "frequency", "duration"})
    public static class Medication {
        /**
         * 药剂名称
         */
        private String name;
        
        /**
         * 用药剂量
         */
        private String dosage;
        
        /**
         * 使用频次
         */
        private String frequency;
        
        /**
         * 使用周期
         */
        private String duration;
    }
}


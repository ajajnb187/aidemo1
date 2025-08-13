package org.example.sdspringboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * RagFlow API 相关的数据传输对象（DTO）。
 *
 * 使用原则：
 * - DTO 仅承载数据，不包含业务逻辑；
 * - 通过 Jackson 注解（如 @JsonProperty）来对接后端字段命名；
 * - @JsonIgnoreProperties(ignoreUnknown = true) 允许服务端新增字段而不影响反序列化，增强兼容性。
 */
public class RagflowDtos {

    /**
     * RagFlow API 请求对象。
     *
     * 典型字段：
     * - question：用户问题；
     * - stream：是否流式返回；
     * - session_id、user_id：可选的上下文字段，便于多轮会话与用户追踪。
     */
    @Data
    public static class RagflowChatRequest {
        /**
         * 用户问题
         */
        private String question;
        
        /**
         * 是否使用流式响应
         */
        private Boolean stream = true;
        
        /**
         * 会话ID（可选）
         */
        @JsonProperty("session_id")
        private String sessionId;
        
        /**
         * 用户ID（可选）
         */
        @JsonProperty("user_id")
        private String userId;
    }

    /**
     * RagFlow API 响应对象。
     *
     * 约定：code=0 代表成功，非 0 代表失败；
     * data 内部包含真实回答、引用与标识信息等。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RagflowApiResponse {
        /**
         * 状态码：0表示成功
         */
        private Integer code;
        
        /**
         * 错误消息（如果有）
         */
        private String message;
        
        /**
         * 响应数据
         */
        private ResponseData data;
        
        /**
         * 响应数据内部类
         */
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ResponseData {
            /**
             * AI回答内容
             */
            private String answer;
            
            /**
             * 引用信息：包含来源文档、片段、元数据等（不同实现结构可能不同）
             */
            private Map<String, Object> reference;
            
            /**
             * 音频二进制数据（如果有）
             */
            @JsonProperty("audio_binary")
            private String audioBinary;
            
            /**
             * 消息ID
             */
            private String id;
            
            /**
             * 会话ID
             */
            @JsonProperty("session_id")
            private String sessionId;
        }
    }
    
    /**
     * 流式响应的最后一条消息标记（部分实现会在流结束时发送一个“完成标记”）。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RagflowStreamEndMarker {
        private Integer code;
        private Boolean data;
    }
}
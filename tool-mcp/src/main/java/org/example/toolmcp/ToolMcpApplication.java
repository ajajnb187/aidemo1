package org.example.toolmcp;

import org.example.toolmcp.tool.CalculatorTool;
import org.example.toolmcp.tool.ImageSearchTool;
import org.example.toolmcp.tool.WeatherTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
//@EnableMcpService
public class ToolMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToolMcpApplication.class, args);
	}
	@Bean
	public ToolCallbackProvider calculatorTools(CalculatorTool calculatorTool,
												ImageSearchTool imageSearchTool,
												WeatherTool weatherTool){
		return MethodToolCallbackProvider.builder()
				.toolObjects(calculatorTool,
						imageSearchTool,
						weatherTool)
				.build();
	}


}

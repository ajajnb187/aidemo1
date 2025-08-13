package org.example.toolmcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTool {

    /**
     * 加法运算工具方法
     * 被@Tool注解标记，使其能被AI模型调用
     * description提供了对该工具的描述，AI会根据这个描述决定何时使用此工具
     *
     * @param a 第一个数，被@ToolParam标记，提供参数描述
     * @param b 第二个数，被@ToolParam标记，提供参数描述
     * @return 两数之和
     */
    @Tool(description = "将两个数字相加并返回和")
    public double add(
            @ToolParam(description = "第一个数") double a,
            @ToolParam(description = "第二个数") double b) {
        System.out.println("add: " + a + " + " + b);
        return a + b;
    }

    /**
     * 减法运算工具方法
     *
     * @param a 被减数
     * @param b 减数
     * @return 差值
     */
    @Tool(description = "用第一个数减去第二个数并返回差值")
    public double subtract(
            @ToolParam(description = "被减数") double a,
            @ToolParam(description = "减数") double b) {
        return a - b;
    }

    /**
     * 乘法运算工具方法
     *
     * @param a 第一个因数
     * @param b 第二个因数
     * @return 乘积
     */
    @Tool(description = "将两个数相乘并返回乘积")
    public double multiply(
            @ToolParam(description = "第一个因数") double a,
            @ToolParam(description = "第二个因数") double b) {
        return a * b;
    }

    /**
     * 除法运算工具方法
     *
     * @param a 被除数
     * @param b 除数
     * @return 商
     */
    @Tool(description = "用第一个数除以第二个数并返回商")
    public double divide(
            @ToolParam(description = "被除数") double a,
            @ToolParam(description = "除数") double b) {
        if (b == 0) {
            throw new ArithmeticException("不能除以零");
        }
        return a / b;
    }
}
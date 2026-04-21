package io.metersphere.workstation.service;

import io.metersphere.commons.exception.MSException;
import io.metersphere.workstation.dto.JQLValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JQL 语法解析器
 *
 * 将类似 Jira JQL 的查询语句解析为抽象语法树（AST）
 * 支持的操作符：=, !=, ~, IN, NOT IN, >, >=, <, <=, CONTAINS, AND, OR
 *
 * @author MeterSphere
 */
@Service
public class JQLParser {

    /**
     * 预编译的JQL正则表达式Pattern
     * 避免每次调用tokenize时重新编译
     */
    private static final Pattern JQL_PATTERN;

    /**
     * 预编译的日期Pattern
     */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    static {
        JQL_PATTERN = Pattern.compile(
            "(?i)\\b(AND|OR|IN|NOT\\s+IN|CONTAINS)\\b|" +
            "([><=!~]+)|" +
            "\"([^\"]*)\"|" +
            "'([^']*)'|" +
            "\\(|\\)|,|" +
            "(\\d{4}-\\d{2}-\\d{2})|" +
            "(\\w+)|" +
            "\\s+"
        );
    }
    
    // 操作符映射
    private static final Map<String, TokenType> OPERATORS = new HashMap<>();
    
    static {
        OPERATORS.put("=", TokenType.EQUALS);
        OPERATORS.put("!=", TokenType.NOT_EQUALS);
        OPERATORS.put("~", TokenType.LIKE);
        OPERATORS.put("IN", TokenType.IN);
        OPERATORS.put("NOT IN", TokenType.NOT_IN);
        OPERATORS.put(">=", TokenType.GREATER_EQUAL);
        OPERATORS.put("<=", TokenType.LESS_EQUAL);
        OPERATORS.put(">", TokenType.GREATER);
        OPERATORS.put("<", TokenType.LESS);
        OPERATORS.put("AND", TokenType.AND);
        OPERATORS.put("OR", TokenType.OR);
        OPERATORS.put("CONTAINS", TokenType.CONTAINS);
    }
    
    /**
     * 解析 JQL 查询语句
     * 
     * 我在做：将 JQL 字符串解析为抽象语法树
     * 目的是：为后续转换为 SQL 做准备
     * 如果不这样做：无法将 JQL 转换为可执行的 SQL
     * 
     * @param jql JQL 查询语句
     * @return 抽象语法树（AST）
     */
    public QueryNode parseJQL(String jql) {
        if (StringUtils.isBlank(jql)) {
            MSException.throwException("JQL 查询语句不能为空");
        }
        
        // 1. 词法分析：将 JQL 字符串分解为 Token 序列
        List<Token> tokens = tokenize(jql);
        if (tokens.isEmpty()) {
            MSException.throwException("JQL 查询语句为空");
        }
        
        // 2. 语法分析：根据语法规则构建 AST
        ParseContext context = new ParseContext(tokens);
        return parseExpression(context);
    }
    
    /**
     * 验证 JQL 语法
     * 
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @return 验证结果
     */
    public JQLValidationResult validateJQL(String jql, String module) {
        if (StringUtils.isBlank(jql)) {
            return JQLValidationResult.error("JQL 查询语句不能为空", 1, 1);
        }
        
        try {
            // 尝试解析 JQL
            parseJQL(jql);
            return JQLValidationResult.success();
        } catch (Exception e) {
            return JQLValidationResult.error(e.getMessage(), 1, 1);
        }
    }
    
    /**
     * 词法分析
     *
     * 我在做：将 JQL 字符串分解为 Token 序列
     * 目的是：为语法分析提供输入
     * 如果不这样做：无法进行语法分析
     *
     * @param jql JQL 查询语句
     * @return Token 列表
     */
    private List<Token> tokenize(String jql) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = JQL_PATTERN.matcher(jql);
        int position = 0;

        while (matcher.find()) {
            String match = matcher.group().trim();
            if (match.isEmpty()) continue;

            TokenType type = determineTokenType(match);
            tokens.add(new Token(type, match, position));
            position = matcher.end();
        }

        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }
    
    /**
     * 确定 Token 类型
     */
    private TokenType determineTokenType(String text) {
        // 检查是否为操作符或关键字
        String upper = text.toUpperCase();
        if (OPERATORS.containsKey(upper)) {
            return OPERATORS.get(upper);
        }
        
        // 检查是否为字符串字面量
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'"))) {
            return TokenType.STRING;
        }
        
        // 检查是否为日期
        if (DATE_PATTERN.matcher(text).matches()) {
            return TokenType.DATE;
        }
        
        // 检查是否为括号或逗号
        if (text.equals("(")) return TokenType.LPAREN;
        if (text.equals(")")) return TokenType.RPAREN;
        if (text.equals(",")) return TokenType.COMMA;
        
        // 默认为标识符
        return TokenType.IDENTIFIER;
    }
    
    /**
     * 解析表达式（处理 AND/OR 优先级）
     */
    private QueryNode parseExpression(ParseContext context) {
        QueryNode left = parseComparison(context);
        
        while (context.hasNext()) {
            Token token = context.peek();
            if (token.getType() == TokenType.AND || token.getType() == TokenType.OR) {
                context.consume();
                QueryNode right = parseComparison(context);
                left = new BinaryOpNode(token.getType(), left, right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    /**
     * 解析比较表达式
     */
    private QueryNode parseComparison(ParseContext context) {
        // 处理括号分组
        if (context.peek().getType() == TokenType.LPAREN) {
            context.consume();
            QueryNode node = parseExpression(context);
            if (context.peek().getType() != TokenType.RPAREN) {
                MSException.throwException("缺少右括号");
            }
            context.consume();
            return node;
        }
        
        // 解析字段名
        Token fieldToken = context.consume();
        if (fieldToken.getType() != TokenType.IDENTIFIER) {
            MSException.throwException("期望字段名，但得到: " + fieldToken.getValue());
        }
        
        String fieldName = fieldToken.getValue();
        
        // 解析操作符
        Token operatorToken = context.consume();
        TokenType operator = operatorToken.getType();
        
        // 解析值
        Object value = parseValue(context, operator);
        
        return new ComparisonNode(fieldName, operator, value);
    }
    
    /**
     * 解析值
     */
    private Object parseValue(ParseContext context, TokenType operator) {
        if (operator == TokenType.IN || operator == TokenType.NOT_IN) {
            return parseValueList(context);
        } else {
            Token valueToken = context.consume();
            return convertValue(valueToken);
        }
    }
    
    /**
     * 解析值列表（用于 IN 操作符）
     */
    private List<Object> parseValueList(ParseContext context) {
        if (context.peek().getType() != TokenType.LPAREN) {
            MSException.throwException("IN 操作符后期望左括号");
        }
        context.consume();
        
        List<Object> values = new ArrayList<>();
        while (context.hasNext() && context.peek().getType() != TokenType.RPAREN) {
            Token valueToken = context.consume();
            values.add(convertValue(valueToken));
            
            if (context.hasNext() && context.peek().getType() == TokenType.COMMA) {
                context.consume();
            }
        }
        
        if (!context.hasNext() || context.peek().getType() != TokenType.RPAREN) {
            MSException.throwException("IN 操作符缺少右括号");
        }
        context.consume();
        
        return values;
    }
    
    /**
     * 转换 Token 值为 Java 对象
     */
    private Object convertValue(Token token) {
        switch (token.getType()) {
            case STRING:
                String str = token.getValue();
                return str.substring(1, str.length() - 1);
            case DATE:
                return token.getValue();
            case IDENTIFIER:
                try {
                    return Integer.parseInt(token.getValue());
                } catch (NumberFormatException e) {
                    return token.getValue();
                }
            default:
                return token.getValue();
        }
    }
    
    /**
     * Token 类（词法单元）
     */
    public static class Token {
        private TokenType type;
        private String value;
        private int position;
        
        public Token(TokenType type, String value, int position) {
            this.type = type;
            this.value = value;
            this.position = position;
        }
        
        public TokenType getType() { return type; }
        public String getValue() { return value; }
        public int getPosition() { return position; }
    }
    
    /**
     * 解析上下文
     */
    private static class ParseContext {
        private List<Token> tokens;
        private int position = 0;
        
        public ParseContext(List<Token> tokens) {
            this.tokens = tokens;
        }
        
        public boolean hasNext() {
            return position < tokens.size() && tokens.get(position).getType() != TokenType.EOF;
        }
        
        public Token peek() {
            return position < tokens.size() ? tokens.get(position) : tokens.get(tokens.size() - 1);
        }
        
        public Token consume() {
            return tokens.get(position++);
        }
    }
    
    /**
     * Token 类型枚举
     */
    public enum TokenType {
        EQUALS, NOT_EQUALS, LIKE, IN, NOT_IN,
        GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
        CONTAINS, AND, OR,
        IDENTIFIER, STRING, DATE, NUMBER,
        LPAREN, RPAREN, COMMA, EOF
    }
    
    /**
     * AST 节点接口
     */
    public interface QueryNode {
    }
    
    /**
     * 比较节点
     */
    public static class ComparisonNode implements QueryNode {
        private String field;
        private TokenType operator;
        private Object value;
        
        public ComparisonNode(String field, TokenType operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
        
        public String getField() { return field; }
        public TokenType getOperator() { return operator; }
        public Object getValue() { return value; }
    }
    
    /**
     * 二元操作节点
     */
    public static class BinaryOpNode implements QueryNode {
        private TokenType operator;
        private QueryNode left;
        private QueryNode right;
        
        public BinaryOpNode(TokenType operator, QueryNode left, QueryNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }
        
        public TokenType getOperator() { return operator; }
        public QueryNode getLeft() { return left; }
        public QueryNode getRight() { return right; }
    }
}

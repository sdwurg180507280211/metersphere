# JQL技术实现方案

## 概述

本文档详细描述了在MeterSphere高级搜索功能中实现JQL（Jira Query Language）查询语法的技术方案。JQL为用户提供了一种类似自然语言的查询方式，支持复杂的逻辑组合和条件表达。

## JQL语法设计

### 基础语法

```sql
-- 基础比较
project = "电商平台"
status = "Pass"
priority != "P0"

-- 模糊匹配
name ~ "登录"
description CONTAINS "功能测试"

-- 列表匹配
status IN ("Pass", "Prepare")
priority NOT IN ("P3", "P4")

-- 数值比较
createTime >= "2024-01-01"
updateTime <= "2024-12-31"

-- 逻辑组合
project = "电商平台" AND status = "Pass"
priority = "P0" OR priority = "P1"

-- 复杂条件
(priority = "P0" OR priority = "P1") AND status != "Deprecated"
```

### 支持的操作符

| 操作符 | 说明 | 适用字段类型 | 示例 |
|--------|------|-------------|------|
| `=` | 等于 | 所有类型 | `status = "Pass"` |
| `!=` | 不等于 | 所有类型 | `priority != "P0"` |
| `~` | 模糊匹配 | 文本类型 | `name ~ "登录"` |
| `IN` | 包含于列表 | 所有类型 | `status IN ("Pass", "Prepare")` |
| `NOT IN` | 不包含于列表 | 所有类型 | `priority NOT IN ("P3", "P4")` |
| `>` | 大于 | 数值、日期 | `createTime > "2024-01-01"` |
| `>=` | 大于等于 | 数值、日期 | `createTime >= "2024-01-01"` |
| `<` | 小于 | 数值、日期 | `updateTime < "2024-12-31"` |
| `<=` | 小于等于 | 数值、日期 | `updateTime <= "2024-12-31"` |
| `CONTAINS` | 包含文本 | 文本类型 | `description CONTAINS "测试"` |
| `AND` | 逻辑与 | - | `status = "Pass" AND priority = "P0"` |
| `OR` | 逻辑或 | - | `assignee = "张三" OR creator = "李四"` |

## 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    JQL处理流程                                    │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ JQL Input   │  │ JQL Parser  │  │ AST Builder │              │
│  │ 用户输入    │  │ 词法分析    │  │ 语法树构建  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│         │               │                │                       │
│         ▼               ▼                ▼                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ Validation  │  │ SQL Builder │  │ Cache Layer │              │
│  │ 语法验证    │  │ SQL生成     │  │ 缓存层      │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│         │               │                │                       │
│         └───────────────┼────────────────┘                       │
│                         ▼                                        │
│              ┌─────────────────────┐                             │
│              │   MyBatis Mapper    │                             │
│              │   SQL执行           │                             │
│              └─────────────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
```

## 核心组件实现

### 1. JQL词法分析器 (JQLLexer)

```java
/**
 * JQL词法分析器
 * 将JQL字符串解析为Token流
 */
@Component
public class JQLLexer {
    
    // 操作符定义
    private static final Map<String, TokenType> OPERATORS = Map.of(
        "=", TokenType.EQUALS,
        "!=", TokenType.NOT_EQUALS,
        "~", TokenType.LIKE,
        "IN", TokenType.IN,
        "NOT IN", TokenType.NOT_IN,
        ">=", TokenType.GREATER_EQUAL,
        "<=", TokenType.LESS_EQUAL,
        ">", TokenType.GREATER,
        "<", TokenType.LESS,
        "AND", TokenType.AND,
        "OR", TokenType.OR,
        "CONTAINS", TokenType.CONTAINS
    );
    
    // 保留字定义
    private static final Set<String> RESERVED_WORDS = Set.of(
        "AND", "OR", "IN", "NOT", "CONTAINS", "NULL", "EMPTY"
    );
    
    /**
     * 解析JQL字符串为Token列表
     */
    public List<Token> tokenize(String jql) {
        List<Token> tokens = new ArrayList<>();
        
        // 使用正则表达式进行词法分析
        Pattern pattern = Pattern.compile(
            "(?i)\\b(AND|OR|IN|NOT\\s+IN|CONTAINS)\\b|" +  // 操作符
            "([><=!~]+)|" +                                 // 比较符
            "\"([^\"]*)\"|" +                              // 字符串字面量
            "'([^']*)'|" +                                 // 字符串字面量
            "\\((.*?)\\)|" +                               // 括号分组
            "(\\d{4}-\\d{2}-\\d{2})|" +                   // 日期字面量
            "(\\w+)|" +                                    // 标识符
            "\\s+"                                         // 空白字符
        );
        
        Matcher matcher = pattern.matcher(jql);
        while (matcher.find()) {
            String match = matcher.group().trim();
            if (match.isEmpty()) continue;
            
            TokenType type = determineTokenType(match);
            tokens.add(new Token(type, match, matcher.start()));
        }
        
        return tokens;
    }
    
    /**
     * 确定Token类型
     */
    private TokenType determineTokenType(String text) {
        // 检查是否为操作符
        if (OPERATORS.containsKey(text.toUpperCase())) {
            return OPERATORS.get(text.toUpperCase());
        }
        
        // 检查是否为字符串字面量
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'"))) {
            return TokenType.STRING;
        }
        
        // 检查是否为日期字面量
        if (text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return TokenType.DATE;
        }
        
        // 检查是否为括号
        if (text.equals("(")) return TokenType.LEFT_PAREN;
        if (text.equals(")")) return TokenType.RIGHT_PAREN;
        
        // 默认为标识符
        return TokenType.IDENTIFIER;
    }
}
```

### 2. JQL语法解析器 (JQLParser)

```java
/**
 * JQL语法解析器
 * 将Token流解析为抽象语法树(AST)
 */
@Component
public class JQLParser {
    
    @Resource
    private JQLLexer jqlLexer;
    
    @Resource
    private FieldMetadataService fieldMetadataService;
    
    /**
     * 解析JQL为抽象语法树
     */
    public QueryNode parseJQL(String jql) throws JQLParseException {
        List<Token> tokens = jqlLexer.tokenize(jql);
        if (tokens.isEmpty()) {
            throw new JQLParseException("空的JQL表达式");
        }
        
        ParseContext context = new ParseContext(tokens);
        return parseExpression(context);
    }
    
    /**
     * 解析表达式（处理AND/OR优先级）
     */
    private QueryNode parseExpression(ParseContext context) throws JQLParseException {
        QueryNode left = parseComparison(context);
        
        while (context.hasNext()) {
            Token token = context.peek();
            if (token.getType() == TokenType.AND || token.getType() == TokenType.OR) {
                context.consume(); // 消费操作符
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
    private QueryNode parseComparison(ParseContext context) throws JQLParseException {
        // 处理括号分组
        if (context.peek().getType() == TokenType.LEFT_PAREN) {
            context.consume(); // 消费 (
            QueryNode node = parseExpression(context);
            if (context.peek().getType() != TokenType.RIGHT_PAREN) {
                throw new JQLParseException("缺少右括号");
            }
            context.consume(); // 消费 )
            return node;
        }
        
        // 解析字段名
        Token fieldToken = context.consume();
        if (fieldToken.getType() != TokenType.IDENTIFIER) {
            throw new JQLParseException("期望字段名，但得到: " + fieldToken.getValue());
        }
        
        String fieldName = fieldToken.getValue();
        validateField(fieldName);
        
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
    private Object parseValue(ParseContext context, TokenType operator) throws JQLParseException {
        if (operator == TokenType.IN || operator == TokenType.NOT_IN) {
            return parseValueList(context);
        } else {
            Token valueToken = context.consume();
            return convertValue(valueToken);
        }
    }
    
    /**
     * 解析值列表 (用于IN操作符)
     */
    private List<Object> parseValueList(ParseContext context) throws JQLParseException {
        if (context.peek().getType() != TokenType.LEFT_PAREN) {
            throw new JQLParseException("IN操作符后期望左括号");
        }
        context.consume(); // 消费 (
        
        List<Object> values = new ArrayList<>();
        while (context.hasNext() && context.peek().getType() != TokenType.RIGHT_PAREN) {
            Token valueToken = context.consume();
            values.add(convertValue(valueToken));
            
            // 检查是否有逗号分隔符
            if (context.hasNext() && context.peek().getType() == TokenType.COMMA) {
                context.consume(); // 消费逗号
            }
        }
        
        if (!context.hasNext() || context.peek().getType() != TokenType.RIGHT_PAREN) {
            throw new JQLParseException("IN操作符缺少右括号");
        }
        context.consume(); // 消费 )
        
        return values;
    }
    
    /**
     * 转换Token值为Java对象
     */
    private Object convertValue(Token token) {
        switch (token.getType()) {
            case STRING:
                // 去除引号
                String str = token.getValue();
                return str.substring(1, str.length() - 1);
            case DATE:
                return parseDate(token.getValue());
            case IDENTIFIER:
                // 可能是数字或其他标识符
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
     * 字段名白名单验证
     */
    private void validateField(String fieldName) throws JQLParseException {
        if (!fieldMetadataService.isValidField(fieldName)) {
            throw new JQLParseException("无效的字段名: " + fieldName);
        }
    }
    
    /**
     * 日期解析
     */
    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            throw new JQLParseException("无效的日期格式: " + dateStr);
        }
    }
}
```

### 3. SQL转换器 (JQLToSQLConverter)

```java
/**
 * JQL到SQL的转换器
 * 将AST转换为MyBatis动态SQL
 */
@Component
public class JQLToSQLConverter {
    
    @Resource
    private FieldMetadataService fieldMetadataService;
    
    private final AtomicInteger paramCounter = new AtomicInteger(0);
    private final Map<String, Object> parameters = new HashMap<>();
    
    /**
     * 将JQL AST转换为SQL WHERE子句
     */
    public SQLResult convertToSQL(QueryNode ast, String module) {
        paramCounter.set(0);
        parameters.clear();
        
        StringBuilder sql = new StringBuilder();
        
        // 添加基础表查询
        sql.append(getBaseQuery(module));
        
        // 转换WHERE条件
        if (ast != null) {
            sql.append(" WHERE ");
            sql.append(convertNode(ast));
        }
        
        return new SQLResult(sql.toString(), parameters);
    }
    
    /**
     * 递归转换AST节点
     */
    private String convertNode(QueryNode node) {
        if (node instanceof ComparisonNode) {
            return convertComparison((ComparisonNode) node);
        } else if (node instanceof BinaryOpNode) {
            return convertBinaryOp((BinaryOpNode) node);
        }
        throw new IllegalArgumentException("未知的节点类型: " + node.getClass());
    }
    
    /**
     * 转换比较节点
     */
    private String convertComparison(ComparisonNode node) {
        String field = mapFieldToColumn(node.getField());
        TokenType operator = node.getOperator();
        Object value = node.getValue();
        
        switch (operator) {
            case EQUALS:
                String paramName1 = generateParamName();
                parameters.put(paramName1, value);
                return String.format("%s = #{%s}", field, paramName1);
                
            case NOT_EQUALS:
                String paramName2 = generateParamName();
                parameters.put(paramName2, value);
                return String.format("%s != #{%s}", field, paramName2);
                
            case LIKE:
                String paramName3 = generateParamName();
                parameters.put(paramName3, "%" + value + "%");
                return String.format("%s LIKE #{%s}", field, paramName3);
                
            case CONTAINS:
                String paramName4 = generateParamName();
                parameters.put(paramName4, "%" + value + "%");
                return String.format("%s LIKE #{%s}", field, paramName4);
                
            case IN:
                String paramName5 = generateParamName();
                parameters.put(paramName5, value);
                return String.format("%s IN <foreach collection='%s' item='item' open='(' separator=',' close=')'>#{item}</foreach>", 
                    field, paramName5);
                
            case NOT_IN:
                String paramName6 = generateParamName();
                parameters.put(paramName6, value);
                return String.format("%s NOT IN <foreach collection='%s' item='item' open='(' separator=',' close=')'>#{item}</foreach>", 
                    field, paramName6);
                
            case GREATER:
                String paramName7 = generateParamName();
                parameters.put(paramName7, value);
                return String.format("%s > #{%s}", field, paramName7);
                
            case GREATER_EQUAL:
                String paramName8 = generateParamName();
                parameters.put(paramName8, value);
                return String.format("%s >= #{%s}", field, paramName8);
                
            case LESS:
                String paramName9 = generateParamName();
                parameters.put(paramName9, value);
                return String.format("%s < #{%s}", field, paramName9);
                
            case LESS_EQUAL:
                String paramName10 = generateParamName();
                parameters.put(paramName10, value);
                return String.format("%s <= #{%s}", field, paramName10);
                
            default:
                throw new IllegalArgumentException("不支持的操作符: " + operator);
        }
    }
    
    /**
     * 转换二元操作节点
     */
    private String convertBinaryOp(BinaryOpNode node) {
        String left = convertNode(node.getLeft());
        String right = convertNode(node.getRight());
        String operator = node.getOperator() == TokenType.AND ? "AND" : "OR";
        
        return String.format("(%s %s %s)", left, operator, right);
    }
    
    /**
     * 字段名映射到数据库列名
     */
    private String mapFieldToColumn(String field) {
        return fieldMetadataService.getColumnName(field);
    }
    
    /**
     * 生成参数名
     */
    private String generateParamName() {
        return "param" + paramCounter.incrementAndGet();
    }
    
    /**
     * 获取基础查询SQL
     */
    private String getBaseQuery(String module) {
        switch (module) {
            case "TEST_CASE":
                return "SELECT * FROM test_case tc LEFT JOIN project p ON tc.project_id = p.id";
            case "ISSUE":
                return "SELECT * FROM issues i LEFT JOIN project p ON i.project_id = p.id";
            case "TEST_PLAN":
                return "SELECT * FROM test_plan tp LEFT JOIN project p ON tp.project_id = p.id";
            case "TEST_CASE_REVIEW":
                return "SELECT * FROM test_case_review tcr LEFT JOIN project p ON tcr.project_id = p.id";
            default:
                throw new IllegalArgumentException("不支持的模块: " + module);
        }
    }
}
```

### 4. JQL缓存服务 (JQLCacheService)

```java
/**
 * JQL缓存服务
 * 缓存解析后的AST以提高性能
 */
@Service
public class JQLCacheService {
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String JQL_AST_CACHE_KEY = "jql:ast:{}";
    private static final String JQL_SQL_CACHE_KEY = "jql:sql:{}:{}";
    private static final int CACHE_EXPIRE_HOURS = 2;
    
    /**
     * 缓存解析后的AST
     */
    public void cacheAST(String jql, QueryNode ast) {
        String key = StrUtil.format(JQL_AST_CACHE_KEY, DigestUtils.md5Hex(jql));
        String astJson = JSON.toJSONString(ast);
        redisTemplate.opsForValue().set(key, astJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }
    
    /**
     * 获取缓存的AST
     */
    public QueryNode getCachedAST(String jql) {
        String key = StrUtil.format(JQL_AST_CACHE_KEY, DigestUtils.md5Hex(jql));
        String cached = redisTemplate.opsForValue().get(key);
        return StringUtils.isNotBlank(cached) ? 
            JSON.parseObject(cached, QueryNode.class) : null;
    }
    
    /**
     * 缓存转换后的SQL
     */
    public void cacheSQL(String jql, String module, SQLResult sqlResult) {
        String key = StrUtil.format(JQL_SQL_CACHE_KEY, DigestUtils.md5Hex(jql), module);
        String sqlJson = JSON.toJSONString(sqlResult);
        redisTemplate.opsForValue().set(key, sqlJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }
    
    /**
     * 获取缓存的SQL
     */
    public SQLResult getCachedSQL(String jql, String module) {
        String key = StrUtil.format(JQL_SQL_CACHE_KEY, DigestUtils.md5Hex(jql), module);
        String cached = redisTemplate.opsForValue().get(key);
        return StringUtils.isNotBlank(cached) ? 
            JSON.parseObject(cached, SQLResult.class) : null;
    }
    
    /**
     * 清除JQL缓存
     */
    public void clearJQLCache(String jql) {
        String astKey = StrUtil.format(JQL_AST_CACHE_KEY, DigestUtils.md5Hex(jql));
        redisTemplate.delete(astKey);
        
        // 清除所有模块的SQL缓存
        String[] modules = {"TEST_CASE", "ISSUE", "TEST_PLAN", "TEST_CASE_REVIEW"};
        for (String module : modules) {
            String sqlKey = StrUtil.format(JQL_SQL_CACHE_KEY, DigestUtils.md5Hex(jql), module);
            redisTemplate.delete(sqlKey);
        }
    }
}
```

## 前端JQL编辑器实现

### JQL编辑器组件

```vue
<template>
  <div class="jql-editor">
    <!-- JQL输入框 -->
    <div class="jql-input-container">
      <el-input
        ref="jqlInput"
        v-model="jqlQuery"
        type="textarea"
        :rows="3"
        placeholder="输入JQL查询，例如：project = '电商平台' AND status IN ('Pass', 'Prepare')"
        @input="onJQLInput"
        @keydown="onKeyDown"
        @focus="onFocus"
        @blur="onBlur"
      />
      
      <!-- 语法高亮覆盖层 -->
      <div class="syntax-highlight-overlay" v-html="highlightedJQL"></div>
    </div>
    
    <!-- 语法错误提示 -->
    <div v-if="syntaxError" class="syntax-error">
      <i class="el-icon-warning"></i>
      {{ syntaxError }}
    </div>
    
    <!-- 智能提示下拉框 -->
    <div v-if="showSuggestions" class="suggestions-dropdown" :style="suggestionStyle">
      <div
        v-for="(suggestion, index) in suggestions"
        :key="index"
        class="suggestion-item"
        :class="{ active: index === activeSuggestionIndex }"
        @click="applySuggestion(suggestion)"
        @mouseenter="activeSuggestionIndex = index"
      >
        <span class="suggestion-type" :class="suggestion.type">{{ suggestion.type }}</span>
        <span class="suggestion-value">{{ suggestion.value }}</span>
        <span class="suggestion-desc">{{ suggestion.description }}</span>
      </div>
    </div>
    
    <!-- 快捷操作栏 -->
    <div class="jql-actions">
      <el-button size="small" @click="showJQLBuilder">可视化构建器</el-button>
      <el-button size="small" @click="showJQLHelp">语法帮助</el-button>
      <el-button size="small" @click="formatJQL">格式化</el-button>
      <el-button type="primary" size="small" @click="executeQuery">执行查询</el-button>
    </div>
  </div>
</template>

<script>
import { debounce } from 'lodash';

export default {
  name: 'JQLEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    module: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      jqlQuery: this.value,
      syntaxError: '',
      showSuggestions: false,
      suggestions: [],
      activeSuggestionIndex: 0,
      suggestionStyle: {},
      cursorPosition: 0,
      highlightedJQL: ''
    };
  },
  watch: {
    value(newVal) {
      this.jqlQuery = newVal;
      this.highlightSyntax();
    },
    jqlQuery(newVal) {
      this.$emit('input', newVal);
    }
  },
  created() {
    // 防抖的语法验证
    this.debouncedValidate = debounce(this.validateJQL, 300);
    // 防抖的智能提示
    this.debouncedSuggestions = debounce(this.getSuggestions, 200);
  },
  mounted() {
    this.highlightSyntax();
  },
  methods: {
    /**
     * JQL输入事件处理
     */
    onJQLInput() {
      this.cursorPosition = this.$refs.jqlInput.$el.querySelector('textarea').selectionStart;
      this.highlightSyntax();
      this.debouncedValidate();
      this.debouncedSuggestions();
    },
    
    /**
     * 键盘事件处理
     */
    onKeyDown(event) {
      if (this.showSuggestions) {
        switch (event.key) {
          case 'ArrowDown':
            event.preventDefault();
            this.activeSuggestionIndex = Math.min(
              this.activeSuggestionIndex + 1,
              this.suggestions.length - 1
            );
            break;
          case 'ArrowUp':
            event.preventDefault();
            this.activeSuggestionIndex = Math.max(this.activeSuggestionIndex - 1, 0);
            break;
          case 'Enter':
          case 'Tab':
            event.preventDefault();
            if (this.suggestions[this.activeSuggestionIndex]) {
              this.applySuggestion(this.suggestions[this.activeSuggestionIndex]);
            }
            break;
          case 'Escape':
            this.showSuggestions = false;
            break;
        }
      }
    },
    
    /**
     * 焦点事件处理
     */
    onFocus() {
      this.getSuggestions();
    },
    
    /**
     * 失焦事件处理
     */
    onBlur() {
      // 延迟隐藏建议，允许点击建议项
      setTimeout(() => {
        this.showSuggestions = false;
      }, 200);
    },
    
    /**
     * 验证JQL语法
     */
    async validateJQL() {
      if (!this.jqlQuery.trim()) {
        this.syntaxError = '';
        return;
      }
      
      try {
        const result = await this.$api.validateJQL(this.jqlQuery, this.module);
        if (result.valid) {
          this.syntaxError = '';
        } else {
          this.syntaxError = result.message;
        }
      } catch (error) {
        this.syntaxError = error.message || 'JQL语法验证失败';
      }
    },
    
    /**
     * 获取智能提示
     */
    async getSuggestions() {
      if (!this.jqlQuery.trim()) {
        this.showSuggestions = false;
        return;
      }
      
      try {
        const context = this.jqlQuery.substring(0, this.cursorPosition);
        const result = await this.$api.getJQLSuggestions(context, this.module, this.cursorPosition);
        
        if (result && result.length > 0) {
          this.suggestions = result;
          this.activeSuggestionIndex = 0;
          this.showSuggestions = true;
          this.updateSuggestionPosition();
        } else {
          this.showSuggestions = false;
        }
      } catch (error) {
        console.error('获取JQL智能提示失败:', error);
        this.showSuggestions = false;
      }
    },
    
    /**
     * 应用建议
     */
    applySuggestion(suggestion) {
      const textarea = this.$refs.jqlInput.$el.querySelector('textarea');
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      
      // 找到当前单词的开始位置
      let wordStart = start;
      while (wordStart > 0 && /\w/.test(this.jqlQuery[wordStart - 1])) {
        wordStart--;
      }
      
      // 替换当前单词
      const before = this.jqlQuery.substring(0, wordStart);
      const after = this.jqlQuery.substring(end);
      this.jqlQuery = before + suggestion.insertText + after;
      
      // 设置光标位置
      this.$nextTick(() => {
        const newPosition = wordStart + suggestion.insertText.length;
        textarea.setSelectionRange(newPosition, newPosition);
        textarea.focus();
      });
      
      this.showSuggestions = false;
    },
    
    /**
     * 语法高亮
     */
    highlightSyntax() {
      let highlighted = this.jqlQuery;
      
      // 高亮关键字
      const keywords = ['AND', 'OR', 'IN', 'NOT IN', 'CONTAINS'];
      keywords.forEach(keyword => {
        const regex = new RegExp(`\\b${keyword}\\b`, 'gi');
        highlighted = highlighted.replace(regex, `<span class="jql-keyword">${keyword}</span>`);
      });
      
      // 高亮操作符
      const operators = ['=', '!=', '~', '>=', '<=', '>', '<'];
      operators.forEach(op => {
        const escapedOp = op.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const regex = new RegExp(escapedOp, 'g');
        highlighted = highlighted.replace(regex, `<span class="jql-operator">${op}</span>`);
      });
      
      // 高亮字符串
      highlighted = highlighted.replace(/"([^"]*)"/g, '<span class="jql-string">"$1"</span>');
      highlighted = highlighted.replace(/'([^']*)'/g, '<span class="jql-string">\'$1\'</span>');
      
      this.highlightedJQL = highlighted;
    },
    
    /**
     * 更新建议框位置
     */
    updateSuggestionPosition() {
      const textarea = this.$refs.jqlInput.$el.querySelector('textarea');
      const rect = textarea.getBoundingClientRect();
      
      // 计算光标位置（简化实现）
      this.suggestionStyle = {
        position: 'absolute',
        top: rect.bottom + 'px',
        left: rect.left + 'px',
        zIndex: 1000
      };
    },
    
    /**
     * 显示可视化构建器
     */
    showJQLBuilder() {
      this.$emit('show-builder');
    },
    
    /**
     * 显示语法帮助
     */
    showJQLHelp() {
      this.$emit('show-help');
    },
    
    /**
     * 格式化JQL
     */
    formatJQL() {
      // 简单的格式化实现
      let formatted = this.jqlQuery
        .replace(/\s+AND\s+/gi, '\n  AND ')
        .replace(/\s+OR\s+/gi, '\n  OR ')
        .trim();
      
      this.jqlQuery = formatted;
    },
    
    /**
     * 执行查询
     */
    executeQuery() {
      this.$emit('execute-query', this.jqlQuery);
    }
  }
};
</script>

<style scoped>
.jql-editor {
  position: relative;
}

.jql-input-container {
  position: relative;
}

.syntax-highlight-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  padding: 5px 15px;
  color: transparent;
  background: transparent;
  z-index: 1;
}

.syntax-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}

.suggestions-dropdown {
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
}

.suggestion-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.suggestion-item:hover,
.suggestion-item.active {
  background-color: #f5f7fa;
}

.suggestion-type {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  color: white;
  margin-right: 8px;
  min-width: 40px;
  text-align: center;
}

.suggestion-type.field { background-color: #409eff; }
.suggestion-type.operator { background-color: #67c23a; }
.suggestion-type.value { background-color: #e6a23c; }
.suggestion-type.function { background-color: #f56c6c; }

.suggestion-value {
  font-weight: bold;
  margin-right: 8px;
}

.suggestion-desc {
  color: #909399;
  font-size: 12px;
}

.jql-actions {
  margin-top: 10px;
  text-align: right;
}

/* 语法高亮样式 */
:deep(.jql-keyword) {
  color: #409eff;
  font-weight: bold;
}

:deep(.jql-operator) {
  color: #67c23a;
  font-weight: bold;
}

:deep(.jql-string) {
  color: #e6a23c;
}
</style>
```

## 性能优化策略

### 1. 缓存策略

- **AST缓存**：解析后的抽象语法树缓存2小时
- **SQL缓存**：转换后的SQL语句按模块分别缓存
- **字段元数据缓存**：字段定义和映射关系缓存

### 2. 解析优化

- **增量解析**：只重新解析变更的部分
- **并行验证**：语法验证和智能提示并行处理
- **预编译模式**：常用JQL模板预编译

### 3. 前端优化

- **防抖处理**：输入事件防抖300ms
- **虚拟滚动**：智能提示列表支持大量选项
- **语法高亮缓存**：高亮结果缓存避免重复计算

## 安全性考虑

### 1. SQL注入防护

- 所有参数使用MyBatis参数化查询
- 字段名白名单验证
- 操作符类型检查

### 2. 权限控制

- 字段访问权限验证
- 项目数据隔离
- 查询结果过滤

### 3. 资源限制

- JQL表达式长度限制（最大10KB）
- 条件数量限制（最大50个）
- 解析深度限制（最大20层嵌套）

## 测试策略

### 1. 单元测试

```java
@Test
public void testJQLParser() {
    String jql = "project = '电商平台' AND status IN ('Pass', 'Prepare')";
    QueryNode ast = jqlParser.parseJQL(jql);
    
    assertThat(ast).isInstanceOf(BinaryOpNode.class);
    BinaryOpNode binaryOp = (BinaryOpNode) ast;
    assertThat(binaryOp.getOperator()).isEqualTo(TokenType.AND);
}

@Test
public void testSQLConversion() {
    ComparisonNode node = new ComparisonNode("status", TokenType.EQUALS, "Pass");
    SQLResult result = sqlConverter.convertToSQL(node, "TEST_CASE");
    
    assertThat(result.getSql()).contains("status = #{param1}");
    assertThat(result.getParameters()).containsEntry("param1", "Pass");
}
```

### 2. 集成测试

- JQL端到端查询测试
- 复杂条件组合测试
- 性能压力测试
- 安全性渗透测试

## 总结

JQL技术方案为MeterSphere高级搜索提供了强大而灵活的查询能力，通过完整的词法分析、语法解析、SQL转换和缓存优化，实现了高性能、高安全性的查询体验。同时保持了与现有系统的良好兼容性，支持渐进式迁移和功能扩展。
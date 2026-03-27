<template>
  <div class="jql-editor-container">
    <!-- JQL 输入框 -->
    <div class="jql-input-wrapper">
      <el-input
        ref="jqlInput"
        v-model="localJql"
        type="textarea"
        :rows="3"
        :placeholder="$t('advanced_search.jql_placeholder')"
        @input="onInput"
        @keydown="onKeyDown"
        @blur="onBlur"
      />
      
      <!-- 智能提示下拉框 -->
      <div
        v-if="showSuggestions && suggestions.length > 0"
        class="suggestions-dropdown"
        :style="dropdownStyle"
      >
        <div
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="suggestion-item"
          :class="{ active: index === selectedIndex }"
          @click="selectSuggestion(suggestion)"
          @mouseenter="selectedIndex = index"
        >
          <span class="suggestion-type">{{ getSuggestionTypeLabel(suggestion.type) }}</span>
          <span class="suggestion-value">{{ suggestion.value }}</span>
          <span class="suggestion-desc">{{ suggestion.description }}</span>
        </div>
      </div>
    </div>
    
    <!-- 验证结果提示 -->
    <div v-if="validationResult && !validationResult.valid" class="validation-error">
      <i class="el-icon-warning"></i>
      <span>{{ validationResult.message }}</span>
    </div>
    
    <!-- 语法帮助 -->
    <div class="jql-help">
      <el-collapse>
        <el-collapse-item :title="$t('advanced_search.jql_help_title')" name="help">
          <div class="help-content">
            <h4>{{ $t('advanced_search.jql_operators') }}</h4>
            <ul>
              <li><code>=</code> - {{ $t('advanced_search.op_equals') }}</li>
              <li><code>!=</code> - {{ $t('advanced_search.op_not_equals') }}</li>
              <li><code>~</code> - {{ $t('advanced_search.op_like') }}</li>
              <li><code>&gt;</code>, <code>&gt;=</code>, <code>&lt;</code>, <code>&lt;=</code> - {{ $t('advanced_search.op_compare') }}</li>
              <li><code>IN</code> - {{ $t('advanced_search.op_in') }}</li>
              <li><code>NOT IN</code> - {{ $t('advanced_search.op_not_in') }}</li>
              <li><code>CONTAINS</code> - {{ $t('advanced_search.op_contains') }}</li>
            </ul>
            
            <h4>{{ $t('advanced_search.jql_examples') }}</h4>
            <ul>
              <li><code>status = "Pass" AND priority = "P0"</code></li>
              <li><code>status IN ("Pass", "Prepare") AND priority NOT IN ("P3", "P4")</code></li>
              <li><code>name ~ "登录" AND createTime &gt;= "2024-01-01"</code></li>
              <li><code>(priority = "P0" OR priority = "P1") AND status != "Deprecated"</code></li>
            </ul>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store/advancedSearch';
import { debounce } from 'lodash';

export default {
  name: 'JQLEditor',
  
  props: {
    value: {
      type: String,
      default: ''
    }
  },
  
  data() {
    return {
      localJql: this.value,
      suggestions: [],
      showSuggestions: false,
      selectedIndex: 0,
      validationResult: null,
      dropdownStyle: {}
    };
  },
  
  computed: {
    store() {
      return useAdvancedSearchStore();
    }
  },
  
  watch: {
    value(newVal) {
      this.localJql = newVal;
    }
  },
  
  methods: {
    /**
     * 输入事件处理
     */
    onInput: debounce(async function() {
      this.$emit('input', this.localJql);
      
      // 获取光标位置
      const textarea = this.$refs.jqlInput.$el.querySelector('textarea');
      const cursorPosition = textarea.selectionStart;
      
      // 获取智能提示
      await this.loadSuggestions(cursorPosition);
      
      // 验证 JQL 语法
      await this.validateJQL();
    }, 300),
    
    /**
     * 键盘事件处理
     */
    onKeyDown(event) {
      if (!this.showSuggestions || this.suggestions.length === 0) {
        return;
      }
      
      // 上箭头
      if (event.key === 'ArrowUp') {
        event.preventDefault();
        this.selectedIndex = Math.max(0, this.selectedIndex - 1);
      }
      // 下箭头
      else if (event.key === 'ArrowDown') {
        event.preventDefault();
        this.selectedIndex = Math.min(
          this.suggestions.length - 1,
          this.selectedIndex + 1
        );
      }
      // 回车或Tab
      else if (event.key === 'Enter' || event.key === 'Tab') {
        if (this.showSuggestions) {
          event.preventDefault();
          this.selectSuggestion(this.suggestions[this.selectedIndex]);
        }
      }
      // Esc
      else if (event.key === 'Escape') {
        this.showSuggestions = false;
      }
    },
    
    /**
     * 失焦事件处理
     */
    onBlur() {
      // 延迟隐藏，以便点击提示项
      setTimeout(() => {
        this.showSuggestions = false;
      }, 200);
    },
    
    /**
     * 加载智能提示
     */
    async loadSuggestions(cursorPosition) {
      try {
        this.suggestions = await this.store.getJQLSuggestions(
          this.localJql,
          cursorPosition
        );
        
        this.showSuggestions = this.suggestions.length > 0;
        this.selectedIndex = 0;
        
        // 计算下拉框位置
        this.calculateDropdownPosition();
      } catch (error) {
        console.error('加载智能提示失败:', error);
      }
    },
    
    /**
     * 选择提示项
     */
    selectSuggestion(suggestion) {
      const textarea = this.$refs.jqlInput.$el.querySelector('textarea');
      const cursorPosition = textarea.selectionStart;
      
      // 获取光标前后的文本
      const before = this.localJql.substring(0, cursorPosition);
      const after = this.localJql.substring(cursorPosition);
      
      // 找到最后一个词的开始位置
      const lastWordStart = before.lastIndexOf(' ') + 1;
      const beforeWord = before.substring(0, lastWordStart);
      
      // 插入提示文本
      this.localJql = beforeWord + suggestion.insertText + after;
      this.$emit('input', this.localJql);
      
      // 隐藏提示
      this.showSuggestions = false;
      
      // 设置光标位置
      this.$nextTick(() => {
        const newPosition = beforeWord.length + suggestion.insertText.length;
        textarea.setSelectionRange(newPosition, newPosition);
        textarea.focus();
      });
    },
    
    /**
     * 验证 JQL 语法
     */
    async validateJQL() {
      if (!this.localJql.trim()) {
        this.validationResult = null;
        return;
      }
      
      try {
        this.validationResult = await this.store.validateJQL(this.localJql);
      } catch (error) {
        console.error('JQL 验证失败:', error);
      }
    },
    
    /**
     * 计算下拉框位置
     */
    calculateDropdownPosition() {
      this.$nextTick(() => {
        const textarea = this.$refs.jqlInput.$el.querySelector('textarea');
        if (!textarea) return;
        
        const rect = textarea.getBoundingClientRect();
        this.dropdownStyle = {
          top: `${rect.height}px`,
          left: '0px',
          width: `${rect.width}px`
        };
      });
    },
    
    /**
     * 获取提示类型标签
     */
    getSuggestionTypeLabel(type) {
      const labels = {
        field: this.$t('advanced_search.suggestion_field'),
        operator: this.$t('advanced_search.suggestion_operator'),
        value: this.$t('advanced_search.suggestion_value'),
        keyword: this.$t('advanced_search.suggestion_keyword')
      };
      return labels[type] || type;
    }
  }
};
</script>

<style scoped lang="scss">
.jql-editor-container {
  position: relative;
}

.jql-input-wrapper {
  position: relative;
}

.suggestions-dropdown {
  position: absolute;
  z-index: 1000;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  max-height: 300px;
  overflow-y: auto;
}

.suggestion-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  
  &:hover,
  &.active {
    background-color: #f5f7fa;
  }
  
  .suggestion-type {
    padding: 2px 6px;
    background-color: #e4e7ed;
    border-radius: 3px;
    font-size: 12px;
    color: #606266;
    flex-shrink: 0;
  }
  
  .suggestion-value {
    font-weight: 500;
    color: #303133;
    flex-shrink: 0;
  }
  
  .suggestion-desc {
    color: #909399;
    font-size: 12px;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.validation-error {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  
  i {
    font-size: 16px;
  }
}

.jql-help {
  margin-top: 16px;
  
  .help-content {
    h4 {
      margin: 12px 0 8px;
      font-size: 14px;
      color: #303133;
      
      &:first-child {
        margin-top: 0;
      }
    }
    
    ul {
      margin: 0;
      padding-left: 20px;
      
      li {
        margin: 4px 0;
        font-size: 13px;
        color: #606266;
        
        code {
          padding: 2px 6px;
          background-color: #f5f7fa;
          border: 1px solid #e4e7ed;
          border-radius: 3px;
          font-family: 'Courier New', monospace;
          color: #e6a23c;
        }
      }
    }
  }
}
</style>

<template>
  <div class="jql-editor">
    <div class="jql-input-container">
      <el-input
        v-model="jqlQuery"
        type="textarea"
        :rows="3"
        :placeholder="$t('advanced_search.jql_placeholder')"
        @input="handleInput"
      />
    </div>
    
    <div v-if="syntaxError" class="syntax-error">
      <i class="el-icon-warning"></i>
      {{ syntaxError }}
    </div>
    
    <div class="jql-actions">
      <el-button size="small" @click="showHelp">
        {{ $t('advanced_search.syntax_help') }}
      </el-button>
      <el-button 
        type="primary" 
        size="small" 
        @click="handleExecute"
      >
        {{ $t('advanced_search.execute_query') }}
      </el-button>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';
import { validateJQL } from '@/api/advanced-search';
import { debounce } from 'lodash';

export default {
  name: 'JQLEditor',
  emits: ['execute-query'],
  setup() {
    const store = useAdvancedSearchStore();
    return { store };
  },
  data() {
    return {
      syntaxError: ''
    };
  },
  computed: {
    jqlQuery: {
      get() {
        return this.store.jqlQuery;
      },
      set(value) {
        this.store.jqlQuery = value;
      }
    }
  },
  created() {
    this.debouncedValidate = debounce(this.validateSyntax, 300);
  },
  methods: {
    handleInput() {
      this.debouncedValidate();
    },
    
    async validateSyntax() {
      if (!this.jqlQuery.trim()) {
        this.syntaxError = '';
        return;
      }
      
      try {
        const result = await validateJQL(this.jqlQuery, this.store.currentModule);
        if (result.data.valid) {
          this.syntaxError = '';
        } else {
          this.syntaxError = result.data.message;
        }
      } catch (error) {
        this.syntaxError = error.message;
      }
    },
    
    handleExecute() {
      if (this.syntaxError) {
        this.$warning(this.$t('advanced_search.fix_syntax_error'));
        return;
      }
      this.$emit('execute-query');
    },
    
    showHelp() {
      this.$alert(
        this.$t('advanced_search.jql_help_content'),
        this.$t('advanced_search.jql_help_title'),
        {
          confirmButtonText: this.$t('commons.confirm'),
          dangerouslyUseHTMLString: true
        }
      );
    }
  }
};
</script>

<style scoped>
.jql-editor {
  background-color: #fff;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.jql-input-container {
  margin-bottom: 12px;
}

.syntax-error {
  color: #f56c6c;
  font-size: 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.syntax-error i {
  margin-right: 4px;
}

.jql-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>

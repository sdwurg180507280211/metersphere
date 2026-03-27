<template>
  <div class="user-selector">
    <el-select
      v-model="selectedUsers"
      multiple
      filterable
      remote
      reserve-keyword
      :placeholder="$t('advanced_search.select_users')"
      :remote-method="searchUsers"
      :loading="loading"
      :multiple-limit="maxSelection"
      @change="handleChange"
    >
      <!-- 快捷选项：我自己 -->
      <el-option
        v-if="showCurrentUser"
        :key="currentUserId"
        :label="$t('advanced_search.current_user')"
        :value="currentUserId"
      >
        <div class="user-option">
          <img 
            v-if="currentUserAvatar" 
            :src="currentUserAvatar" 
            class="user-avatar"
          >
          <i v-else class="el-icon-user-solid user-avatar-icon"></i>
          <span class="user-name">{{ $t('advanced_search.current_user') }}</span>
        </div>
      </el-option>
      
      <!-- 用户列表 -->
      <el-option
        v-for="user in users"
        :key="user.id"
        :label="user.name"
        :value="user.id"
      >
        <div class="user-option">
          <img 
            v-if="user.avatar" 
            :src="user.avatar" 
            class="user-avatar"
          >
          <i v-else class="el-icon-user-solid user-avatar-icon"></i>
          <span class="user-name">{{ user.name }}</span>
          <span v-if="user.email" class="user-email">{{ user.email }}</span>
        </div>
      </el-option>
    </el-select>
  </div>
</template>

<script>
import { getUsers } from '@/api/advanced-search';
import { getCurrentUser } from 'metersphere-frontend/src/utils/token';

export default {
  name: 'UserSelector',
  
  props: {
    // v-model 绑定的值（用户ID数组）
    modelValue: {
      type: Array,
      default: () => []
    },
    
    // 工作空间ID列表（用于过滤用户）
    workspaceIds: {
      type: Array,
      default: () => []
    },
    
    // 最大选择数量
    maxSelection: {
      type: Number,
      default: 10
    },
    
    // 是否显示"我自己"快捷选项
    showCurrentUser: {
      type: Boolean,
      default: true
    }
  },
  
  emits: ['update:modelValue', 'change'],
  
  data() {
    return {
      users: [],
      loading: false,
      currentUserId: null,
      currentUserAvatar: null
    };
  },
  
  computed: {
    selectedUsers: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },
  
  mounted() {
    this.loadCurrentUser();
    this.loadUsers();
  },
  
  methods: {
    /**
     * 加载当前登录用户信息
     */
    async loadCurrentUser() {
      try {
        const user = getCurrentUser();
        if (user) {
          this.currentUserId = user.id;
          this.currentUserAvatar = user.avatar;
        }
      } catch (error) {
        console.error('Failed to load current user:', error);
      }
    },
    
    /**
     * 加载用户列表
     */
    async loadUsers(keyword = '') {
      if (this.workspaceIds.length === 0) {
        return;
      }
      
      this.loading = true;
      try {
        const response = await getUsers({
          workspaceIds: this.workspaceIds.join(','),
          keyword,
          pageNum: 1,
          pageSize: 50
        });
        
        if (response.data && response.data.listObject) {
          this.users = response.data.listObject;
        }
      } catch (error) {
        this.$error(this.$t('advanced_search.load_users_failed'));
        console.error('Failed to load users:', error);
      } finally {
        this.loading = false;
      }
    },
    
    /**
     * 搜索用户（远程搜索）
     */
    searchUsers(keyword) {
      if (keyword) {
        this.loadUsers(keyword);
      } else {
        this.loadUsers();
      }
    },
    
    /**
     * 选择变更事件
     */
    handleChange(value) {
      this.$emit('change', value);
    }
  },
  
  watch: {
    workspaceIds: {
      handler() {
        // 工作空间变更时重新加载用户列表
        this.loadUsers();
      },
      deep: true
    }
  }
};
</script>

<style scoped>
.user-selector {
  width: 100%;
}

.user-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.user-avatar-icon {
  font-size: 24px;
  color: #909399;
}

.user-name {
  font-weight: 500;
  color: #303133;
}

.user-email {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}
</style>

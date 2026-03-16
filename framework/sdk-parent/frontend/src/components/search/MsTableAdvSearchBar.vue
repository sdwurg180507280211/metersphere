<template>
  <span class="adv-search-bar">
    <el-link type="primary" @click="open" v-if="showLink">{{
        $t("commons.adv_search.title")
      }}</el-link>
    <el-dialog
        :title="$t('commons.adv_search.title')"
        :visible.sync="visible"
        custom-class="adv-dialog"
        :append-to-body="true"
    >
      <div class="search-items">
        <div
            class="search-item"
            v-for="component in optional.components"
            :key="component.key"
        >
          <el-row>
            <el-col :span="22">
              <component
                  :is="component.name"
                  :component="component"
                  :components.sync="config.components"
                  @updateKey="changeSearchItemKey"
                  :custom="condition.custom"
              />
            </el-col>
            <el-col :span="2">
              <i
                  class="el-icon-close delete-icon"
                  @click="remove(component)"
                  v-if="optional.components.length !== 1"
              ></i>
            </el-col>
          </el-row>
        </div>
        <el-link
            type="primary"
            icon="el-icon-plus"
            v-if="showAddFilterLink"
            class="add-filter-link"
            @click="addFilter"
        >{{ $t("commons.adv_search.add_filter_link") }}</el-link
        >
      </div>
      <template v-slot:footer>
        <div class="dialog-footer">
          <el-button @click="reset">{{
              $t("commons.adv_search.reset")
            }}</el-button>
          <el-button type="primary" @click="search">{{
              $t("commons.adv_search.search")
            }}</el-button>
        </div>
      </template>
    </el-dialog>
  </span>
</template>

<script>
import components from "./search-components";
import { BUILTIN_ADV_SEARCH_KEYS } from "./search-components";
import {cloneDeep, concat, slice} from "lodash-es";
import {_findByKey, _findIndexByKey} from "./custom-component";
// 导入用户身份工具函数，用于获取当前登录用户 ID
import {getCurrentUserId} from "../../utils/token";
// 导入高级搜索条件记忆相关的工具函数（保存、读取、清除）
import {saveAdvSearchCondition, getAdvSearchCondition, clearAdvSearchCondition} from "../../utils/tableUtils";

 // 高级搜索参数组装规则（非常关键）：
 // 1) 内置字段 key：由 search-components.js 统一维护（BUILTIN_ADV_SEARCH_KEYS），后端直接识别 combine[key]。
 // 2) 非内置字段：一律视为 custom_field 体系字段（包含 system=1 系统字段 + system=0 普通字段），必须走 combine.customs[]。
 // 3) 下面额外补充 request 级别的固定字段（并非 search-components.js 配置项，但确实属于内置参数）。
 const BUILTIN_ADV_SEARCH_KEY_SET = new Set([
   ...(Array.isArray(BUILTIN_ADV_SEARCH_KEYS) ? BUILTIN_ADV_SEARCH_KEYS : []),
   'workspaceId',
   'projectId',
   'creatorName',
   'operator',
   'operatorName',
 ]);

export default {
  components: {...components},
  name: "MsTableAdvSearchBar",
  props: {
    condition: Object,
    showLink: {
      type: Boolean,
      default: true,
    },
    showItemSize: {
      type: Number,
      default() {
        return 4; // 默认展示的搜索条件数量
      },
    },
    // 模块标识符，用于区分不同业务页面的搜索记忆上下文
    // 为空时不启用搜索记忆功能，保持原有行为
    moduleKey: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      visible: false,
      config: {
        components: [],
      },
      optional: {
        components: [],
      },
      showAddFilterLink: true,
      nullFilterKey: "",
      isInit: false,
    };
  },
  mounted() {
    // 注册当前组件为活跃的高级搜索组件
    this.registerAdvSearchComponent();
    // 监听全局Tab键，切换高级搜索显示状态
    document.addEventListener('keydown', this.handleTabKey);
  },
  beforeDestroy() {
    // 注销当前组件
    this.unregisterAdvSearchComponent();
    // 组件销毁时移除监听
    document.removeEventListener('keydown', this.handleTabKey);
  },
  methods: {
    registerAdvSearchComponent() {
      // 在window上注册当前活跃的高级搜索组件
      if (!window._activeAdvSearchComponents) {
        window._activeAdvSearchComponents = [];
      }
      window._activeAdvSearchComponents.push(this);
    },
    unregisterAdvSearchComponent() {
      // 从全局列表中移除当前组件
      if (window._activeAdvSearchComponents) {
        const index = window._activeAdvSearchComponents.indexOf(this);
        if (index > -1) {
          window._activeAdvSearchComponents.splice(index, 1);
        }
      }
    },
    handleTabKey(event) {
      // 按下Tab键时切换高级搜索显示状态
      if (event.key === 'Tab' && !event.shiftKey && !event.ctrlKey && !event.metaKey && !event.altKey) {
        // 检查当前焦点是否在输入框、文本域等元素上
        const activeElement = document.activeElement;
        const isInputFocused = activeElement && (
          activeElement.tagName === 'INPUT' ||
          activeElement.tagName === 'TEXTAREA' ||
          activeElement.isContentEditable
        );
        
        // 如果焦点在输入框上，不拦截Tab键（保持原有Tab切换焦点功能）
        if (isInputFocused) {
          return;
        }
        
        // 检查当前组件是否在DOM中可见
        if (!this.$el || !this.$el.offsetParent) {
          return;
        }
        
        // 只有第一个可见的组件响应Tab键（避免多个组件同时响应）
        if (window._activeAdvSearchComponents && window._activeAdvSearchComponents.length > 0) {
          const firstVisibleComponent = window._activeAdvSearchComponents.find(comp => 
            comp.$el && comp.$el.offsetParent
          );
          if (firstVisibleComponent !== this) {
            return;
          }
        }
        
        // 阻止默认Tab行为
        event.preventDefault();
        
        // 切换高级搜索显示状态
        if (this.visible) {
          this.visible = false;
        } else {
          this.open();
        }
      }
    },
    doInit(handleCustom) {
      let config = cloneDeep(this.condition);
      config.components.forEach((component) => {
        let operator = component.operator.value;
        component.operator.value =
            operator === undefined
                ? component.operator.options[0].value
                : operator;
      });
      if (!handleCustom) {
        return config;
      }
      if (this.condition.custom) {
        let components = [];
        this.systemFiled = config.components.filter(
            (co) => co.custom === undefined || co.custom === false
        );
        this.customFiled = config.components.filter((co) => co.custom === true);
        // 选项分组
        components.push({
          label: this.$t("custom_field.system_field"),
          child: this.systemFiled,
        });
        components.push({
          label: this.$t("custom_field.name"),
          child: this.customFiled,
        });
        this.$set(config, "components", components);
      }
      return config;
    },
    search() {
      let condition = {};
      this.optional.components.forEach((component) => {
        let value = component.value;
        if (Array.isArray(value)) {
          if (value.length > 0) {
            this.setCondition(condition, component);
          }
        } else {
          if (value !== undefined && value !== null && value !== "") {
            this.setCondition(condition, component);
          }
        }
      });

      // 清除name
      if (this.condition.name) this.condition.name = undefined;
      // 添加组合条件
      this.condition.combine = condition;
      this.$emit("update:condition", this.condition);
      this.$emit("search", condition);

      // 【搜索记忆】保存当前搜索条件到 localStorage
      // 仅当传入了 moduleKey 且能获取到有效用户 ID 时才执行保存
      if (this.moduleKey) {
        const userId = getCurrentUserId();
        if (userId) {
          saveAdvSearchCondition(userId, this.moduleKey, this.optional.components);
        }
      }

      this.visible = false;
    },
    setCondition(condition, component) {
      // 说明：
      // - 只要不是内置字段（白名单），就统一走 custom_field 过滤结构（condition.customs）。
      // - component.custom 只是“字段是否为普通自定义字段”的标识；即便 custom=false，只要它来自模板字段（system字段也属于此类），
      //   同样需要走 condition.customs，否则后端不会生效。
      const key = component && component.key ? String(component.key) : '';
      const isBuiltin = BUILTIN_ADV_SEARCH_KEY_SET.has(key);
      if (!isBuiltin) {
        this.handleCustomField(condition, component);
        return;
      }
      condition[key] = {
        operator: component.operator.value,
        value: component.value,
      };
    },
    handleCustomField(condition, component) {
      if (!condition.customs) {
        condition["customs"] = [];
      }
      let value = component.value;
      if (
          component.label === "用例状态" &&
          value.length === 1 &&
          value.indexOf("Trash") > -1
      ) {
        return;
      }
      if (
          component.type === "multipleMember" ||
          component.type === "checkbox" ||
          component.type === "multipleSelect"
      ) {
        try {
          value = JSON.stringify(component.value);
        } catch (e) {
          // nothing
        }
      }
      condition["customs"].push({
        id: component.key,
        operator: component.operator.value,
        value: value,
        type: component.type,
      });
    },
    reset() {
      let source = this.condition.components;
      this.optional.components.forEach((component, index) => {
        if (component.operator.value !== undefined) {
          let operator = _findByKey(source, component.key).operator.value;
          component.operator.value =
              operator === undefined
                  ? component.operator.options[0].value
                  : operator;
        }
        if (component.value !== undefined) {
          component.value = source[index].value;
        }
        if (component.reset && component.reset instanceof Function) {
          component.reset();
        }
      });
      this.condition.combine = undefined;
      this.$emit("update:condition", this.condition);
      this.$emit("search");

      // 【搜索记忆】重置时清除 localStorage 中保存的搜索条件
      if (this.moduleKey) {
        const userId = getCurrentUserId();
        if (userId) {
          clearAdvSearchCondition(userId, this.moduleKey);
        }
      }
    },
    init() {
      this.config = this.doInit(true);
      this.optional = this.doInit();
      if (
          this.optional.components.length &&
          this.optional.components.length <= this.showItemSize
      ) {
        this.showAddFilterLink = false;
      }
      // 默认显示几个搜索条件
      this.optional.components = slice(
          this.optional.components,
          0,
          this.showItemSize
      );

      // 【搜索记忆】从 localStorage 回填上次保存的搜索条件
      // 在 slice 截取默认显示条件之后、设置 disable 状态之前执行
      if (this.moduleKey) {
        const userId = getCurrentUserId();
        if (userId) {
          const saved = getAdvSearchCondition(userId, this.moduleKey);
          if (saved) {
            this._restoreSearchConditions(saved);
          }
        }
      }

      let allComponent = this.condition.custom
          ? concat(
              this.config.components[0].child,
              this.config.components[1].child
          )
          : this.config.components;
      for (let component of allComponent) {
        let co = _findByKey(this.optional.components, component.key);
        co
            ? this.$set(co, "disable", true)
            : this.$set(component, "disable", false);
      }
    },
    open() {
      this.visible = true;
      if (!this.isInit) {
        this.isInit = true;
        this.init();
      } else {
        this.setModulesParam();
        this.refreshComponentOption();
        this.handleCustomComponent();
      }
    },
    refreshComponentOption() {
      // 当前已存在的搜索子组件中是否有需要进行刷新数据选项的
      let comps = this.optional.components.filter(
          (cp) => cp.init && cp.init instanceof Function
      );
      comps.forEach((comp) => comp.init());
    },
    setModulesParam() {
      let comps = this.optional.components.filter(
          (c) => c.key === "moduleIds" && c.options.type === "POST"
      );
      comps.forEach(
          (comp) =>
              (comp.options.params = {projectId: this.condition.projectId})
      );
    },
    handleCustomComponent() {
      let newConfig = cloneDeep(this.condition);
      newConfig.components.forEach((component) => {
        let operator = component.operator.value;
        component.operator.value =
            operator === undefined
                ? component.operator.options[0].value
                : operator;
      });
      this.newCustomFiled = newConfig.components.filter((co) => co.custom);
      for (let customField of this.newCustomFiled) {
        let co = _findByKey(this.optional.components, customField.key);
        co
            ? this.$set(co, "disable", true)
            : this.$set(customField, "disable", false);
      }
      this.config.components[1] = {
        label: this.$t("custom_field.name"),
        child: this.newCustomFiled,
      };
    },
    addFilter() {
      const index = _findIndexByKey(
          this.optional.components,
          this.nullFilterKey
      );
      if (index > -1) {
        this.$warning(this.$t("commons.adv_search.add_filter_link_tip"));
        return;
      }
      let data = {
        key: this.nullFilterKey,
        name: "MsTableSearchInput",
        label: "",
        operator: {
          options: [],
        },
        disable: false,
      };
      this.optional.components.push(data);
    },
    remove(component) {
      this.showAddFilterLink = true;
      if (!this.condition.custom) {
        this.enableOptional(component, this.config.components);
      } else {
        // 系统字段和自定义字段选项合并
        const components = concat(
            this.config.components[0].child,
            this.config.components[1].child
        );
        this.enableOptional(component, components);
      }
      let index = _findIndexByKey(this.optional.components, component.key);
      if (index !== -1) {
        this.optional.components.splice(index, 1);
      }
    },
    enableOptional(component, components) {
      let data = _findByKey(components, component.key);
      if (data) {
        this.$set(data, "disable", false);
      }
    },
    /**
     * 【搜索记忆】回填已保存的搜索条件到当前组件列表
     * 遍历 savedConditions 数组，在 optional.components 中查找匹配的 key：
     * - 找到则恢复该组件的 operator.value 和 value
     * - 未找到（字段已被删除或模板变更）则跳过，不影响其余字段
     * @param {Array} savedConditions - 从 localStorage 读取的搜索条件数组
     */
    _restoreSearchConditions(savedConditions) {
      if (!Array.isArray(savedConditions)) {
        return;
      }

      // 获取所有可用组件（包括未显示的自定义字段）
      let allComponents = this.condition.custom
        ? concat(this.config.components[0].child, this.config.components[1].child)
        : this.config.components;

      savedConditions.forEach((saved) => {
        // 先在当前显示的组件中查找
        let target = _findByKey(this.optional.components, saved.key);

        // 如果未找到，在所有组件中查找并添加到显示列表
        if (!target) {
          target = _findByKey(allComponents, saved.key);
          if (target) {
            this.optional.components.push(target);
          } else {
            return;
          }
        }

        // 恢复操作符值
        if (saved.operator !== undefined && target.operator) {
          target.operator.value = saved.operator;
        }
        // 恢复搜索值
        if (saved.value !== undefined) {
          if (target.options && target.options.url) {
            setTimeout(() => {
              target.value = saved.value;
            }, 1000);
          } else {
            target.value = saved.value;
          }
        }
      });
    },
    // 搜索组件的字段变换时触发
    changeSearchItemKey(newData, oldData) {
      let key = oldData ? oldData.key : this.nullFilterKey;
      const index = _findIndexByKey(this.optional.components, key);
      this.optional.components.splice(index, 1, newData);
      this.showAddFilterLink = false;
      let components = [];
      if (!this.condition.custom) {
        components = this.config.components;
      } else {
        components = concat(
            this.config.components[0].child,
            this.config.components[1].child
        );
      }
      for (let op of components) {
        if (op.disable !== undefined && op.disable === false) {
          this.showAddFilterLink = true;
          break;
        }
      }
    },
  },
};
</script>

<style>
@media only screen and (min-width: 1870px) {
  .el-dialog.adv-dialog {
    width: 70%;
  }
}

@media only screen and (min-width: 1650px) and (max-width: 1869px) {
  .el-dialog.adv-dialog {
    width: 80%;
  }
}

@media only screen and (min-width: 1470px) and (max-width: 1649px) {
  .el-dialog.adv-dialog {
    width: 90%;
  }
}

@media only screen and (max-width: 1469px) {
  .el-dialog.adv-dialog {
    width: 70%;
    min-width: 695px;
  }
}
</style>

<style scoped>
.dialog-footer {
  text-align: center;
}

.search-items {
  width: 100%;
}

@media only screen and (max-width: 1469px) {
  .search-item {
    width: 100%;
  }
}

@media only screen and (min-width: 1470px) {
  .search-item {
    width: 50%;
  }
}

.search-item {
  display: inline-block;
  margin-top: 10px;
}

.delete-icon {
  font-size: 17px;
  margin-top: 8px;
}

.delete-icon:hover {
  cursor: pointer;
}

.add-filter-link {
  position: absolute;
  left: 25px;
  bottom: 50px;
}

:deep(span.el-select__tags-text) {
  display: inline-block;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(i.el-tag__close.el-icon-close) {
  position: relative;
  left: 3px;
  top: -6px;
}
</style>

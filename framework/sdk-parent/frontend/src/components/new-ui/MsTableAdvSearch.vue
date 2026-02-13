<template>
  <span class="adv-search-bar">
    <el-button
      size="mini"
      @click="open"
      ref="filter-btn"
      :class="conditionNum === 0 ? '' : 'btn-active'"
    >
      <svg-icon
        :icon-class="conditionNum === 0 ? 'icon-filter' : 'icon-filter-active'"
      />
      <span class="condition-num">{{
        conditionNum === 0 ? "" : "(" + conditionNum + ")"
      }}</span>
    </el-button>
    <el-dialog
      :title="$t('commons.adv_search.new_title')"
      :visible.sync="visible"
      custom-class="adv-dialog"
      :append-to-body="true"
      width="60%"
    >
      <div class="search-items" style="height: 275px">
        <el-scrollbar style="height: 100%" ref="scrollbar">
          <div
            class="search-item"
            v-for="component in optional.components"
            :key="component.key"
          >
            <el-row>
              <el-col :span="23">
                <component
                  :is="component.name"
                  :component="component"
                  :components.sync="config.components"
                  @updateKey="changeSearchItemKey"
                  :custom="condition.custom"
                />
              </el-col>
              <el-col :span="1">
                <i
                  class="el-icon-delete delete-icon"
                  @click="remove(component)"
                  v-if="optional.components.length !== 1"
                ></i>
              </el-col>
            </el-row>
          </div>
        </el-scrollbar>
        <el-link
          type="primary"
          icon="el-icon-plus"
          v-if="showAddFilterLink"
          :underline="false"
          class="add-filter-link"
          @click="addFilter"
          >{{ $t("commons.adv_search.add_filter_link") }}</el-link
        >
      </div>
      <template v-slot:footer>
        <div class="dialog-footer" style="margin-top: 30px">
          <el-button size="small" @click="reset">{{
            $t("commons.adv_search.reset")
          }}</el-button>
          <el-button
            size="small"
            type="primary"
            @click="search"
            class="custom-btn"
            >{{ $t("commons.adv_search.search") }}</el-button
          >
        </div>
      </template>
    </el-dialog>
  </span>
</template>

<script>
import components from "../search/search-components";
import { BUILTIN_ADV_SEARCH_KEYS } from "../search/search-components";
import { cloneDeep, concat, slice } from "lodash-es";
import { _findByKey, _findIndexByKey } from "../search/custom-component";
// 导入用户身份工具函数，用于获取当前登录用户 ID
import { getCurrentUserId } from "../../utils/token";
// 导入高级搜索条件记忆相关的工具函数（保存、读取、清除）
import { saveAdvSearchCondition, getAdvSearchCondition, clearAdvSearchCondition } from "../../utils/tableUtils";

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
  components: { ...components },
  name: "MsNewUiTableAdvSearch",
  props: {
    condition: Object,
    showLink: {
      type: Boolean,
      default: true,
    },
    showItemSize: {
      type: Number,
      default() {
        return 6; // 默认展示的搜索条件数量
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
      conditionNum: 0,
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
  updated() {
    this.setScrollToBottom();
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
      this.conditionNum = 0;
      let condition = {};
      this.optional.components.forEach((component) => {
        let value = component.value;
        if (Array.isArray(value)) {
          if (value.length > 0) {
            this.setCondition(condition, component);
            this.conditionNum += 1;
          }
        } else {
          if (value !== undefined && value !== null && value !== "") {
            this.setCondition(condition, component);
            this.conditionNum += 1;
          }
        }
      });

      // 处理级联逻辑（新增）
      this.handleCascadeLogic();

      if (this.conditionNum > 0) {
        this.$refs["filter-btn"].$el.focus();
        this.$refs["filter-btn"].$el.style.width = "auto";
      } else {
        this.$refs["filter-btn"].$el.blur();
        this.$refs["filter-btn"].$el.style.width = "32px";
      }

      // 清除name
      if (this.condition.name) {
        this.condition.name = undefined;
      }
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
      this.conditionNum = 0;
      this.$refs["filter-btn"].$el.blur();
      this.$refs["filter-btn"].$el.style.width = "32px";
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
          (comp.options.params = { projectId: this.condition.projectId })
      );
    },
    addFilter() {
      const index = _findIndexByKey(
        this.optional.components,
        this.nullFilterKey
      );
      if (index > -1) {
        this.$warning(this.$t("commons.adv_search.add_filter_link_tip"), false);
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
      savedConditions.forEach((saved) => {
        // 在当前显示的搜索组件中查找与已保存条件 key 匹配的组件
        const target = _findByKey(this.optional.components, saved.key);
        if (!target) {
          // 字段在当前模板中不存在，跳过（兼容模板字段变更场景）
          return;
        }
        // 恢复操作符值（如 like、in、equals 等）
        if (saved.operator !== undefined && target.operator) {
          target.operator.value = saved.operator;
        }
        // 恢复搜索值（可能是字符串、数组等多种类型）
        if (saved.value !== undefined) {
          target.value = saved.value;
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
    // 新增方法：处理级联逻辑（工作空间-项目级联）
    handleCascadeLogic() {
      this.optional.components.forEach((component) => {
        // 检查是否有级联配置
        if (component.cascadeKey && component.cascadeUpdate) {
          // 查找依赖的组件（如工作空间）
          const cascadeComponent = this.optional.components.find(
            c => c.key === component.cascadeKey
          );
          if (cascadeComponent && cascadeComponent.value) {
            // 触发级联更新
            component.cascadeUpdate(component, cascadeComponent.value);
          }
        }
      });
    },
    setScrollToBottom() {
      if (this.$refs["scrollbar"]) {
        this.$refs["scrollbar"].wrap.scrollTop =
          this.$refs["scrollbar"].wrap.scrollHeight;
      }
    },
  },
  computed: {
    isAuto() {
      return this.conditionNum > 0;
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

.search-items .el-scrollbar .el-scrollbar__wrap {
  overflow-x: auto;
}
</style>

<style lang="scss">
.adv-dialog {
  .el-dialog__header {
    text-align: left !important;
  }
  .el-dialog__body {
    text-align: left !important;
  }
  button.el-button.custom-btn.el-button--primary.el-button--small {
    color: #ffffff !important;
  }
}
</style>
<style scoped>
.dialog-footer {
  text-align: right;
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
  display: block;
  width: 100%;
  margin-left: -15px;
}

.delete-icon {
  font-size: 17px;
  margin-top: 8px;
}

.delete-icon:hover {
  cursor: pointer;
}

.add-filter-link {
  position: relative;
  top: -10px;
  padding: 1px 0px 1px 0px;
  height: 26px;
  width: 112px;
  border-radius: 4px;
  float: left;
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

:deep(.el-row) {
  margin: 0px 0px 12px 0px !important;
}

:deep(.search-label) {
  padding-left: 0px;
  width: 30%;
}

:deep(.search-operator) {
  width: 15%;
}

:deep(.search-content) {
  width: 50%;
}

span.condition-num {
  width: 18px;
  height: 10px;
  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  /* line-height: 22px; */
  text-align: center;
  /* color: #783887; */
  flex: none;
  order: 1;
  flex-grow: 0;
  font-family: "PingFang SC";
  font-style: normal;
  line-height: 22px;
  display: flex;
  align-items: center;
  letter-spacing: -0.1px;
  color: #783887;
  position: relative;
  top: -25px;
  left: 12px;
}

:deep(button.el-button.el-button--default.el-button--mini svg) {
  position: relative;
  right: 7px;
  top: -1px;
  width: 14px;
  height: 14px;
}

.add-filter-link:hover {
  background: rgba(120, 56, 135, 0.1);
  border-radius: 2px;
}

:deep(span.el-tag.el-tag--info.el-tag--mini.el-tag--light) {
  flex-direction: row;
  align-items: center;
  padding: 1px 6px;
  gap: 4px;
  height: 24px;
  background: rgba(31, 35, 41, 0.1);
  border-radius: 2px;
  flex: none;
  flex-grow: 0;

  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  line-height: 22px;
  color: #1f2329;
}

:deep(i.el-tag__close.el-icon-close) {
  position: relative;
  left: 6px;
  top: -6px;
}

.adv-search-bar {
  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  line-height: 22px;
  color: #1f2329;
}

:deep(input.el-input__inner) {
  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  line-height: 22px;
  color: #1f2329;
}

:deep (.el-button--mini, .el-button--small) {
  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
}

:deep(.el-button--small span) {
  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  line-height: 22px;
  position: relative;
  top: -5px;
}

.el-button--small {
  min-width: 80px;
  height: 32px;
  border-radius: 4px;
}

.btn-active {
  border: 1px solid #783887 !important;
}
</style>

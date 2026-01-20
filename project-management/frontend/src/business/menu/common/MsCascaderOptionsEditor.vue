<template>
  <div class="ms-cascader-options-editor">
    <div>
      <el-link class="add-text" :underline="false" :disabled="disable" @click="addParent">
        <i class="el-icon-plus">{{ $t('custom_field.add_option') }}</i>
      </el-link>
    </div>

    <draggable :list="data" handle=".handle" class="list-group">
      <div class="parent-item"
           v-for="(parent, parentIdx) in data" :key="parentIdx">
        <div class="list-group-item">
          <!-- 展开/折叠图标 -->
          <i class="el-icon-arrow-right expand-icon"
             :class="{'expanded': expandedParents[parentIdx]}"
             v-if="parent.children && parent.children.length > 0"
             @click="toggleExpand(parentIdx)"></i>
          <span class="expand-placeholder" v-else></span>

          <font-awesome-icon class="handle" :icon="['fas', 'align-justify']"/>

          <!-- 父级选项编辑 -->
          <el-input size="mini" type="text"
                    class="text-item"
                    :placeholder="$t('custom_field.field_text')"
                    maxlength="50"
                    show-word-limit
                    v-if="editParentIndex === parentIdx"
                    @blur="handleParentTextEdit(parent)"
                    v-model="parent.text"/>
          <span class="text-item" v-else>
            <span>{{ parent.text || $t('custom_field.field_text') }}</span>
          </span>

          <!-- 编辑时显示值输入框 -->
          <el-input size="mini" type="text"
                    class="text-item"
                    :placeholder="$t('custom_field.field_value')"
                    maxlength="50"
                    show-word-limit
                    v-if="editParentIndex === parentIdx"
                    @blur="handleParentValueEdit(parent)"
                    v-model="parent.value"/>

          <!-- 操作按钮 -->
          <el-link :underline="false" class="operator-icon" @click="editParent(parentIdx)">
            <i class="el-icon-edit"></i>
          </el-link>
          <el-link :underline="false" class="operator-icon" @click="addChild(parentIdx)" title="添加子选项">
            <i class="el-icon-plus"></i>
          </el-link>
          <el-link :underline="false" class="operator-icon" @click="deleteParent(parentIdx)">
            <i class="el-icon-close"></i>
          </el-link>
        </div>

        <!-- 子级选项列表 -->
        <transition name="slide-fade">
          <div class="children-container" v-if="parent.children && parent.children.length > 0 && expandedParents[parentIdx]">
            <draggable :list="parent.children" handle=".child-handle" class="children-list">
              <div class="list-group-item child-item"
                   v-for="(child, childIdx) in parent.children" :key="childIdx">
                <font-awesome-icon class="child-handle" :icon="['fas', 'align-justify']"/>

                <el-input size="mini" type="text"
                          class="text-item"
                          :placeholder="$t('custom_field.field_text')"
                          maxlength="50"
                          show-word-limit
                          v-if="editChildIndex[parentIdx] === childIdx"
                          @blur="handleChildTextEdit(parent, child)"
                          v-model="child.text"/>
                <span class="text-item" v-else>
                  <span>{{ child.text || $t('custom_field.field_text') }}</span>
                </span>

                <!-- 编辑时显示值输入框 -->
                <el-input size="mini" type="text"
                          class="text-item"
                          :placeholder="$t('custom_field.field_value')"
                          maxlength="50"
                          show-word-limit
                          v-if="editChildIndex[parentIdx] === childIdx"
                          @blur="handleChildValueEdit(parent, child)"
                          v-model="child.value"/>

                <el-link :underline="false" class="operator-icon" @click="editChild(parentIdx, childIdx)">
                  <i class="el-icon-edit"></i>
                </el-link>
                <el-link :underline="false" class="operator-icon" @click="deleteChild(parentIdx, childIdx)">
                  <i class="el-icon-close"></i>
                </el-link>
              </div>
            </draggable>
          </div>
        </transition>
      </div>
    </draggable>
  </div>
</template>

<script>
import draggable from "vuedraggable";
import {getUUID} from "metersphere-frontend/src/utils";

export default {
  name: "MsCascaderOptionsEditor",
  components: {
    draggable
  },
  data() {
    return {
      editParentIndex: -1,
      editChildIndex: {}, // {parentIdx: childIdx}
      expandedParents: {} // {parentIdx: boolean} 记录每个父级选项的展开状态，默认展开
    };
  },
  mounted() {
    // 初始化所有有子选项的父级为折叠状态（默认合并）
    this.data.forEach((parent, idx) => {
      if (parent.children && parent.children.length > 0) {
        this.$set(this.expandedParents, idx, false);
      }
    });
  },
  watch: {
    data: {
      handler(newData) {
        // 当数据变化时，更新展开状态（默认折叠）
        newData.forEach((parent, idx) => {
          if (parent.children && parent.children.length > 0 && this.expandedParents[idx] === undefined) {
            this.$set(this.expandedParents, idx, false);
          }
        });
      },
      deep: true
    }
  },
  props: {
    disable: Boolean,
    data: {
      type: Array,
      default() {
        return [];
      }
    }
  },
  methods: {
    addParent() {
      let item = {
        text: '',
        value: getUUID().substring(0, 8),
        children: []
      };
      this.data.push(item);
      const newIdx = this.data.length - 1;
      this.editParentIndex = newIdx;
      this.$set(this.editChildIndex, newIdx, -1);
      this.$set(this.expandedParents, newIdx, false);
    },
    toggleExpand(parentIdx) {
      this.$set(this.expandedParents, parentIdx, !this.expandedParents[parentIdx]);
    },
    editParent(parentIdx) {
      if (this.disable) {
        return;
      }
      this.editParentIndex = parentIdx;
    },
    handleParentTextEdit(parent) {
      if (parent.text && parent.value) {
        this.editParentIndex = -1;
      }
    },
    handleParentValueEdit(parent) {
      if (parent.text && parent.value) {
        this.editParentIndex = -1;
      }
    },
    deleteParent(parentIdx) {
      if (this.disable) {
        return;
      }
      this.data.splice(parentIdx, 1);
      if (this.editParentIndex === parentIdx) {
        this.editParentIndex = -1;
      } else if (this.editParentIndex > parentIdx) {
        this.editParentIndex--;
      }
      this.$delete(this.editChildIndex, parentIdx);
      this.$delete(this.expandedParents, parentIdx);
      // 更新后续父级索引
      let newEditChildIndex = {};
      let newExpandedParents = {};
      Object.keys(this.editChildIndex).forEach(key => {
        let idx = parseInt(key);
        if (idx < parentIdx) {
          newEditChildIndex[idx] = this.editChildIndex[key];
        } else if (idx > parentIdx) {
          newEditChildIndex[idx - 1] = this.editChildIndex[key];
        }
      });
      Object.keys(this.expandedParents).forEach(key => {
        let idx = parseInt(key);
        if (idx < parentIdx) {
          newExpandedParents[idx] = this.expandedParents[key];
        } else if (idx > parentIdx) {
          newExpandedParents[idx - 1] = this.expandedParents[key];
        }
      });
      this.editChildIndex = newEditChildIndex;
      this.expandedParents = newExpandedParents;
    },
    addChild(parentIdx) {
      if (this.disable) {
        return;
      }
      if (!this.data[parentIdx].children) {
        this.$set(this.data[parentIdx], 'children', []);
      }
      let child = {
        text: '',
        value: getUUID().substring(0, 8)
      };
      this.data[parentIdx].children.push(child);
      this.$set(this.editChildIndex, parentIdx, this.data[parentIdx].children.length - 1);
      // 确保父级展开
      this.$set(this.expandedParents, parentIdx, true);
    },
    editChild(parentIdx, childIdx) {
      if (this.disable) {
        return;
      }
      this.$set(this.editChildIndex, parentIdx, childIdx);
    },
    handleChildTextEdit(parent, child) {
      if (child.text && child.value) {
        this.$set(this.editChildIndex, this.data.indexOf(parent), -1);
      }
    },
    handleChildValueEdit(parent, child) {
      if (child.text && child.value) {
        this.$set(this.editChildIndex, this.data.indexOf(parent), -1);
      }
    },
    deleteChild(parentIdx, childIdx) {
      if (this.disable) {
        return;
      }
      this.data[parentIdx].children.splice(childIdx, 1);
      if (this.editChildIndex[parentIdx] === childIdx) {
        this.$set(this.editChildIndex, parentIdx, -1);
      } else if (this.editChildIndex[parentIdx] > childIdx) {
        this.$set(this.editChildIndex, parentIdx, this.editChildIndex[parentIdx] - 1);
      }
    }
  }
};
</script>

<style scoped>
.text-item {
  margin: 5px;
  width: 150px;
}

.operator-icon:hover {
  font-size: 15px;
  font-weight: bold;
}

.operator-icon {
  margin-right: 6px;
}

.operator-icon:first-child {
  margin-left: 20px;
}

.add-text {
  font-size: 14px;
}

.add-text:hover {
  font-size: 14px;
  font-weight: bold;
}

.list-group-item {
  display: flex;
  align-items: center;
}

.parent-item {
  margin-bottom: 5px;
}

.child-item {
  margin-left: 30px;
  margin-top: 5px;
}

.children-container {
  width: 100%;
  margin-top: 5px;
  padding-left: 30px;
}

.children-list {
  width: 100%;
}

.expand-icon {
  cursor: pointer;
  margin-right: 8px;
  transition: transform 0.3s;
  color: #909399;
  font-size: 12px;
  display: inline-block;
  width: 12px;
  text-align: center;
  vertical-align: middle;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.expand-placeholder {
  display: inline-block;
  width: 12px;
  margin-right: 8px;
  vertical-align: middle;
}

/* 展开/折叠动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
  overflow: hidden;
}

.slide-fade-leave-active {
  transition: all 0.3s ease-in;
  overflow: hidden;
}

.slide-fade-enter {
  max-height: 0;
  opacity: 0;
  transform: translateY(-10px);
}

.slide-fade-enter-to {
  max-height: 1000px;
  opacity: 1;
  transform: translateY(0);
}

.slide-fade-leave {
  max-height: 1000px;
  opacity: 1;
  transform: translateY(0);
}

.slide-fade-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-10px);
}
</style>


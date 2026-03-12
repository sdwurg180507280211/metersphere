<template>
  <div class="app-container">
    <div v-show="showSearch" class="mb-[10px] showSearch">
      <el-card shadow="hover">
        <el-form
          :inline="true"
          :model="queryParams"
          label-position="left"
          label-width="auto"
          ref="searchFormRef"
          v-show="showSearch">
          <el-row :gutter="20">
            <template v-for="item of filterList">
              <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" v-if="item.isShow" :key="item.prop">
                <span
                  class="list-item"
                  @mouseenter="item.showDelIcon = true"
                  @mouseleave="item.showDelIcon = false">
                  <el-icon
                    class="mini-icon"
                    v-if="item.showDelIcon"
                    @click="delFiter(item)">
                    <Close/>
                  </el-icon>
                  <el-form-item
                    :label="item.label"
                    :prop="item.prop">
                  <el-input
                    v-if="item.type === 'input'"
                    v-model="queryParams[item.prop]"
                    :placeholder="'请输入' + item.label"
                    style="width: 180px"
                    clearable/>
                  <el-select
                    v-else-if="
                      item.type === 'select' &&
                      item.prop !== 'createUser1' &&
                      item.prop !== 'createdept'
                    "
                    style="width: 180px"
                    v-model="queryParams[item.prop]"
                    :placeholder="'请选择' + item.label"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    filterable
                    clearable
                  >
                    <el-option
                      v-for="item of chooseList(item)"
                      :label="item.label"
                      :value="item.value"
                    ></el-option>
                  </el-select>
                  <!-- 部门 -->
                  <el-select
                    v-else-if="
                      item.type === 'select' && item.prop === 'createdept'
                    "
                    style="width: 180px"
                    v-model="queryParams.createdept"
                    allow-create
                    filterable
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    value-key="value"
                    placeholder="请选择需求申请部门"
                    @change="deptChange"
                    clearable>
                    <el-option
                      v-for="item of deptList"
                      :key="item.value"
                      :label="item.label"
                      :value="item">
                    </el-option>
                  </el-select>
                  <!-- 申请人 -->
                  <el-select
                    v-else-if="item.type === 'select' && item.prop === 'createUser1'"
                    style="width: 180px"
                    v-model="queryParams.createUser1"
                    placeholder="请选择需求申请人"
                    allow-create
                    filterable
                    clearable
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    :no-data-text="queryParams.createdept ? '无数据' : '请先选择需求申请部门'"
                  >
                    <el-option
                      v-for="item of userList"
                      :label="item.label"
                      :value="item.label"
                    ></el-option>
                  </el-select>
                <el-date-picker
                  v-if="item.type === 'date'"
                  v-model="queryParams[item.prop]"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  start-placeholder="请选择日期"
                  end-placeholder="请选择日期"
                  style="width: 300px"
                  clearable
                />
              </el-form-item>
            </span>
              </el-col>
            </template>
          </el-row>
          <el-row>
            <el-col :span="10">
              <el-dropdown
                :hide-on-click="false"
                trigger="click"
                max-height="200px"
              >
                <el-button type="primary" plain :icon="Filter"
                  >条件选择</el-button
                >
                <template #dropdown>
                  <el-dropdown-menu>
                    <template v-for="item in filterList" :key="item.prop">
                      <el-dropdown-item>
                        <el-checkbox
                          :checked="item.isShow"
                          :label="item.label"
                          v-model="item.isShow"
                          @change="changeChecked(item)"
                        />
                      </el-dropdown-item>
                    </template>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <el-button
                style="margin-left: 10px"
                :icon="DocumentChecked"
                v-debounce:click="saveFilter"
                >保存筛选</el-button
              >
            </el-col>
            <el-col :span="12">
              <el-button
                type="primary"
                plain
                :icon="Search"
                v-debounce:click="searchQuery"
                >搜索</el-button
              >
              <el-button :icon="Refresh" v-debounce:click="reset"
                >重置</el-button
              >
            </el-col>
            <el-form-item> </el-form-item>
          </el-row>
        </el-form>
      </el-card>
    </div>

    <el-card shadow="hover" style="margin-top: 10px">
      <template #header>
        <el-row :gutter="10" class="toolbar">
          <el-form-item label="筛选器" class="filter">
            <el-select
              style="width: 196px"
              placeholder="请选择"
              v-model="filterName1"
              @change="searchListChange"
              filterable
              clearable
            >
              <el-option
                v-for="item of searchList"
                :key="item.searchName"
                :label="item.searchName"
                :value="item"
              >
                <!-- 显示删除按钮 -->
                <template #default>
                  {{ item.searchName }}
                  <el-button
                    size="small"
                    type="text"
                    icon="el-icon-delete"
                    @click.prevent="removeFliter(item)"
                  ></el-button> </template
              ></el-option>
            </el-select>
          </el-form-item>
          <el-button
            :class="[isClick ? 'bg' : '', 'mb-10']"
            type="primary"
            plain
            v-debounce:click="aboutMe"
            >与我相关</el-button
          >
          <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            class="mb-10"
          >
            导出
          </el-button>
          <right-toolbar
            v-model:showSearch="showSearch"
            @queryTable="reset()"
          ></right-toolbar>
          <FilterTable
            :columns="columns"
            :tableData="demandQueryList"
            @changeColumns="changeColumns"
          ></FilterTable>
        </el-row>
      </template>
      <!-- 评审单列表 -->
      <el-table
        v-loading="loading"
        :style="{ height: tableHeight + 'px' }"
        :data="demandQueryList"
        border
        style="width: 100%"
      >
        <el-table-column
          v-for="item of headerList"
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :width="item.width"
          :align="item.align"
          :fixed="item.fixed"
          show-overflow-tooltip
        >
          <template #default="{ row }" v-show="false">
            <template v-if="item.prop === 'dmpNum'">
              <span
                class="nameClickSty"
                @click="handleFlowDetail(row, item.prop)"
              >
                {{ row[item.prop] }}
              </span>
            </template>
            <template v-else-if="item.prop === 'actNameStatus'">
              <span
                :class="[
                  'status-badge',
                  row.actNameStatus === '已上线' && 'status-finish',
                  row.actNameStatus === '未上线' && 'status-dev',
                  row.actNameStatus === '取消' && 'status-cancel',
                ]"
              >
                {{ row.actNameStatus }}
              </span>
            </template>
            <template v-else-if="item.prop === 'operation'">
              <el-link
                :disabled="justifyEdit(row, '0')"
                type="primary"
                :underline="false"
                @click="handleopenDialog(row, '0')"
                style="margin-right: 15px"
                >修改</el-link
              >
              <el-link
                :disabled="justifyEdit(row, '1')"
                type="danger"
                :underline="false"
                @click="handleCancel(row)"
                style="margin-right: 15px"
                >取消</el-link
              >
              <el-link
                v-if="
                  row.actOrSuspend === '激活' &&
                  (row.reqManagerId == useUserStore().id ||
                    row.startUserId == useUserStore().id)
                "
                type="danger"
                :underline="false"
                @click="handleAct(row)"
                style="margin-right: 15px"
                >激活</el-link
              >
              <el-link
                v-if="
                  row.actOrSuspend === '挂起' &&
                  (row.reqManagerId == useUserStore().id ||
                    row.startUserId == useUserStore().id)
                "
                type="danger"
                :underline="false"
                @click="handleAct(row)"
                style="margin-right: 15px"
                >挂起</el-link
              >
              <!-- <el-link :disabled="justifyEdit(row)" icon="Delete" type="danger" :underline="false" @click="handleopenDialog(row, '1')">删除</el-link> -->
            </template>
            <template v-else>
              {{ row[item.prop] || "--" }}
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagintion"
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        background
        :total="total"
        layout="total,sizes,prev,pager,next,jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      ></el-pagination>
    </el-card>

    <!-- 修改流程弹窗 -->
    <editProcessEdit ref="editProcessEditRef" @updateList="getList" />
    <!-- 删除流程 -->
    <deleteProcess ref="deleteProcessRef" @updateList="getList" />
  </div>
</template>

<script setup name="DemandQuery">
import editProcessEdit from "../components/editProcessEdit.vue";
import deleteProcess from "../components/deleteProcess.vue";
import {
  Search,
  Refresh,
  DocumentChecked,
  Filter,
  Close,
} from "@element-plus/icons-vue";
import {
  getDemandQueryList,
  getDeptList,
  getFatherSys,
  getUserList,
  getSystemList,
  getLoops,
  exportFile,
  getSearch,
  addSearch,
  delSearch,
  editSearch,
} from "@/api/demandQuery/demandQuery.js";
import { todoList } from "../../../api/homepage";
import { cancelTask, actOrSuspend } from "@/api/tool/gen.js";
import { nextTick, watch } from "vue";
import { debounce } from "../../../utils";
import { jumpFinished } from "../../../api/review/review";
import router from "@/router";
import useUserStore from "@/store/modules/user";
import { ElMessageBox, ElSelect, ElOption } from "element-plus";
import { headerList, filterList } from "../requireManagent/tableDataLists";

const { proxy } = getCurrentInstance();
const loading = ref(false);
const showSearch = ref(true);
const filterName1 = ref("");
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  name1: null,
  dmpNum: null,
  reqFatherClass: null,
  reqSonClass: null,
  createdept: null,
  createUser1: null,
  startUserName: null,
  deptName: null,
  reqManagerName: null,
  systemName: null,
  createTime: null,
  actName: null,
  parentWfinstCode: null,
  userId: null,
  reviewType: null,
  penTest: null,
  parentName: null,
  upTime: null,
});
const columns = ref([]);
const originalHeaderList = ref([]);
//列表数据
const demandQueryList = ref([]);

const total = ref(0);
const tableHeight = ref(0);
const showSearch_h = ref(0);
let resizeObserver;
// getFatherList();
onMounted(async () => {
  autoHeight();
  getList();
  getDept();
  getSystem();
  getCurrentLoops();
  getSearchList();
  getFatherList();
  // filterList.value.forEach((item) => {
  //   watch(
  //     () => item.isShow,
  //     (nv, ov) => {
  //       autoHeight();
  //     }
  //   );
  // });
  originalHeaderList.value = JSON.parse(JSON.stringify(headerList.value));
  columns.value = originalHeaderList.value.map((item) => {
    return {
      label: item.label,
      prop: item.prop,
      isShow: true,
      disabled: item.prop === "operation",
    };
  });

  const showSearchElement = document.querySelector(".showSearch");

  showSearch_h.value = showSearchElement.offsetHeight;
  resizeObserver = new ResizeObserver((entries) => {
    for (const entry of entries) {
      showSearch_h.value = entry.target.offsetHeight;
    }
    autoHeight();
  });
  resizeObserver.observe(showSearchElement);

  window.addEventListener("resize", autoHeight);
});

function changeColumns(val) {
  headerList.value = originalHeaderList.value.filter((item) => {
    return val.some((i) => i.prop === item.prop && i.isShow);
  });
}

// 流程取消
function handleCancel(row) {
  // console.log(row, row.processInsId, row.name1)
  ElMessageBox.confirm(
    `<div>
      <p style="font-size: 16px;">是否要取消流程：${row.name1}?</p>
      <p style="color: red; font-size: 13px;">温馨提示：需求位于'代码质量检测中'或'待上线'节点的需求不支持取消。若需取消,请联系管理员删除或迁移处理。取消后无法恢复，请谨慎处理！</p>
      </div>`,
    "系统提示",
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: "确认",
      cancelButtonText: "返回",
      type: "warning",
    },
  ).then(async () => {
    proxy.$modal.loading("取消中...");
    try {
      await cancelTask({ procInsId: row.processInsId });
      proxy.$modal.msgSuccess("取消成功");
      getList();
    } finally {
      proxy.$modal.closeLoading();
    }
  });
}
// 流程激活或挂起
function handleAct(row) {
  ElMessageBox.confirm(
    `<div>
      <p style="font-size: 16px;">是否要激活/挂起流程：${row.name1}?</p>
      </div>`,
    "系统提示",
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: "确认",
      cancelButtonText: "返回",
      type: "warning",
    },
  ).then(async () => {
    proxy.$modal.loading("操作中...");
    try {
      await actOrSuspend({
        actOrSuspend: row.actOrSuspend,
        processInsId: row.processInsId,
      });
      proxy.$modal.msgSuccess("成功");
      getList();
    } finally {
      proxy.$modal.closeLoading();
    }
  });
}
const routerMap = new Map([
  ["上线", "/onlinemanage/onlinelist"],
  ["紧急上线", "/onlinemanage/urgencyOnline"],
  ["紧急评审", "/experReview/urgentReview"],
  ["专家评审", "/experReview/reviewlist"],
  ["需求分析管理", "/requirementAnalysis/reqAnalysisList"],
  ["UI需求设计", "/uiDesign/list"],
  ["需求个人报工", "/workReport/list"],
  ["其他个人报工", "/workReport/list"],
]);
// 跳转待办
function jumpToDo(data) {
  if (routerMap.has(data.procDefName)) {
    router.push({
      path: routerMap.get(data.procDefName),
      query: {},
    });
  } else {
    router.push({
      path: `/unitview/${data.taskId}`,
      query: {
        procInsId: data.procInsId,
        executionId: data.executionId,
        deployId: data.deployId,
        taskId: data.taskId,
        taskName: data.taskName,
        procDefName: data.procDefName,
        startUser: data.startUserName + "-" + data.startDeptName,
        taskDefKey: data.taskDefKey,
        assigneeName: data.assigneeName,
      },
    });
  }
}

// 判断是否可以编辑
function justifyEdit(row, type) {
  if (type === "0") {
    return (row.reqManagerId == useUserStore().id ||
      row.startUserId == useUserStore().id) &&
      row.actName != "结束" &&
      row.isAddOnline == "false"
      ? false
      : true;
  } else {
    return (row.reqManagerId == useUserStore().id ||
      row.startUserId == useUserStore().id) &&
      row.actName != "结束" &&
      row.actName != "取消"
      ? false
      : true;
  }
}

// 控制编辑弹窗显示隐藏
function handleopenDialog(row, type) {
  if (type === "0") {
    proxy.$refs.editProcessEditRef.openDialog(row);
  } else {
    proxy.$refs.deleteProcessRef.open(row);
  }
}

watch(
  () => showSearch.value,
  () => {
    autoHeight();
  },
);
/** 流程流转记录 */
const handleFlowDetail = async (row, prop) => {
  try {
    let obj;
    const obj1 = {
      procInsId: row.processInsId,
      taskId: row.taskId,
      executionId: row.executionId,
    };
    // 跳转待办
    if (
      row.assignee &&
      row.assignee == useUserStore().id &&
      row.actName !== "结束"
    ) {
      let procId;
      if (row.isZxq === "true") {
        procId = row.toflowid;
      } else {
        procId = row.processInsId;
      }

      let res = await todoList({
        // procInsId: row.isZxq === "true" ? row.toflowid : row.processInsId,
        procInsId: procId,
        // procInsId: row.processInsId,
      });
      if (res.code === 200 && res.rows.length > 0) {
        jumpToDo(res.rows[0]);
      }
    } else {
      // 跳转已办
      let res = await jumpFinished(obj1);
      if (res.code === 200) {
        const data = res.data[0];
        obj = {
          taskId: data.taskId,
          procInsId: data.procInsId,
          deployId: data.deployId,
          executionId: data.executionId,
          procDefName: data.procName,
          refill: data.refill,
          taskName: data.taskName,
          finishTime: "finished",
          revokeTask: false,
        };
        router.push({
          path: `/newprocessdetails/${obj.taskId}`,
          query: obj,
        });
      }
    }
  } catch (error) {
    throw error;
  }
};
// 定义状态映射对象
const statusMap = {
  0: "未上线",
  1: "已上线",
  2: "取消",
};
// 需求列表
async function getList(data = null) {
  loading.value = true;
  const obj = data ? { ...data } : { ...queryParams.value };

  for (let k in obj) {
    if (Array.isArray(obj[k])) {
      if (k === "createdept") {
        obj[k] = obj[k].map((d) => d.label).join();
      } else {
        obj[k] = obj[k].join();
      }
    }
  }
  const widthArr = [];
  const pattern = /\d/;
  try {
    let res = await getDemandQueryList(obj);
    if (res.code === 200) {
      total.value = res.data.total;
      loading.value = false;
      demandQueryList.value = res.data.records.map((item) => {
        // if (item.isZjps) {
        //   item.isZjps = item.isZjps === "0" ? "否" : "是";
        // }
        if (item.subPassStatus) {
          item.subPassStatus = item.subPassStatus === "0" ? "驳回" : "通过";
        }
        if (item.name1) {
          widthArr.push(pattern.test(item.name1) ? 12 : item.name1.length);
          headerList.value[2].width = Math.max(...widthArr) * 16 + 10;
        }
        if (item.actNameStatus) {
          item.actNameStatus = statusMap[item.actNameStatus * 1];
        }

        return item;
      });
    }

    return res;
  } catch (err) {
    throw err;
  }
}
// 申请人
const userList = ref([]);
async function getUser(params) {
  try {
    let res = await getUserList(params);
    if (res.code === 200) {
      userList.value = res.data.map((item) => {
        let obj = {};
        obj.label = item.nickName;
        obj.value = item.id;
        return obj;
      });
    }
  } catch (error) {
    throw error;
  }
}
// 父系统
const fatherSysList = ref([]);
async function getFatherList() {
  try {
    let res = await getFatherSys();
    if (res.code === 200) {
      fatherSysList.value = res.data.data.map((item) => {
        let obj = {};
        obj.label = item.abbreviation + "/" + item.nameZh;
        obj.value = item.abbreviation + "/" + item.nameZh;
        return obj;
      });
    }
  } catch (error) {
    throw error;
  }
}
// getFatherList();
// 部门
const deptList = ref([]);
async function getDept() {
  try {
    let res = await getDeptList();
    if (res.code === 200) {
      deptList.value = res.data.map((item) => {
        let obj = {};
        obj.label = item.deptName;
        obj.value = item.deptId;
        return obj;
      });
    }
  } catch (error) {
    throw error;
  }
}
// 系统
const systemList = ref([]);
async function getSystem() {
  try {
    let res = await getSystemList();
    if (res.code === 200) {
      systemList.value = res.data.map((item) => {
        let obj = {};
        obj.label = item.abbreviation + "/" + item.nameZh;
        // obj.value = item.deployConfirmerId;
        obj.value = item.abbreviation + "/" + item.nameZh;
        return obj;
      });
    }
  } catch (error) {
    throw error;
  }
}
// 当前环节
const loops = ref([]);
async function getCurrentLoops() {
  try {
    let res = await getLoops();
    if (res.code === 200) {
      loops.value = res.data.map((item) => {
        return { label: item, value: item };
      });
    }
  } catch (error) {
    throw error;
  }
}
// 常用搜索
const searchList = ref([]);
async function getSearchList() {
  try {
    let res = await getSearch();
    if (res.code === 200) {
      searchList.value = res.data.map((item) => {
        return item;
      });
    }
  } catch (error) {
    throw error;
  }
}
// 勾选筛选器
const changeChecked = (data) => {
  if (data.prop === "createUser1" && data.isShow) {
    filterList.value = filterList.value.map((item) => {
      if (item.prop === "createdept") {
        item.isShow = true;
      }
      return item;
    });
  }
  if (data.prop === "createdept" && !data.isShow) {
    const arr = filterList.value.filter((item) => item.prop === "createUser1");

    if (arr[0].isShow === true) {
      filterList.value = filterList.value.map((item) => {
        if (item.prop === "createUser1") {
          item.isShow = false;
        }
        return item;
      });
    }
  }
};
// 下拉列表
const chooseList = (item) => {
  switch (item.prop) {
    case "reqFatherClass":
      return [
        { label: "系统优化", value: "系统优化" },
        { label: "需求", value: "需求" },
      ];
    case "reqSonClass":
      return [
        { label: "新产品", value: "新产品" },
        { label: "日常需求", value: "日常需求" },
        { label: "银保通产品", value: "银保通产品" },
        { label: "项目", value: "项目" },
      ];
    case "penTest":
      return [
        { label: "不涉及", value: "不涉及" },
        { label: "通过", value: "通过" },
        { label: "未通过", value: "未通过" },
      ];
    case "reviewType":
      return [
        { label: "不涉及", value: "不涉及" },
        { label: "通过", value: "通过" },
        { label: "未通过", value: "未通过" },
        { label: "未评审", value: "未评审" },
      ];
    case "parentName":
      return fatherSysList.value;
    case "actName":
      return loops.value;
    case "systemName":
      return systemList.value;
  }
};

// 选择常用
const searchListChange = (item) => {
  const { pageNum, pageSize } = queryParams.value;
  // 赋值
  queryParams.value = {
    ...Object.fromEntries(
      Object.keys(queryParams.value).map((key) => [key, null]),
    ),
    pageNum,
    pageSize,
  };
  // 重置表单数据
  for (let i = 0; i < filterList.value.length; i++) {
    filterList.value[i].isShow = false;
  }

  for (let k in item) {
    if (item[k]) {
      for (let i = 0; i < filterList.value.length; i++) {
        const d = filterList.value[i];
        if (d.prop === k) {
          // 针对下拉多选
          if (item[k].includes(",") && d.type === "select") {
            queryParams.value[k] = item[k].split(",");
            d.isShow = true;
            break;
          } else {
            queryParams.value[k] = d.type === "select" ? [item[k]] : item[k];
            d.isShow = true;
            break;
          }
        }
        // 计划上线日期
        if (k === "upTimeString1") {
          queryParams.value.upTime = [item[k].split(" ")[0]];
          break;
        }
        if (k === "upTimeString2") {
          queryParams.value.upTime.push(item[k].split(" ")[0]);
          filterList.value.map((t) => {
            if (t.prop === "upTime") {
              t.isShow = true;
            }
          });
          break;
        }
      }
      // 创建日期
    } else if (
      k === "createTime" &&
      item.createTimeString1 &&
      item.createTimeString2
    ) {
      filterList.value.map((t) => {
        if (t.prop === "createTime") {
          t.isShow = true;
        }
        return item;
      });
      queryParams.value.createTime = [
        item.createTimeString1.split(" ")[0],
        item.createTimeString2.split(" ")[0],
      ];
    }
  }

  filterName1.value = item.searchName;
};
// 保存筛选
const selectedValue = ref("");
const saveFilter = () => {
  ElMessageBox({
    title: "系统提示",
    message: () =>
      h("div", [
        h("div", [
          h(
            "p",
            { style: "font-size: 16px;padding-bottom:10px;" },
            "是否要保存当前搜索为筛选器?",
          ),
        ]),
        h(
          ElSelect,
          {
            modelValue: selectedValue.value,
            "onUpdate:modelValue": (val) => {
              selectedValue.value = val;
            },
            placeholder: "请选择",
            filterable: true, // 开启可过滤功能，实现可输入搜索
            allowCreate: true, // 允许用户创建新的选项
            placeholder: "请选择或输入",
            clearable: true, // 显示清除按钮
            size: "large",
            style: { width: "390px" },
            valueKey: "searchName",
          },
          () =>
            searchList.value.map((option) =>
              h(ElOption, {
                label: option.searchName,
                value: option,
              }),
            ),
        ),
      ]),

    showCancelButton: true,
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    beforeClose: (action, instance, done) => {
      if (action === "cancel") {
        return done();
      }
      if (action === "confirm") {
        if (typeof selectedValue.value === "string") {
          if (selectedValue.value === "") {
            return proxy.$message.error("请输入或选择筛选器名称！");
          }
        } else {
          if (!selectedValue.value.searchName) {
            return proxy.$message.error("请输入或选择筛选器名称！");
          }
        }
      }

      done();
    },
  })
    .then(() => {
      const arr = filterList.value.reduce((acc, item) => {
        if (item.isShow) {
          acc.push(item.prop);
        }
        return acc;
      }, []);
      if (arr.length === 0) {
        proxy.$message.warning("请选择筛选条件！");
        return;
      }
      let name;
      if (typeof selectedValue.value === "string") {
        name = searchList.value.some(
          (item) => item.searchName === selectedValue.value,
        );
      } else {
        name = searchList.value.some(
          (item) => item.searchName === selectedValue.value.searchName,
        );
      }
      // 存在重名走修改
      if (name) {
        const obj = selectedValue.value;
        for (let k in obj) {
          if (k === "createTimeString1" || k === "createTimeString2") {
            timeString(obj, "createTime");
          } else if (k === "upTimeString1" || k === "upTimeString2") {
            timeString(obj, "upTime");
          } else if (k === "id" || k === "userId" || k === "searchName") {
            obj[k] = obj[k];
          } else if (k === "createdept") {
            obj[k] =
              queryParams.value.createdept &&
              queryParams.value.createdept
                .map((item) => {
                  return item.label;
                })
                .join();
          } else {
            obj[k] = Array.isArray(queryParams.value[k])
              ? queryParams.value[k].join()
              : queryParams.value[k];
          }
        }
        obj["searchName"] = selectedValue.value.searchName;
        obj["createTime"] = null;
        obj["upTime"] = null;
        editFilter(obj);
      } else {
        // 不存在走新增
        const obj = {};
        arr.map((item) => {
          if (queryParams.value[item]) {
            if (item === "createTime") {
              timeString(obj, "createTime");
            } else if (item === "upTime") {
              timeString(obj, "upTime");
            } else if (item === "createdept") {
              obj[item] =
                queryParams.value.createdept &&
                queryParams.value.createdept
                  .map((item1) => {
                    return item1.label;
                  })
                  .join();
            } else {
              obj[item] = Array.isArray(queryParams.value[item])
                ? queryParams.value[item].join()
                : queryParams.value[item];
            }
          }
          return item;
        });
        obj.searchName = selectedValue.value;
        addFilter(obj);
      }
    })
    .catch(() => {});
};
// 修改筛选器
async function editFilter(obj) {
  try {
    let res = await editSearch(obj);
    if (res.code === 200) {
      proxy.$message.success("修改成功！");
      selectedValue.value = null;
      await getSearchList();
    }
  } catch (error) {
    proxy.$message.msgError("修改失败！");
    throw error;
  }
}
// 新增筛选器
async function addFilter(obj) {
  try {
    let res = await addSearch(obj);
    if (res.code === 200) {
      proxy.$message.success("保存成功！");
      selectedValue.value = null;
      await getSearchList();
    }
  } catch (error) {
    proxy.$message.msgError("保存失败！");
    throw error;
  }
}
// 删除筛选器
const removeFliter = async (item) => {
  try {
    let res = await delSearch({ id: item.id });
    if (res.code === 200) {
      proxy.$message.success("删除成功");
      await getSearchList();
      filterName1.value = null;
    }
  } catch (error) {
    throw error;
  }
};
// 删除筛选条件
const delFiter = (item) => {
  filterList.value.forEach((d) => {
    if (d.prop === item.prop) {
      d.isShow = false;
      queryParams.value[d.prop] = null;
    }
  });
};
const deptChange = async (data) => {
  if (data.length !== 0) {
    const deptIds = data.map((item) => {
      return item.value;
    });
    await getUser({ deptId: deptIds.join() });
  } else {
    userList.value = null;
  }
};
const handleSizeChange = async (value) => {
  queryParams.value.pageSize = value;
  await searchQuery();
};
const handleCurrentChange = async (value) => {
  queryParams.value.pageNum = value;
  await searchQuery();
};
// 与我相关
const isClick = ref(false);
async function aboutMe() {
  isClick.value = !isClick.value;
  queryParams.value.userId = isClick.value ? useUserStore().id : null;
  let obj = {};
  for (let k in queryParams.value) {
    if (queryParams.value[k]) {
      if (k === "createTime") {
        timeString(obj, "createTime");
      } else if (k === "upTime") {
        timeString(obj, "upTime");
      } else if (Array.isArray(queryParams.value[k])) {
        obj[k] =
          k === "createdept"
            ? queryParams.value[k]
                .map((item) => {
                  return item.label;
                })
                .join()
            : queryParams.value[k].join();
      } else {
        obj[k] = queryParams.value[k];
      }
    }
  }

  await getList(obj);
}

// 重置搜索
function reset() {
  const { pageNum, pageSize } = { ...queryParams.value };
  for (let k in queryParams.value) {
    if (k === "pageNum") {
      queryParams.value[k] = pageNum;
    } else if (k === "pageSize") {
      queryParams.value[k] = pageSize;
    } else {
      queryParams.value[k] = null;
    }
  }
  isClick.value = false;
  userList.value = null;
  getList();
}
// 生成时间字符串
function timeString(obj, key) {
  const timeRange = queryParams.value[key];
  if (timeRange) {
    obj[`${key}String1`] = timeRange[0];
    obj[`${key}String2`] = timeRange[1];
  }
}

// 搜索关键词
async function searchQuery() {
  try {
    const obj = { ...queryParams.value };
    delete obj.createTime;
    delete obj.upTime;
    timeString(obj, "createTime");
    timeString(obj, "upTime");
    if (obj.createdept && Array.isArray(obj.createdept)) {
      obj.createdept = queryParams.value.createdept
        .map((item) => {
          return item.label;
        })
        .join();
    }
    await getList(obj);
  } catch (err) {
    throw err;
  }
}
// 导出
async function handleExport() {
  try {
    const obj = { ...queryParams.value };
    delete obj.pageNum;
    delete obj.pageSize;
    delete obj.createTime;
    delete obj.upTime;
    timeString(obj, "createTime");
    timeString(obj, "upTime");
    for (let k in obj) {
      if (Array.isArray(obj[k])) {
        if (k === "createdept") {
          obj[k] = obj[k]
            .map((item) => {
              return item.label;
            })
            .join();
        } else {
          obj[k] = obj[k].join();
        }
      }
    }
    proxy.download(
      "/flowable/rzrs/exportProAndSys",
      { ...obj },
      `需求查询_${new Date().toLocaleDateString()}.xlsx`,
    );
  } catch (error) {
    throw error;
  }
}

function autoHeight() {
  nextTick(() => {
    const container_h = document.querySelector(".app-container").offsetHeight;
    // const showSearch_h = document.querySelector(".showSearch")
    //   ? document.querySelector(".showSearch").offsetHeight
    //   : 0;
    const toolbar_h = document.querySelector(
      ".app-container .toolbar",
    ).offsetHeight;
    tableHeight.value = container_h - toolbar_h - showSearch_h.value - 65 - 40;
  });
}
const vDebounce = {
  mounted(el, binding) {
    el.addEventListener("click", debounce(binding.value, 300, false));
  },
};

onUnmounted(() => {
  window.removeEventListener("resize", autoHeight);
  if (resizeObserver) {
    resizeObserver.disconnect();
  }
});
</script>

<style lang="scss" scoped>
.app-container {
  height: calc(100vh - 100px);
  padding-top: 0;
  padding-bottom: 0;
  ::v-deep .el-form-item {
    margin-bottom: 10px;
  }
  .nameClickSty {
    color: #409eff;
    cursor: pointer;
  }
}
.pagintion {
  margin-top: 10px;
  margin-bottom: 10px;
  float: right;
}
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.4;
}
.status-dev {
  background-color: #e6f7ff;
  color: #1890ff;
}
.status-finish {
  background-color: #e6f7ff;
  color: #67c23a;
}
.status-cancel {
  background-color: #e6f7ff;
  color: #e6a23c;
}
.bg {
  background-color: #409eff;
  color: #fff;
}
.list-item {
  display: inline-block;
  position: relative;
}
.mini-icon {
  font-size: 12px;
  position: absolute;
  top: 0;
  left: -10px;
  cursor: pointer;
}
.flex-center {
  display: flex;
  align-items: center;
  .mb-10 {
    margin-bottom: 10px;
    margin-left: 10px;
  }
}
/* .el-tag .is-closable .el-tag--info .el-tag--default .el-tag--light {
  max-width: 84px !important;
} */
:deep(.el-select__selected-item) {
  .el-tag {
    max-width: 83px !important;
  }
}
/* .el-tag.is-closable {
  padding-right: 5px;
} */
.toolbar {
  .filter {
    margin-bottom: 0;
    margin-right: 20px;
  }
  align-items: center;
  ::v-deep .filter-table {
    margin-bottom: 0 !important;
  }
}
::v-deep .el-checkbox-group {
  padding: 10px !important;
}
</style>

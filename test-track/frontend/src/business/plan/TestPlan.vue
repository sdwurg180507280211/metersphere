<template>
  <ms-container>
    <ms-aside-container page-key="TEST_PLAN_LIST" max-width="600px" :enable-aside-hidden.sync="enableAsideHidden" class="plan-aside">
      <test-plan-node-tree ref="planNodeTree" :plan-condition="condition" @setTreeNodes="setTreeNodes"
                                   @nodeSelectEvent="handleCaseNodeSelect" @refreshTable="refreshTestPlanList"/>
    </ms-aside-container>

    <ms-main-container>
      <test-plan-list
        @openTestPlanEditDialog="openTestPlanEditDialog"
        @testPlanEdit="openTestPlanEditDialog"
        @refreshTree="refreshTreeByCondition"
        @setCondition="setCondition"
        :current-node="currentNode"
        :current-select-nodes="currentSelectNodes"
        :tree-nodes="treeNodes"
        ref="testPlanList"/>
    </ms-main-container>

    <test-plan-edit ref="testPlanEditDialog" @refresh="refreshTestPlanList"/>
  </ms-container>
</template>

<script>
import {TEST_PLAN_CONFIGS} from "metersphere-frontend/src/components/search/search-components";
import TestPlanNodeTree from "@/business/module/TestPlanNodeTree.vue";
import TestPlanList from './components/TestPlanList';
import TestPlanEdit from './components/TestPlanEdit';
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsAsideContainer from "metersphere-frontend/src/components/MsAsideContainer";
import MsMainContainer from "metersphere-frontend/src/components/MsMainContainer";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";
import TestCaseReviewList from "@/business/review/components/TestCaseReviewList.vue";

const TEST_PLAN_LIST_STATE_KEY = "TEST_PLAN_LIST_RETURN_STATE";

export default {
  name: "TestPlan",
  components: {
    TestCaseReviewList,
    TestPlanNodeTree, MsMainContainer, MsAsideContainer, MsContainer, TestPlanList, TestPlanEdit},
  data() {
    return {
      condition: {},
      currentNode: null,
      currentSelectNodes: [],
      enableAsideHidden: true,
      treeNodes: [],
      restoreNodeId: null,
    };
  },
  computed: {
    projectId() {
      return getCurrentProjectID();
    },
  },
  beforeRouteLeave(to, from, next) {
    if (to.path.indexOf("/track/plan/view/") >= 0) {
      this.saveReturnState();
    }
    next();
  },
  mounted() {
    if (this.$route.path.indexOf("/track/plan/create") >= 0) {
      this.openTestPlanEditDialog();
      this.$router.push('/track/plan/all');
    } else if (this.$route.query.restoreState === 'true') {
      this.restoreReturnState();
    }
  },
  watch: {
    '$route'(to, from) {
      if (to.path.indexOf("/track/plan/create") >= 0) {
        if (!this.projectId) {
          this.$warning(this.$t('commons.check_project_tip'));
          return;
        }
        this.openTestPlanEditDialog();
        this.$router.push('/track/plan/all');
      } else if (to.path.indexOf("/track/plan/all") >= 0) {
        if (to.query.restoreState === 'true') {
          this.restoreReturnState();
          return;
        }
        // 清空模块树相关参数
        this.currentNode = null;
        this.currentSelectNodes = [];
        this.$refs.planNodeTree.currentNode = {};
      }
    }
  },
  methods: {
    getReturnStateKey() {
      return TEST_PLAN_LIST_STATE_KEY + "_" + (this.projectId || "default");
    },
    saveReturnState() {
      const list = this.$refs.testPlanList;
      const nodeId = this.currentNode && this.currentNode.data ? this.currentNode.data.id : null;
      const state = {
        nodeId: nodeId,
        currentSelectNodes: this.currentSelectNodes || [],
        condition: list && list.condition ? list.condition : this.condition,
        currentPage: list ? list.currentPage : 1,
        pageSize: list ? list.pageSize : 10,
      };
      sessionStorage.setItem(this.getReturnStateKey(), JSON.stringify(state));
    },
    restoreReturnState() {
      const stateText = sessionStorage.getItem(this.getReturnStateKey());
      if (!stateText) {
        return;
      }
      try {
        const state = JSON.parse(stateText);
        this.restoreNodeId = state.nodeId;
        this.currentSelectNodes = state.currentSelectNodes || [];
        this.condition = state.condition || {};
        if (this.restoreNodeId) {
          this.currentNode = {data: {id: this.restoreNodeId}};
        }
        this.$nextTick(() => {
          const list = this.$refs.testPlanList;
          if (list) {
            list.condition = state.condition || list.condition;
            list.currentPage = state.currentPage || 1;
            list.pageSize = state.pageSize || list.pageSize;
            list.initTableData(this.currentSelectNodes);
          }
          this.applyRestoredNode();
        });
      } catch (e) {
        sessionStorage.removeItem(this.getReturnStateKey());
      }
    },
    findNodeById(nodes, id) {
      if (!nodes || !id) {
        return null;
      }
      for (let node of nodes) {
        if (node.id === id) {
          return node;
        }
        const child = this.findNodeById(node.children, id);
        if (child) {
          return child;
        }
      }
      return null;
    },
    applyRestoredNode() {
      if (!this.restoreNodeId || !this.$refs.planNodeTree) {
        return;
      }
      const nodeData = this.restoreNodeId === 'root'
        ? {id: 'root'}
        : this.findNodeById(this.treeNodes, this.restoreNodeId);
      if (nodeData) {
        this.currentNode = {data: nodeData};
        this.$refs.planNodeTree.currentNode = this.currentNode;
        this.$nextTick(() => this.$refs.planNodeTree.justSetCurrentKey());
      }
    },
    setTreeNodes(data) {
      this.treeNodes = data;
      this.applyRestoredNode();
    },
    setCondition(data) {
      this.condition = data;
    },
    openTestPlanEditDialog(data) {
      this.$refs.testPlanEditDialog.openTestPlanEditDialog(data, this.currentNode);
    },
    refreshTestPlanList(nodeIds) {
      this.$refs.testPlanList.condition = {components: TEST_PLAN_CONFIGS};
      this.$refs.testPlanList.initTableData(nodeIds ? nodeIds : this.currentSelectNodes);
    },
    refreshTreeByCondition() {
      this.$refs.planNodeTree.list();
    },
    handleCaseNodeSelect(node, nodeIds, pNodes) {
      this.currentNode = node;
      this.currentSelectNodes = nodeIds;
      this.$refs.testPlanList.initTableData(nodeIds);
    }
  }
};
</script>

<style>
.plan-aside .hiddenBottom {
  top: 300px!important;
}

.plan-aside .el-icon-arrow-left:before {
  font-size: 17px;
}

.plan-aside .el-icon-arrow-right:before {
  font-size: 17px;
}

.plan-aside .hiddenBottom i {
  margin-left: -4px;
  margin-top: 18px;
}

.plan-aside .node-tree {
  height: calc(100vh - 125px)!important;
}

.plan-aside .ms-aside-node-tree {
  height: 100%!important;
}
</style>
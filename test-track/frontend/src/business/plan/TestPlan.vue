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
import TestPlanList from './components/TestPlanListWithReturnState';
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
      treeLoaded: false,
      restoreNodeId: null,
      restoringReturnState: false,
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
      const list = this.$refs.testPlanList;
      if (list && list.prepareReturnStateRestore) {
        list.prepareReturnStateRestore();
      }
    }
    next();
  },
  mounted() {
    if (this.$route.path.indexOf("/track/plan/create") >= 0) {
      this.openTestPlanEditDialog();
      this.$router.push('/track/plan/all');
    } else if (this.isRestoreRoute(this.$route)) {
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
        if (this.isRestoreRoute(to)) {
          this.restoreReturnState();
          return;
        }
        this.resetModuleSelection();
      }
    }
  },
  methods: {
    isRestoreRoute(route) {
      return route && route.query && route.query.restoreState === 'true';
    },
    cloneReturnStateValue(value, fallback) {
      if (value === undefined || value === null) {
        return fallback;
      }
      return JSON.parse(JSON.stringify(value));
    },
    getReturnStateKey() {
      return TEST_PLAN_LIST_STATE_KEY + "_" + (this.projectId || "default");
    },
    saveReturnState() {
      const list = this.$refs.testPlanList;
      const nodeId = this.currentNode && this.currentNode.data ? this.currentNode.data.id : null;
      const state = {
        nodeId,
        currentSelectNodes: this.currentSelectNodes || [],
        condition: list && list.condition ? list.condition : this.condition,
        currentPage: list ? list.currentPage : 1,
        pageSize: list ? list.pageSize : 10,
      };
      sessionStorage.setItem(this.getReturnStateKey(), JSON.stringify(state));
    },
    restoreReturnState() {
      if (this.restoringReturnState) {
        return;
      }
      this.restoringReturnState = true;
      const stateKey = this.getReturnStateKey();
      const stateText = sessionStorage.getItem(stateKey);
      sessionStorage.removeItem(stateKey);
      if (!stateText) {
        this.initializeDefaultList();
        this.finishReturnStateRestore();
        return;
      }
      try {
        const state = JSON.parse(stateText);
        const list = this.$refs.testPlanList;
        if (!list) {
          this.$nextTick(() => {
            this.restoringReturnState = false;
            this.restoreReturnStateFromState(state);
          });
          return;
        }
        this.restoreReturnStateFromState(state);
      } catch (e) {
        this.initializeDefaultList();
        this.finishReturnStateRestore();
      }
    },
    restoreReturnStateFromState(state) {
      const list = this.$refs.testPlanList;
      if (!list) {
        this.initializeDefaultList();
        this.finishReturnStateRestore();
        return;
      }
      const restoredCondition = this.cloneReturnStateValue(state.condition, {});
      const restoredNodeIds = this.cloneReturnStateValue(state.currentSelectNodes, []);
      const restoredPage = Number.isInteger(state.currentPage) && state.currentPage > 0
        ? state.currentPage
        : 1;
      const restoredPageSize = Number.isInteger(state.pageSize) && state.pageSize > 0
        ? state.pageSize
        : list.pageSize;
      this.restoreNodeId = state.nodeId || 'root';
      this.currentSelectNodes = restoredNodeIds;
      this.condition = this.cloneReturnStateValue(restoredCondition, {});
      this.currentNode = {data: {id: this.restoreNodeId}};
      list.condition = this.cloneReturnStateValue(restoredCondition, {});
      list.currentPage = restoredPage;
      list.pageSize = restoredPageSize;
      list.restoreTableData(this.currentSelectNodes);
      this.$nextTick(() => {
        const restoredList = this.$refs.testPlanList;
        if (!restoredList) {
          return;
        }
        restoredList.condition = this.cloneReturnStateValue(restoredCondition, {});
        restoredList.currentPage = restoredPage;
        restoredList.pageSize = restoredPageSize;
        this.condition = restoredList.condition;
      });
      this.finishReturnStateRestore();
    },
    initializeDefaultList() {
      const list = this.$refs.testPlanList;
      if (list) {
        list.restoreTableData([]);
      } else {
        this.$nextTick(() => {
          const nextList = this.$refs.testPlanList;
          if (nextList) {
            nextList.restoreTableData([]);
          }
        });
      }
    },
    finishReturnStateRestore() {
      this.restoringReturnState = false;
      this.clearRestoreQuery();
    },
    clearRestoreQuery() {
      if (!this.isRestoreRoute(this.$route)) {
        return;
      }
      const query = {...this.$route.query};
      delete query.restoreState;
      const resolved = this.$router.resolve({
        path: this.$route.path,
        query,
      });
      window.history.replaceState(window.history.state, '', resolved.href);
    },
    resetModuleSelection() {
      this.restoreNodeId = null;
      this.currentNode = null;
      this.currentSelectNodes = [];
      if (this.$refs.planNodeTree) {
        this.$refs.planNodeTree.currentNode = {};
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
      if (!this.restoreNodeId || !this.treeLoaded || !this.$refs.planNodeTree) {
        return;
      }
      const restoreNodeId = this.restoreNodeId;
      const nodeData = restoreNodeId === 'root'
        ? {id: 'root'}
        : this.findNodeById(this.treeNodes, restoreNodeId);
      if (nodeData) {
        this.currentNode = {data: nodeData};
        this.$refs.planNodeTree.currentNode = this.currentNode;
        this.$nextTick(() => this.$refs.planNodeTree.justSetCurrentKey());
        this.restoreNodeId = null;
        return;
      }
      this.restoreNodeId = null;
      this.currentSelectNodes = [];
      this.currentNode = {data: {id: 'root'}};
      this.$refs.planNodeTree.currentNode = this.currentNode;
      this.$nextTick(() => this.$refs.planNodeTree.justSetCurrentKey());
      const list = this.$refs.testPlanList;
      if (list) {
        list.currentPage = 1;
        list.restoreTableData([]);
      }
    },
    setTreeNodes(data) {
      this.treeNodes = data || [];
      this.treeLoaded = true;
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
      this.restoreNodeId = null;
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

import BaseTestPlanList from "./TestPlanList";
import {deepClone} from "metersphere-frontend/src/utils/tableUtils";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";
import {testPlanList, testPlanMetric} from "@/api/remote/plan/test-plan";

export default {
  name: "TestPlanListWithReturnState",
  extends: BaseTestPlanList,
  data() {
    return {
      tableRequestId: 0,
      restoreQueryReady: !this.isReturnRestoreRoute(),
    };
  },
  methods: {
    isReturnRestoreRoute() {
      return this.$route.query && this.$route.query.restoreState === "true";
    },
    prepareReturnStateRestore() {
      this.restoreQueryReady = false;
    },
    restoreTableData(nodeIds) {
      this.restoreQueryReady = true;
      return this.initTableData(nodeIds);
    },
    initTableData(nodeIds) {
      if (this.isReturnRestoreRoute() && !this.restoreQueryReady) {
        return Promise.resolve();
      }
      const requestId = ++this.tableRequestId;
      this.cardLoading = true;
      this.condition.nodeIds = [];
      if (this.planId) {
        this.condition.planId = this.planId;
      }
      if (this.selectNodeIds && this.selectNodeIds.length > 0) {
        this.condition.nodeIds = [...this.selectNodeIds];
      }
      if (nodeIds && Array.isArray(nodeIds) && nodeIds.length > 0) {
        this.condition.nodeIds = [...nodeIds];
      }
      if (!this.projectId) {
        if (requestId === this.tableRequestId) {
          this.cardLoading = false;
        }
        return Promise.resolve();
      }
      this.condition.projectId = getCurrentProjectID();
      this.$emit("setCondition", this.condition);
      this.$emit("refreshTree");
      const requestCondition = deepClone(this.condition);
      return testPlanList(
        {pageNum: this.currentPage, pageSize: this.pageSize},
        requestCondition
      ).then((response) => {
        if (requestId !== this.tableRequestId) {
          return;
        }
        const data = response.data;
        this.total = data.itemCount;
        const testPlanIds = [];
        data.listObject.forEach((item) => {
          testPlanIds.push(item.id);
          if (item.tags) {
            item.tags = JSON.parse(item.tags);
            if (item.tags.length === 0) {
              item.tags = null;
            }
          }
          if (item.principalUsers) {
            const principalUsers = item.principalUsers;
            let principal = "";
            const principalIds = principalUsers.map((user) => user.id);
            principalUsers.forEach((user) => {
              principal = principal ? principal + "、" + user.name : user.name;
            });
            this.$set(item, "principalName", principal);
            this.$set(item, "principals", principalIds);
          }
        });
        this.tableData = data.listObject;
        this.getTestPlanDetailData(testPlanIds, requestId);
      }).catch(() => {
        if (requestId === this.tableRequestId) {
          this.total = 0;
          this.tableData = [];
        }
      }).finally(() => {
        if (requestId === this.tableRequestId) {
          this.cardLoading = false;
        }
      });
    },
    getTestPlanDetailData(testPlanIds, requestId = this.tableRequestId) {
      if (requestId !== this.tableRequestId || !testPlanIds || testPlanIds.length === 0) {
        return;
      }
      testPlanMetric(testPlanIds)
        .then((res) => {
          if (requestId !== this.tableRequestId) {
            return;
          }
          const metricDataList = res.data;
          if (metricDataList) {
            this.tableData.forEach((item) => {
              const metricData = metricDataList.find((metricItem) => item.id === metricItem.id);
              if (metricData) {
                this.$set(item, "isMetricLoadOver", true);
                this.$set(item, "passRate", metricData.passRate + "%");
                this.$set(item, "testRate", metricData.testRate);
                this.$set(item, "passed", metricData.passed);
                this.$set(item, "tested", metricData.tested);
                this.$set(item, "total", metricData.total);
                if (metricData.followUsers) {
                  const followUsers = metricData.followUsers;
                  let follow = "";
                  const followIds = followUsers.map((user) => user.id);
                  let showFollow = false;
                  followUsers.forEach((user) => {
                    follow = follow ? follow + "、" + user.name : user.name;
                    if (this.currentUser().id === user.id) {
                      showFollow = true;
                    }
                  });
                  this.$set(item, "follow", follow);
                  this.$set(item, "follows", followIds);
                  this.$set(item, "showFollow", showFollow);
                }
              } else {
                this.resetTestPlanRow(item);
              }
            });
          }
        })
        .catch(() => {
          if (requestId !== this.tableRequestId) {
            return;
          }
          this.tableData.forEach((item) => {
            this.resetTestPlanRow(item);
          });
        });
    },
  },
};

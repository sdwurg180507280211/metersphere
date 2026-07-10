<template>
  <div class="test-plan-header-bar">
    <div class="header-back" v-if="$slots.back">
      <slot name="back"></slot>
    </div>
    <div class="header-info">
      <slot name="info"></slot>
    </div>
    <div class="menu-ul">
      <slot name="menu"></slot>
    </div>
  </div>
</template>

<script>


import SelectMenu from "../../../../common/SelectMenu";
export default {
  name: "MsTestPlanHeaderBar",
  components: {SelectMenu},
  data() {
    return {
    }
  },
  props: {
  },
  methods: {
  }
}

</script>

<style scoped>

.test-plan-header-bar {
  height: 50px;
  display: flex;
  align-items: center;
  background-color: #FFF;
  border-bottom: 2px solid #E6E6E6;
}

.header-back {
  flex: 0 0 auto;
  padding: 0 12px;
}

.header-info {
  height: 50px;
  width: 300px;
  line-height: 50px;
  vertical-align: top;
  display: inline-block;
}

.menu-ul {
  width: 600px;
  display: inline-block;
}

.menu-ul :deep(.el-menu-item) {
  height: 51px !important;
}

</style>

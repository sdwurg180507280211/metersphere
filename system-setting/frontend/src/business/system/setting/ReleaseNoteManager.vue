<template>
  <!-- 上线记录管理组件 -->
  <div v-loading="loading">
    <!-- 操作栏：新增上线记录 + 横幅公告设置 在同一行 -->
    <el-row style="margin-bottom: 10px;" type="flex" justify="space-between" align="middle">
      <el-col :span="12">
        <el-button icon="el-icon-circle-plus-outline" plain size="mini" v-permission="['SYSTEM_SETTING:READ+EDIT']" @click="handleAdd">
          {{ $t('announcement.release_note_add') }}
        </el-button>
      </el-col>
      <el-col :span="12" style="text-align: right;">
        <el-button type="primary" icon="el-icon-s-flag" size="mini" v-permission="['SYSTEM_SETTING:READ+EDIT']" @click="$emit('openBanner')">
          {{ $t('announcement.banner_setting') }}
        </el-button>
      </el-col>
    </el-row>

    <el-table border class="adjust-table" :data="tableData" style="width: 100%">
      <el-table-column prop="title" :label="$t('announcement.release_note_title')" show-overflow-tooltip />
      <el-table-column prop="creator" :label="$t('announcement.release_note_creator')" width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" :label="$t('announcement.release_note_create_time')" width="180">
        <template v-slot:default="scope">
          <span>{{ scope.row.createTime | datetimeFormat }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('commons.operating')" width="150">
        <template v-slot:default="scope">
          <el-button type="primary" icon="el-icon-edit" size="mini" circle v-permission="['SYSTEM_SETTING:READ+EDIT']" @click="handleEdit(scope.row)" />
          <el-button type="danger" icon="el-icon-delete" size="mini" circle v-permission="['SYSTEM_SETTING:READ+EDIT']" @click="handleDelete(scope.row)" />
        </template>
      </el-table-column>
    </el-table>

    <ms-table-pagination :change="loadData" :current-page.sync="currentPage" :page-size.sync="pageSize" :total="total" />
    <release-note-dialog ref="releaseNoteDialog" @save="handleSave" />
  </div>
</template>

<script>
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import ReleaseNoteDialog from "./ReleaseNoteDialog";
import {addReleaseNote, updateReleaseNote, deleteReleaseNote, listReleaseNotes} from "../../../api/release-note";

export default {
  name: "ReleaseNoteManager",
  components: {MsTablePagination, ReleaseNoteDialog},
  data() {
    return {
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    loadData() {
      this.loading = true;
      listReleaseNotes(this.currentPage, this.pageSize)
        .then(res => {
          let data = res.data;
          this.tableData = data.listObject;
          this.total = data.itemCount;
        })
        .finally(() => { this.loading = false; });
    },
    handleAdd() {
      this.$refs.releaseNoteDialog.open();
    },
    handleEdit(row) {
      this.$refs.releaseNoteDialog.open(row);
    },
    handleDelete(row) {
      this.$confirm(this.$t('commons.confirm_delete'), this.$t('commons.prompt'), {
        confirmButtonText: this.$t('commons.confirm'),
        cancelButtonText: this.$t('commons.cancel'),
        type: 'warning'
      }).then(() => {
        this.loading = true;
        deleteReleaseNote(row.id)
          .then(() => {
            this.$success(this.$t('commons.delete_success'));
            this.loadData();
          })
          .finally(() => { this.loading = false; });
      }).catch(() => {});
    },
    handleSave(formData) {
      this.loading = true;
      let request = formData.id ? updateReleaseNote(formData) : addReleaseNote(formData);
      request
        .then(() => {
          this.$success(this.$t('commons.save_success'));
          this.loadData();
        })
        .catch(() => { this.$error(this.$t('commons.save_failed')); })
        .finally(() => { this.loading = false; });
    }
  }
};
</script>

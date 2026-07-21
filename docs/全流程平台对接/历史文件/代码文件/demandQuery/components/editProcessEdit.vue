<template>
  <el-dialog
    v-model="dialogVisible"
    width="60%"
    :destroy-on-close="true"
    :close-on-click-modal="false"
  >
    <template #header>
      <div class="my-header">
        <div class="title">修改流程信息</div>
        <div class="notice">
          请特别注意：该弹窗主要是对需求的常用信息和开发任务调整做修改。若当前存在开发任务才会显示开发相关内容。如若不涉及调整开发任务，请保证 “是否修改开发任务” 为 "否"
        </div>
      </div>
    </template>
    <div v-loading="loading">
      <el-form :model="form" label-width="150" style="width: 100%;">
        <el-form-item label="需求名称">
          <el-input v-model="form.Name" placeholder="请输入需求名称" clearable/>
        </el-form-item>
        <el-form-item label="是否专家评审" v-if="form.isZjps">
          <el-radio-group v-model="form.isZjps">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主需求预计上线时间" >
          <el-date-picker
            v-model="form.duetime"
            type="date"
            placeholder="请选择"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            style="width: 300px;"
          />
        </el-form-item>
        <el-form-item label="子需求预计上线时间">
          <el-date-picker
            v-model="form.plannedCompletionTime"
            type="date"
            placeholder="请选择"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            style="width: 300px;"
          />
        </el-form-item>
        <el-form-item label="需求申请部门">
          <el-select 
            v-model="form.createdept" 
            placeholder="请选择" 
            filterable clearable 
            style="width: 300px;"
            @change="getApplicationUsers">
            <el-option 
              v-for="item in applicationDept" :key="item.deptId"
              :label="item.deptName"
              :value="item.deptName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="需求申请人">
          <el-select 
            v-model="form.createuser" 
            placeholder="请选择" 
            filterable clearable 
            :disabled="!applicationUsers.length"
            style="width: 300px;"
            @change="getcreateusers">
            <el-option 
              v-for="item in applicationUsers" :key="item.id"
              :label="item.nickName"
              :value="item.nickName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="需求大类">
          <el-select 
            v-model="form.reqFatherClass" 
            placeholder="请选择" 
            clearable 
            style="width: 300px;"
            @change="handleChangeReqFatherClass">
            <el-option label="系统优化" value="系统优化" />
            <el-option label="需求" value="需求" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求子类" v-if="form.reqFatherClass === '需求'">
          <el-select 
            v-model="form.reqSonClass" 
            placeholder="请选择" 
            clearable 
            style="width: 300px;"
            @change="handleChangeReqSonClass">
            <el-option label="新产品" value="新产品" />
            <el-option label="日常需求" value="日常需求" />
            <el-option label="项目" value="项目" />
            <el-option label="银保通产品" value="银保通产品" />
          </el-select>
        </el-form-item>
        <el-form-item label="需求确认单号" v-if="form.reqFatherClass === '需求' && (form.reqSonClass === '新产品' || form.reqSonClass === '银保通产品' || form.reqSonClass === '日常需求')">
          <el-input v-model="form.reqConfirmationNum" placeholder="请输入需求确认单号" clearable style="width: 300px;"/>
        </el-form-item>
        <el-form-item label="需求申请单号" v-if="form.reqFatherClass === '需求' && (form.reqSonClass === '新产品' || form.reqSonClass === '银保通产品' || form.reqSonClass === '日常需求')">
          <el-input v-model="form.reqApplicationNum" placeholder="请输入申请单号" clearable style="width: 300px;"/>
        </el-form-item>
        <el-form-item label="立项签报号" v-if="form.reqFatherClass === '需求' && form.reqSonClass === '项目'">
          <el-input v-model="form.projectAppNum" placeholder="请输入申请单号" clearable style="width: 300px;"/>
        </el-form-item>
        <el-form-item label="是否修改开发任务" v-if="originalData.takInfo && originalData.takInfo.length">
          <el-radio-group v-model="form.isEdit">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label-width="0" v-if="form.takInfo && form.takInfo.length && form.isEdit == '1'">
          <el-table :data="form.takInfo" style="width: 100%">
            <el-table-column width="200">
              <template #header>
                <span style="margin-right: 10px;">操作</span>
                <el-button link type="primary" @click="addRow">新增</el-button>
                <el-button link type="danger" @click="openDateDialog">关闭</el-button>
              </template>
              <!-- <template #default="scope">
                <el-button link icon="Delete" type="danger" @click.prevent="deleteRow(scope.$index)">删除</el-button>
              </template> -->
              <template #default="scope">
                <el-popconfirm title="是否取消流程？" @confirm="handleOperation(scope.row, '1')">
                  <template #reference>
                    <el-button link type="warning">取消</el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm title="是否挂起流程？" @confirm="handleOperation(scope.row, '2')">
                  <template #reference>
                    <el-button link type="primary">挂起</el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm title="是否激活流程？" @confirm="handleOperation(scope.row, '3')">
                  <template #reference>
                    <el-button link type="primary">激活</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <el-table-column prop="date" label="人员选择" width="240">
              <template #default="scope">
                <el-select v-model="scope.row.userSelect"  placeholder="请选择" filterable clearable @change="ChangeSys">
                  <el-option
                    v-for="item in selectList" :key="item.value" 
                    :label="item.label" :value="item.value"
                    :disabled="isOptionDisabled(item.value, scope.$index)"
                    />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="开发内容">
              <template #default="scope">
                <el-input v-model="scope.row.textarea67425" :autosize="{ minRows: 1 }" type="textarea" placeholder="请输入"/>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取 消</el-button>
        <el-button type="primary" @click="onSubmit">保 存</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="dateDialogVisible" title="请选择关闭时间" width="500"  class="date-dialog">
    <el-date-picker 
      format="YYYY-MM-DD" 
      value-format="YYYY-MM-DD" 
      v-model="closeDate" 
      type="date" 
      placeholder="请选择" 
    />
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dateDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="cancelOperation">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="editProcessEdit">
import { selectProcInfo, reqVendorMapping, updateProcInfo, developerTaskOperation, closeDeveloperTasks } from '@/api/flowable/demand'
import { getDeptList, getUserList } from '@/api/demandQuery/demandQuery'
import { ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()
const dialogVisible = ref(false)
const form = ref({
  Name: '',
  isZjps: '',
  takInfo: [],
  isEdit: '0',
  plannedCompletionTime: '',
  duetime: ''
})
const originalData = ref({})
const loading = ref(false)
const selectList = ref([])
const selectedUser = ref([])  // 记录已选中的人员
const emit = defineEmits(['updateList'])
const processInsId = ref('')
const applicationDept = ref([])  // 需求申请部门
const applicationUsers = ref([]) // 需求申请人

// 取消 挂起 激活流程
async function handleOperation(row, type) {
  const params = {
    procInsId: processInsId.value,
    taskId: row.TaskId,
    variables: {
      operation: type
    }
  }
  await developerTaskOperation(params)
  proxy.$modal.msgSuccess("操作成功")
}

const dateDialogVisible = ref(false)
const closeDate = ref('')
// 打开关闭弹窗
function openDateDialog() {
  closeDate.value = getCurrentDate()
  dateDialogVisible.value = true
}
// 关闭流程
async function cancelOperation() {
  if(!closeDate.value) {
    return proxy.$modal.msgWarning("关闭时间不能为空")
  }
  const params = {
    procInsId: processInsId.value,
    kfEndDate: closeDate.value
  }
  // console.log('关闭流程参数', params)
  await closeDeveloperTasks(params)
  proxy.$modal.msgSuccess("关闭成功")
  dateDialogVisible.value = false
}

// 提交
async function onSubmit() {
  // 对表单内容进行对比
  const data = {}
  data.Name = form.value.Name === originalData.value.Name ? '' : form.value.Name
  data.isZjps = form.value.isZjps === originalData.value.isZjps ? '' : form.value.isZjps
  data.isEdit = form.value.isEdit
  data.plannedCompletionTime = form.value.plannedCompletionTime ? form.value.plannedCompletionTime : ''
  data.duetime = form.value.duetime ? form.value.duetime : ''
  // 校验人员选择和开发内容
  if(form.value.isEdit == '1') {
    for(let i = 0; i < form.value.takInfo.length; i++) {
      if(!form.value.takInfo[i].userSelect) return proxy.$modal.msgWarning("人员选择不能为空")
      if(!form.value.takInfo[i].textarea67425) return proxy.$modal.msgWarning("开发内容不能为空")
    }
  }
  if (form.value.reqFatherClass === '需求' && form.value.reqSonClass === '项目') {
    if(!form.value.projectAppNum) return proxy.$modal.msgWarning('立项签报号不能为空')
  }
  data.takInfo = form.value.takInfo
  data.processInsId = processInsId.value
  data.createdept = form.value.createdept ? form.value.createdept : ''
  data.createdeptid = form.value.createdeptid ? form.value.createdeptid : ''
  data.createuser = form.value.createuser ? form.value.createuser : ''
  data.createuserid = form.value.createuserid ? form.value.createuserid : ''
  data.reqFatherClass = form.value.reqFatherClass ? form.value.reqFatherClass : ''
  data.reqSonClass = form.value.reqSonClass ? form.value.reqSonClass : ''
  data.reqConfirmationNum = form.value.reqConfirmationNum ? form.value.reqConfirmationNum : ''
  data.reqApplicationNum = form.value.reqApplicationNum ? form.value.reqApplicationNum : ''
  data.projectAppNum = form.value.projectAppNum ? form.value.projectAppNum : ''
  console.log('提交参数', data)
  ElMessageBox.alert('是否要保存提交', '系统提示', {
    confirmButtonText: '确 认',
    showCancelButton: true,
  }).then(async() => {
      await updateProcInfo(JSON.stringify(data))
      proxy.$modal.msgSuccess("修改成功")
      emit('updateList')
      dialogVisible.value = false
  })
}
// 获取需求申请部门数据
async function getApplicationDept() {
  const { data } = await getDeptList()
  applicationDept.value = data
}
// 获取需求申请人数据
async function getApplicationUsers(value) {
  const dept = applicationDept.value.filter(item => item.deptName == value)
  console.log(dept, value)
  if(!dept || dept.length == 0) {
    applicationUsers.value = []
  }else {
    const { data } = await getUserList({ deptId: dept[0].deptId })
    applicationUsers.value = data
  }
  form.value.createuser = ''
  form.value.createdeptid = dept[0].deptId
}
// 更改需求申请人数据
function getcreateusers(value) {
  const user = applicationUsers.value.filter(item => item.nickName == value)
  console.log(user, value)
  form.value.createuserid = user[0].id;
}
// 修改需求大类
function handleChangeReqFatherClass(val) {
  form.value.reqSonClass = ''
}
// 修改需求子类
function handleChangeReqSonClass(val) {
  /*if(val == '新产品' || val == '银保通产品' || val == '日常需求') {
    form.value.projectAppNum = ''
  }else if(val == '项目') {
    form.value.reqConfirmationNum = ''
    form.value.reqApplicationNum = ''
  }else {
    form.value.projectAppNum = ''
    form.value.reqConfirmationNum = ''
    form.value.reqApplicationNum = ''
  }*/
}

// 获取表格数据
async function getSystemList() {
  await nextTick()
  const { data } = await reqVendorMapping()
  // 系统表格赋值
  selectList.value = data.map(item => {
    return {
      value: item.userId,
      label: item.nickName + '/' + item.userName
    }
  })
}
// 新增
function addRow() {
  form.value.takInfo.push({
    userSelect: '',
    textarea67425: ''
  })
}
// 删除
function deleteRow(index) {
  form.value.takInfo.splice(index, 1)
  selectedUser.value = form.value.takInfo.map(item => item.userSelect)
}
// 判断选项是否应该被禁用
function isOptionDisabled(value, currentRowIndex) {
  // 当前选中的不禁用
  if(form.value.takInfo[currentRowIndex].userSelect == value) {
    return false
  }
  // 其他选中的禁用
  return selectedUser.value.includes(value)
}
function ChangeSys() {
  selectedUser.value = form.value.takInfo.map(item => item.userSelect)
}
// 打开弹窗
async function openDialog(row) {
  processInsId.value = row.processInsId
  dialogVisible.value = true
  loading.value = true
  try{
    const { data } = await selectProcInfo({ processInsId: row.processInsId })
    originalData.value = JSON.parse(JSON.stringify(data))
    form.value.Name = data.Name
    form.value.isZjps = data.isZjps
    form.value.takInfo = data.takInfo
    form.value.plannedCompletionTime = data.plannedCompletionTime ? data.plannedCompletionTime : ''
    form.value.duetime = data.duetime ? data.duetime : ''
    form.value.isEdit = '0'
    form.value.createdept = data.createdept
    form.value.createuser = data.createuser
    form.value.reqFatherClass = data.reqFatherClass
    form.value.reqSonClass = data.reqSonClass
    form.value.reqConfirmationNum = data.reqConfirmationNum
    form.value.reqApplicationNum = data.reqApplicationNum
    form.value.projectAppNum = data.projectAppNum
    selectedUser.value = form.value.takInfo.map(item => item.userSelect)
    await getSystemList()
    await getApplicationDept()
  }catch(error){
    dialogVisible.value = false
  }finally {
    loading.value = false
  }
}

function handleClose() {
  dialogVisible.value = false
  selectedUser.value = []
}

// 获取当前时间  YYYY-MM-DD
function getCurrentDate() {
    const date = new Date();
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
}

defineExpose({
  openDialog
})
</script>

<style lang="scss" scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.my-header {
  display: block;
  .title {
    font-size: 20px;
  }
  ::v-deep .el-dialog__title {
    font-size: 16px;
    margin: 0;
  }
  .notice {
    font-size: 14px;
    color: red;
    margin-left: 10px;
  }
}
</style>
<style lang="scss">
 .el-popconfirm {
  padding: 10px !important;
} 
</style>
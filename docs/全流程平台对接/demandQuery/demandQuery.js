import request from "@/utils/request";
// 需求
export function getDemandQueryList(params) {
  return request({
    url: "/flowable/rzrs/selectProAndSysEndList",
    method: "get",
    params,
  });
}
// 部门
export function getDeptList() {
  return request({
    url: "/flowable/rzrs/selDeptList",
    method: "get",
  });
}
// 人
export function getUserList(params) {
  return request({
    url: "/flowable/rzrs/selUserList",
    method: "get",
    params,
  });
}
// 系统
export function getSystemList() {
  return request({
    url: "/system/system/systemList",
    method: "post",
    data: {},
  });
}
// 导出
export function exportFile(params) {
  let url = "/flowable/rzrs/exportProAndSys?";
  for (let k in params) {
    if (params[k]) {
      url += `${k}=${params[k]}&`;
    }
  }
  url = url.slice(0, -1);
  return request({
    url,
    method: "post",
    responseType: "blob",
  });
}
// 系统
export function getLoops() {
  return request({
    url: "/flowable/rzrs/selectActName",
    method: "get",
  });
}
// 常用搜索
export function getSearch() {
  return request({
    url: "/flowable/rzrs/selectProSysSearch",
    method: "get",
  });
}
// 新增常用搜索
export function addSearch(data) {
  return request({
    url: "/flowable/rzrs/insertProSysSearch",
    method: "post",
    data,
  });
}
// 删除常用搜索
export function delSearch(params) {
  return request({
    url: "/flowable/rzrs/delete",
    method: "delete",
    params,
  });
}
// 修改搜索
export function editSearch(data) {
  return request({
    url: "/flowable/rzrs/updateProAndSysSearch",
    method: "put",
    data,
  });
}
// 父系统
export function getFatherSys() {
  return request({
    url: "/flowable/rzrs/selectParentName",
    method: "get",
  });
}
// 更新附件 
export function getFile(params) {
  return request({
    url: '/message/application/getFile',
    method: 'get',
    params,
  })
}

// 更新全部附件
export function getFiles() {
  return request({
    url: '/message/application/getFiles',
    method: 'get',
  })
}

// 需求确认池 更新附件
export function getConfirmFile(params) {
  return request({
    url: '/message/confirmation/getFile',
    method: 'get',
    params,
  })
}
// 需求确认池 更新全部附件
export function getConfirmFiles() {
  return request({
    url: '/message/confirmation/getFiles',
    method: 'get',
  })
}

// 用户验收池 更新附件
export function getUserAcceptFile(params) {
  return request({
    url: '/message/userAcceptanceApplication/getFile',
    method: 'get',
    params,
  })
}

// 用户验收池 更新全部附件
export function getUserAcceptFiles() {
  return request({
    url: '/message/userAcceptanceApplication/getFiles',
    method: 'get',
  })
}

// 上线审批池 更新附件 
export function getUserOnlineFile(params) {
  return request({
    url: '/message/userOnlineApplication/getFile',
    method: 'get',
    params,
  })
}
// 上线审批池 更新全部附件
export function getUserOnlineFiles() {
  return request({
    url: '/message/userOnlineApplication/getFiles',
    method: 'get',
  })
}
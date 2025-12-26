import { get, post, request } from "metersphere-frontend/src/plugins/request";

function workflowBaseUrl() {
  return `${window.location.origin}`;
}

function pocBasePath() {
  return "/workflow/poc";
}

export function deployProcess(file, name) {
  const formData = new FormData();
  formData.append("file", file);
  if (name) {
    formData.append("name", name);
  }
  return request({
    method: "POST",
    url: `${workflowBaseUrl()}${pocBasePath()}/deploy`,
    data: formData,
    headers: {
      "Content-Type": undefined,
    },
  }).then((res) => res.data);
}

export function deployProcessXml(xml, name) {
  const fileName = name || 'process.bpmn20.xml';
  const blob = new Blob([xml], { type: 'application/xml' });
  const file = new File([blob], fileName, { type: 'application/xml' });
  return deployProcess(file, fileName);
}

export function startProcess(data) {
  return post(`${workflowBaseUrl()}${pocBasePath()}/start`, data);
}

export function queryTasks(params) {
  return get(`${workflowBaseUrl()}${pocBasePath()}/tasks`, params);
}

export function completeTask(taskId, data) {
  return post(`${workflowBaseUrl()}${pocBasePath()}/tasks/${taskId}/complete`, data);
}

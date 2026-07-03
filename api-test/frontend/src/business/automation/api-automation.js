import { editScenario } from '@/api/scenario';
import { getUUID } from 'metersphere-frontend/src/utils';
import { ELEMENT_TYPE } from '@/business/automation/scenario/Setting';
import { baseSocket, getUploadConfig } from '@/api/base-network';
import { useApiStore } from '@/store';

const store = useApiStore();

function buildBodyFile(item, bodyUploadFiles, obj, bodyParam) {
  if (bodyParam) {
    bodyParam.forEach((param) => {
      if (param.files) {
        param.files.forEach((fileItem) => {
          if (fileItem.file) {
            fileItem.name = fileItem.file.name;
            obj.bodyFileRequestIds.push(item.id);
            bodyUploadFiles.push(fileItem.file);
          }
        });
      }
    });
  }
}

function setFiles(item, bodyUploadFiles, obj) {
  if (item.body) {
    buildBodyFile(item, bodyUploadFiles, obj, item.body.kvs);
    buildBodyFile(item, bodyUploadFiles, obj, item.body.binary);
  }
}

function recursiveFile(arr, bodyUploadFiles, obj) {
  if (arr) {
    arr.forEach((item) => {
      setFiles(item, bodyUploadFiles, obj);
      if (item.hashTree && item.hashTree.length > 0) {
        recursiveFile(item.hashTree, bodyUploadFiles, obj);
      }
    });
  }
}

export function getBodyUploadFiles(obj, scenarioDefinition) {
  let bodyUploadFiles = [];
  obj.bodyFileRequestIds = [];
  scenarioDefinition.forEach((item) => {
    setFiles(item, bodyUploadFiles, obj);
    if (item.hashTree && item.hashTree.length > 0) {
      recursiveFile(item.hashTree, bodyUploadFiles, obj);
    }
  });
  return bodyUploadFiles;
}

function getScenarioFiles(obj, scenarioDefinition) {
  let scenarioFiles = [];
  obj.scenarioFileIds = [];
  // 场景变量csv 文件
  if (obj.variables) {
    setVariablesFiles(obj, scenarioFiles, obj.scenarioFileIds);
  }
  //场景步骤的场景文件
  scenarioDefinition.forEach((item) => {
    if (item.variables && item.type === 'scenario' && item.referenced === 'Copy') {
      setVariablesFiles(item, scenarioFiles, obj.scenarioFileIds);
    }
  });
  return scenarioFiles;
}

function setVariablesFiles(obj, scenarioFiles, scenarioFileIds) {
  if (obj.variables) {
    obj.variables.forEach((param) => {
      if (param.type === 'CSV' && param.files) {
        param.files.forEach((item) => {
          if (item.file) {
            if (!item.id) {
              let fileId = getUUID().substring(0, 12);
              item.name = item.file.name;
              item.id = fileId;
            }
            scenarioFileIds.push(item.id);
            scenarioFiles.push(item.file);
          }
        });
      }
    });
  }
}
async function checkFile(scenarioFiles) {
  return new Promise((resolve, reject) => {
    scenarioFiles.forEach((item) => {
      let func = item
        .slice(0, 1)
        .arrayBuffer()
        .then(() => {
          resolve();
        })
        .catch(() => {
          resolve(item.name);
        });
      return func;
    });
    if (!scenarioFiles || scenarioFiles.length == 0) {
      resolve();
    }
  });
}
export async function saveScenario(url, scenario, scenarioDefinition, _this, success) {
  let bodyFiles = getBodyUploadFiles(scenario, scenarioDefinition);
  if (store.pluginFiles && store.pluginFiles.length > 0) {
    store.pluginFiles.forEach((fileItem) => {
      if (fileItem.file) {
        scenario.bodyFileRequestIds.push(fileItem.file.uid);
        bodyFiles.push(fileItem.file);
      }
    });
  }
  let scenarioFiles = getScenarioFiles(scenario, scenarioDefinition);
  let fileName = await checkFile(scenarioFiles);
  fileName = fileName || (await checkFile(bodyFiles));
  if (fileName) {
    _this.$error('[ ' + fileName + ' ]' + _this.$t('automation.document_validity_msg'));
    _this.isPreventReClick = false;
    _this.errorRefresh();
    return;
  }
  let formData = new FormData();
  if (bodyFiles) {
    bodyFiles.forEach((f) => {
      formData.append('bodyFiles', f);
    });
  }
  if (scenarioFiles) {
    scenarioFiles.forEach((f) => {
      formData.append('scenarioFiles', f);
    });
  }
  formData.append('request', new Blob([JSON.stringify(scenario)], { type: 'application/json' }));
  let config = getUploadConfig(url, formData);
  editScenario(config).then(
    (response) => {
      if (success) {
        success(response.data);
      }
    },
    (error) => {
      _this.isPreventReClick = false;
      _this.errorRefresh(error);
    }
  );
}

export function savePreciseEnvProjectIds(projectIds, envMap) {
  if (envMap != null && projectIds != null && projectIds.length > 0) {
    let keys = envMap.keys();
    for (let key of keys) {
      if (!projectIds.has(key)) {
        envMap.delete(key);
      }
    }
    for (let id of projectIds) {
      if (!envMap.get(id)) {
        envMap.set(id, '');
      }
    }
  }
}

export function scenarioSort(_this) {
  for (let i = 0; i < _this.scenarioDefinition.length; i++) {
    const scenario = _this.scenarioDefinition[i];
    // 排序
    _this.$set(scenario, 'index', i + 1);
    // 设置循环控制
    if (
      scenario.type === ELEMENT_TYPE.LoopController &&
      scenario.hashTree &&
      scenario.hashTree.length > 1
    ) {
      scenario.countController.proceed = true;
    }
    // 设置项目ID
    if (!scenario.projectId) {
      scenario.projectId = _this.projectId;
    }

    if (scenario.hashTree != undefined && scenario.hashTree.length > 0) {
      if (_this.hideTreeNode) {
        _this.hideTreeNode(scenario, scenario.hashTree);
      }
      recursiveSorting(_this, scenario.hashTree, scenario.projectId);
    }
    // 添加debug结果
    if (_this.debugResult && _this.debugResult.get(scenario.id + scenario.name)) {
      scenario.requestResult = _this.debugResult.get(scenario.id + scenario.name);
    }
  }
}

export function recursiveSorting(_this, arr, scenarioProjectId) {
  for (let i = 0; i < arr.length; i++) {
    const step = arr[i];
    _this.$set(step, 'index', i + 1);
    if (
      step.type === ELEMENT_TYPE.LoopController &&
      step.loopType === 'LOOP_COUNT' &&
      step.hashTree &&
      step.hashTree.length > 1
    ) {
      step.countController.proceed = true;
    }
    if (!step.projectId) {
      step.projectId = scenarioProjectId ? scenarioProjectId : _this.projectId;
    }
    if (step.hashTree != undefined && step.hashTree.length > 0) {
      if (_this.hideTreeNode) {
        _this.hideTreeNode(step, step.hashTree);
      }
      recursiveSorting(_this, step.hashTree, step.projectId);
    }
    // 添加debug结果
    if (_this.debugResult && _this.debugResult.get(step.id + step.name)) {
      step.requestResult = _this.debugResult.get(step.id + step.name);
    }
  }
}

export function copyScenarioRow(row, node) {
  if (!row || !node) {
    return;
  }
  const parent = node.parent;
  const hashTree = parent.data.hashTree || parent.data;
  // 深度复制
  let obj = JSON.parse(JSON.stringify(row));
  if (obj.hashTree && obj.hashTree.length > 0) {
    resetResourceId(obj.hashTree);
  }
  obj.resourceId = getUUID();
  if (obj.name) {
    obj.name = obj.name + '_copy';
  }
  const index = hashTree.findIndex((d) => d.resourceId === row.resourceId);
  if (index != -1) {
    hashTree.splice(index + 1, 0, obj);
  } else {
    hashTree.push(obj);
  }
  return obj;
}

export function resetResourceId(hashTree) {
  hashTree.forEach((item) => {
    item.resourceId = getUUID();
    if (item.hashTree && item.hashTree.length > 0) {
      resetResourceId(item.hashTree);
    }
  });
}

export function getReportMessageSocket(reportId) {
  return baseSocket(reportId);
}

export function handleCtrlSEvent(event, func) {
  if (event.keyCode === 83 && event.ctrlKey) {
    func();
    event.preventDefault();
    event.returnValue = false;
    return false;
  }
}

export function handleCtrlREvent(event, func) {
  if (event.keyCode === 82 && event.ctrlKey) {
    func();
    event.preventDefault();
    event.returnValue = false;
    return false;
  }
}

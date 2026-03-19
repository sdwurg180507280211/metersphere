import { getCurrentProjectID, getCurrentUser } from "./token";
import { CUSTOM_TABLE_HEADER } from "./default-table-header";
import { updateCustomFieldTemplate } from "../api/custom-field-template";
import i18n from "../i18n";
import Sortable from "sortablejs";
import { dateFormat, datetimeFormat } from "fit2cloud-ui/src/filters/time";
import { hasLicense } from "../utils/permission";
import { getUUID, humpToLine } from "./index";
import {
  CUSTOM_FIELD_TYPE_OPTION,
  SYSTEM_FIELD_NAME_MAP,
} from "./table-constants";
import { generateColumnKey } from "../components/search/custom-component";

export function _handleSelectAll(
  component,
  selection,
  tableData,
  selectRows,
  condition
) {
  selectRows.clear();
  if (selection.length > 0) {
    selection.forEach((item) => {
      selectRows.add(item);
    });
    if (selection.length === 1) {
      selection.hashTree = [];
      tableData.forEach((item) => {
        component.$set(item, "showMore", true);
      });
    } else {
      tableData.forEach((item) => {
        item.hashTree = [];
        component.$set(item, "showMore", true);
      });
    }
  } else {
    selectRows.clear();
    tableData.forEach((item) => {
      component.$set(item, "showMore", false);
    });
  }
  if (condition) {
    condition.selectAll = false;
  }
}

export function _handleSelect(component, selection, row, selectRowMap) {
  row.hashTree = [];
  if (selectRowMap.has(row.id)) {
    component.$set(row, "showMore", false);
    selectRowMap.delete(row.id);
  } else {
    component.$set(row, "showMore", true);
    selectRowMap.set(row.id, row);
  }
  let arr = Array.from(selectRowMap.values());
  arr.forEach((row) => {
    component.$set(row, "showMore", true);
  });
}

// 设置 unSelectIds 查询条件，返回当前选中的条数
export function setUnSelectIds(tableData, condition, selectRows) {
  let ids = Array.from(selectRows).map((o) => o.id);
  let allIDs = tableData.map((o) => o.id);
  let thisUnSelectIds = allIDs.filter(function (val) {
    return ids.indexOf(val) === -1;
  });
  if (condition.unSelectIds) {
    //首先将选择的ID从unSelectIds中排除
    condition.unSelectIds = condition.unSelectIds.filter(function (val) {
      return ids.indexOf(val) === -1;
    });
    //去掉unselectIds中存在的ID
    let needPushIds = thisUnSelectIds.filter(function (val) {
      return condition.unSelectIds.indexOf(val) === -1;
    });
    needPushIds.forEach((id) => {
      condition.unSelectIds.push(id);
    });
  }
}

export function getSelectDataCounts(condition, total, selectRows) {
  if (condition.selectAll) {
    return total - condition.unSelectIds.length;
  } else {
    return selectRows.size;
  }
}

// 全选操作
export function toggleAllSelection(table, tableData, selectRows) {
  //如果已经全选，不需要再操作了
  if (selectRows.size !== tableData.length) {
    table.toggleAllSelection();
  }
}

//检查表格每一行是否应该选择(使用场景：全选数据时进行翻页操作)
export function checkTableRowIsSelect(
  component,
  condition,
  tableData,
  table,
  selectRows
) {
  //如果默认全选的话，则选中应该选中的行
  if (condition.selectAll) {
    let unSelectIds = condition.unSelectIds;
    tableData.forEach((row) => {
      if (unSelectIds.indexOf(row.id) < 0) {
        table.toggleRowSelection(row, true);

        //默认全选，需要把选中对行添加到selectRows中。不然会影响到勾选函数统计
        if (!selectRows.has(row)) {
          component.$set(row, "showMore", true);
          selectRows.add(row);
        }
      } else {
        //不勾选的行，也要判断是否被加入了selectRow中。加入了的话就去除。
        if (selectRows.has(row)) {
          component.$set(row, "showMore", false);
          selectRows.delete(row);
        }
      }
    });
  }
}

//删除不需要的row(使用场景：点击表格下拉框全选时，在翻页的时候会把翻页的数据也加勾选，如果勾选了，table认为已经选中，当点击只选此页数据时，前几页的数据不会消失)
export function deleteTableRow(
  component,
  condition,
  tableData,
  table,
  selectRows
) {
  //所有以选中的数据
  let selectRowMap = new Map();
  for (let selectRow of selectRows) {
    selectRowMap.set(selectRow.id, selectRow);
  }
  //表格标为选中的数据
  table.selection.forEach((t) => {
    if (!selectRowMap.get(t.id)) {
      table.toggleRowSelection(t, false);
    }
  });
}

// nexttick:表格加载完成之后触发。判断是否需要勾选行
export function checkTableRowIsSelected(veuObj, table) {
  veuObj.$nextTick(function () {
    if (table) {
      table.checkTableRowIsSelect();
      table.doLayout();
    }
  });
}

//表格数据过滤
export function _filter(filters, condition) {
  if (!condition.filters) {
    condition.filters = {};
  }
  for (let filter in filters) {
    if (filters.hasOwnProperty(filter)) {
      let filterName = filter.startsWith("custom")
        ? filter
        : humpToLine(filter);
      if (filters[filter] && filters[filter].length > 0) {
        condition.filters[filterName] = filters[filter];
      } else {
        condition.filters[filterName] = null;
      }
    }
  }
}

//表格数据排序
export function _sort(column, condition) {
  let field = humpToLine(
    column.column.columnKey ? column.column.columnKey : column.prop
  );
  if (column.order === "descending") {
    column.order = "desc";
  } else if (column.order === "ascending") {
    column.order = "asc";
  }
  if (!condition.orders) {
    condition.orders = [];
  }
  if (column.order == null) {
    return;
  }
  let hasProp = false;
  condition.orders.forEach((order) => {
    if (order.name === field) {
      order.type = column.order;
      hasProp = true;
    }
  });
  /* if (column.prop === 'case_passing_rate' || column.prop === 'case_total') {
     hasProp = true;
   }*/
  if (!hasProp) {
    condition.orders.push({ name: field, type: column.order });
  }
}

export function initCondition(condition, isSelectAll) {
  if (!isSelectAll) {
    condition.selectAll = false;
    condition.unSelectIds = [];
  }
}

export function getLabel(vueObj, type) {
  let param = {};
  param.userId = getCurrentUser().id;
  param.type = type;
  vueObj.result = vueObj.$post("/system/header/info", param, (response) => {
    if (response.data != null) {
      vueObj.tableLabel = eval(response.data.props);
    } else {
      let param = {};
      param.type = type;
      vueObj.result = vueObj.$post(
        "/system/system/header",
        param,
        (response) => {
          if (response.data != null) {
            vueObj.tableLabel = eval(response.data.props);
          }
        }
      );
    }
  });
}

export function buildBatchParam(vueObj, selectIds, projectId) {
  let param = {};
  if (vueObj.selectRows) {
    param.ids = selectIds
      ? selectIds
      : Array.from(vueObj.selectRows).map((row) => row.id);
  } else {
    param.ids = selectIds;
  }
  param.projectId = projectId ? projectId : getCurrentProjectID();
  param.condition = vueObj.condition;
  return param;
}

// 深拷贝
export function deepClone(source) {
  if (!source && typeof source !== "object") {
    throw new Error("error arguments", "deepClone");
  }
  const targetObj = source.constructor === Array ? [] : {};
  Object.keys(source).forEach((keys) => {
    if (source[keys] && typeof source[keys] === "object") {
      targetObj[keys] = deepClone(source[keys]);
    } else {
      targetObj[keys] = source[keys];
    }
  });
  return targetObj;
}

export function getPageInfo(condition) {
  return {
    total: 0,
    pageSize: 10,
    currentPage: 1,
    result: {},
    data: [],
    condition: condition ? condition : {},
    loading: false,
  };
}

export function buildPagePath(path, page) {
  return path + "/" + page.currentPage + "/" + page.pageSize;
}

export function getPageDate(response, page) {
  let data = response.data;
  page.total = data.itemCount;
  page.data = data.listObject;
}

/**
 * 获取自定义表头
 * 如果 localStorage 没有，获取默认表头
 * @param key
 * @returns {[]|*}
 */
export function getCustomTableHeader(key, customFields) {
  let fieldSetting = getAllFieldWithCustomFields(key, customFields);
  return getCustomTableHeaderByFiledSetting(key, fieldSetting);
}


export function getCustomTableHeaderByDefault(key, customFields) {
  let fieldSetting = getAllFieldWithCustomFieldsByDefault(key, customFields);
  return getCustomTableHeaderByFiledSetting(key, fieldSetting);
}

export function getAllFieldWithCustomFieldsByDefault(key, customFields) {
  let fieldStr = localStorage.getItem(key);
  if (fieldStr == null) {
    let fieldSetting = [...CUSTOM_TABLE_HEADER[key]];
    // 如果没有 license, 排除 xpack
    if (!hasLicense()) {
      fieldSetting = fieldSetting.filter((v) => !v.xpack);
    }
    fieldSetting = fieldSetting.filter((v) => !v.defaultHide);
    fieldSetting = JSON.parse(JSON.stringify(fieldSetting));
    translateLabel(fieldSetting);
    if (customFields) {
      customFields.forEach((item) => {
        let field = {
          id: item.name,
          key: item.key,
          label: item.name,
          isCustom: true,
        };
        fieldSetting.push(field);
      });
    }
    return fieldSetting;
  } else {
    return getAllFieldWithCustomFields(key, customFields)
  }

}


/**
 * 获取 localStorage 的值，过滤
 * @param key
 * @param fieldSetting
 * @returns {[]|*}
 */
function getCustomTableHeaderByFiledSetting(key, fieldSetting) {
  let fieldStr = localStorage.getItem(key);
  if (fieldStr !== null) {
    let fields = [];
    for (let i = 0; i < fieldStr.length; i++) {
      let fieldKey = fieldStr[i];
      for (let j = 0; j < fieldSetting.length; j++) {
        let item = fieldSetting[j];
        if (item.key === fieldKey) {
          fields.push(item);
          break;
        }
      }
    }
    return fields;
  }
  return fieldSetting;
}

/**
 * 获取带自定义字段的表头
 * @param key
 * @param customFields
 * @returns {[]|*}
 */
export function getTableHeaderWithCustomFields(
  key,
  customFields,
  projectMembers = []
) {
  let fieldSetting = [...CUSTOM_TABLE_HEADER[key]];
  fieldSetting = JSON.parse(JSON.stringify(fieldSetting)); // 复制，国际化
  translateLabel(fieldSetting);
  let keys = getCustomFieldsKeys(customFields);
  projectMembers.forEach((member) => {
    member["text"] = member.name;
    // 高级搜索使用
    member["label"] = member.name;
    member["value"] = member.id;
    member["showLabel"] = member.name + "(" + member.id + ")";
  });
  customFields.forEach((item) => {
    if (!item.key) {
      // 兼容旧版，更新key
      item.key = generateTableHeaderKey(keys, customFields);
      updateCustomFieldTemplate({ id: item.id, key: item.key });
    }
    let field = {
      id: item.name,
      key: item.key,
      label: item.system ? i18n.t(SYSTEM_FIELD_NAME_MAP[item.name]) : item.name,
      type: item.type,
      isCustom: true,
      sortable: ["richText", "textarea"].indexOf(item.type) > -1 ? false : true,
      columnKey: generateColumnKey(item),
      filters: getCustomFieldFilter(item),
    };
    // 设置宽度
    if (!field.minWidth) {
      field.minWidth = 25 + field.label.length * 16;
      if (field.sortable) {
        field.minWidth += 20;
      }
      if (field.filters && field.filters.length > 0) {
        field.minWidth += 20;
      }
    }
    fieldSetting.push(field);
    if (
      (item.type === "member" || item.type === "multipleMember") &&
      projectMembers &&
      projectMembers.length > 0
    ) {
      item.options = projectMembers;
    }
  });
  return getCustomTableHeaderByFiledSetting(key, fieldSetting);
}

export function translateLabel(fieldSetting) {
  if (fieldSetting) {
    fieldSetting.forEach((item) => {
      if (item.label && !/^[A-Za-z]+$/.test(item.label)) {
        item.label = i18n.t(item.label);
      }
    });
  }
}

/**
 * 获取所有字段
 * @param key
 * @param customFields
 * @returns {*[]}
 */
export function getAllFieldWithCustomFields(key, customFields) {
  let fieldSetting = [...CUSTOM_TABLE_HEADER[key]];
  // 如果没有 license, 排除 xpack
  if (!hasLicense()) {
    fieldSetting = fieldSetting.filter((v) => !v.xpack);
  }
  fieldSetting = JSON.parse(JSON.stringify(fieldSetting));
  translateLabel(fieldSetting);
  if (customFields) {
    customFields.forEach((item) => {
      let field = {
        id: item.name,
        key: item.key,
        label: item.name,
        isCustom: true,
      };
      fieldSetting.push(field);
    });
  }
  return fieldSetting;
}

/**
 * 获取所有字段(排序后)
 * @param key
 * @param customFields
 * @returns {*[]}
 */
export function getAllDragOrCheckFieldWithCustomFields(
  fieldKey,
  fieldDragKey,
  customFields
) {
  let fieldSetting = [...CUSTOM_TABLE_HEADER[fieldKey]];
  // 如果没有 license, 排除 xpack
  if (!hasLicense()) {
    fieldSetting = fieldSetting.filter((v) => !v.xpack);
  }
  fieldSetting = JSON.parse(JSON.stringify(fieldSetting));
  translateLabel(fieldSetting);
  if (customFields) {
    customFields.forEach((item) => {
      let field = {
        id: item.name,
        key: item.key,
        label: item.name,
        isCustom: true,
      };
      fieldSetting.push(field);
    });
  }
  let fieldStr = localStorage.getItem(fieldDragKey);
  if (fieldStr !== null && fieldStr !== "") {
    let fields = [];
    for (let i = 0; i < fieldStr.length; i++) {
      let fieldKey = fieldStr[i];
      for (let j = 0; j < fieldSetting.length; j++) {
        let item = fieldSetting[j];
        if (item.key === fieldKey) {
          fields.push(item);
          break;
        }
      }
    }
    return fields;
  }
  return fieldSetting;
}

export function generateTableHeaderKey(keys) {
  let customFieldKeys = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  for (let i = 0; i < customFieldKeys.length; i++) {
    let key = customFieldKeys[i];
    if (keys.has(key)) {
      continue;
    }
    keys.add(key);
    return key;
  }
  return "";
}

export function getCustomFieldsKeys(customFields) {
  let keys = new Set();
  customFields.forEach((item) => {
    if (item.key) {
      keys.add(item.key);
    }
  });
  return keys;
}

/**
 * 将自定义表头存在 localStorage
 * 格式简化，减小占用
 * @param key
 * @param fields
 */
export function saveCustomTableHeader(key, fields) {
  let result = "";
  if (fields) {
    fields.forEach((item) => {
      result += item.key;
    });
  }
  localStorage.setItem(key, result);
}

/**
 * 将上一次的表格排序字段存在 localStorage
 * @param key
 * @param fields
 */
export function saveLastTableSortField(key, field) {
  let result = field;
  localStorage.setItem(key + "_SORT", result);
}

export function getLastTableSortField(key) {
  let orderJsonStr = localStorage.getItem(key + "_SORT");
  if (orderJsonStr) {
    try {
      return JSON.parse(orderJsonStr);
    } catch (e) {
      return [];
    }
  }
  return [];
}

/**
 * 获取对应表格的列宽
 * @param key
 * @returns {{}|any}
 */
export function getCustomTableWidth(key) {
  let fieldStr = localStorage.getItem(key + "_WITH");
  if (fieldStr !== null) {
    let fields = JSON.parse(fieldStr);
    return fields;
  }
  return {};
}

/**
 * 存储表格的列宽
 * @param key
 * @param fieldKey
 * @param colWith
 */
export function saveCustomTableWidth(key, fieldKey, colWith) {
  let fields = getCustomTableWidth(key);
  fields[fieldKey] = colWith + "";
  localStorage.setItem(key + "_WITH", JSON.stringify(fields));
}

export const OPTION_LABEL_PREFIX = "optionLabel:";

/**
 * 获取列表的自定义字段的显示值
 * @param row
 * @param field
 * @param members
 * @returns {VueI18n.TranslateResult|*}
 */
export function getCustomFieldValue(row, field, members) {
  if (row.fields) {
    for (let i = 0; i < row.fields.length; i++) {
      let item = row.fields[i];
      if (item.id === field.id) {
        if (item.value === 0) {
          return "0";
        }
        if (!item.value) {
          return "";
        }

        if (
          item.textValue &&
          item.textValue.startsWith(OPTION_LABEL_PREFIX) &&
          field.options
        ) {
          // 处理 jira 远程搜索字段
          if (item.value instanceof Array) {
            // 多选
            try {
              let optionLabel = item.textValue.substring(
                OPTION_LABEL_PREFIX.length
              );
              if (optionLabel) {
                let optionLabelMap = JSON.parse(optionLabel);
                let label = "";
                for (let j = 0; j < item.value.length; j++) {
                  let val = item.value[j];
                  let option = field.options.find((i) => i.value === val);
                  if (option) {
                    label +=
                      option.text + (j === item.value.length - 1 ? "" : " , ");
                  } else {
                    label +=
                      optionLabelMap[val] +
                      (j === item.value.length - 1 ? "" : " , ");
                  }
                }
                return label;
              }
            } catch (e) {
              console.error("getCustomFieldValue error ", e);
            }
          } else if (
            field.options.filter((i) => i.value === item.value).length < 1
          ) {
            // 单选
            return item.textValue.substring(OPTION_LABEL_PREFIX.length);
          }
        }

        if (field.type === "member") {
          for (let j = 0; j < members.length; j++) {
            let member = members[j];
            if (member.id === item.value) {
              return member.name;
            }
          }
        } else if (field.type === "multipleMember") {
          let values = "";
          if (item.value.length > 0) {
            item.value.forEach((v) => {
              for (let j = 0; j < members.length; j++) {
                let member = members[j];
                if (member.id === v) {
                  values += member.name;
                  values += " ";
                  break;
                }
              }
            });
          }
          return values;
        } else if (["radio", "select"].indexOf(field.type) > -1) {
          if (field.options) {
            for (let j = 0; j < field.options.length; j++) {
              let option = field.options[j];
              if (option.value == item.value) {
                return field.system ? i18n.t(option.text) : option.text;
              }
            }
          }
        } else if (["multipleSelect", "checkbox"].indexOf(field.type) > -1) {
          let values = "";
          try {
            if (field.type === "multipleSelect") {
              if (
                typeof item.value === "string" ||
                item.value instanceof String
              ) {
                item.value = JSON.parse(item.value);
              }
            }
            item.value.forEach((v) => {
              for (let j = 0; j < field.options.length; j++) {
                let option = field.options[j];
                if (option.value === v) {
                  values += field.system ? i18n.t(option.text) : option.text;
                  values += " ";
                  break;
                }
              }
            });
          } catch (e) {
            values = "";
          }
          return values;
        } else if (field.type === "cascadingSelect") {
          let val = "";
          let options = field.options;
          for (const v of item.value) {
            if (!options) break;
            for (const o of options) {
              if (o.value === v) {
                val = o.text;
                options = o.children;
                break;
              }
            }
          }
          return val;
        } else if (field.type === "multipleInput") {
          let val = "";
          if (!item.value || item.value === "") {
            return val;
          }
          let mulArr = parseMultipleInputToArray(item.value);
          mulArr.forEach((i) => {
            val += i + " ";
          });
          return val;
        } else if (field.type === "datetime") {
          return datetimeFormat(item.value);
        } else if (field.type === "date") {
          return dateFormat(item.value);
        } else if (["richText", "textarea"].indexOf(field.type) > -1) {
          return item.textValue;
        }
        return item.value;
      }
    }
  }
}

/**
 * 多值输入值解析, 按照导入规则括号中字符可解析[, ; ，；|]
 * @param mulInputStr
 * @returns {*[]|*}
 */
export function parseMultipleInputToArray(mulInputStr) {
  if (mulInputStr instanceof Array) {
    return mulInputStr;
  } else if (mulInputStr.indexOf(",")) {
    return mulInputStr.split(",");
  } else if (mulInputStr.indexOf(";")) {
    return mulInputStr.split(";");
  } else if (mulInputStr.indexOf("，")) {
    return mulInputStr.split("，");
  } else if (mulInputStr.indexOf("；")) {
    return mulInputStr.split("；");
  } else if (mulInputStr.indexOf("|")) {
    return mulInputStr.split("|");
  } else {
    let mulArr = [];
    mulArr.push(mulInputStr);
    return mulArr;
  }
}

/**
 * 获取批量编辑的自定义字段选项
 * @param customFields
 * @param typeArr
 * @param valueArr
 * @param members
 */
export function getCustomFieldBatchEditOption(
  customFields,
  typeArr,
  valueArr,
  members
) {
  customFields.forEach((item) => {
    if (item.options) {
      typeArr.push({
        id: item.id,
        name: item.name,
        uuid: item.id,
        custom: "custom" + item.id,
      });

      let options = [];
      if (["multipleMember", "member"].indexOf(item.type) > -1) {
        members.forEach((member) => {
          options.push({
            id: member.id,
            name: member.name,
          });
        });
      } else {
        item.options.forEach((option) => {
          options.push({
            id: option.value,
            name: option.system ? i18n.t(option.text) : option.text,
          });
        });
      }
      valueArr[item.name] = options;
    }
  });
}

export function parseCustomFilesForList(data) {
  data.forEach((item) => {
    if (item.fields) {
      item.fields.forEach((i) => {
        parseCustomFilesForItem(i);
      });
    }
  });
}

export function parseCustomFilesForItem(data) {
  if (data.value) {
    // 自定义字段内容存在回车,换行符, 需转义.
    data.value = JSON.parse(
      data.value.replace(/\n/g, "\\n").replace(/\r/g, "\\r")
    );
  }
  if (data.textValue && !data.textValue.startsWith(OPTION_LABEL_PREFIX)) {
    data.value = data.textValue;
  }
}

// 多个监听共享变量
// 否则切换 pageSize 等刷新操作会导致部分行的回调函数中 data 数据不一致
let shareDragParam = {};

// 清除 shareDragParam ，减少内存占用
export function clearShareDragParam() {
  shareDragParam.data = null;
}

export function handleRowDrop(data, callback, msTableKey) {
  setTimeout(() => {
    const tbody = document.querySelector(
      `#${msTableKey} .el-table__body-wrapper tbody`
    );
    if (!tbody) {
      return;
    }
    const dropBars = tbody.getElementsByClassName("table-row-drop-bar");

    const msTable = document.getElementsByClassName("ms-table");

    // 每次调用生成一个class
    // 避免增删列表数据时，回调函数中的 data 与实际 data 不一致
    let dropClass = "table-row-drop-bar-random" + "_" + getUUID();

    for (let i = 0; i < dropBars.length; i++) {
      dropBars[i].classList.add(dropClass);
    }

    shareDragParam.data = data;

    Sortable.create(tbody, {
      handle: "." + dropClass,
      animation: 100,
      onStart: function (/**Event*/ evt) {
        // 解决拖拽时高亮阴影停留在原位置的问题
        if (msTable) {
          for (let i = 0; i < msTable.length; i++) {
            msTable[i].classList.add("disable-hover");
          }
        }
      },
      onEnd({ newIndex, oldIndex }) {
        let param = {};
        param.moveId = shareDragParam.data[oldIndex].id;
        if (newIndex === 0) {
          param.moveMode = "BEFORE";
          param.targetId = shareDragParam.data[0].id;
        } else {
          // 默认从后面添加
          param.moveMode = "AFTER";
          if (newIndex < oldIndex) {
            // 如果往前拖拽，则添加到当前下标的前一个元素后面
            param.targetId = shareDragParam.data[newIndex - 1].id;
          } else {
            // 如果往后拖拽，则添加到当前下标的元素后面
            param.targetId = shareDragParam.data[newIndex].id;
          }
        }
        if (
          shareDragParam.data &&
          shareDragParam.data.length > 1 &&
          newIndex !== oldIndex
        ) {
          const currRow = shareDragParam.data.splice(oldIndex, 1)[0];
          shareDragParam.data.splice(newIndex, 0, currRow);
          if (callback) {
            callback(param);
          }
        }

        for (let i = 0; i < msTable.length; i++) {
          msTable[i].classList.remove("disable-hover");
        }
      },
    });
  }, 100);
}

export function getCustomFieldFilter(field, userFilter) {
  if (field.type === "multipleMember") {
    return null;
  }
  if (field.type === "member" && userFilter) {
    return userFilter;
  }

  let optionTypes = CUSTOM_FIELD_TYPE_OPTION.filter((x) => x.hasOption).map(
    (x) => x.value
  );

  if (
    optionTypes.indexOf(field.type) > -1 &&
    Array.isArray(field.options) &&
    field.options.length > 0
  ) {
    field.options.forEach((item) => {
      if (item.system && i18n.t(item.text)) {
        item.text = i18n.t(item.text);
      }
    });
    return field.options;
  }
  return null;
}

// ============================================================
// 高级搜索条件记忆功能 - 工具函数
// 用于在 localStorage 中保存、读取、清除高级搜索条件
// 存储键格式：ADV_SEARCH_{userId}_{projectId}_{moduleKey}
// ============================================================

/**
 * 获取当前项目ID
 * 只从 sessionStorage.projectId 获取，保证项目切换后立即生效，避免读到脏数据
 * @returns {string} 项目ID，如果获取不到则返回空字符串
 */
export function getCurrentProjectId() {
  try {
    return sessionStorage.getItem('projectId') || '';
  } catch (e) {
    return '';
  }
}

/**
 * 生成高级搜索条件的 localStorage 存储键
 * 格式为 ADV_SEARCH_{userId}_{projectId}_{moduleKey}，确保用户隔离、项目隔离和模块隔离
 * @param {string} userId - 用户 ID
 * @param {string} projectId - 项目 ID
 * @param {string} moduleKey - 模块标识符（如 ISSUE_LIST、TEST_CASE_LIST）
 * @returns {string} 存储键
 */
function _buildAdvSearchStorageKey(userId, projectId, moduleKey) {
  return `ADV_SEARCH_${userId}_${projectId}_${moduleKey}`;
}

/**
 * 保存高级搜索条件到 localStorage
 * 从 components 数组中提取每个搜索项的 key、operator.value、value，
 * 序列化为 JSON 字符串后存入 localStorage。
 * 所有操作包裹在 try-catch 中，存储失败（如空间已满）时静默忽略，不影响搜索功能。
 * @param {string} userId - 用户 ID
 * @param {string} projectId - 项目 ID
 * @param {string} moduleKey - 模块标识符
 * @param {Array} components - 搜索组件数组（optional.components）
 */
export function saveAdvSearchCondition(userId, projectId, moduleKey, components) {
  try {
    // 从组件数组中提取需要持久化的字段：key、operator 值、搜索值
    const conditions = (components || []).map((comp) => ({
      key: comp.key,
      operator: comp.operator ? comp.operator.value : undefined,
      value: comp.value,
    }));
    const storageKey = _buildAdvSearchStorageKey(userId, projectId, moduleKey);
    localStorage.setItem(storageKey, JSON.stringify(conditions));
  } catch (e) {
    // 静默忽略存储异常（如 localStorage 空间已满），不影响正常搜索流程
  }
}

/**
 * 从 localStorage 读取高级搜索条件
 * 读取对应存储键的 JSON 字符串并解析为数组。
 * 如果记录不存在或 JSON 解析失败（数据损坏），返回 null。
 * @param {string} userId - 用户 ID
 * @param {string} projectId - 项目 ID
 * @param {string} moduleKey - 模块标识符
 * @returns {Array|null} 搜索条件数组，不存在或解析失败时返回 null
 */
export function getAdvSearchCondition(userId, projectId, moduleKey) {
  try {
    const storageKey = _buildAdvSearchStorageKey(userId, projectId, moduleKey);
    const raw = localStorage.getItem(storageKey);
    if (!raw) {
      return null;
    }
    return JSON.parse(raw);
  } catch (e) {
    // JSON 解析失败（数据损坏）时返回 null，组件将按默认条件展示
    return null;
  }
}

/**
 * 清除 localStorage 中的高级搜索条件
 * 删除对应存储键的记录。操作包裹在 try-catch 中，删除失败时静默忽略。
 * @param {string} userId - 用户 ID
 * @param {string} projectId - 项目 ID
 * @param {string} moduleKey - 模块标识符
 */
export function clearAdvSearchCondition(userId, projectId, moduleKey) {
  try {
    const storageKey = _buildAdvSearchStorageKey(userId, projectId, moduleKey);
    localStorage.removeItem(storageKey);
  } catch (e) {
    // 静默忽略删除异常，不影响重置功能
  }
}

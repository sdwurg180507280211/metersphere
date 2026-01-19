import {OPERATORS} from "./search-components";

export function getAdvSearchCustomField(condition, fields) {
  let componentArr = condition.components;
  const components = [];
  for (let field of fields) {
    let index = componentArr.findIndex(a => a.key === field.id);
    if (index > -1 || field.id === 'platformStatus') {
      continue;
    }
    // 自定义字段中有"系统字段"属性为否的，记录为含有自定义字段
    if (!field.system && !condition.custom) {
      condition.custom = true;
    }
    components.push(_createComponent(field));
  }
  return components;
}

function _createComponent(field) {
  const componentType = getComponentName(field.type);
  let component = {
    key: field.id,
    name: componentType,
    label: field.name,
    operator: getComponentOperator(componentType, field.type, false), // 自定义字段可以异步获取选项？
    options: getComponentOptions(field),
    custom: !field.system,
    type: field.type
  }
  _handleComponentAttributes(component, componentType);
  return component;
}

function _handleComponentAttributes(component, componentType) {
  // 作为搜索条件时，可以多选
  if (componentType === 'MsTableSearchSelect') {
    component['props'] = {
      multiple: true
    }
  }
  // 浮点数精度
  if (componentType === 'MsTableSearchInputNumber' && component.type === 'float') {
    component['props'] = {
      precision: 2
    }
  }
  //
  if (component.type === 'member' || component.type === 'multipleMember') {
    component['isShow']= operator => {
      return operator !== OPERATORS.CURRENT_USER.value;
    }
  }
}

function getComponentOptions(field) {
  // 说明：
  // - 普通下拉/单选/多选：后端通常直接下发 options 数组（[{text,value}, ...]）。
  // - 某些特殊字段（如所属系统）：options 需要前端异步拉取，因此这里返回对象结构，
  //   由 MsTableSearchSelect 在 created() 中根据 url 拉取后组装为 {label,value}。
  if (field && (field.type === 'associatedSystem' || field.type === 'multipleAssociatedSystem')) {
    return {
      url: '/associatedSystem/list/all',
      labelKey: 'name',
      valueKey: 'id',
      showLabel: option => {
        return (option.description ? option.description + ' / ' : '') + option.label;
      }
    };
  }
  return Array.isArray(field.options) ? (field.options.length > 0 ? field.options : []) : [];
}

function getComponentName(type) {
  switch (type) {
    case 'input':
    case 'textarea':
    case 'richText':
      return 'MsTableSearchInput';
    case 'select':
    case 'multipleSelect':
    case 'radio':
    case 'checkbox':
    case 'member':
    case 'multipleMember':
    case 'associatedSystem':
    case 'multipleAssociatedSystem':
      return 'MsTableSearchSelect';
    case 'date':
      return 'MsTableSearchDatePicker';
    case 'datetime':
      return 'MsTableSearchDateTimePicker';
    case 'int':
    case 'float':
      return 'MsTableSearchInputNumber';
    case 'multipleInput':
      return 'MsTableSearchInput'; // todo 创建对应组件
    default:
      return 'MsTableSearchInput';
  }
}

function getComponentOperator(componentType, fieldType, async) {
  let operator = {};
  switch (componentType) {
    case 'MsTableSearchInput':
      operator = { // 运算符设置
        value: OPERATORS.LIKE.value, // 如果未设置value初始值，则value初始值为options[0]
        options: [OPERATORS.LIKE, OPERATORS.NOT_LIKE] // 运算符候选项
      }
      break;
    case 'MsTableSearchSelect':
      if (async || fieldType === 'member' || fieldType === 'multipleMember') {
        operator = {
          options: [OPERATORS.IN, OPERATORS.NOT_IN, OPERATORS.CURRENT_USER],
          //todo
          change: function (component, value) { // 运算符change事件
            if (value === OPERATORS.CURRENT_USER.value) {
              component.value = value;
            }
          }
        }
      } else {
        operator = {
          options: [OPERATORS.IN, OPERATORS.NOT_IN]
        }
      }
      break;
    case 'MsTableSearchDatePicker':
    case 'MsTableSearchDateTimePicker':
      operator = {
        options: [OPERATORS.BETWEEN, OPERATORS.GT, OPERATORS.LT]
      }
      break;
    case 'MsTableSearchInputNumber':
      operator = {
        options: [OPERATORS.GT, OPERATORS.GE, OPERATORS.LT, OPERATORS.LE, OPERATORS.EQ]
      }
      break;
  }
  return operator;
}

export function _findByKey(components, key) {
  return components.find(co => co.key === key);
}

export function _findIndexByKey(components, key) {
  return components.findIndex(co => co.key === key);
}

export function generateColumnKey(field) {
  // 单选类型字段：生成 custom_single-{id} 格式的 key
  // 目的是：让后端 ExtIssuesMapper.xml 中的 custom_single 分支能正确处理这些字段的高级搜索
  if (field.type === 'select' || field.type === 'radio' || field.type === 'member'
      || field.type === 'associatedSystem') {  // 新增：所属系统字段也是单选类型
    return 'custom_single-' + field.id;
  } else if (field.type === 'multipleSelect' || field.type === 'checkbox' || field.type === 'multipleMember'
      || field.type === 'multipleAssociatedSystem') {  // 新增：多选所属系统字段
    // 多选类型字段：生成 custom_multiple-{id} 格式的 key
    return 'custom_multiple-' + field.id;
  } else {
    return 'custom-' + field.id;
  }
}


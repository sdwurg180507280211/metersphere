/**
 * 格式化工具函数
 * 用于统一处理日期、时间、数字等格式化需求
 */

/**
 * 格式化时间戳为可读字符串
 * @param {number|string|Date} timestamp - 时间戳（毫秒）、日期字符串或Date对象
 * @param {string} format - 格式模板，默认 'YYYY-MM-DD HH:mm:ss'
 * @returns {string} 格式化后的时间字符串
 * 
 * 支持的格式占位符：
 * - YYYY: 四位年份
 * - MM: 两位月份（01-12）
 * - DD: 两位日期（01-31）
 * - HH: 两位小时（00-23）
 * - mm: 两位分钟（00-59）
 * - ss: 两位秒（00-59）
 */
export function formatTime(timestamp, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!timestamp) {
    return '-';
  }

  // 统一转换为Date对象
  let date;
  if (timestamp instanceof Date) {
    date = timestamp;
  } else if (typeof timestamp === 'string') {
    date = new Date(timestamp);
  } else {
    date = new Date(timestamp);
  }

  // 检查日期有效性
  if (isNaN(date.getTime())) {
    return '-';
  }

  // 提取日期时间各部分（使用padStart确保两位数）
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  // 替换格式占位符
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
}

/**
 * 格式化为相对时间（如：刚刚、5分钟前、2小时前）
 * @param {number|string|Date} timestamp - 时间戳
 * @returns {string} 相对时间描述
 */
export function formatRelativeTime(timestamp) {
  if (!timestamp) {
    return '-';
  }

  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime(); // 毫秒差

  if (diff < 0) {
    return formatTime(timestamp, 'YYYY-MM-DD HH:mm');
  }

  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (seconds < 60) {
    return '刚刚';
  } else if (minutes < 60) {
    return `${minutes}分钟前`;
  } else if (hours < 24) {
    return `${hours}小时前`;
  } else if (days < 7) {
    return `${days}天前`;
  } else {
    return formatTime(timestamp, 'YYYY-MM-DD HH:mm');
  }
}

/**
 * 格式化数字（添加千分位分隔符）
 * @param {number} num - 数字
 * @param {number} decimals - 小数位数，默认0
 * @returns {string} 格式化后的数字字符串
 */
export function formatNumber(num, decimals = 0) {
  if (num === null || num === undefined || isNaN(num)) {
    return '-';
  }

  const fixed = Number(num).toFixed(decimals);
  const parts = fixed.split('.');
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return parts.join('.');
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的文件大小（如：1.5 MB）
 */
export function formatFileSize(bytes) {
  if (!bytes || bytes === 0) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const k = 1024;
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${units[i]}`;
}

/**
 * 格式化百分比
 * @param {number} value - 数值（0-1或0-100）
 * @param {boolean} isDecimal - 是否为小数形式（0-1），默认false
 * @param {number} decimals - 小数位数，默认2
 * @returns {string} 格式化后的百分比字符串
 */
export function formatPercent(value, isDecimal = false, decimals = 2) {
  if (value === null || value === undefined || isNaN(value)) {
    return '-';
  }

  const percent = isDecimal ? value * 100 : value;
  return `${percent.toFixed(decimals)}%`;
}

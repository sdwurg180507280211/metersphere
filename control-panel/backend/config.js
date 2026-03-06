/**
 * 配置模块
 */
const fs = require('fs');
const path = require('path');

const CONFIG_PATH = path.join(__dirname, '../config.json');

function loadConfig() {
  try {
    const config = JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'));
    return {
      port: config.port || 3000,
      projectRoot: path.join(__dirname, '..', config.projectRoot || '..'),
      maxLogLines: config.maxLogLines || 1000,
      services: config.services || {}
    };
  } catch (error) {
    console.error('加载配置文件失败:', error.message);
    process.exit(1);
  }
}

module.exports = loadConfig();

import {get, post} from 'metersphere-frontend/src/plugins/request';


export function getSystemStatisticsData() {
  return get('/system/statistics/data');
}

export function getSystemVersion() {
  return get('/system/version');
}

export function getSystemBaseSetting() {
  return get('/system/base/info');
}

export function saveSystemBaseSetting(obj) {
  return post('/system/save/base', obj);
}

export function getSystemMailServerInfo() {
  return get('/system/mail/info');
}

export function modifySystemMailServerInfo(mail) {
  return post('/system/edit/email', mail);
}

export function testMailServerConnect(mail) {
  return post('/system/testConnection', mail);
}

export function getLdapInfo() {
  return get('/system/ldap/info');
}

export function testLdapConnect(ldap) {
  return post('/ldap/test/connect', ldap);
}

export function saveLdapInfo(ldap) {
  return post('/system/save/ldap', ldap);
}

export function testLdapLogin(ldap) {
  return post('/ldap/test/login',ldap);
}

// 获取公告内容
export function getAnnouncementContent() {
  return get('/system/get/info/announcement.content');
}

// 保存公告内容
export function saveAnnouncementContent(content) {
  return post('/system/edit/info', {
    paramKey: 'announcement.content',
    paramValue: content,
    type: 'text'
  });
}

// 获取公告启用状态
export function getAnnouncementEnabled() {
  return get('/system/get/info/announcement.enabled');
}

// 保存公告启用状态
export function saveAnnouncementEnabled(enabled) {
  return post('/system/edit/info', {
    paramKey: 'announcement.enabled',
    paramValue: String(enabled),
    type: 'text'
  });
}

// 获取公告样式
export function getAnnouncementStyle() {
  return get('/system/get/info/announcement.style');
}

// 保存公告样式
export function saveAnnouncementStyle(style) {
  return post('/system/edit/info', {
    paramKey: 'announcement.style',
    paramValue: JSON.stringify(style),
    type: 'text'
  });
}

// 获取公告滚动配置
export function getAnnouncementScroll() {
  return get('/system/get/info/announcement.scroll');
}

// 保存公告滚动配置
export function saveAnnouncementScroll(scroll) {
  return post('/system/edit/info', {
    paramKey: 'announcement.scroll',
    paramValue: String(scroll),
    type: 'text'
  });
}

// 获取公告滚动速度
export function getAnnouncementScrollSpeed() {
  return get('/system/get/info/announcement.scroll.speed');
}

// 保存公告滚动速度
export function saveAnnouncementScrollSpeed(speed) {
  return post('/system/edit/info', {
    paramKey: 'announcement.scroll.speed',
    paramValue: String(speed),
    type: 'text'
  });
}

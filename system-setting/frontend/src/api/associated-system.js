import {get, post} from 'metersphere-frontend/src/plugins/request';
export function getAssociatedSystemPages(goPage, pageSize, param) {
  return post(`/associatedSystem/list/${goPage}/${pageSize}`, param);
}
export function delAssociatedSystemById(id) {
    return get(`/associatedSystem/delete/${id}`);
  }

export function createAssociatedSystem(param) {
    return post('/associatedSystem/add', param);
  }
export function updateAssociatedSystem(param) {
    return post('/associatedSystem/update', param);
  }




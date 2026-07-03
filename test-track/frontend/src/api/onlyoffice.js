import { get, post } from "@/business/utils/sdk-utils";

export function getOnlyOfficeConfig() {
  return get("/track/onlyoffice/config");
}

export function createOnlyOfficeCaseSession(param) {
  return post("/track/onlyoffice/case/session", param);
}

export function getOnlyOfficeCaseSessionState(sessionId) {
  return get(`/track/onlyoffice/case/session/${sessionId}/state`);
}

export function saveOnlyOfficeCaseSession(sessionId) {
  return post(`/track/onlyoffice/case/session/${sessionId}/save`);
}

import { get } from "metersphere-frontend/src/plugins/request";

export function getApitableConfig() {
  return get("/track/multitable/config");
}

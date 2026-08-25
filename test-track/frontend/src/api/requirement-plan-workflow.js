import {post} from "metersphere-frontend/src/plugins/request";

const BASE_URL = "/requirement/plan-workflow/";

export function submitRequirementPlanApproval(planId) {
  return post(BASE_URL + `submit-approval/${planId}`, {});
}

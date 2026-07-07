# Test Plan Requirement Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a non-blocking requirement assessment and approval workflow for requirement-synced test plans without breaking MeterSphere's existing test plan execution lifecycle.

**Architecture:** Keep `test_plan.status` as the existing execution status. Add requirement-specific fields and a focused `RequirementPlanWorkflowService` to own assessment, approval, display status, callback handling, and editability rules. Frontend displays a single combined “需求处理状态” while keeping the original “测试计划状态” column unchanged.

**Tech Stack:** Java 17, Spring Boot 3.2.12, MyBatis, Flyway, Vue 2, Element UI, RocketMQ.

## Global Constraints

- Only requirement-synced test plans participate in this workflow; ordinary manually created test plans keep the original MeterSphere lifecycle.
- `status` remains limited to `Prepare / Underway / Completed / Finished / Archived / Cancelled`.
- Requirement approval must not block test execution.
- Exempted plans must not be treated as normal `Finished` test execution.
- The UI should expose “测试计划状态” and a combined “需求处理状态”, not three raw backend fields.
- Follow the design spec at `docs/superpowers/specs/2026-07-07-test-plan-requirement-workflow-design.md`.

---

## File Structure

### Backend Domain And Persistence

- Modify `test-track/backend/src/main/resources/db/migration/2.10.26/ddl/V21__alter_test_plan_requirement_workflow.sql`
  - Add requirement workflow columns to `test_plan`.
- Modify `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestPlan.java`
  - Add workflow fields.
- Modify `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestPlanWithBLOBs.java`
  - Confirm it inherits workflow fields from `TestPlan`.
- Modify `test-track/backend/src/main/java/io/metersphere/base/mapper/TestPlanMapper.xml`
  - Add result mappings and insert/update column handling.
- Modify `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtTestPlanMapper.xml`
  - Include fields in list query and filtering if needed.

### Backend Workflow

- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/constants/RequirementAssessmentStatus.java`
- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/constants/RequirementApprovalStatus.java`
- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/dto/RequirementPlanDisplayStatus.java`
- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/request/SubmitContinueTestRequest.java`
- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/request/SubmitExemptTestRequest.java`
- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/request/RequirementApprovalCallbackRequest.java`
- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/service/RequirementPlanWorkflowService.java`
- Create `test-track/backend/src/main/java/io/metersphere/requirement/workflow/controller/RequirementPlanWorkflowController.java`
- Modify `test-track/backend/src/main/java/io/metersphere/requirement/pool/dto/RequirementCallbackMessage.java`
  - Add fields for assessment result and exempt information.
- Modify `test-track/backend/src/main/java/io/metersphere/requirement/pool/service/RequirementPoolService.java`
  - Replace the old requirement-pool-first flow with workflow initialization for directly synced plans.
- Modify `test-track/backend/src/main/java/io/metersphere/plan/service/TestPlanService.java`
  - Guard automatic status calculation for exempted requirement plans.

### Frontend

- Modify `test-track/frontend/src/business/plan/components/TestPlanList.vue`
  - Add “需求处理状态” column and requirement workflow actions.
- Modify `test-track/frontend/src/business/plan/components/TestPlanEdit.vue`
  - Apply editability rules for submitted, approved, rejected, and exempt states.
- Create `test-track/frontend/src/business/plan/components/RequirementProcessStatusItem.vue`
  - Render combined status.
- Create `test-track/frontend/src/business/plan/components/RequirementExemptDialog.vue`
  - Exempt submission form.
- Create `test-track/frontend/src/api/requirement-plan-workflow.js`
  - API client functions.
- Modify `test-track/frontend/src/i18n/lang/zh-CN.js`
  - Add Chinese labels.
- Modify `test-track/frontend/src/i18n/lang/en-US.js`
  - Add English fallback labels.
- Modify `test-track/frontend/src/i18n/lang/zh-TW.js`
  - Add Traditional Chinese labels.

---

## Task 1: Persist Requirement Workflow Fields

**Files:**
- Create: `test-track/backend/src/main/resources/db/migration/2.10.26/ddl/V21__alter_test_plan_requirement_workflow.sql`
- Modify: `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestPlan.java`
- Read: `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestPlanWithBLOBs.java`
- Modify: `test-track/backend/src/main/java/io/metersphere/base/mapper/TestPlanMapper.xml`

**Interfaces:**
- Produces fields on `TestPlan`: `requirementAssessmentStatus`, `requirementApprovalStatus`, `requirementSystemName`, `requirementDocUrl`, `requirementSummary`, `requirementRelatedUsers`, `requirementExemptReason`.
- Later tasks consume these fields through existing `TestPlanMapper` and `TestPlanWithBLOBs`.

- [ ] **Step 1: Create the Flyway migration**

Create `test-track/backend/src/main/resources/db/migration/2.10.26/ddl/V21__alter_test_plan_requirement_workflow.sql`:

```sql
ALTER TABLE test_plan
    ADD COLUMN requirement_assessment_status varchar(64) DEFAULT 'NONE' COMMENT '需求评估状态：NONE/PENDING/CONTINUE_TEST/EXEMPT_SUBMITTED',
    ADD COLUMN requirement_approval_status varchar(64) DEFAULT 'NONE' COMMENT '需求审批状态：NONE/SUBMITTED/APPROVED/REJECTED',
    ADD COLUMN requirement_system_name varchar(255) DEFAULT NULL COMMENT '需求所属系统名称',
    ADD COLUMN requirement_doc_url varchar(1024) DEFAULT NULL COMMENT '需求规格说明书链接',
    ADD COLUMN requirement_summary varchar(1024) DEFAULT NULL COMMENT '需求简述',
    ADD COLUMN requirement_related_users varchar(1024) DEFAULT NULL COMMENT '免测关联人员',
    ADD COLUMN requirement_exempt_reason text DEFAULT NULL COMMENT '免测说明';

CREATE INDEX idx_test_plan_req_assessment ON test_plan(requirement_assessment_status);
CREATE INDEX idx_test_plan_req_approval ON test_plan(requirement_approval_status);
```

- [ ] **Step 2: Add fields to `TestPlan`**

Add these properties after `requirementNumber` in `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestPlan.java`:

```java
    private String requirementAssessmentStatus;

    private String requirementApprovalStatus;

    private String requirementSystemName;

    private String requirementDocUrl;

    private String requirementSummary;

    private String requirementRelatedUsers;

    private String requirementExemptReason;
```

- [ ] **Step 3: Confirm `TestPlanWithBLOBs` inheritance**

Open `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestPlanWithBLOBs.java` and confirm it extends `TestPlan`:

```java
public class TestPlanWithBLOBs extends TestPlan implements Serializable
```

No duplicate workflow fields should be added to `TestPlanWithBLOBs`.

- [ ] **Step 4: Update `TestPlanMapper.xml` mappings**

In `test-track/backend/src/main/java/io/metersphere/base/mapper/TestPlanMapper.xml`, add result mappings:

```xml
<result column="requirement_assessment_status" jdbcType="VARCHAR" property="requirementAssessmentStatus" />
<result column="requirement_approval_status" jdbcType="VARCHAR" property="requirementApprovalStatus" />
<result column="requirement_system_name" jdbcType="VARCHAR" property="requirementSystemName" />
<result column="requirement_doc_url" jdbcType="VARCHAR" property="requirementDocUrl" />
<result column="requirement_summary" jdbcType="VARCHAR" property="requirementSummary" />
<result column="requirement_related_users" jdbcType="VARCHAR" property="requirementRelatedUsers" />
<result column="requirement_exempt_reason" jdbcType="LONGVARCHAR" property="requirementExemptReason" />
```

Add the same columns to insert, selective insert, update, and selective update sections following the local XML style.

- [ ] **Step 5: Compile backend domain**

Run:

```bash
./mvnw -pl framework/sdk-parent/domain,test-track/backend -am -DskipTests compile
```

Expected: build succeeds with no missing getter/setter or MyBatis property errors.

- [ ] **Step 6: Commit**

```bash
git add test-track/backend/src/main/resources/db/migration/2.10.26/ddl/V21__alter_test_plan_requirement_workflow.sql \
  framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/TestPlan.java \
  test-track/backend/src/main/java/io/metersphere/base/mapper/TestPlanMapper.xml
git commit -m "feat: add requirement workflow fields to test plans"
```

---

## Task 2: Add Requirement Workflow Types And Display Mapping

**Files:**
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/constants/RequirementAssessmentStatus.java`
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/constants/RequirementApprovalStatus.java`
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/dto/RequirementPlanDisplayStatus.java`
- Create: `test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementWorkflowStatusTest.java`

**Interfaces:**
- Produces enum values used by service/controller tasks.
- Produces `RequirementPlanDisplayStatus.from(String assessment, String approval)`.

- [ ] **Step 1: Create assessment enum**

```java
package io.metersphere.requirement.workflow.constants;

public enum RequirementAssessmentStatus {
    NONE,
    PENDING,
    CONTINUE_TEST,
    EXEMPT_SUBMITTED
}
```

- [ ] **Step 2: Create approval enum**

```java
package io.metersphere.requirement.workflow.constants;

public enum RequirementApprovalStatus {
    NONE,
    SUBMITTED,
    APPROVED,
    REJECTED
}
```

- [ ] **Step 3: Create display status DTO**

```java
package io.metersphere.requirement.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequirementPlanDisplayStatus {
    private String value;
    private String text;

    public static RequirementPlanDisplayStatus from(String assessment, String approval) {
        String key = String.valueOf(assessment) + ":" + String.valueOf(approval);
        switch (key) {
            case "PENDING:NONE":
                return new RequirementPlanDisplayStatus("PENDING", "待评估");
            case "CONTINUE_TEST:SUBMITTED":
                return new RequirementPlanDisplayStatus("CONTINUE_TEST_SUBMITTED", "继续测试-已提交");
            case "CONTINUE_TEST:APPROVED":
                return new RequirementPlanDisplayStatus("CONTINUE_TEST_APPROVED", "继续测试-审批通过");
            case "CONTINUE_TEST:REJECTED":
                return new RequirementPlanDisplayStatus("CONTINUE_TEST_REJECTED", "继续测试-已驳回");
            case "EXEMPT_SUBMITTED:SUBMITTED":
                return new RequirementPlanDisplayStatus("EXEMPT_SUBMITTED", "免测已提交");
            case "EXEMPT_SUBMITTED:APPROVED":
                return new RequirementPlanDisplayStatus("EXEMPT_APPROVED", "免测已确认");
            case "EXEMPT_SUBMITTED:REJECTED":
                return new RequirementPlanDisplayStatus("EXEMPT_REJECTED", "免测已驳回");
            default:
                return new RequirementPlanDisplayStatus("NONE", "--");
        }
    }
}
```

- [ ] **Step 4: Add display mapping test**

Create `test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementWorkflowStatusTest.java`:

```java
package io.metersphere.requirement.workflow;

import io.metersphere.requirement.workflow.dto.RequirementPlanDisplayStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequirementWorkflowStatusTest {
    @Test
    void shouldMapPendingStatus() {
        RequirementPlanDisplayStatus status = RequirementPlanDisplayStatus.from("PENDING", "NONE");
        assertEquals("PENDING", status.getValue());
        assertEquals("待评估", status.getText());
    }

    @Test
    void shouldMapRejectedContinueTestStatus() {
        RequirementPlanDisplayStatus status = RequirementPlanDisplayStatus.from("CONTINUE_TEST", "REJECTED");
        assertEquals("CONTINUE_TEST_REJECTED", status.getValue());
        assertEquals("继续测试-已驳回", status.getText());
    }

    @Test
    void shouldMapUnknownStatusToNone() {
        RequirementPlanDisplayStatus status = RequirementPlanDisplayStatus.from("NONE", "NONE");
        assertEquals("NONE", status.getValue());
        assertEquals("--", status.getText());
    }
}
```

- [ ] **Step 5: Run test**

```bash
./mvnw -pl test-track/backend -Dtest=RequirementWorkflowStatusTest test
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add test-track/backend/src/main/java/io/metersphere/requirement/workflow \
  test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementWorkflowStatusTest.java
git commit -m "feat: add requirement workflow status model"
```

---

## Task 3: Add Workflow Service And Controller

**Files:**
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/request/SubmitContinueTestRequest.java`
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/request/SubmitExemptTestRequest.java`
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/request/RequirementApprovalCallbackRequest.java`
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/service/RequirementPlanWorkflowService.java`
- Create: `test-track/backend/src/main/java/io/metersphere/requirement/workflow/controller/RequirementPlanWorkflowController.java`
- Modify: `test-track/backend/src/main/java/io/metersphere/requirement/pool/dto/RequirementCallbackMessage.java`

**Interfaces:**
- Produces REST endpoints:
  - `POST /requirement/plan-workflow/continue-test`
  - `POST /requirement/plan-workflow/exempt-test`
  - `POST /requirement/plan-workflow/approval-callback`
- Consumes `RequirementCallbackProducer.sendCallbackMessage(RequirementCallbackMessage msg)`.

- [ ] **Step 1: Create request classes**

`SubmitContinueTestRequest`:

```java
package io.metersphere.requirement.workflow.request;

import lombok.Data;

@Data
public class SubmitContinueTestRequest {
    private String planId;
    private Long plannedStartTime;
    private Long plannedEndTime;
}
```

`SubmitExemptTestRequest`:

```java
package io.metersphere.requirement.workflow.request;

import lombok.Data;

@Data
public class SubmitExemptTestRequest {
    private String planId;
    private String requirementSystemName;
    private String requirementSummary;
    private String requirementRelatedUsers;
    private String requirementExemptReason;
}
```

`RequirementApprovalCallbackRequest`:

```java
package io.metersphere.requirement.workflow.request;

import lombok.Data;

@Data
public class RequirementApprovalCallbackRequest {
    private String requirementNumber;
    private String approvalStatus;
    private String traceId;
}
```

- [ ] **Step 2: Extend callback message**

Add fields to `RequirementCallbackMessage`:

```java
    private String assessmentResult;
    private String requirementSystemName;
    private String requirementSummary;
    private String requirementRelatedUsers;
    private String requirementExemptReason;
```

- [ ] **Step 3: Create workflow service**

Implement `RequirementPlanWorkflowService` with methods:

```java
public TestPlan submitContinueTest(SubmitContinueTestRequest request)
public TestPlan submitExemptTest(SubmitExemptTestRequest request)
public TestPlan handleApprovalCallback(RequirementApprovalCallbackRequest request)
public RequirementPlanDisplayStatus getDisplayStatus(TestPlan plan)
public boolean isRequirementSyncedPlan(TestPlan plan)
public boolean canEditKeyFields(TestPlan plan)
```

Rules:

- Reject blank `planId`.
- Reject workflow actions when `requirementNumber` is blank.
- `submitContinueTest` sets:
  - `plannedStartTime`
  - `plannedEndTime`
  - `requirementAssessmentStatus = CONTINUE_TEST`
  - `requirementApprovalStatus = SUBMITTED`
- `submitExemptTest` sets:
  - exempt fields from request
  - `requirementAssessmentStatus = EXEMPT_SUBMITTED`
  - `requirementApprovalStatus = SUBMITTED`
- `handleApprovalCallback` maps:
  - `APPROVED` to `requirementApprovalStatus = APPROVED`
  - `REJECTED` to `requirementApprovalStatus = REJECTED`
- Do not change `status` in approval callback.
- Send `RequirementCallbackMessage` after continue-test and exempt submission.

- [ ] **Step 4: Create controller**

Controller path:

```java
@RestController
@RequestMapping("/requirement/plan-workflow")
public class RequirementPlanWorkflowController {
    @PostMapping("/continue-test")
    public TestPlan submitContinueTest(@RequestBody SubmitContinueTestRequest request)

    @PostMapping("/exempt-test")
    public TestPlan submitExemptTest(@RequestBody SubmitExemptTestRequest request)

    @PostMapping("/approval-callback")
    public TestPlan approvalCallback(@RequestBody RequirementApprovalCallbackRequest request)
}
```

Use existing permission pattern from `TestPlanController`: require project track plan edit permission for test-user actions and authenticated access for callbacks if the existing integration has a callback permission convention.

- [ ] **Step 5: Compile**

```bash
./mvnw -pl test-track/backend -am -DskipTests compile
```

Expected: compile succeeds.

- [ ] **Step 6: Commit**

```bash
git add test-track/backend/src/main/java/io/metersphere/requirement/workflow \
  test-track/backend/src/main/java/io/metersphere/requirement/pool/dto/RequirementCallbackMessage.java
git commit -m "feat: add requirement plan workflow service"
```

---

## Task 4: Initialize Workflow During Requirement Sync

**Files:**
- Modify: `test-track/backend/src/main/java/io/metersphere/requirement/pool/service/RequirementPoolService.java`
- Modify: `test-track/backend/src/main/java/io/metersphere/requirement/pool/dto/RequirementSyncMessage.java`
- Test: `test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementSyncWorkflowTest.java`

**Interfaces:**
- Consumes Task 2 enum values.
- Produces directly synced test plans with `status=Prepare`, `requirementAssessmentStatus=PENDING`, `requirementApprovalStatus=NONE`.

- [ ] **Step 1: Add sync DTO fields if missing**

Ensure `RequirementSyncMessage` can carry:

```java
private String systemCode;
private String systemName;
private String requirementDocUrl;
private String requirementSummary;
```

- [ ] **Step 2: Update direct test plan creation**

In the requirement sync path that creates `AddTestPlanRequest`, set:

```java
testPlanRequest.setStatus(TestPlanStatus.Prepare.name());
testPlanRequest.setRequirementNumber(msg.getDmpNum());
testPlanRequest.setRequirementAssessmentStatus(RequirementAssessmentStatus.PENDING.name());
testPlanRequest.setRequirementApprovalStatus(RequirementApprovalStatus.NONE.name());
testPlanRequest.setRequirementSystemName(msg.getSystemName());
testPlanRequest.setRequirementDocUrl(msg.getRequirementDocUrl());
testPlanRequest.setRequirementSummary(msg.getRequirementSummary());
```

- [ ] **Step 3: Preserve ordinary plan creation**

Confirm `TestPlanService.addTestPlan` still defaults ordinary manual plans to:

```java
status = Prepare
requirementAssessmentStatus = NONE
requirementApprovalStatus = NONE
```

If the DB default is not enough because full insert passes nulls, set defaults in `addTestPlan`.

- [ ] **Step 4: Add sync workflow test**

Create a focused service test that builds an `AddTestPlanRequest` from a requirement message and asserts:

```java
assertEquals("Prepare", plan.getStatus());
assertEquals("PENDING", plan.getRequirementAssessmentStatus());
assertEquals("NONE", plan.getRequirementApprovalStatus());
assertEquals("REQ-001", plan.getRequirementNumber());
```

- [ ] **Step 5: Run backend tests**

```bash
./mvnw -pl test-track/backend -Dtest=RequirementSyncWorkflowTest,RequirementWorkflowStatusTest test
```

Expected: both tests pass.

- [ ] **Step 6: Commit**

```bash
git add test-track/backend/src/main/java/io/metersphere/requirement/pool \
  test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementSyncWorkflowTest.java
git commit -m "feat: initialize requirement workflow on synced plans"
```

---

## Task 5: Protect Existing Execution Status Logic

**Files:**
- Modify: `test-track/backend/src/main/java/io/metersphere/plan/service/TestPlanService.java`
- Test: `test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementWorkflowExecutionStatusTest.java`

**Interfaces:**
- Consumes `requirementAssessmentStatus`.
- Produces guarantee that exempted plans are not auto-calculated into normal execution states.

- [ ] **Step 1: Add helper method**

In `TestPlanService`, add a private helper:

```java
private boolean isExemptRequirementPlan(TestPlan testPlan) {
    return testPlan != null
            && StringUtils.isNotBlank(testPlan.getRequirementNumber())
            && StringUtils.equals(testPlan.getRequirementAssessmentStatus(), "EXEMPT_SUBMITTED");
}
```

- [ ] **Step 2: Guard automatic status calculation**

At the beginning of `checkTestPlanStatus(String testPlanId)`, after loading `testPlanWithBLOBs`, return when exempt:

```java
if (isExemptRequirementPlan(testPlanWithBLOBs)) {
    return;
}
```

Keep the existing `Archived` and `Cancelled` guard.

- [ ] **Step 3: Guard execution start if required**

In methods that start execution, such as places setting status to `Underway`, reject exempted plans with a clear exception message:

```java
MSException.throwException("免测测试计划不允许执行");
```

Apply this only to requirement plans where `requirementAssessmentStatus = EXEMPT_SUBMITTED`.

- [ ] **Step 4: Add test**

Add a test that constructs an exempted requirement plan and verifies status calculation is skipped. If direct unit testing is difficult due to service dependencies, add a focused test for the helper by making it package-private:

```java
assertTrue(testPlanService.isExemptRequirementPlan(plan));
```

- [ ] **Step 5: Run tests**

```bash
./mvnw -pl test-track/backend -Dtest=RequirementWorkflowExecutionStatusTest test
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add test-track/backend/src/main/java/io/metersphere/plan/service/TestPlanService.java \
  test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementWorkflowExecutionStatusTest.java
git commit -m "fix: protect execution status from exempt requirement plans"
```

---

## Task 6: Add Frontend API And Status Renderer

**Files:**
- Create: `test-track/frontend/src/api/requirement-plan-workflow.js`
- Create: `test-track/frontend/src/business/plan/components/RequirementProcessStatusItem.vue`
- Modify: `test-track/frontend/src/i18n/lang/zh-CN.js`
- Modify: `test-track/frontend/src/i18n/lang/en-US.js`
- Modify: `test-track/frontend/src/i18n/lang/zh-TW.js`

**Interfaces:**
- Produces API methods:
  - `submitContinueTest(data)`
  - `submitExemptTest(data)`
- Produces Vue component prop contract:
  - `assessmentStatus: String`
  - `approvalStatus: String`

- [ ] **Step 1: Create API client**

```javascript
import { post } from 'metersphere-frontend/src/plugins/request';

const BASE_URL = '/requirement/plan-workflow/';

export function submitContinueTest(data) {
  return post(BASE_URL + 'continue-test', data);
}

export function submitExemptTest(data) {
  return post(BASE_URL + 'exempt-test', data);
}
```

- [ ] **Step 2: Create status renderer**

`RequirementProcessStatusItem.vue` should compute labels:

```javascript
const STATUS_MAP = {
  'PENDING:NONE': '待评估',
  'CONTINUE_TEST:SUBMITTED': '继续测试-已提交',
  'CONTINUE_TEST:APPROVED': '继续测试-审批通过',
  'CONTINUE_TEST:REJECTED': '继续测试-已驳回',
  'EXEMPT_SUBMITTED:SUBMITTED': '免测已提交',
  'EXEMPT_SUBMITTED:APPROVED': '免测已确认',
  'EXEMPT_SUBMITTED:REJECTED': '免测已驳回',
  'NONE:NONE': '--',
};
```

Use a small tag style matching `PlanStatusTableItem`.

- [ ] **Step 3: Add i18n keys**

Add keys under the existing test plan namespace:

```javascript
requirement_process_status: '需求处理状态',
requirement_pending: '待评估',
requirement_continue_submitted: '继续测试-已提交',
requirement_continue_approved: '继续测试-审批通过',
requirement_continue_rejected: '继续测试-已驳回',
requirement_exempt_submitted: '免测已提交',
requirement_exempt_approved: '免测已确认',
requirement_exempt_rejected: '免测已驳回',
```

- [ ] **Step 4: Run frontend lint/build check**

```bash
cd test-track/frontend
npm run lint
```

Expected: lint succeeds. If this module has no lint script, run `npm run build` only after confirming local dependencies are installed.

- [ ] **Step 5: Commit**

```bash
git add test-track/frontend/src/api/requirement-plan-workflow.js \
  test-track/frontend/src/business/plan/components/RequirementProcessStatusItem.vue \
  test-track/frontend/src/i18n/lang/zh-CN.js \
  test-track/frontend/src/i18n/lang/en-US.js \
  test-track/frontend/src/i18n/lang/zh-TW.js
git commit -m "feat: add requirement workflow frontend status model"
```

---

## Task 7: Add List Actions And Exempt Dialog

**Files:**
- Modify: `test-track/frontend/src/business/plan/components/TestPlanList.vue`
- Create: `test-track/frontend/src/business/plan/components/RequirementExemptDialog.vue`
- Modify: `test-track/frontend/src/business/plan/components/TestPlanEdit.vue`

**Interfaces:**
- Consumes API from Task 6.
- Consumes plan row fields:
  - `requirementNumber`
  - `requirementAssessmentStatus`
  - `requirementApprovalStatus`
  - `requirementSystemName`
  - `requirementSummary`
  - `requirementRelatedUsers`
  - `requirementExemptReason`

- [ ] **Step 1: Add “需求处理状态” column**

In `TestPlanList.vue`, add an `ms-table-column` near the existing status column:

```vue
<ms-table-column
  prop="requirementProcessStatus"
  min-width="130px"
  :label="$t('test_track.plan.requirement_process_status')">
  <template v-slot:default="scope">
    <requirement-process-status-item
      :assessment-status="scope.row.requirementAssessmentStatus"
      :approval-status="scope.row.requirementApprovalStatus"/>
  </template>
</ms-table-column>
```

- [ ] **Step 2: Add action visibility helpers**

Add methods:

```javascript
isRequirementPlan(row) {
  return !!row.requirementNumber;
},
canSubmitContinueTest(row) {
  return this.isRequirementPlan(row)
    && row.requirementAssessmentStatus === 'PENDING'
    && row.requirementApprovalStatus === 'NONE';
},
canSubmitExemptTest(row) {
  return this.isRequirementPlan(row)
    && (row.requirementAssessmentStatus === 'PENDING'
      || row.requirementApprovalStatus === 'REJECTED');
},
canResubmitRequirement(row) {
  return this.isRequirementPlan(row)
    && row.requirementApprovalStatus === 'REJECTED';
}
```

- [ ] **Step 3: Add continue-test action**

When user clicks “继续测试”, open the existing `TestPlanEdit` dialog or a confirmation flow that requires `plannedStartTime` and `plannedEndTime`. On confirmation call:

```javascript
submitContinueTest({
  planId: row.id,
  plannedStartTime: row.plannedStartTime,
  plannedEndTime: row.plannedEndTime,
}).then(() => {
  this.$success(this.$t('commons.save_success'));
  this.initTableData();
});
```

- [ ] **Step 4: Create exempt dialog**

`RequirementExemptDialog.vue` form fields:

```javascript
form: {
  planId: '',
  requirementSystemName: '',
  requirementSummary: '',
  requirementRelatedUsers: '',
  requirementExemptReason: '',
}
```

Validation:

- `requirementSystemName` required.
- `requirementSummary` required.
- `requirementRelatedUsers` required.
- `requirementExemptReason` required.

Submit:

```javascript
submitExemptTest(this.form).then(() => {
  this.$success(this.$t('commons.save_success'));
  this.$emit('submitted');
});
```

- [ ] **Step 5: Lock key fields in edit dialog**

In `TestPlanEdit.vue`, compute:

```javascript
isRequirementSubmitted() {
  return this.form && this.form.requirementNumber
    && this.form.requirementApprovalStatus === 'SUBMITTED';
}
```

Disable key fields when `isRequirementSubmitted` is true. Allow editing again when `requirementApprovalStatus === 'REJECTED'`.

- [ ] **Step 6: Run frontend check**

```bash
cd test-track/frontend
npm run lint
```

Expected: lint succeeds.

- [ ] **Step 7: Commit**

```bash
git add test-track/frontend/src/business/plan/components/TestPlanList.vue \
  test-track/frontend/src/business/plan/components/TestPlanEdit.vue \
  test-track/frontend/src/business/plan/components/RequirementExemptDialog.vue
git commit -m "feat: add requirement workflow actions to test plan list"
```

---

## Task 8: Add Backend Integration Tests And Manual API Checks

**Files:**
- Test: `test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementPlanWorkflowServiceTest.java`
- Test: `test-track/backend/src/test/java/io/metersphere/requirement/workflow/RequirementPlanWorkflowControllerTest.java`

**Interfaces:**
- Verifies Task 3 service/controller behavior.

- [ ] **Step 1: Add service tests**

Cover:

- Continue-test sets `CONTINUE_TEST/SUBMITTED`.
- Exempt sets `EXEMPT_SUBMITTED/SUBMITTED` and stores exempt fields.
- Approval callback sets only approval status and does not change `status`.
- Rejected active plan keeps `Underway` or `Completed`.

- [ ] **Step 2: Add controller tests**

Use Spring MVC test style used by this module. Verify:

```text
POST /requirement/plan-workflow/continue-test -> 200
POST /requirement/plan-workflow/exempt-test -> 200
POST /requirement/plan-workflow/approval-callback -> 200
```

- [ ] **Step 3: Add manual curl checks for test environment**

Use these after local service starts:

```bash
curl -X POST http://localhost:8005/requirement/plan-workflow/continue-test \
  -H 'Content-Type: application/json' \
  -d '{"planId":"PLAN-REQ-001","plannedStartTime":1783440000000,"plannedEndTime":1784044800000}'

curl -X POST http://localhost:8005/requirement/plan-workflow/exempt-test \
  -H 'Content-Type: application/json' \
  -d '{"planId":"PLAN-REQ-001","requirementSystemName":"核心业务系统","requirementSummary":"需求简述","requirementRelatedUsers":"张三","requirementExemptReason":"仅文案调整，无测试影响"}'

curl -X POST http://localhost:8005/requirement/plan-workflow/approval-callback \
  -H 'Content-Type: application/json' \
  -d '{"requirementNumber":"REQ-001","approvalStatus":"APPROVED","traceId":"manual-test"}'
```

- [ ] **Step 4: Run backend test suite subset**

```bash
./mvnw -pl test-track/backend -Dtest=RequirementPlanWorkflowServiceTest,RequirementPlanWorkflowControllerTest,RequirementWorkflowStatusTest test
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add test-track/backend/src/test/java/io/metersphere/requirement/workflow
git commit -m "test: cover requirement plan workflow"
```

---

## Task 9: Final Verification And Delivery Notes

**Files:**
- Modify: `docs/superpowers/specs/2026-07-07-test-plan-requirement-workflow-design.md` if implementation decisions changed.
- Create: `docs/全流程平台对接/测试计划需求工作流上线说明.md`

**Interfaces:**
- Produces delivery documentation for deployment and stakeholder review.

- [ ] **Step 1: Run backend compile**

```bash
./mvnw -pl test-track/backend -am -DskipTests compile
```

Expected: compile succeeds.

- [ ] **Step 2: Run focused backend tests**

```bash
./mvnw -pl test-track/backend -Dtest='Requirement*Test' test
```

Expected: tests pass.

- [ ] **Step 3: Run frontend verification**

```bash
cd test-track/frontend
npm run lint
```

Expected: lint succeeds. If dependencies are missing, run `npm install` first using the project Node version from `.nvmrc`.

- [ ] **Step 4: Create上线说明**

Create `docs/全流程平台对接/测试计划需求工作流上线说明.md` with:

```markdown
# 测试计划需求工作流上线说明

## 数据库变更

- `test_plan` 新增需求评估、审批、免测说明相关字段。
- 执行 Flyway 脚本：`2.10.26/ddl/V21__alter_test_plan_requirement_workflow.sql`。

## 配置变更

- 复用现有 RocketMQ 需求回传配置。
- 需求平台审批回调需调用 `/requirement/plan-workflow/approval-callback`。

## 验证场景

1. 普通测试计划创建和状态流转不受影响。
2. 需求同步计划默认显示“待评估”。
3. 继续测试提交后显示“继续测试-已提交”。
4. 审批通过后显示“继续测试-审批通过”。
5. 审批驳回后显示“继续测试-已驳回”，并允许重新提交。
6. 提交免测后显示“免测已提交”。
7. 免测确认后不进入测试执行生命周期。

## 回滚说明

- 回滚应用版本。
- 如需回滚数据库，先确认新增字段无生产数据依赖，再按 DBA 流程删除新增字段和索引。
```

- [ ] **Step 5: Final git status**

```bash
git status --short
```

Expected: no uncommitted changes.

- [ ] **Step 6: Commit delivery notes**

```bash
git add docs/全流程平台对接/测试计划需求工作流上线说明.md \
  docs/superpowers/specs/2026-07-07-test-plan-requirement-workflow-design.md
git commit -m "docs: add requirement workflow delivery notes"
```

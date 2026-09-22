package io.metersphere.excel.listener;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestCaseNoModelDataListenerTest {

    @Test
    void normalizeNodePathTrimsEveryModuleSegment() {
        assertEquals(
                "/营销渠道/瑞众爱相随（3.0版）定期寿险/ICBS-UW-20260904-9862",
                TestCaseNoModelDataListener.normalizeNodePath(
                        "/营销渠道/瑞众爱相随（3.0版）定期寿险 /ICBS-UW-20260904-9862 "
                )
        );
    }

    @Test
    void normalizeNodePathKeepsInvalidEmptySegmentForValidation() {
        assertEquals(
                "/营销渠道//子模块",
                TestCaseNoModelDataListener.normalizeNodePath("/营销渠道//子模块")
        );
    }

    @Test
    void normalizedPathResolvesTheExactModuleInsteadOfFallingBackToProjectScope() {
        Map<String, String> pathMap = new HashMap<>();
        pathMap.put("/营销渠道/瑞众爱相随（3.0版）定期寿险/ICBS-UW-20260904-9862", "target-node");

        String normalized = TestCaseNoModelDataListener.normalizeNodePath(
                "/营销渠道/瑞众爱相随（3.0版）定期寿险 /ICBS-UW-20260904-9862 "
        );

        assertEquals("target-node", pathMap.get(normalized));
        assertNull(pathMap.get("/其他模块"));
    }
}

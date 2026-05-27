package io.metersphere.requirement.pool.producer;

import io.metersphere.requirement.pool.dto.RequirementCallbackMessage;

public interface RequirementCallbackMessageSender {

    void sendCallbackMessage(RequirementCallbackMessage msg) throws Exception;
}

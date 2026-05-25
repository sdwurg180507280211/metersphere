package io.metersphere.requirement.pool.producer;

import io.metersphere.requirement.pool.dto.RequirementSyncMessage;

public interface RequirementSyncMessageSender {

    void sendSyncMessage(RequirementSyncMessage msg) throws Exception;
}

package io.metersphere.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Author : lijiaxin
 * @date 2025-12-15 15:11
 */
@Getter
@Setter
public class AssociatedSystemRequest {
    private String id;
    private String name;
    private String workspaceId;
    private String description;
    private List<OrderRequest> orders;
}

package io.metersphere.config;

import jakarta.annotation.PostConstruct;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.mgt.SecurityManager;
import org.springframework.context.annotation.Configuration;

/**
 * Bridge Shiro SecurityManager for async dispatch threads.
 */
@Configuration
public class ShiroSecurityManagerBridgeConfig {

    private final SecurityManager securityManager;

    public ShiroSecurityManagerBridgeConfig(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @PostConstruct
    public void bindStaticSecurityManager() {
        try {
            SecurityUtils.getSecurityManager();
        } catch (UnavailableSecurityManagerException ex) {
            SecurityUtils.setSecurityManager(securityManager);
        }
    }
}

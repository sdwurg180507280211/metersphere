CREATE TABLE IF NOT EXISTS wf_model (
    id              VARCHAR(50)  NOT NULL,
    category        VARCHAR(50)  NOT NULL,
    model_key       VARCHAR(128) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    version         INT          NOT NULL,
    latest          TINYINT(1)   NOT NULL DEFAULT 1,
    xml             LONGTEXT     NOT NULL,
    svg             LONGTEXT     NULL,
    created_by      VARCHAR(50)  NULL,
    created_time    BIGINT       NOT NULL,
    updated_by      VARCHAR(50)  NULL,
    updated_time    BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wf_model_category_key_version (category, model_key, version),
    KEY idx_wf_model_category_key_latest (category, model_key, latest),
    KEY idx_wf_model_updated_time (updated_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_model_deploy (
    id                       VARCHAR(50)  NOT NULL,
    model_id                 VARCHAR(50)  NOT NULL,
    deployment_id            VARCHAR(64)  NOT NULL,
    process_definition_id    VARCHAR(64)  NULL,
    process_definition_key   VARCHAR(255) NULL,
    process_definition_name  VARCHAR(255) NULL,
    process_definition_version INT        NULL,
    deployed_time            BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_wf_model_deploy_model_id (model_id),
    KEY idx_wf_model_deploy_deployment_id (deployment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

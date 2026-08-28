-- R5.2 流程配置：可配置审批流程类型（类型定义 + 表单字段模板）
CREATE TABLE approval_type_configs (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT         NOT NULL,
    code         VARCHAR(32)    NOT NULL,            -- 流程类型编码（租户内唯一，如 expense-review）
    name         VARCHAR(64)    NOT NULL,            -- 流程名称（如 差旅报销）
    description  VARCHAR(255),                       -- 说明
    category     VARCHAR(16)    NOT NULL,            -- 映射基础审批类型（EXPENSE/PURCHASE/SALE/OTHER）
    fields       TEXT,                               -- 表单字段定义 JSON：[{key,label,type,required,options}]
    enabled      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_atc_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_atc_category CHECK (category IN ('EXPENSE', 'PURCHASE', 'SALE', 'OTHER'))
);

CREATE INDEX idx_atc_tenant ON approval_type_configs (tenant_id);

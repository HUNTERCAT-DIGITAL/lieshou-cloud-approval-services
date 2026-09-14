-- ============================================================
-- V5 · 多级审批字段（对齐 framework-approval ApprovalRequest 实体）
--
-- 背景：2026-09-12 framework submodule pin 前移后，上游实体已含
--   approverIds（逗号分隔审批人链，空 = 单步审批）
--   currentStep（0-based 当前步骤）
-- 而本仓迁移链停在 V4 → JPA ddl-auto=validate 在全新库上直接失败
-- （新环境服务起不来；本地老库因手工加过列才没暴露）。
-- ============================================================

ALTER TABLE approval_requests ADD COLUMN approver_ids VARCHAR(500);
ALTER TABLE approval_requests ADD COLUMN current_step INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN approval_requests.approver_ids IS '审批人链（逗号分隔用户 ID，多级审批；空 = 单步审批）';
COMMENT ON COLUMN approval_requests.current_step IS '当前审批步骤（0-based，多级审批用）';

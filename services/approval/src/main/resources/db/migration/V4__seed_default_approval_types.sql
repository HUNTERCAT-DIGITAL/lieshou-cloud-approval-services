-- ============================================================
-- V4 · 标准审批流程 seed：6 类（A4 · R5.2 动态表单）
--
-- 6 类：立项 / 差旅报销 / 采购付款 / 合同审批 / 用印申请 / HR 入转调离
-- 只对示例租户（id=2）seed；其他租户可自行配置（客户专属流程由交付仓 seed 扩展）。
-- fields = 表单字段模板 JSON：[{key,label,type,required,options}]
-- ============================================================

INSERT INTO approval_type_configs (tenant_id, code, name, description, category, fields, enabled)
VALUES
-- 1. 立项申请（挂 OTHER，审批通过由 approval→project Feign 自动建项目）
(2, 'project-proposal', '立项申请', '新项目立项：说明背景/目标/范围/里程碑', 'OTHER',
 '[{"key":"projectName","label":"项目名称","type":"INPUT","required":true},'
 '{"key":"orgId","label":"归属公司","type":"INPUT","required":true},'
 '{"key":"owner","label":"负责人","type":"INPUT","required":true},'
 '{"key":"budget","label":"预算(万元)","type":"NUMBER","required":false},'
 '{"key":"background","label":"立项背景","type":"TEXTAREA","required":true},'
 '{"key":"goals","label":"目标","type":"TEXTAREA","required":false},'
 '{"key":"scope","label":"范围","type":"TEXTAREA","required":false},'
 '{"key":"milestones","label":"里程碑","type":"TEXTAREA","required":false}]', TRUE),

-- 2. 差旅报销（EXPENSE）
(2, 'expense-travel', '差旅报销', '差旅费用报销：交通/住宿/餐补', 'EXPENSE',
 '[{"key":"tripDate","label":"出差日期","type":"DATE","required":true},'
 '{"key":"destination","label":"出差地点","type":"INPUT","required":true},'
 '{"key":"amount","label":"报销金额(元)","type":"NUMBER","required":true},'
 '{"key":"items","label":"费用明细","type":"TEXTAREA","required":true},'
 '{"key":"projectId","label":"关联项目","type":"INPUT","required":false}]', TRUE),

-- 3. 采购付款（PURCHASE）
(2, 'purchase-payment', '采购付款', '供应商采购付款申请', 'PURCHASE',
 '[{"key":"supplier","label":"供应商","type":"INPUT","required":true},'
 '{"key":"amount","label":"付款金额(元)","type":"NUMBER","required":true},'
 '{"key":"purpose","label":"采购用途","type":"TEXTAREA","required":true},'
 '{"key":"dueDate","label":"期望付款日","type":"DATE","required":false},'
 '{"key":"invoice","label":"发票号","type":"INPUT","required":false}]', TRUE),

-- 4. 合同审批（OTHER）
(2, 'contract-review', '合同审批', '对外合同签署审批', 'OTHER',
 '[{"key":"contractName","label":"合同名称","type":"INPUT","required":true},'
 '{"key":"counterparty","label":"对方单位","type":"INPUT","required":true},'
 '{"key":"amount","label":"合同金额(元)","type":"NUMBER","required":false},'
 '{"key":"startDate","label":"生效日期","type":"DATE","required":false},'
 '{"key":"duration","label":"合同期限","type":"INPUT","required":false},'
 '{"key":"terms","label":"关键条款","type":"TEXTAREA","required":true}]', TRUE),

-- 5. 用印申请（OTHER）
(2, 'seal-application', '用印申请', '公章/合同章/财务章使用申请', 'OTHER',
 '[{"key":"sealType","label":"印章类型","type":"SELECT","required":true,'
 '"options":["公章","合同章","财务章","法人章","发票章"]},'
 '{"key":"docName","label":"用印文件","type":"INPUT","required":true},'
 '{"key":"usePurpose","label":"用印事由","type":"TEXTAREA","required":true},'
 '{"key":"copies","label":"份数","type":"NUMBER","required":false}]', TRUE),

-- 6. HR 入转调离（OTHER）
(2, 'hr-lifecycle', 'HR 入转调离', '员工入职/转正/调岗/离职审批（接 B3）', 'OTHER',
 '[{"key":"action","label":"动作","type":"SELECT","required":true,'
 '"options":["入职","转正","调岗","离职"]},'
 '{"key":"employee","label":"员工姓名","type":"INPUT","required":true},'
 '{"key":"department","label":"部门","type":"INPUT","required":false},'
 '{"key":"position","label":"岗位","type":"INPUT","required":false},'
 '{"key":"effectiveDate","label":"生效日期","type":"DATE","required":false},'
 '{"key":"reason","label":"事由/说明","type":"TEXTAREA","required":true}]', TRUE);

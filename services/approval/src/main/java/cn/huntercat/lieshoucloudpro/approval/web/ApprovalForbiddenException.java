package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.http.HttpStatus;

import cn.huntercat.lieshoucloudpro.common.api.BaseException;

/** 无权操作（非审批人审批 / 非发起人撤销 → 403 FORBIDDEN · ADR-0032 · L2-1 基类下沉）. */
public class ApprovalForbiddenException extends BaseException {
  public ApprovalForbiddenException(String message) {
    super("FORBIDDEN", HttpStatus.FORBIDDEN, message);
  }
}

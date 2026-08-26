package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.http.HttpStatus;

import cn.huntercat.lieshoucloudpro.common.api.BaseException;

/** 状态机冲突（非 PENDING 的单据尝试审批/撤销 → 409 ALREADY_DECIDED · ADR-0032 · L2-1 基类下沉）. */
public class AlreadyDecidedException extends BaseException {
  public AlreadyDecidedException(String message) {
    super("ALREADY_DECIDED", HttpStatus.CONFLICT, message);
  }
}

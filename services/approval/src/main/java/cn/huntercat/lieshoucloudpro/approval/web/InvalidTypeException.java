package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.http.HttpStatus;

import cn.huntercat.lieshoucloudpro.common.api.BaseException;

/** 审批类型非法（→ 400 INVALID_TYPE · L2-1 基类下沉）. */
public class InvalidTypeException extends BaseException {
  public InvalidTypeException(String value) {
    super(
        "INVALID_TYPE",
        HttpStatus.BAD_REQUEST,
        "非法审批类型: " + value + "（仅支持 EXPENSE / PURCHASE / SALE / OTHER）");
  }
}

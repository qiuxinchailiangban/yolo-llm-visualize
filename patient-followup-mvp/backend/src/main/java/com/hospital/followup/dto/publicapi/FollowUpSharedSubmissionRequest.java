package com.hospital.followup.dto.publicapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 共享随访问卷的提交请求。患者通过模板级二维码扫码后，自报姓名+手机号，
 * 后端用「手机号 + 姓名」匹配到既有患者，并定位 ta 在该阶段未完成的随访任务。
 */
public record FollowUpSharedSubmissionRequest(
    @NotBlank(message = "扫码 token 不能为空") String token,
    @NotBlank(message = "请填写您的姓名") String name,
    @NotBlank(message = "请填写您的手机号") String phone,
    @NotNull(message = "answers 不能为空") Map<String, Object> answers
) {
}

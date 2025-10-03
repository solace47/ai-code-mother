package com.spring.aicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.spring.aicodemother.model.entity.EmailVerificationCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮箱验证码 Mapper 接口
 *
 * @author lambor
 */
@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCode> {
}

/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package com.anji.captcha.service.impl;

import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.iquicker.framework.base.exception.ServiceException;
import com.iquicker.framework.base.model.web.R;
import com.iquicker.framework.utils.StringUtils;

import java.util.Properties;

/**
 * Created by raodeming on 2019/12/25.
 */
public class DefaultCaptchaServiceImpl extends AbstractCaptchaService{

    @Override
    public String captchaType() {
        return "default";
    }

    @Override
    public void init(Properties config) {
        for (String s : CaptchaServiceFactory.instances.keySet()) {
            if(captchaType().equals(s)){
                continue;
            }
            getService(s).init(config);
        }
    }

	@Override
	public void destroy(Properties config) {
		for (String s : CaptchaServiceFactory.instances.keySet()) {
			if(captchaType().equals(s)){
				continue;
			}
			getService(s).destroy(config);
		}
	}

	private CaptchaService getService(String captchaType){
        return CaptchaServiceFactory.instances.get(captchaType);
    }

    @Override
    public R<?> get(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            throw new ServiceException("captchaVO是null");
//            return RepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (!StringUtils.hasLength(captchaVO.getCaptchaType())) {
            throw new ServiceException("CaptchaType为空");
//            return RepCodeEnum.NULL_ERROR.parseError("类型");
        }
        return getService(captchaVO.getCaptchaType()).get(captchaVO);
    }

    @Override
    public R check(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            throw new ServiceException("captchaVO是null");
//            return RepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (!StringUtils.hasLength(captchaVO.getCaptchaType())) {
            throw new ServiceException("CaptchaType为空");
//            return RepCodeEnum.NULL_ERROR.parseError("类型");
        }
        if (!StringUtils.hasLength(captchaVO.getToken())) {
            throw new ServiceException("token");
//            return RepCodeEnum.NULL_ERROR.parseError("token");
        }
        return getService(captchaVO.getCaptchaType()).check(captchaVO);
    }

    @Override
    public R verification(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            throw new ServiceException("captchaVO是null");
//            return RepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (!StringUtils.hasLength(captchaVO.getCaptchaVerification())) {
            throw new ServiceException("二次校验参数是null");
//            return RepCodeEnum.NULL_ERROR.parseError("二次校验参数");
        }
        try {
            String codeKey = String.format(REDIS_SECOND_CAPTCHA_KEY, captchaVO.getCaptchaVerification());
            if (!CaptchaServiceFactory.getCache(cacheType).exists(codeKey)) {
                throw new ServiceException("验证码已失效，请重新获取");
//                return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_INVALID);
            }
            //二次校验取值后，即刻失效
            CaptchaServiceFactory.getCache(cacheType).delete(codeKey);
        } catch (Exception e) {
            logger.error("验证码坐标解析失败", e);
            throw new ServiceException("验证码坐标解析失败", e);
//            return ResponseModel.errorMsg(e.getMessage());
        }
        return R.success();
    }

}

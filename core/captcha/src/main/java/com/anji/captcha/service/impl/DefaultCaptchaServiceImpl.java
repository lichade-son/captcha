/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package com.anji.captcha.service.impl;

import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.iquicker.framework.base.constants.StatusConstant;
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
    public R<CaptchaVO> get(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            return R.error(StatusConstant.ErrorStatus.MISSING_ARGUMENT, "captchaVO不能是null");
        }
        if (!StringUtils.hasLength(captchaVO.getCaptchaType())) {
            return R.error(StatusConstant.ErrorStatus.MISSING_ARGUMENT, "验证码类型不能为空");
        }
        return getService(captchaVO.getCaptchaType()).get(captchaVO);
    }

    @Override
    public R check(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            return R.error(StatusConstant.ErrorStatus.MISSING_ARGUMENT, "captchaVO不能是null");
        }
        if (!StringUtils.hasLength(captchaVO.getCaptchaType())) {
            return R.error(StatusConstant.ErrorStatus.MISSING_ARGUMENT, "验证码类型不能为空");
        }
        if (!StringUtils.hasLength(captchaVO.getToken())) {
            return R.error(StatusConstant.ErrorStatus.MISSING_ARGUMENT, "token不能为空");
        }
        return getService(captchaVO.getCaptchaType()).check(captchaVO);
    }

    @Override
    public R verification(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            return R.error(StatusConstant.ErrorStatus.MISSING_ARGUMENT, "captchaVO不能是null");
        }
        if (!StringUtils.hasLength(captchaVO.getCaptchaVerification())) {
            return R.error(StatusConstant.ErrorStatus.MISSING_ARGUMENT, "二次校验参数不能是null");
        }
        try {
            String codeKey = String.format(REDIS_SECOND_CAPTCHA_KEY, captchaVO.getCaptchaVerification());
            if (!CaptchaServiceFactory.getCache(cacheType).exists(codeKey)) {
                return R.error(StatusConstant.ErrorStatus.SERVICE_EXCEPTION, "验证码已失效，请重新获取");
            }
            //二次校验取值后，即刻失效
            CaptchaServiceFactory.getCache(cacheType).delete(codeKey);
        } catch (Exception e) {
            logger.error("验证码坐标解析失败", e);
            return R.error(StatusConstant.ErrorStatus.SERVICE_EXCEPTION, "验证码坐标解析失败");
        }
        return R.success();
    }

}

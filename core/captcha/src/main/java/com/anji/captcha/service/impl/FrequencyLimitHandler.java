package com.anji.captcha.service.impl;

import com.anji.captcha.model.common.Const;
import com.anji.captcha.model.common.RepCodeEnum;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaCacheService;
import com.iquicker.framework.base.constants.StatusConstant;
import com.iquicker.framework.base.exception.ServiceException;
import com.iquicker.framework.base.model.web.R;
import com.iquicker.framework.utils.StringUtils;

import java.util.Objects;
import java.util.Properties;

/**
 * @author WongBin
 * @date 2021/1/21
 */
public interface FrequencyLimitHandler {

    String LIMIT_KEY = "AJ.CAPTCHA.REQ.LIMIT-%s-%s";

    /**
     * get 接口限流
     *
     * @param captchaVO
     * @return
     */
    R<CaptchaVO> validateGet(CaptchaVO captchaVO);

    /**
     * check接口限流
     *
     * @param captchaVO
     * @return
     */
    R<?> validateCheck(CaptchaVO captchaVO);

    /**
     * verify接口限流
     *
     * @param captchaVO
     * @return
     */
    R<?> validateVerify(CaptchaVO captchaVO);


    /***
     * 验证码接口限流:
     *      客户端ClientUid 组件实例化时设置一次，如：场景码+UUID，客户端可以本地缓存,保证一个组件只有一个值
     *
     * 针对同一个客户端的请求，做如下限制:
     * get
     * 	 1分钟内check失败5次，锁定5分钟
     * 	 1分钟内不能超过120次。
     * check:
     *   1分钟内不超过600次
     * verify:
     *   1分钟内不超过600次
     */
    class DefaultLimitHandler implements FrequencyLimitHandler {
        private final Properties config;
        private final CaptchaCacheService cacheService;

        public DefaultLimitHandler(Properties config, CaptchaCacheService cacheService) {
            this.config = config;
            this.cacheService = cacheService;
        }

        private String getClientCId(CaptchaVO input, String type) {
            return String.format(LIMIT_KEY ,type,input.getClientUid());
        }

        @Override
        public R<CaptchaVO> validateGet(CaptchaVO d) {
        	// 无客户端身份标识，不限制
        	if(!StringUtils.hasLength(d.getClientUid())){
        		return null;
			}
            String getKey = getClientCId(d, "GET");
            String lockKey = getClientCId(d, "LOCK");
            // 失败次数过多，锁定
            if (Objects.nonNull(cacheService.get(lockKey))) {
                return R.error(StatusConstant.ErrorStatus.TOO_MANY_REQUESTS, "接口验证失败数过多，请稍后再试");
            }
            String getCnts = cacheService.get(getKey);
            if (Objects.isNull(getCnts)) {
                cacheService.set(getKey, "1", 60);
                getCnts = "1";
            }
            cacheService.increment(getKey, 1);
            // 1分钟内请求次数过多
            if (Long.parseLong(getCnts) > Long.parseLong(config.getProperty(Const.REQ_GET_MINUTE_LIMIT, "120"))) {
                return R.error(StatusConstant.ErrorStatus.TOO_MANY_REQUESTS, "get接口请求次数超限，请稍后再试!");
            }

            // 失败次数验证
            String failKey = getClientCId(d, "FAIL");
            String failCnts = cacheService.get(failKey);
            // 没有验证失败，通过校验
            if (Objects.isNull(failCnts)) {
                return null;
            }
            // 1分钟内失败5次
            if (Long.parseLong(failCnts) > Long.parseLong(config.getProperty(Const.REQ_GET_LOCK_LIMIT, "5"))) {
                // get接口锁定5分钟
                cacheService.set(lockKey, "1", Long.parseLong(config.getProperty(Const.REQ_GET_LOCK_SECONDS, "300")));
                return R.error(StatusConstant.ErrorStatus.TOO_MANY_REQUESTS, "接口验证失败数过多，请稍后再试");
            }
            return null;
        }

        @Override
        public R validateCheck(CaptchaVO d) {
			// 无客户端身份标识，不限制
			if(!StringUtils.hasLength(d.getClientUid())){
				return null;
			}
            /*String getKey = getClientCId(d, "GET");
            if(Objects.isNull(cacheService.get(getKey))){
                return ResponseModel.errorMsg(RepCodeEnum.API_REQ_INVALID);
            }*/
            String key = getClientCId(d, "CHECK");
            String v = cacheService.get(key);
            if (Objects.isNull(v)) {
                cacheService.set(key, "1", 60);
                v = "1";
            }
            cacheService.increment(key, 1);
            if (Long.parseLong(v) > Long.parseLong(config.getProperty(Const.REQ_CHECK_MINUTE_LIMIT, "600"))) {
                return R.error(StatusConstant.ErrorStatus.TOO_MANY_REQUESTS, "check接口请求次数超限，请稍后再试!");
            }
            return null;
        }

        @Override
        public R validateVerify(CaptchaVO d) {
            /*String getKey = getClientCId(d, "GET");
            if(Objects.isNull(cacheService.get(getKey))){
                return ResponseModel.errorMsg(RepCodeEnum.API_REQ_INVALID);
            }*/
            String key = getClientCId(d, "VERIFY");
            String v = cacheService.get(key);
            if (Objects.isNull(v)) {
                cacheService.set(key, "1", 60);
                v = "1";
            }
            cacheService.increment(key, 1);
            if (Long.parseLong(v) > Long.parseLong(config.getProperty(Const.REQ_VALIDATE_MINUTE_LIMIT, "600"))) {
                return R.error(StatusConstant.ErrorStatus.TOO_MANY_REQUESTS, "verify请求次数超限!");
            }
            return null;
        }
    }

}
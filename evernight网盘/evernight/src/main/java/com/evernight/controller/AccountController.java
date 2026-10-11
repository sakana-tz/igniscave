package com.evernight.controller;


import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.base.exception.BusinessException;
import com.evernight.components.captchaComponent.CaptchaDto;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import com.evernight.service.AccountService;
import com.evernight.service.CaptchaService;
import com.evernight.service.FileOssService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
public class AccountController extends ABaseController {

    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private FileOssService fileOssService;


    /**
     * 发送登录图像验证码(base64)
     * <p>CaptchaDto字段为:
     * <p>captchaKey :字符串类型的key,
     * <p>captchaImage :字符串类型的Base64图片
     */
    @RequestMapping("/getCaptcha")
    public ResponseVO getCaptcha() {
        // 生成验证码
        CaptchaDto captchaDto = captchaService.generateCaptcha();
        // 响应验证码
        return getSuccessResponseVO(captchaDto);
    }

    /**
     * 发送邮箱验证码
     */
    @RequestMapping("/getEmailCaptcha")
    public ResponseVO getEmailCaptcha(@RequestParam("email") @Email String email) {
        // 生成并发送验证码
        captchaService.generateEmailCaptcha(email);
        // 成功响应
        return getSuccessResponseVO(null);
    }


    /**
     * 登录(邮箱验证码或密码登录)
     */
    @RequestMapping("/login")
    public ResponseVO login(
            @RequestParam(value = "email") @Email String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "checkCodeKey", required = false) String checkCodeKey,
            @RequestParam(value = "checkCode", required = false) String checkCode,
            @RequestParam(value = "emailCheckCode", required = false) String emailCheckCode

    ) {
        String jwt;
        // 验证邮箱验证码登录
        if (StringUtils.hasText(emailCheckCode)) {
            captchaService.checkCaptcha(email, emailCheckCode);
            jwt = accountService.login(email, null, getIpAddr());
        }
        // 验证密码登录参数
        else if (StringUtils.hasText(password) && StringUtils.hasText(checkCodeKey) && StringUtils.hasText(checkCode)) {
            captchaService.checkCaptcha(checkCodeKey, checkCode);
            jwt = accountService.login(email, password, getIpAddr());
        } else {
            throw new BusinessException(ResponseCodeEnum.CODE_400);
        }
        saveToken2Cookie(USER_TOKEN_JWT, jwt, 7 * 24 * 60 * 60);
        return getSuccessResponseVO(jwt);
    }


    /**
     * 自动登录
     * <p>
     * 无需传参，@UserInfoValidation注解作用：自动从请求头解析
     */
    @RequestMapping("/autoLogin")
    public ResponseVO autoLogin(@UserInfoValidation UserSessionDto userSessionDto) {
        return getSuccessResponseVO(null);
    }


    /**
     * 登出
     */
    @RequestMapping("/logout")
    public ResponseVO logout(@UserInfoValidation UserSessionDto userSessionDto) {
        // 从Cookie删除userTokenJwt
        saveToken2Cookie(USER_TOKEN_JWT, null, 0);
        // 从redis删除session
        accountService.logout(userSessionDto.getUserId());
        return getSuccessResponseVO(null);
    }


    /**
     * 用户注册
     *
     * @param username       用户名
     * @param password       密码
     * @param email          邮箱
     * @param emailCheckCode 邮箱验证码
     */
    @RequestMapping("/register")
    public ResponseVO register(
            @RequestParam("username") @Size(min = 2, max = 30) String username,
            @RequestParam("password") @Size(min = 6, max = 42) String password,
            @RequestParam("email") @Email String email,
            @RequestParam("emailCheckCode") String emailCheckCode
    ) {
        // 邮箱验证码校验
        captchaService.checkCaptcha(email, emailCheckCode);

        // 调用service方法,用户注册
        accountService.register(username, password, email);

        // 返回登录结果
        return getSuccessResponseVO(null);
    }


    /**
     * 修改账户信息
     *
     * @param username 用户名
     * @param password 密码
     */
    @RequestMapping("/updateAccount")
    public ResponseVO updateAccount(
            @RequestParam(required = false) @Size(min = 2, max = 30) String username,
            @RequestParam(required = false) @Size(min = 6, max = 42) String password,
            @UserInfoValidation UserSessionDto userSessionDto // 自动获取 前端无需传递
    ) {
        // 调用service进行修改
        accountService.updateAccount(username, password, userSessionDto);

        // 从Cookie删除userTokenJwt信息
        saveToken2Cookie(USER_TOKEN_JWT, null, 0);
        // 从redis删除session
        accountService.logout(userSessionDto.getUserId());

        // 返回结果
        return getSuccessResponseVO(null);

    }


    /**
     * 获取头像上传预签名URL（前端直传OSS）
     *
     */
    @RequestMapping("/getAvatarUploadUrl")
    public ResponseVO getAvatarUploadUrl(@UserInfoValidation UserSessionDto userSessionDto) {
        return getSuccessResponseVO(fileOssService.getAvatarUploadUrl(userSessionDto.getUserId()));
    }

    /**
     * 确认头像上传完成
     *
     */
    @RequestMapping("/confirmAvatarUpload")
    public ResponseVO confirmAvatarUpload(@UserInfoValidation UserSessionDto userSessionDto) {
        return getSuccessResponseVO(fileOssService.confirmAvatarUpload(userSessionDto.getUserId()));
    }

    /**
     * 获取用户头像
     *
     */
    @RequestMapping("/getAvatar")
    public ResponseVO getAvatar(@UserInfoValidation UserSessionDto userSessionDto) {
        String url = fileOssService.getAvatar(userSessionDto.getUserId());
        return getSuccessResponseVO(url);
    }

    /**
     * 获取用户信息
     */
    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(@UserInfoValidation UserSessionDto userSessionDto) {
        return getSuccessResponseVO(userSessionDto);
    }




//    /**
//     * 禁用用户
//     */
//    private void disableUser(Integer userId) {
//        // 禁用用户
//        accountService.disableUser(userId);
//        // 登出
//        accountService.logout(userId);
//        // 删除用户token
//        saveToken2Cookie(USER_TOKEN_JWT, null, 0);
//    }
}
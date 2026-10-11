package com.evernight.controller.admin;

import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.service.FileService;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 文件表 Controller（管理员）
 */
@RestController
@RequestMapping("/admin/file")
public class AdminFileController extends ABaseController {

    @Resource
    private FileService fileService;

    /**
     * 查询存储总文件大小<br>
     * 无需传参，@UserInfoValidation注解作用：自动从请求头解析
     */
    @RequestMapping("totalFileSize")
    public ResponseVO totalFileSize(@UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return getSuccessResponseVO(fileService.getStorageTotalSize(userSessionDto));
    }

    /**
     * 查询文件总数<br>
     * 无需传参，@UserInfoValidation注解作用：自动从请求头解析
     */
    @RequestMapping("fileTotalCount")
    public ResponseVO fileTotalCount(@UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return getSuccessResponseVO(fileService.getFileTotalCount(userSessionDto));
    }

}

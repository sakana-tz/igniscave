package com.evernight.controller;

import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.entity.annotation.CheckFilePath;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import com.evernight.service.FileOssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
public class FileController extends ABaseController {

    @Autowired
    private FileOssService fileOssService;


    /**
     * 获取文件上传预签名URL（前端直传OSS）
     *
     * @param fileName    文件名
     * @param fileSize    文件大小（字节）
     * @param contentType 文件MIME类型
     * @param path        文件存储路径
     *                    <br> @UserInfoValidation注解作用：自动从请求头解析，无需传该参数
     */
    @RequestMapping("/getUploadUrl")
    public ResponseVO getUploadUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("contentType") String contentType,
            @RequestParam("path") @CheckFilePath(isDirectory = true, formatCorrection = true) String path,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(
                fileOssService.getUploadUrl(fileName, fileSize, contentType, path, userSessionDto)
        );
    }

    /**
     * 确认文件上传完成
     *
     * @param ossKey      文件在OSS上的存储路径
     * @param fileName    文件名
     * @param fileSize    文件大小（字节）
     * @param contentType 文件MIME类型
     * @param path        文件存储路径
     *                    <br> @UserInfoValidation注解作用：自动从请求头解析，无需传该参数
     */
    @RequestMapping("/confirmUpload")
    public ResponseVO confirmUpload(
            @RequestParam("ossKey") String ossKey,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("contentType") String contentType,
            @RequestParam("path") @CheckFilePath(isDirectory = true, formatCorrection = true) String path,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        fileOssService.confirmUpload(ossKey, fileName, fileSize, contentType, path, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取用户文件列表
     */
    @RequestMapping("/getUserFileList")
    public ResponseVO getUserFileList(@UserInfoValidation UserSessionDto userSessionDto) {
        return getSuccessResponseVO(fileOssService.getUserFileList(userSessionDto.getUserId()));
    }

    /**
     * 获取用户文件URL
     *
     * @param fileId 文件id
     */
    @RequestMapping("/getUserFile")
    public ResponseVO getUserFile(
            @RequestParam("fileId") Long fileId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(fileOssService.getUserFile(fileId, userSessionDto));
    }

    /**
     * 删除用户文件到回收站，或从回收站恢复用户文件
     *
     * @param fileId 文件id
     */
    @RequestMapping("/delOrResUserFile")
    public ResponseVO delOrResUserFile(
            @RequestParam("fileId") Long fileId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        fileOssService.delOrResUserFile(fileId, userSessionDto);

        return getSuccessResponseVO(null);
    }

    /**
     * 彻底删除用户文件
     */
    @RequestMapping("/deleteFileByFileId")
    public ResponseVO deleteFileByFileId(
            @RequestParam("fileId") Long fileId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        fileOssService.deleteFileByFileId(fileId, userSessionDto);

        return getSuccessResponseVO(null);
    }

    /**
     * 修改用户文件路径
     */
    @RequestMapping("/updateUserFilePath")
    public ResponseVO updateUserFilePath(
            @RequestParam("fileId") Long fileId,
            @RequestParam("path") @CheckFilePath(isDirectory = true, formatCorrection = true) String path,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        fileOssService.updateUserFilePath(fileId, path, userSessionDto);

        return getSuccessResponseVO(null);

    }


    /**
     * 用户文件共享
     */
    @RequestMapping("/shareUserFile")
    public ResponseVO shareUserFile(
            @RequestParam("fileId") Long fileId,
            @RequestParam("limitSecond") Integer limitSecond,
            @RequestParam("limitCount") Integer limitCount,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(
                fileOssService.shareUserFile(fileId, limitSecond, limitCount, userSessionDto)
        );
    }


    /**
     * 获取共享文件Url
     */
    @RequestMapping("/getShareFileUrl/{shareKey}")
    public ResponseVO getShareFileUrl(
            @PathVariable("shareKey") String shareKey
    ) {
        return getSuccessResponseVO(
                fileOssService.getShareFileUrl(shareKey)
        );
    }




}

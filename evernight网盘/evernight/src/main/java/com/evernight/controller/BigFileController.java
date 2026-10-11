package com.evernight.controller;

import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.entity.annotation.CheckFilePath;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import com.evernight.service.BigFileOssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/file")
public class BigFileController extends ABaseController {

    @Autowired
    private BigFileOssService bigFileOssService;

    /**
     * 上传状态查询,不要频繁调用，不要每上传一次调用一次
     * @param uploadId 上传ID
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     */
    @RequestMapping("/getUploadStatus")
    public ResponseVO getUploadStatus(
            @RequestParam("uploadId") String uploadId,
            @UserInfoValidation UserSessionDto userSessionDto
    ){
        return getSuccessResponseVO(
                bigFileOssService.getChunkStatus(uploadId,userSessionDto)
        );
    }

    /**
     * 文件预上传（前端直传OSS模式）
     * <p>
     * 后端初始化OSS分片上传，返回uploadId、分片大小、分片总数、以及每个分片的预签名PUT URL。
     * 前端拿到预签名URL后直接将各分片PUT到OSS，不经过后端。
     *
     * @param path     文件路径
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @return MultipartUploadInitDto（含uploadId、chunkSize、totalChunks、presignedUrls）
     */
    @RequestMapping("/preUploadFile")
    public ResponseVO preUploadFile(
            @RequestParam("path") @CheckFilePath(isDirectory = true, formatCorrection = true) String path,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(
                bigFileOssService.initMultipartUpload(path, fileName, fileSize, userSessionDto)
        );
    }

    /**
     * 上报分片上传完成
     * <p>
     * 前端使用预签名URL将分片直接上传到OSS后，调用此方法上报已成功上传的分片编号，
     * 后端更新Redis分片状态。
     *
     * @param uploadId      上传ID
     * @param uploadedParts 已成功上传的分片编号列表（从1开始）
     */
    @RequestMapping("/reportUploadedParts")
    public ResponseVO reportUploadedParts(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("uploadedParts") List<Integer> uploadedParts,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        bigFileOssService.reportUploadedParts(uploadId, uploadedParts, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 重新获取分片预签名URL（断点续传时URL过期后调用）
     * <p>
     * 不会重新初始化上传，仅为指定分片生成新的预签名PUT URL。
     * 适用于 uploadId 未过期但预签名URL已过期的场景。
     *
     * @param uploadId    上传ID
     * @param partNumbers 需要重新获取URL的分片编号列表（从1开始），为空则返回所有分片
     */
    @RequestMapping("/refreshPresignedUrls")
    public ResponseVO refreshPresignedUrls(
            @RequestParam("uploadId") String uploadId,
            @RequestParam(value = "partNumbers", required = false) List<Integer> partNumbers,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(
                bigFileOssService.refreshPresignedUrls(uploadId, partNumbers, userSessionDto)
        );
    }

    /**
     * 合并文件，完成分片上传
     * @param uploadId 上传ID
     *
     */
    @RequestMapping("/mergeFile")
    public ResponseVO mergeFile(
            @RequestParam("uploadId") String uploadId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        bigFileOssService.completeMultipartUpload(uploadId, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 检查待恢复的上传（页面刷新后断点续传）
     * <p>
     * 前端通过文件路径和文件名查询是否存在未完成的上传任务，
     * 如果存在则返回uploadId和分片上传状态，前端可据此恢复上传。
     *
     * @param path     文件路径
     * @param fileName 文件名
     */
    @RequestMapping("/checkPendingUpload")
    public ResponseVO checkPendingUpload(
            @RequestParam("path") @CheckFilePath(isDirectory = true) String path,
            @RequestParam("fileName") String fileName,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(
                bigFileOssService.checkPendingUpload(path, fileName, userSessionDto)
        );
    }

    /**
     * 大文件分片下载
     * <p>返回OSS预签名URL及分片信息，前端使用Range请求头实现分片下载和断点续传</p>
     * <p>前端使用方式：</p>
     * <ul>
     *   <li>根据返回的 chunkSize 计算各分片范围</li>
     *   <li>每次请求携带 Range: bytes=start-end 请求头</li>
     *   <li>断点恢复时，从上次未完成的分片继续请求即可</li>
     * </ul>
     *
     * @param fileId 文件ID
     */
    @RequestMapping("/downloadBigFile")
    public ResponseVO downloadBigFile(
            @RequestParam("fileId") Long fileId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(
                bigFileOssService.downloadBigFile(fileId, userSessionDto)
        );
    }


}

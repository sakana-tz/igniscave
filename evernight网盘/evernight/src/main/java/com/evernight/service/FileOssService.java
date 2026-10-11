package com.evernight.service;

import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.po.File;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.mapper.FileMapper;
import com.evernight.components.aliOssComponent.FileProperties;
import com.evernight.components.aliOssComponent.FileRedisComponent;
import com.evernight.components.aliOssComponent.FileRedisProperties;
import com.evernight.components.aliOssComponent.OssComponent;
import com.evernight.entity.dto.FileDownloadInfo;
import com.evernight.entity.dto.FileTreeDto;
import com.evernight.entity.dto.ShareFileDto;
import com.evernight.entity.dto.ShareFileToUserDto;
import com.evernight.entity.dto.UserSessionDto;
import com.evernight.entity.dto.FileUploadInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.evernight.components.captchaComponent.CaptchaComponent.getRandomString;

@Service
public class FileOssService {

    @Autowired
    private OssComponent ossComponent;
    @Autowired
    private FileMapper<File, Object> fileMapper;
    @Autowired
    private FileRedisComponent fileRedisComponent;
    @Autowired
    private FileRedisProperties fileRedisProperties;
    @Autowired
    private FileProperties fileProperties;

    /**
     * 获取头像上传预签名URL（前端直传OSS）
     *
     * @param userId 用户id
     * @return 预签名上传URL
     */
    public String getAvatarUploadUrl(Integer userId) {
        String ossKey = fileProperties.getAvatarKey() + userId + ".png";
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        return ossComponent.generatePresignedUploadUrl(ossKey, "image/png", expiration);
    }

    /**
     * 确认头像上传完成
     *
     * @param userId 用户id
     * @return 头像访问地址
     */
    public String confirmAvatarUpload(Integer userId) {
        String ossKey = fileProperties.getAvatarKey() + userId + ".png";
        if (!ossComponent.doesObjectExist(ossKey)) {
            throw new BusinessException(ResponseCodeEnum.CODE_610);
        }
        return getAvatar(userId);
    }

    /**
     * 获取头像
     *
     * @param userId 用户id
     * @return 文件访问地址
     */
    public String getAvatar(Integer userId) {
        String key = ossComponent.doesObjectExist(fileProperties.getAvatarKey() + userId + ".png")
                ? fileProperties.getAvatarKey() + userId + ".png"
                : fileProperties.getAvatarKey() + "default.png";
        return ossComponent.getFileDownloadInfo(key).getUrl();
    }






    /**
     * 获取文件上传预签名URL（前端直传OSS）
     *
     * @param fileName       文件名
     * @param fileSize       文件大小（字节）
     * @param contentType    文件MIME类型
     * @param path           文件存储路径
     * @param userSessionDto 用户会话信息
     * @return 文件上传信息（含预签名URL和ossKey）
     */
    public FileUploadInfo getUploadUrl(String fileName, Long fileSize, String contentType, String path, UserSessionDto userSessionDto) {
        // 文件大小校验
        if (fileSize > fileProperties.getMaxFileSize()) {
            throw new BusinessException(ResponseCodeEnum.CODE_611);
        }

        // redis用户文件上传流量限制
        checkAndIncrementUserTraffic(fileSize, userSessionDto.getUserId());

        // 拼接ossKey
        String ossKey = fileProperties.getUserFileKey() + userSessionDto.getUserId() + "/" + path + fileName;

        // 生成预签名上传URL（1小时有效）
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        String uploadUrl = ossComponent.generatePresignedUploadUrl(ossKey, contentType, expiration);

        return new FileUploadInfo()
                .setUploadUrl(uploadUrl)
                .setOssKey(ossKey);
    }

    /**
     * 确认文件上传完成，入库记录
     *
     * @param ossKey         文件在OSS上的存储路径
     * @param fileName       文件名
     * @param fileSize       文件大小（字节）
     * @param contentType    文件MIME类型
     * @param path           文件存储路径
     * @param userSessionDto 用户会话信息
     */
    public void confirmUpload(String ossKey, String fileName, Long fileSize, String contentType, String path, UserSessionDto userSessionDto) {
        // 验证文件确实已上传到OSS
        if (!ossComponent.doesObjectExist(ossKey)) {
            throw new BusinessException(ResponseCodeEnum.CODE_610);
        }

        // 保存文件信息
        fileMapper.insert(new File()
                .setUserId(userSessionDto.getUserId())
                .setFileName(fileName)
                .setFilePath(path + fileName)
                .setFileType(contentType)
                .setFileSize(fileSize)
        );

        // 清理redis旧文件列表缓存
        fileRedisComponent.delete(fileRedisProperties.getUserFileListKey() + userSessionDto.getUserId());
    }

    /**
     * 获取用户文件列表
     *
     *
     */
    public List<FileTreeDto> getUserFileList(Integer userId) {
        String cacheKey = fileRedisProperties.getUserFileListKey() + userId;
        if (fileRedisComponent.exists(cacheKey)) {
            List<FileTreeDto> cached = fileRedisComponent.getList(cacheKey, FileTreeDto.class);
            if (cached != null) return cached;
        }

        // mysql文件列表查询
        List<File> fileList = fileMapper.selectListByUserId(userId);
        List<FileTreeDto> fileTreeDtoList = new FileTreeDto().files2Tree(fileList);
        // redis文件列表缓存优化
        fileRedisComponent.setEx(fileRedisProperties.getUserFileListKey() + userId, fileTreeDtoList, fileRedisProperties.getUserFileListTimeout(), TimeUnit.SECONDS);

        return fileTreeDtoList;
    }

    /**
     * 获取用户文件（返回预签名下载信息，前端直接下载）
     *
     * @param fileId 文件id
     *
     */
    public FileDownloadInfo getUserFile(Long fileId, UserSessionDto userSessionDto) {
        // 查询存储键
        List<FileTreeDto> fileTreeDtoList = getUserFileList(userSessionDto.getUserId());
        String key = FileTreeDto.findFileTreeDtoById(fileTreeDtoList, fileId).getPath();

        if (key == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        return ossComponent.getFileDownloadInfo(fileProperties.getUserFileKey() + userSessionDto.getUserId() + "/" + key);
    }

    /**
     * 删除/恢复用户文件
     *
     * @param fileId 文件id
     *
     */
    public void delOrResUserFile(Long fileId, UserSessionDto userSessionDto) {
        // 查询存储键
        List<FileTreeDto> fileTreeDtoList = getUserFileList(userSessionDto.getUserId());
        FileTreeDto fileTreeDto = FileTreeDto.findFileTreeDtoById(fileTreeDtoList, fileId);
        if (fileTreeDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // mysql更新
        fileMapper.updateByFileId(new File().setIsDeleted(fileTreeDto.getDel() ? 0 : 1), fileId);

        // 清理redis旧文件列表缓存
        fileRedisComponent.delete(fileRedisProperties.getUserFileListKey() + userSessionDto.getUserId());

    }

    /**
     * 彻底删除文件
     *
     * @param fileId         文件ID
     * @param userSessionDto 用户会话信息
     */
    public void deleteFileByFileId(Long fileId, UserSessionDto userSessionDto) {
        // 查询存储键
        List<FileTreeDto> fileTreeDtoList = getUserFileList(userSessionDto.getUserId());
        FileTreeDto fileTreeDto = FileTreeDto.findFileTreeDtoById(fileTreeDtoList, fileId);
        if (fileTreeDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // mysql 删除
        fileMapper.deleteByFileId(fileId);

        // 清理redis旧文件列表缓存
        fileRedisComponent.delete(fileRedisProperties.getUserFileListKey() + userSessionDto.getUserId());

    }

    /**
     * 更新用户文件路径
     *
     * @param fileId         文件ID
     * @param path           新的文件路径
     * @param userSessionDto 用户会话信息
     */
    public void updateUserFilePath(Long fileId, String path, UserSessionDto userSessionDto) {
        // 查询存储键
        List<FileTreeDto> fileTreeDtoList = getUserFileList(userSessionDto.getUserId());
        FileTreeDto fileTreeDto = FileTreeDto.findFileTreeDtoById(fileTreeDtoList, fileId);
        if (fileTreeDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // mysql 更新
        fileMapper.updateByFileId(new File().setFilePath(path + fileTreeDto.getFileName()), fileId);

        // 清理redis旧文件列表缓存
        fileRedisComponent.delete(fileRedisProperties.getUserFileListKey() + userSessionDto.getUserId());
    }


    /**
     * 分享用户文件
     *
     * @param fileId      文件id
     * @param limitSecond 分享有效期（秒）
     *
     */
    public String shareUserFile(Long fileId, Integer limitSecond, Integer limitCount, UserSessionDto userSessionDto) {
        // 查询存储键
        List<FileTreeDto> fileTreeDtoList = getUserFileList(userSessionDto.getUserId());
        FileTreeDto fileTreeDto = FileTreeDto.findFileTreeDtoById(fileTreeDtoList, fileId);
        if (fileTreeDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // 生成随机密钥
        String shareKey = getRandomString(32);

        // 分享时间限制
        limitSecond = Math.min(fileProperties.getShareLimitSeconds(), limitSecond);
        // 分享次数限制
        limitCount = Math.min(fileProperties.getShareLimitCount(), limitCount);

        // 封装类
        ShareFileDto shareFileDto = new ShareFileDto() // redis缓存部分
                .setShareKey(shareKey)
                .setLimitShareCount(limitCount)
                .setShareCount(0)
                .setExpire(Long.valueOf(limitSecond))
                .setFileId(fileId)
                .setMasterId(userSessionDto.getUserId());

        // redis文件分享缓存
        fileRedisComponent.setEx(fileRedisProperties.getFileShareKey() + shareKey, shareFileDto, limitSecond, TimeUnit.SECONDS);

        return shareKey;
    }

    /**
     * 获取分享文件
     *
     * @param shareKey 分享密钥
     *
     */
    public ShareFileToUserDto getShareFileUrl(String shareKey) {
        // redis校验shareKey
        ShareFileDto shareFileDto = fileRedisComponent.get(fileRedisProperties.getFileShareKey() + shareKey, ShareFileDto.class);
        if (shareFileDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // 查询存储键
        List<FileTreeDto> fileTreeDtoList = getUserFileList(shareFileDto.getMasterId());
        FileTreeDto fileTreeDto = FileTreeDto.findFileTreeDtoById(fileTreeDtoList, shareFileDto.getFileId());
        if (fileTreeDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // 更新分享次数
        shareFileDto.setShareCount(shareFileDto.getShareCount() + 1);
        if (shareFileDto.getShareCount() > shareFileDto.getLimitShareCount()) {
            fileRedisComponent.delete(fileRedisProperties.getFileShareKey() + shareKey);
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        fileRedisComponent.setEx(fileRedisProperties.getFileShareKey() + shareKey, shareFileDto, shareFileDto.getRemainSeconds(), TimeUnit.SECONDS);

        return new ShareFileToUserDto(shareFileDto) // 封装类
                .setFileUrl(ossComponent.getFileDownloadInfo(fileProperties.getUserFileKey() + shareFileDto.getMasterId() + "/" + fileTreeDto.getPath()).getUrl())
                .setFileName(fileTreeDto.getLabel());
    }


    /**
     * 检查并增加用户流量限制
     *
     * @param fileSize 文件大小
     * @param userId 用户ID
     */
    protected void checkAndIncrementUserTraffic(long fileSize, Integer userId) {
        if (!fileRedisComponent.exists(fileRedisProperties.getUserTrafficKey() + userId)) {
            fileRedisComponent.setEx(fileRedisProperties.getUserTrafficKey() + userId, 0, fileRedisProperties.getUserTrafficTimeout(), TimeUnit.SECONDS);
        }
        if (fileRedisComponent.increment(fileRedisProperties.getUserTrafficKey() + userId, fileSize) > fileRedisProperties.getUserTrafficMaxTraffic()) {
            throw new BusinessException(ResponseCodeEnum.CODE_611);
        }
    }








}

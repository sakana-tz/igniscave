package com.evernight.service;

import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.po.File;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.mapper.FileMapper;
import com.evernight.components.aliOssComponent.FileProperties;
import com.evernight.components.aliOssComponent.FileRedisComponent;
import com.evernight.components.aliOssComponent.FileRedisProperties;
import com.evernight.components.aliOssComponent.OssComponent;
import com.evernight.entity.dto.ChunkFileDto;
import com.evernight.entity.dto.ChunkStatusDto;
import com.evernight.entity.dto.FileDownloadInfo;
import com.evernight.entity.dto.FileTreeDto;
import com.evernight.entity.dto.MultipartUploadInitDto;
import com.evernight.entity.dto.UserSessionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.evernight.components.aliOssComponent.OssTools.getContentType;

@Slf4j
@Service
public class BigFileOssService extends FileOssService {
    private static final String CHUNK_INFO_KEY = "chunkInfo";
    private static final String CHUNK_STATUS_LIST_KEY = "chunkStatus";
    private static final String UPLOAD_INDEX_KEY = "uploadIndex";

    @Autowired
    private OssComponent ossComponent;
    @Autowired
    private FileRedisComponent fileRedisComponent;
    @Autowired
    private FileRedisProperties fileRedisProperties;
    @Autowired
    private FileProperties fileProperties;
    @Autowired
    private FileMapper<File, Object> fileMapper;

    /**
     * 上传状态查询
     *
     * @param uploadId       上传ID
     * @param userSessionDto 用户信息
     * @return ChunkStatusDto 上传状态
     */
    public ChunkStatusDto getChunkStatus(String uploadId, UserSessionDto userSessionDto) {
        // redis查询分片总表
        ChunkFileDto chunkFileDto = fileRedisComponent.get(fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY, ChunkFileDto.class);
        // 信息校验
        if (chunkFileDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_400);
        }

        // 查询上传进度
        List<Integer> chunkStatusList = fileRedisComponent.getAllBitMap(fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_STATUS_LIST_KEY);

        // 返回结果
        return new ChunkStatusDto(
                chunkFileDto.getUploadId(),
                chunkFileDto.getFilename(),
                chunkFileDto.getStChunkSize(),
                chunkFileDto.getTotalChunks(),
                chunkStatusList
        );
    }


    /**
     * 初始化分片上传（前端直传OSS模式）
     * <p>
     * 后端初始化OSS分片上传，并为每个分片生成预签名PUT URL，
     * 前端拿到URL后直接将各分片PUT到OSS，不经过后端。
     *
     * @param path           文件存储路径
     * @param fileName       文件名
     * @param fileSize       文件大小
     * @param userSessionDto 用户会话信息
     * @return MultipartUploadInitDto 包含uploadId、分片大小、分片总数、预签名URL列表
     */
    public MultipartUploadInitDto initMultipartUpload(String path, String fileName, long fileSize, UserSessionDto userSessionDto) {

        // 文件大小检查
        if (fileSize > fileProperties.getMaxFileSize()) {
            throw new BusinessException(ResponseCodeEnum.CODE_611);
        }

        // 用户流量检查
        checkAndIncrementUserTraffic(fileSize, userSessionDto.getUserId());

        // oss初始化上传
        String key = path + fileName;
        String ossKey = fileProperties.getUserFileKey() + userSessionDto.getUserId() + "/" + key;
        String uploadId = ossComponent.initMultipartUpload(ossKey, getContentType(fileName));

        // 计算分片数
        int totalChunks = (int) Math.ceil((double) fileSize / fileProperties.getChunkSize());

        // 为每个分片生成预签名PUT URL（1小时有效）
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        List<String> presignedUrls = new ArrayList<>(totalChunks);
        for (int i = 1; i <= totalChunks; i++) {
            presignedUrls.add(ossComponent.generatePresignedUrlForPart(uploadId, ossKey, i, expiration));
        }

        // 分片上传类封装，redis存储
        ChunkFileDto chunkFileDto = new ChunkFileDto()
                .setUploadId(uploadId)
                .setFilename(fileName)
                .setStChunkSize(fileProperties.getChunkSize())
                .setTotalChunks(totalChunks)
                .setDownloadKey(key);
        fileRedisComponent.setEx(
                fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY,
                chunkFileDto,
                fileRedisProperties.getFileChunkUploadTimeout(),
                TimeUnit.SECONDS
        );

        // 存储文件→uploadId索引，用于页面刷新后断点续传查询
        String indexKey = fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + UPLOAD_INDEX_KEY + ":" + ossKey;
        fileRedisComponent.setEx(indexKey, uploadId, fileRedisProperties.getFileChunkUploadTimeout(), TimeUnit.SECONDS);

        return new MultipartUploadInitDto()
                .setUploadId(uploadId)
                .setChunkSize(fileProperties.getChunkSize())
                .setTotalChunks(totalChunks)
                .setPresignedUrls(presignedUrls);
    }


    /**
     * 上报分片上传完成（前端直传OSS后调用）
     * <p>
     * 前端使用预签名URL将分片直接上传到OSS后，调用此方法上报已上传成功的分片编号，
     * 后端更新Redis分片状态bitmap。
     *
     * @param uploadId       上传ID
     * @param uploadedParts  已成功上传的分片编号列表
     * @param userSessionDto 用户会话信息
     */
    public void reportUploadedParts(String uploadId, List<Integer> uploadedParts, UserSessionDto userSessionDto) {
        // redis查询分片总表
        ChunkFileDto chunkFileDto = fileRedisComponent.get(fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY, ChunkFileDto.class);
        // 信息校验
        if (chunkFileDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_400);
        }

        // redis更新分片上传状态
        String bitmapKey = fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_STATUS_LIST_KEY;
        for (Integer partNumber : uploadedParts) {
            if (partNumber > 0 && partNumber <= chunkFileDto.getTotalChunks()) {
                fileRedisComponent.markBitMap(bitmapKey, partNumber);
            }
        }

        // 设置过期时间
        fileRedisComponent.expire(bitmapKey, fileRedisProperties.getFileChunkUploadTimeout(), TimeUnit.SECONDS);
        fileRedisComponent.expire(fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY, fileRedisProperties.getFileChunkUploadTimeout(), TimeUnit.SECONDS);
    }


    /**
     * 重新获取分片预签名URL（断点续传时URL过期后调用）
     * <p>
     * 不会重新初始化上传，仅为指定分片生成新的预签名PUT URL。
     * 适用于 uploadId 未过期但预签名URL已过期的场景。
     *
     * @param uploadId       上传ID
     * @param partNumbers    需要重新获取URL的分片编号列表（从1开始），为null则返回所有分片
     * @param userSessionDto 用户会话信息
     * @return 包含uploadId、chunkSize、totalChunks、presignedUrls的DTO
     */
    public MultipartUploadInitDto refreshPresignedUrls(String uploadId, List<Integer> partNumbers, UserSessionDto userSessionDto) {
        // redis查询分片总表
        ChunkFileDto chunkFileDto = fileRedisComponent.get(
                fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY,
                ChunkFileDto.class
        );
        if (chunkFileDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_400);
        }

        String ossKey = fileProperties.getUserFileKey() + userSessionDto.getUserId() + "/" + chunkFileDto.getDownloadKey();
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);

        // 确定需要生成URL的分片列表
        List<Integer> targetParts;
        if (partNumbers == null || partNumbers.isEmpty()) {
            targetParts = new ArrayList<>(chunkFileDto.getTotalChunks());
            for (int i = 1; i <= chunkFileDto.getTotalChunks(); i++) {
                targetParts.add(i);
            }
        } else {
            targetParts = partNumbers;
        }

        // 生成预签名URL
        List<String> presignedUrls = new ArrayList<>(targetParts.size());
        for (Integer partNumber : targetParts) {
            if (partNumber > 0 && partNumber <= chunkFileDto.getTotalChunks()) {
                presignedUrls.add(ossComponent.generatePresignedUrlForPart(uploadId, ossKey, partNumber, expiration));
            }
        }

        return new MultipartUploadInitDto()
                .setUploadId(uploadId)
                .setChunkSize(chunkFileDto.getStChunkSize())
                .setTotalChunks(chunkFileDto.getTotalChunks())
                .setPresignedUrls(presignedUrls);
    }


    /**
     * 完成分片上传（合并文件）
     * <p>
     * 前端直传OSS所有分片后，调用此方法通知后端合并。
     * 后端通过Redis bitmap验证分片完整性，然后通过OSS listParts获取ETag并合并。
     *
     * @param uploadId 上传ID
     */
    public void completeMultipartUpload(String uploadId, UserSessionDto userSessionDto) {
        // redis查询分片总表
        ChunkFileDto chunkFileDto = fileRedisComponent.get(fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY, ChunkFileDto.class);
        // 信息校验
        if (chunkFileDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_610);
        }

        // redis查询分片上传状态，验证所有分片已上传
        String bitmapKey = fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_STATUS_LIST_KEY;
        long uploadedCount = fileRedisComponent.countBitMap(bitmapKey);
        if (uploadedCount != chunkFileDto.getTotalChunks()) {
            throw new BusinessException(ResponseCodeEnum.CODE_610);
        }

        // oss合并文件（通过listParts获取ETag后合并，OSS级别最终校验）
        String ossKey = fileProperties.getUserFileKey() + userSessionDto.getUserId() + "/" + chunkFileDto.getDownloadKey();
        CompleteMultipartUploadResult result = ossComponent.completeMultipartUpload(uploadId, ossKey);
        if (result == null || result.getETag() == null) {
            log.error("OSS合并文件失败，uploadId={}, ossKey={}", uploadId, ossKey);
            throw new BusinessException(ResponseCodeEnum.CODE_610);
        }

        // redis删除分片信息
        String indexKey = fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + UPLOAD_INDEX_KEY + ":" + ossKey;
        fileRedisComponent.delete(fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY);
        fileRedisComponent.delete(bitmapKey);
        fileRedisComponent.delete(indexKey);

        // oss文件大小查询
        long fileSize = ossComponent.getFileSize(ossKey);

        // mapper更新
        fileMapper.insert(new File(
                null,
                userSessionDto.getUserId(),
                chunkFileDto.getFilename(),
                chunkFileDto.getDownloadKey(),
                getContentType(chunkFileDto.getFilename()),
                fileSize,
                null, 0,
                LocalDateTime.now(), null));

        // 清理redis旧文件列表缓存
        fileRedisComponent.delete(fileRedisProperties.getUserFileListKey() + userSessionDto.getUserId());
    }


    /**
     * 检查待恢复的上传（页面刷新后断点续传）
     * <p>
     * 前端通过文件路径和文件名查询是否存在未完成的上传任务，
     * 如果存在则返回uploadId和分片上传状态，前端可据此恢复上传。
     *
     * @param path           文件路径
     * @param fileName       文件名
     * @param userSessionDto 用户会话信息
     * @return ChunkStatusDto 如果存在待恢复上传则返回状态，否则返回null
     */
    public ChunkStatusDto checkPendingUpload(String path, String fileName, UserSessionDto userSessionDto) {
        String ossKey = fileProperties.getUserFileKey() + userSessionDto.getUserId() + "/" + path + fileName;
        String indexKey = fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + UPLOAD_INDEX_KEY + ":" + ossKey;

        // 通过索引查找uploadId
        String uploadId = fileRedisComponent.get(indexKey, String.class);
        if (uploadId == null) {
            return null;
        }

        // 验证ChunkFileDto是否仍然存在
        ChunkFileDto chunkFileDto = fileRedisComponent.get(
                fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_INFO_KEY,
                ChunkFileDto.class
        );
        if (chunkFileDto == null) {
            // 索引残留，清理
            fileRedisComponent.delete(indexKey);
            return null;
        }

        // 查询上传进度
        String bitmapKey = fileRedisProperties.getFileChunkUploadKey() + userSessionDto.getUserId() + ":" + uploadId + ":" + CHUNK_STATUS_LIST_KEY;
        List<Integer> chunkStatusList = fileRedisComponent.getAllBitMap(bitmapKey);

        return new ChunkStatusDto(
                chunkFileDto.getUploadId(),
                chunkFileDto.getFilename(),
                chunkFileDto.getStChunkSize(),
                chunkFileDto.getTotalChunks(),
                chunkStatusList
        );
    }


    /**
     * 大文件分片下载
     * <p>返回OSS预签名URL及分片信息，前端通过Range请求实现分片下载和断点续传</p>
     *
     * @param fileId         文件ID
     * @param userSessionDto 用户会话信息
     * @return 文件下载信息（含预签名URL、文件大小、分片大小）
     */
    public FileDownloadInfo downloadBigFile(Long fileId, UserSessionDto userSessionDto) {
        // 查询存储键
        List<FileTreeDto> fileTreeDtoList = getUserFileList(userSessionDto.getUserId());
        String key = FileTreeDto.findFileTreeDtoById(fileTreeDtoList, fileId).getPath();

        if (key == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // 获取OSS预签名下载URL
        String ossKey = fileProperties.getUserFileKey() + userSessionDto.getUserId() + "/" + key;
        FileDownloadInfo info = ossComponent.getFileDownloadInfo(ossKey);

        // 填充分片下载信息
        info.setChunkSize(fileProperties.getDownloadChunkSize());
        info.setChunkThreshold(fileProperties.getDownloadChunkThreshold());

        return info;
    }

}

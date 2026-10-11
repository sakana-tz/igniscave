package com.evernight.components.aliOssComponent;

import com.aliyun.oss.OSS;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.model.*;
import com.evernight.entity.dto.FileDownloadInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OssComponent {

    @Autowired
    private OSS ossClient;
    @Autowired
    private OssProperties ossProperties;


    /**
     * 上传单个文件到OSS。
     *
     * @param file 文件对象
     * @param key  存储键 (例如: "folder/subfolder/filename.ext")
     * @throws IOException 如果读取文件输入流时出错
     */
    public void uploadFile(MultipartFile file, String key) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    key,
                    file.getInputStream()
            );
            ossClient.putObject(putObjectRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取文件下载信息（预签名URL，前端直接下载）
     * <br>
     * 支持断点续传：前端可使用 Range 请求头实现分片下载
     *
     * @param key 文件在OSS上的存储路径
     * @return 文件下载信息（包含预签名URL、文件大小、Content-Type）
     */
    public FileDownloadInfo getFileDownloadInfo(String key) {
        try {
            // 获取文件元数据
            ObjectMetadata metadata = ossClient.getObjectMetadata(ossProperties.getBucketName(), key);
            long fileSize = metadata.getContentLength();
            String contentType = metadata.getContentType();

            // 生成预签名下载URL（1小时有效期）
            // 预签名URL天然支持 Range 请求，前端可直接实现断点续传
            Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucketName(), key);
            request.setExpiration(expiration);
            URL url = ossClient.generatePresignedUrl(request);

            return new FileDownloadInfo()
                    .setUrl(url.toString())
                    .setFileSize(fileSize)
                    .setContentType(contentType);
        } catch (Exception e) {
            log.error("Failed to get file download info for key: {}", key, e);
            throw new RuntimeException("Failed to get file download info", e);
        }
    }

    /**
     * 生成预签名上传URL（PUT方式，前端直接上传到OSS）
     *
     * @param key         文件存储键
     * @param contentType 文件类型
     * @param expiration  URL过期时间
     * @return 预签名上传URL
     */
    public String generatePresignedUploadUrl(String key, String contentType, Date expiration) {
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucketName(), key);
            request.setMethod(HttpMethod.PUT);
            request.setExpiration(expiration);
            if (contentType != null) {
                request.setContentType(contentType);
            }
            return ossClient.generatePresignedUrl(request).toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate presigned upload URL", e);
        }
    }

    /**
     * 检查OSS上是否存在指定key的文件。
     *
     * @param key 文件在OSS上的唯一标识符
     * @return true 如果文件存在，否则 false
     */
    public boolean doesObjectExist(String key) {
        return ossClient.doesObjectExist(ossProperties.getBucketName(), key);
    }

    /**
     * 上传文本内容到OSS（用于帖子MD内容等小文本存储）
     *
     * @param content 文本内容
     * @param key     存储键
     */
    public void uploadTextContent(String content, String key) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("text/markdown; charset=utf-8");
            metadata.setContentLength(content.getBytes(StandardCharsets.UTF_8).length);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(), key, inputStream, metadata);
            ossClient.putObject(putObjectRequest);
        } catch (IOException e) {
            log.error("Failed to upload text content to key: {}", key, e);
            throw new RuntimeException("Failed to upload text content", e);
        }
    }

    /**
     * 从OSS下载文本内容（用于获取帖子MD内容）
     *
     * @param key 存储键
     * @return 文本内容
     */
    public String downloadTextContent(String key) {
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), key);
            try (InputStream inputStream = ossObject.getObjectContent()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Failed to download text content from key: {}", key, e);
            return null;
        }
    }

    /**
     * 从OSS删除单个文件。
     *
     * @param key 文件在OSS上的唯一标识符
     */
    public void deleteFile(String key) {
        ossClient.deleteObject(ossProperties.getBucketName(), key);
    }

    /**
     * 查询OSS上指定文件的大小。
     *
     * @param key 文件在OSS上的唯一标识符
     * @return 文件大小（字节），如果文件不存在则返回 -1
     */
    public long getFileSize(String key) {
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(ossProperties.getBucketName(), key);
            return metadata.getContentLength();
        } catch (Exception e) {
            log.error("Failed to get file size for key: {}", key, e);
            return -1;
        }
    }


    /**
     * 初始化分片上传
     *
     * @param key         文件存储键
     * @param contentType 文件类型
     * @return 上传ID
     */
    public String initMultipartUpload(String key, String contentType) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                ossProperties.getBucketName(), key);

        if (contentType != null) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            request.setObjectMetadata(metadata);
        }

        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        return result.getUploadId();
    }

    // 已弃用：当前采用前端直传OSS模式（预签名URL），后端中转上传分片不再使用
    // /**
    //  * 上传分片到OSS（后端中转）
    //  *
    //  * @param uploadId   上传ID
    //  * @param key        文件存储键
    //  * @param partNumber 分片序号
    //  * @param file       文件
    //  * @return 分片的ETag
    //  */
    // public String uploadChunk(String uploadId, String key, int partNumber, MultipartFile file)
    //         throws IOException {
    //     try (InputStream inputStream = file.getInputStream()) {
    //         UploadPartRequest uploadPartRequest = new UploadPartRequest();
    //         uploadPartRequest.setBucketName(ossProperties.getBucketName());
    //         uploadPartRequest.setKey(key);
    //         uploadPartRequest.setUploadId(uploadId);
    //         uploadPartRequest.setPartNumber(partNumber);
    //         uploadPartRequest.setInputStream(inputStream);
    //         uploadPartRequest.setPartSize(file.getSize());
    //
    //         UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
    //         return uploadPartResult.getPartETag().getETag();
    //     }
    // }


    /**
     * 生成分片上传预签名URL（PUT方式，前端直传OSS分片）
     *
     * @param uploadId   分片上传ID
     * @param key        文件存储键
     * @param partNumber 分片序号（从1开始）
     * @param expiration URL过期时间
     * @return 预签名上传URL
     */
    public String generatePresignedUrlForPart(String uploadId, String key, int partNumber, Date expiration) {
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucketName(), key);
            request.setMethod(HttpMethod.PUT);
            request.setExpiration(expiration);
            request.addQueryParameter("uploadId", uploadId);
            request.addQueryParameter("partNumber", String.valueOf(partNumber));
            return ossClient.generatePresignedUrl(request).toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for part: key={}, partNumber={}", key, partNumber, e);
            throw new RuntimeException("Failed to generate presigned URL for part", e);
        }
    }

    /**
     * 完成分片上传（合并文件）
     * <br>
     * 通过 listParts 从 OSS 获取各分片 ETag，适用于前端直传 OSS 场景（无需前端上报 ETag）
     *
     * @param uploadId 分片上传ID
     * @param key      文件存储键
     */
    public CompleteMultipartUploadResult completeMultipartUpload(String uploadId, String key) {
        // 从OSS获取该uploadId下所有已上传分片的ETag
        ListPartsRequest listPartsRequest = new ListPartsRequest(ossProperties.getBucketName(), key, uploadId);
        PartListing partListing = ossClient.listParts(listPartsRequest);

        List<PartETag> partETagList = new ArrayList<>();
        for (PartSummary part : partListing.getParts()) {
            partETagList.add(new PartETag(part.getPartNumber(), part.getETag()));
        }

        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                ossProperties.getBucketName(), key, uploadId, partETagList);
        return ossClient.completeMultipartUpload(completeRequest);
    }

    /**
     * 完成分片上传（合并文件）
     *
     * @param uploadId  上传ID
     * @param key       文件存储键
     * @param partETags 分片信息
     */
    public CompleteMultipartUploadResult completeMultipartUpload(
            String uploadId, String key, Map<Integer, String> partETags) {

        List<PartETag> partETagList = new ArrayList<>();
        partETags.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    PartETag partETag = new PartETag(entry.getKey(), entry.getValue());
                    partETagList.add(partETag);
                });

        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                ossProperties.getBucketName(), key, uploadId, partETagList);
        return ossClient.completeMultipartUpload(completeRequest);
    }

    /**
     * 根据前缀删除OSS上的所有文件
     *
     * @param prefix 文件路径前缀
     */
    public void deleteFilesByPrefix(String prefix) {
        try {
            // 列出所有匹配前缀的对象
            ObjectListing objectListing = ossClient.listObjects(ossProperties.getBucketName(), prefix);
            List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();

            // 批量删除
            if (!objectSummaries.isEmpty()) {
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(ossProperties.getBucketName());
                List<String> keys = new ArrayList<>();
                for (OSSObjectSummary summary : objectSummaries) {
                    keys.add(summary.getKey());
                }
                deleteRequest.setKeys(keys);
                ossClient.deleteObjects(deleteRequest);
            }
        } catch (Exception e) {
            log.error("Failed to delete files by prefix: {}", prefix, e);
        }
    }


}

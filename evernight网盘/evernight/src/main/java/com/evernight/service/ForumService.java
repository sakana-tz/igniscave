package com.evernight.service;

import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.po.ForumComment;
import com.evernight.base.entity.po.ForumPost;
import com.evernight.base.entity.query.ForumCommentQuery;
import com.evernight.base.entity.query.ForumPostQuery;
import com.evernight.base.entity.query.SimplePage;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.mapper.ForumCommentMapper;
import com.evernight.base.mapper.ForumPostMapper;
import com.evernight.components.aliOssComponent.FileProperties;
import com.evernight.components.aliOssComponent.OssComponent;
import com.evernight.components.aliOssComponent.OssProperties;
import com.evernight.components.redisComponent.RedisComponent;
import com.evernight.components.redisComponent.RedisProperties;
import com.evernight.entity.dto.HotPostDto;
import com.evernight.entity.dto.PostDetailDto;
import com.evernight.entity.dto.UserSessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 帖子模块业务逻辑
 */
@Service
public class ForumService {

    @Autowired
    private ForumPostMapper<ForumPost, ForumPostQuery> forumPostMapper;
    @Autowired
    private ForumCommentMapper<ForumComment, ForumCommentQuery> forumCommentMapper;
    @Autowired
    private OssComponent ossComponent;
    @Autowired
    private FileProperties fileProperties;
    @Autowired
    private OssProperties ossProperties;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private RedisProperties redisProperties;


    /**
     * 获取热门帖子列表（浏览量前10，缓存1天）
     *
     * @return 热门帖子列表（含发帖用户名）
     */
    @SuppressWarnings("unchecked")// 告诉编译器忽略 unchecked 警告信息
    public List<HotPostDto> getHotPostList() {
        // 先从 Redis 缓存中获取
        Object cached = redisComponent.get(redisProperties.getFileForumPostKey());
        if (cached != null) {
            return (List<HotPostDto>) cached;
        }

        // 缓存未命中，查询数据库
        List<HotPostDto> list = forumPostMapper.selectHotPosts();

        // 写入 Redis 缓存，过期时间1天
        redisComponent.setEx(redisProperties.getFileForumPostKey(),
                list, redisProperties.getFileForumPostTimeout(), TimeUnit.SECONDS
        );

        return list;
    }

    /**
     * 获取帖子MD内容的OSS存储键
     *
     * @param postId 帖子ID
     * @return OSS存储键
     */
    private String getPostContentKey(Integer postId) {
        return fileProperties.getForumPostKey() + postId + ".md";
    }

    /**
     * 创建帖子（默认草稿状态，同时初始化MD内容到OSS）
     *
     * @param title          帖子标题
     * @param content        帖子MD内容（可为空，初始化为空字符串）
     * @param userSessionDto 用户会话信息
     * @return 新建帖子的ID
     */
    public Integer createPost(String title, String content, UserSessionDto userSessionDto) {
        // 创建帖子记录（默认草稿状态）
        ForumPost post = new ForumPost()
                .setUserId(userSessionDto.getUserId())
                .setTitle(title)
                .setViewCount(0L)
                .setPostStatus(0)
                .setUpdatedTime(LocalDateTime.now());
        forumPostMapper.insert(post);

        // 将MD内容上传到OSS（使用主键ID作为存储键的一部分）
        String ossKey = getPostContentKey(post.getPostId());
        ossComponent.uploadTextContent(content != null ? content : "", ossKey);

        return post.getPostId();
    }

    /**
     * 发布帖子（将草稿状态改为已发布）
     *
     * @param postId         帖子ID
     * @param userSessionDto 用户会话信息
     */
    public void publishPost(Integer postId, UserSessionDto userSessionDto) {
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        if (!post.getUserId().equals(userSessionDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        if (post.getPostStatus() == 1) {
            // 已经是发布状态，无需重复发布
            return;
        }
        forumPostMapper.updateByPostId(
                new ForumPost().setPostStatus(1).setUpdatedTime(LocalDateTime.now()),
                postId
        );
    }

    /**
     * 更新帖子状态（0:草稿,1:发布）
     *
     * @param postId         帖子ID
     * @param postStatus     目标状态
     * @param userSessionDto 用户会话信息
     */
    public void updatePostStatus(Integer postId, Integer postStatus, UserSessionDto userSessionDto) {
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        if (!post.getUserId().equals(userSessionDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        if (post.getPostStatus().equals(postStatus)) {
            return;
        }
        forumPostMapper.updateByPostId(
                new ForumPost().setPostStatus(postStatus).setUpdatedTime(LocalDateTime.now()),
                postId
        );
        // 状态变化，清除热门帖子缓存
        redisComponent.delete(redisProperties.getFileForumPostKey());
    }

    /**
     * 校验帖子归属权（草稿帖子仅本人可访问）
     *
     * @param postId         帖子ID
     * @param userSessionDto 用户会话信息
     * @return 帖子对象
     */
    private ForumPost checkPostOwnership(Integer postId, UserSessionDto userSessionDto) {
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        // 草稿帖子仅本人可访问
        if (post.getPostStatus() == 0 && !post.getUserId().equals(userSessionDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return post;
    }

    /**
     * 获取帖子列表（分页，仅已发布，含评论数）
     * <br>keyword 为空时查全部，有值时按标题模糊搜索
     *
     * @param keyword  搜索关键词（可为null）
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @return 包含帖子列表和分页信息的结果
     */
    public Map<String, Object> getPostList(String keyword, Integer pageNo, Integer pageSize) {
        ForumPostQuery query = new ForumPostQuery();
        query.setPostStatus(1);
        if (keyword != null && !keyword.isEmpty()) {
            query.setTitleFuzzy(keyword);
        }
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        query.setOrderBy("f.updated_time desc");

        Integer total = forumPostMapper.selectCount(query);
        query.setSimplePage(new SimplePage(pageNo, total, pageSize));

        List<ForumPost> list = forumPostMapper.selectListWithUsername(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }


    /**
     * 获取当前用户的帖子列表（分页，含所有状态，含评论数）
     *
     * @param keyword    搜索关键词（可为null）
     * @param pageNo     页码
     * @param pageSize   每页数量
     * @param userSessionDto 用户会话信息
     * @return 包含帖子列表和分页信息的结果
     */
    public Map<String, Object> getMyPostList(String keyword, Integer pageNo, Integer pageSize, UserSessionDto userSessionDto) {
        ForumPostQuery query = new ForumPostQuery();
        query.setUserId(userSessionDto.getUserId());
        if (keyword != null && !keyword.isEmpty()) {
            query.setTitleFuzzy(keyword);
        }
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        query.setOrderBy("f.updated_time desc");

        Integer total = forumPostMapper.selectCount(query);
        query.setSimplePage(new SimplePage(pageNo, total, pageSize));

        List<ForumPost> list = forumPostMapper.selectListWithUsername(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 获取帖子详情（含MD内容和评论数，同时增加浏览量）
     *
     * @param postId 帖子ID
     * @return 帖子详情（包含MD内容）
     */
    public PostDetailDto getPostDetail(Integer postId) {
        // 获取帖子详情（含评论数）
        ForumPost post = forumPostMapper.selectDetailByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        // 从OSS获取MD内容
        String ossKey = getPostContentKey(postId);
        String content = ossComponent.downloadTextContent(ossKey);

        // 构造详情DTO
        PostDetailDto dto = new PostDetailDto(post);
        dto.setContent(content != null ? content : "");

        return dto;
    }

    /**
     * 更新帖子（标题和MD内容）
     *
     * @param postId         帖子ID
     * @param title          新标题
     * @param content        新MD内容
     * @param userSessionDto 用户会话信息
     */
    public void updatePost(Integer postId, String title, String content, UserSessionDto userSessionDto) {
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        if (!post.getUserId().equals(userSessionDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }

        // 更新帖子基本信息
        forumPostMapper.updateByPostId(
                new ForumPost().setTitle(title).setUpdatedTime(LocalDateTime.now()),
                postId
        );

        // 更新OSS中的MD内容
        if (content != null) {
            String ossKey = getPostContentKey(postId);
            ossComponent.uploadTextContent(content, ossKey);
        }
    }

    /**
     * 删除帖子（同时删除OSS中的MD内容、封面和图片）
     *
     * @param postId         帖子ID
     * @param userSessionDto 用户会话信息
     */
    public void deletePost(Integer postId, UserSessionDto userSessionDto) {
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        if (!post.getUserId().equals(userSessionDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }

        // 删除帖子（评论通过外键CASCADE自动删除）
        forumPostMapper.deleteByPostId(postId);

        // 删除OSS中的MD内容
        String ossKey = getPostContentKey(postId);
        if (ossComponent.doesObjectExist(ossKey)) {
            ossComponent.deleteFile(ossKey);
        }

        // 删除封面
        deletePostCover(postId);

        // 删除帖子图片
        deletePostImages(postId);
    }

    /**
     * 添加评论
     *
     * @param postId         帖子ID
     * @param content        评论内容
     * @param userSessionDto 用户会话信息
     */
    public void addComment(Integer postId, String content, UserSessionDto userSessionDto) {
        // 校验帖子是否存在
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }

        ForumComment comment = new ForumComment()
                .setPostId(postId)
                .setUserId(userSessionDto.getUserId())
                .setContent(content)
                .setCreatedTime(LocalDateTime.now());
        forumCommentMapper.insert(comment);
    }

    /**
     * 获取帖子评论列表（按时间升序）
     *
     * @param postId 帖子ID
     * @return 评论列表
     */
    public List<ForumComment> getCommentList(Integer postId) {
        ForumCommentQuery query = new ForumCommentQuery();
        query.setPostId(postId);
        query.setOrderBy("f.created_time asc");
        return forumCommentMapper.selectListWithUsername(query);
    }

    /**
     * 删除评论
     *
     * @param commentId      评论ID
     * @param userSessionDto 用户会话信息
     */
    public void deleteComment(Integer commentId, UserSessionDto userSessionDto) {
        ForumComment comment = forumCommentMapper.selectByCommentId(commentId);
        if (comment == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        if (!comment.getUserId().equals(userSessionDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        forumCommentMapper.deleteByCommentId(commentId);
    }

    /**
     * 增加帖子浏览量
     *
     * @param postId 帖子ID
     */
    public void incrementViewCount(Integer postId) {
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        forumPostMapper.updateByPostId(
                new ForumPost().setViewCount(post.getViewCount() + 1),
                postId
        );
        // 浏览量变化，清除热门帖子缓存
        redisComponent.delete(redisProperties.getFileForumPostKey());
    }

    // =================== 帖子封面相关方法 ===================

    /**
     * 获取帖子封面的OSS存储键
     *
     * @param postId 帖子ID
     * @return OSS存储键
     */
    private String getPostCoverKey(Integer postId) {
        return fileProperties.getForumPostCoverKey() + postId + ".png";
    }

    /**
     * 获取帖子封面上传预签名URL（前端直传OSS）
     *
     * @param postId 帖子ID
     * @param userSessionDto 用户会话信息
     * @return 预签名上传URL
     */
    public String getCoverUploadUrl(Integer postId, UserSessionDto userSessionDto) {
        // 校验帖子归属
        checkPostOwnership(postId, userSessionDto);
        String ossKey = getPostCoverKey(postId);
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        return ossComponent.generatePresignedUploadUrl(ossKey, "image/png", expiration);
    }

    /**
     * 获取帖子封面URL（预签名URL，1小时有效）
     *
     * @param postId 帖子ID
     * @return 封面图片URL
     */
    public String getCoverUrl(Integer postId) {
        String ossKey = getPostCoverKey(postId);
        if (!ossComponent.doesObjectExist(ossKey)) {
            return null;
        }
        return ossComponent.getFileDownloadInfo(ossKey).getUrl();
    }

    /**
     * 删除帖子封面
     *
     * @param postId 帖子ID
     */
    private void deletePostCover(Integer postId) {
        String ossKey = getPostCoverKey(postId);
        if (ossComponent.doesObjectExist(ossKey)) {
            ossComponent.deleteFile(ossKey);
        }
    }

    // =================== 帖子图片相关方法 ===================

    /**
     * 上传帖子图片，返回上传URL和永久可访问的代理URL
     *
     * @param postId 帖子ID
     * @param fileName 文件名
     * @param contentType 文件MIME类型
     * @param userSessionDto 用户会话信息
     * @return 包含uploadUrl和proxyUrl的Map
     */
    public Map<String, String> uploadPostImage(Integer postId, String fileName, String contentType, UserSessionDto userSessionDto) {
        // 校验帖子归属
        checkPostOwnership(postId, userSessionDto);

        // 生成唯一文件名
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String imageKey = fileProperties.getForumPostImageKey() + postId + "/" + UUID.randomUUID() + extension;

        // 生成预签名上传URL（1小时有效）
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        String uploadUrl = ossComponent.generatePresignedUploadUrl(imageKey, contentType, expiration);

        // 生成代理访问URL（永久有效）
        String proxyUrl = ossProperties.getDomain() + "/forum/getForumImage/" +
                URLEncoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(imageKey.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        Map<String, String> result = new HashMap<>();
        result.put("uploadUrl", uploadUrl);
        result.put("proxyUrl", proxyUrl);
        return result;
    }

    /**
     * 获取帖子图片的预签名URL（用于代理重定向）
     *
     * @param encryptedKey Base64编码的加密OSS键
     * @return 预签名URL
     */
    public String getForumImagePresignedUrl(String encryptedKey) {
        try {
            // 解密OSS键
            String ossKey = new String(Base64.getUrlDecoder().decode(encryptedKey), StandardCharsets.UTF_8);

            // 生成预签名URL（1小时有效）
            return ossComponent.getFileDownloadInfo(ossKey).getUrl();
        } catch (Exception e) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
    }

    /**
     * 删除帖子所有图片
     *
     * @param postId 帖子ID
     */
    private void deletePostImages(Integer postId) {
        String prefix = fileProperties.getForumPostImageKey() + postId + "/";
        ossComponent.deleteFilesByPrefix(prefix);
    }

    // =================== 管理员专用方法 ===================

    /**
     * 【管理员】分页查询帖子列表（含用户名、评论数，不限状态）
     *
     * @param query 查询参数
     * @return 分页结果
     */
    public Map<String, Object> adminGetPostList(ForumPostQuery query) {
        query.setOrderBy("f.updated_time desc");
        Integer total = forumPostMapper.selectCount(query);
        query.setSimplePage(new SimplePage(query.getPageNo(), total, query.getPageSize()));
        List<ForumPost> list = forumPostMapper.selectListWithUsername(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNo", query.getPageNo());
        result.put("pageSize", query.getPageSize());
        return result;
    }

    /**
     * 【管理员】获取帖子详情（含MD内容，不限用户归属）
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    public PostDetailDto adminGetPostDetail(Integer postId) {
        ForumPost post = forumPostMapper.selectDetailByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        String ossKey = getPostContentKey(postId);
        String content = ossComponent.downloadTextContent(ossKey);
        PostDetailDto dto = new PostDetailDto(post);
        dto.setContent(content != null ? content : "");
        return dto;
    }

    /**
     * 【管理员】删除帖子（不限用户归属，同时删除OSS中的MD内容、封面和图片）
     *
     * @param postId 帖子ID
     */
    public void adminDeletePost(Integer postId) {
        ForumPost post = forumPostMapper.selectByPostId(postId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        forumPostMapper.deleteByPostId(postId);
        String ossKey = getPostContentKey(postId);
        if (ossComponent.doesObjectExist(ossKey)) {
            ossComponent.deleteFile(ossKey);
        }

        // 删除封面
        deletePostCover(postId);

        // 删除帖子图片
        deletePostImages(postId);

        // 清除热门帖子缓存
        redisComponent.delete(redisProperties.getFileForumPostKey());
    }

    /**
     * 【管理员】分页查询评论列表（含用户名）
     *
     * @param query 查询参数
     * @return 分页结果
     */
    public Map<String, Object> adminGetCommentList(ForumCommentQuery query) {
        query.setOrderBy("f.created_time desc");
        Integer total = forumCommentMapper.selectCount(query);
        query.setSimplePage(new SimplePage(query.getPageNo(), total, query.getPageSize()));
        List<ForumComment> list = forumCommentMapper.selectListWithUsername(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNo", query.getPageNo());
        result.put("pageSize", query.getPageSize());
        return result;
    }

    /**
     * 【管理员】删除评论（不限用户归属）
     *
     * @param commentId 评论ID
     */
    public void adminDeleteComment(Integer commentId) {
        ForumComment comment = forumCommentMapper.selectByCommentId(commentId);
        if (comment == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_602);
        }
        forumCommentMapper.deleteByCommentId(commentId);
    }

}

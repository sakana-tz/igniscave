package com.evernight.controller;

import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import com.evernight.service.ForumService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/forum")
public class ForumController extends ABaseController {

    @Autowired
    private ForumService forumService;

    /**
     * 获取热门帖子列表（浏览量前10，缓存1天）
     *
     * @return 热门帖子列表
     * <br>ForumPost字段: postId, userId, title, viewCount, commentCount, postStatus, updatedTime
     */
    @RequestMapping("/getHotPostList")
    public ResponseVO getHotPostList() {
        return getSuccessResponseVO(forumService.getHotPostList());
    }
    
    /**
     * 获取帖子列表（分页，含评论数）
     * <br>keyword 为空时查全部已发布帖子，有值时按标题模糊搜索
     *
     * @param keyword  搜索关键词（可选，为空则查全部）
     * @param pageNo   页码，默认1
     * @param pageSize 每页数量，默认20
     * @return 帖子列表及分页信息
     * <br>返回字段: list(帖子列表), total(总数), pageNo(当前页), pageSize(每页数量)
     * <br>ForumPost字段: postId, userId, username, title, viewCount, commentCount, postStatus, updatedTime
     */
    @RequestMapping("/getPostList")
    public ResponseVO getPostList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return getSuccessResponseVO(forumService.getPostList(keyword, pageNo, pageSize));
    }

    /**
     * 获取当前用户的帖子列表（分页，含所有状态，含评论数）
     * <br>keyword 为空时不模糊搜索，有值时按标题模糊搜索
     *
     * @param keyword    搜索关键词（可选，为空则不搜索）
     * @param pageNo     页码，默认1
     * @param pageSize   每页数量，默认20
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     * @return 帖子列表及分页信息
     * <br>返回字段: list(帖子列表), total(总数), pageNo(当前页), pageSize(每页数量)
     * <br>ForumPost字段: postId, userId, username, title, viewCount, commentCount, postStatus, updatedTime
     */
    @RequestMapping("/getMyPostList")
    public ResponseVO getMyPostList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(forumService.getMyPostList(keyword, pageNo, pageSize, userSessionDto));
    }

    /**
     * 获取帖子详情（含MD内容和评论数，前端可直接渲染和编辑MD）
     *
     * @param postId 帖子ID
     * @return PostDetailDto: postId, userId, username, title, viewCount, commentCount, postStatus, updatedTime, content(MD内容)
     */
    @RequestMapping("/getPostDetail")
    public ResponseVO getPostDetail(
            @RequestParam("postId") Integer postId
    ) {
        return getSuccessResponseVO(forumService.getPostDetail(postId));
    }

    /**
     * 创建帖子（默认草稿状态，含初始MD内容）
     *
     * @param title          帖子标题
     * @param content        帖子MD内容（可为空，前端后续可通过updatePost保存）
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     * @return 新建帖子ID（草稿状态，需调用publishPost发布）
     */
    @RequestMapping("/createPost")
    public ResponseVO createPost(
            @RequestParam("title") String title,
            @RequestParam(value = "content", defaultValue = "") String content,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(forumService.createPost(title, content, userSessionDto));
    }

    /**
     * 发布帖子（将草稿状态改为已发布）
     *
     * @param postId         帖子ID
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     */
    @RequestMapping("/publishPost")
    public ResponseVO publishPost(
            @RequestParam("postId") Integer postId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        forumService.publishPost(postId, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 更新帖子状态（草稿/发布切换）
     *
     * @param postId         帖子ID
     * @param postStatus     目标状态（0:草稿, 1:发布）
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     */
    @RequestMapping("/updatePostStatus")
    public ResponseVO updatePostStatus(
            @RequestParam("postId") Integer postId,
            @RequestParam("postStatus") Integer postStatus,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        forumService.updatePostStatus(postId, postStatus, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 更新帖子（标题和MD内容）
     *
     * @param postId         帖子ID
     * @param title          新标题
     * @param content        新MD内容（前端编辑后提交）
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     */
    @RequestMapping("/updatePost")
    public ResponseVO updatePost(
            @RequestParam("postId") Integer postId,
            @RequestParam("title") String title,
            @RequestParam(value = "content", required = false) String content,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        forumService.updatePost(postId, title, content, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除帖子（同时删除OSS中的MD内容）
     *
     * @param postId         帖子ID
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     */
    @RequestMapping("/deletePost")
    public ResponseVO deletePost(
            @RequestParam("postId") Integer postId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        forumService.deletePost(postId, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取帖子评论列表（按时间升序排列）
     *
     * @param postId 帖子ID
     * @return 评论列表
     * <br>ForumComment字段: commentId, postId, userId, username, content, createdTime
     */
    @RequestMapping("/getCommentList")
    public ResponseVO getCommentList(
            @RequestParam("postId") Integer postId
    ) {
        return getSuccessResponseVO(forumService.getCommentList(postId));
    }

    /**
     * 添加评论
     *
     * @param postId         帖子ID
     * @param content        评论内容（限500字）
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     */
    @RequestMapping("/addComment")
    public ResponseVO addComment(
            @RequestParam("postId") Integer postId,
            @RequestParam("content") String content,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        forumService.addComment(postId, content, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除评论
     *
     * @param commentId      评论ID
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     */
    @RequestMapping("/deleteComment")
    public ResponseVO deleteComment(
            @RequestParam("commentId") Integer commentId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        forumService.deleteComment(commentId, userSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 增加帖子浏览量
     *
     * @param postId 帖子ID
     */
    @RequestMapping("/incrementViewCount")
    public ResponseVO incrementViewCount(
            @RequestParam("postId") Integer postId
    ) {
        forumService.incrementViewCount(postId);
        return getSuccessResponseVO(null);
    }

    // =================== 帖子封面相关接口 ===================

    /**
     * 获取帖子封面上传预签名URL（前端直传OSS）
     *
     * @param postId 帖子ID
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     * @return 预签名上传URL
     */
    @RequestMapping("/getCoverUploadUrl")
    public ResponseVO getCoverUploadUrl(
            @RequestParam("postId") Integer postId,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(forumService.getCoverUploadUrl(postId, userSessionDto));
    }

    /**
     * 获取帖子封面URL（预签名URL，1小时有效）
     *
     * @param postId 帖子ID
     * @return 封面图片URL
     */
    @RequestMapping("/getCoverUrl")
    public ResponseVO getCoverUrl(
            @RequestParam("postId") Integer postId
    ) {
        return getSuccessResponseVO(forumService.getCoverUrl(postId));
    }

    // =================== 帖子图片相关接口 ===================

    /**
     * 上传帖子图片，返回上传URL和永久可访问的代理URL
     *
     * @param postId 帖子ID
     * @param fileName 文件名
     * @param contentType 文件MIME类型
     * @param userSessionDto 用户信息，自动从请求头中获取，无需传参
     * @return {uploadUrl: 预签名上传URL, proxyUrl: 永久代理URL}
     */
    @RequestMapping("/uploadPostImage")
    public ResponseVO uploadPostImage(
            @RequestParam("postId") Integer postId,
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        return getSuccessResponseVO(forumService.uploadPostImage(postId, fileName, contentType, userSessionDto));
    }

    /**
     * 帖子图片代理访问（302重定向到预签名URL）
     *
     * @param encryptedKey Base64编码的加密OSS键
     */
    @RequestMapping("/getForumImage/{encryptedKey}")
    public void getForumImage(
            @PathVariable("encryptedKey") String encryptedKey,
            HttpServletResponse response
    ) {
        try {
            String presignedUrl = forumService.getForumImagePresignedUrl(encryptedKey);
            response.sendRedirect(presignedUrl);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}

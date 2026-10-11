package com.evernight.controller.admin;

import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.query.ForumCommentQuery;
import com.evernight.base.entity.query.ForumPostQuery;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.base.exception.BusinessException;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import com.evernight.service.ForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 论坛管理 Controller（管理员）
 */
@RestController
@RequestMapping("/admin/forum")
public class AdminForumController extends ABaseController {

    @Autowired
    private ForumService forumService;

    /**
     * 分页查询帖子列表（含用户名、评论数，不限状态）
     */
    @RequestMapping("/post/loadDataList")
    public ResponseVO loadPostList(
            ForumPostQuery query,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return getSuccessResponseVO(forumService.adminGetPostList(query));
    }

    /**
     * 根据PostId查询帖子详情（含MD内容）
     */
    @RequestMapping("/post/getPostByPostId")
    public ResponseVO getPostByPostId(Integer postId, @UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return getSuccessResponseVO(forumService.adminGetPostDetail(postId));
    }

    /**
     * 根据PostId删除帖子（同时删除OSS中的MD内容）
     */
    @RequestMapping("/post/deletePostByPostId")
    public ResponseVO deletePostByPostId(Integer postId, @UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        forumService.adminDeletePost(postId);
        return getSuccessResponseVO(null);
    }

    /**
     * 分页查询评论列表（含评论用户名）
     */
    @RequestMapping("/comment/loadDataList")
    public ResponseVO loadCommentList(
            ForumCommentQuery query,
            @UserInfoValidation UserSessionDto userSessionDto
    ) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        return getSuccessResponseVO(forumService.adminGetCommentList(query));
    }

    /**
     * 根据CommentId删除评论
     */
    @RequestMapping("/comment/deleteCommentByCommentId")
    public ResponseVO deleteCommentByCommentId(Integer commentId, @UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }
        forumService.adminDeleteComment(commentId);
        return getSuccessResponseVO(null);
    }

}

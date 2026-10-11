package com.evernight.controller.admin;

import com.evernight.base.controller.ABaseController;
import com.evernight.base.entity.enums.ResponseCodeEnum;
import com.evernight.base.entity.query.ForumCommentQuery;
import com.evernight.base.entity.query.ForumPostQuery;
import com.evernight.base.entity.query.UserQuery;
import com.evernight.base.entity.vo.ResponseVO;
import com.evernight.base.exception.BusinessException;
import com.evernight.base.service.FileService;
import com.evernight.base.service.ForumCommentService;
import com.evernight.base.service.ForumPostService;
import com.evernight.base.service.UserService;
import com.evernight.entity.annotation.UserInfoValidation;
import com.evernight.entity.dto.UserSessionDto;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


/**
 * 系统仪表盘 Controller（管理员）
 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController extends ABaseController {

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    @Resource
    private ForumPostService forumPostService;

    @Resource
    private ForumCommentService forumCommentService;

    /**
     * 获取系统概览统计数据
     * <br>返回：用户总数、存储总大小(字节)、帖子总数、评论总数
     */
    @RequestMapping("/getOverview")
    public ResponseVO getOverview(@UserInfoValidation UserSessionDto userSessionDto) {
        if (userSessionDto.getUserStatus() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_603);
        }

        Map<String, Object> overview = new HashMap<>();
        overview.put("userCount", userService.findCountByParam(new UserQuery()));
        overview.put("totalStorageSize", fileService.getStorageTotalSize(userSessionDto));
        overview.put("postCount", forumPostService.findCountByParam(new ForumPostQuery()));
        overview.put("commentCount", forumCommentService.findCountByParam(new ForumCommentQuery()));

        return getSuccessResponseVO(overview);
    }

}

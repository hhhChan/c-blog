package com.can.cblog.xo.vo;

import com.can.cblog.base.validator.annotation.NotBlank;
import com.can.cblog.base.validator.group.GetList;
import com.can.cblog.base.validator.group.GetOne;
import com.can.cblog.base.validator.group.Insert;
import com.can.cblog.base.vo.BaseVO;
import lombok.Data;
import lombok.ToString;

/**
 * @author ccc
 */
@ToString
@Data
public class CommentVO extends BaseVO<CommentVO> {

    /**
     * 用户uid
     */
    @NotBlank(groups = {Insert.class, GetOne.class})
    private String userUid;

    /**
     * 回复某条评论的uid
     */
    private String toUid;

    /**
     * 回复某个人的uid
     */
    private String toUserUid;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 评论类型： 0: 评论   1: 点赞
     */
    private Integer type;

    /**
     * 评论内容
     */
    @NotBlank(groups = {Insert.class})
    private String content;

    /**
     * 博客uid
     */
    private String blogUid;

    /**
     * 评论来源： MESSAGE_BOARD，ABOUT，BLOG_INFO 等
     */
    @NotBlank(groups = {Insert.class, GetList.class})
    private String source;
}

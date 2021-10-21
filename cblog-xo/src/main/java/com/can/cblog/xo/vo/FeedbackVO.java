package com.can.cblog.xo.vo;

import com.can.cblog.base.validator.annotation.NotBlank;
import com.can.cblog.base.validator.group.Insert;
import com.can.cblog.base.validator.group.Update;
import com.can.cblog.base.vo.BaseVO;
import lombok.Data;
import lombok.ToString;

/**
 * 反馈表
 * @author ccc
 */
@ToString
@Data
public class FeedbackVO extends BaseVO<FeedbackVO> {

    /**
     * 用户uid
     */
    private String userUid;

    /**
     * 标题
     */
    @NotBlank(groups = {Insert.class, Update.class})
    private String title;

    /**
     * 反馈的内容
     */
    @NotBlank(groups = {Insert.class, Update.class})
    private String content;

    /**
     * 回复
     */
    private String reply;

    /**
     * 反馈状态： 0：已开启  1：进行中  2：已完成  3：已拒绝
     */
    private Integer feedbackStatus;

}


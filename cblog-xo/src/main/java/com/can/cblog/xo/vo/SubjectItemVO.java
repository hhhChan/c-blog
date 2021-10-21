package com.can.cblog.xo.vo;

import com.can.cblog.base.validator.annotation.IntegerNotNull;
import com.can.cblog.base.validator.annotation.NotBlank;
import com.can.cblog.base.validator.group.Insert;
import com.can.cblog.base.validator.group.Update;
import com.can.cblog.base.vo.BaseVO;
import lombok.Data;

/**
 * @author ccc
 */
@Data
public class SubjectItemVO extends BaseVO<SubjectItemVO> {

    /**
     * 专题UID
     */
    @NotBlank(groups = {Insert.class, Update.class})
    private String subjectUid;

    /**
     * 博客UID
     */
    @NotBlank(groups = {Insert.class, Update.class})
    private String blogUid;

    /**
     * 排序字段，数值越大，越靠前
     */
    @IntegerNotNull(groups = {Insert.class, Update.class})
    private int sort;

}

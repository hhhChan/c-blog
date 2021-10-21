package com.can.cblog.xo.vo;

import com.can.cblog.base.validator.annotation.NotBlank;
import com.can.cblog.base.validator.group.Insert;
import com.can.cblog.base.validator.group.Update;
import com.can.cblog.base.vo.BaseVO;
import lombok.Data;

/**
 * @author ccc
 */
@Data
public class TagVO extends BaseVO<TagVO> {

    /**
     * 标签内容
     */
    @NotBlank(groups = {Insert.class, Update.class})
    private String content;

    /**
     * 排序字段
     */
    private Integer sort;

    /**
     * OrderBy排序字段（desc: 降序）
     */
    private String orderByDescColumn;

    /**
     * OrderBy排序字段（asc: 升序）
     */
    private String orderByAscColumn;

    /**
     * 无参构造方法，初始化默认值
     */
    TagVO() {

    }
}

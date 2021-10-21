package com.can.cblog.base.vo;

import com.can.cblog.base.validator.Messages;
import com.can.cblog.base.validator.annotation.LongNotNull;
import com.can.cblog.base.validator.group.GetList;
import lombok.Data;

/**
 * PageInfo 用于分页
 * @author ccc
 */
@Data
public class PageInfo<T> {
    /**
     * 关键字
     */
    private String keyword;

    /**
     * 当前页
     */
    @LongNotNull(groups = {GetList.class}, message = Messages.PAGE_NOT_NULL)
    private Long currentPage;

    /**
     * 页大小
     */
    @LongNotNull(groups = {GetList.class}, message = Messages.SIZE_NOT_NULL)
    private Long pageSize;
}

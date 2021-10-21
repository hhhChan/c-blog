package com.can.cblog.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.can.cblog.base.entity.SuperEntity;
import lombok.Data;

import java.util.List;

/**
 * @author ccc
 */
@Data
@TableName("t_subject")
public class Subject extends SuperEntity<Subject> {

    private static final long serialVersionUID = 1L;

    /**
     * 专题名
     */
    private String subjectName;

    /**
     * 分类简介
     */
    private String summary;

    /**
     * 封面图片UID
     */
    private String fileUid;

    /**
     * 专题点击数
     */
    private String clickCount;

    /**
     * 专题收藏数
     */
    private String collectCount;

    /**
     * 排序字段，数值越大，越靠前
     */
    private int sort;

    /**
     * 分类图
     */
    @TableField(exist = false)
    private List<String> photoList;
}


package com.can.cblog.xo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.can.cblog.base.service.SuperService;
import com.can.cblog.common.entity.Picture;
import com.can.cblog.xo.vo.PictureVO;

import java.util.List;

/**
 * @author ccc
 */
public interface PictureService extends SuperService<Picture> {


    /**
     * 获取图片列表
     *
     * @param pictureVO
     * @return
     */
    public IPage<Picture> getPageList(PictureVO pictureVO);

    /**
     * 新增图片
     *
     * @param pictureVOList
     * @return
     */
    public String addPicture(List<PictureVO> pictureVOList);

    /**
     * 编辑图片
     *
     * @param pictureVO
     * @return
     */
    public String editPicture(PictureVO pictureVO);

    /**
     * 批量删除图片
     *
     * @param pictureVO
     */
    public String deleteBatchPicture(PictureVO pictureVO);

    /**
     * 设置图片封面
     *
     * @param pictureVO
     */
    public String setPictureCover(PictureVO pictureVO);

    /**
     * 获取最新图片,按时间排序
     *
     * @return
     */
    public Picture getTopOne();
}

package com.can.cblog.xo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.can.cblog.base.service.SuperService;
import com.can.cblog.common.entity.StudyVideo;
import com.can.cblog.xo.vo.StudyVideoVO;

import java.util.List;

/**
 * 学习视频表 服务类
 * @author ccc
 */
public interface StudyVideoService extends SuperService<StudyVideo> {
    /**
     * 获取学习视频列表
     *
     * @param studyVideoVO
     * @return
     */
    public IPage<StudyVideo> getPageList(StudyVideoVO studyVideoVO);

    /**
     * 新增学习视频
     *
     * @param studyVideoVO
     */
    public String addStudyVideo(StudyVideoVO studyVideoVO);

    /**
     * 编辑学习视频
     *
     * @param studyVideoVO
     */
    public String editStudyVideo(StudyVideoVO studyVideoVO);

    /**
     * 批量删除学习视频
     *
     * @param studyVideoVOList
     */
    public String deleteBatchStudyVideo(List<StudyVideoVO> studyVideoVOList);
}

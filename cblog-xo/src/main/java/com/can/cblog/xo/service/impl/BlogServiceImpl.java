package com.can.cblog.xo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.can.cblog.base.enums.*;
import com.can.cblog.base.global.BaseSQLConf;
import com.can.cblog.base.global.BaseSysConf;
import com.can.cblog.base.global.Constants;
import com.can.cblog.base.holder.RequestHolder;
import com.can.cblog.base.service.impl.SuperServiceImpl;
import com.can.cblog.common.entity.*;
import com.can.cblog.common.feign.PictureFeignClient;
import com.can.cblog.utils.*;
import com.can.cblog.xo.global.MessageConf;
import com.can.cblog.xo.global.RedisConf;
import com.can.cblog.xo.global.SQLConf;
import com.can.cblog.xo.global.SysConf;
import com.can.cblog.xo.mapper.BlogMapper;
import com.can.cblog.xo.mapper.BlogSortMapper;
import com.can.cblog.xo.mapper.TagMapper;
import com.can.cblog.xo.producer.message.BlogMessage;
import com.can.cblog.xo.service.*;
import com.can.cblog.xo.utils.WebUtil;
import com.can.cblog.xo.vo.BlogVO;
import com.google.gson.internal.LinkedTreeMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author ccc
 */
@Service
@Slf4j
public class BlogServiceImpl extends SuperServiceImpl<BlogMapper, Blog> implements BlogService {

    @Autowired
    private WebUtil webUtil;

    @Autowired
    private CommentService commentService;

    @Autowired
    private WebVisitService webVisitService;

    @Autowired
    private TagService tagService;

    @Autowired
    private PictureService pictureService;

    @Autowired
    private BlogSortService blogSortService;

    @Autowired
    private RedisUtil redisUtil;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private BlogSortMapper blogSortMapper;

    @Resource
    private BlogMapper blogMapper;

    @Autowired
    private AdminService adminService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private SysParamsService sysParamsService;

    @Autowired
    private BlogService blogService;

    @Autowired
    private SubjectItemService subjectItemService;

    @Resource
    private PictureFeignClient pictureFeignClient;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public List<Blog> setTagByBlogList(List<Blog> list) {
        for (Blog item : list) {
            if (item != null) {
                setTagByBlog(item);
            }
        }
        return list;
    }

    @Override
    public List<Blog> setTagAndSortByBlogList(List<Blog> list) {
        List<String> sortUids = new ArrayList<>();
        List<String> tagUids = new ArrayList<>();
        list.forEach(item -> {
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                sortUids.add(item.getBlogSortUid());
            }
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                List<String> tagUidsTemp = StringUtils.changeStringToString(item.getTagUid(), BaseSysConf.FILE_SEGMENTATION);
                for (String itemTagUid : tagUidsTemp) {
                    tagUids.add(itemTagUid);
                }
            }
        });
        Collection<BlogSort> sortList = new ArrayList<>();
        Collection<Tag> tagList = new ArrayList<>();
        if (sortUids.size() > 0) {
            sortList = blogSortMapper.selectBatchIds(sortUids);
        }
        if (tagUids.size() > 0) {
            tagList = tagMapper.selectBatchIds(tagUids);
        }
        Map<String, BlogSort> sortMap = new HashMap<>();
        Map<String, Tag> tagMap = new HashMap<>();
        sortList.forEach(item -> {
            sortMap.put(item.getUid(), item);
        });
        tagList.forEach(item -> {
            tagMap.put(item.getUid(), item);
        });
        for (Blog item : list) {

            //????????????
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                item.setBlogSort(sortMap.get(item.getBlogSortUid()));
            }
            //????????????
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                List<String> tagUidsTemp = StringUtils.changeStringToString(item.getTagUid(), BaseSysConf.FILE_SEGMENTATION);
                List<Tag> tagListTemp = new ArrayList<Tag>();
                tagUidsTemp.forEach(tag -> {
                    tagListTemp.add(tagMap.get(tag));
                });
                item.setTagList(tagListTemp);
            }
        }

        return list;
    }

    @Override
    public List<Blog> setTagAndSortAndPictureByBlogList(List<Blog> list) {

        List<String> sortUids = new ArrayList<>();
        List<String> tagUids = new ArrayList<>();
        Set<String> fileUidSet = new HashSet<>();

        list.forEach(item -> {
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                fileUidSet.add(item.getFileUid());
            }
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                sortUids.add(item.getBlogSortUid());
            }
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                // tagUid???????????????????????????
                List<String> tagUidsTemp = StringUtils.changeStringToString(item.getTagUid(), BaseSysConf.FILE_SEGMENTATION);
                for (String itemTagUid : tagUidsTemp) {
                    tagUids.add(itemTagUid);
                }
            }
        });

        String pictureList = null;
        StringBuffer fileUids = new StringBuffer();
        List<Map<String, Object>> picList = new ArrayList<>();
        // feign????????????????????????
        if(fileUidSet.size() > 0) {
            int count = 1;
            for(String fileUid: fileUidSet) {
                fileUids.append(fileUid + ",");
                if(count % 10 == 0) {
                    pictureList = this.pictureFeignClient.getPicture(fileUids.toString(), Constants.SYMBOL_COMMA);
                    List<Map<String, Object>> tempPicList = webUtil.getPictureMap(pictureList);
                    picList.addAll(tempPicList);
                    fileUids = new StringBuffer();
                }
                count ++;
            }
            // ????????????????????????????????????
            if(fileUids.length() >= Constants.NUM_32) {
                pictureList = this.pictureFeignClient.getPicture(fileUids.toString(), Constants.SYMBOL_COMMA);
                List<Map<String, Object>> tempPicList = webUtil.getPictureMap(pictureList);
                picList.addAll(tempPicList);
            }
        }

        Collection<BlogSort> sortList = new ArrayList<>();
        Collection<Tag> tagList = new ArrayList<>();
        if (sortUids.size() > 0) {
            sortList = blogSortService.listByIds(sortUids);
        }
        if (tagUids.size() > 0) {
            tagList = tagService.listByIds(tagUids);
        }
        Map<String, BlogSort> sortMap = new HashMap<>();
        Map<String, Tag> tagMap = new HashMap<>();
        Map<String, String> pictureMap = new HashMap<>();

        sortList.forEach(item -> {
            sortMap.put(item.getUid(), item);
        });

        tagList.forEach(item -> {
            tagMap.put(item.getUid(), item);
        });

        picList.forEach(item -> {
            pictureMap.put(item.get(SysConf.UID).toString(), item.get(SysConf.URL).toString());
        });

        for (Blog item : list) {
            //????????????
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                item.setBlogSort(sortMap.get(item.getBlogSortUid()));
                if (sortMap.get(item.getBlogSortUid()) != null) {
                    item.setBlogSortName(sortMap.get(item.getBlogSortUid()).getSortName());
                }
            }

            //????????????
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                List<String> tagUidsTemp = StringUtils.changeStringToString(item.getTagUid(), ",");
                List<Tag> tagListTemp = new ArrayList<Tag>();

                tagUidsTemp.forEach(tag -> {
                    tagListTemp.add(tagMap.get(tag));
                });
                item.setTagList(tagListTemp);
            }

            //????????????
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                List<String> pictureUidsTemp = StringUtils.changeStringToString(item.getFileUid(), Constants.SYMBOL_COMMA);
                List<String> pictureListTemp = new ArrayList<String>();

                pictureUidsTemp.forEach(picture -> {
                    pictureListTemp.add(pictureMap.get(picture));
                });
                item.setPhotoList(pictureListTemp);
                // ????????????????????????
                if (pictureListTemp.size() > 0) {
                    item.setPhotoUrl(pictureListTemp.get(0));
                } else {
                    item.setPhotoUrl("");
                }
            }
        }
        return list;
    }

    @Override
    public Blog setTagByBlog(Blog blog) {
        String tagUid = blog.getTagUid();
        if (!StringUtils.isEmpty(tagUid)) {
            String[] uids = tagUid.split(SysConf.FILE_SEGMENTATION);
            List<Tag> tagList = new ArrayList<>();
            for (String uid : uids) {
                Tag tag = tagMapper.selectById(uid);
                if (tag != null && tag.getStatus() != EStatus.DISABLED) {
                    tagList.add(tag);
                }
            }
            blog.setTagList(tagList);
        }
        return blog;
    }

    @Override
    public Blog setSortByBlog(Blog blog) {

        if (blog != null && !StringUtils.isEmpty(blog.getBlogSortUid())) {
            BlogSort blogSort = blogSortMapper.selectById(blog.getBlogSortUid());
            blog.setBlogSort(blogSort);
        }
        return blog;
    }

    @Override
    public List<Blog> getBlogListByLevel(Integer level) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(BaseSQLConf.LEVEL, level);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);

        List<Blog> list = blogMapper.selectList(queryWrapper);
        return list;
    }

    @Override
    public IPage<Blog> getBlogPageByLevel(Page<Blog> page, Integer level, Integer useSort) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(BaseSQLConf.LEVEL, level);
        queryWrapper.eq(BaseSQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);

        if (useSort == 0) {
            queryWrapper.orderByDesc(BaseSQLConf.CREATE_TIME);
        } else {
            queryWrapper.orderByDesc(BaseSQLConf.SORT);
        }

        //????????????????????????????????????????????????????????????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SysConf.CONTENT));

        return blogMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Integer getBlogCount(Integer status) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(BaseSQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        return blogMapper.selectCount(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> getBlogCountByTag() {
        // ???Redis???????????????????????????????????????
        String jsonArrayList = redisUtil.get(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_COUNT_BY_TAG);
        if (StringUtils.isNotEmpty(jsonArrayList)) {
            ArrayList jsonList = JsonUtils.jsonArrayToArrayList(jsonArrayList);
            return jsonList;
        }

        List<Map<String, Object>> blogCoutByTagMap = blogMapper.getBlogCountByTag();
        Map<String, Integer> tagMap = new HashMap<>();
        for (Map<String, Object> item : blogCoutByTagMap) {
            String tagUid = String.valueOf(item.get(SQLConf.TAG_UID));
            // java.lang.Number???Integer,Long?????????
            Number num = (Number) item.get(SysConf.COUNT);
            Integer count = num.intValue();
            //??????????????????UID?????????
            if (tagUid.length() == 32) {
                //??????????????????????????????????????????
                if (tagMap.get(tagUid) == null) {
                    tagMap.put(tagUid, count);
                } else {
                    Integer tempCount = tagMap.get(tagUid) + count;
                    tagMap.put(tagUid, tempCount);
                }
            } else {
                //??????????????????32?????????????????????UID
                if (StringUtils.isNotEmpty(tagUid)) {
                    List<String> strList = StringUtils.changeStringToString(tagUid, ",");
                    for (String strItem : strList) {
                        if (tagMap.get(strItem) == null) {
                            tagMap.put(strItem, count);
                        } else {
                            Integer tempCount = tagMap.get(strItem) + count;
                            tagMap.put(strItem, tempCount);
                        }
                    }
                }
            }
        }

        //???????????????Tag??????Map???
        Set<String> tagUids = tagMap.keySet();
        Collection<Tag> tagCollection = new ArrayList<>();
        if (tagUids.size() > 0) {
            tagCollection = tagMapper.selectBatchIds(tagUids);
        }

        Map<String, String> tagEntityMap = new HashMap<>();
        for (Tag tag : tagCollection) {
            if (StringUtils.isNotEmpty(tag.getContent())) {
                tagEntityMap.put(tag.getUid(), tag.getContent());
            }
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tagMap.entrySet()) {
            String tagUid = entry.getKey();
            if (tagEntityMap.get(tagUid) != null) {
                String tagName = tagEntityMap.get(tagUid);
                Integer count = entry.getValue();
                Map<String, Object> itemResultMap = new HashMap<>();
                itemResultMap.put(SysConf.TAG_UID, tagUid);
                itemResultMap.put(SysConf.NAME, tagName);
                itemResultMap.put(SysConf.VALUE, count);
                resultList.add(itemResultMap);
            }
        }
        // ??? ??????????????????????????? ?????????Redis???????????????2?????????
        if (resultList.size() > 0) {
            redisUtil.setEx(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_COUNT_BY_TAG, JsonUtils.objectToJson(resultList), 2, TimeUnit.HOURS);
        }
        return resultList;
    }

    @Override
    public List<Map<String, Object>> getBlogCountByBlogSort() {
        // ???Redis?????????????????????????????????????????????
        String jsonArrayList = redisUtil.get(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_COUNT_BY_SORT);
        if (StringUtils.isNotEmpty(jsonArrayList)) {
            ArrayList jsonList = JsonUtils.jsonArrayToArrayList(jsonArrayList);
            return jsonList;
        }
        List<Map<String, Object>> blogCoutByBlogSortMap = blogMapper.getBlogCountByBlogSort();
        Map<String, Integer> blogSortMap = new HashMap<>();
        for (Map<String, Object> item : blogCoutByBlogSortMap) {

            String blogSortUid = String.valueOf(item.get(SQLConf.BLOG_SORT_UID));
            // java.lang.Number???Integer,Long?????????
            Number num = (Number) item.get(SysConf.COUNT);
            Integer count = 0;
            if (num != null) {
                count = num.intValue();
            }
            blogSortMap.put(blogSortUid, count);
        }

        //???????????????BlogSort??????Map???
        Set<String> blogSortUids = blogSortMap.keySet();
        Collection<BlogSort> blogSortCollection = new ArrayList<>();

        if (blogSortUids.size() > 0) {
            blogSortCollection = blogSortMapper.selectBatchIds(blogSortUids);
        }

        Map<String, String> blogSortEntityMap = new HashMap<>();
        for (BlogSort blogSort : blogSortCollection) {
            if (StringUtils.isNotEmpty(blogSort.getSortName())) {
                blogSortEntityMap.put(blogSort.getUid(), blogSort.getSortName());
            }
        }

        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : blogSortMap.entrySet()) {

            String blogSortUid = entry.getKey();

            if (blogSortEntityMap.get(blogSortUid) != null) {
                String blogSortName = blogSortEntityMap.get(blogSortUid);
                Integer count = entry.getValue();
                Map<String, Object> itemResultMap = new HashMap<>();
                itemResultMap.put(SysConf.BLOG_SORT_UID, blogSortUid);
                itemResultMap.put(SysConf.NAME, blogSortName);
                itemResultMap.put(SysConf.VALUE, count);
                resultList.add(itemResultMap);
            }
        }
        // ??? ??????????????????????????? ?????????Redis???????????????2?????????
        if (resultList.size() > 0) {
            redisUtil.setEx(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_COUNT_BY_SORT, JsonUtils.objectToJson(resultList), 2, TimeUnit.HOURS);
        }
        return resultList;
    }

    @Override
    public Map<String, Object> getBlogContributeCount() {
        // ???Redis?????????????????????????????????????????????
        String jsonMap = redisUtil.get(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_CONTRIBUTE_COUNT);
        if (StringUtils.isNotEmpty(jsonMap)) {
            Map<String, Object> resultMap = JsonUtils.jsonToMap(jsonMap);
            return resultMap;
        }

        // ????????????????????????
        String endTime = DateUtils.getNowTime();
        // ??????365???????????????
        Date temp = DateUtils.getDate(endTime, -365);
        String startTime = DateUtils.dateTimeToStr(temp);
        List<Map<String, Object>> blogContributeMap = blogMapper.getBlogContributeCount(startTime, endTime);
        List<String> dateList = DateUtils.getDayBetweenDates(startTime, endTime);
        Map<String, Object> dateMap = new HashMap<>();
        for (Map<String, Object> itemMap : blogContributeMap) {
            dateMap.put(itemMap.get("DATE").toString(), itemMap.get("COUNT"));
        }

        List<List<Object>> resultList = new ArrayList<>();
        for (String item : dateList) {
            Integer count = 0;
            if (dateMap.get(item) != null) {
                count = Integer.valueOf(dateMap.get(item).toString());
            }
            List<Object> objectList = new ArrayList<>();
            objectList.add(item);
            objectList.add(count);
            resultList.add(objectList);
        }

        Map<String, Object> resultMap = new HashMap<>(Constants.NUM_TWO);
        List<String> contributeDateList = new ArrayList<>();
        contributeDateList.add(startTime);
        contributeDateList.add(endTime);
        resultMap.put(SysConf.CONTRIBUTE_DATE, contributeDateList);
        resultMap.put(SysConf.BLOG_CONTRIBUTE_COUNT, resultList);
        // ??? ????????????????????? ?????????Redis???????????????2?????????
        redisUtil.setEx(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_CONTRIBUTE_COUNT, JsonUtils.objectToJson(resultMap), 2, TimeUnit.HOURS);
        return resultMap;
    }

    @Override
    public Blog getBlogByUid(String uid) {
        Blog blog = blogMapper.selectById(uid);
        if (blog != null && blog.getStatus() != EStatus.DISABLED) {
            blog = setTagByBlog(blog);
            blog = setSortByBlog(blog);
            return blog;
        }
        return null;
    }

    @Override
    public List<Blog> getSameBlogByBlogUid(String blogUid) {
        Blog blog = blogService.getById(blogUid);
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        Page<Blog> page = new Page<>();
        page.setCurrent(1);
        page.setSize(10);
        // ?????????????????????????????????
        String blogSortUid = blog.getBlogSortUid();
        queryWrapper.eq(SQLConf.BLOG_SORT_UID, blogSortUid);
        queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = blogService.setTagAndSortByBlogList(list);

        //????????????????????????
        List<Blog> newList = new ArrayList<>();
        for (Blog item : list) {
            if (item.getUid().equals(blogUid)) {
                continue;
            }
            newList.add(item);
        }
        return newList;
    }

    @Override
    public List<Blog> getBlogListByTop(Integer top) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        Page<Blog> page = new Page<>();
        page.setCurrent(1);
        page.setSize(top);
        queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.orderByDesc(SQLConf.SORT);
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = blogService.setTagAndSortAndPictureByBlogList(list);
        return list;
    }

    @Override
    public IPage<Blog> getPageList(BlogVO blogVO) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        // ??????????????????
        if (StringUtils.isNotEmpty(blogVO.getKeyword()) && !StringUtils.isEmpty(blogVO.getKeyword().trim())) {
            queryWrapper.like(SQLConf.TITLE, blogVO.getKeyword().trim());
        }
        if (!StringUtils.isEmpty(blogVO.getTagUid())) {
            queryWrapper.like(SQLConf.TAG_UID, blogVO.getTagUid());
        }
        if (!StringUtils.isEmpty(blogVO.getBlogSortUid())) {
            queryWrapper.like(SQLConf.BLOG_SORT_UID, blogVO.getBlogSortUid());
        }
        if (!StringUtils.isEmpty(blogVO.getLevelKeyword())) {
            queryWrapper.eq(SQLConf.LEVEL, blogVO.getLevelKeyword());
        }
        if (!StringUtils.isEmpty(blogVO.getIsPublish())) {
            queryWrapper.eq(SQLConf.IS_PUBLISH, blogVO.getIsPublish());
        }
        if (!StringUtils.isEmpty(blogVO.getIsOriginal())) {
            queryWrapper.eq(SQLConf.IS_ORIGINAL, blogVO.getIsOriginal());
        }
        if(!StringUtils.isEmpty(blogVO.getType())) {
            queryWrapper.eq(SQLConf.TYPE, blogVO.getType());
        }

        //??????
        Page<Blog> page = new Page<>();
        page.setCurrent(blogVO.getCurrentPage());
        page.setSize(blogVO.getPageSize());
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);

        if(StringUtils.isNotEmpty(blogVO.getOrderByAscColumn())) {
            // ???????????????????????????
            String column = StringUtils.underLine(new StringBuffer(blogVO.getOrderByAscColumn())).toString();
            queryWrapper.orderByAsc(column);
        }else if(StringUtils.isNotEmpty(blogVO.getOrderByDescColumn())) {
            // ???????????????????????????
            String column = StringUtils.underLine(new StringBuffer(blogVO.getOrderByDescColumn())).toString();
            queryWrapper.orderByDesc(column);
        } else {
            // ????????????????????????
            if (blogVO.getUseSort() == 0) {
                // ?????????????????????????????????
                queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
            } else {
                // ??????????????????sort???????????????
                queryWrapper.orderByDesc(SQLConf.SORT);
            }
        }

        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();

        if (list.size() == 0) {
            return pageList;
        }

        final StringBuffer fileUids = new StringBuffer();
        List<String> sortUids = new ArrayList<>();
        List<String> tagUids = new ArrayList<>();

        list.forEach(item -> {
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                fileUids.append(item.getFileUid() + SysConf.FILE_SEGMENTATION);
            }
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                sortUids.add(item.getBlogSortUid());
            }
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                List<String> tagUidsTemp = StringUtils.changeStringToString(item.getTagUid(), SysConf.FILE_SEGMENTATION);
                for (String itemTagUid : tagUidsTemp) {
                    tagUids.add(itemTagUid);
                }
            }
        });
        String pictureList = null;
        if (fileUids != null) {
            pictureList = this.pictureFeignClient.getPicture(fileUids.toString(), SysConf.FILE_SEGMENTATION);
        }

        List<Map<String, Object>> picList = webUtil.getPictureMap(pictureList);
        Collection<BlogSort> sortList = new ArrayList<>();
        Collection<Tag> tagList = new ArrayList<>();

        if (sortUids.size() > 0) {
            sortList = blogSortService.listByIds(sortUids);
        }
        if (tagUids.size() > 0) {
            tagList = tagService.listByIds(tagUids);
        }


        Map<String, BlogSort> sortMap = new HashMap<>();
        Map<String, Tag> tagMap = new HashMap<>();
        Map<String, String> pictureMap = new HashMap<>();

        sortList.forEach(item -> {
            sortMap.put(item.getUid(), item);
        });

        tagList.forEach(item -> {
            tagMap.put(item.getUid(), item);
        });

        picList.forEach(item -> {
            pictureMap.put(item.get(SQLConf.UID).toString(), item.get(SQLConf.URL).toString());
        });


        for (Blog item : list) {

            //????????????
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                item.setBlogSort(sortMap.get(item.getBlogSortUid()));
            }

            //????????????
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                List<String> tagUidsTemp = StringUtils.changeStringToString(item.getTagUid(), SysConf.FILE_SEGMENTATION);
                List<Tag> tagListTemp = new ArrayList<Tag>();

                tagUidsTemp.forEach(tag -> {
                    tagListTemp.add(tagMap.get(tag));
                });
                item.setTagList(tagListTemp);
            }

            //????????????
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                List<String> pictureUidsTemp = StringUtils.changeStringToString(item.getFileUid(), SysConf.FILE_SEGMENTATION);
                List<String> pictureListTemp = new ArrayList<>();

                pictureUidsTemp.forEach(picture -> {
                    pictureListTemp.add(pictureMap.get(picture));
                });
                item.setPhotoList(pictureListTemp);
            }
        }
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public String addBlog(BlogVO blogVO) {
        HttpServletRequest request = RequestHolder.getRequest();
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.LEVEL, blogVO.getLevel());
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        Integer count = blogService.count(queryWrapper);
        // ?????????????????????????????????????????????????????????
        String addVerdictResult = addVerdict(count + 1, blogVO.getLevel());
        // ??????????????????????????????
        if (StringUtils.isNotBlank(addVerdictResult)) {
            return addVerdictResult;
        }
        Blog blog = new Blog();
        //??????????????????????????????????????????
        String projectName = sysParamsService.getSysParamsValueByKey(SysConf.PROJECT_NAME_);
        if (EOriginal.ORIGINAL.equals(blogVO.getIsOriginal())) {
            Admin admin = adminService.getById(request.getAttribute(SysConf.ADMIN_UID).toString());
            if (admin != null) {
                if(StringUtils.isNotEmpty(admin.getNickName())) {
                    blog.setAuthor(admin.getNickName());
                } else {
                    blog.setAuthor(admin.getUserName());
                }
                blog.setAdminUid(admin.getUid());
            }
            blog.setArticlesPart(projectName);
        } else {
            blog.setAuthor(blogVO.getAuthor());
            blog.setArticlesPart(blogVO.getArticlesPart());
        }
        blog.setTitle(blogVO.getTitle());
        blog.setSummary(blogVO.getSummary());
        blog.setContent(blogVO.getContent());
        blog.setTagUid(blogVO.getTagUid());
        blog.setBlogSortUid(blogVO.getBlogSortUid());
        blog.setFileUid(blogVO.getFileUid());
        blog.setLevel(blogVO.getLevel());
        blog.setIsOriginal(blogVO.getIsOriginal());
        blog.setIsPublish(blogVO.getIsPublish());
        blog.setType(blogVO.getType());
        blog.setOutsideLink(blogVO.getOutsideLink());
        blog.setStatus(EStatus.ENABLE);
        blog.setOpenComment(blogVO.getOpenComment());
        Boolean isSave = blogService.save(blog);

        //???????????????????????????????????????solr ??? redis
        updateSolrAndRedis(isSave, blog);
        return ResultUtil.successWithMessage(MessageConf.INSERT_SUCCESS);
    }

    @Override
    public String editBlog(BlogVO blogVO) {
        HttpServletRequest request = RequestHolder.getRequest();
        Blog blog = blogService.getById(blogVO.getUid());
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.LEVEL, blogVO.getLevel());
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        Integer count = blogService.count(queryWrapper);
        if (blog != null) {
            //????????????????????????????????????????????????????????????????????????????????????????????????count??????1
            if (!blog.getLevel().equals(blogVO.getLevel())) {
                count += 1;
            }
        }
        String addVerdictResult = addVerdict(count, blogVO.getLevel());
        //???????????????????????????
        if (StringUtils.isNotBlank(addVerdictResult)) {
            return addVerdictResult;
        }
        //??????????????????????????????????????????
        Admin admin = adminService.getById(request.getAttribute(SysConf.ADMIN_UID).toString());
        blog.setAdminUid(admin.getUid());
        if (EOriginal.ORIGINAL.equals(blogVO.getIsOriginal())) {
            if(StringUtils.isNotEmpty(admin.getNickName())) {
                blog.setAuthor(admin.getNickName());
            } else {
                blog.setAuthor(admin.getUserName());
            }
            String projectName = sysParamsService.getSysParamsValueByKey(SysConf.PROJECT_NAME_);
            blog.setArticlesPart(projectName);
        } else {
            blog.setAuthor(blogVO.getAuthor());
            blog.setArticlesPart(blogVO.getArticlesPart());
        }

        blog.setTitle(blogVO.getTitle());
        blog.setSummary(blogVO.getSummary());
        blog.setContent(blogVO.getContent());
        blog.setTagUid(blogVO.getTagUid());
        blog.setBlogSortUid(blogVO.getBlogSortUid());
        blog.setFileUid(blogVO.getFileUid());
        blog.setLevel(blogVO.getLevel());
        blog.setIsOriginal(blogVO.getIsOriginal());
        blog.setIsPublish(blogVO.getIsPublish());
        blog.setOpenComment(blogVO.getOpenComment());
        blog.setUpdateTime(new Date());
        blog.setType(blogVO.getType());
        blog.setOutsideLink(blogVO.getOutsideLink());
        blog.setStatus(EStatus.ENABLE);

        Boolean isSave = blog.updateById();
        //???????????????????????????????????????solr ??? redis
        updateSolrAndRedis(isSave, blog);
        return ResultUtil.successWithMessage(MessageConf.UPDATE_SUCCESS);
    }

    @Override
    public String editBatch(List<BlogVO> blogVOList) {
        if (blogVOList.size() <= 0) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }
        List<String> blogUidList = new ArrayList<>();
        Map<String, BlogVO> blogVOMap = new HashMap<>();
        blogVOList.forEach(item -> {
            blogUidList.add(item.getUid());
            blogVOMap.put(item.getUid(), item);
        });

        Collection<Blog> blogList = blogService.listByIds(blogUidList);
        blogList.forEach(blog -> {
            BlogVO blogVO = blogVOMap.get(blog.getUid());
            if (blogVO != null) {
                blog.setAuthor(blogVO.getAuthor());
                blog.setArticlesPart(blogVO.getArticlesPart());
                blog.setTitle(blogVO.getTitle());
                blog.setSummary(blogVO.getSummary());
                blog.setContent(blogVO.getContent());
                blog.setTagUid(blogVO.getTagUid());
                blog.setBlogSortUid(blogVO.getBlogSortUid());
                blog.setFileUid(blogVO.getFileUid());
                blog.setLevel(blogVO.getLevel());
                blog.setIsOriginal(blogVO.getIsOriginal());
                blog.setIsPublish(blogVO.getIsPublish());
                blog.setSort(blogVO.getSort());
                blog.setType(blogVO.getType());
                blog.setOutsideLink(blogVO.getOutsideLink());
                blog.setStatus(EStatus.ENABLE);
            }
        });
        Boolean save = blogService.updateBatchById(blogList);

        //???????????????????????????????????????solr ??? redis
        if (save) {
            Map<String, Object> map = new HashMap<>();
            map.put(SysConf.COMMAND, SysConf.EDIT_BATCH);
            //?????????Rocketmq
            rocketMQTemplate.syncSend(BlogMessage.TOPIC, map);
        }

        return ResultUtil.successWithMessage(MessageConf.UPDATE_SUCCESS);
    }

    @Override
    public String deleteBlog(BlogVO blogVO) {
        Blog blog = blogService.getById(blogVO.getUid());
        blog.setStatus(EStatus.DISABLED);
        Boolean save = blog.updateById();

        //???????????????????????????????????????solr ??? redis, ?????????????????????Item??????????????????
        if (save) {
            Map<String, Object> map = new HashMap<>();

            map.put(SysConf.COMMAND, SysConf.DELETE);
            map.put(SysConf.BLOG_UID, blog.getUid());
            map.put(SysConf.LEVEL, blog.getLevel());
            map.put(SysConf.CREATE_TIME, blog.getCreateTime());
            //?????????Rocketmq
            rocketMQTemplate.syncSend(BlogMessage.TOPIC, map);

            // ????????????????????????????????????Item
            List<String> blogUidList = new ArrayList<>(Constants.NUM_ONE);
            blogUidList.add(blogVO.getUid());
            subjectItemService.deleteBatchSubjectItemByBlogUid(blogUidList);

            // ??????????????????????????????
            commentService.batchDeleteCommentByBlogUid(blogUidList);
        }
        return ResultUtil.successWithMessage(MessageConf.DELETE_SUCCESS);
    }

    @Override
    public String deleteBatchBlog(List<BlogVO> blogVoList) {
        if (blogVoList.size() <= 0) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }
        List<String> uidList = new ArrayList<>();
        StringBuffer uidSbf = new StringBuffer();
        blogVoList.forEach(item -> {
            uidList.add(item.getUid());
            uidSbf.append(item.getUid() + SysConf.FILE_SEGMENTATION);
        });
        Collection<Blog> blogList = blogService.listByIds(uidList);

        blogList.forEach(item -> {
            item.setStatus(EStatus.DISABLED);
        });

        Boolean save = blogService.updateBatchById(blogList);
        //???????????????????????????????????????solr ??? redis
        if (save) {
            Map<String, Object> map = new HashMap<>();
            map.put(SysConf.COMMAND, SysConf.DELETE_BATCH);
            map.put(SysConf.UID, uidSbf);
            //?????????Rocketmq
            rocketMQTemplate.syncSend(BlogMessage.TOPIC, map);
            // ????????????????????????????????????Item
            subjectItemService.deleteBatchSubjectItemByBlogUid(uidList);
            // ??????????????????????????????
            commentService.batchDeleteCommentByBlogUid(uidList);
        }
        return ResultUtil.successWithMessage(MessageConf.DELETE_SUCCESS);
    }

    @Override
    public String uploadLocalBlog(List<MultipartFile> filedatas) {
        SystemConfig systemConfig = systemConfigService.getConfig();
        if (systemConfig == null) {
            return ResultUtil.errorWithMessage(MessageConf.SYSTEM_CONFIG_NOT_EXIST);
        } else {
            if (EOpenStatus.OPEN.equals(systemConfig.getUploadQiNiu()) && (StringUtils.isEmpty(systemConfig.getQiNiuPictureBaseUrl()) || StringUtils.isEmpty(systemConfig.getQiNiuAccessKey())
                    || StringUtils.isEmpty(systemConfig.getQiNiuSecretKey()) || StringUtils.isEmpty(systemConfig.getQiNiuBucket()) || StringUtils.isEmpty(systemConfig.getQiNiuArea()))) {
                return ResultUtil.errorWithMessage(MessageConf.PLEASE_SET_QI_NIU);
            }

            if (EOpenStatus.OPEN.equals(systemConfig.getUploadLocal()) && StringUtils.isEmpty(systemConfig.getLocalPictureBaseUrl())) {
                return ResultUtil.errorWithMessage(MessageConf.PLEASE_SET_LOCAL);
            }
        }

        List<MultipartFile> fileList = new ArrayList<>();
        List<String> fileNameList = new ArrayList<>();
        for (MultipartFile file : filedatas) {
            String fileOriginalName = file.getOriginalFilename();
            if (FileUtils.isMarkdown(fileOriginalName)) {
                fileList.add(file);
                // ???????????????
                fileNameList.add(FileUtils.getFileName(fileOriginalName));
            } else {
                return ResultUtil.errorWithMessage("???????????????Markdown??????");
            }
        }

        if (fileList.size() == 0) {
            return ResultUtil.errorWithMessage("??????????????????????????????");
        }

        // ????????????
        List<String> fileContentList = new ArrayList<>();
        for (MultipartFile multipartFile : fileList) {
            try {
                Reader reader = new InputStreamReader(multipartFile.getInputStream(), "utf-8");
                BufferedReader br = new BufferedReader(reader);
                String line;
                String content = "";
                while ((line = br.readLine()) != null) {
                    content += line + "\n";
                }
                // ???Markdown?????????html
                String blogContent = FileUtils.markdownToHtml(content);
                fileContentList.add(blogContent);
            } catch (Exception e) {
                log.error("??????????????????");
                log.error(e.getMessage());
            }
        }

        HttpServletRequest request = RequestHolder.getRequest();
        String pictureList = request.getParameter(SysConf.PICTURE_LIST);
        List<LinkedTreeMap<String, String>> list = (List<LinkedTreeMap<String, String>>) JsonUtils.jsonArrayToArrayList(pictureList);
        Map<String, String> pictureMap = new HashMap<>();
        for (LinkedTreeMap<String, String> item : list) {

            if (EFilePriority.QI_NIU.equals(systemConfig.getContentPicturePriority())) {
                // ???????????????????????????
                pictureMap.put(item.get(SysConf.FILE_OLD_NAME), item.get(SysConf.QI_NIU_URL));
            } else if(EFilePriority.LOCAL.equals(systemConfig.getContentPicturePriority())) {
                // ?????????????????????
                pictureMap.put(item.get(SysConf.FILE_OLD_NAME), item.get(SysConf.PIC_URL));
            } else if(EFilePriority.MINIO.equals(systemConfig.getContentPicturePriority())) {
                // ??????MINIO?????????
                pictureMap.put(item.get(SysConf.FILE_OLD_NAME), item.get(SysConf.MINIO_URL));
            }
        }
        // ?????????????????????Map
        Map<String, String> matchUrlMap = new HashMap<>();
        for (String blogContent : fileContentList) {
            List<String> matchList = RegexUtils.match(blogContent, "<img\\s+(?:[^>]*)src\\s*=\\s*([^>]+)>");
            for (String matchStr : matchList) {
                String[] splitList = matchStr.split("\"");
                // ?????????????????????
                if (splitList.length >= 5) {
                    // alt ??? src???????????????
                    // ???????????????????????????
                    String pictureUrl = "";
                    if (matchStr.indexOf("alt") > matchStr.indexOf("src")) {
                        pictureUrl = splitList[1];
                    } else {
                        pictureUrl = splitList[3];
                    }

                    // ???????????????????????????????????????
                    if (!pictureUrl.startsWith(SysConf.HTTP)) {
                        // ??????????????????????????????map????????????
                        for (Map.Entry<String, String> map : pictureMap.entrySet()) {
                            // ??????Map????????????????????????????????????key???
                            if (pictureUrl.indexOf(map.getKey()) > -1) {
                                if (EFilePriority.QI_NIU.equals(systemConfig.getContentPicturePriority())) {
                                    // ???????????????????????????
                                    matchUrlMap.put(pictureUrl, systemConfig.getQiNiuPictureBaseUrl() + map.getValue());
                                } else if(EFilePriority.LOCAL.equals(systemConfig.getContentPicturePriority())) {
                                    // ?????????????????????
                                    matchUrlMap.put(pictureUrl, systemConfig.getLocalPictureBaseUrl() + map.getValue());
                                } else if(EFilePriority.MINIO.equals(systemConfig.getContentPicturePriority())) {
                                    // ??????MINIO?????????
                                    matchUrlMap.put(pictureUrl, systemConfig.getMinioPictureBaseUrl() + map.getValue());
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // ????????????????????????????????????????????????
        BlogSort blogSort = blogSortService.getTopOne();
        Tag tag = tagService.getTopTag();

        // ????????????????????????
        Picture picture = pictureService.getTopOne();
        if (blogSort == null || tag == null || picture == null) {
            return ResultUtil.errorWithMessage("??????????????????????????????????????????????????????????????????????????????????????????");
        }

        // ?????????????????????
        Admin admin = adminService.getMe();
        // ???????????????????????????
        List<Blog> blogList = new ArrayList<>();
        // ??????????????????????????????
        Integer count = 0;
        String projectName = sysParamsService.getSysParamsValueByKey(SysConf.PROJECT_NAME_);
        for (String content : fileContentList) {
            // ???????????????????????????
            for (Map.Entry<String, String> map : matchUrlMap.entrySet()) {
                content = content.replace(map.getKey(), map.getValue());
            }
            Blog blog = new Blog();
            blog.setBlogSortUid(blogSort.getUid());
            blog.setTagUid(tag.getUid());
            blog.setAdminUid(admin.getUid());
            blog.setAuthor(admin.getNickName());
            blog.setArticlesPart(projectName);
            blog.setLevel(ELevel.NORMAL);
            blog.setTitle(fileNameList.get(count));
            blog.setSummary(fileNameList.get(count));
            blog.setContent(content);
            blog.setFileUid(picture.getFileUid());
            blog.setIsOriginal(EOriginal.ORIGINAL);
            blog.setIsPublish(EPublish.NO_PUBLISH);
            blog.setOpenComment(EOpenStatus.OPEN);
            blog.setType(Constants.STR_ZERO);
            blogList.add(blog);
            count++;
        }
        // ??????????????????
        blogService.saveBatch(blogList);
        return ResultUtil.successWithMessage(MessageConf.INSERT_SUCCESS);
    }

    @Override
    public void deleteRedisByBlogSort() {
        // ??????Redis?????????????????????????????????
        redisUtil.delete(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_COUNT_BY_SORT);
        // ????????????????????????
        deleteRedisByBlog();
    }

    @Override
    public void deleteRedisByBlogTag() {
        // ??????Redis?????????????????????????????????
        redisUtil.delete(RedisConf.DASHBOARD + Constants.SYMBOL_COLON + RedisConf.BLOG_COUNT_BY_TAG);
        // ????????????????????????
        deleteRedisByBlog();
    }

    @Override
    public void deleteRedisByBlog() {
        // ????????????????????????
        redisUtil.delete(RedisConf.NEW_BLOG);
        redisUtil.delete(RedisConf.HOT_BLOG);
        redisUtil.delete(RedisConf.BLOG_LEVEL + Constants.SYMBOL_COLON + ELevel.FIRST);
        redisUtil.delete(RedisConf.BLOG_LEVEL + Constants.SYMBOL_COLON + ELevel.SECOND);
        redisUtil.delete(RedisConf.BLOG_LEVEL + Constants.SYMBOL_COLON + ELevel.THIRD);
        redisUtil.delete(RedisConf.BLOG_LEVEL + Constants.SYMBOL_COLON + ELevel.FOURTH);
    }

    @Override
    public IPage<Blog> getBlogPageByLevel(Integer level, Long currentPage, Integer useSort) {

        //???Redis???????????????
        String jsonResult = redisUtil.get(RedisConf.BLOG_LEVEL + RedisConf.SEGMENTATION + level);

        //??????redis??????????????????
        if (StringUtils.isNotEmpty(jsonResult)) {
            List jsonResult2List = JsonUtils.jsonArrayToArrayList(jsonResult);
            IPage pageList = new Page();
            pageList.setRecords(jsonResult2List);
            return pageList;
        }
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        String blogCount = null;
        switch (level) {
            case ELevel.NORMAL: {
                blogCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_NEW_COUNT);
            }
            break;
            case ELevel.FIRST: {
                blogCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_FIRST_COUNT);
            }
            break;
            case ELevel.SECOND: {
                blogCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_SECOND_COUNT);
            }
            break;
            case ELevel.THIRD: {
                blogCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_THIRD_COUNT);
            }
            break;
            case ELevel.FOURTH: {
                blogCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_FOURTH_COUNT);
            }
            break;
        }
        if (StringUtils.isEmpty(blogCount)) {
            log.error(MessageConf.PLEASE_CONFIGURE_SYSTEM_PARAMS);
        } else {
            page.setSize(Long.valueOf(blogCount));
        }

        IPage<Blog> pageList = blogService.getBlogPageByLevel(page, level, useSort);
        List<Blog> list = pageList.getRecords();

        // ?????????????????????????????????????????????????????????top5???????????????????????????????????????
        if ((level == SysConf.ONE || level == SysConf.TWO) && list.size() == 0) {
            QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
            Page<Blog> hotPage = new Page<>();
            hotPage.setCurrent(1);
            String blogHotCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_HOT_COUNT);
            String blogSecondCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_SECOND_COUNT);
            if (StringUtils.isEmpty(blogHotCount) || StringUtils.isEmpty(blogSecondCount)) {
                log.error(MessageConf.PLEASE_CONFIGURE_SYSTEM_PARAMS);
            } else {
                hotPage.setSize(Long.valueOf(blogHotCount));
            }
            queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
            queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
            queryWrapper.orderByDesc(SQLConf.CLICK_COUNT);
            queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));
            IPage<Blog> hotPageList = blogService.page(hotPage, queryWrapper);
            List<Blog> hotBlogList = hotPageList.getRecords();
            List<Blog> secondBlogList = new ArrayList<>();
            List<Blog> firstBlogList = new ArrayList<>();
            for (int a = 0; a < hotBlogList.size(); a++) {
                // ??????????????????????????????
                if ((hotBlogList.size() - firstBlogList.size()) > Long.valueOf(blogSecondCount)) {
                    firstBlogList.add(hotBlogList.get(a));
                } else {
                    secondBlogList.add(hotBlogList.get(a));
                }
            }

            firstBlogList = setBlog(firstBlogList);
            secondBlogList = setBlog(secondBlogList);

            // ???????????????????????????????????????redis????????????1??????????????? [?????? list ??????????????????????????? redis ?????????]
            if (firstBlogList.size() > 0) {
                redisUtil.setEx(RedisConf.BLOG_LEVEL + Constants.SYMBOL_COLON + Constants.NUM_ONE, JsonUtils.objectToJson(firstBlogList), 1, TimeUnit.HOURS);
            }
            if (secondBlogList.size() > 0) {
                redisUtil.setEx(RedisConf.BLOG_LEVEL + Constants.SYMBOL_COLON + Constants.NUM_TWO, JsonUtils.objectToJson(secondBlogList), 1, TimeUnit.HOURS);
            }

            switch (level) {
                case SysConf.ONE: {
                    pageList.setRecords(firstBlogList);
                }
                break;
                case SysConf.TWO: {
                    pageList.setRecords(secondBlogList);
                }
                break;
            }
            return pageList;
        }

        list = setBlog(list);
        pageList.setRecords(list);

        // ???????????????????????????????????????redis??? [?????? list ??????????????????????????? redis ?????????]
        if (list.size() > 0) {
            redisUtil.setEx(SysConf.BLOG_LEVEL + SysConf.REDIS_SEGMENTATION + level, JsonUtils.objectToJson(list).toString(), 1, TimeUnit.HOURS);
        }
        return pageList;
    }

    @Override
    public IPage<Blog> getHotBlog() {
        //???Redis???????????????
        String jsonResult = redisUtil.get(RedisConf.HOT_BLOG);
        //??????redis??????????????????
        if (StringUtils.isNotEmpty(jsonResult)) {
            List jsonResult2List = JsonUtils.jsonArrayToArrayList(jsonResult);
            IPage pageList = new Page();
            pageList.setRecords(jsonResult2List);
            return pageList;
        }
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        Page<Blog> page = new Page<>();
        page.setCurrent(0);
        String blogHotCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_HOT_COUNT);
        if (StringUtils.isEmpty(blogHotCount)) {
            log.error(MessageConf.PLEASE_CONFIGURE_SYSTEM_PARAMS);
        } else {
            page.setSize(Long.valueOf(blogHotCount));
        }
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.orderByDesc(SQLConf.CLICK_COUNT);
        //????????????????????????????????????????????????????????????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = setBlog(list);
        pageList.setRecords(list);
        // ???????????????????????????????????????redis???[??????list???????????????????????????redis?????????]
        if (list.size() > 0) {
            redisUtil.setEx(RedisConf.HOT_BLOG, JsonUtils.objectToJson(list), 1, TimeUnit.HOURS);
        }
        return pageList;
    }

    @Override
    public IPage<Blog> getNewBlog(Long currentPage, Long pageSize) {

        String blogNewCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_NEW_COUNT);
        if (StringUtils.isEmpty(blogNewCount)) {
            log.error(MessageConf.PLEASE_CONFIGURE_SYSTEM_PARAMS);
        }

//        // ??????Redis????????????????????????????????????
//        if (currentPage == 1L) {
//            //???Redis???????????????
//            String jsonResult = redisUtil.get(RedisConf.NEW_BLOG);
//            //??????redis??????????????????
//            if (StringUtils.isNotEmpty(jsonResult)) {
//                IPage pageList = JsonUtils.jsonToPojo(jsonResult, Page.class);
//                return pageList;
//            }
//        }

        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        page.setSize(Long.valueOf(blogNewCount));
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);

        //????????????????????????????????????????????????????????????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));

        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();

        if (list.size() <= 0) {
            return pageList;
        }

        list = setBlog(list);
        pageList.setRecords(list);

        //???????????????????????????redis???
//        if (currentPage == 1L) {
//            redisUtil.setEx(RedisConf.NEW_BLOG, JsonUtils.objectToJson(pageList), 1, TimeUnit.HOURS);
//        }
        return pageList;
    }

    @Override
    public IPage<Blog> getBlogBySearch(Long currentPage, Long pageSize) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        String blogNewCount = sysParamsService.getSysParamsValueByKey(SysConf.BLOG_NEW_COUNT);
        if (StringUtils.isEmpty(blogNewCount)) {
            log.error(MessageConf.PLEASE_CONFIGURE_SYSTEM_PARAMS);
        } else {
            page.setSize(Long.valueOf(blogNewCount));
        }
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        if (list.size() <= 0) {
            return pageList;
        }
        list = setBlog(list);
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public IPage<Blog> getBlogByTime(Long currentPage, Long pageSize) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        page.setSize(pageSize);
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        //????????????????????????????????????????????????????????????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = setBlog(list);
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public Integer getBlogPraiseCountByUid(String uid) {
        Integer pariseCount = 0;
        if (StringUtils.isEmpty(uid)) {
            log.error("?????????UID??????");
            return pariseCount;
        }
        //???Redis????????????????????????
        String pariseJsonResult = redisUtil.get(RedisConf.BLOG_PRAISE + RedisConf.SEGMENTATION + uid);
        if (!StringUtils.isEmpty(pariseJsonResult)) {
            pariseCount = Integer.parseInt(pariseJsonResult);
        }
        return pariseCount;
    }

    @Override
    public String praiseBlogByUid(String uid) {
        if (StringUtils.isEmpty(uid)) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }

        HttpServletRequest request = RequestHolder.getRequest();
        // ?????????????????????
        if (request.getAttribute(SysConf.USER_UID) != null) {
            String userUid = request.getAttribute(SysConf.USER_UID).toString();
            QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(SQLConf.USER_UID, userUid);
            queryWrapper.eq(SQLConf.BLOG_UID, uid);
            queryWrapper.eq(SQLConf.TYPE, ECommentType.PRAISE);
            queryWrapper.last(SysConf.LIMIT_ONE);
            Comment praise = commentService.getOne(queryWrapper);
            if (praise != null) {
                return ResultUtil.errorWithMessage(MessageConf.YOU_HAVE_BEEN_PRISE);
            }
        } else {
            return ResultUtil.errorWithMessage(MessageConf.PLEASE_LOGIN_TO_PRISE);
        }
        Blog blog = blogService.getById(uid);
        String pariseJsonResult = redisUtil.get(RedisConf.BLOG_PRAISE + RedisConf.SEGMENTATION + uid);
        if (StringUtils.isEmpty(pariseJsonResult)) {
            //?????????????????????
            redisUtil.set(RedisConf.BLOG_PRAISE + RedisConf.SEGMENTATION + uid, "1");
            blog.setCollectCount(1);
            blog.updateById();

        } else {
            Integer count = blog.getCollectCount() + 1;
            //?????????????????? +1
            redisUtil.set(RedisConf.BLOG_PRAISE + RedisConf.SEGMENTATION + uid, count.toString());
            blog.setCollectCount(count);
            blog.updateById();
        }
        // ????????????????????????????????????????????????
        if (request.getAttribute(SysConf.USER_UID) != null) {
            String userUid = request.getAttribute(SysConf.USER_UID).toString();
            Comment comment = new Comment();
            comment.setUserUid(userUid);
            comment.setBlogUid(uid);
            comment.setSource(ECommentSource.BLOG_INFO.getCode());
            comment.setType(ECommentType.PRAISE);
            comment.insert();
        }
        return ResultUtil.successWithData(blog.getCollectCount());
    }

    @Override
    public IPage<Blog> getSameBlogByTagUid(String tagUid) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        Page<Blog> page = new Page<>();
        page.setCurrent(1);
        page.setSize(10);
        queryWrapper.like(SQLConf.TAG_UID, tagUid);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = blogService.setTagAndSortByBlogList(list);
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public IPage<Blog> getListByBlogSortUid(String blogSortUid, Long currentPage, Long pageSize) {
        //??????
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        page.setSize(pageSize);
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.eq(SQLConf.BLOG_SORT_UID, blogSortUid);

        //????????????????????????????????????????????????????????????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));
        IPage<Blog> pageList = blogService.page(page, queryWrapper);

        //??????????????????????????????
        List<Blog> list = blogService.setTagAndSortAndPictureByBlogList(pageList.getRecords());
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public Map<String, Object> getBlogByKeyword(String keywords, Long currentPage, Long pageSize) {
        final String keyword = keywords.trim();
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.like(SQLConf.TITLE, keyword).or().like(SQLConf.SUMMARY, keyword));
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));
        queryWrapper.orderByDesc(SQLConf.CLICK_COUNT);
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        page.setSize(pageSize);

        IPage<Blog> iPage = blogService.page(page, queryWrapper);
        List<Blog> blogList = iPage.getRecords();
        List<String> blogSortUidList = new ArrayList<>();
        Map<String, String> pictureMap = new HashMap<>();
        final StringBuffer fileUids = new StringBuffer();
        blogList.forEach(item -> {
            // ????????????uid
            blogSortUidList.add(item.getBlogSortUid());
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                fileUids.append(item.getFileUid() + SysConf.FILE_SEGMENTATION);
            }
            // ??????????????????????????????
            item.setTitle(getHitCode(item.getTitle(), keyword));
            item.setSummary(getHitCode(item.getSummary(), keyword));

        });

        // ?????????????????????????????????
        String pictureList = null;
        if (fileUids != null) {
            pictureList = this.pictureFeignClient.getPicture(fileUids.toString(), SysConf.FILE_SEGMENTATION);
        }
        List<Map<String, Object>> picList = webUtil.getPictureMap(pictureList);

        picList.forEach(item -> {
            pictureMap.put(item.get(SQLConf.UID).toString(), item.get(SQLConf.URL).toString());
        });

        Collection<BlogSort> blogSortList = new ArrayList<>();
        if (blogSortUidList.size() > 0) {
            blogSortList = blogSortService.listByIds(blogSortUidList);
        }

        Map<String, String> blogSortMap = new HashMap<>();
        blogSortList.forEach(item -> {
            blogSortMap.put(item.getUid(), item.getSortName());
        });

        // ??????????????? ??? ??????
        blogList.forEach(item -> {
            if (blogSortMap.get(item.getBlogSortUid()) != null) {
                item.setBlogSortName(blogSortMap.get(item.getBlogSortUid()));
            }

            //????????????
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                List<String> pictureUidsTemp = StringUtils.changeStringToString(item.getFileUid(), SysConf.FILE_SEGMENTATION);
                List<String> pictureListTemp = new ArrayList<>();

                pictureUidsTemp.forEach(picture -> {
                    pictureListTemp.add(pictureMap.get(picture));
                });
                // ????????????????????????
                if (pictureListTemp.size() > 0) {
                    item.setPhotoUrl(pictureListTemp.get(0));
                } else {
                    item.setPhotoUrl("");
                }
            }
        });

        Map<String, Object> map = new HashMap<>();
        // ??????????????????
        map.put(SysConf.TOTAL, iPage.getTotal());
        // ???????????????
        map.put(SysConf.TOTAL_PAGE, iPage.getPages());
        // ?????????????????????
        map.put(SysConf.PAGE_SIZE, pageSize);
        // ???????????????
        map.put(SysConf.CURRENT_PAGE, iPage.getCurrent());
        // ????????????
        map.put(SysConf.BLOG_LIST, blogList);
        return map;
    }

    @Override
    public IPage<Blog> searchBlogByTag(String tagUid, Long currentPage, Long pageSize) {
        Tag tag = tagService.getById(tagUid);
        if (tag != null) {
            HttpServletRequest request = RequestHolder.getRequest();
            String ip = IpUtils.getIpAddr(request);
            //???Redis??????????????????????????????24????????????????????????????????????
            String jsonResult = redisUtil.get(RedisConf.TAG_CLICK + RedisConf.SEGMENTATION + ip + "#" + tagUid);
            if (StringUtils.isEmpty(jsonResult)) {
                //????????????????????????
                int clickCount = tag.getClickCount() + 1;
                tag.setClickCount(clickCount);
                tag.updateById();
                //?????????????????????????????????redis???, 24???????????????
                redisUtil.setEx(RedisConf.TAG_CLICK + RedisConf.SEGMENTATION + ip + RedisConf.WELL_NUMBER + tagUid, clickCount + "",
                        24, TimeUnit.HOURS);
            }
        }
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        page.setSize(pageSize);

        queryWrapper.like(SQLConf.TAG_UID, tagUid);
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SysConf.CONTENT));
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = blogService.setTagAndSortAndPictureByBlogList(list);
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public IPage<Blog> searchBlogByBlogSort(String blogSortUid, Long currentPage, Long pageSize) {
        BlogSort blogSort = blogSortService.getById(blogSortUid);
        if (blogSort != null) {
            HttpServletRequest request = RequestHolder.getRequest();
            String ip = IpUtils.getIpAddr(request);

            //???Redis??????????????????????????????24????????????????????????????????????
            String jsonResult = redisUtil.get(RedisConf.TAG_CLICK + RedisConf.SEGMENTATION + ip + RedisConf.WELL_NUMBER + blogSortUid);
            if (StringUtils.isEmpty(jsonResult)) {
                //????????????????????????
                int clickCount = blogSort.getClickCount() + 1;
                blogSort.setClickCount(clickCount);
                blogSort.updateById();
                //?????????????????????????????????redis???, 24???????????????
                redisUtil.setEx(RedisConf.TAG_CLICK + RedisConf.SEGMENTATION + ip + RedisConf.WELL_NUMBER + blogSortUid, clickCount + "",
                        24, TimeUnit.HOURS);
            }
        }

        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        page.setSize(pageSize);
        queryWrapper.eq(SQLConf.BLOG_SORT_UID, blogSortUid);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.eq(BaseSQLConf.STATUS, EStatus.ENABLE);
        // ??????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SysConf.CONTENT));
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = blogService.setTagAndSortAndPictureByBlogList(list);
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public IPage<Blog> searchBlogByAuthor(String author, Long currentPage, Long pageSize) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();

        Page<Blog> page = new Page<>();
        page.setCurrent(currentPage);
        page.setSize(pageSize);
        queryWrapper.eq(SQLConf.AUTHOR, author);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        queryWrapper.eq(BaseSQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SysConf.CONTENT));
        IPage<Blog> pageList = blogService.page(page, queryWrapper);
        List<Blog> list = pageList.getRecords();
        list = blogService.setTagAndSortAndPictureByBlogList(list);
        pageList.setRecords(list);
        return pageList;
    }

    @Override
    public String getBlogTimeSortList() {
        //???Redis???????????????
        String monthResult = redisUtil.get(SysConf.MONTH_SET);
        //??????redis??????????????????????????????
        if (StringUtils.isNotEmpty(monthResult)) {
            List list = JsonUtils.jsonArrayToArrayList(monthResult);
            return ResultUtil.successWithData(list);
        }
        // ??????????????????????????????
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
        //????????????????????????????????????????????????????????????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));
        List<Blog> list = blogService.list(queryWrapper);

        //???????????????????????????????????????
        list = blogService.setTagAndSortAndPictureByBlogList(list);

        Map<String, List<Blog>> map = new HashMap<>();
        Iterator iterable = list.iterator();
        Set<String> monthSet = new TreeSet<>();
        while (iterable.hasNext()) {
            Blog blog = (Blog) iterable.next();
            Date createTime = blog.getCreateTime();

            String month = new SimpleDateFormat("yyyy???MM???").format(createTime).toString();

            monthSet.add(month);

            if (map.get(month) == null) {
                List<Blog> blogList = new ArrayList<>();
                blogList.add(blog);
                map.put(month, blogList);
            } else {
                List<Blog> blogList = map.get(month);
                blogList.add(blog);
                map.put(month, blogList);
            }
        }

        // ?????????????????????????????????  key: ??????   value???????????????????????????
        map.forEach((key, value) -> {
            redisUtil.set(SysConf.BLOG_SORT_BY_MONTH + SysConf.REDIS_SEGMENTATION + key, JsonUtils.objectToJson(value).toString());
        });

        //???????????????????????????????????????redis???
        redisUtil.set(SysConf.MONTH_SET, JsonUtils.objectToJson(monthSet).toString());
        return ResultUtil.successWithData(monthSet);
    }

    @Override
    public String getArticleByMonth(String monthDate) {
        if (StringUtils.isEmpty(monthDate)) {
            return ResultUtil.errorWithMessage(MessageConf.PARAM_INCORRECT);
        }
        //???Redis???????????????
        String contentResult = redisUtil.get(SysConf.BLOG_SORT_BY_MONTH + SysConf.REDIS_SEGMENTATION + monthDate);

        //??????redis????????????????????????????????????
        if (StringUtils.isNotEmpty(contentResult)) {
            List list = JsonUtils.jsonArrayToArrayList(contentResult);
            return ResultUtil.successWithData(list);
        }

        // ??????????????????????????????
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.orderByDesc(SQLConf.CREATE_TIME);
        queryWrapper.eq(BaseSQLConf.IS_PUBLISH, EPublish.PUBLISH);
        //????????????????????????????????????????????????????????????????????????
        queryWrapper.select(Blog.class, i -> !i.getProperty().equals(SQLConf.CONTENT));
        List<Blog> list = blogService.list(queryWrapper);

        //???????????????????????????????????????
        list = blogService.setTagAndSortAndPictureByBlogList(list);

        Map<String, List<Blog>> map = new HashMap<>();
        Iterator iterable = list.iterator();
        Set<String> monthSet = new TreeSet<>();
        while (iterable.hasNext()) {
            Blog blog = (Blog) iterable.next();
            Date createTime = blog.getCreateTime();

            String month = new SimpleDateFormat("yyyy???MM???").format(createTime).toString();

            monthSet.add(month);

            if (map.get(month) == null) {
                List<Blog> blogList = new ArrayList<>();
                blogList.add(blog);
                map.put(month, blogList);
            } else {
                List<Blog> blogList = map.get(month);
                blogList.add(blog);
                map.put(month, blogList);
            }
        }

        // ?????????????????????????????????  key: ??????   value???????????????????????????
        map.forEach((key, value) -> {
            redisUtil.set(SysConf.BLOG_SORT_BY_MONTH + SysConf.REDIS_SEGMENTATION + key, JsonUtils.objectToJson(value).toString());
        });
        //???????????????????????????????????????redis???
        redisUtil.set(SysConf.MONTH_SET, JsonUtils.objectToJson(monthSet));
        return ResultUtil.successWithData(map.get(monthDate));
    }

    /**
     * ???????????????
     *
     * @param count
     * @param level
     * @return
     */
    private String addVerdict(Integer count, Integer level) {

        //???????????????????????????
        switch (level) {
            case ELevel.FIRST: {
                Long blogFirstCount = Long.valueOf(sysParamsService.getSysParamsValueByKey(SysConf.BLOG_FIRST_COUNT));
                if (count > blogFirstCount) {
                    return ResultUtil.errorWithMessage("????????????????????????" + blogFirstCount + "???");
                }
            }
            break;

            case ELevel.SECOND: {
                Long blogSecondCount = Long.valueOf(sysParamsService.getSysParamsValueByKey(SysConf.BLOG_SECOND_COUNT));
                if (count > blogSecondCount) {
                    return ResultUtil.errorWithMessage("????????????????????????" + blogSecondCount + "???");
                }
            }
            break;

            case ELevel.THIRD: {
                Long blogThirdCount = Long.valueOf(sysParamsService.getSysParamsValueByKey(SysConf.BLOG_THIRD_COUNT));
                if (count > blogThirdCount) {
                    return ResultUtil.errorWithMessage("????????????????????????" + blogThirdCount + "???");
                }
            }
            break;

            case ELevel.FOURTH: {
                Long blogFourthCount = Long.valueOf(sysParamsService.getSysParamsValueByKey(SysConf.BLOG_FOURTH_COUNT));
                if (count > blogFourthCount) {
                    return ResultUtil.errorWithMessage("????????????????????????" + blogFourthCount + "???");
                }
            }
            break;
            default: {

            }
        }
        return null;
    }

    /**
     * ???????????????????????????????????????solr ??? redis
     *
     * @param isSave
     * @param blog
     */
    private void updateSolrAndRedis(Boolean isSave, Blog blog) {
        // ??????????????????????????????????????????
        if (isSave && EPublish.PUBLISH.equals(blog.getIsPublish())) {
            Map<String, Object> map = new HashMap<>();
            map.put(SysConf.COMMAND, SysConf.ADD);
            map.put(SysConf.BLOG_UID, blog.getUid());
            map.put(SysConf.LEVEL, blog.getLevel());
            map.put(SysConf.CREATE_TIME, blog.getCreateTime());

            rocketMQTemplate.syncSend(BlogMessage.TOPIC, map);
        } else if (EPublish.NO_PUBLISH.equals(blog.getIsPublish())) {

            //?????????????????????????????????redis????????????????????????
            Map<String, Object> map = new HashMap<>();
            map.put(SysConf.COMMAND, SysConf.EDIT);
            map.put(SysConf.BLOG_UID, blog.getUid());
            map.put(SysConf.LEVEL, blog.getLevel());
            map.put(SysConf.CREATE_TIME, blog.getCreateTime());

            rocketMQTemplate.syncSend(BlogMessage.TOPIC, map);
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param list
     * @return
     */
    private List<Blog> setBlog(List<Blog> list) {
        final StringBuffer fileUids = new StringBuffer();
        List<String> sortUids = new ArrayList<>();
        List<String> tagUids = new ArrayList<>();

        list.forEach(item -> {
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                fileUids.append(item.getFileUid() + SysConf.FILE_SEGMENTATION);
            }
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                sortUids.add(item.getBlogSortUid());
            }
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                tagUids.add(item.getTagUid());
            }
        });
        String pictureList = null;

        if (fileUids != null) {
            pictureList = this.pictureFeignClient.getPicture(fileUids.toString(), SysConf.FILE_SEGMENTATION);
        }
        List<Map<String, Object>> picList = webUtil.getPictureMap(pictureList);
        Collection<BlogSort> sortList = new ArrayList<>();
        Collection<Tag> tagList = new ArrayList<>();
        if (sortUids.size() > 0) {
            sortList = blogSortService.listByIds(sortUids);
        }
        if (tagUids.size() > 0) {
            tagList = tagService.listByIds(tagUids);
        }

        Map<String, BlogSort> sortMap = new HashMap<>();
        Map<String, Tag> tagMap = new HashMap<>();
        Map<String, String> pictureMap = new HashMap<>();

        sortList.forEach(item -> {
            sortMap.put(item.getUid(), item);
        });

        tagList.forEach(item -> {
            tagMap.put(item.getUid(), item);
        });

        picList.forEach(item -> {
            pictureMap.put(item.get(SQLConf.UID).toString(), item.get(SQLConf.URL).toString());
        });


        for (Blog item : list) {

            //????????????
            if (StringUtils.isNotEmpty(item.getBlogSortUid())) {
                item.setBlogSort(sortMap.get(item.getBlogSortUid()));
            }

            //????????????
            if (StringUtils.isNotEmpty(item.getTagUid())) {
                List<String> tagUidsTemp = StringUtils.changeStringToString(item.getTagUid(), SysConf.FILE_SEGMENTATION);
                List<Tag> tagListTemp = new ArrayList<Tag>();

                tagUidsTemp.forEach(tag -> {
                    if (tagMap.get(tag) != null) {
                        tagListTemp.add(tagMap.get(tag));
                    }
                });
                item.setTagList(tagListTemp);
            }

            //????????????
            if (StringUtils.isNotEmpty(item.getFileUid())) {
                List<String> pictureUidsTemp = StringUtils.changeStringToString(item.getFileUid(), SysConf.FILE_SEGMENTATION);
                List<String> pictureListTemp = new ArrayList<>();

                pictureUidsTemp.forEach(picture -> {
                    pictureListTemp.add(pictureMap.get(picture));
                });
                item.setPhotoList(pictureListTemp);
            }
        }
        return list;
    }

    /**
     * ????????????
     *
     * @param str
     * @param keyword
     * @return
     */
    private String getHitCode(String str, String keyword) {
        if (StringUtils.isEmpty(keyword) || StringUtils.isEmpty(str)) {
            return str;
        }
        String startStr = "<span style = 'color:red'>";
        String endStr = "</span>";
        // ??????????????????????????????????????????????????????????????????
        if (str.equals(keyword)) {
            return startStr + str + endStr;
        }
        String lowerCaseStr = str.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        String[] lowerCaseArray = lowerCaseStr.split(lowerKeyword);
        Boolean isEndWith = lowerCaseStr.endsWith(lowerKeyword);

        // ?????????????????????????????????
        Integer count = 0;
        List<Map<String, Integer>> list = new ArrayList<>();
        List<Map<String, Integer>> keyList = new ArrayList<>();
        for (int a = 0; a < lowerCaseArray.length; a++) {
            // ????????????????????????map
            Map<String, Integer> map = new HashMap<>();
            Map<String, Integer> keyMap = new HashMap<>();
            map.put("startIndex", count);
            Integer len = lowerCaseArray[a].length();
            count += len;
            map.put("endIndex", count);
            list.add(map);
            if (a < lowerCaseArray.length - 1 || isEndWith) {
                // ???keyword??????map
                keyMap.put("startIndex", count);
                count += keyword.length();
                keyMap.put("endIndex", count);
                keyList.add(keyMap);
            }
        }
        // ??????????????????
        List<String> arrayList = new ArrayList<>();
        for (Map<String, Integer> item : list) {
            Integer start = item.get("startIndex");
            Integer end = item.get("endIndex");
            String itemStr = str.substring(start, end);
            arrayList.add(itemStr);
        }
        // ???????????????
        List<String> keyArrayList = new ArrayList<>();
        for (Map<String, Integer> item : keyList) {
            Integer start = item.get("startIndex");
            Integer end = item.get("endIndex");
            String itemStr = str.substring(start, end);
            keyArrayList.add(itemStr);
        }

        StringBuffer sb = new StringBuffer();
        for (int a = 0; a < arrayList.size(); a++) {
            sb.append(arrayList.get(a));
            if (a < arrayList.size() - 1 || isEndWith) {
                sb.append(startStr);
                sb.append(keyArrayList.get(a));
                sb.append(endStr);
            }
        }
        return sb.toString();
    }

}

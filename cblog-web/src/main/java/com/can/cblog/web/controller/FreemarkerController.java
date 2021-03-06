package com.can.cblog.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.can.cblog.base.enums.ELevel;
import com.can.cblog.base.enums.EPublish;
import com.can.cblog.base.enums.EStatus;
import com.can.cblog.common.entity.Blog;
import com.can.cblog.common.entity.BlogSort;
import com.can.cblog.common.entity.SystemConfig;
import com.can.cblog.common.entity.Tag;
import com.can.cblog.common.feign.PictureFeignClient;
import com.can.cblog.utils.ResultUtil;
import com.can.cblog.utils.StringUtils;
import com.can.cblog.web.global.SQLConf;
import com.can.cblog.web.global.SysConf;
import com.can.cblog.xo.service.*;
import com.can.cblog.xo.utils.WebUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author ccc
 */
@RequestMapping("freemarker")
@RefreshScope
@Controller
public class FreemarkerController {

    @Autowired
    WebUtil webUtil;

    @Autowired
    WebConfigService webConfigService;

    @Autowired
    SystemConfigService systemConfigService;

    @Autowired
    BlogService blogService;

    @Autowired
    TagService tagService;

    @Autowired
    BlogSortService blogSortService;

    @Autowired
    LinkService linkService;

    @Autowired
    private PictureFeignClient pictureFeignClient;

    @Value(value = "${file.upload.path}")
    private String fileUploadPath;

    @Value(value = "${data.webSite.url}")
    private String webSiteUrl;

    @Value(value = "${data.web.url}")
    private String webUrl;

    @RequestMapping("/info/{uid}")
    public String index(Map<String, Object> map, @PathVariable("uid") String uid) {
        // fc98d2ae7756d2587390ae441b82f52d
        List<Blog> sameBlog = blogService.getSameBlogByBlogUid(uid);
        sameBlog = setBlog(sameBlog);

        List<Blog> thirdBlog = blogService.getBlogListByLevel(ELevel.THIRD);
        thirdBlog = setBlog(thirdBlog);

        List<Blog> fourthBlog = blogService.getBlogListByLevel(ELevel.FOURTH);
        fourthBlog = setBlog(fourthBlog);

        SystemConfig systemConfig = systemConfigService.getConfig();
        if (systemConfig == null) {
            return ResultUtil.result(SysConf.ERROR, "??????????????????");
        }

        map.put("vueWebBasePath", webSiteUrl);
        map.put("webBasePath", webUrl);
        map.put("staticBasePath", systemConfig.getLocalPictureBaseUrl());
        map.put("webConfig", webConfigService.getWebConfig());
        map.put("blog", blogService.getBlogByUid(uid));
        map.put("sameBlog", sameBlog);
        map.put("thirdBlogList", thirdBlog);
        map.put("fourthBlogList", fourthBlog);
        map.put("fourthBlogList", fourthBlog);
        map.put("hotBlogList", blogService.getBlogListByTop(SysConf.FIVE));
        return "info";
    }

    /**
     * ??????????????????
     *
     * @throws IOException
     * @throws TemplateException
     */
    public void generateHtml(String uid) {
        try {
            //???????????????
            Configuration configuration = new Configuration(Configuration.getVersion());
            String classpath = this.getClass().getResource("/").getPath();
            //??????????????????
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //???????????????
            configuration.setDefaultEncoding("utf-8");
            //????????????
            Template template = configuration.getTemplate("info.ftl");
            //????????????
            Map map = new HashMap();

            List<Blog> sameBlog = blogService.getSameBlogByBlogUid(uid);
            sameBlog = setBlog(sameBlog);

            List<Blog> thirdBlog = blogService.getBlogListByLevel(ELevel.THIRD);
            thirdBlog = setBlog(thirdBlog);

            List<Blog> fourthBlog = blogService.getBlogListByLevel(ELevel.FOURTH);
            fourthBlog = setBlog(fourthBlog);

            map.put("vueWebBasePath", "http://localhost:9527/#/");
            map.put("webBasePath", "http://localhost:8603");
            map.put("staticBasePath", "http://localhost:8600");
            map.put("webConfig", webConfigService.getWebConfig());
            map.put("blog", blogService.getBlogByUid(uid));
            map.put("sameBlog", sameBlog);
            map.put("hotBlogList", blogService.getBlogListByTop(SysConf.FIVE));

            //?????????
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

            InputStream inputStream = IOUtils.toInputStream(content);
            //????????????
            String savePath = fileUploadPath + "/blog/page/" + uid + ".html";
            FileOutputStream fileOutputStream = new FileOutputStream(new File(savePath));
            int copy = IOUtils.copy(inputStream, fileOutputStream);
        } catch (Exception e) {
            e.getMessage();
        }

    }

    /**
     * ?????????????????????????????????
     */
    @RequestMapping("/getAllHtml")
    @ResponseBody
    public String getAllHtml() throws IOException {
        FileOutputStream fileOutputStream = null;
        InputStream inputStream = null;
        try {
            //???????????????
            Configuration configuration = new Configuration(Configuration.getVersion());
            String classpath = this.getClass().getResource("/").getPath();
            //??????????????????
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //???????????????
            configuration.setDefaultEncoding("utf-8");
            //????????????
            Template template = configuration.getTemplate("info.ftl");

            QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
            queryWrapper.eq(SQLConf.IS_PUBLISH, EPublish.PUBLISH);
            List<Blog> blogList = blogService.list(queryWrapper);
            blogList = setBlog(blogList);
            Map<String, List<Blog>> blogMap = new HashMap<>();
            List<Blog> thirdBlog = new ArrayList<>();
            List<Blog> fourthBlog = new ArrayList<>();
            List<Blog> hotBlogList = blogService.getBlogListByTop(SysConf.FIVE);
            blogList.forEach(item -> {

                if (item.getLevel() == ELevel.THIRD) {
                    thirdBlog.add(item);
                } else if (item.getLevel() == ELevel.FOURTH) {
                    fourthBlog.add(item);
                }

                List<Blog> tempList = blogMap.get(item.getBlogSortUid());
                if (tempList != null && tempList.size() > 0) {
                    tempList.add(item);
                    blogMap.put(item.getBlogSortUid(), tempList);
                } else {
                    List<Blog> temp = new ArrayList<>();
                    temp.add(item);
                    blogMap.put(item.getBlogSortUid(), temp);
                }
            });

            SystemConfig systemConfig = systemConfigService.getConfig();
            if (systemConfig == null) {
                return ResultUtil.result(SysConf.ERROR, "??????????????????");
            }

            for (int a = 0; a < blogList.size(); a++) {
                //????????????
                Map map = new HashMap();
                List<Blog> sameBlog = blogMap.get(blogList.get(a).getBlogSortUid());
                map.put("vueWebBasePath", webSiteUrl);
                map.put("webBasePath", webUrl);
                map.put("staticBasePath", systemConfig.getLocalPictureBaseUrl());
                map.put("webConfig", webConfigService.getWebConfig());
                map.put("blog", blogList.get(a));
                map.put("sameBlog", sameBlog);
                map.put("thirdBlogList", thirdBlog);
                map.put("fourthBlogList", fourthBlog);
                map.put("hotBlogList", hotBlogList);
                //?????????
                String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

                inputStream = IOUtils.toInputStream(content);

                //????????????
                String savePath = fileUploadPath + "/blog/page/" + blogList.get(a).getUid() + ".html";
                fileOutputStream = new FileOutputStream(new File(savePath));
                IOUtils.copy(inputStream, fileOutputStream);
            }
            return ResultUtil.result(SysConf.SUCCESS, "????????????");
        } catch (Exception e) {
            e.getMessage();
        } finally {
            inputStream.close();
            fileOutputStream.close();
        }
        return ResultUtil.result(SysConf.SUCCESS, "????????????");
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
        return list;
    }
}


/**
 * 不要因为走了很远就忘记当初出发的目的:whatever happened,be yourself
 */
package com.xiaoyu.modules.biz.article.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xiaoyu.common.base.BaseService;
import com.xiaoyu.common.base.ResponseCode;
import com.xiaoyu.common.base.ResponseMapper;
import com.xiaoyu.common.utils.ElasticUtils;
import com.xiaoyu.common.utils.IdGenerator;
import com.xiaoyu.common.utils.JedisUtils;
import com.xiaoyu.common.utils.TimeUtils;
import com.xiaoyu.common.utils.UserUtils;
import com.xiaoyu.maple.core.MapleUtil;
import com.xiaoyu.modules.biz.article.dao.ArticleAttrDao;
import com.xiaoyu.modules.biz.article.dao.ArticleCollectDao;
import com.xiaoyu.modules.biz.article.dao.ArticleCommentDao;
import com.xiaoyu.modules.biz.article.dao.ArticleDao;
import com.xiaoyu.modules.biz.article.dao.ArticleLikeDao;
import com.xiaoyu.modules.biz.article.entity.Article;
import com.xiaoyu.modules.biz.article.entity.ArticleAttr;
import com.xiaoyu.modules.biz.article.entity.ArticleCollect;
import com.xiaoyu.modules.biz.article.entity.ArticleComment;
import com.xiaoyu.modules.biz.article.entity.ArticleLike;
import com.xiaoyu.modules.biz.article.entity.CommentLike;
import com.xiaoyu.modules.biz.article.service.api.IArticleService;
import com.xiaoyu.modules.biz.article.vo.ArticleCommentVo;
import com.xiaoyu.modules.biz.article.vo.ArticleVo;
import com.xiaoyu.modules.biz.message.entity.Message;
import com.xiaoyu.modules.biz.message.service.MessageHandler;
import com.xiaoyu.modules.biz.user.dao.UserAttrDao;
import com.xiaoyu.modules.biz.user.dao.UserDao;
import com.xiaoyu.modules.biz.user.entity.User;
import com.xiaoyu.modules.constant.NumCountType;

/**
 * @author xiaoyu 2016年3月29日
 */
@Service
@Primary
public class ArticleServiceImpl extends BaseService<ArticleDao, Article> implements IArticleService {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleServiceImpl.class);

    @Autowired
    private UserDao userDao;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private ArticleAttrDao attrDao;
    @Autowired
    private ArticleCollectDao collectDao;
    @Autowired
    private UserAttrDao userAttrDao;
    @Autowired
    private MessageHandler msgHandler;

    private Map<String, Object> article2Map(ArticleVo a) {
        return MapleUtil.wrap(a)
                .rename("id", "articleId")
                .stick("createDate", TimeUtils.format(a.getCreateDate(), "yyyy-MM-dd"))
                .stick("createTime", TimeUtils.format(a.getCreateDate(), "HH:mm"))
                .stick("user", this.userDao.getVoById(a.getUserId()))
                .map();
    }

    private Map<String, Object> article2Map1(ArticleVo a) {
        Map<String, Object> map = MapleUtil.wrap(a)
                .rename("id", "articleId")
                .stick("content", a.getContent().length() > 150 ? a.getContent().substring(0, 149) : a.getContent())
                .stick("isLike", "0")
                .stick("isCollect", "0")
                .stick("user", this.userDao.getVoById(a.getUserId()))
                .map();
        return map;
    }

    // 发送消息
    private void sendMsg(String userId, int type, String bizId, int bizType, int bizAction, String content,
            String reply) {
        final Message msg = new Message();
        msg.setSenderId(userId)
                .setType(type)
                .setBizId(bizId)
                .setBizType(bizType)
                .setBizAction(bizAction);
        if (content != null) {
            msg.setContent(content);
        }
        if (reply != null) {
            msg.setReply(reply);
        }
        try {
            this.msgHandler.produce(JSON.toJSONString(msg));
        } catch (Exception e) {
            LOG.error("produce msg error.", e);
            // do nothing
        }

    }

    @Override
    public String detail(String articleId) {
        ResponseMapper mapper = ResponseMapper.createMapper();
        ArticleVo a = this.articleDao.getVo(articleId);
        if (a == null) {
            return mapper.code(ResponseCode.NO_DATA.statusCode()).resultJson();
        }
        return mapper.data(this.article2Map(a)).resultJson();
    }

    @Transactional(readOnly = false, rollbackFor = RuntimeException.class)
    private String publish(String userId, String title, String content) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        Article t = new Article();
        ArticleAttr attr = new ArticleAttr();
        t.setContent(content)
                .setTitle(title)
                .setUserId(userId)
                .setId(IdGenerator.uuid());
        try {
            this.articleDao.insert(t);

            attr.setArticleId(t.getId())
                    .setId(IdGenerator.uuid());
            this.attrDao.insert(attr);
            // 增加发文数
            this.userAttrDao.addNum(NumCountType.ArticleNum.ordinal(), 1, userId);
        } catch (RuntimeException e) {
            LOG.error("publish artile failed,then rollback", e);
            throw e;
        }
        return mapper.data(t.getId()).resultJson();
    }

    @Override
    public String hotList(HttpServletRequest request) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        // if (EhCacheUtil.IsExist("SystemCache")) {// 从缓存取
        // @SuppressWarnings("unchecked")
        // List<Object> total = (List<Object>) EhCacheUtil.get("SystemCache",
        // "pageList");
        // if (total != null && total.size() > 0) {
        // return mapper.data(total).resultJson();
        // }
        // }
        PageHelper.startPage(1, 12);
        Page<ArticleVo> page = (Page<ArticleVo>) this.articleDao.findHotList();
        List<ArticleVo> list = page.getResult();
        List<Object> total = new ArrayList<>();
        // 是否登录
        boolean isLogin = (UserUtils.checkLoginDead(request) != null);
        ArticleLike t = new ArticleLike();
        t.setUserId(request.getHeader("userId"));
        final ArticleCollect t1 = new ArticleCollect();
        t1.setUserId(request.getHeader("userId"));

        for (final ArticleVo a : list) {
            final Map<String, Object> m = this.article2Map1(a);
            if (isLogin) {
                t.setArticleId(a.getId());
                final ArticleLike al = this.likeDao.get(t);
                if (al != null) {
                    m.put("isLike", al.getStatus() + "");
                }
                t1.setArticleId(a.getId());
                final ArticleCollect ac = this.collectDao.get(t1);
                if (ac != null) {
                    m.put("isCollect", ac.getStatus() + "");
                }
            }
            total.add(m);
        }
        // EhCacheUtil.put("SystemCache", "pageList", total);// 存入缓存
        return mapper.data(total).resultJson();
    }

    @Override
    public String list(HttpServletRequest request, String userId, Integer pageNum, Integer pageSize) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        final Article article = new Article();
        article.setUserId(userId);
        if (pageNum == null || pageSize == null || pageNum < 0 || pageSize < 0) {
            pageNum = 0;
            pageSize = 10;
        }

        final Page<ArticleVo> page = this.findByPageWithAttr(userId, pageNum, pageSize);
        final List<ArticleVo> list = page.getResult();
        final List<Map<String, Object>> total = new ArrayList<>();
        // 是否登录
        final boolean isLogin = (UserUtils.checkLoginDead(request) != null);
        final ArticleLike t = new ArticleLike();
        t.setUserId(request.getHeader("userId"));
        final ArticleCollect t1 = new ArticleCollect();
        t1.setUserId(request.getHeader("userId"));

        if (list != null && list.size() > 0) {
            for (final ArticleVo a : list) {
                final Map<String, Object> m = this.article2Map1(a);
                if (isLogin) {
                    t.setArticleId(a.getId());
                    final ArticleLike al = this.likeDao.get(t);
                    if (al != null) {
                        m.put("isLike", al.getStatus() + "");
                    }
                    t1.setArticleId(a.getId());
                    final ArticleCollect ac = this.collectDao.get(t1);
                    if (ac != null) {
                        m.put("isCollect", ac.getStatus() + "");
                    }

                }
                total.add(m);
            }
        }
        return mapper.data(total).resultJson();
    }

    @Override
    public String collectList(HttpServletRequest request, String userId, Integer pageNum, Integer pageSize) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        // 是否登录
        final boolean isLogin = (UserUtils.checkLoginDead(request) != null);
        final Article article = new Article();
        article.setUserId(userId);
        if (pageNum == null || pageSize == null || pageNum < 0 || pageSize < 0) {
            pageNum = 0;
            pageSize = 10;
        }

        Page<ArticleVo> page = new Page<ArticleVo>();
        PageHelper.startPage(pageNum, pageSize);
        page = this.articleDao.findCollectList(userId);
        final List<ArticleVo> list = page.getResult();
        final List<Map<String, Object>> total = new ArrayList<>();

        final ArticleLike t = new ArticleLike();
        t.setUserId(request.getHeader("userId"));
        final ArticleCollect t1 = new ArticleCollect();
        t1.setUserId(request.getHeader("userId"));

        if (list != null && list.size() > 0) {
            for (final ArticleVo a : list) {
                final Map<String, Object> m = this.article2Map1(a);
                if (isLogin) {
                    t.setArticleId(a.getId());
                    final ArticleLike al = this.likeDao.get(t);
                    if (al != null) {
                        m.put("isLike", al.getStatus() + "");
                    }
                    t1.setArticleId(a.getId());
                    final ArticleCollect ac = this.collectDao.get(t1);
                    if (ac != null) {
                        m.put("isCollect", ac.getStatus() + "");
                    }

                }
                total.add(m);
            }
        }
        return mapper.data(total).resultJson();
    }

    private Page<ArticleVo> findByPageWithAttr(String userId, int pageNum, int pageSize) {
        Page<ArticleVo> page = new Page<ArticleVo>();
        PageHelper.startPage(pageNum, pageSize);
        page = (Page<ArticleVo>) this.articleDao.findByListWithAttr(userId);
        return page;
    }

    @Override
    public String addArticle(HttpServletRequest request, String title, String content, String userId, String token) {
        ResponseMapper mapper = ResponseMapper.createMapper();
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode())
                    .message("登录失效,请刷新登录1")
                    .resultJson();
        }
        final User user = (User) session.getAttribute(token);
        if (user == null) {
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode())
                    .message("登录失效,请刷新登录2")
                    .resultJson();
        }
        if (!userId.equals(user.getId())) {
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode())
                    .message("登录失效,请刷新登录3")
                    .resultJson();
        }
        return this.publish(userId, title, content);
    }

    @Override
    public String editArticle(HttpServletRequest request, String title, String content, String userId, String articleId,
            String token) {
        ResponseMapper mapper = ResponseMapper.createMapper();
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode())
                    .message("登录失效,请刷新登录1")
                    .resultJson();
        }
        final User user = (User) session.getAttribute(token);
        if (user == null) {
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode())
                    .message("登录失效,请刷新登录2")
                    .resultJson();
        }
        if (!userId.equals(user.getId())) {
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode())
                    .message("登录失效,请刷新登录3")
                    .resultJson();
        }
        return this.edit(userId, title, content, articleId);
    }

    private String edit(String userId, String title, String content, String articleId) {
        ResponseMapper mapper = ResponseMapper.createMapper();
        Article ar = this.get(articleId);
        if (ar == null || !userId.equals(ar.getUserId())) {
            return mapper.code(ResponseCode.NO_DATA.statusCode()).resultJson();
        }
        Article temp = new Article();
        temp.setTitle(title)
                .setContent(content)
                .setId(articleId);
        if (this.articleDao.update(temp) <= 0) {
            return mapper.code(ResponseCode.FAILED.statusCode()).resultJson();
        }
        return mapper.data(articleId).resultJson();
    }

    @Override
    public String addReadNum(HttpServletRequest request, String articleId) {
        final String ip = request.getRemoteHost();
        if (JedisUtils.get("user:login:" + ip) != null) {
            return ResponseMapper.createMapper().resultJson();
        }
        // 行级锁
        final ArticleAttr attr = this.attrDao.getForUpdate(articleId);
        final ArticleAttr temp = new ArticleAttr();
        temp.setId(attr.getId());
        temp.setArticleId(attr.getArticleId());
        temp.setReadNum(attr.getReadNum() + 1);
        this.attrDao.update(temp);
        JedisUtils.set("user:login:" + ip, temp.getReadNum().toString(), 60 * 10);
        return ResponseMapper.createMapper().data(temp.getReadNum()).resultJson();
    }

    @Autowired
    private ArticleLikeDao likeDao;

    @Override
    public String addLike(HttpServletRequest request, String articleId, Integer isLike) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        if (this.articleDao.isExist(articleId) < 1) {
            return mapper.code(ResponseCode.ARGS_ERROR.statusCode()).resultJson();
        }
        // 没登录 或失效
        if (UserUtils.checkLoginDead(request) == null) {
            if (isLike == 0) {
                this.addLikeNum(articleId, true);
            } else if (isLike == 1) {
                this.addLikeNum(articleId, false);
            }
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode()).resultJson();
        } else {
            final ArticleLike t = new ArticleLike();
            t.setUserId(request.getHeader("userId")).setArticleId(articleId);
            // 已经点过
            if (this.likeDao.isExist(t) > 0) {
                final ArticleLike like = this.likeDao.getForUpdate(t);
                // 取消点赞
                if (like.getStatus() == 1) {
                    t.setStatus(0);
                    this.addLikeNum(articleId, false);
                }
                // 进行点赞
                else {
                    t.setNum(like.getNum() + 1).setStatus(1);
                    this.addLikeNum(articleId, true);
                }
                if (this.likeDao.update(t) > 0) {
                    // 点赞的
                    if (like.getStatus() == 0) {
                        // 消息推送
                        this.sendMsg(request.getHeader("userId"), 0, articleId, 0, 2, null, null);
                    }
                }
            }
            // 没点过赞
            else {
                t.setNum(1);
                if (this.likeDao.insert(t) > 0) {
                    // 消息推送
                    this.sendMsg(request.getHeader("userId"), 0, articleId, 0, 2, null, null);
                }
                this.addLikeNum(articleId, true);
            }

        }
        return mapper.resultJson();

    }

    private void addLikeNum(String articleId, boolean flag) {
        final ArticleAttr attr = new ArticleAttr();
        final ArticleAttr at = this.attrDao.getForUpdate(articleId);
        attr.setArticleId(articleId);
        if (flag) {// 点赞
            attr.setLikeNum(at.getLikeNum() + 1);
        } else {
            attr.setLikeNum(at.getLikeNum() - 1);
        }
        this.attrDao.update(attr);
    }

    /*
     * isCollect 0取消收藏 1收藏
     */
    @Override
    public String addCollect(HttpServletRequest request, String articleId, Integer isCollect) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        if (this.articleDao.isExist(articleId) < 1) {
            return mapper.code(ResponseCode.ARGS_ERROR.statusCode()).resultJson();
        }
        final String userId = request.getHeader("userId");
        // 没登录 或失效
        if (UserUtils.checkLoginDead(request) == null) {
            if (isCollect == 0) {
                this.addCollectNum(articleId, userId, true);
            } else if (isCollect == 1) {
                this.addCollectNum(articleId, userId, false);
            }
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode()).resultJson();
        } else {
            final ArticleCollect t = new ArticleCollect();
            t.setUserId(userId).setArticleId(articleId);
            // 已经收藏
            if (this.collectDao.isExist(t) > 0) {
                final ArticleCollect co = this.collectDao.getForUpdate(t);
                // 取消收藏
                if (co.getStatus() == 1) {
                    t.setStatus(0);
                    this.addCollectNum(articleId, userId, false);
                }
                // 进行收藏
                else {
                    t.setStatus(1);
                    if (this.addCollectNum(articleId, userId, true) > 0) {
                        // 消息推送
                        this.sendMsg(userId, 0, articleId, 0, 3, null, null);
                    }
                }
                this.collectDao.update(t);
            }
            // 没收藏
            else {
                t.setId(IdGenerator.uuid());
                this.collectDao.insert(t);
                if (this.addCollectNum(articleId, userId, true) > 0) {
                    // 消息推送
                    this.sendMsg(userId, 0, articleId, 0, 3, null, null);
                }
            }

        }
        return mapper.resultJson();

    }

    private int addCollectNum(String articleId, String userId, boolean flag) {
        final ArticleAttr attr = new ArticleAttr();
        final ArticleAttr at = this.attrDao.getForUpdate(articleId);
        attr.setArticleId(articleId);
        // 收藏
        if (flag) {
            attr.setCollectNum(at.getCollectNum() + 1);
            try {
                this.userAttrDao.addNum(NumCountType.CollectNum.ordinal(), 1, userId);
            } catch (final Exception e) {
                // do nothing
            }
        } else {
            attr.setCollectNum(at.getCollectNum() - 1);
            try {
                this.userAttrDao.addNum(NumCountType.CollectNum.ordinal(), -1, userId);
            } catch (final Exception e) {
                // do nothing
            }
        }
        return this.attrDao.update(attr);
    }

    @Autowired
    private ArticleCommentDao arCommentDao;

    @Override
    public String comment(HttpServletRequest request, String articleId, String content) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        if (org.apache.commons.lang3.StringUtils.isBlank(content)) {
            return mapper.code(ResponseCode.ARGS_ERROR.statusCode()).resultJson();
        }
        final User user = UserUtils.checkLoginDead(request);
        if (user == null) {
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode()).resultJson();
        }
        final ArticleComment co = new ArticleComment();
        final Article ar = this.articleDao.getById(articleId);
        if (ar == null) {
            return mapper.code(ResponseCode.ARGS_ERROR.statusCode()).resultJson();
        }
        co.setArticleId(articleId)
                .setReplyerId(user.getId())
                .setContent(content)
                .setAuthorId(ar.getUserId())
                .setId(IdGenerator.uuid());
        // 为表情内容的设置
        this.arCommentDao.predo();
        if (this.arCommentDao.insert(co) > 0) {
            this.addCommentNum(articleId, true);
            // 别人评论的
            if (!co.getAuthorId().equals(co.getReplyerId())) {
                // 消息推送
                this.sendMsg(user.getId(), 0, articleId, 0, 1, content, null);
            }
        }
        final Map<String, String> map = new HashMap<>();
        map.put("replyerId", user.getId());
        map.put("replyerName", user.getNickname());
        map.put("replyerAvatar", user.getAvatar());
        map.put("content", content);
        map.put("createDate", TimeUtils.format(new Date(), "yyyy-MM-dd HH:mm"));
        return mapper.data(map).resultJson();
    }

    private void addCommentNum(String articleId, boolean flag) {
        final ArticleAttr attr = new ArticleAttr();
        final ArticleAttr at = this.attrDao.getForUpdate(articleId);
        attr.setArticleId(articleId);
        // 评论
        if (flag) {
            attr.setCommentNum(at.getCommentNum() + 1);
        }
        // 删除评论
        else {
            attr.setCommentNum(at.getCommentNum() - 1);
        }
        this.attrDao.update(attr);
    }

    @Override
    public String comments(HttpServletRequest request, String articleId, Integer pageNum) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        PageHelper.startPage(pageNum, 10);
        final Page<ArticleCommentVo> page = (Page<ArticleCommentVo>) this.arCommentDao.findList(articleId);
        final List<ArticleCommentVo> list = page.getResult();
        if (list == null || list.size() < 1) {
            return mapper.resultJson();
        }
        // 是否登录
        final boolean isLogin = (UserUtils.checkLoginDead(request) != null);
        final CommentLike t = new CommentLike();
        if (isLogin) {
            t.setUserId(request.getHeader("userId"));
        }

        Map<String, Object> map = null;
        final List<Map<String, Object>> total = new ArrayList<>();
        for (final ArticleCommentVo a : list) {
            map = MapleUtil.wrap(a)
                    .rename("id", "commentId")
                    .stick("createDate", TimeUtils.format(a.getCreateDate(), "yyyy-MM-dd HH:mm"))
                    .stick("isLike", "0")
                    .map();
            // map.put("commentId", a.getId());
            // map.put("num", a.getNum().toString());
            // map.put("replyerName", a.getReplyerName());
            // map.put("replyerId", a.getReplyerId());
            // map.put("replyerAvatar", a.getReplyerAvatar());
            // map.put("parentReplyerId", a.getParentReplyerId());
            // map.put("parentReplyerName", a.getParentReplyerName());
            // map.put("content", a.getContent());
            // map.put("createDate", TimeUtils.format(a.getCreateDate(), "yyyy-MM-dd
            // HH:mm"));
            // map.put("isLike", "0");
            if (isLogin) {
                t.setCommentId(a.getId());
                final CommentLike cl = this.arCommentDao.getLike(t);
                if (cl != null) {
                    map.put("isLike", cl.getStatus() + "");
                }
            }
            total.add(map);
        }
        return mapper.count(page.getTotal()).data(total).resultJson();
    }

    @Override
    public String newComments(HttpServletRequest request, String articleId) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        final List<ArticleCommentVo> list = this.arCommentDao.findNewComments(articleId);
        if (list == null || list.size() < 1) {
            return mapper.resultJson();
        }
        Map<String, Object> map = null;
        final List<Map<String, Object>> total = new ArrayList<>();
        // 是否登录
        final boolean isLogin = (UserUtils.checkLoginDead(request) != null);
        final CommentLike t = new CommentLike();
        if (isLogin) {
            t.setUserId(request.getHeader("userId"));
        }
        for (final ArticleCommentVo a : list) {
            map = MapleUtil.wrap(a)
                    .rename("id", "commentId")
                    .stick("createDate", TimeUtils.format(a.getCreateDate(), "yyyy-MM-dd HH:mm"))
                    .stick("isLike", "0")
                    .map();
            // map.put("commentId", a.getId());
            // map.put("num", a.getNum().toString());
            // map.put("replyerName", a.getReplyerName());
            // map.put("replyerId", a.getReplyerId());
            // map.put("replyerAvatar", a.getReplyerAvatar());
            // map.put("parentReplyerId", a.getParentReplyerId());
            // map.put("parentReplyerName", a.getParentReplyerName());
            // map.put("content", a.getContent());
            // map.put("createDate", TimeUtils.format(a.getCreateDate(), "yyyy-MM-dd
            // HH:mm"));
            // map.put("isLike", "0");
            if (isLogin) {
                t.setCommentId(a.getId());
                final CommentLike cl = this.arCommentDao.getLike(t);
                if (cl != null) {
                    map.put("isLike", cl.getStatus() + "");
                }
            }
            total.add(map);
        }

        return mapper.data(total).resultJson();
    }

    // =============================评论相关===========================//
    @Override
    public String addCommentLike(HttpServletRequest request, String commentId, Integer isLike) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        // 没登录 或失效
        if (UserUtils.checkLoginDead(request) == null) {
            if (isLike == 0) {
                this.addCommentLikeNum(commentId, true);
            } else if (isLike == 1) {
                this.addCommentLikeNum(commentId, false);
            }
            return mapper.code(ResponseCode.LOGIN_INVALIDATE.statusCode()).resultJson();
        } else {
            final CommentLike t = new CommentLike();
            t.setUserId(request.getHeader("userId")).setCommentId(commentId);
            // 已经点过
            if (this.arCommentDao.isLiked(t) > 0) {
                final CommentLike like = this.arCommentDao.getLikeForUpdate(t);
                // 取消点赞
                if (like.getStatus() == 1) {
                    t.setStatus(0);
                    this.addCommentLikeNum(commentId, false);
                }
                // 进行点赞
                else {
                    t.setStatus(1);
                    if (this.addCommentLikeNum(commentId, true) > 0) {
                        // 消息推送
                        this.sendMsg(request.getHeader("userId"), 0, commentId, 0, 6, null, null);
                    }
                }
                this.arCommentDao.updateLike(t);
            }
            // 没点过赞
            else {
                if (this.arCommentDao.insertLike(t) > 0) {
                    // 消息推送
                    this.sendMsg(request.getHeader("userId"), 0, commentId, 0, 6, null, null);
                }
                this.addCommentLikeNum(commentId, true);
            }

        }
        return mapper.resultJson();
    }

    private int addCommentLikeNum(String commentId, boolean flag) {
        final ArticleComment ac = this.arCommentDao.getForUpdate(commentId);
        final ArticleComment temp = new ArticleComment();
        temp.setId(commentId);
        if (flag) {// 点赞
            temp.setNum(ac.getNum() + 1);
        } else {
            temp.setNum(ac.getNum() - 1);
        }
        return this.arCommentDao.update(temp);
    }

    @Override
    public String latestOfUsers(HttpServletRequest request, String[] userIds) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        final List<ArticleVo> list = this.articleDao.findLatestOfUsers(userIds);
        if (list != null && list.size() > 0) {
            mapper.data(list);
        }
        return mapper.resultJson();
    }

    @Override
    public String search(HttpServletRequest request, String keyword) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        final Map<String, Object> map = ElasticUtils.searchWithCount(new String[] { "website" },
                new String[] { "article" }, 0, 10, keyword, new String[] { "title", "content" });
        return mapper.data(map).resultJson();
    }

    @Override
    public String synElastic(HttpServletRequest request, String password) {
        final ResponseMapper mapper = ResponseMapper.createMapper();
        Page<Article> page = new Page<>();
        if ("xiaoyu".equals(password)) {
            final int count = this.articleDao.count();
            // 分页同步 防止一次性取出量过大
            for (int i = 1; i <= (count + 50 - 1) / 50; i++) {
                PageHelper.startPage(i, 50, true);
                page = (Page<Article>) this.articleDao.findByList(new Article());
                if (page != null && page.getResult() != null && page.getResult().size() > 0) {
                    final List<Article> list = page.getResult();
                    final Map<String, String> jsonMap = new HashMap<>();
                    for (final Article a : list) {
                        jsonMap.put(a.getId(), JSON.toJSONString(a));
                        ElasticUtils.upsertList("website", "article", jsonMap);
                    }
                }
            }
            return mapper.message("同步成功").resultJson();
        }

        return mapper.message("同步失败").resultJson();
    }

}
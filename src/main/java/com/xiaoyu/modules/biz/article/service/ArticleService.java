/**
 * 不要因为走了很远就忘记当初出发的目的:whatever happened,be yourself
 */
package com.xiaoyu.modules.biz.article.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import com.github.pagehelper.Page;
import com.google.common.collect.Lists;
import com.xiaoyu.common.base.BaseService;
import com.xiaoyu.common.base.ResponseMapper;
import com.xiaoyu.common.base.ResultConstant;
import com.xiaoyu.common.utils.EhCacheUtil;
import com.xiaoyu.common.utils.IdGenerator;
import com.xiaoyu.modules.biz.article.dao.ArticleAttrDao;
import com.xiaoyu.modules.biz.article.dao.ArticleDao;
import com.xiaoyu.modules.biz.article.entity.Article;
import com.xiaoyu.modules.biz.article.entity.ArticleAttr;
import com.xiaoyu.modules.biz.article.service.api.IArticleService;
import com.xiaoyu.modules.biz.user.dao.UserDao;
import com.xiaoyu.modules.biz.user.entity.User;
import com.xiaoyu.modules.sys.constant.PageUrl;

/**
 * @author xiaoyu 2016年3月29日
 */
@Service
@Transactional(readOnly = true)
public class ArticleService extends BaseService<ArticleDao, Article> implements IArticleService {

	@Autowired
	private UserDao userDao;
	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private ArticleAttrDao attrDao;

	private Map<String, Object> article2Map(Article a) {
		Map<String, Object> map = new HashMap<>();
		map.put("articleId", a.getId());
		map.put("content", a.getContent());
		map.put("createDate", a.getCreateDate());
		map.put("readNum", a.getReadNum());
		map.put("title", a.getTitle());
		map.put("user", this.user2Map(this.userDao.getById(a.getUserId())));
		return map;
	}

	private Map<String, Object> article2Map1(Article a) {
		Map<String, Object> map = new HashMap<>();
		map.put("articleId", a.getId());
		map.put("content", a.getContent().length() > 100 ? a.getContent().substring(0, 99) : a.getContent());
		map.put("title", a.getTitle());
		map.put("user", this.user2Map(this.userDao.getById(a.getUserId())));
		return map;
	}

	private Map<String, Object> user2Map(User a) {
		Map<String, Object> map = new HashMap<>();
		if (a != null) {
			map.put("userId", a.getId());
			map.put("nickname", a.getNickname());
			map.put("createDate", a.getImg());
		}
		return map;
	}

	@Override
	public String detail(String articleId) {
		ResponseMapper mapper = ResponseMapper.createMapper();
		Article a = super.get(articleId);
		if (a == null) {
			return mapper.setCode(ResultConstant.NOT_DATA).setData(PageUrl.Not_Found).getResultJson();
		}
		return mapper.setData(this.article2Map(a)).getResultJson();
	}

	@Transactional(readOnly = false)
	public String publish(String userId, String content) {
		ResponseMapper mapper = ResponseMapper.createMapper();
		Article t = new Article();
		ArticleAttr attr = new ArticleAttr();
		Date date = new Date();
		t.setId(IdGenerator.uuid());
		t.setCreateDate(date);
		t.setUpdateDate(date);
		t.setContent(content);
		t.setUserId(userId);
		try {
			this.articleDao.insert(t);
			attr.setArticleId(t.getId());
			attr.setId(IdGenerator.uuid());
			attr.setCreateDate(date);
			attr.setUpdateDate(date);
			this.attrDao.insert(attr);
		} catch (RuntimeException e) {
			throw e;
		}
		return mapper.setData(t.getId()).getResultJson();
	}

	@Override
	public String hotList() {
		ResponseMapper mapper = ResponseMapper.createMapper();

		if (EhCacheUtil.IsExist("SystemCache")) {// 从缓存取
			@SuppressWarnings("unchecked")
			List<Object> total = (List<Object>) EhCacheUtil.get("SystemCache", "pageList");

			// model.addAttribute("list", total);
			if (total != null && total.size() > 0) {
				return mapper.setData(total).getResultJson();
				// return "article/articleList";
			}
		}
		Page<Article> page = this.findByPage(new Article(), 1, 12);
		List<Article> list = page.getResult();
		List<Object> total = new ArrayList<>();

		List<Map<String,Object>> childList1 = Lists.newArrayList();
		List<Map<String,Object>> childList2 = Lists.newArrayList();
		List<Map<String,Object>> childList3 = Lists.newArrayList();
		List<Map<String,Object>> childList4 = Lists.newArrayList();

		for (int i = 0; i < list.size(); i++) {
			if (i < 3) {
				childList1.add(this.article2Map1(list.get(i)));
			} else if (i > 2 && i < 6) {
				childList2.add(this.article2Map1(list.get(i)));
			} else if (i > 5 && i < 9) {
				childList3.add(this.article2Map1(list.get(i)));
			} else {
				childList4.add(this.article2Map1(list.get(i)));
			}
		}
		total.add(childList1);
		total.add(childList2);
		total.add(childList3);
		total.add(childList4);
		EhCacheUtil.put("SystemCache", "pageList", total);// 存入缓存
		//model.addAttribute("list", total);
		//return "article/articleList";
		return mapper.setData(total).getResultJson();
	}
}

/**
 * 不要因为走了很远就忘记当初出发的目的:whatever happened,be yourself
 */
package com.xiaoyu.modules.biz.article.vo;

import com.xiaoyu.common.base.BaseEntity;
import com.xiaoyu.modules.biz.user.vo.UserVo;

/**
 * @author xiaoyu 2016年3月29日
 */
public class ArticleVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String title;
    private String content;
    private String columnId;

    private UserVo user;
    private ArticleAttrVo attr;

    private String columnName;

    public String getColumnId() {
        return columnId;
    }

    public ArticleVo setColumnId(String columnId) {
        this.columnId = columnId;
        return this;
    }

    public String getColumnName() {
        return columnName;
    }

    public ArticleVo setColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public ArticleAttrVo getAttr() {
        return this.attr;
    }

    public UserVo getUser() {
        return this.user;
    }

    public ArticleVo setUser(UserVo user) {
        this.user = user;
        return this;
    }

    public ArticleVo setAttr(ArticleAttrVo attr) {
        this.attr = attr;
        return this;
    }

    public String getUserId() {
        return this.userId;
    }

    public ArticleVo setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getTitle() {
        return this.title;
    }

    public ArticleVo setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getContent() {
        return this.content;
    }

    public ArticleVo setContent(String content) {
        this.content = content;
        return this;
    }

}

var url = document.URL;
var userId = url.split('/')[4];
var $lock = true;
var $tabType = "all";
var $ajaxPromise1 = $.ajax({
    type : "get",
    async : true,
    url : '/api/v1/user/' + userId,
    success : function(data) {
        var obj = jQuery.parseJSON(data);
        if (obj.code != "0") {
            window.location.href = "/common/404";
            return false;
        }
        var $user = obj.data;
        if (!checkNull($user)) {
            setTitle($user.nickname + '-个人主页');
            var $userPanel = $(".panel");
            if (checkNull($user.avatar)) {
                $user.avatar = imgHead + 'common/avatar.png';
            }
            if (checkNull($user.background)) {
                $user.background = 'common/4.jpg';
            }
            $(".panel").css("background", 'url(' + imgHead + $user.background + ') no-repeat 0% 70%/cover');
            $userPanel.find("img").attr("src", imgHead + $user.avatar);
            $userPanel.find("img").attr("id", $user.userId);
            $userPanel.find(".nickname_panel").html($user.nickname);
            $userPanel.find(".des_panel").html($user.signature);

        }
    }
});
var before = function(xhr) {
    var userInfo = jQuery.parseJSON($.session.get("user"));
    if (!checkNull(userInfo)) {
        xhr.setRequestHeader('token', userInfo.token);
        xhr.setRequestHeader('userId', userInfo.userId);
    }
};
var handleAll = function(obj,pageNum) {
    var userInfo = jQuery.parseJSON($.session.get("user"));
    var arHtml = "";
    if (obj.code != "0" || checkNull(obj.data) || obj.data.length <= 0) {
        if(pageNum == 1) {
            $(".list-group").html(blankPage);
        }
        return;
    }
    $.each(obj.data, function(index, ar) {
        arHtml += '<li class="list-group-item"   id="' + ar.articleId + '">';
        arHtml += '<label style="color:#cd5c5c;">' + ar.title + '</label>';

        arHtml += '<p class="group_item_p">' + ar.content + '...' + '</p>';
        if (userInfo != null && userId == userInfo.userId) {
            arHtml += '<div class="group_item_edit_part">' + '<label  class="edit_part_label edit_label">编辑</label>'
                    + '<label  class="edit_part_label remove_label">删除</label>' + '</div>';
        }
        arHtml += '<div class="comment_bar"><div class="bar_part">';
        if (ar.isLike == "1") {
            arHtml += '<i class="icon_like" style="color:#fd4d4d;" data-like="1"></i>';
        } else {
            arHtml += '<i class="icon_like" data-like="0"></i>';
        }

        arHtml += '<label style="margin: 2px;">' + ar.attr.likeNum + '</label></div>';
        arHtml += '<div class="bar_part">';
        arHtml += '<i class="icon_comment_alt"></i>';
        arHtml += '<label style="margin: 2px;">' + ar.attr.commentNum + '</label></div>';
        arHtml += '<div class="bar_part">';
        if (ar.isCollect == "1") {
            arHtml += '<i class="icon_heart_alt" style="color:#fd4d4d;" data-heart="1"></i>';
        } else {
            arHtml += '<i class="icon_heart_alt" data-heart="0"></i>';
        }
        arHtml += '<label style="margin: 2px;">' + ar.attr.collectNum + '</label></div>';
        arHtml += '</div>';
        arHtml += '</li>';

    });
    if(pageNum == 1) {
        $(".list-group").html(arHtml);
    } else {
        $(".list-group").append(arHtml);
    }
    
    $(".list-group").attr("id", "list-all");
    $.session.set("pr-al-0", $(".list-group").html(), 10 * 60);

    return true;

};

var handleCollected = function(data,pageNum) {
    var $userInfo = jQuery.parseJSON($.session.get("user"));
    var isSelf = false;
    if (!checkNull($userInfo) && $userInfo.userId == userId) {
        isSelf = true;
    }

    var obj = jQuery.parseJSON(data);
    if (obj.code != "0" || checkNull(obj.data) || obj.data.length <= 0) {
        if(pageNum == 1) {
            $(".list-group").html(blankPage);
        }
        return;
    }
    var arHtml = "";
    $.each(obj.data, function(index, ar) {
        arHtml += '<div class="collect_list_item">' + '<div class="collect_ar_info">' + '<dt>来自:</dt>' + '<dt id="' + ar.user.userId
                + '" class="collect_ar_author">' + ar.user.nickname + '</dt>';
        if (isSelf) {
            arHtml += '<dt id="' + ar.articleId + '" class="collect_ar_unlove">取消收藏</dt>';
        }
        arHtml += '</div>' + '<div style="text-align: center;">' + '	<label class="collect_ar_title" ar-id="' + ar.articleId + '">' + ar.title
                + '</label>' + '<div style="color: #9e9e9e;">' + '	<label id="collect_ar_like">' + ar.attr.likeNum + '个赞</label> '
                + '<label id="collect_ar_com">' + ar.attr.commentNum + '个评论&nbsp;</label>' + '<label id="collect_ar_col">' + ar.attr.collectNum
                + '个收藏</label>' + '</div>' + '</div>' + '</div>';
    });
    if ($(".list-group").attr("id") == "list-collected") {
        if(pageNum == 1) {
            $(".list-group").html(arHtml);
        } else {
            $(".list-group").append(arHtml);
        }
    }
    $.session.set("pr-cd-1", $(".list-group").html(), 10 * 60);

    return true;
};
var handleFollowing = function(data,pageNum) {
    var obj = jQuery.parseJSON(data);
    if (obj.code != "0" || checkNull(obj.data) || obj.data.length <= 0) {
        if(pageNum == 1) {
            $(".list-group").html(blankPage);
        }
        return;
    }
    var arHtml = "";
    var $ht = '<div class="love_list">';
    $ht += '<div class="page_arrow">';
    $ht += '<i class="arrow_carrot-left" data-page="1"></i>';
    $ht += '</div>';
    var $l = '<div class="love_model">';
    var $r = '<div class="love_model">';
    var ids = new Array();
    $.each(obj.data, function(index, u) {
        ids[index] = u.userId;
        if (index < 5) {
            $l += '<img id="' + u.userId + '" src="' + imgHead + u.userAvatar + '" class="avatar small compact" title="' + u.userName + '" />';
        }
        if (index == 4) {
            $l += '</div>';
        }
        if (index > 4 && index < 10) {
            $r += '<img id="' + u.userId + '" src="' + imgHead + u.userAvatar + '" class="avatar small compact" title="' + u.userName + '" />';
        }
    });

    $r += '</div>';
    $r += '</div>';
    $r += '<div class="page_arrow" style="position: absolute;right: 0"><i class="arrow_carrot-right" data-page="2"></i></div>';
    $ht += $l;
    $ht += $r;
    $ht += '</div>';
    $ht += '<div class="love_ar_list">';
    if(ids.length > 0) {
        $.ajax({
            type : "post",
            async : false,
            url : '/api/v1/article/latest',
            data : {
                userId : ids
            },
            beforeSend : function(xhr) {
                return before(xhr);
            },
            success : function(data) {
                var obj = jQuery.parseJSON(data);
                if (obj.code != '0' || checkNull(obj.data) || obj.data.length <= 0) {
                    return false;
                }
                $.each(obj.data, function(index, ar) {
                    $ht += '<dl>';
                    $ht += '<div class="love_ar_model">';
                    $ht += '	<span class="item_userinfo">';
                    $ht += '<img class="avatar small" img-type="avatar" src="' + imgHead + ar.user.avatar + '" id="' + ar.userId + '">';
                    $ht += '<label class="item_desc">' + ar.user.signature + '</label></span>';
                    $ht += '<dt class="item_username">' + ar.user.nickname + '</dt>';
                    $ht += '<label style="font-size: 13px;">最近发表:</label>';
                    $ht += '<div class="font_center" >';
                    $ht += '	<label style="cursor:pointer;width:100%;" id="' + ar.id + '">"' + ar.title + '"</label>';
                    $ht += '	<time style="font-size: 12px;">' + ar.createDate + '</time>';
                    $ht += '</div>';
                    $ht += '</div>';
                    $ht += '</dl>';
                });
            }
        });
    }
    
    $ht += '</div>';
    if(pageNum == 1) {
        $(".list-group").html($ht);
    } else {
        $(".list-group").append($ht);
    }

    return true;
};

var getSelfList = function(urlType, pageNum, pageSize) {
    var $url;
    if ("all" == urlType) {
        $url = '/api/v1/article/list';
    } else if ("collected" == urlType) {
        $url = '/api/v1/article/list/collect';
    } else if ("following" == urlType) {
        $url = '/api/v1/user/following';
    }
    var $all = $.ajax({
        type : "get",
        async : true,
        cache : false,
        url : $url,
        data : {
            userId : userId
        },
        beforeSend : function(xhr) {
            xhr.setRequestHeader('pageNum', pageNum);
            xhr.setRequestHeader('pageSize', pageSize);
            return before(xhr);
        },
        success : function(data) {
            $lock = false;
            var obj = jQuery.parseJSON(data);
            if (pageNum == 1) {
                if (obj.code != '0') {
                    $(".list-group").html(blankPage);
                    return;
                } else if (checkNull(obj.data) && obj.data.length <= 0) {
                    $(".list-group").html(blankPage);
                    return;
                } else {
                    $(".list-group").html("");
                }
            }
            if ("all" == urlType) {
                return handleAll(obj,pageNum);
            } else if ("collected" == urlType) {
                return handleCollected(data,pageNum);
            } else if ("following" == urlType) {
                return handleFollowing(data,pageNum);
            }

        }
    });
    $all.promise().done(function() {
    });
};

$.ajax({
    type : "get",
    async : true,
    url : '/api/v1/user/commonNums',
    data : {
        userId : userId
    },
    success : function(data) {
        var obj = jQuery.parseJSON(data);
        if (obj.code != '0' || checkNull(obj.data) || obj.data.length <= 0) {
            return false;
        }
        var $data = obj.data;
        $(".ar_number").html($data.articleNum);
        $(".co_number").html($data.collectNum);
        $(".fo_number").html($data.followerNum);
    }
});
var removeAllCache = function() {
    $.session.remove("pr-al-0");
    $.session.remove("pr-cd-1");
};
$(document).ready(function() {
    $ajaxPromise1.promise().done(function() {
    });
    var $all_pageNum = 1;
    var $collected_pageNum = 1;
    var $following_pageNum = 1;
    getSelfList("all", $all_pageNum, 12);
    // tab page
    $(".tab_ul").on('click', 'li', function() {
        var $selected = $(this);
        $.each($selected.siblings(), function(i, v) {
            $(v).removeClass('li_active');
        });
        $selected.addClass('li_active');
        switch ($selected.attr('data-select')) {
            case '0' :
                $tabType = "all";
                var $ck_al = $.session.get("pr-al-0");
                if (!checkNull($ck_al) && $ck_al != 'null') {
                    $(".list-group").html($ck_al);
                } else {
                    getSelfList("all", $all_pageNum, 12);
                }
                $(".list-group").attr("id", "list-all");
                break;
            case '1' :
                $tabType = "collected";
                var $pr_cd = $.session.get("pr-cd-1");
                if (!checkNull($pr_cd) && $pr_cd != 'null') {
                    $(".list-group").html($pr_cd);
                } else {
                    getSelfList("collected", $collected_pageNum, 12);
                }
                $(".list-group").attr("id", "list-collected");
                break;
            case '2' :
                $tabType = "following";
                getSelfList("following", $following_pageNum, 12);
                $(".list-group").attr("id", "list-following");
                break;
        }
    });

    $(".list-group").delegate(".icon_comment_alt", "click", function() {
        var $icon = $(this);
        var elem = $icon.parent().parent().parent();
        window.location.href = "/article/" + elem.attr("id") + "/comments"

    });
    $(".list-group").delegate(".font_center label", "click", function() {
        var elem = $(this);
        window.location.href = '/article/' + elem.attr("id");
    });
    $(".list-group").delegate(".icon_like", "click", function() {
        var elem = $(this).parent().parent().parent();
        var $icon = $(this);
        var $next = $icon.next();
        var num = $next.html();
        var $isLike;
        if ($icon.attr('data-like') == '0') {
            $icon.css("color", "#fd4d4d");
            $icon.attr("data-like", "1");
            $next.html(num - (-1));
            $isLike = 0;
        } else if ($icon.attr('data-like') == '1') {
            $icon.css("color", "#a7a7a7");
            $icon.attr("data-like", "0");
            $next.html(num - 1);
            $isLike = 1;
        }
        $.ajax({
            type : "post",
            async : true,
            url : '/api/v1/article/like',
            data : {
                articleId : elem.attr("id"),
                isLike : $isLike
            },
            beforeSend : function(xhr) {
                return before(xhr);

            },
            success : function(data) {
                var obj = jQuery.parseJSON(data);
                if (obj.code == "20001") {
                    console.log("未登录");
                    $.session.remove("user");
                }
                return true;
            },
            error : function(data) {
                return false;
            }
        });
        removeAllCache();
    });

    $(".list-group").delegate("p", "click", function() {
        var $icon = $(this);
        var elem = $icon.parent();
        window.location.href = '/article/' + elem.attr("id");
    })
    $(".list-group").delegate(".collect_ar_title", "click", function() {
        var $icon = $(this);
        window.location.href = '/article/' + $icon.attr("ar-id");
    });
    $(".list-group").delegate(".collect_ar_unlove", "click", function() {
        var $userInfo = jQuery.parseJSON($.session.get("user"));
        if (checkNull($userInfo)) {
            window.location.href = "/login";
            return false;
        }
        var elem = $(this);
        var item = elem.parent().parent();
        item.css({
            "opacity" : "0",
            "transition" : "opacity 2s",
        });

        $.ajax({
            type : "post",
            async : true,
            url : '/api/v1/article/collect',
            data : {
                articleId : elem.attr("id"),
                isCollect : 0
            },
            beforeSend : function(xhr) {
                return before(xhr);
            },
            success : function(data) {
                if(checkNull(data)) {
                    return;
                }
                var obj = jQuery.parseJSON(data);
                if (obj.code == "20001") {
                    console.log("未登录");
                    $.session.remove("user");
                    window.location.href = "/login";
                    return false;
                } else if (obj.code == '0') {
                    setTimeout(function() {
                        item.css({
                            "display" : "none"
                        });
                    }, 1000);
                }

                return true;
            },
            error : function(data) {
                item.css({
                    "opacity" : "1"
                });
                return false;
            }
        });
        removeAllCache();
    });
    $(".list-group").delegate(".compact", "click", function() {
        var $elem = $(this);
        window.location.href = "/user/" + $elem.attr('id');
    });
    $(".list-group").delegate(".icon_heart_alt", "click", function() {
        var $userInfo = jQuery.parseJSON($.session.get("user"));
        if (checkNull($userInfo)) {
            window.location.href = "/login";
            return false;
        }
        var $icon = $(this);
        var elem = $icon.parent().parent().parent();
        var $next = $icon.next();
        var num = $next.html();
        var $isCollect;
        if ($icon.attr('data-heart') == '0') {
            $icon.css("color", "#fd4d4d");
            $icon.attr("data-heart", "1");
            $next.html(num - (-1));
            $isCollect = 0;
        } else if ($icon.attr('data-heart') == '1') {
            $icon.css("color", "#a7a7a7");
            $icon.attr("data-heart", "0");
            $next.html(num - 1);
            $isCollect = 1;
        }
        $.ajax({
            type : "post",
            async : true,
            url : '/api/v1/article/collect',
            data : {
                articleId : elem.attr("id"),
                isCollect : $isCollect
            },
            beforeSend : function(xhr) {
                return before(xhr);
            },
            success : function(data) {
                if(checkNull(data)) {
                    return;
                }
                var obj = jQuery.parseJSON(data);
                if (obj.code == "20001") {
                    console.log("未登录");
                    $.session.remove("user");
                    window.location.href = "/login";
                    return false;
                }
                return true;
            },
            error : function(data) {
                return false;
            }
        });
        removeAllCache();
    });

    $(".list-group").delegate(".edit_label", "click", function() {
        var $ar = $(this).parent().parent();
        window.location.href = "/article/edit/" + $ar.attr("id");
    });
    $("#login").bind("click", function() {
        gotoLogin('/user/' + userId);
    });
    var $userInfo = jQuery.parseJSON($.session.get("user"));
    var $thisUserId = userId;
    if (!checkNull($userInfo) && $userInfo.userId == userId) {
        if (isPC()) {
            $(".panel").hover(function() {
                var $edit = $(".panel").find('label')[0];
                if (!checkNull($edit)) {
                    $($edit).css("display", "initial");
                    return;
                }
                var $html = '<label style="cursor:pointer;float:left;padding:5px;color:#d64444;">编辑资料</label>';
                $(".panel").prepend($html);
                $(".panel").find('label').on("click", function() {
                    window.location.href = "/user/edit";
                });
            }, function() {
                var $edit = $(".panel").find('label')[0];
                $($edit).css("display", "none");
            });
        } else {
            $(".avatar").on("click", function() {
                window.location.href = "/user/edit";
            });
        }

    }

    // 滚动事件触发
    window.onscroll = function() {
        if (getScrollTop() + getClientHeight() == getScrollHeight()) {
            if (!$lock) {
                $lock = true;
                $(".loading").css("visibility", "visible");
                setTimeout(function() {
                    if ($tabType == "all") {
                        getSelfList($tabType, ++$all_pageNum, 12);
                    } else if ($tabType == "collected") {
                        getSelfList($tabType, ++$collected_pageNum, 12);
                    } else if ($tabType == "following") {
                        getSelfList($tabType, $following_pageNum, 12);
                    }
                    $(".loading").css("visibility", "hidden");
                }, 50);
            }
        }
    }
});
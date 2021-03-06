var url = document.URL;
var word = url.substring(url.lastIndexOf('search?keyword=') + 15);
setTitle("搜索-" + decodeURI(word));
var $searchPromise = $.ajax({
    cache : false,
    type : "get",
    async : true,
    url : '/api/v1/article/search?keyword=' + word,
    success : function(data) {
        var obj = jQuery.parseJSON(data);
        var $sp1 = $(".co_num").find("span")[0];
        var $sp2 = $(".co_num").find("span")[1];
        if (checkNull(obj) || checkNull(obj.data.result)) {
            $(".co_list").html(blankPage);
            $sp1.innerText = '搜索' + '"' + decodeURI(word) + '"' + '相关结果';
            $sp2.innerText = "0篇";
            return false;
        }
        var result = obj.data.result;
        $sp1.innerText = '搜索' + '"' + decodeURI(word) + '"' + '相关结果';
        $sp2.innerText = (checkNull(obj.data.count) ? 0 : obj.data.count) + "篇";
        var arHtml = "";
        $.each(result, function(index, ar) {
            arHtml += '<li class="co_item"   id="' + ar.id + '">';
            arHtml += '<label>' + ar.title + '</label>';
            arHtml += '<p class="item_up" style="cursor:pointer;">' + ar.content.substring(0, 200) + '...' + '</p>';
            arHtml += '</li>';
        });
        $(".co_list").html(arHtml);
        $(".co_item").on("click", function() {
            var $item = $(this);
            window.location.href = "/article/" + $item.attr('id');
        });
        return true;
    }
});

$(document).ready(function() {
    $("#login").bind("click", function() {
        gotoLogin('/api/v1/article/search?keyword=' + word);
    });
    $searchPromise.promise().done(function() {
    });
});
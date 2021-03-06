function publish() {
    var tip = "";
    var userInfo = jQuery.parseJSON($.session.get("user"));
    if (checkNull(userInfo)) {
        tip = "登录后再来写写吧";
        $('.tooltip').jBox('Tooltip', {
            content : tip,
            pointer : false,
            animation : 'zoomIn',
            closeOnClick : 'body',
            target : $(".btn")
        }).open();
        return false;
    }
    var content = $("#articleContent");
    var title = $(".note_input");
    if (checkNull(title.val())) {
        tip = "先写个标题吧"
        $('.tooltip').jBox('Tooltip', {
            content : tip,
            pointer : false,
            animation : 'zoomIn',
            closeOnClick : 'body',
            target : $(".btn")
        }).open();
        return false;
    }
    if (checkNull(content.val())) {
        tip = "不如先写几个字吧"
        $('.tooltip').jBox('Tooltip', {
            content : tip,
            pointer : false,
            animation : 'zoomIn',
            closeOnClick : 'body',
            target : $(".btn")
        }).open();
        return false;
    }
    $("#publish").attr("disabled","disabled");
    $.ajax({
        type : "post",
        url : "/api/v1/article/add",
        async : true,
        data : {
            content : content.val(),
            title : title.val()
        },
        beforeSend : function(xhr) {
            xhr.setRequestHeader('token', userInfo.token);
            xhr.setRequestHeader('userId', userInfo.userId);
        },
        error : function(data) {
            $("#publish").removeAttr("disabled");
            tip = "没成功,是不是没网啊"
            $('.tooltip').jBox('Tooltip', {
                content : tip,
                pointer : false,
                animation : 'zoomIn',
                closeOnClick : 'body',
                target : $(".btn")
            }).open();
            return false;
        },
        success : function(data) {
            $("#publish").removeAttr("disabled");
            var jsonObj = jQuery.parseJSON(data);
            if (jsonObj.code == '0') {
                window.location.href = "/article/" + jsonObj.data;
            } else if (jsonObj.code = '20001') {
                $.session.remove('user');
                $('.tooltip').jBox('Tooltip', {
                    content : jsonObj.message,
                    pointer : false,
                    animation : 'zoomIn',
                    closeOnClick : 'body',
                    target : $(".btn")
                }).open();

            } else {
                $('.tooltip').jBox('Tooltip', {
                    content : jsonObj.message,
                    pointer : false,
                    animation : 'zoomIn',
                    closeOnClick : 'body',
                    target : $(".btn")
                }).open();
            }
            return true;
        }
    });
}
$(document).ready(function() {
    $("#publish").bind("click", function() {
        publish();
    });

});
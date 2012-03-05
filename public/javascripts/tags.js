//$(document).ready(function(){
//	$('.tags').hover(function(){
//		$('.tag-cloud').toggle();
//	});
//});

$(document).ready(function(){
	$('.tag-link').each(function(){
		$(this).css({"font-size": ($(this).attr('name') < 10) ? $(this).attr('name')*200/10 + 100 + "%" : "300%",
					 "color": "rgb(" + Math.round(($(this).attr('name')*200/10+65)) + "," + Math.round(($(this).attr('name')*200/10+55)) + "," + Math.round(($(this).attr('name')*200/10+55)) + ")"});
	});
});
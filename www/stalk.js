var STALK_conf = {
	httpUrl     : "http://www.stalk.io",
	appUrl      : "http://www.stalk.io:8080",
	serverInfo  : {},
	divName     : "STALK_SCREEN",
	refer       : "",
	user        : {},
	isLogined   : false,
	sock        : {},
	socketId    : "",
	currentUserCnt : 0,
    isEmptyMessage : true,


	defaultCloseWidth   : '100px',
	defaultOpenWidth    : '300px',
	defaultMaxOpenWidth : '500px'
};

var STALK_utils = {

    getHashCode : function (s) {
        var hash = 0;
        if (s.length === 0) return hash;
        for (var i = 0; i < s.length; i++) {
            var char1 = s.charCodeAt(i);
            hash = ((hash<<5)-hash)+char1;
            hash = hash & hash;
        }
        return hash;
    },

    loadScript :  function(url, callback){
        var script = document.createElement("script");
        script.type = "text/javascript";
        if (script.readyState){  //IE
            script.onreadystatechange = function(){
                if (script.readyState == "loaded" ||
                        script.readyState == "complete"){
                    //script.onreadystatechange = null;
                    callback();
                }
            };
        } else {  //Others
            script.onload = function(){
                callback();
            };
        }
        script.src = url;
        document.getElementsByTagName("head")[0].appendChild(script);
    },
	
	loadCss : function (url) {
		var link = document.createElement('link');
		link.type = 'text/css';
		link.rel = 'stylesheet';
		link.href = url;
		document.getElementsByTagName('head')[0].appendChild(link);
		return link;
	},
	
    loadJson : function(url, callbackStr){

        var script = document.createElement("script");
        // Add script object attributes
        script.type     = "text/javascript";
        script.charset  = "utf-8";
        script.id       = this.getHashCode(url);

        if (script.readyState){  //IE
            script.onreadystatechange = function(){
                if (script.readyState == "loaded" ||
                        script.readyState == "complete"){
                    //script.onreadystatechange = null;
                    // DO Something?
                }
            };
        } else {  //Others
            script.onload = function(){
               // DO Something?
            };
        }
        script.src = url + '&callback='+callbackStr+'&_noCacheIE=' + (new Date()).getTime();
        
        //document.getElementsByTagName("head").item(0).removeChild(script);
        document.getElementsByTagName("head")[0].appendChild(script);

    },
    
    setUserInfo : function(userInfo)
	{
		//var date = new Date();
		//date.setDate(date.getDate() + 10);
		document.cookie = 'STALK_USER=' + escape(JSON.stringify(userInfo)) + ';path=/' 
		//';expires=' + date.toGMTString()+';path=/';
	},
	
	delUserInfo : function() {
		var date = new Date(); 
		var validity = -1;
		date.setDate(date.getDate() + validity);
		document.cookie = "STALK_USER=;expires=" + date.toGMTString()+';path=/';
	},
	getUserInfo : function() {
		var allcookies = document.cookie;
		var cookies = allcookies.split("; ");
		for ( var i = 0; i < cookies.length; i++) {
			var keyValues = cookies[i].split("=");
			if (keyValues[0] == "STALK_USER") {
				
				return JSON.parse(unescape(keyValues[1]));
			}
		}
		return {};
	},
	userLog : function() {
		var allcookies = document.cookie;
		var cookies = allcookies.split("; ");
		for ( var i = 0; i < cookies.length; i++) {
			var keyValues = cookies[i].split("=");
			if (keyValues[0] == "STALK") {
				//console.log(unescape(keyValues[1]));
			}
		}
		return {};
	}
};




var STALK_window = {
    
    rootDivName     : '',
    imageServer     : '',
    textareaHeight  : -1,

	divCloseWidth	: STALK_conf.defaultCloseWidth,  //'100px',
	divOpenWidth	: STALK_conf.defaultOpenWidth,   //'300px',
	divMaxOpenWidth	: STALK_conf.defaultMaxOpenWidth,//'500px',
	currentSize : 'MIN',

    isLogined       : false,
    
    blinkTimeout : '',
    eventType : '',
    pageSize : {},
    
    hasClass : function(el, val) {
       var pattern = new RegExp("(^|\\s)" + val + "(\\s|$)");
    	 return pattern.test(el.className);
    },
    addClass : function(ele, cls) {
        if (!this.hasClass(ele, cls)) ele.className += " " + cls;
    },
    removeClass : function(ele, cls) {
        if (this.hasClass(ele, cls)) {
            var reg = new RegExp('(\\s|^)' + cls + '(\\s|$)');
            ele.className = ele.className.replace(reg, ' ');
        }
    },
    getBodySize : function(){
	var w = window,
		d = document,
		e = d.documentElement,
		g = d.getElementsByTagName('body')[0],
		x = w.innerWidth || e.clientWidth || g.clientWidth,
		y = w.innerHeight|| e.clientHeight|| g.clientHeight;
	
	return {width:x, height:y};
    },
    getStyle : function(el, cssprop){
		if (el.currentStyle) //IE
			return el.currentStyle[cssprop];
		else if (document.defaultView && document.defaultView.getComputedStyle) //Firefox
			return document.defaultView.getComputedStyle(el, "")[cssprop];
		else //try and get inline style
			return el.style[cssprop];
	},
    initWin : function(rootDivName, imageServer){
	this.pageSize = this.getBodySize();
     
        this.rootDivName = rootDivName;
        this.imageServer = imageServer;
		

		STALK_utils.loadCss(this.imageServer+'/stalk.css');

        var div_root = document.getElementById(rootDivName);
		
        if(div_root === null){
            	div_root = document.createElement('div');
            	div_root.id = rootDivName;
		div_root.style.bottom = '0px';
		if(this.pageSize.width <= 980 ){
			div_root.style.right = '0px';
			//div_root.style.width = this.pageSize.width+'px';//'100%';
			div_root.style.width = this.divOpenWidth;
		}else{
			div_root.style.right = '100px';
			div_root.style.width = this.divOpenWidth;
		}
            this.addClass(div_root, "stalk");
            document.getElementsByTagName('body')[0].appendChild(div_root);
        }
		
		this.divOpenWidth = this.getStyle(div_root, 'width');
		
        div_root.innerHTML = 
		'<div id="'+rootDivName+'_head" onclick="javascript:return STALK_window.toggleChatBoxGrowth();" class="stalk_head" >'+
		'<div id="'+rootDivName+'_title" class="stalk_title"></div><div id="'+rootDivName+'_options" class="stalk_options"></div><div id="'+rootDivName+'_size" class="stalk_options"></div><br clear="all"></div>'+
		'<div id="'+rootDivName+'_content" class="stalk_content"></div>'+
		'<div id="'+rootDivName+'_input" class="stalk_input"><textarea id="'+rootDivName+'_textarea" class="stalk_textarea" onkeydown="javascript:return STALK_window.inputChatMessage(event,this);" ></textarea></div>'+
        '<div id="'+rootDivName+'_login" class="stalk_login"><span align=right>CONNECT<br/>WITH</span> '+
        '<a href="#" class="stalk_tooltip" title="login with facebook" onclick="return !window.open(STALK.getOauthUrl(\'facebook\'),\'STALK_OAUTH\',\'menubar=no,location=no,resizable=yes,scrollbars=yes,status=yes,width=350,height=350\')" target="_blank"><img src="'+this.imageServer+'/img/facebook.png" class="stalk_snsbtn" /></a>&nbsp;'+
        '<a href="#" class="stalk_tooltip" title="login with twitter" onclick="return !window.open(STALK.getOauthUrl(\'twitter\'),\'STALK_OAUTH\',\'menubar=no,location=no,resizable=yes,scrollbars=yes,status=yes,width=700,height=450\')" target="_blank"><img src="'+this.imageServer+'/img/twitter.png" class="stalk_snsbtn" /></a>&nbsp;'+
        '<a href="#" class="stalk_tooltip" title="login with google+" onclick="return !window.open(STALK.getOauthUrl(\'googleplus\'),\'STALK_OAUTH\',\'menubar=no,location=no,resizable=yes,scrollbars=yes,status=yes,width=800,height=450\')" target="_blank"><img src="'+this.imageServer+'/img/google.png" class="stalk_snsbtn" /></a>&nbsp;'+
        //'<div id="'+rootDivName+'_info" class="stalk_info"><a href="http://www.stalk.io" target="_blank">by stalk.io</a></div>' +
        '</div>'+
	'<div id="'+rootDivName+'_sitelink" class="stalk_sitelink"><a href="http://stalk.io" target="_blank">stalk.io</a></div>';
        
        
        var div_content = document.getElementById(rootDivName+'_content');
        var div_login = document.getElementById(rootDivName+'_login');
        var div_input = document.getElementById(rootDivName+'_input');
        
        div_root.onclick = function(){
            
            if(div_content.style.display != 'none'){
                
                if(div_input.style.display  != 'none'){
                    var div_textarea = document.getElementById(rootDivName+'_textarea');
                    div_textarea.focus();
                    div_textarea.value = div_textarea.value;
                }
            }
        }; 
        
        //div_root.style.display = 'block';
        div_root.style.display = 'none';

	// pageSize 20130922
	if(this.pageSize.width <= 980 ){
            document.getElementById(this.rootDivName+'_sitelink').style.right = '10px';
	}else{
            document.getElementById(this.rootDivName+'_sitelink').style.right = '110px';
	}
        
        this.toggleChatBoxGrowth();

        

    },

    showRootDiv : function(isShow) {
        var div_root = document.getElementById(this.rootDivName);
        if(isShow){
            div_root.style.display = 'block';
        }else{
            div_root.style.display = 'none';
        }
    },
    
    isShowRootDiv : function() {
        var div_root = document.getElementById(this.rootDivName);
        if(div_root.style.display == 'block'){
            return true;
        }else{
        	return false;
        }
    },
    
    blinkHeader : function(isDone){
    	if(isDone){
    		clearInterval(this.blinkTimeout);
			var titleDivForBlick = document.getElementById(this.rootDivName+'_head')
    		this.removeClass(titleDivForBlick, 'stalk_blink');
    	}else{
    		clearInterval(this.blinkTimeout);
    		this.blinkTimeout = 
	    		setInterval(function(){ 
	    			var titleDivForBlick = document.getElementById(STALK_window.rootDivName+'_head')
	    			if(STALK_window.hasClass(titleDivForBlick, 'stalk_blink')){
	    				STALK_window.removeClass(titleDivForBlick, 'stalk_blink');
	    			}else{
	    				STALK_window.addClass(titleDivForBlick, 'stalk_blink');
	    			}
	    		},1000);
    	}
    },
    
    inputChatMessage : function(event,chatboxtextarea) {
    	
    	this.blinkHeader(true);
    	
        if(event.keyCode == 13 && !event.shiftKey) {
            
            if(event.preventDefault) {
                event.preventDefault();   
            }else{
                event.returnValue = false;
            }

            var message = chatboxtextarea.value;
            message = message.replace(/^\s+|\s+$/g,"");
            
            if(message.length > 0){
                
            	STALK.sendMessage(encodeURIComponent(message));
                    
                chatboxtextarea.value = '';
                //chatboxtextarea.focus();
                chatboxtextarea.style.height = '30px';                    

	            return false;
        	}
        }
        
        var adjustedHeight = chatboxtextarea.clientHeight;
        var maxHeight = 94;
        
        if (maxHeight > adjustedHeight) {
        adjustedHeight = Math.max(chatboxtextarea.scrollHeight, adjustedHeight);
        if (maxHeight)
            adjustedHeight = Math.min(maxHeight, adjustedHeight);
        if (adjustedHeight > chatboxtextarea.clientHeight)
            chatboxtextarea.style.height = adjustedHeight+8 +'px';
        } else {
            chatboxtextarea.style.overflow = 'auto';
        }
    },


    
    setChatMessage : function(user, message){
        var div_content = document.getElementById(this.rootDivName+'_content');
        
        var chatDiv = document.createElement("div");
        this.addClass(chatDiv, 'stalk_message');
        
        var messageHTML = '';
        if(user.link){
            messageHTML = messageHTML + '<span class="stalk_messagefrom"><a target="_blank" href="'+user.link+'" class="stalk_userlink" alt="GO TO User Page!">'+user.name+'</a>:&nbsp;&nbsp;</span>';
        }
        chatDiv.innerHTML = '<span class="stalk_messagecontent">'+messageHTML + decodeURIComponent(message)+'</span>&nbsp;&nbsp;&nbsp;<span class="stalk_messagetime">'+this.getNowStr()+'</span>';
        
        div_content.appendChild(chatDiv);
        div_content.scrollTop = div_content.scrollHeight;

        if(document.getElementById(this.rootDivName+'_content').style.display != 'block'){
        	this.blinkHeader();
        }
        STALK_conf.isEmptyMessage = false;

    },
    
    setSysMessage : function(message){
        
        var div_content = document.getElementById(this.rootDivName+'_content');
        
        var chatDiv = document.createElement("div");
        this.addClass(chatDiv, 'stalk_message');
        chatDiv.innerHTML = '<span class="stalk_systemcontent">'+this.getNowStr()+' - '+message+'</span>';
        
        div_content.appendChild(chatDiv);
        div_content.scrollTop = div_content.scrollHeight;
    },
    
    toggleChatBoxGrowth : function() {
    	
    	if("OPTION_BUTTON" == this.eventType){
    		this.eventType = "";
    		return false;
    	}
    	
    	this.blinkHeader(true);
        var div_content = document.getElementById(this.rootDivName+'_content');
		var div_root 	= document.getElementById(this.rootDivName);
        if (div_content.style.display == 'none') {  

			document.getElementById(this.rootDivName).style.width = this.divOpenWidth;
            document.getElementById(this.rootDivName+'_options').style.display = 'block';
            document.getElementById(this.rootDivName+'_size').style.display = 'block';
			document.getElementById(this.rootDivName+'_title').style["float"] = 'left';
			document.getElementById(this.rootDivName+'_title').style.textAlign = '';
			document.getElementById(this.rootDivName+'_title').style.width = '';
            document.getElementById(this.rootDivName+'_sitelink').style.display = 'block';
			document.getElementById(this.rootDivName+'_head').style.padding = '7px 7px 7px 7px';
			
            div_content.style.display = 'block';
            if(this.isLogined){

                document.getElementById(this.rootDivName+'_login').style.display = 'none';
                document.getElementById(this.rootDivName+'_input').style.display = 'block';
            }else{

                document.getElementById(this.rootDivName+'_login').style.display = 'block';
                document.getElementById(this.rootDivName+'_input').style.display = 'none';
            }
            this.sizing();

        } else {

			document.getElementById(this.rootDivName).style.width = this.divCloseWidth;
			document.getElementById(this.rootDivName+'_title').style["float"] = '';
			document.getElementById(this.rootDivName+'_title').style.textAlign = 'center';
			document.getElementById(this.rootDivName+'_title').style.width = '100%';
            div_content.style.display = 'none';
            document.getElementById(this.rootDivName+'_login').style.display = 'none';
            document.getElementById(this.rootDivName+'_input').style.display = 'none';
            document.getElementById(this.rootDivName+'_options').style.display = 'none';
            document.getElementById(this.rootDivName+'_size').style.display = 'none';
            document.getElementById(this.rootDivName+'_input').style.display = 'none';
            document.getElementById(this.rootDivName+'_sitelink').style.display = 'none';
			if(this.isIE()) document.getElementById(this.rootDivName+'_head').style.padding = '7px 0px 0px 0px';
        }
       
    },

    setTitleMessage : function(message){
        var div_title = document.getElementById(this.rootDivName+'_title');
        div_title.innerHTML = message;
    },
    
    logined : function(target){
        
        var div_login = document.getElementById(this.rootDivName+'_login');
        var div_input = document.getElementById(this.rootDivName+'_input');
        
        if(document.getElementById(this.rootDivName+'_content').style.display == 'block'){
            div_login.style.display = 'none';
            div_input.style.display = 'block';
        }else{
            div_login.style.display = 'none';
            div_input.style.display = 'none';    
        }
        
        document.getElementById(this.rootDivName+'_options').innerHTML = '<a href="javascript:void(0)" onclick="javascript:return STALK.logout();" class="stalk_tooltip" title="Logout from '+target+'"><img src="'+this.imageServer+'/img/logout.png"></a>';

        var div_textarea = document.getElementById(this.rootDivName+'_textarea');

        if(div_input.style.display  != 'none') div_textarea.focus();
        div_textarea.value = div_textarea.value;
        
        this.isLogined = true;
        
        this.setSysMessage('logined with '+target);

    },

    logout : function(){
        
    	if(!this.isIE()) this.eventType = "OPTION_BUTTON";
    		
        var div_login = document.getElementById(this.rootDivName+'_login');
        var div_input = document.getElementById(this.rootDivName+'_input');
        
        if(document.getElementById(this.rootDivName+'_content').style.display == 'block'){
            div_login.style.display = 'block';
            div_input.style.display = 'none';
        }else{
            div_login.style.display = 'none';
            div_input.style.display = 'none';    
        }

        STALK_utils.delUserInfo();
        document.getElementById(this.rootDivName+'_options').innerHTML = "";
        
        this.isLogined = false;

        this.setSysMessage('logout.');
        

    },

    sizing : function(gbn){
    	
    	if(gbn){
    		if(!this.isIE()) this.eventType = "OPTION_BUTTON";
    		this.currentSize = gbn;
    	}

	if(this.pageSize.width <= 980 ) return;

    	if(this.currentSize == "MIN"){
    		document.getElementById(this.rootDivName+'_size').innerHTML = '<a href="javascript:void(0)" onclick="javascript:return STALK.sizing(\'MAX\');" class="stalk_tooltip" title="Big Size"><img src="'+this.imageServer+'/img/max.png"></a>';
    		document.getElementById(this.rootDivName).style.width = this.divOpenWidth;
    	}else{
    		document.getElementById(this.rootDivName+'_size').innerHTML = '<a href="javascript:void(0)" onclick="javascript:return STALK.sizing(\'MIN\');" class="stalk_tooltip" title="Small Size"><img src="'+this.imageServer+'/img/min.png"></a>';
    		document.getElementById(this.rootDivName).style.width = this.divMaxOpenWidth;
    	}
    },

    getNowStr : function(){
        var currentTime = new Date();
        var hours = currentTime.getHours();
        var minutes = currentTime.getMinutes();

        if (minutes < 10){
            minutes = "0" + minutes;
        }
        var rtn = hours + ":" + minutes + " ";
        if(hours > 11){
            rtn = rtn + "PM";
        } else {
            rtn = rtn + "AM";
        }

        return rtn;
    },
    
    isIE : function(){
    	return (/MSIE (\d+\.\d+);/.test(navigator.userAgent));
    }
    
};


////////////////////////////////////////////////////////////////////////////////
/////////////////// STALK Module /////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

var STALK = (function(CONF, UTILS, WIN) {

    /*** PRIAVTE AREA ***/

	var fn_init = function(){
    	//console.log(" # 1. fn_init \n\t"+JSON.stringify(CONF));
        UTILS.loadJson(CONF.appUrl+'/node?refer='+CONF.refer, 'STALK.callbackInit');
    };
    var fn_reInit = function(){
    	//console.log(" # 1. fn_init \n\t"+JSON.stringify(CONF));
        UTILS.loadJson(CONF.appUrl+'/node?refer='+CONF.refer, 'STALK.callbackSocketCallback');
    };

    var fn_callbackInit = function(data){

    	//console.log(" # 2. callbackInit \n\t"+JSON.stringify(data));
    	
    	if(data.status != "ok"){
			//console.log("ERROR");
    		return;
    	}
    	
        CONF.serverInfo = data;

        WIN.initWin(CONF.divName, CONF.httpUrl);
        
        CONF.user = STALK_utils.getUserInfo();
        //console.log("USER : "+JSON.stringify(user));
        if ( typeof CONF.user.target == "undefined") {
        	CONF.isLogined = false;
        }else{
        	CONF.isLogined = true;
        	WIN.logined(CONF.user.target);
        }
        
        UTILS.loadScript("http://cdn.sockjs.org/sockjs-0.3.min.js", fn_callbackSocketCallback);
        
    };
    var dummy = function(){
    	
    };
    var fn_callbackSocketCallback = function(data){
    	
    	if(data){ // re connect !!

        	//console.log(" # 2(2). callbackSocketCallback \n\t"+JSON.stringify(data));
    		if(data.status != "ok"){
    			// Error is occured!!
    			if(WIN.isShowRootDiv()){
    				WIN.setSysMessage("I'm so sorry. Traffic jam right now.<br/> Please connect again later.");
    			}
        		return;
        	}
    		CONF.serverInfo = data;
    	}

        WIN.showRootDiv(true);
        
        CONF.sock = new SockJS('http://'+CONF.serverInfo.host+':'+CONF.serverInfo.port+'/message');
        
        CONF.sock.onopen = function(e) {
            
            var reqJson = {
            	action 	: "JOIN",
            	refer 	: CONF.refer,
            	user 	: CONF.user
            };
            CONF.sock.send(JSON.stringify(reqJson));
            
            CONF.__intvalId = setInterval(function(){if(CONF.isEmptyMessage && CONF.currentUserCnt >= 2){WIN.setChatMessage({}, ' * You can talk to each other! :) *');CONF.isEmptyMessage=false;clearInterval(CONF.__intvalId);}},10000);
        };
        CONF.sock.onmessage = function(e) {
        	
            //console.log(' ---- message : '+e);
            
            var resJson = JSON.parse(e.data);
            if(resJson.action == "JOIN"){
            	CONF.socketId = resJson.socketId;
            	CONF.currentUserCnt = resJson.count;
            	WIN.setTitleMessage("ONLINE : "+CONF.currentUserCnt);
            }else if(resJson.action == "IN"){
            	CONF.currentUserCnt = resJson.count;
            	WIN.setTitleMessage("ONLINE : "+CONF.currentUserCnt);
            }else if(resJson.action == "LOGIN"){
            	UTILS.setUserInfo(resJson.user);
            	CONF.isLogined = true;
            	CONF.user = resJson.user;
            	WIN.logined(CONF.user.target);

                var reqJson = {
                	action 	: "JOIN",
                	refer 	: CONF.refer,
                	user 	: CONF.user
                };
                CONF.sock.send(JSON.stringify(reqJson));
                
            }else if(resJson.action == "LOGOUT"){
            	CONF.currentUserCnt = resJson.count;
            	WIN.setTitleMessage("ONLINE : "+CONF.currentUserCnt);
            }else if(resJson.action == "OUT"){
            	CONF.currentUserCnt = resJson.count;
            	WIN.setTitleMessage("ONLINE : "+CONF.currentUserCnt);
            }else if(resJson.action == "MESSAGE"){
            	WIN.setChatMessage(resJson.user, resJson.message);
            }else {
            	
            }
            
        };
        CONF.sock.onclose = function() {
            //console.log('close');
            fn_reInit();
        };
    };


    var fn_sendMessage = function(message){
        var reqJson = {
        	action 	: "MESSAGE",
        	refer 	: CONF.refer,
        	user 	: CONF.user,
        	message : message
        };
        CONF.sock.send(JSON.stringify(reqJson));        
    };

    var fn_logout = function(){
    	var reqJson = {
        	action 	: "LOGOUT",
        	refer 	: CONF.refer,
        	user 	: CONF.user
    	};
    	CONF.sock.send(JSON.stringify(reqJson));
    	
    	WIN.logout();
    	CONF.isLogined = false;
    };

    /*** PUBLIC AREA (func) ***/
    return {

        // ## INIT ## //
        init: function(data) {
            if(CONF.isReady) return false; 
            CONF.isReady = true;
            CONF.refer = location.host + location.pathname;
            if(data){
                if(data.divName)    CONF.divName      = data.divName;
                if(data.refer)     	CONF.refer        = data.refer;
            }
            if(CONF.refer){}else{ 
            	return;
            }
            // TODO ! !! ! ! !
            fn_init();
        },

        callbackInit : function(data){
            fn_callbackInit(data);
        },
        callbackSocketCallback : function(data){
        	fn_callbackSocketCallback(data);
        },
        sendMessage : function(message){
            fn_sendMessage(message);
        },
        callbackSendMessage : function(data){
            
        },
        logout : function() {
            fn_logout();
        },
        getOauthUrl : function(targetName){
       		//if(targetName == "google"){
		
		//} 	
            return CONF.appUrl + '/auth?target='+targetName+'&channel='+CONF.serverInfo.channel+'&socketId='+CONF.socketId+'&refer='+CONF.refer;
        },
        sizing : function(gbn){
        	WIN.sizing(gbn);
        }

    };

})(STALK_conf, STALK_utils, STALK_window);

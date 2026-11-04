(function () {
  'use strict';
  function byId(id) { return document.getElementById(id); }
  function on(el, name, fn) { if (el.addEventListener) el.addEventListener(name, fn, false); else el.attachEvent('on' + name, fn); }
  function hasClass(el, name) { return (' ' + el.className + ' ').indexOf(' ' + name + ' ') !== -1; }
  function addClass(el, name) { if (!hasClass(el, name)) el.className += (el.className ? ' ' : '') + name; }
  function removeClass(el, name) { el.className = (' ' + el.className + ' ').replace(' ' + name + ' ', ' ').replace(/^\s+|\s+$/g, ''); }
  function clear(el) { while (el.firstChild) el.removeChild(el.firstChild); }
  function text(el, value) { el.textContent = value == null ? '' : String(value); }
  function array(value) { return Object.prototype.toString.call(value) === '[object Array]' ? value : []; }
  function encode(value) { return encodeURIComponent(value == null ? '' : String(value)); }
  var state = { server:'', username:'', password:'', section:'live', categories:[], categoryId:null, streams:[], cache:{}, returnFocus:null, inSeries:false, favorites:[], recent:[], favoritesOnly:false, recentOnly:false, playbackKey:null, resumeAt:0, resumeApplied:false, visibleCount:0 };
  var PAGE_SIZE = 60;
  var sections = {
    live:{title:'Live TV', cats:'get_live_categories', items:'get_live_streams'},
    movie:{title:'Movies', cats:'get_vod_categories', items:'get_vod_streams'},
    series:{title:'Series', cats:'get_series_categories', items:'get_series'}
  };
  function apiUrl(action, categoryId, seriesId) {
    var query = 'username=' + encode(state.username) + '&password=' + encode(state.password);
    if (action) query += '&action=' + encode(action);
    if (categoryId !== null && categoryId !== undefined) query += '&category_id=' + encode(categoryId);
    if (seriesId !== null && seriesId !== undefined) query += '&series_id=' + encode(seriesId);
    return state.server.replace(/\/+$/, '') + '/player_api.php?' + query;
  }
  function requestText(url, done, failed) {
    var xhr = new XMLHttpRequest(), finished = false;
    var timer = setTimeout(function () { if (!finished) { finished = true; xhr.abort(); failed(new Error('Connection timed out.')); } }, 25000);
    xhr.onreadystatechange = function () {
      if (xhr.readyState !== 4 || finished) return;
      finished = true; clearTimeout(timer);
      if (xhr.status < 200 || xhr.status >= 300) { failed(new Error('Playlist server returned HTTP ' + xhr.status)); return; }
      done(xhr.responseText || '');
    };
    xhr.onerror = function () { if (!finished) { finished = true; clearTimeout(timer); failed(new Error('Could not download playlist. The server may block TV browser requests (CORS).')); } };
    try { xhr.open('GET', url, true); xhr.timeout = 25000; xhr.send(null); } catch (e) { if (!finished) { finished = true; clearTimeout(timer); failed(e); } }
  }
  function requestJson(url, done, failed) {
    var xhr = new XMLHttpRequest(), finished = false;
    var timer = setTimeout(function () { if (!finished) { finished = true; xhr.abort(); failed(new Error('Connection timed out.')); } }, 18000);
    xhr.onreadystatechange = function () {
      if (xhr.readyState !== 4 || finished) return;
      finished = true; clearTimeout(timer);
      if (xhr.status < 200 || xhr.status >= 300) { failed(new Error('Server returned HTTP ' + xhr.status)); return; }
      try { done(JSON.parse(xhr.responseText)); } catch (e) { failed(new Error('Invalid server response.')); }
    };
    xhr.onerror = function () { if (!finished) { finished = true; clearTimeout(timer); failed(new Error('Network error or server CORS restriction.')); } };
    xhr.ontimeout = function () { if (!finished) { finished = true; clearTimeout(timer); failed(new Error('Connection timed out.')); } };
    try { xhr.open('GET', url, true); xhr.timeout = 18000; xhr.send(null); } catch (e) { if (!finished) { finished = true; clearTimeout(timer); failed(e); } }
  }
  function show(id) {
    var ids = ['login-screen','catalog-screen','player-screen'], i;
    for (i=0;i<ids.length;i++) { if (ids[i] === id) removeClass(byId(ids[i]), 'hidden'); else addClass(byId(ids[i]), 'hidden'); }
    if (id === 'catalog-screen') focusFirst();
  }
  function status(message) { text(byId('status'), message); }
  function focusFirst() {
    var target = document.querySelector('.media-card') || document.querySelector('.category-chip') || document.querySelector('.nav-item[data-section]');
    if (target && target.focus) target.focus();
  }
  function registerKeys() {
    try {
      if (window.tizen && window.tizen.tvinputdevice) {
        var keys = ['MediaPlayPause','MediaStop','MediaFastForward','MediaRewind','MediaPlay','MediaPause'], i;
        for (i=0;i<keys.length;i++) { try { window.tizen.tvinputdevice.registerKey(keys[i]); } catch (ignore) {} }
      }
    } catch (ignore2) {}
  }
  function connect(event) {
    if (event && event.preventDefault) event.preventDefault();
    state.server = byId('server').value.replace(/^\s+|\s+$/g,'').replace(/\/+$/,'');
    state.username = byId('username').value.replace(/^\s+|\s+$/g,'');
    state.password = byId('password').value; state.sourceType='xtream';
    text(byId('login-error'), '');
    requestJson(apiUrl(''), function (info) {
      if (!info || (!info.user_info && !info.server_info)) { text(byId('login-error'),'Unrecognized server response. Check your details.'); return; }
      var accountStatus = info.user_info && info.user_info.status;
      if (accountStatus && ['Active','Enabled','Trial'].indexOf(accountStatus) < 0) { text(byId('login-error'),'This account is not active.'); return; }
      try { localStorage.setItem('tvi-profile', JSON.stringify({server:state.server,username:state.username})); } catch (ignore) {}
      text(byId('profile-status'), state.username); state.cache = {}; show('catalog-screen'); loadSection('live');
    }, function (error) { text(byId('login-error'), error.message || 'Could not connect.'); });
    return false;
  }
  function connectM3u() {
    var url = byId('m3u-url').value.replace(/^\s+|\s+$/g,'');
    if (!/^https?:\/\//i.test(url)) { text(byId('login-error'),'Enter a valid http or https M3U playlist URL.'); return; }
    text(byId('login-error'),''); status('Downloading M3U playlist…');
    requestText(url,function(body){
      var lines=body.replace(/^\uFEFF/,'').split(/\r?\n/),items=[],categories={},pending='',i,line,attrs,name,group,logo,match,entry;
      for(i=0;i<lines.length;i++){
        line=lines[i].replace(/^\s+|\s+$/g,'');
        if(line.indexOf('#EXTINF:')===0){pending=line;continue;}
        if(!line||line.charAt(0)==='#'||!pending)continue;
        attrs={};
        var re=/([\w-]+)=("(?:[^"]*)"|'(?:[^']*)'|[^,\s]*)/g;
        while((match=re.exec(pending))!==null)attrs[match[1].toLowerCase()]=match[2].replace(/^["']|["']$/g,'');
        name=pending.substring(pending.lastIndexOf(',')+1).replace(/^\s+|\s+$/g,'')||attrs['tvg-name']||'Untitled channel';
        group=attrs['group-title']||'Other';logo=attrs['tvg-logo']||'';
        entry={stream_id:'m3u-'+items.length,name:name,category_id:group,stream_icon:logo,direct_url:line,container_extension:''};
        items.push(entry);categories[group]=true;pending='';
      }
      if(!items.length){text(byId('login-error'),'No valid #EXTINF channel entries were found in this playlist.');return;}
      state.sourceType='m3u';state.server='';state.username='';state.password='';state.cache={};state.section='live';
      state.categories=[];for(group in categories){if(Object.prototype.hasOwnProperty.call(categories,group))state.categories.push({category_id:group,category_name:group});}
      state.cache['live:categories']=state.categories;state.cache['live:all']=items;
      try{localStorage.setItem('tvi-m3u-url',url);}catch(ignore){}
      text(byId('profile-status'),'M3U playlist');show('catalog-screen');loadSection('live');
    },function(error){text(byId('login-error'),error.message||'Could not load playlist.');});
  }
  function loadSection(section) {
    if(state.sourceType==='m3u'&&section!=='live'){window.alert('This M3U playlist contains Live TV only.');return;}
    if ((section==='movie'||section==='series') && !parentalUnlock()) return;
    state.favoritesOnly=false; state.recentOnly=false; updateFilterButtons();
    state.section=section; state.categoryId=null; state.categories=[]; state.streams=[]; state.inSeries=false;
    var nav=document.querySelectorAll('.nav-item[data-section]'), i;
    for(i=0;i<nav.length;i++) { if(nav[i].getAttribute('data-section')===section) addClass(nav[i],'active'); else removeClass(nav[i],'active'); }
    text(byId('section-title'),sections[section].title); clear(byId('media-grid')); clear(byId('category-list')); text(byId('catalog-count'),''); status('Loading categories…');
    var key=section+':categories';
    if (state.cache[key]) { state.categories=state.cache[key]; categoriesReady(); return; }
    requestJson(apiUrl(sections[section].cats),function(data){state.categories=array(data);state.cache[key]=state.categories;categoriesReady();},function(error){status('Could not load categories: '+error.message);});
  }
  function categoriesReady() {
    renderCategories();
    if (!state.categories.length) { status('No categories returned by this service.'); return; }
    var first=state.categories[0], id=first.category_id != null ? first.category_id : first.id;
    loadCategory(String(id));
  }
  function renderCategories() {
    var root=byId('category-list'); clear(root);
    state.categories.forEach(function(category,index){
      var id=String(category.category_id != null ? category.category_id : (category.id != null ? category.id : ''));
      var button=document.createElement('button'); button.className='category-chip'+(id===state.categoryId?' active':'');
      text(button,category.category_name||category.name||('Category '+(index+1))); button.setAttribute('data-category-id',id);
      on(button,'click',function(){loadCategory(id);}); root.appendChild(button);
    });
  }
  function loadCategory(id) {
    state.categoryId=id; state.inSeries=false; renderCategories(); clear(byId('media-grid'));
    var key=state.section+':'+id;
    if(state.sourceType==='m3u'){state.streams=(state.cache['live:all']||[]).filter(function(item){return String(item.category_id)===String(id);});state.visibleCount=0;renderMedia(true);status(state.streams.length?'':'No channels in this group.');return;}
    if (state.cache[key]) {state.streams=state.cache[key];state.visibleCount=0;renderMedia(true);status('');return;}
    status('Loading '+sections[state.section].title.toLowerCase()+'…');
    requestJson(apiUrl(sections[state.section].items,id),function(data){
      state.streams=array(data);state.cache[key]=state.streams;state.visibleCount=0;renderMedia(true);status(state.streams.length?'':'No items in this category.');
    },function(error){status('Could not load this category: '+error.message);});
  }
  function itemId(item) { return item.stream_id != null ? item.stream_id : (item.series_id != null ? item.series_id : item.id); }
  function itemName(item) { return item.name || item.title || 'Untitled'; }
  function mediaKey(item){return state.section+':'+String(itemId(item));}
  function isFavorite(item){return state.favorites.indexOf(mediaKey(item))!==-1;}
  function saveLists(){try{localStorage.setItem('tvi-favorites',JSON.stringify(state.favorites));localStorage.setItem('tvi-recent',JSON.stringify(state.recent));}catch(ignore){}}
  function updateFilterButtons(){
    var f=byId('show-favorites'),r=byId('show-recent');
    if(f){f.className=state.favoritesOnly?'filter-button active':'filter-button';}
    if(r){r.className=state.recentOnly?'filter-button active':'filter-button';}
  }
  function toggleFavorite(item){
    var key=mediaKey(item),at=state.favorites.indexOf(key);
    if(at<0)state.favorites.push(key);else state.favorites.splice(at,1);
    saveLists();renderMedia();
  }
  function rememberItem(item){
    var key=mediaKey(item),i;
    for(i=state.recent.length-1;i>=0;i--){if(state.recent[i].key===key)state.recent.splice(i,1);}
    state.recent.unshift({key:key,section:state.section,id:String(itemId(item)),name:itemName(item),position:0,duration:0,url:item.direct_url||streamUrl(state.section==='live'?'live':'movie',itemId(item),item.container_extension||(state.section==='live'?'ts':'mp4'))});
    if(state.recent.length>40)state.recent.length=40;saveLists();
  }
  function savePlaybackProgress(){
    var video=byId('video-player'),i,item;if(!state.playbackKey||!video||!isFinite(video.currentTime))return;
    for(i=0;i<state.recent.length;i++){if(state.recent[i].key===state.playbackKey){item=state.recent[i];break;}}
    if(!item)return;item.position=Math.max(0,Math.floor(video.currentTime));item.duration=isFinite(video.duration)?Math.floor(video.duration):0;
    if(item.duration>0&&item.position>=item.duration-30){item.position=0;}
    saveLists();
  }
  function applyResume(){
    var video=byId('video-player'),item,i;if(state.resumeApplied||!state.playbackKey)return;
    state.resumeApplied=true;
    for(i=0;i<state.recent.length;i++){if(state.recent[i].key===state.playbackKey){item=state.recent[i];break;}}
    if(item&&item.position>10&&(!item.duration||item.position<item.duration-30)){
      try{video.currentTime=item.position;status('');}catch(ignore){}
    }
  }
  function renderMedia(reset) {
    var root=byId('media-grid'),shown=[],query=(byId('catalog-search').value||'').toLowerCase(),i,item,key,name;
    if(reset!==false){state.visibleCount=PAGE_SIZE;clear(root);}
    for(i=0;i<state.streams.length;i++){
      item=state.streams[i];key=mediaKey(item);name=itemName(item).toLowerCase();
      if(query&&name.indexOf(query)===-1)continue;
      if(state.favoritesOnly&&!isFavorite(item))continue;
      if(state.recentOnly&&!recentHas(key))continue;
      shown.push(item);
    }
    if(state.visibleCount>shown.length)state.visibleCount=shown.length;
    text(byId('catalog-count'),shown.length+' / '+state.streams.length+' items');
    for(i=root.querySelectorAll('.load-more').length-1;i>=0;i--){var old=root.querySelectorAll('.load-more')[i];if(old.parentNode)old.parentNode.removeChild(old);}
    for(i=0;i<shown.length&&i<state.visibleCount;i++){
      item=shown[i];
      var button=document.createElement('button');button.className='media-card';button.setAttribute('data-item-id',itemId(item));
      var poster=document.createElement('span');poster.className='poster';
      var imageUrl=item.stream_icon||item.cover||item.movie_image||'';
      if(imageUrl){var img=document.createElement('img');img.alt='';img.src=imageUrl;img.setAttribute('loading','lazy');on(img,'error',function(){if(this.parentNode){this.parentNode.removeChild(this);}});poster.appendChild(img);}
      else text(poster,state.section==='live'?'▶':'▧');
      var title=document.createElement('span');title.className='media-title';text(title,itemName(item));button.appendChild(poster);button.appendChild(title);
      if(isFavorite(item))addClass(button,'is-favorite');
      on(button,'click',(function(selected){return function(){if(state.section==='series')openSeries(selected,this);else play(selected);};}(item)));
      on(button,'dblclick',(function(selected){return function(){toggleFavorite(selected);};}(item)));
      root.appendChild(button);
    }
    if(shown.length>state.visibleCount){
      var more=document.createElement('button');more.className='filter-button load-more';more.type='button';
      text(more,'Load more ('+Math.min(PAGE_SIZE,shown.length-state.visibleCount)+' of '+(shown.length-state.visibleCount)+')');
      on(more,'click',function(){state.visibleCount=Math.min(shown.length,state.visibleCount+PAGE_SIZE);renderMedia(false);});
      root.appendChild(more);
    }
    if(reset!==false)focusFirst();
  }
  function recentHas(key){var i;for(i=0;i<state.recent.length;i++){if(state.recent[i].key===key)return true;}return false;}
  function parentalUnlock(){
    var saved='';try{saved=localStorage.getItem('tvi-parental-pin')||'';}catch(ignore){}
    if(!saved){var created=window.prompt('Create a 4-digit parental PIN to protect Movies and Series:');if(!created)return false;if(!/^\d{4}$/.test(created)){window.alert('PIN must contain exactly 4 digits.');return false;}try{localStorage.setItem('tvi-parental-pin',created);}catch(ignore2){}return true;}
    var entered=window.prompt('Enter parental PIN:');if(entered===saved)return true;window.alert('Incorrect PIN.');return false;
  }
  function showEpg(item){
    if(state.section!=='live')return;
    status('Loading programme information…');
    requestJson(apiUrl('get_short_epg')+'&stream_id='+encode(itemId(item)),function(data){
      var entries=data&&data.epg_listings?data.epg_listings:[],lines=[],i;
      for(i=0;i<entries.length&&i<5;i++){var e=entries[i],title=e.title||'Programme';try{title=decodeURIComponent(escape(window.atob(title)));}catch(ignore){}lines.push(title+(e.start?' — '+e.start:''));}
      status(lines.length?itemName(item)+' · '+lines.join(' | '):'No EPG information available for '+itemName(item)+'.');
    },function(){status('EPG is unavailable for this channel.');});
  }
  function openSeries(series,sourceButton) {
    state.returnFocus=sourceButton;status('Loading episodes…');
    requestJson(apiUrl('get_series_info',null,itemId(series)),function(details){
      var episodes=details && details.episodes ? details.episodes : {}, flat=[], season;
      for(season in episodes){if(Object.prototype.hasOwnProperty.call(episodes,season)){flat=flat.concat(array(episodes[season]));}}
      if(!flat.length){status('No episodes were returned for this series.');return;}
      state.inSeries=true;clear(byId('media-grid'));text(byId('section-title'),itemName(series));text(byId('catalog-count'),flat.length+' episodes');
      flat.forEach(function(episode,index){
        var button=document.createElement('button');button.className='media-card episode-card';
        var poster=document.createElement('span');poster.className='poster';text(poster,'▶');
        var title=document.createElement('span');title.className='media-title';text(title,episode.title||('Episode '+(episode.episode_num||index+1)));
        button.appendChild(poster);button.appendChild(title);on(button,'click',function(){playEpisode(episode,series);});byId('media-grid').appendChild(button);
      });
      var back=document.createElement('button');back.className='category-chip';text(back,'← Back to series');
      on(back,'click',returnToSeries);byId('category-list').insertBefore(back,byId('category-list').firstChild);
      status('Select an episode. Use Back to return to series.');focusFirst();
    },function(error){status('Could not load episodes: '+error.message);});
  }
  function returnToSeries(){state.inSeries=false;text(byId('section-title'),sections.series.title);renderCategories();renderMedia();if(state.returnFocus&&state.returnFocus.focus)state.returnFocus.focus();}
  function streamUrl(path,id,ext){return state.server.replace(/\/+$/,'')+'/'+path+'/'+encode(state.username)+'/'+encode(state.password)+'/'+encode(id)+'.'+(ext||'mp4');}
  function startVideo(url,title,key){var video=byId('video-player');state.playbackKey=key||null;state.resumeApplied=false;state.resumeAt=0;video.src=url;text(byId('playing-title'),title);show('player-screen');var result=video.play();if(result&&result.catch)result.catch(function(){});}
  function play(item){var path=state.section==='live'?'live':'movie',key=mediaKey(item);rememberItem(item);startVideo(item.direct_url||streamUrl(path,itemId(item),item.container_extension||(state.section==='live'?'ts':'mp4')),itemName(item),key);}
  function playEpisode(episode,series){var id=episode.id||episode.stream_id;if(!id){status('This episode has no playable stream ID.');return;}var key='episode:'+String(id),i,entry=null;for(i=state.recent.length-1;i>=0;i--){if(state.recent[i].key===key){entry=state.recent[i];state.recent.splice(i,1);break;}}if(!entry)entry={key:key,section:'episode',id:String(id),name:episode.title||itemName(series),position:0,duration:0,url:streamUrl('series',id,episode.container_extension||'mp4')};state.recent.unshift(entry);if(state.recent.length>40)state.recent.length=40;saveLists();startVideo(streamUrl('series',id,episode.container_extension||'mp4'),episode.title||itemName(series),key);}
  function closePlayer(){var video=byId('video-player');savePlaybackProgress();video.pause();video.removeAttribute('src');video.load();state.playbackKey=null;show('catalog-screen');}
  function back(){if(!hasClass(byId('player-screen'),'hidden')){closePlayer();return true;}if(!hasClass(byId('catalog-screen'),'hidden')&&state.inSeries){returnToSeries();return true;}if(!hasClass(byId('catalog-screen'),'hidden')){byId('change-account').focus();return true;}return false;}
  on(byId('login-form'),'submit',connect);
  on(byId('connect-m3u'),'click',connectM3u);
  on(byId('section-nav'),'click',function(event){event=event||window.event;var target=event.target||event.srcElement;while(target&&target!==byId('section-nav')&&!target.getAttribute('data-section'))target=target.parentNode;if(target&&target.getAttribute('data-section'))loadSection(target.getAttribute('data-section'));});
  on(byId('change-account'),'click',function(){state.cache={};byId('password').value='';show('login-screen');byId('server').focus();});
  on(byId('close-player'),'click',closePlayer);
  on(byId('video-player'),'timeupdate',savePlaybackProgress);
  on(byId('video-player'),'pause',savePlaybackProgress);
  on(byId('video-player'),'loadedmetadata',applyResume);
  on(byId('video-player'),'ended',function(){if(state.playbackKey){var i;for(i=0;i<state.recent.length;i++){if(state.recent[i].key===state.playbackKey){state.recent[i].position=0;saveLists();break;}}}});
  on(byId('catalog-search'),'input',function(){state.favoritesOnly=false;state.recentOnly=false;state.visibleCount=0;updateFilterButtons();renderMedia(true);});
  on(byId('show-favorites'),'click',function(){state.favoritesOnly=!state.favoritesOnly;state.recentOnly=false;state.visibleCount=0;updateFilterButtons();renderMedia(true);});
  on(byId('show-recent'),'click',function(){state.recentOnly=!state.recentOnly;state.favoritesOnly=false;state.visibleCount=0;updateFilterButtons();renderMedia(true);});
  on(byId('parental-settings'),'click',function(){var old='';try{old=localStorage.getItem('tvi-parental-pin')||'';}catch(ignore){}if(old){var verify=window.prompt('Enter current parental PIN:');if(verify!==old){window.alert('Incorrect PIN.');return;}}var pin=window.prompt(old?'Enter new parental PIN (4 digits):':'Set parental PIN (4 digits):');if(pin===null)return;if(!/^\d{4}$/.test(pin)){window.alert('PIN must contain exactly 4 digits.');return;}try{localStorage.setItem('tvi-parental-pin',pin);window.alert('Parental PIN saved on this TV.');}catch(ignore2){window.alert('Could not save PIN on this TV.');}});
  on(document,'keydown',function(event){
    event=event||window.event;var key=event.key||'',code=event.keyCode;
    if(key==='Escape'||key==='Backspace'||code===10009){if(back()){if(event.preventDefault)event.preventDefault();return;}}
    if(key==='MediaPlayPause'||code===10252){var video=byId('video-player');if(!hasClass(byId('player-screen'),'hidden')){if(video.paused){var p=video.play();if(p&&p.catch)p.catch(function(){});}else video.pause();if(event.preventDefault)event.preventDefault();}return;}
    var active=document.activeElement;
    if((key==='f'||key==='F')&&active&&active.getAttribute&&active.getAttribute('data-item-id')!==null){
      var favoriteId=active.getAttribute('data-item-id'),fi;
      for(fi=0;fi<state.streams.length;fi++){if(String(itemId(state.streams[fi]))===String(favoriteId)){toggleFavorite(state.streams[fi]);break;}}
      if(event.preventDefault)event.preventDefault();return;
    }
    if(key==='Info'||key==='Information'||code===457){
      var focused=document.activeElement,id=focused&&focused.getAttribute?focused.getAttribute('data-item-id'):null,i;
      if(id!==null){for(i=0;i<state.streams.length;i++){if(String(itemId(state.streams[i]))===String(id)){showEpg(state.streams[i]);break;}}}
      if(event.preventDefault)event.preventDefault();return;
    }
    if(key==='MediaStop'||code===10253){if(!hasClass(byId('player-screen'),'hidden')){closePlayer();if(event.preventDefault)event.preventDefault();}return;}
    var dirs={ArrowRight:[1,0],ArrowLeft:[-1,0],ArrowDown:[0,1],ArrowUp:[0,-1]},d=dirs[key],active=document.activeElement;
    if(key==='Enter'&&active&&active.tagName==='BUTTON'){active.click();if(event.preventDefault)event.preventDefault();return;}
    if(!d||!hasClass(byId('player-screen'),'hidden'))return;
    var candidates=document.querySelectorAll('#catalog-screen button:not(:disabled), #login-screen button:not(:disabled), #login-screen input:not(:disabled)'),origin=active&&active.getBoundingClientRect?active.getBoundingClientRect():null;
    if(!origin)return;
    var ox=origin.left+origin.width/2,oy=origin.top+origin.height/2,best=null,bestScore=Infinity,i;
    for(i=0;i<candidates.length;i++){var candidate=candidates[i];if(candidate===active||!candidate.getClientRects().length)continue;var rect=candidate.getBoundingClientRect(),vx=rect.left+rect.width/2-ox,vy=rect.top+rect.height/2-oy,primary=d[0]?vx*d[0]:vy*d[1],secondary=d[0]?Math.abs(vy):Math.abs(vx),score=primary+secondary*2.2;if(primary>4&&score<bestScore){best=candidate;bestScore=score;}}
    if(best){best.focus();if(event.preventDefault)event.preventDefault();}
    
  });
  on(window,'load',function(){registerKeys();try{state.favorites=array(JSON.parse(localStorage.getItem('tvi-favorites')||'[]'));state.recent=array(JSON.parse(localStorage.getItem('tvi-recent')||'[]'));var saved=JSON.parse(localStorage.getItem('tvi-profile')||'null');if(saved&&saved.server&&saved.username){byId('server').value=saved.server;byId('username').value=saved.username;}var m3u=localStorage.getItem('tvi-m3u-url');if(m3u)byId('m3u-url').value=m3u;}catch(ignore){}});
}());

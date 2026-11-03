(() => {
  'use strict';
  const $ = (id) => document.getElementById(id);
  const state = { server: '', username: '', password: '', section: 'live', categories: [], categoryId: null, streams: [], cache: new Map(), focusIndex: 0, returnFocus: null };
  const sections = {
    live: { title: 'Live TV', cats: 'get_live_categories', items: 'get_live_streams' },
    movie: { title: 'Movies', cats: 'get_vod_categories', items: 'get_vod_streams' },
    series: { title: 'Series', cats: 'get_series_categories', items: 'get_series' }
  };
  const apiUrl = (action, extra = '') => {
    const q = new URLSearchParams({ username: state.username, password: state.password });
    if (action) q.set('action', action);
    if (extra) q.set('category_id', extra);
    return state.server.replace(/\/+$/, '') + '/player_api.php?' + q.toString();
  };
  async function fetchJson(url) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 18000);
    try {
      const response = await fetch(url, { signal: controller.signal, cache: 'no-store' });
      if (!response.ok) throw new Error('Server returned HTTP ' + response.status);
      return await response.json();
    } finally { clearTimeout(timer); }
  }
  function show(id) {
    ['login-screen','catalog-screen','player-screen'].forEach(x => $(x).classList.toggle('hidden', x !== id));
    if (id === 'catalog-screen') focusFirstAvailable();
  }
  function setStatus(message) { $('status').textContent = message; }
  function validArray(value) { return Array.isArray(value) ? value : []; }
  function focusFirstAvailable() {
    const target = document.querySelector('.media-card') || document.querySelector('.category-chip') || document.querySelector('.nav-item[data-section]');
    target?.focus();
  }
  function registerSamsungRemoteKeys() {
    try {
      const api = window.tizen?.tvinputdevice;
      if (!api) return;
      ['MediaPlayPause','MediaStop','MediaFastForward','MediaRewind','MediaPlay','MediaPause'].forEach(key => {
        try { api.registerKey(key); } catch (_) {}
      });
    } catch (_) {}
  }
  async function connect(event) {
    event?.preventDefault();
    state.server = $('server').value.trim().replace(/\/+$/, '');
    state.username = $('username').value.trim();
    state.password = $('password').value;
    $('login-error').textContent = '';
    try {
      const info = await fetchJson(apiUrl(''));
      if (!info || (!info.user_info && !info.server_info)) throw new Error('The server response was not recognized. Check the URL and login details.');
      const status = info.user_info?.status;
      if (status && !['Active','Enabled','Trial'].includes(status)) throw new Error('This account is not active.');
      localStorage.setItem('tvi-profile', JSON.stringify({ server: state.server, username: state.username }));
      $('profile-status').textContent = state.username;
      state.cache.clear();
      show('catalog-screen');
      await loadSection('live');
    } catch (error) { $('login-error').textContent = error.name === 'AbortError' ? 'Connection timed out. Try again.' : (error.message || 'Could not connect.'); }
  }
  async function loadSection(section) {
    state.section = section; state.categoryId = null; state.categories = []; state.streams = [];
    document.querySelectorAll('.nav-item[data-section]').forEach(button => button.classList.toggle('active', button.dataset.section === section));
    $('section-title').textContent = sections[section].title;
    $('media-grid').replaceChildren(); $('category-list').replaceChildren(); $('catalog-count').textContent = '';
    setStatus('Loading categories…');
    try {
      const key = section + ':categories';
      state.categories = state.cache.get(key) || validArray(await fetchJson(apiUrl(sections[section].cats)));
      state.cache.set(key, state.categories);
      renderCategories();
      if (!state.categories.length) { setStatus('No categories returned by this service.'); return; }
      await loadCategory(String(state.categories[0].category_id ?? state.categories[0].id));
    } catch (error) { setStatus('Could not load categories: ' + (error.message || 'network error')); }
  }
  function renderCategories() {
    const root = $('category-list'); root.replaceChildren();
    state.categories.forEach((category, index) => {
      const id = String(category.category_id ?? category.id ?? '');
      const button = document.createElement('button');
      button.className = 'category-chip' + (id === state.categoryId ? ' active' : '');
      button.textContent = category.category_name || category.name || ('Category ' + (index + 1));
      button.dataset.categoryId = id;
      button.addEventListener('click', () => loadCategory(id));
      root.append(button);
    });
  }
  async function loadCategory(id) {
    state.categoryId = id; renderCategories();
    const cacheKey = state.section + ':' + id;
    $('media-grid').replaceChildren();
    if (state.cache.has(cacheKey)) {
      state.streams = state.cache.get(cacheKey); renderMedia(); setStatus('');
      return;
    }
    setStatus('Loading ' + sections[state.section].title.toLowerCase() + '…');
    try {
      const data = validArray(await fetchJson(apiUrl(sections[state.section].items, id)));
      state.streams = data; state.cache.set(cacheKey, data);
      renderMedia(); setStatus(data.length ? '' : 'No items in this category.');
    } catch (error) { setStatus('Could not load this category: ' + (error.message || 'network error')); }
  }
  function itemId(item) { return item.stream_id ?? item.series_id ?? item.id; }
  function itemName(item) { return item.name || item.title || 'Untitled'; }
  function renderMedia() {
    const root = $('media-grid'); root.replaceChildren();
    $('catalog-count').textContent = state.streams.length + ' items';
    const fragment = document.createDocumentFragment();
    state.streams.forEach((item) => {
      const button = document.createElement('button'); button.className = 'media-card'; button.dataset.itemId = itemId(item);
      const poster = document.createElement('span'); poster.className = 'poster';
      const imageUrl = item.stream_icon || item.cover || item.movie_image || '';
      if (imageUrl) {
        const img = document.createElement('img'); img.loading = 'lazy'; img.alt = ''; img.src = imageUrl;
        img.onerror = () => { img.remove(); poster.textContent = '▶'; };
        poster.append(img);
      } else poster.textContent = state.section === 'live' ? '▶' : '▧';
      const title = document.createElement('span'); title.className = 'media-title'; title.textContent = itemName(item);
      button.append(poster, title); button.addEventListener('click', () => state.section === 'series' ? openSeries(item, button) : play(item));
      fragment.append(button);
    });
    root.append(fragment); state.focusIndex = 0;
    focusFirstAvailable();
  }
  async function openSeries(series, sourceButton) {
    state.returnFocus = sourceButton;
    setStatus('Loading episodes…');
    try {
      const details = await fetchJson(apiUrl('get_series_info', String(itemId(series))));
      const episodes = details.episodes || {};
      const flat = Object.values(episodes).flat().filter(Boolean);
      if (!flat.length) { setStatus('No episodes were returned for this series.'); return; }
      const root = $('media-grid'); root.replaceChildren();
      $('section-title').textContent = itemName(series);
      $('catalog-count').textContent = flat.length + ' episodes';
      flat.forEach((episode, index) => {
        const button = document.createElement('button'); button.className = 'media-card episode-card';
        const poster = document.createElement('span'); poster.className = 'poster'; poster.textContent = '▶';
        const title = document.createElement('span'); title.className = 'media-title';
        title.textContent = episode.title || ('Episode ' + (episode.episode_num || index + 1));
        button.append(poster, title); button.addEventListener('click', () => playEpisode(episode, series));
        root.append(button);
      });
      setStatus('Select an episode. Use Back to return to series.');
      const back = document.createElement('button'); back.className = 'category-chip'; back.textContent = '← Back to series';
      back.addEventListener('click', () => { $('section-title').textContent = sections.series.title; renderMedia(); state.returnFocus?.focus(); });
      $('category-list').prepend(back);
      focusFirstAvailable();
    } catch (error) { setStatus('Could not load episodes: ' + (error.message || 'network error')); }
  }
  function streamUrl(path, id, ext) {
    return state.server.replace(/\/+$/, '') + '/' + path + '/' + encodeURIComponent(state.username) + '/' + encodeURIComponent(state.password) + '/' + encodeURIComponent(id) + '.' + (ext || 'mp4');
  }
  function startVideo(url, title) {
    $('video-player').src = url; $('playing-title').textContent = title; show('player-screen');
    $('video-player').play().catch(() => {});
  }
  function play(item) {
    const path = state.section === 'live' ? 'live' : 'movie';
    const ext = item.container_extension || (state.section === 'live' ? 'ts' : 'mp4');
    startVideo(streamUrl(path, itemId(item), ext), itemName(item));
  }
  function playEpisode(episode, series) {
    const id = episode.id || episode.stream_id;
    if (!id) { setStatus('This episode has no playable stream ID.'); return; }
    startVideo(streamUrl('series', id, episode.container_extension || 'mp4'), (episode.title || itemName(series)));
  }
  function closePlayer() {
    $('video-player').pause(); $('video-player').removeAttribute('src'); $('video-player').load(); show('catalog-screen');
  }
  function handleBack() {
    if (!$('player-screen').classList.contains('hidden')) { closePlayer(); return true; }
    if (!$('catalog-screen').classList.contains('hidden') && state.section === 'series' && $('section-title').textContent !== sections.series.title) {
      $('section-title').textContent = sections.series.title; renderMedia(); state.returnFocus?.focus(); return true;
    }
    if (!$('catalog-screen').classList.contains('hidden')) {
      $('change-account').focus(); return true;
    }
    return false;
  }
  $('login-form').addEventListener('submit', connect);
  $('section-nav').addEventListener('click', event => { const button = event.target.closest('[data-section]'); if (button) loadSection(button.dataset.section); });
  $('change-account').addEventListener('click', () => { state.cache.clear(); $('password').value = ''; show('login-screen'); $('server').focus(); });
  $('close-player').addEventListener('click', closePlayer);
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' || event.key === 'Backspace' || event.keyCode === 10009) {
      if (handleBack()) { event.preventDefault(); event.stopPropagation(); return; }
    }
    if (event.key === 'MediaPlayPause' || event.keyCode === 10252) {
      const video = $('video-player');
      if (!$('player-screen').classList.contains('hidden')) { video.paused ? video.play().catch(() => {}) : video.pause(); event.preventDefault(); }
      return;
    }
    if (event.key === 'MediaStop' || event.keyCode === 10253) {
      if (!$('player-screen').classList.contains('hidden')) { closePlayer(); event.preventDefault(); }
      return;
    }
    const active = document.activeElement;
    const directions = { ArrowRight: [1, 0], ArrowLeft: [-1, 0], ArrowDown: [0, 1], ArrowUp: [0, -1] };
    if (directions[event.key] && !$('player-screen').classList.contains('hidden')) return;
    if (directions[event.key]) {
      const [dx, dy] = directions[event.key];
      const candidates = [...document.querySelectorAll('#catalog-screen button:not(:disabled), #login-screen button:not(:disabled), #login-screen input:not(:disabled)')]
        .filter(el => el.getClientRects().length > 0 && !el.classList.contains('hidden'));
      const origin = active?.getBoundingClientRect();
      if (origin) {
        const ox = origin.left + origin.width / 2, oy = origin.top + origin.height / 2;
        let best = null, bestScore = Infinity;
        for (const candidate of candidates) {
          if (candidate === active) continue;
          const rect = candidate.getBoundingClientRect();
          const x = rect.left + rect.width / 2, y = rect.top + rect.height / 2;
          const vx = x - ox, vy = y - oy;
          const primary = dx ? vx * dx : vy * dy;
          const secondary = dx ? Math.abs(vy) : Math.abs(vx);
          if (primary <= 4) continue;
          const score = primary + secondary * 2.2;
          if (score < bestScore) { best = candidate; bestScore = score; }
        }
        if (best) { best.focus(); event.preventDefault(); }
      }
    }
    if (event.key === 'Enter' && active && active.tagName === 'BUTTON') active.click();
  });
  window.addEventListener('load', () => {
    registerSamsungRemoteKeys();
    try {
      const saved = JSON.parse(localStorage.getItem('tvi-profile') || 'null');
      if (saved?.server && saved?.username) { $('server').value = saved.server; $('username').value = saved.username; }
    } catch (_) {}
  });
})();
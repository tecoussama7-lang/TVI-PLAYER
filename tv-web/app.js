(() => {
  'use strict';
  const $ = (id) => document.getElementById(id);
  const state = { server: '', username: '', password: '', section: 'live', categories: [], categoryId: null, streams: [], cache: new Map(), focusIndex: 0 };
  const sections = {
    live: { title: 'Live TV', cats: 'get_live_categories', items: 'get_live_streams', ext: 'ts' },
    movie: { title: 'Movies', cats: 'get_vod_categories', items: 'get_vod_streams', ext: 'mp4' },
    series: { title: 'Series', cats: 'get_series_categories', items: 'get_series', ext: 'mp4' }
  };
  const apiUrl = (action, extra = '') => {
    const base = state.server.replace(/\/+$/, '');
    const q = new URLSearchParams({ username: state.username, password: state.password, action });
    if (extra) q.set('category_id', extra);
    return base + '/player_api.php?' + q.toString();
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
  function show(id) { ['login-screen','catalog-screen','player-screen'].forEach(x => $(x).classList.toggle('hidden', x !== id)); }
  function setStatus(message) { $('status').textContent = message; }
  function validArray(value) { return Array.isArray(value) ? value : []; }
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
    state.categoryId = id;
    renderCategories();
    const cacheKey = state.section + ':' + id;
    $('media-grid').replaceChildren();
    if (state.cache.has(cacheKey)) {
      state.streams = state.cache.get(cacheKey); renderMedia(); setStatus('');
      return;
    }
    setStatus('Loading ' + (sections[state.section].title.toLowerCase()) + '…');
    try {
      const data = validArray(await fetchJson(apiUrl(sections[state.section].items, id)));
      state.streams = data;
      state.cache.set(cacheKey, data);
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
      button.append(poster, title); button.addEventListener('click', () => play(item)); fragment.append(button);
    });
    root.append(fragment);
    state.focusIndex = 0;
  }
  function play(item) {
    if (state.section === 'series') { setStatus('Series episode playback requires get_series_info support; choose an episode in the series detail flow.'); return; }
    const id = itemId(item);
    const ext = state.section === 'live' ? (item.container_extension || 'ts') : (item.container_extension || 'mp4');
    const path = state.section === 'live' ? 'live' : 'movie';
    const url = state.server.replace(/\/+$/, '') + '/' + path + '/' + encodeURIComponent(state.username) + '/' + encodeURIComponent(state.password) + '/' + encodeURIComponent(id) + '.' + ext;
    $('video-player').src = url; $('playing-title').textContent = itemName(item); show('player-screen');
    $('video-player').play().catch(() => {});
  }
  function closePlayer() { $('video-player').pause(); $('video-player').removeAttribute('src'); $('video-player').load(); show('catalog-screen'); }
  $('login-form').addEventListener('submit', connect);
  $('section-nav').addEventListener('click', event => { const button = event.target.closest('[data-section]'); if (button) loadSection(button.dataset.section); });
  $('change-account').addEventListener('click', () => { state.cache.clear(); $('password').value = ''; show('login-screen'); });
  $('close-player').addEventListener('click', closePlayer);
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' || event.key === 'Backspace') {
      if (!$('player-screen').classList.contains('hidden')) { closePlayer(); event.preventDefault(); }
    }
    if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
      const active = document.activeElement;
      if (active?.classList.contains('media-card')) {
        const cards = [...document.querySelectorAll('.media-card')];
        const next = cards.indexOf(active) + (event.key === 'ArrowRight' ? 1 : -1);
        if (cards[next]) { cards[next].focus(); event.preventDefault(); }
      }
    }
  });
  try {
    const saved = JSON.parse(localStorage.getItem('tvi-profile') || 'null');
    if (saved?.server && saved?.username) { $('server').value = saved.server; $('username').value = saved.username; }
  } catch (_) {}
})();
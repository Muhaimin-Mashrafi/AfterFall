// AfterFall — Cinematic presentation layer.
// Pure presentation: renders backend state and events; it never computes simulation facts.
(function () {
  'use strict';

  const reduced = () => window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const store = {
    get(k) { try { return window.sessionStorage.getItem(k); } catch (e) { return null; } },
    set(k, v) { try { window.sessionStorage.setItem(k, v); } catch (e) { /* storage unavailable */ } }
  };

  function el(tag, cls, text) {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined) n.textContent = text;
    return n;
  }

  // ---------- Chapters ----------
  const ROMAN = ['', 'I', 'II', 'III', 'IV'];
  const TAGLINES = {
    1: 'The record begins.',
    2: 'Something gives way.',
    3: 'The record does not match.',
    4: 'What remains is kept.'
  };

  // "CHAPTER_2_THE_FRACTURE" -> { n: 2, roman: 'II', title: 'The Fracture' }
  function chapterInfo(key) {
    const m = /^CHAPTER_(\d+)_(.+)$/.exec(String(key || ''));
    if (!m) return null;
    const n = parseInt(m[1], 10);
    const title = m[2].toLowerCase().split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
    return { n, roman: ROMAN[n] || String(n), title, tagline: TAGLINES[n] || '' };
  }

  let chapterCard = null;
  let chapterTimer = null;

  function ensureChapterCard() {
    if (chapterCard) return chapterCard;
    chapterCard = el('div', 'cine-overlay cine-grain chapter-card');
    chapterCard.setAttribute('role', 'dialog');
    chapterCard.setAttribute('aria-label', 'Chapter title');
    chapterCard.innerHTML =
      '<div class="cc-numeral" aria-hidden="true"></div>' +
      '<div class="cc-body"><div class="cc-kicker"></div><div class="cc-rule"></div>' +
      '<h1 class="cc-title"></h1><div class="cc-sub"></div></div>' +
      '<div class="cine-hint">Click to continue</div>';
    chapterCard.addEventListener('click', hideChapterCard);
    document.body.appendChild(chapterCard);
    document.addEventListener('keydown', (e) => {
      if (chapterCard.classList.contains('on') && (e.key === 'Escape' || e.key === 'Enter' || e.key === ' ')) {
        hideChapterCard();
      }
    });
    return chapterCard;
  }

  function hideChapterCard() {
    if (!chapterCard) return;
    clearTimeout(chapterTimer);
    chapterCard.classList.remove('on');
  }

  function showChapterCard(key) {
    const info = chapterInfo(key);
    if (!info) return;
    const card = ensureChapterCard();
    card.querySelector('.cc-numeral').textContent = info.roman;
    card.querySelector('.cc-kicker').textContent = 'Chapter ' + info.roman;
    card.querySelector('.cc-title').textContent = info.title;
    card.querySelector('.cc-sub').textContent = info.tagline;
    // restart CSS animations
    card.classList.remove('on');
    void card.offsetWidth;
    card.classList.add('on');
    clearTimeout(chapterTimer);
    chapterTimer = setTimeout(hideChapterCard, reduced() ? 2500 : 4600);
  }

  // Show the card once per chapter per browser session; never on a pristine, not-yet-started run.
  function noteChapter(state) {
    if (!state || !state.chapter) return;
    const started = (state.tick || 0) > 0 || state.lifecycle === 'RUNNING';
    if (!started) return;
    if (store.get('af.chapterShown') !== state.chapter) {
      store.set('af.chapterShown', state.chapter);
      showChapterCard(state.chapter);
    }
  }

  // ---------- Crisis weight ----------
  const CRISIS_WORDS = { CRISIS_FAMINE: 'Famine', CRISIS_DISEASE: 'Outbreak' };
  let veil = null, band = null, bandWord = null;
  let lastCrisisType = null, lastCrisisTick = -999, lastDeathTick = -999, crisisUntilTick = -1;

  function ensureCrisisLayers() {
    if (veil) return;
    veil = el('div', 'crisis-veil');
    veil.setAttribute('aria-hidden', 'true');
    const steady = el('div', 'crisis-veil steady');
    steady.setAttribute('aria-hidden', 'true');
    band = el('div', 'crisis-band');
    band.setAttribute('aria-hidden', 'true');
    bandWord = el('div', 'cb-word');
    band.appendChild(bandWord);
    band.appendChild(el('div', 'cb-tag', 'CRISIS DECLARED'));
    document.body.append(steady, veil, band);
  }

  function flash(node, cls, ms) {
    node.classList.remove(cls);
    void node.offsetWidth;
    node.classList.add(cls);
    setTimeout(() => node.classList.remove(cls), ms);
  }

  // event: { type, tick } from /topic/events
  function crisisEvent(event) {
    if (!event) return;
    const tick = event.tick || 0;
    ensureCrisisLayers();
    if (CRISIS_WORDS[event.type]) {
      // The same crisis can emit every tick: announce only a new crisis or one after a long gap.
      if (event.type !== lastCrisisType || tick - lastCrisisTick >= 20) {
        bandWord.textContent = CRISIS_WORDS[event.type];
        veil.classList.remove('death');
        flash(band, 'show', reduced() ? 2500 : 3500);
        flash(veil, 'pulse', reduced() ? 2500 : 2700);
      }
      lastCrisisType = event.type;
      lastCrisisTick = tick;
      crisisUntilTick = tick + 12;
      document.body.classList.add('crisis-active');
    } else if (event.type === 'SURVIVOR_DEATH' && tick - lastDeathTick >= 8) {
      lastDeathTick = tick;
      veil.classList.add('death');
      flash(veil, 'pulse', reduced() ? 2500 : 2700);
    }
  }

  // Called on every completed tick: lets the lingering crisis tone fade out.
  function tickAdvanced(tick) {
    if (document.body.classList.contains('crisis-active') && tick > crisisUntilTick) {
      document.body.classList.remove('crisis-active');
    }
  }

  // ---------- Exposure reveal ----------
  let exposeOverlay = null;
  function ensureExpose() {
    if (exposeOverlay) return exposeOverlay;
    exposeOverlay = el('div', 'cine-overlay cine-grain expose-overlay');
    exposeOverlay.setAttribute('role', 'dialog');
    exposeOverlay.setAttribute('aria-label', 'Altered report exposed');
    exposeOverlay.innerHTML =
      '<div class="expose-wrap"><div class="expose-title">The record <span>was altered.</span></div>' +
      '<div class="expose-cols">' +
      '<div class="expose-col truth"><div class="expose-label">WHAT HAPPENED</div><div class="expose-text" data-k="truth"></div></div>' +
      '<div class="expose-col claim"><div class="expose-label">WHAT THE OBSERVER REPORTED</div><div class="expose-text" data-k="claim"></div></div>' +
      '</div><div class="expose-meta"></div></div>' +
      '<div class="cine-hint">Click to continue</div>';
    exposeOverlay.addEventListener('click', hideExpose);
    document.body.appendChild(exposeOverlay);
    document.addEventListener('keydown', (e) => {
      if (exposeOverlay.classList.contains('on') && (e.key === 'Escape' || e.key === 'Enter' || e.key === ' ')) hideExpose();
    });
    return exposeOverlay;
  }
  let exposeTimer = null;
  function hideExpose() {
    if (!exposeOverlay) return;
    clearTimeout(exposeTimer);
    exposeOverlay.classList.remove('on');
  }
  // item: evidence DTO { evidenceId, tick, policy, target, originalReport, alteredReport }
  function exposeAlteredReport(item) {
    if (!item) return;
    const o = ensureExpose();
    o.querySelector('[data-k="truth"]').textContent = item.originalReport || '';
    o.querySelector('[data-k="claim"]').textContent = item.alteredReport || '';
    o.querySelector('.expose-meta').textContent =
      [item.evidenceId, item.tick !== undefined ? 'TICK ' + item.tick : '', item.policy].filter(Boolean).join(' — ');
    o.classList.remove('on');
    void o.offsetWidth;
    o.classList.add('on');
    clearTimeout(exposeTimer);
    exposeTimer = setTimeout(hideExpose, reduced() ? 9000 : 14000);
  }

  // ---------- Final recording ----------
  let finalEl = null, finalCancel = null;
  function closeFinal() {
    if (finalCancel) finalCancel();
    if (finalEl) finalEl.classList.remove('on');
  }

  // recap: /api/recap payload. opts.onFullRecap: callback for the "full recap" button.
  function playFinalRecording(recap, opts) {
    if (!recap) return;
    opts = opts || {};
    if (finalEl) finalEl.remove();
    finalEl = el('div', 'cine-overlay cine-grain final-rec');
    finalEl.setAttribute('role', 'dialog');
    finalEl.setAttribute('aria-label', 'Final recording');
    const stage = el('div', 'fr-stage');
    const skip = el('button', 'btn btn-secondary fr-skip', 'SKIP TO END');
    const dots = el('div', 'fr-dots');
    finalEl.append(skip, stage, dots);
    document.body.appendChild(finalEl);
    void finalEl.offsetWidth;
    finalEl.classList.add('on');

    const fr = recap.finalRecording || {};
    let scenes = (fr.scenes || []).filter(s => s && s.narrativeText);
    // The recording can repeat the same line many times: show each distinct beat once.
    const seen = new Set();
    scenes = scenes.filter(s => !seen.has(s.narrativeText) && seen.add(s.narrativeText));
    // Prefer story beats over engine bookkeeping lines (selection only; no text is altered).
    const storyOnly = scenes.filter(s => !/backstop triggered|dwell \(/i.test(s.narrativeText));
    if (storyOnly.length >= 3) scenes = storyOnly;
    const closing = fr.closingLine || '';
    const timers = [];
    let step = -1, ended = false, advanceTimer = null;
    const sceneMs = reduced() ? 5000 : 5200;

    finalCancel = () => { ended = true; clearTimeout(advanceTimer); timers.forEach(clearTimeout); document.removeEventListener('keydown', onKey); };

    // Long runs: show a handful of beats, always the first, last and evenly spaced ones between.
    const MAX_SCENES = 12;
    let beats = scenes;
    if (scenes.length > MAX_SCENES) {
      beats = [];
      for (let i = 0; i < MAX_SCENES; i++) beats.push(scenes[Math.round(i * (scenes.length - 1) / (MAX_SCENES - 1))]);
    }
    dots.append(...beats.map(() => document.createElement('b')));

    function paintDots(i) {
      [...dots.children].forEach((d, j) => { d.className = j < i ? 'done' : (j === i ? 'now' : ''); });
    }

    function showScene(i) {
      stage.innerHTML = '';
      paintDots(i);
      const s = beats[i];
      const meta = el('div', 'fr-meta', 'Recording — tick ' + s.tick);
      const text = el('div', 'fr-text wide fr-fade-in', s.narrativeText);
      stage.append(meta, text);
    }

    function showStats() {
      stage.innerHTML = '';
      paintDots(beats.length);
      const wrap = el('div', 'fr-fade-in');
      wrap.appendChild(el('div', 'fr-meta', 'The record, in full'));
      const stats = el('div', 'fr-stats');
      const add = (n, l, red) => {
        const s = el('div', 'fr-stat');
        s.appendChild(el('div', 'n' + (red ? ' red' : ''), String(n)));
        s.appendChild(el('div', 'l', l));
        stats.appendChild(s);
      };
      add(recap.finalAliveSurvivors + ' / ' + recap.initialPopulation, 'REMAINED', false);
      add(recap.totalDeceasedSurvivors, 'LOST', true);
      add(recap.totalTicks, 'DAYS RECORDED', false);
      wrap.appendChild(stats);
      stage.appendChild(wrap);
    }

    function showClosing() {
      stage.innerHTML = '';
      paintDots(beats.length);
      // "The settlement fell silent. The Observer remains active." -> two staged lines
      const idx = closing.indexOf('. ');
      const first = idx > -1 ? closing.slice(0, idx + 1) : closing;
      const second = idx > -1 ? closing.slice(idx + 2) : '';
      const line = el('div', 'fr-close-line');
      line.appendChild(el('span', 'l1', first));
      if (second) line.appendChild(el('span', 'l2', second));
      const status = el('div', 'fr-status');
      status.innerHTML = '<i></i><span>OBSERVER — ACTIVE</span>';
      const actions = el('div', 'fr-actions');
      const full = el('button', 'btn btn-secondary', 'OPEN FULL RECAP');
      const close = el('button', 'btn btn-crimson', 'CLOSE');
      full.onclick = () => { closeFinal(); if (opts.onFullRecap) opts.onFullRecap(); };
      close.onclick = closeFinal;
      actions.append(full, close);
      stage.append(line, status, actions);
      void line.offsetWidth;
      [line, status, actions].forEach(n => n.classList.add('run'));
      skip.style.visibility = 'hidden';
      ended = true;
      clearTimeout(advanceTimer);
    }

    function next() {
      if (ended && step >= beats.length + 1) return;
      step++;
      if (step < beats.length) {
        showScene(step);
        advanceTimer = setTimeout(next, sceneMs);
      } else if (step === beats.length) {
        showStats();
        advanceTimer = setTimeout(next, reduced() ? 4500 : 5000);
      } else {
        showClosing();
      }
    }

    function toEnd() { clearTimeout(advanceTimer); step = beats.length + 1; showClosing(); }
    function onKey(e) {
      if (!finalEl.classList.contains('on')) return;
      if (e.key === 'Escape') { closeFinal(); return; }
      if (e.key === ' ' || e.key === 'ArrowRight' || e.key === 'Enter') {
        e.preventDefault();
        if (step <= beats.length) { clearTimeout(advanceTimer); next(); }
      }
    }
    skip.onclick = toEnd;
    finalEl.addEventListener('click', (e) => {
      if (e.target.closest('button')) return;
      if (step <= beats.length) { clearTimeout(advanceTimer); next(); }
    });
    document.addEventListener('keydown', onKey);
    next();
  }

  window.Cinema = {
    chapterInfo, showChapterCard, noteChapter,
    crisisEvent, tickAdvanced,
    exposeAlteredReport, playFinalRecording
  };
})();

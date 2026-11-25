// AfterFall — Story Screen Controller

document.addEventListener('DOMContentLoaded', () => {
  // UI Elements
  const wsPill = document.getElementById('ws-pill');
  const simStatePill = document.getElementById('sim-state-pill');
  const tickCounter = document.getElementById('tick-counter');
  const chapterLabel = document.getElementById('chapter-label');
  const streamFeed = document.getElementById('stream-feed');
  const streamCount = document.getElementById('stream-count');

  // Stockpiles & Stats
  const stockpileFood = document.getElementById('stockpile-food');
  const stockpileWater = document.getElementById('stockpile-water');
  const stockpileMeds = document.getElementById('stockpile-meds');
  const popAlive = document.getElementById('pop-alive');
  const popDead = document.getElementById('pop-dead');
  const popCritical = document.getElementById('pop-critical');

  // Control Buttons
  const btnStart = document.getElementById('btn-start');
  const btnPause = document.getElementById('btn-pause');
  const btnResume = document.getElementById('btn-resume');
  const btnStep = document.getElementById('btn-step');

  // Observer & Choice Modal
  const observerContainer = document.getElementById('observer-interventions-container');
  const statInterventions = document.getElementById('stat-interventions');
  const statApproved = document.getElementById('stat-approved');
  const statRejected = document.getElementById('stat-rejected');

  const choiceModal = document.getElementById('choice-modal');
  const choiceDilemmaTitle = document.getElementById('choice-dilemma-title');
  const choiceDilemmaDesc = document.getElementById('choice-dilemma-desc');
  const choiceOptionsList = document.getElementById('choice-options-list');

  let entryCount = 0;
  let activeChoiceId = null;

  // 1. Initial State Fetch
  async function refreshState() {
    try {
      const state = await ApiClient.getState();
      updateStateDisplay(state);
      await loadStory();
      await loadMetrics();
      await loadRecords();
    } catch (err) {
      console.error('Failed to fetch initial state:', err);
    }
  }

  function updateStateDisplay(state) {
    if (!state) return;

    tickCounter.textContent = state.tick ?? 0;
    const chInfo = window.Cinema && Cinema.chapterInfo(state.chapter);
    chapterLabel.textContent = chInfo ? `${chInfo.roman} · ${chInfo.title}`.toUpperCase() : (state.chapter ?? 'PROLOGUE').toUpperCase();
    if (window.Cinema) Cinema.noteChapter(state);

    // Simulation Pill
    const status = state.lifecycle || state.status || 'UNINITIALIZED';
    simStatePill.textContent = status;
    simStatePill.className = 'status-pill';
    if (status === 'RUNNING') simStatePill.classList.add('status-running');
    else if (status === 'PAUSED' || status === 'PAUSE_REQUESTED') simStatePill.classList.add('status-paused');
    else if (status === 'STOPPED' || status === 'TERMINATED') simStatePill.classList.add('status-offline');

    // Choice Moment Check
    if (state.hasPendingChoice) {
      ApiClient.getStory().then(story => {
        if (story && story.activeChoice) {
          presentChoice(story.activeChoice);
        }
      }).catch(() => {});
    } else {
      dismissChoice();
    }
  }

  // 2. Fetch Story & Captions
  async function loadStory() {
    try {
      const story = await ApiClient.getStory();
      if (story && story.recentCaptions && story.recentCaptions.length > 0) {
        streamFeed.innerHTML = '';
        entryCount = 0;
        story.recentCaptions.forEach(c => appendCaption(c));
      } else {
        // If no captions yet, maintain initial chronicler baseline count
        const existingEntries = streamFeed.querySelectorAll('.caption-entry');
        entryCount = existingEntries.length;
        streamCount.textContent = `${entryCount} ENTRIES`;
      }
    } catch (err) {
      console.warn('Could not load story captions:', err);
    }
  }

  function appendCaption(caption) {
    if (!caption) return;
    entryCount++;
    streamCount.textContent = `${entryCount} ENTRIES`;

    const entry = document.createElement('div');
    entry.className = 'caption-entry';
    if (caption.urgent || (caption.text && caption.text.toLowerCase().includes('death'))) {
      entry.classList.add('urgent');
    }
    // Severity drives presentation weight only; the text itself is untouched.
    if (caption.severity === 'CRITICAL') entry.classList.add('critical');
    else if (caption.severity === 'SIGNIFICANT') entry.classList.add('significant');

    const meta = document.createElement('div');
    meta.className = 'caption-meta';
    meta.innerHTML = `
      <span>TICK ${String(caption.tick).padStart(3, '0')}</span>
      <span>${caption.povEntityId ? 'SURVIVOR ' + caption.povEntityId : 'OVERVIEW'}</span>
    `;

    const text = document.createElement('div');
    text.className = 'caption-text';
    text.textContent = caption.text || caption.content || '';

    entry.appendChild(meta);
    entry.appendChild(text);
    streamFeed.appendChild(entry);

    // Scenario 4 Defect Fix: Cap rendered DOM nodes at 100 while preserving scroll
    while (streamFeed.children.length > 100) {
      streamFeed.removeChild(streamFeed.firstElementChild);
    }

    streamFeed.scrollTop = streamFeed.scrollHeight;
  }

  // 3. Load Metrics & Stockpiles
  async function loadMetrics() {
    try {
      const m = await ApiClient.getMetrics();
      if (!m) return;
      stockpileFood.textContent = m.foodQuantity ?? (m.stockpile ? m.stockpile.food : '—');
      stockpileWater.textContent = m.waterQuantity ?? (m.stockpile ? m.stockpile.water : '—');
      stockpileMeds.textContent = m.medicineQuantity ?? (m.stockpile ? m.stockpile.medicine : '—');
      popAlive.textContent = m.aliveSurvivors ?? '—';
      popDead.textContent = m.deceasedSurvivors ?? m.deadSurvivors ?? '0';
      popCritical.textContent = m.droppedMinorEvents ?? m.criticalSurvivors ?? '0';
    } catch (err) {
      console.warn('Could not load metrics:', err);
    }
  }

  // 4. Load Records & Interventions
  async function loadRecords() {
    try {
      const records = await ApiClient.getRecords();
      if (!records) return;

      const interventions = records.interventions || [];
      const total = interventions.length;
      const approved = interventions.filter(r => r.status === 'APPLIED' || r.status === 'APPROVED').length;
      const rejected = interventions.filter(r => r.status === 'REJECTED').length;

      statInterventions.textContent = records.totalInterventions ?? total;
      statApproved.textContent = records.approvedInterventions ?? approved;
      statRejected.textContent = records.rejectedInterventions ?? rejected;

      const pending = interventions.filter(r => r.status === 'PROPOSED' || r.status === 'PENDING');
      if (pending.length > 0) {
        observerContainer.innerHTML = '';
        pending.forEach(item => {
          const card = document.createElement('div');
          card.className = 'intervention-card';
          card.innerHTML = `
            <div class="mono text-xs text-crimson font-bold" style="margin-bottom:0.25rem;">
              DIRECTIVE ${item.id} — TICK ${item.tick}
            </div>
            <div class="text-xs text-ivory" style="margin-bottom:0.5rem;">
              Target: ${item.target} | Policy: ${item.type}
            </div>
            <div style="display:flex; gap:0.5rem;">
              <button class="btn btn-primary" style="padding:0.25rem 0.6rem; font-size:0.75rem;" onclick="handleApproveIntervention('${item.id}')">APPROVE</button>
              <button class="btn btn-secondary" style="padding:0.25rem 0.6rem; font-size:0.75rem;" onclick="handleRejectIntervention('${item.id}')">REJECT</button>
            </div>
          `;
          observerContainer.appendChild(card);
        });
      } else {
        observerContainer.innerHTML = '<div class="text-dim text-sm" style="font-style: italic;">No pending interventions.</div>';
      }
    } catch (err) {
      console.warn('Could not load records:', err);
    }
  }

  // 5. Choice Presentation
  function presentChoice(choice) {
    if (!choice) return;
    activeChoiceId = choice.id || choice.choiceId;
    choiceDilemmaTitle.textContent = choice.title || 'MOMENT OF HARD CHOICE';
    choiceDilemmaDesc.textContent = choice.dilemma || choice.description || 'A critical fork in the shelter’s survival path has emerged.';

    choiceOptionsList.innerHTML = '';
    const options = choice.options || [];
    options.forEach((opt, idx) => {
      const btn = document.createElement('button');
      btn.className = 'choice-option-btn';
      btn.innerHTML = `
        <div class="mono text-xs text-crimson font-bold" style="margin-bottom: 0.25rem;">OPTION ${String.fromCharCode(65 + idx)}</div>
        <div class="text-sm font-bold text-ivory" style="margin-bottom: 0.25rem;">${opt.text || opt.label}</div>
        <div class="text-xs text-dim">${opt.consequencePreview || opt.preview || 'Action will alter trajectory immediately.'}</div>
      `;
      btn.onclick = () => submitChoiceSelection(activeChoiceId, opt.id || idx);
      choiceOptionsList.appendChild(btn);
    });

    choiceModal.classList.add('active');
  }

  function dismissChoice() {
    activeChoiceId = null;
    choiceModal.classList.remove('active');
  }

  async function submitChoiceSelection(choiceId, optionId) {
    try {
      await ApiClient.submitChoice(choiceId, optionId);
      dismissChoice();
      await refreshState();
    } catch (err) {
      alert('Failed to submit choice: ' + err.message);
    }
  }

  // Global handlers for interventions
  window.handleApproveIntervention = async function(id) {
    try {
      await ApiClient.approveIntervention(id);
      await loadRecords();
    } catch (err) {
      alert('Error approving intervention: ' + err.message);
    }
  };

  window.handleRejectIntervention = async function(id) {
    try {
      await ApiClient.rejectIntervention(id);
      await loadRecords();
    } catch (err) {
      alert('Error rejecting intervention: ' + err.message);
    }
  };

  // 6. Simulation Controls Binding
  btnStart.onclick = async () => {
    try {
      const res = await ApiClient.startSimulation();
      updateStateDisplay(res);
    } catch (e) { alert(e.message); }
  };

  btnPause.onclick = async () => {
    try {
      const res = await ApiClient.pauseSimulation();
      updateStateDisplay(res);
    } catch (e) { alert(e.message); }
  };

  btnResume.onclick = async () => {
    try {
      const res = await ApiClient.resumeSimulation();
      updateStateDisplay(res);
    } catch (e) { alert(e.message); }
  };

  btnStep.onclick = async () => {
    try {
      const res = await ApiClient.stepSimulation();
      updateStateDisplay(res);
      await loadMetrics();
      await loadRecords();
    } catch (e) { alert(e.message); }
  };

  // 7. WebSocket Live Streaming
  WsClient.connect({
    onConnect: () => {
      wsPill.textContent = 'STREAM LIVE';
      wsPill.className = 'status-pill status-running';

      // Subscribe to Captions
      WsClient.subscribe('/topic/captions', (caption) => {
        appendCaption(caption);
      });

      // Subscribe to Events
      WsClient.subscribe('/topic/events', (event) => {
        if (window.Cinema && event) Cinema.crisisEvent(event);
        if (event && event.type === 'TICK_COMPLETED') {
          tickCounter.textContent = event.tick;
          if (window.Cinema) Cinema.tickAdvanced(event.tick);
          loadMetrics();
        } else if (event && (event.type === 'CHAPTER_TRANSITION' || event.type === 'CHAPTER_FORCED')) {
          ApiClient.getState().then(updateStateDisplay).catch(() => {});
        } else if (event && event.type === 'CHOICE_REQUESTED') {
          refreshState();
        }
      });
    },
    onDisconnect: () => {
      wsPill.textContent = 'RECONNECTING';
      wsPill.className = 'status-pill status-connecting';
    },
    onError: (err) => {
      console.warn('WS error:', err);
      wsPill.textContent = 'OFFLINE';
      wsPill.className = 'status-pill status-offline';
    }
  });

  // Scenario 5 Guard: Prevent accidental modal dismissal via Escape or outside clicks
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && choiceModal.classList.contains('active')) {
      e.preventDefault();
      // Choice pause cannot be escaped; Overseer must submit a decision
    }
  });

  choiceModal.addEventListener('click', (e) => {
    if (e.target === choiceModal) {
      e.stopPropagation();
      // Modal backdrop click cannot bypass pause
    }
  });

  // Start initialization
  refreshState();
});

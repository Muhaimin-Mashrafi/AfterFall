// AfterFall — Survivor Directory Controller

document.addEventListener('DOMContentLoaded', () => {
  const survivorsGrid = document.getElementById('survivors-grid');
  const dossierContent = document.getElementById('dossier-content');
  const survivorCountPill = document.getElementById('survivor-count-pill');

  let survivorsList = [];
  let selectedSurvivorId = null;

  async function loadSurvivors() {
    try {
      survivorsList = await ApiClient.getSurvivors();
      renderGrid(survivorsList);
      survivorCountPill.textContent = `${survivorsList.length} SURVIVORS REGISTERED`;

      // Select first survivor if none selected
      if (!selectedSurvivorId && survivorsList.length > 0) {
        selectSurvivor(survivorsList[0].id);
      }
    } catch (err) {
      console.error('Failed to load survivors:', err);
      survivorsGrid.innerHTML = `<div class="text-crimson text-sm">Failed to retrieve personnel records: ${err.message}</div>`;
    }
  }

  function renderGrid(survivors) {
    survivorsGrid.innerHTML = '';
    if (!survivors || survivors.length === 0) {
      survivorsGrid.innerHTML = '<div class="text-dim text-sm">No survivors recorded in shelter manifest.</div>';
      return;
    }

    survivors.forEach(s => {
      const card = document.createElement('div');
      card.className = 'survivor-card';
      const isAlive = s.isAlive ?? s.alive ?? true;
      if (!isAlive) card.classList.add('deceased');
      if (s.id === selectedSurvivorId) card.classList.add('selected');

      const health = s.needs ? s.needs.health : (s.health || 0);
      const hunger = s.needs ? s.needs.hunger : (s.hunger || 0);
      const thirst = s.needs ? s.needs.thirst : (s.thirst || 0);
      const displayName = s.name || `SURVIVOR #${s.id}`;

      card.innerHTML = `
        <div class="survivor-header">
          <div class="survivor-name">${escapeHtml(displayName)}</div>
          <span class="survivor-badge ${isAlive ? 'badge-alive' : 'badge-dead'}">
            ${isAlive ? 'ALIVE' : 'DECEASED'}
          </span>
        </div>
        <div class="text-xs text-dim mono" style="margin-bottom: 0.65rem;">
          REGION ${s.region ?? s.regionId ?? '0'} | ${escapeHtml(s.profession || 'SURVIVOR')}
        </div>

        <div class="vital-bar-row">
          <span class="vital-label">HLTH</span>
          <div class="vital-bar-track">
            <div class="vital-bar-fill ${health < 30 ? 'critical' : ''}" style="width: ${Math.max(0, Math.min(100, health))}%;"></div>
          </div>
          <span class="mono text-xs text-dim">${Math.round(health)}%</span>
        </div>

        <div class="vital-bar-row">
          <span class="vital-label">HNGR</span>
          <div class="vital-bar-track">
            <div class="vital-bar-fill ${hunger > 75 ? 'critical' : ''}" style="width: ${Math.max(0, Math.min(100, hunger))}%;"></div>
          </div>
          <span class="mono text-xs text-dim">${Math.round(hunger)}%</span>
        </div>

        <div class="vital-bar-row">
          <span class="vital-label">THST</span>
          <div class="vital-bar-track">
            <div class="vital-bar-fill ${thirst > 75 ? 'critical' : ''}" style="width: ${Math.max(0, Math.min(100, thirst))}%;"></div>
          </div>
          <span class="mono text-xs text-dim">${Math.round(thirst)}%</span>
        </div>
      `;

      card.onclick = () => selectSurvivor(s.id);
      survivorsGrid.appendChild(card);
    });
  }

  async function selectSurvivor(id) {
    selectedSurvivorId = id;

    // Update selection styling in grid
    document.querySelectorAll('.survivor-card').forEach(c => c.classList.remove('selected'));
    const cards = survivorsGrid.children;
    for (let i = 0; i < survivorsList.length; i++) {
      if (survivorsList[i].id === id && cards[i]) {
        cards[i].classList.add('selected');
        break;
      }
    }

    try {
      const s = await ApiClient.getSurvivor(id);
      renderDossier(s);
    } catch (err) {
      dossierContent.innerHTML = `<div class="text-crimson text-sm">Failed to retrieve individual file: ${err.message}</div>`;
    }
  }

  function renderDossier(s) {
    if (!s) return;

    const isAlive = s.isAlive ?? s.alive ?? true;
    const health = s.needs ? s.needs.health : (s.health || 0);
    const hunger = s.needs ? s.needs.hunger : (s.hunger || 0);
    const thirst = s.needs ? s.needs.thirst : (s.thirst || 0);
    const fatigue = s.needs ? s.needs.fatigue : (s.exhaustion || 0);
    const displayName = s.name || `SURVIVOR #${s.id}`;

    dossierContent.innerHTML = `
      <div style="border-bottom: 2px solid var(--af-crimson-core); padding-bottom: 0.75rem; margin-bottom: 1rem;">
        <h2 style="font-family: var(--af-font-serif); font-size: 1.5rem; margin: 0 0 0.25rem 0; color: var(--af-ivory-primary);">
          ${escapeHtml(displayName)}
        </h2>
        <div class="mono text-xs text-dim">
          ID: ${escapeHtml(s.id)} | SECTOR: REGION ${s.region ?? s.regionId ?? '0'} | ${escapeHtml(s.profession || 'CITIZEN')}
        </div>
      </div>

      <div class="dossier-row">
        <span class="text-dim">Status:</span>
        <span class="mono font-bold ${isAlive ? 'text-ivory' : 'text-crimson'}">${isAlive ? 'FUNCTIONAL' : 'CASUALTY (TERMINATED)'}</span>
      </div>
      <div class="dossier-row">
        <span class="text-dim">Dominant Trait:</span>
        <span class="mono text-crimson font-bold">${escapeHtml(s.personality || s.dominantTrait || 'UNDETERMINED')}</span>
      </div>
      <div class="dossier-row">
        <span class="text-dim">Vital Health:</span>
        <span class="mono text-ivory">${Math.round(health)} / 100</span>
      </div>
      <div class="dossier-row">
        <span class="text-dim">Hunger Index:</span>
        <span class="mono text-ivory">${Math.round(hunger)} / 100</span>
      </div>
      <div class="dossier-row">
        <span class="text-dim">Dehydration:</span>
        <span class="mono text-ivory">${Math.round(thirst)} / 100</span>
      </div>
      <div class="dossier-row">
        <span class="text-dim">Exhaustion:</span>
        <span class="mono text-ivory">${Math.round(fatigue)} / 100</span>
      </div>

      <div style="margin-top: 1.25rem;">
        <div class="panel-header">RELATIONSHIPS & MEMORIES</div>
        <div class="text-xs text-dim" style="line-height: 1.6; margin-top: 0.5rem;">
          ${s.memories && s.memories.length > 0
            ? s.memories.map(m => `&bull; ${escapeHtml(m)}<br/>`).join('')
            : 'No abnormal psychological fixation logged for this subject.'
          }
        </div>
      </div>
    `;
  }

  function escapeHtml(text) {
    if (!text) return '';
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  loadSurvivors();
});

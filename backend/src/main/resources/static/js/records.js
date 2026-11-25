// AfterFall — Archival Records Controller

document.addEventListener('DOMContentLoaded', () => {
  const recordsTableBody = document.getElementById('records-table-body');
  const archivedCountPill = document.getElementById('archived-count-pill');
  const evidenceGrid = document.getElementById('evidence-grid');
  const evidenceCountLabel = document.getElementById('evidence-count-label');
  const btnViewRecap = document.getElementById('btn-view-recap');
  const btnCloseRecap = document.getElementById('btn-close-recap');
  const recapModal = document.getElementById('recap-modal');
  const recapSummaryContent = document.getElementById('recap-summary-content');
  const scenesList = document.getElementById('scenes-list');

  async function loadData() {
    await loadRecords();
    await loadEvidence();
  }

  async function loadRecords() {
    try {
      const recordsData = await ApiClient.getRecords();
      renderTable(recordsData);
    } catch (err) {
      console.error('Failed to load archival records:', err);
      recordsTableBody.innerHTML = `
        <tr>
          <td colspan="5" class="text-crimson" style="padding: 1.5rem; text-align: center;">
            Failed to load archival records: ${escapeHtml(err.message)}
          </td>
        </tr>
      `;
    }
  }

  async function loadEvidence() {
    try {
      const evidenceList = await ApiClient.getEvidence();
      renderEvidence(evidenceList);
    } catch (err) {
      console.warn('Failed to load evidence items:', err);
      if (evidenceGrid) {
        evidenceGrid.innerHTML = `<div class="text-dim text-sm">Evidence telemetry currently offline.</div>`;
      }
    }
  }

  function renderTable(data) {
    const list = data?.interventions || data?.records || [];
    if (!list || list.length === 0) {
      recordsTableBody.innerHTML = `
        <tr>
          <td colspan="5" class="text-dim" style="padding: 2rem; text-align: center; font-style: italic;">
            No recovered interventions logged yet in this timeline.
          </td>
        </tr>
      `;
      archivedCountPill.textContent = '0 RECORDS FOUND';
      return;
    }

    archivedCountPill.textContent = `${list.length} RECORDS CATALOGUED`;
    recordsTableBody.innerHTML = '';

    list.forEach(r => {
      const tr = document.createElement('tr');

      const status = (r.status || 'PENDING').toUpperCase();
      let statusClass = 'status-pending';
      if (status === 'APPROVED' || status === 'APPLIED') statusClass = 'status-approved';
      else if (status === 'REJECTED') statusClass = 'status-rejected';

      tr.innerHTML = `
        <td class="mono font-bold">${r.tick ?? '0'}</td>
        <td class="mono text-xs text-dim">${escapeHtml(r.target || 'GLOBAL')}</td>
        <td class="truth-col">${escapeHtml(r.originalReport || r.originalTruth || r.reason || 'Unrecorded ground truth')}</td>
        <td class="altered-col">${escapeHtml(r.alteredReport || r.action || 'Sanitized for psychological stability')}</td>
        <td>
          <span class="status-badge ${statusClass}">${status}</span>
        </td>
      `;

      recordsTableBody.appendChild(tr);
    });
  }

  function renderEvidence(items) {
    if (!evidenceGrid) return;
    if (!items || items.length === 0) {
      evidenceGrid.innerHTML = `<div class="text-dim text-sm" style="font-style: italic;">No Observer discrepancies uncovered yet. Advance simulation to generate interventions.</div>`;
      if (evidenceCountLabel) evidenceCountLabel.textContent = `0 DISCOVERED`;
      return;
    }

    const discoveredCount = items.filter(e => e.isDiscovered).length;
    if (evidenceCountLabel) evidenceCountLabel.textContent = `${discoveredCount} / ${items.length} DISCOVERED`;

    evidenceGrid.innerHTML = '';
    items.forEach(ev => {
      const card = document.createElement('div');
      card.className = `evidence-card ${ev.isDiscovered ? 'discovered' : 'undiscovered'}`;
      card.dataset.evidenceId = ev.evidenceId;

      card.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:baseline;">
          <span class="mono text-xs text-crimson font-bold">${escapeHtml(ev.evidenceId)} — TICK ${ev.tick}</span>
          <span class="status-badge ${ev.isDiscovered ? 'status-approved' : 'status-pending'}">
            ${ev.isDiscovered ? 'DECODED' : 'ENCRYPTED'}
          </span>
        </div>
        <div class="mono text-xs text-dim">POLICY: ${escapeHtml(ev.policy)} | TARGET: ${escapeHtml(ev.target)}</div>
        <div class="mono text-xs text-crimson">ANOMALY: ${escapeHtml(ev.discrepancyType)}</div>

        ${ev.isDiscovered ? `
          <div style="border-left: 2px solid var(--af-ivory-primary); padding-left: 0.5rem; margin-top: 0.5rem;">
            <div class="mono text-xs text-dim">GROUND TRUTH:</div>
            <div class="text-xs text-ivory">${escapeHtml(ev.originalReport)}</div>
          </div>
          <div style="border-left: 2px solid var(--af-crimson-core); padding-left: 0.5rem; margin-top: 0.35rem;">
            <div class="mono text-xs text-crimson">OBSERVER CLAIM:</div>
            <div class="text-xs text-dim">${escapeHtml(ev.alteredReport)}</div>
          </div>
        ` : `
          <div class="text-xs text-dim" style="font-style:italic; margin: 0.5rem 0;">
            Classified psychological manipulation directive. Investigate to decrypt discrepancies.
          </div>
          <button class="btn btn-secondary" style="font-size: 0.75rem; padding: 0.35rem 0.65rem;" onclick="handleDiscoverEvidence('${ev.evidenceId}')">
            [ INVESTIGATE &amp; DECRYPT ]
          </button>
        `}
      `;
      evidenceGrid.appendChild(card);
    });
  }

  window.handleDiscoverEvidence = async function(id) {
    try {
      await ApiClient.discoverEvidence(id);
      const items = await ApiClient.getEvidence();
      renderEvidence(items);
      const found = (items || []).find(e => e.evidenceId === id);
      if (found && window.Cinema) Cinema.exposeAlteredReport(found);
      const card = evidenceGrid && evidenceGrid.querySelector(`[data-evidence-id="${id}"]`);
      if (card) card.classList.add('just-decoded');
    } catch (err) {
      alert('Error decrypting evidence: ' + err.message);
    }
  };

  // Final Recap Modal Binding
  if (btnViewRecap && recapModal) {
    btnViewRecap.onclick = async () => {
      try {
        const recap = await ApiClient.getRecap();
        renderRecapModal(recap);
        if (window.Cinema) {
          Cinema.playFinalRecording(recap, { onFullRecap: () => recapModal.classList.add('active') });
        } else {
          recapModal.classList.add('active');
        }
      } catch (err) {
        alert('Could not load final recap: ' + err.message);
      }
    };
  }

  if (btnCloseRecap && recapModal) {
    btnCloseRecap.onclick = () => {
      recapModal.classList.remove('active');
    };
  }

  function renderRecapModal(recap) {
    if (!recap) return;

    recapSummaryContent.innerHTML = `
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
        <div class="panel-box">
          <div class="mono text-xs text-dim">TOTAL TICKS</div>
          <div class="mono text-lg font-bold text-ivory">${recap.totalTicks}</div>
        </div>
        <div class="panel-box">
          <div class="mono text-xs text-dim">SURVIVORS ALIVE</div>
          <div class="mono text-lg font-bold text-ivory">${recap.finalAliveSurvivors} / ${recap.initialPopulation}</div>
        </div>
        <div class="panel-box">
          <div class="mono text-xs text-dim">CASUALTIES</div>
          <div class="mono text-lg font-bold text-crimson">${recap.totalDeceasedSurvivors}</div>
        </div>
        <div class="panel-box">
          <div class="mono text-xs text-dim">EVIDENCE DECODED</div>
          <div class="mono text-lg font-bold text-ivory">${recap.discoveredEvidenceIds ? recap.discoveredEvidenceIds.length : 0}</div>
        </div>
      </div>

      <div class="panel-box" style="margin-bottom: 1.5rem;">
        <div class="panel-header">FINAL STOCKPILE RESERVES</div>
        <div class="mono text-xs text-ivory">
          FOOD: ${recap.finalResources?.food ?? 0} | WATER: ${recap.finalResources?.water ?? 0} | MEDICINE: ${recap.finalResources?.medicine ?? 0}
        </div>
      </div>
    `;

    scenesList.innerHTML = '';
    const scenes = recap.finalRecording?.scenes || [];
    if (scenes.length === 0) {
      scenesList.innerHTML = `<div class="text-dim text-xs font-italic">No scenes logged in this run.</div>`;
    } else {
      scenes.forEach(s => {
        const item = document.createElement('div');
        item.style.padding = '0.5rem 0.75rem';
        item.style.background = 'rgba(255,255,255,0.02)';
        item.style.borderLeft = '2px solid var(--af-crimson-core)';
        item.innerHTML = `
          <div class="mono text-xs text-dim">TICK ${s.tick} — ${escapeHtml(s.chapter)}</div>
          <div style="font-family: var(--af-font-serif); font-size: 0.95rem; color: var(--af-ivory-primary); margin-top: 0.25rem;">
            ${escapeHtml(s.narrativeText)}
          </div>
        `;
        scenesList.appendChild(item);
      });
    }

    const closingLineEl = document.getElementById('recap-closing-line');
    if (closingLineEl && recap.finalRecording?.closingLine) {
      closingLineEl.textContent = `"${recap.finalRecording.closingLine}"`;
    }
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

  loadData();
});

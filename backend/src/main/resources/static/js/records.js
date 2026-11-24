// AfterFall — Archival Records Controller

document.addEventListener('DOMContentLoaded', () => {
  const recordsTableBody = document.getElementById('records-table-body');
  const archivedCountPill = document.getElementById('archived-count-pill');

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

  function escapeHtml(text) {
    if (!text) return '';
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  loadRecords();
});

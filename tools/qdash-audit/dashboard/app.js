/**
 * app.js — Qdash Audit Dashboard frontend logic
 * Pure vanilla ES2020+, no external dependencies, no build step.
 * Reads JSON files from ../reports/ via fetch (works when served locally).
 */

'use strict';

// ─── Constants ───────────────────────────────────────────────────────────────
const SEVERITY_ORDER  = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'];
const SEVERITY_LABELS = {
  CRITICAL: 'حرج',
  HIGH:     'عالي',
  MEDIUM:   'متوسط',
  LOW:      'منخفض',
  INFO:     'معلومة',
};

const CATEGORY_ICONS = {
  Architecture: '🏗️',
  Database:     '🗄️',
  Threading:    '⚡',
  RTL:          '🔄',
  Duplication:  '♊',
  Size:         '📏',
  Orphan:       '👻',
};

const REPORTS_BASE = '../reports';
const INDEX_FILE   = `${REPORTS_BASE}/index.json`;

// ─── State ────────────────────────────────────────────────────────────────────
const state = {
  tab:          'overview',
  history:      [],       // ScanSummary[]
  currentReport: null,    // ScanReport | null
  selectedScanId: null,   // string | null (for history drill-down)
  drillReport:   null,    // ScanReport | null
};

// ─── DOM references ───────────────────────────────────────────────────────────
const $ = id => document.getElementById(id);
const $$ = sel => document.querySelectorAll(sel);

// ─── Initialisation ───────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  setupTabs();
  setupScanButton();
  setupModalClose();
  await loadAll();
});

async function loadAll() {
  showLoading(true);
  try {
    // Offline mode: audit.py writes data.js with window.AUDIT_DATA embedded
    if (window.AUDIT_DATA) {
      state.history = (window.AUDIT_DATA.history || []).sort(
        (a, b) => b.started_at.localeCompare(a.started_at)
      );
      state.currentReport = window.AUDIT_DATA.current || null;
    } else {
      // Server mode fallback (requires local HTTP server)
      await loadIndex();
    }
    renderCurrentTab();
    // Update last-scan label in topbar
    if (state.currentReport) {
      const el = $('last-scan-label');
      if (el) el.textContent = 'آخر فحص: ' + new Date(state.currentReport.started_at).toLocaleString('ar-DZ');
    }
  } catch(e) {
    showEmpty('overview-panel', 'لا توجد بيانات', 'قم بتشغيل الفحص أولاً عبر terminal', '📭');
    showEmpty('history-panel', 'لا يوجد سجل', 'ستظهر نتائج الفحص هنا بعد التشغيل', '📋');
  } finally {
    showLoading(false);
  }
}

// ─── Data loading ─────────────────────────────────────────────────────────────
async function loadIndex() {
  const resp = await fetch(INDEX_FILE + '?v=' + Date.now());
  if (!resp.ok) throw new Error('No index.json found');
  state.history = (await resp.json()).sort(
    (a, b) => b.started_at.localeCompare(a.started_at)
  );

  if (state.history.length > 0) {
    state.currentReport = await loadReport(state.history[0].report_file);
  }
}

async function loadReport(filename) {
  const resp = await fetch(`${REPORTS_BASE}/${filename}?v=` + Date.now());
  if (!resp.ok) throw new Error(`Cannot load report: ${filename}`);
  return await resp.json();
}

// ─── Tab switching ─────────────────────────────────────────────────────────────
function setupTabs() {
  $$('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const target = tab.dataset.tab;
      if (state.tab === target) return;
      state.tab = target;
      $$('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === target));
      $$('.tab-panel').forEach(p => p.classList.toggle('hidden', p.id !== `${target}-panel`));
      renderCurrentTab();
    });
  });
}

function renderCurrentTab() {
  switch (state.tab) {
    case 'overview': renderOverview(); break;
    case 'history':  renderHistory();  break;
    case 'dbhealth': renderDbHealth(); break;
  }
}

// ─── Overview ─────────────────────────────────────────────────────────────────
function renderOverview() {
  const report = state.currentReport;
  if (!report) {
    renderStats(null);
    showEmpty('issues-list', 'لا توجد بيانات فحص', 'قم بتشغيل audit.py ثم أعد تحميل الصفحة', '📭');
    return;
  }

  renderStats(report);
  renderIssueList(report.issues || [], 'issues-list');
  updateTabBadge('overview', report.total_issues);
}

function renderStats(report) {
  const el = $('stats-container');
  if (!el) return;

  if (!report) {
    el.innerHTML = `
      <div class="stat-card">
        <div class="stat-label">آخر فحص</div>
        <div class="stat-value" style="font-size:1rem;color:var(--text-muted)">لم يتم الفحص بعد</div>
      </div>`;
    return;
  }

  const sevs = SEVERITY_ORDER.map(s => {
    const count = (report.issues_by_severity || {})[s] || 0;
    return `<div class="stat-card ${s.toLowerCase()}">
      <div class="stat-label">${SEVERITY_LABELS[s] || s}</div>
      <div class="stat-value">${count}</div>
      <div class="stat-sub">${s}</div>
    </div>`;
  }).join('');

  const lastScan = new Date(report.started_at).toLocaleString('ar-DZ');
  const newIss   = (report.new_issue_ids  || []).length;
  const resIss   = (report.resolved_issue_ids || []).length;

  el.innerHTML = `
    <div class="stat-card">
      <div class="stat-label">إجمالي المشكلات</div>
      <div class="stat-value" style="color:var(--text-primary)">${report.total_issues}</div>
      <div class="stat-sub">${report.total_files_scanned} ملف تم فحصه</div>
    </div>
    ${sevs}
    <div class="stat-card ${newIss > 0 ? 'critical' : 'success'}">
      <div class="stat-label">مشكلات جديدة</div>
      <div class="stat-value">${newIss}</div>
      <div class="stat-sub">${resIss} تم حلها</div>
    </div>
    <div class="stat-card">
      <div class="stat-label">آخر فحص</div>
      <div class="stat-value" style="font-size:1rem;line-height:1.3">${lastScan}</div>
      <div class="stat-sub">${report.duration_seconds}s · إصدار DB: v${report.schema_version || '?'}</div>
    </div>`;
}

function renderIssueList(issues, containerId) {
  const el = $(containerId);
  if (!el) return;

  if (issues.length === 0) {
    el.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">✅</div>
        <div class="empty-title">لا توجد مشكلات</div>
        <div class="empty-sub">الكود في حالة جيدة</div>
      </div>`;
    return;
  }

  // Group by severity
  const grouped = {};
  SEVERITY_ORDER.forEach(s => grouped[s] = []);
  issues.forEach(iss => (grouped[iss.severity] || (grouped[iss.severity] = [])).push(iss));

  el.innerHTML = SEVERITY_ORDER.filter(s => grouped[s].length > 0).map(sev => {
    const group = grouped[sev];
    const icon  = { CRITICAL:'🔴', HIGH:'🟠', MEDIUM:'🟡', LOW:'🔵', INFO:'⚫' }[sev] || '•';
    return `
      <div class="severity-group">
        <div class="severity-group-header ${sev}" onclick="toggleGroup(this)">
          ${icon} ${SEVERITY_LABELS[sev] || sev}
          <span style="font-weight:400;font-size:0.82rem;opacity:0.8">(${group.length})</span>
          <span class="chevron">▾</span>
        </div>
        <div class="severity-group-body">
          ${group.map(renderIssueCard).join('')}
        </div>
      </div>`;
  }).join('');
}

function renderIssueCard(issue) {
  const catIcon = CATEGORY_ICONS[issue.category] || '•';
  const filePath = issue.file_path || '';
  const lineInfo = issue.line_start ? `:${issue.line_start}` : '';
  const snippet  = issue.code_snippet
    ? `<div class="issue-detail-section">
         <div class="issue-detail-label">مقتطف الكود</div>
         <div class="code-block">${escHtml(issue.code_snippet)}</div>
       </div>` : '';
  const affected = (issue.affected_files || []).length > 0
    ? `<div class="issue-detail-section">
         <div class="issue-detail-label">ملفات متأثرة</div>
         ${issue.affected_files.map(f => `<div class="code-path mt-1">${escHtml(f)}</div>`).join('')}
       </div>` : '';

  return `
    <div class="issue-card" onclick="toggleIssue(this)">
      <div class="issue-header">
        <div class="issue-rule-id">${escHtml(issue.rule_id)} ${catIcon}</div>
        <div class="issue-title-group">
          <div class="issue-title">${escHtml(issue.title)}</div>
          <div class="issue-file code-path">${escHtml(filePath)}${lineInfo}</div>
        </div>
        <div class="issue-chevron">▾</div>
      </div>
      <div class="issue-detail">
        <div class="issue-detail-section">
          <div class="issue-detail-label">الوصف</div>
          <div class="issue-description">${escHtml(issue.description)}</div>
        </div>
        ${snippet}
        ${affected}
        <div class="issue-detail-section">
          <div class="issue-detail-label">الأعراض المتوقعة في التطبيق</div>
          <div class="issue-symptom ar">${escHtml(issue.user_symptom || '—')}</div>
        </div>
        <div class="issue-detail-section">
          <div class="issue-detail-label">معرّف المشكلة</div>
          <div class="code-path">${escHtml(issue.id || '')}</div>
        </div>
      </div>
    </div>`;
}

// ─── History ──────────────────────────────────────────────────────────────────
function renderHistory() {
  const el = $('history-list');
  if (!el) return;

  updateTabBadge('history', state.history.length);

  if (state.history.length === 0) {
    el.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-title">لا يوجد سجل فحص</div>
        <div class="empty-sub">قم بتشغيل audit.py لبدء تتبع السجل</div>
      </div>`;
    return;
  }

  el.innerHTML = state.history.map((s, idx) => {
    const dt     = new Date(s.started_at);
    const dtStr  = dt.toLocaleDateString('ar-DZ', {year:'numeric',month:'short',day:'numeric'});
    const tmStr  = dt.toLocaleTimeString('ar-DZ', {hour:'2-digit',minute:'2-digit'});
    const sevBars = SEVERITY_ORDER.map(sev => {
      const c = (s.issues_by_severity || {})[sev] || 0;
      return c ? `<span class="badge badge-${sev}" style="font-size:0.7rem">${c} ${sev}</span>` : '';
    }).join(' ');
    const newC = s.new_issues    || 0;
    const resC = s.resolved_issues || 0;
    const isLatest = idx === 0;

    return `
      <div class="history-row ${isLatest ? 'active' : ''}" onclick="drillHistory('${escHtml(s.scan_id)}', '${escHtml(s.report_file)}', this)">
        <div>
          <div class="history-ts">${dtStr} ${tmStr} ${isLatest ? '<span class="badge badge-INFO">آخر فحص</span>' : ''}</div>
          <div style="margin-top:4px;display:flex;gap:4px;flex-wrap:wrap">${sevBars}</div>
        </div>
        <div class="history-stat">
          <div class="history-stat-val">${s.total_files_scanned}</div>
          <div class="history-stat-label">ملف</div>
        </div>
        <div class="history-stat">
          <div class="history-stat-val">${s.total_issues}</div>
          <div class="history-stat-label">مشكلة</div>
        </div>
        <div class="history-stat">
          <div class="history-stat-val delta-new">+${newC}</div>
          <div class="history-stat-label">جديدة</div>
        </div>
        <div class="history-stat">
          <div class="history-stat-val delta-resolved">-${resC}</div>
          <div class="history-stat-label">محلولة</div>
        </div>
        <div class="history-stat">
          <div class="history-stat-val" style="font-size:0.9rem;color:var(--text-muted)">${s.duration_seconds}s</div>
          <div class="history-stat-label">مدة</div>
        </div>
      </div>`;
  }).join('');
}

async function drillHistory(scanId, reportFile, rowEl) {
  $$('.history-row').forEach(r => r.classList.remove('active'));
  rowEl.classList.add('active');

  const drillEl = $('history-drill');
  if (!drillEl) return;

  drillEl.innerHTML = '<div class="empty-state"><div class="empty-icon">⏳</div><div class="empty-title">جاري التحميل…</div></div>';

  try {
    const report = await loadReport(reportFile);
    drillEl.innerHTML = `
      <div style="margin-bottom:12px;display:flex;align-items:center;gap:12px">
        <strong>تفاصيل الفحص</strong>
        <span class="badge badge-INFO">${new Date(report.started_at).toLocaleString('ar-DZ')}</span>
        <span class="badge badge-INFO">${report.total_issues} مشكلة</span>
        <span class="badge badge-INFO">${report.total_files_scanned} ملف</span>
      </div>
      <div id="drill-issues"></div>`;
    renderIssueList(report.issues || [], 'drill-issues');
  } catch (e) {
    drillEl.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-title">تعذّر تحميل التقرير</div></div>`;
  }
}

// ─── DB Health ────────────────────────────────────────────────────────────────
function renderDbHealth() {
  const el = $('db-content');
  if (!el) return;

  const report = state.currentReport;
  const db     = report?.db_health;

  if (!db) {
    el.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">🗄️</div>
        <div class="empty-title">لا توجد بيانات قاعدة البيانات</div>
        <div class="empty-sub">قم بتشغيل الفحص للحصول على معلومات Room Schema</div>
      </div>`;
    return;
  }

  const migChips = (db.migration_versions || []).map(([f, t]) =>
    `<span class="migration-chip ok">v${f}→v${t}</span>`
  ).join('');
  const missingChips = (db.missing_migrations || []).map(m =>
    `<span class="migration-chip missing">⚠ ${m}</span>`
  ).join('');
  const destructiveWarn = db.has_destructive_migration
    ? `<div class="badge badge-CRITICAL" style="margin-top:8px">⚠ fallbackToDestructiveMigration مُفعّل!</div>` : '';

  const entityRows = (db.entities || []).map(e => {
    const flagged = e.is_flagged;
    const reasons = (e.flag_reasons || []).join(', ');
    const idxStr  = (e.indices || []).join(', ') || '—';
    const fkStr   = (e.foreign_keys || []).join(', ') || '—';
    return `<tr>
      <td><span class="flag-dot ${flagged ? 'flagged' : ''}"></span> ${escHtml(e.name)}</td>
      <td>${escHtml(e.table_name)}</td>
      <td>${(e.columns || []).length}</td>
      <td>${escHtml(idxStr)}</td>
      <td>${escHtml(fkStr)}</td>
      <td>${flagged ? `<span style="color:var(--critical);font-size:0.75rem">${escHtml(reasons)}</span>` : '<span style="color:var(--success)">✓</span>'}</td>
    </tr>`;
  }).join('');

  el.innerHTML = `
    <div class="db-grid">
      <div class="db-card">
        <div class="db-card-title">معلومات عامة</div>
        <div style="display:flex;flex-direction:column;gap:10px">
          <div>
            <div class="issue-detail-label">إصدار المخطط الحالي</div>
            <div style="font-size:2rem;font-weight:700;color:var(--accent)">v${db.schema_version}</div>
          </div>
          <div>
            <div class="issue-detail-label">عدد الجداول</div>
            <div style="font-size:1.4rem;font-weight:600">${db.entity_count}</div>
          </div>
          <div>
            <div class="issue-detail-label">جداول بها مشاكل</div>
            <div style="font-size:1.4rem;font-weight:600;color:${db.flagged_entities.length > 0 ? 'var(--critical)' : 'var(--success)'}">
              ${db.flagged_entities.length}
            </div>
          </div>
          ${destructiveWarn}
        </div>
      </div>
      <div class="db-card">
        <div class="db-card-title">تغطية الهجرات (Migrations)</div>
        <div style="margin-bottom:8px;font-size:0.82rem;color:var(--text-muted)">
          ${db.migration_versions.length} هجرة مُعرَّفة
        </div>
        <div style="display:flex;flex-wrap:wrap">${migChips || '<span style="color:var(--text-muted);font-size:0.82rem">لا توجد هجرات</span>'}</div>
        ${missingChips ? `<div style="margin-top:12px"><div class="issue-detail-label">هجرات مفقودة</div>${missingChips}</div>` : ''}
      </div>
    </div>

    <div class="db-card" style="overflow-x:auto">
      <div class="db-card-title">قائمة الجداول</div>
      <table class="entity-table">
        <thead>
          <tr>
            <th>الكيان</th><th>اسم الجدول</th><th>الأعمدة</th>
            <th>الفهارس</th><th>المفاتيح الخارجية</th><th>الحالة</th>
          </tr>
        </thead>
        <tbody>${entityRows || '<tr><td colspan="6" style="text-align:center;color:var(--text-muted);padding:20px">لا توجد بيانات</td></tr>'}</tbody>
      </table>
    </div>`;
}

// ─── Scan button & modal ─────────────────────────────────────────────────────
function setupScanButton() {
  const btn = $('btn-scan');
  if (btn) btn.addEventListener('click', showScanModal);
  const closeBtn = $('modal-close');
  if (closeBtn) closeBtn.addEventListener('click', hideScanModal);
  const overlay = $('modal-overlay');
  if (overlay) overlay.addEventListener('click', e => {
    if (e.target === overlay) hideScanModal();
  });
}

function setupModalClose() {
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') hideScanModal();
  });
}

function showScanModal() {
  const overlay = $('modal-overlay');
  if (overlay) overlay.classList.add('visible');
}

function hideScanModal() {
  const overlay = $('modal-overlay');
  if (overlay) overlay.classList.remove('visible');
}

async function refreshAfterScan() {
  hideScanModal();
  showToast('⏳ جاري تحديث البيانات…');
  showLoading(true);
  try {
    await loadAll();
    showToast('✅ تم تحديث البيانات');
  } finally {
    showLoading(false);
  }
}

// ─── UI helpers ───────────────────────────────────────────────────────────────
function toggleGroup(header) {
  header.classList.toggle('collapsed');
  const body = header.nextElementSibling;
  if (body) body.style.display = header.classList.contains('collapsed') ? 'none' : '';
}

function toggleIssue(card) {
  card.classList.toggle('expanded');
}

function showEmpty(containerId, title, sub, icon = '📭') {
  const el = $(containerId);
  if (!el) return;
  el.innerHTML = `
    <div class="empty-state">
      <div class="empty-icon">${icon}</div>
      <div class="empty-title">${title}</div>
      <div class="empty-sub">${sub}</div>
    </div>`;
}

function showLoading(on) {
  const bar = $('loading-bar');
  if (!bar) return;
  bar.style.width = on ? '60%' : '100%';
  if (!on) setTimeout(() => { bar.style.width = '0%'; }, 300);
}

function showToast(msg) {
  const toast = $('toast');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('visible');
  setTimeout(() => toast.classList.remove('visible'), 3000);
}

function updateTabBadge(tabName, count) {
  const tab = document.querySelector(`.tab[data-tab="${tabName}"] .tab-badge`);
  if (tab) tab.textContent = count;
}

function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}


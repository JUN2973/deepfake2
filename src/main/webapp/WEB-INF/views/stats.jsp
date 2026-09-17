<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  request.setAttribute("activePage", "stats");
  String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>검증 통계 - DeepScan</title>
  <link rel="icon" type="image/svg+xml" href="<%= contextPath %>/resources/image/deepscan-mark.svg?v=30">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;600;700&display=swap" rel="stylesheet">
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
  <style>
    :root {
      color-scheme: dark;
      --page: #090b0e;
      --surface: #11151a;
      --surface-raised: #171c23;
      --line: #2a3038;
      --line-strong: #3a424d;
      --text: #f3f4f6;
      --muted: #929aa5;
      --blue: #4d8ff7;
      --cyan: #22c3d6;
      --green: #34d399;
      --amber: #fbbf24;
      --red: #fb7185;
    }

    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-width: 320px;
      background: var(--page);
      color: var(--text);
      font-family: "Noto Sans KR", sans-serif;
      letter-spacing: 0;
    }
    button, input { font: inherit; }
    button { color: inherit; }
    .page {
      width: min(1240px, calc(100% - 40px));
      margin: 0 auto;
    }
    .page { padding: 102px 0 64px; }
    .page-heading {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 24px;
      margin-bottom: 26px;
    }
    .eyebrow {
      margin-bottom: 7px;
      color: #79aaff;
      font-size: 12px;
      font-weight: 700;
    }
    h1 { margin: 0; font-size: 30px; line-height: 1.3; }
    .heading-copy { margin: 7px 0 0; color: var(--muted); font-size: 14px; }
    .data-badge {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      border: 1px solid rgba(52, 211, 153, 0.28);
      border-radius: 999px;
      background: rgba(52, 211, 153, 0.08);
      padding: 7px 10px;
      color: #6ee7b7;
      font-size: 12px;
      white-space: nowrap;
    }
    .filter-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 18px;
      margin-bottom: 18px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--surface);
      padding: 14px;
    }
    .segments { display: inline-flex; gap: 3px; border-radius: 7px; background: #0b0e12; padding: 3px; }
    .segment {
      min-width: 58px;
      border: 0;
      border-radius: 5px;
      background: transparent;
      padding: 8px 12px;
      color: var(--muted);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
    }
    .segment.active { background: #253044; color: #b9d2ff; }
    .date-controls { display: flex; align-items: center; gap: 8px; }
    .date-field {
      height: 38px;
      border: 1px solid var(--line-strong);
      border-radius: 6px;
      background: #0b0e12;
      padding: 0 10px;
      color: var(--text);
      font-size: 13px;
    }
    .date-separator { color: var(--muted); }
    .refresh-button {
      display: inline-flex;
      height: 38px;
      align-items: center;
      gap: 7px;
      border: 0;
      border-radius: 6px;
      background: var(--blue);
      padding: 0 14px;
      color: #fff;
      font-size: 13px;
      font-weight: 700;
      cursor: pointer;
    }
    .refresh-button:hover { background: #69a1f8; }
    .refresh-button:disabled { cursor: wait; opacity: 0.65; }
    .status-banner {
      display: none;
      align-items: center;
      gap: 9px;
      margin-bottom: 14px;
      border: 1px solid var(--line);
      border-radius: 7px;
      padding: 11px 13px;
      font-size: 13px;
    }
    .status-banner.visible { display: flex; }
    .status-banner.loading { border-color: rgba(77, 143, 247, 0.3); background: rgba(77, 143, 247, 0.08); color: #b9d2ff; }
    .status-banner.error { border-color: rgba(251, 113, 133, 0.3); background: rgba(251, 113, 133, 0.08); color: #fda4af; }
    .status-banner.empty { border-color: rgba(251, 191, 36, 0.28); background: rgba(251, 191, 36, 0.07); color: #fcd34d; }
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 18px;
    }
    .kpi {
      min-height: 138px;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--surface);
      padding: 19px;
    }
    .kpi-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
    .kpi-label { color: #b2bac5; font-size: 13px; font-weight: 600; }
    .kpi-icon {
      display: inline-flex;
      width: 32px;
      height: 32px;
      align-items: center;
      justify-content: center;
      border-radius: 7px;
    }
    .kpi-value { margin-top: 16px; font-size: 29px; font-weight: 700; line-height: 1; }
    .kpi-meta { margin-top: 10px; color: var(--muted); font-size: 12px; }
    .up { color: var(--green); }
    .blue { color: var(--blue); background: rgba(77, 143, 247, 0.12); }
    .cyan { color: var(--cyan); background: rgba(34, 195, 214, 0.12); }
    .amber { color: var(--amber); background: rgba(251, 191, 36, 0.12); }
    .red { color: var(--red); background: rgba(251, 113, 133, 0.12); }
    .dashboard-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.8fr);
      gap: 12px;
      margin-bottom: 12px;
    }
    .panel {
      min-width: 0;
      border: 1px solid var(--line);
      border-radius: 8px;
      background: var(--surface);
      padding: 20px;
    }
    .panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
    .panel-title { margin: 0; font-size: 16px; }
    .panel-caption { color: var(--muted); font-size: 12px; white-space: nowrap; }
    .chart-wrap { position: relative; width: 100%; height: 300px; }
    .donut-wrap { position: relative; width: min(100%, 270px); height: 230px; margin: 0 auto; }
    .legend-list { display: grid; gap: 9px; margin-top: 10px; }
    .legend-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; font-size: 13px; }
    .legend-name { display: flex; align-items: center; gap: 8px; color: #c7cdd5; }
    .legend-dot { width: 8px; height: 8px; border-radius: 50%; }
    .legend-value { font-weight: 700; }
    .bottom-grid { display: grid; grid-template-columns: 1.25fr 0.75fr; gap: 12px; }
    .report-table { width: 100%; border-collapse: collapse; }
    .report-table th,
    .report-table td { border-bottom: 1px solid var(--line); padding: 13px 9px; text-align: left; font-size: 13px; }
    .report-table th { color: var(--muted); font-size: 12px; font-weight: 500; }
    .report-table td:last-child,
    .report-table th:last-child { text-align: right; }
    .status {
      display: inline-flex;
      border-radius: 999px;
      padding: 4px 8px;
      font-size: 11px;
      font-weight: 700;
    }
    .status.pending { background: rgba(251, 191, 36, 0.1); color: #fcd34d; }
    .status.done { background: rgba(52, 211, 153, 0.1); color: #6ee7b7; }
    .insight-list { display: grid; gap: 16px; }
    .insight { display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 11px; }
    .insight-icon {
      display: flex;
      width: 34px;
      height: 34px;
      align-items: center;
      justify-content: center;
      border: 1px solid var(--line);
      border-radius: 7px;
      background: #0c1015;
    }
    .insight strong { display: block; margin: 1px 0 4px; font-size: 13px; }
    .insight p { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.65; }

    @media (max-width: 960px) {
      .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .dashboard-grid, .bottom-grid { grid-template-columns: 1fr; }
    }
    @media (max-width: 680px) {
      .page { width: min(100% - 24px, 1240px); padding-top: 90px; }
      .page-heading { align-items: flex-start; flex-direction: column; gap: 12px; }
      h1 { font-size: 25px; }
      .filter-bar { align-items: stretch; flex-direction: column; }
      .segments { width: 100%; }
      .segment { flex: 1; min-width: 0; padding-inline: 7px; }
      .date-controls { display: grid; grid-template-columns: 1fr 12px 1fr; }
      .date-field { min-width: 0; width: 100%; }
      .refresh-button { grid-column: 1 / -1; justify-content: center; }
      .kpi-grid { grid-template-columns: 1fr; }
      .kpi { min-height: 122px; }
      .panel { padding: 16px; }
      .panel-caption { display: none; }
      .chart-wrap { height: 250px; }
      .report-table th:nth-child(2), .report-table td:nth-child(2) { display: none; }
    }
  </style>
</head>
<body>
  <%@ include file="common/dashboard-nav.jspf" %>

  <main class="page">
    <div class="page-heading">
      <div>
        <div class="eyebrow">VERIFICATION STATISTICS</div>
        <h1>검증 통계</h1>
        <p class="heading-copy">전체 사용자의 이미지 검증 결과와 재검토 요청 현황을 기간별로 확인합니다.</p>
      </div>
      <span class="data-badge"><i data-lucide="database" width="14" height="14"></i> DB 통계</span>
    </div>

    <section class="filter-bar" aria-label="통계 조회 기간">
      <div class="segments" role="group" aria-label="빠른 기간 선택">
        <button type="button" class="segment active" data-days="7">7일</button>
        <button type="button" class="segment" data-days="30">30일</button>
        <button type="button" class="segment" data-days="90">90일</button>
      </div>
      <div class="date-controls">
        <input id="startDate" class="date-field" type="date" aria-label="시작일">
        <span class="date-separator">-</span>
        <input id="endDate" class="date-field" type="date" aria-label="종료일">
        <button id="refreshButton" class="refresh-button" type="button">
          <i data-lucide="refresh-cw" width="15" height="15"></i>
          조회
        </button>
      </div>
    </section>

    <div id="statusBanner" class="status-banner" role="status" aria-live="polite"></div>

    <section class="kpi-grid" aria-label="핵심 통계">
      <article class="kpi">
        <div class="kpi-top"><span class="kpi-label">전체 검증</span><span class="kpi-icon blue"><i data-lucide="scan-search" width="17" height="17"></i></span></div>
        <div id="totalValue" class="kpi-value">0건</div>
        <div id="totalMeta" class="kpi-meta">전체 사용자 누적 검증 수</div>
      </article>
      <article class="kpi">
        <div class="kpi-top"><span class="kpi-label">AI 이미지 판정</span><span class="kpi-icon cyan"><i data-lucide="sparkles" width="17" height="17"></i></span></div>
        <div id="aiRatioValue" class="kpi-value">0.0%</div>
        <div id="aiCountMeta" class="kpi-meta">0건이 AI 생성 의심으로 판정됨</div>
      </article>
      <article class="kpi">
        <div class="kpi-top"><span class="kpi-label">평균 신뢰도</span><span class="kpi-icon amber"><i data-lucide="gauge" width="17" height="17"></i></span></div>
        <div id="confidenceValue" class="kpi-value">0.0%</div>
        <div class="kpi-meta">전체 검증 결과의 평균 점수</div>
      </article>
      <article class="kpi">
        <div class="kpi-top"><span class="kpi-label">오탐 신고 비율</span><span class="kpi-icon red"><i data-lucide="flag" width="17" height="17"></i></span></div>
        <div id="reportRatioValue" class="kpi-value">0.0%</div>
        <div id="reportCountMeta" class="kpi-meta">재검토 요청 0건</div>
      </article>
    </section>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-heading">
          <h2 class="panel-title">일별 검증 추이</h2>
          <span class="panel-caption">검증 건수 / AI 이미지 판정</span>
        </div>
        <div class="chart-wrap"><canvas id="trendChart"></canvas></div>
      </section>
      <section class="panel">
        <div class="panel-heading">
          <h2 class="panel-title">판정 분포</h2>
          <span id="verdictTotalCaption" class="panel-caption">총 0건</span>
        </div>
        <div class="donut-wrap"><canvas id="verdictChart"></canvas></div>
        <div class="legend-list">
          <div class="legend-row"><span class="legend-name"><span class="legend-dot" style="background:#4d8ff7"></span>실제 이미지</span><span id="realLegendValue" class="legend-value">0건</span></div>
          <div class="legend-row"><span class="legend-name"><span class="legend-dot" style="background:#fb7185"></span>AI 생성 의심</span><span id="aiLegendValue" class="legend-value">0건</span></div>
          <div class="legend-row"><span class="legend-name"><span class="legend-dot" style="background:#64748b"></span>판정 불가</span><span id="unknownLegendValue" class="legend-value">0건</span></div>
        </div>
      </section>
    </div>

    <div class="bottom-grid">
      <section class="panel">
        <div class="panel-heading">
          <h2 class="panel-title">일별 재검토 요청</h2>
          <span class="panel-caption">선택 기간 접수 건수</span>
        </div>
        <div class="chart-wrap"><canvas id="reviewChart"></canvas></div>
      </section>
      <section class="panel">
        <div class="panel-heading"><h2 class="panel-title">통계 요약</h2></div>
        <div class="insight-list">
          <div class="insight">
            <span class="insight-icon blue"><i data-lucide="trending-up" width="16" height="16"></i></span>
            <div><strong id="totalInsightTitle">검증 0건</strong><p id="totalInsightText">선택 기간의 검증 기록을 집계합니다.</p></div>
          </div>
          <div class="insight">
            <span class="insight-icon cyan"><i data-lucide="image" width="16" height="16"></i></span>
            <div><strong id="aiInsightTitle">AI 판정 0건</strong><p id="aiInsightText">AI 생성 의심 판정 비율을 표시합니다.</p></div>
          </div>
          <div class="insight">
            <span class="insight-icon amber"><i data-lucide="circle-alert" width="16" height="16"></i></span>
            <div><strong id="reviewInsightTitle">재검토 요청 0건</strong><p id="reviewInsightText">오탐 신고 비율을 표시합니다.</p></div>
          </div>
        </div>
      </section>
    </div>
  </main>

  <script>
    const contextPath = "<%= contextPath %>";
    const gridColor = "rgba(148, 163, 184, 0.12)";
    const tickColor = "#84909f";
    Chart.defaults.color = tickColor;
    Chart.defaults.font.family = '"Noto Sans KR", sans-serif';

    const trendChart = new Chart(document.getElementById("trendChart"), {
      type: "line",
      data: {
        labels: [],
        datasets: [
          { label: "전체 검증", data: [], borderColor: "#4d8ff7", backgroundColor: "rgba(77,143,247,.12)", borderWidth: 2, pointRadius: 3, pointHoverRadius: 5, tension: .35, fill: true },
          { label: "AI 이미지 판정", data: [], borderColor: "#fb7185", backgroundColor: "transparent", borderWidth: 2, pointRadius: 3, pointHoverRadius: 5, tension: .35 }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        interaction: { mode: "index", intersect: false },
        plugins: { legend: { position: "top", align: "end", labels: { usePointStyle: true, boxWidth: 7, padding: 18 } } },
        scales: {
          x: { grid: { display: false }, border: { color: gridColor } },
          y: { beginAtZero: true, grid: { color: gridColor }, border: { display: false }, ticks: { precision: 0 } }
        }
      }
    });

    const verdictChart = new Chart(document.getElementById("verdictChart"), {
      type: "doughnut",
      data: { labels: ["실제 이미지", "AI 생성 의심", "판정 불가"], datasets: [{ data: [0, 0, 0], backgroundColor: ["#4d8ff7", "#fb7185", "#64748b"], borderColor: "#11151a", borderWidth: 4, hoverOffset: 3 }] },
      options: { responsive: true, maintainAspectRatio: false, cutout: "70%", plugins: { legend: { display: false } } }
    });

    const reviewChart = new Chart(document.getElementById("reviewChart"), {
      type: "bar",
      data: { labels: [], datasets: [{ label: "재검토 요청", data: [], backgroundColor: "rgba(251,191,36,.55)", borderColor: "#fbbf24", borderWidth: 1, borderRadius: 4, maxBarThickness: 30 }] },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, border: { color: gridColor } },
          y: { beginAtZero: true, grid: { color: gridColor }, border: { display: false }, ticks: { precision: 0, stepSize: 1 } }
        }
      }
    });

    function formatDate(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      return year + "-" + month + "-" + day;
    }

    function setDateRange(days) {
      const end = new Date();
      const start = new Date(end);
      start.setDate(end.getDate() - (days - 1));
      document.getElementById("startDate").value = formatDate(start);
      document.getElementById("endDate").value = formatDate(end);
    }

    function numberValue(value) {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : 0;
    }

    function countText(value) {
      return numberValue(value).toLocaleString("ko-KR") + "건";
    }

    function percentText(value) {
      return numberValue(value).toFixed(1) + "%";
    }

    function dateLabel(value) {
      const parts = String(value || "").split("-");
      return parts.length === 3 ? parts[1] + "." + parts[2] : value;
    }

    function showStatus(type, message) {
      const banner = document.getElementById("statusBanner");
      if (!message) {
        banner.className = "status-banner";
        banner.innerHTML = "";
        return;
      }
      const icon = type === "error" ? "circle-alert" : type === "empty" ? "database-zap" : "loader-circle";
      banner.className = "status-banner visible " + type;
      banner.innerHTML = '<i data-lucide="' + icon + '" width="16" height="16"></i>';
      const text = document.createElement("span");
      text.textContent = message;
      banner.appendChild(text);
      lucide.createIcons();
    }

    function setLoading(loading) {
      const button = document.getElementById("refreshButton");
      button.disabled = loading;
      button.innerHTML = loading
        ? '<i data-lucide="loader-circle" width="15" height="15"></i> 조회 중'
        : '<i data-lucide="refresh-cw" width="15" height="15"></i> 조회';
      lucide.createIcons();
    }

    function renderStats(data) {
      const daily = Array.isArray(data.dailyStatistics) ? data.dailyStatistics : [];
      const total = numberValue(data.totalVerificationCount);
      const aiCount = numberValue(data.aiDetectionCount);
      const realCount = numberValue(data.realCount);
      const unknownCount = numberValue(data.unknownCount);
      const reviewCount = numberValue(data.reviewRequestCount);

      document.getElementById("totalValue").textContent = countText(total);
      document.getElementById("totalMeta").textContent = data.startDate + " ~ " + data.endDate;
      document.getElementById("aiRatioValue").textContent = percentText(data.aiDetectionRatio);
      document.getElementById("aiCountMeta").textContent = countText(aiCount) + "이 AI 생성 의심으로 판정됨";
      document.getElementById("confidenceValue").textContent = percentText(data.averageConfidence);
      document.getElementById("reportRatioValue").textContent = percentText(data.falsePositiveReportRatio);
      document.getElementById("reportCountMeta").textContent = "재검토 요청 " + countText(reviewCount);

      document.getElementById("verdictTotalCaption").textContent = "총 " + countText(total);
      document.getElementById("realLegendValue").textContent = countText(realCount);
      document.getElementById("aiLegendValue").textContent = countText(aiCount);
      document.getElementById("unknownLegendValue").textContent = countText(unknownCount);

      const labels = daily.map(function(item) { return dateLabel(item.statisticsDate); });
      trendChart.data.labels = labels;
      trendChart.data.datasets[0].data = daily.map(function(item) { return numberValue(item.totalCount); });
      trendChart.data.datasets[1].data = daily.map(function(item) { return numberValue(item.aiDetectionCount); });
      trendChart.update();

      verdictChart.data.datasets[0].data = [realCount, aiCount, unknownCount];
      verdictChart.update();

      reviewChart.data.labels = labels;
      reviewChart.data.datasets[0].data = daily.map(function(item) { return numberValue(item.reviewRequestCount); });
      reviewChart.update();

      document.getElementById("totalInsightTitle").textContent = "검증 " + countText(total);
      document.getElementById("totalInsightText").textContent = data.startDate + "부터 " + data.endDate + "까지의 검증 결과입니다.";
      document.getElementById("aiInsightTitle").textContent = "AI 판정 " + countText(aiCount);
      document.getElementById("aiInsightText").textContent = "전체 검증 중 " + percentText(data.aiDetectionRatio) + "가 AI 생성 의심으로 판정됐습니다.";
      document.getElementById("reviewInsightTitle").textContent = "재검토 요청 " + countText(reviewCount);
      document.getElementById("reviewInsightText").textContent = "전체 검증 대비 오탐 신고 비율은 " + percentText(data.falsePositiveReportRatio) + "입니다.";

      showStatus(total === 0 ? "empty" : "", total === 0 ? "선택한 기간에 저장된 검증 기록이 없습니다." : "");
    }

    async function loadStats() {
      const startDate = document.getElementById("startDate").value;
      const endDate = document.getElementById("endDate").value;
      setLoading(true);
      showStatus("loading", "DB에서 검증 통계를 불러오고 있습니다.");

      try {
        const query = new URLSearchParams({ startDate: startDate, endDate: endDate });
        const response = await fetch(contextPath + "/api/v1/stats?" + query.toString(), {
          headers: { "Accept": "application/json" }
        });
        if (!response.ok) {
          throw new Error("통계 서버 요청에 실패했습니다.");
        }
        const payload = await response.json();
        if (!payload.success) {
          throw new Error(payload.error && payload.error.message ? payload.error.message : "통계를 불러오지 못했습니다.");
        }
        renderStats(payload.data || {});
      } catch (error) {
        showStatus("error", error.message || "검증 통계를 불러오는 중 오류가 발생했습니다.");
      } finally {
        setLoading(false);
      }
    }

    document.querySelectorAll(".segment").forEach(function(button) {
      button.addEventListener("click", function() {
        document.querySelectorAll(".segment").forEach(function(item) { item.classList.remove("active"); });
        button.classList.add("active");
        const days = Number(button.dataset.days);
        setDateRange(days);
        loadStats();
      });
    });

    document.getElementById("refreshButton").addEventListener("click", loadStats);

    setDateRange(7);
    lucide.createIcons();
    loadStats();
  </script>
</body>
</html>

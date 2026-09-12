<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/deepscan-theme.css?v=3">
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>분석 리포트 - DeepScan</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
    <style>
        body {
            font-family: "Pretendard", "Noto Sans KR", sans-serif;
            background: #020617;
        }

        .report-shell {
            border: 1px solid rgba(148, 163, 184, 0.18);
            background: rgba(15, 23, 42, 0.78);
            box-shadow: 0 24px 80px rgba(2, 6, 23, 0.38);
        }

        .mode-tab {
            display: inline-flex;
            flex: 1 1 0;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            border-bottom: 2px solid transparent;
            padding: 0.95rem 0.55rem;
            color: #94a3b8;
            font-size: 0.875rem;
            font-weight: 700;
            transition: background-color 160ms ease, color 160ms ease, border-color 160ms ease;
        }

        .mode-tab:hover {
            background: rgba(255, 255, 255, 0.04);
            color: #f8fafc;
        }

        .mode-tab.active {
            border-color: #38bdf8;
            background: rgba(14, 165, 233, 0.18);
            color: #7dd3fc;
        }

        .analysis-stage {
            position: relative;
            display: flex;
            min-height: 360px;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            border-radius: 1rem;
            background: #020617;
        }

        .analysis-image-wrap {
            position: relative;
            display: inline-block;
            max-width: 100%;
            line-height: 0;
        }

        .analysis-image-wrap img {
            display: block;
            max-height: 70vh;
            max-width: 100%;
            object-fit: contain;
            border-radius: 0.9rem;
        }

        .heatmap-overlay-image {
            position: absolute;
            inset: 0;
            width: 100%;
            height: 100%;
            object-fit: contain;
            opacity: 0.56;
            mix-blend-mode: screen;
            pointer-events: none;
        }

        .region-layer {
            position: absolute;
            inset: 0;
            pointer-events: none;
        }

        .region-box {
            position: absolute;
            border: 2px solid rgba(248, 113, 113, 0.96);
            border-radius: 0.75rem;
            background: rgba(248, 113, 113, 0.16);
            box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12), 0 0 28px rgba(248, 113, 113, 0.28);
            pointer-events: auto;
        }

        .region-box::after {
            content: attr(data-label);
            position: absolute;
            left: 0;
            bottom: calc(100% + 0.5rem);
            max-width: 13rem;
            border: 1px solid rgba(56, 189, 248, 0.28);
            border-radius: 0.75rem;
            background: rgba(15, 23, 42, 0.96);
            padding: 0.42rem 0.62rem;
            color: #e0f2fe;
            font-size: 0.75rem;
            font-weight: 700;
            line-height: 1.15rem;
            opacity: 0;
            pointer-events: none;
            white-space: nowrap;
            transform: translateY(4px);
            transition: opacity 140ms ease, transform 140ms ease;
        }

        .region-box:hover::after {
            opacity: 1;
            transform: translateY(0);
        }

        .notice-box {
            border: 1px solid rgba(251, 191, 36, 0.22);
            background: rgba(251, 191, 36, 0.08);
            color: #fde68a;
        }

        .provider-badge {
            border: 1px solid rgba(56, 189, 248, 0.28);
            background: rgba(14, 165, 233, 0.12);
            color: #7dd3fc;
        }

        .detail-row {
            display: grid;
            grid-template-columns: 1fr 1.15fr 2fr auto;
            gap: 1rem;
            align-items: center;
            border: 1px solid rgba(148, 163, 184, 0.14);
            border-radius: 1rem;
            background: rgba(15, 23, 42, 0.72);
            padding: 1rem;
            transition: border-color 160ms ease, background-color 160ms ease;
        }

        .detail-row:hover {
            border-color: rgba(56, 189, 248, 0.28);
            background: rgba(30, 41, 59, 0.74);
        }

        .risk-badge {
            display: inline-flex;
            min-width: 4.25rem;
            justify-content: center;
            border-radius: 999px;
            border: 1px solid transparent;
            padding: 0.35rem 0.7rem;
            font-size: 0.75rem;
            font-weight: 800;
            white-space: nowrap;
        }

        .risk-high {
            border-color: rgba(248, 113, 113, 0.35);
            background: rgba(239, 68, 68, 0.16);
            color: #fca5a5;
        }

        .risk-medium {
            border-color: rgba(251, 146, 60, 0.35);
            background: rgba(249, 115, 22, 0.14);
            color: #fdba74;
        }

        .risk-low {
            border-color: rgba(96, 165, 250, 0.3);
            background: rgba(59, 130, 246, 0.12);
            color: #93c5fd;
        }

        .risk-unknown {
            border-color: rgba(148, 163, 184, 0.25);
            background: rgba(100, 116, 139, 0.14);
            color: #cbd5e1;
        }

        @media (max-width: 760px) {
            .detail-row {
                grid-template-columns: 1fr;
                gap: 0.55rem;
            }

            .detail-heading {
                display: none;
            }
        }
    </style>
</head>
<body class="ds-page min-h-screen bg-slate-950 text-white">
<script id="apiRawData" type="application/json"><c:out value="${safeApiRaw}" escapeXml="false"/></script>
<script id="analysisJsonData" type="application/json"><c:out value="${safeAnalysisJson}" escapeXml="false"/></script>

<div class="pointer-events-none fixed inset-0 overflow-hidden">
    <div id="topGlow" class="absolute left-1/4 top-20 h-96 w-96 rounded-full blur-3xl opacity-20"></div>
    <div class="absolute bottom-20 right-1/4 h-96 w-96 rounded-full bg-cyan-500 blur-3xl opacity-10"></div>
</div>

<main class="relative mx-auto max-w-7xl px-4 py-8">
    <section class="report-shell overflow-hidden rounded-3xl">
        <header class="flex items-start justify-between border-b border-white/10 px-6 py-5">
            <div>
                <h1 class="text-2xl font-bold text-white md:text-3xl">분석 리포트</h1>
                <p class="mt-2 max-w-xl break-all text-sm text-slate-400"><c:out value="${result.originalName}"/></p>
            </div>
            <div class="flex items-center gap-3">
                <span class="provider-badge hidden rounded-full px-3 py-1 text-xs font-bold sm:inline-flex">Reality Defender</span>
                <button type="button" onclick="goPage('<%= contextPath %>/history')" class="rounded-full p-2 text-slate-400 transition hover:bg-white/10 hover:text-white" aria-label="닫기">
                    <i data-lucide="x" class="h-6 w-6"></i>
                </button>
            </div>
        </header>

        <div class="grid gap-7 p-5 lg:grid-cols-[1.08fr_0.92fr] lg:p-7">
            <section class="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/60">
                <div class="flex border-b border-white/10">
                    <button id="originalTab" type="button" onclick="setViewMode('original')" class="mode-tab">
                        <i data-lucide="eye" class="h-4 w-4"></i>
                        원본
                    </button>
                    <button id="regionsTab" type="button" onclick="setViewMode('regions')" class="mode-tab">
                        <i data-lucide="scan-search" class="h-4 w-4"></i>
                        특이점 보기
                    </button>
                    <button id="overlayTab" type="button" onclick="setViewMode('overlay')" class="mode-tab active">
                        <i data-lucide="layers" class="h-4 w-4"></i>
                        오버레이
                    </button>
                </div>

                <div class="p-4">
                    <div class="analysis-stage">
                        <div id="imageWrap" class="analysis-image-wrap">
                            <img id="baseImage" src="<c:out value='${result.publicUrl}'/>" alt="<c:out value='${result.originalName}'/>">
                            <img id="heatmapImage" class="heatmap-overlay-image hidden" alt="IMD 히트맵 오버레이">
                            <div id="regionLayer" class="region-layer hidden"></div>
                        </div>
                    </div>

                    <div class="mt-4 rounded-2xl border border-white/10 bg-slate-800/40 p-4">
                        <div class="mb-2 flex items-center gap-2 text-sm font-semibold text-white">
                            <i data-lucide="activity" class="h-4 w-4 text-amber-300"></i>
                            참고용 시각화
                        </div>
                        <p id="visualDescription" class="text-sm leading-6 text-slate-300"></p>
                        <p class="mt-2 text-xs leading-5 text-slate-500">
                            IMD / ManTraNet 시각화는 최종 판정 기준이 아니라, 픽셀 패턴 차이를 확인하기 위한 참고 자료입니다.
                        </p>
                    </div>

                    <div id="visualNoticeBox" class="notice-box mt-4 hidden rounded-2xl p-4 text-sm leading-6"></div>
                </div>
            </section>

            <section class="space-y-5">
                <div class="relative">
                    <div id="verdictGlow" class="absolute inset-0 rounded-3xl blur-2xl opacity-25"></div>
                    <div class="relative rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                        <div class="mb-5 flex items-center justify-between gap-3">
                            <div class="flex items-center gap-4">
                                <div id="verdictIconWrap" class="rounded-2xl p-3"></div>
                                <div>
                                    <div class="mb-1 text-sm text-slate-400">최종 판정</div>
                                    <div id="verdictLabel" class="text-2xl font-bold"></div>
                                </div>
                            </div>
                            <span class="provider-badge rounded-full px-3 py-1 text-xs font-bold">Reality Defender</span>
                        </div>

                        <div class="mb-4 text-center">
                            <div class="text-6xl font-bold text-white">
                                <span id="scoreValue">0</span>
                                <span class="text-3xl text-slate-400">/100</span>
                            </div>
                            <div class="mt-2 text-sm text-slate-400">Reality Defender 최종 점수</div>
                        </div>

                        <div class="h-3 overflow-hidden rounded-full bg-slate-800">
                            <div id="scoreProgress" class="h-full transition-transform duration-700 ease-out"></div>
                        </div>
                        <div id="scoreHint" class="mt-3 text-xs text-slate-500"></div>
                    </div>
                </div>

                <div class="rounded-3xl border border-sky-500/20 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <div class="flex items-center gap-3">
                        <div class="rounded-2xl bg-sky-400/10 p-2.5 text-sky-300">
                            <i data-lucide="file-down" class="h-5 w-5"></i>
                        </div>
                        <div>
                            <h2 class="text-lg font-semibold text-white">신고 제출용 PDF</h2>
                            <p class="mt-1 text-sm text-slate-400">현재 검증 결과와 저장된 AI 해설을 신고자료로 정리합니다.</p>
                        </div>
                    </div>

                    <form id="reportPdfForm" class="mt-5 space-y-4 border-t border-white/10 pt-5">
                        <div>
                            <div class="mb-2 flex items-center justify-between gap-3">
                                <label for="reportPdfReason" class="text-sm font-semibold text-slate-300">신고 사유</label>
                                <span id="reportPdfReasonCount" class="text-xs text-slate-500">0 / 2000</span>
                            </div>
                            <textarea id="reportPdfReason" maxlength="2000" rows="4" required class="w-full resize-y rounded-xl border border-white/10 bg-slate-950/80 px-3 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-slate-600 focus:border-sky-300/60 focus:ring-2 focus:ring-sky-300/20" placeholder="신고 기관에 전달할 사실과 요청 사항을 작성해 주세요."></textarea>
                        </div>
                        <div>
                            <label for="reportPdfSourceUrl" class="mb-2 block text-sm font-semibold text-slate-300">발견한 페이지 주소 <span class="font-normal text-slate-500">선택</span></label>
                            <input id="reportPdfSourceUrl" type="url" maxlength="1000" class="min-h-11 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-sky-300/60 focus:ring-2 focus:ring-sky-300/20" placeholder="https://example.com/post/123">
                        </div>
                        <div class="grid gap-3 sm:grid-cols-3">
                            <label class="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-white/10 bg-slate-950/50 px-3 text-sm text-slate-300">
                                <input id="reportPdfOriginal" type="checkbox" class="h-4 w-4 accent-sky-400">
                                원본 이미지
                            </label>
                            <label class="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-white/10 bg-slate-950/50 px-3 text-sm text-slate-300">
                                <input id="reportPdfHeatmap" type="checkbox" class="h-4 w-4 accent-sky-400">
                                히트맵
                            </label>
                            <label class="flex min-h-11 cursor-pointer items-center gap-2 rounded-xl border border-white/10 bg-slate-950/50 px-3 text-sm text-slate-300">
                                <input id="reportPdfAiDraft" type="checkbox" class="h-4 w-4 accent-sky-400">
                                AI 신고 문구
                            </label>
                        </div>
                        <button id="createReportPdfButton" type="submit" class="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-sky-400 px-4 py-2.5 text-sm font-bold text-slate-950 transition hover:bg-sky-300 disabled:cursor-wait disabled:opacity-60">
                            <i data-lucide="file-plus-2" class="h-4 w-4"></i>
                            <span>PDF 신고자료 만들기</span>
                        </button>
                    </form>
                    <div id="reportPdfError" class="mt-4 hidden text-sm leading-6 text-rose-300" role="alert"></div>
                    <div id="reportPdfListWrap" class="mt-5 hidden border-t border-white/10 pt-5">
                        <p class="mb-3 text-sm font-semibold text-slate-300">생성된 신고자료</p>
                        <div id="reportPdfList" class="space-y-3"></div>
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <h2 class="mb-3 text-lg font-semibold text-white">분석 설명</h2>
                    <p id="explanationText" class="mb-4 leading-7 text-slate-300"></p>
                    <div class="notice-box rounded-2xl p-4 text-sm leading-6">
                        이 결과는 조작 여부를 확정하는 값이 아닙니다. 최종 판정과 점수는 Reality Defender 결과를 기준으로 표시하며, 히트맵과 영역 표시는 픽셀 패턴 차이를 이해하기 위한 참고용 분석입니다.
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <div class="flex flex-wrap items-center justify-between gap-3">
                        <div class="flex items-center gap-3">
                            <div class="rounded-2xl bg-cyan-400/10 p-2.5 text-cyan-300">
                                <i data-lucide="sparkles" class="h-5 w-5"></i>
                            </div>
                            <div>
                                <h2 class="text-lg font-semibold text-white">Gemini 상세 해설</h2>
                                <span id="aiRiskBadge" class="risk-badge risk-unknown mt-1 hidden">확인 중</span>
                            </div>
                        </div>
                        <button id="generateAiExplanationButton" type="button" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-cyan-500 px-4 py-2.5 text-sm font-bold text-slate-950 transition hover:bg-cyan-400 disabled:cursor-wait disabled:opacity-60">
                            <i id="generateAiExplanationIcon" data-lucide="sparkles" class="h-4 w-4"></i>
                            <span id="generateAiExplanationLabel">상세 해설 생성</span>
                        </button>
                    </div>

                    <div id="aiExplanationLoading" class="mt-5 hidden items-center gap-3 border-t border-white/10 pt-5 text-sm text-slate-400" role="status">
                        <i data-lucide="loader-circle" class="h-5 w-5 animate-spin text-cyan-300"></i>
                        상세 해설을 생성하고 있습니다.
                    </div>

                    <div id="aiExplanationError" class="mt-5 hidden border-t border-red-400/20 pt-5 text-sm leading-6 text-red-300" role="alert"></div>

                    <div id="aiExplanationResult" class="mt-5 hidden space-y-5 border-t border-white/10 pt-5">
                        <div>
                            <h3 class="mb-2 text-sm font-semibold text-cyan-200">종합 해석</h3>
                            <p id="aiSummary" class="leading-7 text-white"></p>
                        </div>
                        <div>
                            <h3 class="mb-2 text-sm font-semibold text-slate-300">상세 설명</h3>
                            <p id="aiExplanation" class="whitespace-pre-line leading-7 text-slate-300"></p>
                        </div>
                        <div>
                            <h3 class="mb-2 text-sm font-semibold text-slate-300">확인 방법</h3>
                            <p id="aiActionGuide" class="whitespace-pre-line leading-7 text-slate-300"></p>
                        </div>
                        <div class="notice-box rounded-2xl p-4 text-sm leading-6">
                            <span id="aiDisclaimer"></span>
                        </div>
                        <div id="aiExplanationMeta" class="text-xs text-slate-500"></div>
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <div class="flex items-center gap-3">
                            <div class="rounded-2xl bg-emerald-400/10 p-2.5 text-emerald-300">
                                <i data-lucide="scan-search" class="h-5 w-5"></i>
                            </div>
                            <div>
                                <h2 class="text-lg font-semibold text-white">AI 이미지 2차 검증</h2>
                                <span id="imageReviewStatusBadge" class="risk-badge risk-unknown mt-1 hidden">검토 전</span>
                            </div>
                        </div>
                        <button id="generateImageReviewButton" type="button" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-400 px-4 py-2.5 text-sm font-bold text-slate-950 transition hover:bg-emerald-300 disabled:cursor-wait disabled:opacity-60">
                            <i data-lucide="scan-search" class="h-4 w-4"></i>
                            <span>이미지 다시 검토</span>
                        </button>
                    </div>

                    <div id="imageReviewLoading" class="mt-5 hidden items-center gap-3 border-t border-white/10 pt-5 text-sm text-slate-400" role="status">
                        <i data-lucide="loader-circle" class="h-5 w-5 animate-spin text-emerald-300"></i>
                        Gemini가 원본 이미지를 독립적으로 검토하고 있습니다.
                    </div>

                    <div id="imageReviewError" class="mt-5 hidden border-t border-red-400/20 pt-5 text-sm leading-6 text-red-300" role="alert"></div>

                    <div id="imageReviewResult" class="mt-5 hidden space-y-5 border-t border-white/10 pt-5">
                        <div>
                            <h3 class="mb-2 text-sm font-semibold text-emerald-200">교차 검증 결론</h3>
                            <p id="imageReviewConclusion" class="leading-7 text-white"></p>
                        </div>
                        <div>
                            <h3 class="mb-2 text-sm font-semibold text-slate-300">이미지 관찰 결과</h3>
                            <p id="imageReviewSummary" class="leading-7 text-slate-300"></p>
                        </div>
                        <div>
                            <h3 class="mb-2 text-sm font-semibold text-slate-300">관찰된 시각적 단서</h3>
                            <ul id="imageReviewIndicators" class="space-y-2 text-sm leading-6 text-slate-300"></ul>
                        </div>
                        <div class="notice-box rounded-2xl p-4 text-sm leading-6">
                            <span id="imageReviewLimitations"></span>
                        </div>
                        <div id="imageReviewMeta" class="text-xs text-slate-500"></div>
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <div class="flex items-center gap-3">
                        <div class="rounded-2xl bg-amber-400/10 p-2.5 text-amber-300">
                            <i data-lucide="flag" class="h-5 w-5"></i>
                        </div>
                        <div>
                            <h2 class="text-lg font-semibold text-white">오탐 신고 및 재검토 요청</h2>
                            <p class="mt-1 text-sm text-slate-400">현재 검증 결과가 실제 이미지와 다르다고 판단되면 재검토를 요청할 수 있습니다.</p>
                        </div>
                    </div>

                    <form id="reviewRequestForm" class="mt-5 space-y-4 border-t border-white/10 pt-5">
                        <div>
                            <label for="reviewRequestType" class="mb-2 block text-sm font-semibold text-slate-300">요청 유형</label>
                            <select id="reviewRequestType" class="min-h-11 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 text-sm text-white outline-none transition focus:border-amber-300/60 focus:ring-2 focus:ring-amber-300/20">
                                <option value="RECHECK">판정 재검토</option>
                                <option value="FALSE_POSITIVE">오탐 신고: 실제인데 조작으로 판정</option>
                                <option value="FALSE_NEGATIVE">미탐 신고: 조작인데 실제로 판정</option>
                            </select>
                        </div>
                        <div>
                            <div class="mb-2 flex items-center justify-between gap-3">
                                <label for="reviewRequestReason" class="text-sm font-semibold text-slate-300">요청 사유</label>
                                <span id="reviewRequestReasonCount" class="text-xs text-slate-500">0 / 1000</span>
                            </div>
                            <textarea id="reviewRequestReason" maxlength="1000" rows="4" required class="w-full resize-y rounded-xl border border-white/10 bg-slate-950/80 px-3 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-slate-600 focus:border-amber-300/60 focus:ring-2 focus:ring-amber-300/20" placeholder="판정이 잘못되었다고 생각하는 이유를 구체적으로 작성해 주세요."></textarea>
                        </div>
                        <button id="submitReviewRequestButton" type="submit" class="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-amber-300 px-4 py-2.5 text-sm font-bold text-slate-950 transition hover:bg-amber-200 disabled:cursor-wait disabled:opacity-60">
                            <i data-lucide="send" class="h-4 w-4"></i>
                            <span>재검토 요청 접수</span>
                        </button>
                    </form>

                    <div id="reviewRequestLoading" class="mt-5 hidden items-center gap-3 border-t border-white/10 pt-5 text-sm text-slate-400" role="status">
                        <i data-lucide="loader-circle" class="h-5 w-5 animate-spin text-amber-300"></i>
                        재검토 요청 상태를 확인하고 있습니다.
                    </div>
                    <div id="reviewRequestError" class="mt-5 hidden border-t border-red-400/20 pt-5 text-sm leading-6 text-red-300" role="alert"></div>

                    <div id="reviewRequestResult" class="mt-5 hidden space-y-4 border-t border-white/10 pt-5">
                        <div class="flex flex-wrap items-center justify-between gap-3">
                            <div>
                                <p class="text-xs text-slate-500">요청 유형</p>
                                <p id="reviewRequestTypeText" class="mt-1 font-semibold text-white"></p>
                            </div>
                            <span id="reviewRequestStatusBadge" class="risk-badge risk-unknown"></span>
                        </div>
                        <div>
                            <p class="text-xs text-slate-500">요청 사유</p>
                            <p id="reviewRequestReasonText" class="mt-1 whitespace-pre-line text-sm leading-6 text-slate-300"></p>
                        </div>
                        <div id="reviewerNoteWrap" class="hidden rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                            <p class="text-xs text-slate-500">검토자 답변</p>
                            <p id="reviewerNoteText" class="mt-1 whitespace-pre-line text-sm leading-6 text-slate-300"></p>
                        </div>
                        <p id="reviewRequestMeta" class="text-xs text-slate-500"></p>
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <h2 class="mb-4 text-lg font-semibold text-white">상세 정보</h2>
                    <div class="space-y-3">
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">최종 분석 제공사</span>
                            <span id="apiProviderText" class="font-medium text-white">Reality Defender</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">최종 분석 모델</span>
                            <span id="modelNameText" class="font-medium text-white">Reality Defender</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">히트맵 시각화</span>
                            <span id="heatmapProviderText" class="font-medium text-white">IMD / ManTraNet</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">표시 영역 수</span>
                            <span id="regionCountText" class="font-medium text-white">0개</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">히트맵 상태</span>
                            <span id="heatmapStatusText" class="font-medium text-white">확인 중</span>
                        </div>
                        <div class="flex items-center justify-between py-2">
                            <span class="text-sm text-slate-400">분석 일시</span>
                            <span class="font-medium text-white"><c:out value="${result.regDt}"/></span>
                        </div>
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <h2 class="mb-4 text-lg font-semibold text-white">분석 상세 근거</h2>
                    <div class="detail-heading grid grid-cols-[1fr_1.15fr_2fr_auto] gap-4 px-4 pb-2 text-xs font-semibold text-slate-500">
                        <span>분석 항목</span>
                        <span>감지 결과</span>
                        <span>설명</span>
                        <span>위험도</span>
                    </div>
                    <div id="detailedAnalysisRows" class="space-y-3"></div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                    <button type="button" onclick="goPage('<%= contextPath %>/history')" class="flex items-center justify-center gap-2 rounded-2xl border border-white/15 bg-white/5 px-6 py-4 font-semibold text-white transition hover:bg-white/10">
                        <i data-lucide="history" class="h-5 w-5"></i>
                        검증기록
                    </button>
                    <button id="shareButton" type="button" class="flex items-center justify-center gap-2 rounded-2xl bg-sky-500 px-6 py-4 font-semibold text-white transition hover:bg-sky-400">
                        <i id="shareIcon" data-lucide="share-2" class="h-5 w-5"></i>
                        <span id="shareLabel">결과 공유</span>
                    </button>
                </div>
            </section>
        </div>
    </section>
</main>

<script>
    const contextPath = "<%= contextPath %>";
    const verdict = "<c:out value='${result.verdict}'/>";
    const score = Number("<c:out value='${result.score}'/>");
    const resultId = "<c:out value='${result.id}'/>";
    const originalImageUrl = "<c:out value='${result.publicUrl}'/>";
    const apiProvider = "<c:out value='${result.apiProvider}'/>";
    const confidence = Number.isNaN(score) ? 0 : Math.round(Math.max(0, Math.min(score, 1)) * 100);

    const parsedApiRaw = parseJsonFromNode("apiRawData");
    const parsedAnalysis = parseJsonFromNode("analysisJsonData");
    const combinedResult = Object.assign({}, parsedApiRaw || {}, parsedAnalysis || {}, {
        verdict: verdict,
        score: score,
        publicUrl: originalImageUrl,
        apiProvider: apiProvider
    });

    const heatmapUrl = getHeatmapUrl(combinedResult);
    const anomalyRegions = getAnomalyRegions(combinedResult);

    const statusConfig = {
        SAFE: { label: "낮은 의심", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
        REAL: { label: "낮은 의심", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
        AUTHENTIC: { label: "낮은 의심", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
        SUSPECT: { label: "주의 필요", glowClass: "bg-yellow-500", badgeClass: "text-yellow-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500" },
        SUSPICIOUS: { label: "주의 필요", glowClass: "bg-yellow-500", badgeClass: "text-yellow-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500" },
        HIGH_RISK: { label: "추가 확인 필요", glowClass: "bg-red-500", badgeClass: "text-red-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-red-500 to-rose-500" },
        FAKE: { label: "추가 확인 필요", glowClass: "bg-red-500", badgeClass: "text-red-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-red-500 to-rose-500" },
        NOT_APPLICABLE: { label: "분석 제한", glowClass: "bg-slate-500", badgeClass: "text-slate-300", icon: "ban", progressClass: "bg-gradient-to-r from-slate-500 to-slate-400" },
        UNABLE_TO_EVALUATE: { label: "분석 제한", glowClass: "bg-slate-500", badgeClass: "text-slate-300", icon: "ban", progressClass: "bg-gradient-to-r from-slate-500 to-slate-400" }
    };

    const fallbackStatusKey = confidence >= 70 ? "HIGH_RISK" : confidence >= 35 ? "SUSPICIOUS" : "SAFE";
    const statusKey = statusConfig[verdict] ? verdict : fallbackStatusKey;
    const config = statusConfig[statusKey];

    function parseJsonFromNode(id) {
        const node = document.getElementById(id);
        const text = node ? node.textContent.trim() : "";
        if (!text) return {};
        try {
            return JSON.parse(text);
        } catch (error) {
            return {};
        }
    }

    function goPage(path) {
        window.location.href = path;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function valueAt(data, path) {
        if (!data || typeof data !== "object") return null;
        return path.split(".").reduce(function(current, key) {
            return current && Object.prototype.hasOwnProperty.call(current, key) ? current[key] : null;
        }, data);
    }

    function firstValue(data, paths) {
        for (const path of paths) {
            const value = valueAt(data, path);
            if (value !== null && value !== undefined && value !== "") return value;
        }
        return null;
    }

    function normalizeImageUrl(url) {
        if (!url) return "";
        const value = String(url).trim();
        if (!value) return "";
        if (value.startsWith("data:") || value.startsWith("blob:") || value.startsWith("http://") || value.startsWith("https://")) return value;
        if (value.startsWith("//")) return window.location.protocol + value;
        if (value.startsWith("/")) return contextPath && !value.startsWith(contextPath + "/") ? contextPath + value : value;
        return contextPath + "/" + value.replace(/^\.?\//, "");
    }

    function normalizeImageData(value) {
        if (!value) return "";
        if (typeof value === "string") return normalizeImageUrl(value);
        if (typeof value !== "object") return "";
        if (value.url) return normalizeImageUrl(value.url);
        const data = String(value.data || value.base64 || "").trim();
        if (!data) return "";
        if (data.startsWith("data:")) return data;
        if (isImageUrlLike(data)) return normalizeImageUrl(data);
        return "data:" + (value.type || value.mimeType || "image/png") + ";base64," + data.replace(/\s/g, "");
    }

    function isImageUrlLike(value) {
        return value.startsWith("http://")
            || value.startsWith("https://")
            || value.startsWith("/")
            || value.startsWith("./")
            || value.startsWith("../")
            || value.startsWith("blob:");
    }

    function getHeatmapUrl(result) {
        const paths = [
            "heatmapUrl",
            "heatmap_url",
            "overlayUrl",
            "overlay_url",
            "imdHeatmapUrl",
            "imd_heatmap_url",
            "visualizationUrl",
            "visualization_url",
            "processedHeatmap",
            "overlayHeatmap",
            "rawHeatmap",
            "heatmap",
            "overlay",
            "imdHeatmapResult.heatmapUrl",
            "imdHeatmapResult.heatmap_url",
            "imdHeatmapResult.overlayUrl",
            "imdHeatmapResult.overlay_url",
            "imdHeatmapResult.imdHeatmapUrl",
            "imdHeatmapResult.imd_heatmap_url",
            "imdHeatmapResult.visualizationUrl",
            "imdHeatmapResult.visualization_url",
            "imdHeatmapResult.processedHeatmap",
            "imdHeatmapResult.overlayHeatmap",
            "imdHeatmapResult.rawHeatmap",
            "imdHeatmapResult.heatmap",
            "imdHeatmapResult.overlay"
        ];
        const value = firstValue(result, paths);
        return normalizeImageData(value);
    }

    function getAnomalyRegions(result) {
        const candidates = [
            firstValue(result, ["suspiciousRegions"]),
            firstValue(result, ["anomalyRegions"]),
            firstValue(result, ["regions"]),
            firstValue(result, ["boxes"]),
            firstValue(result, ["bboxes"]),
            firstValue(result, ["imdHeatmapResult.suspiciousRegions"]),
            firstValue(result, ["imdHeatmapResult.anomalyRegions"]),
            firstValue(result, ["imdHeatmapResult.regions"]),
            firstValue(result, ["imdHeatmapResult.boxes"]),
            firstValue(result, ["imdHeatmapResult.bboxes"]),
            firstValue(result, ["resultsSummary.suspiciousRegions"]),
            firstValue(result, ["resultsSummary.metadata.suspiciousRegions"])
        ];

        const source = candidates.find(Array.isArray) || [];
        return source.map(normalizeRegion).filter(function(region) {
            return region.width > 0 && region.height > 0;
        });
    }

    function normalizeRegion(region) {
        if (Array.isArray(region)) {
            return normalizeRegion({ x: region[0], y: region[1], width: region[2], height: region[3] });
        }

        const box = region && (region.bbox || region.box || region.bounds) || {};
        let x = Number(region?.x ?? region?.left ?? box.x ?? box.left ?? 0);
        let y = Number(region?.y ?? region?.top ?? box.y ?? box.top ?? 0);
        let width = Number(region?.width ?? region?.w ?? box.width ?? box.w ?? 0);
        let height = Number(region?.height ?? region?.h ?? box.height ?? box.h ?? 0);

        if ((!width || !height) && Number.isFinite(Number(region?.right)) && Number.isFinite(Number(region?.bottom))) {
            width = Number(region.right) - x;
            height = Number(region.bottom) - y;
        }

        const normalized = region?.normalized === true || (x <= 1 && y <= 1 && width <= 1 && height <= 1);
        const confidenceRaw = Number(region?.confidence ?? region?.score ?? region?.intensity ?? region?.weight ?? 0);
        const confidence = Number.isNaN(confidenceRaw) ? 0 : (confidenceRaw > 1 ? confidenceRaw / 100 : confidenceRaw);

        return {
            x: normalized ? clamp01(x) : x,
            y: normalized ? clamp01(y) : y,
            width: normalized ? clamp01(width) : width,
            height: normalized ? clamp01(height) : height,
            normalized: normalized,
            label: getKoreanRegionLabel(region),
            confidence: clamp01(confidence)
        };
    }

    function clamp01(value) {
        return Math.max(0, Math.min(1, Number(value) || 0));
    }

    function getKoreanRegionLabel(region) {
        const label = String(region?.label || region?.name || region?.type || "").toLowerCase();
        if (label.includes("eye")) return "눈 주변 픽셀 패턴 차이";
        if (label.includes("mouth")) return "입 주변 픽셀 패턴 차이";
        if (label.includes("skin") || label.includes("face")) return "얼굴 영역 픽셀 패턴 차이";
        if (label.includes("boundary") || label.includes("edge")) return "경계선 패턴 차이";
        return "픽셀 패턴 차이 영역";
    }

    function isHighRisk() {
        return ["HIGH_RISK", "FAKE"].includes(statusKey) || confidence >= 70;
    }

    function isMediumRisk() {
        return ["SUSPECT", "SUSPICIOUS"].includes(statusKey) || (confidence >= 35 && confidence < 70);
    }

    function getPrimaryRiskLevel() {
        if (isHighRisk()) return "높음";
        if (isMediumRisk()) return "중간";
        return "낮음";
    }

    function getSecondaryRiskLevel() {
        if (isHighRisk()) return confidence >= 85 ? "높음" : "중간";
        if (isMediumRisk()) return "중간";
        return "낮음";
    }

    function getDetailedAnalysisRows(result) {
        const hasHeatmap = Boolean(getHeatmapUrl(result));
        const primaryRisk = getPrimaryRiskLevel();
        const secondaryRisk = getSecondaryRiskLevel();

        return [
            {
                item: "픽셀 패턴",
                detected: primaryRisk === "낮음" ? "강한 차이는 크지 않음" : "주변 픽셀과 다른 패턴 감지",
                description: "얼굴 또는 주요 영역에서 주변 픽셀 패턴과 다른 변화가 감지될 수 있습니다.",
                risk: primaryRisk
            },
            {
                item: "경계선",
                detected: secondaryRisk === "낮음" ? "뚜렷한 경계 차이는 낮음" : "일부 경계선이 부자연스러움",
                description: "얼굴, 손, 물체 윤곽 등에서 색상이나 질감 변화가 갑자기 나타나는 영역을 참고합니다.",
                risk: secondaryRisk
            },
            {
                item: "질감",
                detected: secondaryRisk === "낮음" ? "질감 차이 신호 낮음" : "질감 분포 차이 감지",
                description: "피부, 배경, 옷감 등의 표면 패턴이 주변과 다르게 나타나는지 확인합니다.",
                risk: secondaryRisk
            },
            {
                item: "압축 흔적",
                detected: confidence >= 60 ? "압축 패턴 차이 가능성" : "강한 압축 차이는 낮음",
                description: "이미지 일부 영역에서 압축 또는 재저장 과정에서 생길 수 있는 패턴 차이를 참고합니다.",
                risk: confidence >= 60 ? "중간" : "낮음"
            },
            {
                item: "색상/조명",
                detected: secondaryRisk === "낮음" ? "색상 또는 밝기 차이 낮음" : "색상 또는 밝기 차이 감지",
                description: "특정 영역의 밝기, 색감, 그림자 패턴이 주변과 다르게 보일 수 있는지 확인합니다.",
                risk: secondaryRisk
            },
            {
                item: "히트맵 반응",
                detected: hasHeatmap ? "특정 영역에서 반응 확인" : "분석 데이터 없음",
                description: hasHeatmap
                    ? "IMD 히트맵에서 주변 픽셀 패턴과 다르게 감지된 영역이 강조되었습니다."
                    : "현재 이미지에 대해 표시 가능한 IMD 히트맵 데이터가 제공되지 않았습니다.",
                risk: hasHeatmap ? primaryRisk : "정보 없음"
            }
        ];
    }

    function getRiskBadgeClass(level) {
        if (level === "높음") return "risk-high";
        if (level === "중간") return "risk-medium";
        if (level === "낮음") return "risk-low";
        return "risk-unknown";
    }

    function buildDetailedDescription() {
        if (["NOT_APPLICABLE", "UNABLE_TO_EVALUATE"].includes(statusKey)) {
            return "현재 이미지는 분석 기준을 충분히 만족하지 못해 결과 해석에 제한이 있습니다. Reality Defender 응답을 기준으로 최종 상태를 표시했으며, 히트맵 또는 영역 데이터가 제공된 경우에만 참고 시각화를 표시합니다. 가능한 경우 더 선명한 원본 이미지와 출처 정보를 함께 확인하는 것이 좋습니다.";
        }

        if (isHighRisk()) {
            return "분석 결과, 이미지 내 일부 영역에서 비정상적인 패턴이 감지되었습니다. 이 결과는 조작 여부를 확정하는 것이 아니라, 추가 확인이 필요한 참고 신호로 볼 수 있습니다. 특히 히트맵에서 강조되는 부분과 픽셀 패턴 차이가 나타나는 영역을 중심으로 원본 여부를 함께 검토하는 것이 좋습니다.";
        }

        if (isMediumRisk()) {
            return "분석 결과, 일부 영역에서 주변과 다른 픽셀 패턴 차이가 관찰될 수 있습니다. 다만 이 결과만으로 조작 여부를 단정할 수 없으며, 추가 확인이 필요한 참고용 신호로 해석하는 것이 적절합니다. 원본 출처, 촬영 맥락, 재저장 여부를 함께 확인하는 것이 좋습니다.";
        }

        return "분석 결과, 강한 조작 의심 신호는 크지 않습니다. 다만 AI 분석은 참고용 결과이므로 원본 출처와 촬영 맥락을 함께 확인하는 것이 좋습니다. 히트맵이나 영역 데이터가 제공된 경우에도 최종 판정 기준은 Reality Defender 결과를 우선합니다.";
    }

    function renderDetailedAnalysisRows(rows) {
        const container = document.getElementById("detailedAnalysisRows");
        container.innerHTML = rows.map(function(row) {
            return ""
                + '<div class="detail-row">'
                + '  <div class="font-semibold text-white">' + escapeHtml(row.item) + '</div>'
                + '  <div class="text-sm text-slate-200">' + escapeHtml(row.detected) + '</div>'
                + '  <div class="text-sm leading-6 text-slate-400">' + escapeHtml(row.description) + '</div>'
                + '  <div><span class="risk-badge ' + getRiskBadgeClass(row.risk) + '">' + escapeHtml(row.risk) + '</span></div>'
                + '</div>';
        }).join("");
    }

    function showNotice(message) {
        const box = document.getElementById("visualNoticeBox");
        box.textContent = message;
        box.classList.remove("hidden");
    }

    function hideNotice() {
        const box = document.getElementById("visualNoticeBox");
        box.textContent = "";
        box.classList.add("hidden");
    }

    function renderRegionBoxes(show) {
        const layer = document.getElementById("regionLayer");
        const image = document.getElementById("baseImage");
        layer.innerHTML = "";
        layer.classList.toggle("hidden", !show || anomalyRegions.length === 0);
        if (!show || anomalyRegions.length === 0) return;

        anomalyRegions.forEach(function(region) {
            const box = document.createElement("div");
            box.className = "region-box";
            box.dataset.label = region.label;

            const left = region.normalized ? region.x * 100 : region.x / Math.max(1, image.naturalWidth) * 100;
            const top = region.normalized ? region.y * 100 : region.y / Math.max(1, image.naturalHeight) * 100;
            const width = region.normalized ? region.width * 100 : region.width / Math.max(1, image.naturalWidth) * 100;
            const height = region.normalized ? region.height * 100 : region.height / Math.max(1, image.naturalHeight) * 100;

            box.style.left = left + "%";
            box.style.top = top + "%";
            box.style.width = width + "%";
            box.style.height = height + "%";
            layer.appendChild(box);
        });
    }

    function renderHeatmapOverlay() {
        const heatmapImage = document.getElementById("heatmapImage");
        if (!heatmapUrl) {
            heatmapImage.classList.add("hidden");
            heatmapImage.removeAttribute("src");
            document.getElementById("heatmapStatusText").textContent = "데이터 없음";
            showNotice("IMD 히트맵 데이터가 없습니다. 현재는 참고 가능한 히트맵 시각화가 제공되지 않았습니다.");
            return false;
        }

        heatmapImage.src = heatmapUrl;
        heatmapImage.classList.remove("hidden");
        document.getElementById("heatmapStatusText").textContent = "표시 가능";
        return true;
    }

    function setViewMode(mode) {
        const baseImage = document.getElementById("baseImage");
        const heatmapImage = document.getElementById("heatmapImage");
        const regionLayer = document.getElementById("regionLayer");
        const tabs = {
            original: document.getElementById("originalTab"),
            regions: document.getElementById("regionsTab"),
            overlay: document.getElementById("overlayTab")
        };
        Object.keys(tabs).forEach(function(key) {
            tabs[key].classList.toggle("active", key === mode);
        });

        baseImage.src = originalImageUrl;
        heatmapImage.classList.add("hidden");
        heatmapImage.removeAttribute("src");
        heatmapImage.style.opacity = "0";
        regionLayer.classList.add("hidden");
        regionLayer.innerHTML = "";
        renderRegionBoxes(false);
        hideNotice();

        if (mode === "original") {
            document.getElementById("visualDescription").textContent = "원본 이미지를 기준으로 Reality Defender의 최종 분석 결과를 함께 확인합니다.";
            return;
        }

        if (mode === "regions") {
            document.getElementById("visualDescription").textContent = "응답에 포함된 세부 의심 영역 좌표를 원본 이미지 위에 표시합니다.";
            renderRegionBoxes(true);
            if (anomalyRegions.length === 0) {
                showNotice("표시 가능한 세부 의심 영역이 없습니다. 이 경우 오버레이 탭의 전체 히트맵을 참고해주세요.");
            }
            return;
        }

        document.getElementById("visualDescription").textContent = "원본 이미지 위에 IMD / ManTraNet 히트맵을 반투명하게 겹쳐 픽셀 패턴 차이를 참고할 수 있습니다.";
        heatmapImage.style.opacity = "0.56";
        renderHeatmapOverlay();
    }

    function setTemporaryShareState(copied) {
        document.getElementById("shareIcon").setAttribute("data-lucide", copied ? "check" : "share-2");
        document.getElementById("shareLabel").textContent = copied ? "링크 복사됨" : "결과 공유";
        lucide.createIcons();
    }

    function legacyCopyToClipboard(text) {
        const textArea = document.createElement("textarea");
        textArea.value = text;
        textArea.style.position = "fixed";
        textArea.style.left = "-999999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        try {
            document.execCommand("copy");
            setTemporaryShareState(true);
            setTimeout(function() { setTemporaryShareState(false); }, 2000);
        } finally {
            document.body.removeChild(textArea);
        }
    }

    function setAiExplanationLoading(loading) {
        const button = document.getElementById("generateAiExplanationButton");
        const loadingBox = document.getElementById("aiExplanationLoading");
        button.disabled = loading;
        button.innerHTML = loading
            ? '<i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i><span>생성 중</span>'
            : '<i data-lucide="sparkles" class="h-4 w-4"></i><span>상세 해설 생성</span>';
        loadingBox.classList.toggle("hidden", !loading);
        loadingBox.classList.toggle("flex", loading);
        lucide.createIcons();
    }

    function aiRiskPresentation(riskLevel) {
        const levels = {
            HIGH: { label: "높음", className: "risk-high" },
            MEDIUM: { label: "중간", className: "risk-medium" },
            LOW: { label: "낮음", className: "risk-low" },
            UNKNOWN: { label: "정보 부족", className: "risk-unknown" }
        };
        return levels[String(riskLevel || "UNKNOWN").toUpperCase()] || levels.UNKNOWN;
    }

    function renderAiExplanation(data) {
        const risk = aiRiskPresentation(data.riskLevel);
        const riskBadge = document.getElementById("aiRiskBadge");
        riskBadge.textContent = risk.label;
        riskBadge.className = "risk-badge mt-1 " + risk.className;

        document.getElementById("aiSummary").textContent = data.summary || "";
        document.getElementById("aiExplanation").textContent = data.explanation || "";
        document.getElementById("aiActionGuide").textContent = data.actionGuide || "";
        document.getElementById("aiDisclaimer").textContent = data.disclaimer || "";

        const meta = [];
        if (data.model) meta.push(data.model);
        if (Number.isFinite(data.totalTokens)) meta.push("총 " + data.totalTokens + " 토큰");
        if (data.cached === true) meta.push("캐시된 해설");
        document.getElementById("aiExplanationMeta").textContent = meta.join(" · ");

        document.getElementById("aiExplanationResult").classList.remove("hidden");
        document.getElementById("aiExplanationError").classList.add("hidden");
        document.getElementById("generateAiExplanationButton").classList.add("hidden");
    }

    async function generateAiExplanation() {
        const errorBox = document.getElementById("aiExplanationError");
        errorBox.classList.add("hidden");
        errorBox.textContent = "";
        setAiExplanationLoading(true);

        try {
            const response = await fetch(
                contextPath + "/api/v1/ai/verifications/" + encodeURIComponent(resultId) + "/explanation",
                {
                    method: "POST",
                    credentials: "same-origin",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        verificationId: Number(resultId),
                        taskType: "EXPAND_EXISTING_EXPLANATION",
                        userQuestion: "기존 분석 설명을 바탕으로 판정의 의미와 확인 방법을 자세히 설명해 주세요.",
                        tone: "clear, calm, and detailed",
                        includeReportDraft: false
                    })
                }
            );
            const payload = await response.json();
            if (!response.ok || !payload.success || !payload.data) {
                throw new Error(payload.error?.message || "상세 해설을 생성하지 못했습니다.");
            }
            renderAiExplanation(payload.data);
        } catch (error) {
            errorBox.textContent = error.message || "상세 해설을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.";
            errorBox.classList.remove("hidden");
        } finally {
            setAiExplanationLoading(false);
        }
    }

    async function loadSavedAiExplanation() {
        try {
            const response = await fetch(
                contextPath + "/api/v1/ai/verifications/" + encodeURIComponent(resultId) + "/explanation",
                { method: "GET", credentials: "same-origin" }
            );
            const payload = await response.json();
            if (response.ok && payload.success && payload.data) {
                renderAiExplanation(payload.data);
            }
        } catch (error) {
            console.debug("저장된 Gemini 해설을 불러오지 못했습니다.", error);
        }
    }

    function setImageReviewLoading(loading) {
        const button = document.getElementById("generateImageReviewButton");
        const loadingBox = document.getElementById("imageReviewLoading");
        button.disabled = loading;
        button.innerHTML = loading
            ? '<i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i><span>검토 중</span>'
            : '<i data-lucide="scan-search" class="h-4 w-4"></i><span>이미지 다시 검토</span>';
        loadingBox.classList.toggle("hidden", !loading);
        loadingBox.classList.toggle("flex", loading);
        lucide.createIcons();
    }

    function imageReviewPresentation(status) {
        const statuses = {
            AGREES: { label: "1차 판독과 일치", className: "risk-medium" },
            DISAGREES: { label: "판독 결과 불일치", className: "risk-high" },
            INCONCLUSIVE: { label: "판단 보류", className: "risk-unknown" }
        };
        return statuses[String(status || "INCONCLUSIVE").toUpperCase()] || statuses.INCONCLUSIVE;
    }

    function renderImageReview(data) {
        const presentation = imageReviewPresentation(data.crossCheckStatus);
        const badge = document.getElementById("imageReviewStatusBadge");
        badge.textContent = presentation.label;
        badge.className = "risk-badge mt-1 " + presentation.className;

        document.getElementById("imageReviewConclusion").textContent = data.combinedConclusion || "";
        document.getElementById("imageReviewSummary").textContent = data.visualSummary || "";
        document.getElementById("imageReviewLimitations").textContent = data.limitations ||
            "이미지 기반 AI 검토는 진위를 확정하는 증거가 아닙니다.";

        const indicators = document.getElementById("imageReviewIndicators");
        indicators.innerHTML = "";
        const items = Array.isArray(data.visualIndicators) && data.visualIndicators.length > 0
            ? data.visualIndicators
            : ["명확하게 구분할 수 있는 시각적 단서가 확인되지 않았습니다."];
        items.forEach(function(item) {
            const li = document.createElement("li");
            li.className = "flex gap-2";
            const marker = document.createElement("span");
            marker.className = "mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-300";
            const textNode = document.createElement("span");
            textNode.textContent = item;
            li.appendChild(marker);
            li.appendChild(textNode);
            indicators.appendChild(li);
        });

        const meta = [];
        if (data.model) meta.push(data.model);
        if (data.confidenceLevel) meta.push("시각 검토 신뢰 수준 " + data.confidenceLevel);
        if (Number.isFinite(data.totalTokens)) meta.push("총 " + data.totalTokens + " 토큰");
        if (data.cached === true) meta.push("저장된 검토 결과");
        document.getElementById("imageReviewMeta").textContent = meta.join(" · ");

        document.getElementById("imageReviewResult").classList.remove("hidden");
        document.getElementById("imageReviewError").classList.add("hidden");
        document.getElementById("generateImageReviewButton").classList.add("hidden");
    }

    async function generateImageReview() {
        const errorBox = document.getElementById("imageReviewError");
        errorBox.classList.add("hidden");
        errorBox.textContent = "";
        setImageReviewLoading(true);
        try {
            const response = await fetch(
                contextPath + "/api/v1/ai/verifications/" + encodeURIComponent(resultId) + "/image-review",
                { method: "POST", credentials: "same-origin" }
            );
            const payload = await response.json();
            if (!response.ok || !payload.success || !payload.data) {
                throw new Error(payload.error?.message || "AI 이미지 2차 검증을 완료하지 못했습니다.");
            }
            renderImageReview(payload.data);
        } catch (error) {
            errorBox.textContent = error.message || "AI 이미지 2차 검증을 완료하지 못했습니다.";
            errorBox.classList.remove("hidden");
        } finally {
            setImageReviewLoading(false);
        }
    }

    async function loadSavedImageReview() {
        try {
            const response = await fetch(
                contextPath + "/api/v1/ai/verifications/" + encodeURIComponent(resultId) + "/image-review",
                { method: "GET", credentials: "same-origin" }
            );
            const payload = await response.json();
            if (response.ok && payload.success && payload.data) {
                renderImageReview(payload.data);
            }
        } catch (error) {
            console.debug("저장된 AI 이미지 검토를 불러오지 못했습니다.", error);
        }
    }

    function reviewRequestTypeLabel(requestType) {
        const labels = {
            FALSE_POSITIVE: "오탐 신고: 실제인데 조작으로 판정",
            FALSE_NEGATIVE: "미탐 신고: 조작인데 실제로 판정",
            RECHECK: "판정 재검토"
        };
        return labels[String(requestType || "RECHECK").toUpperCase()] || labels.RECHECK;
    }

    function reviewRequestStatusPresentation(status) {
        const statuses = {
            PENDING: { label: "접수 완료", className: "risk-medium" },
            REVIEWING: { label: "검토 중", className: "risk-medium" },
            COMPLETED: { label: "검토 완료", className: "risk-low" },
            REJECTED: { label: "요청 반려", className: "risk-high" }
        };
        return statuses[String(status || "PENDING").toUpperCase()] || statuses.PENDING;
    }

    function setReviewRequestLoading(loading, submitting) {
        const button = document.getElementById("submitReviewRequestButton");
        const loadingBox = document.getElementById("reviewRequestLoading");
        button.disabled = loading;
        button.innerHTML = loading && submitting
            ? '<i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i><span>접수 중</span>'
            : '<i data-lucide="send" class="h-4 w-4"></i><span>재검토 요청 접수</span>';
        loadingBox.classList.toggle("hidden", !loading || submitting);
        loadingBox.classList.toggle("flex", loading && !submitting);
        lucide.createIcons();
    }

    function showReviewRequestError(message) {
        const errorBox = document.getElementById("reviewRequestError");
        errorBox.textContent = message;
        errorBox.classList.remove("hidden");
    }

    function renderReviewRequest(data) {
        const presentation = reviewRequestStatusPresentation(data.status);
        const badge = document.getElementById("reviewRequestStatusBadge");
        badge.textContent = presentation.label;
        badge.className = "risk-badge " + presentation.className;

        document.getElementById("reviewRequestTypeText").textContent = reviewRequestTypeLabel(data.requestType);
        document.getElementById("reviewRequestReasonText").textContent = data.reason || "작성된 요청 사유가 없습니다.";

        const reviewerNoteWrap = document.getElementById("reviewerNoteWrap");
        const reviewerNote = String(data.reviewerNote || "").trim();
        document.getElementById("reviewerNoteText").textContent = reviewerNote;
        reviewerNoteWrap.classList.toggle("hidden", reviewerNote.length === 0);

        const meta = [];
        if (data.id != null) meta.push("요청 번호 " + data.id);
        if (data.regDt) meta.push("접수 " + data.regDt);
        if (data.updDt && data.updDt !== data.regDt) meta.push("변경 " + data.updDt);
        document.getElementById("reviewRequestMeta").textContent = meta.join(" · ");

        document.getElementById("reviewRequestForm").classList.add("hidden");
        document.getElementById("reviewRequestError").classList.add("hidden");
        document.getElementById("reviewRequestResult").classList.remove("hidden");
    }

    async function submitReviewRequest(event) {
        event.preventDefault();
        const reasonInput = document.getElementById("reviewRequestReason");
        const reason = reasonInput.value.trim();
        if (!reason) {
            showReviewRequestError("재검토 요청 사유를 입력해 주세요.");
            reasonInput.focus();
            return;
        }

        document.getElementById("reviewRequestError").classList.add("hidden");
        setReviewRequestLoading(true, true);
        try {
            const response = await fetch(
                contextPath + "/api/v1/verifications/" + encodeURIComponent(resultId) + "/review-requests",
                {
                    method: "POST",
                    credentials: "same-origin",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        requestType: document.getElementById("reviewRequestType").value,
                        reason: reason
                    })
                }
            );
            const payload = await response.json();
            if (!response.ok || !payload.success || !payload.data) {
                throw new Error(payload.error?.message || "재검토 요청을 접수하지 못했습니다.");
            }
            renderReviewRequest(payload.data);
        } catch (error) {
            showReviewRequestError(error.message || "재검토 요청을 접수하지 못했습니다.");
        } finally {
            setReviewRequestLoading(false, true);
        }
    }

    async function loadSavedReviewRequest() {
        setReviewRequestLoading(true, false);
        try {
            const response = await fetch(
                contextPath + "/api/v1/review-requests",
                { method: "GET", credentials: "same-origin" }
            );
            const payload = await response.json();
            if (!response.ok || !payload.success) {
                throw new Error(payload.error?.message || "재검토 요청 상태를 불러오지 못했습니다.");
            }
            const requests = Array.isArray(payload.data) ? payload.data : [];
            const savedRequest = requests.find(function(item) {
                return String(item.verificationId) === String(resultId);
            });
            if (savedRequest) renderReviewRequest(savedRequest);
        } catch (error) {
            showReviewRequestError(error.message || "재검토 요청 상태를 불러오지 못했습니다.");
        } finally {
            setReviewRequestLoading(false, false);
        }
    }

    function reportPdfItem(report) {
        const row = document.createElement("div");
        row.className = "flex flex-wrap items-center justify-between gap-3 rounded-xl border border-white/10 bg-slate-950/50 p-3";

        const info = document.createElement("div");
        info.className = "min-w-0";
        const name = document.createElement("p");
        name.className = "truncate text-sm font-semibold text-white";
        name.textContent = report.fileName || "신고 제출용 PDF";
        const meta = document.createElement("p");
        meta.className = "mt-1 text-xs text-slate-500";
        meta.textContent = [report.regDt, report.aiReportDraftIncluded ? "AI 신고 문구 포함" : ""].filter(Boolean).join(" · ");
        info.append(name, meta);

        const actions = document.createElement("div");
        actions.className = "flex items-center gap-2";
        const download = document.createElement("a");
        download.className = "inline-flex min-h-10 items-center gap-2 rounded-lg bg-sky-400 px-3 text-sm font-bold text-slate-950 transition hover:bg-sky-300";
        download.href = contextPath + (report.downloadUrl || ("/api/v1/reports/" + report.id + "/download"));
        download.innerHTML = '<i data-lucide="download" class="h-4 w-4"></i><span>다운로드</span>';
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "inline-flex h-10 w-10 items-center justify-center rounded-lg border border-rose-500/30 text-rose-300 transition hover:bg-rose-500/15";
        remove.title = "신고자료 삭제";
        remove.setAttribute("aria-label", "신고자료 삭제");
        remove.innerHTML = '<i data-lucide="trash-2" class="h-4 w-4"></i>';
        remove.addEventListener("click", function() { deleteReportPdf(report.id); });
        actions.append(download, remove);
        row.append(info, actions);
        return row;
    }

    function renderReportPdfs(reports) {
        const list = document.getElementById("reportPdfList");
        list.innerHTML = "";
        const matched = (Array.isArray(reports) ? reports : []).filter(function(report) {
            return String(report.verificationId) === String(resultId);
        });
        matched.forEach(function(report) { list.appendChild(reportPdfItem(report)); });
        document.getElementById("reportPdfListWrap").classList.toggle("hidden", matched.length === 0);
        lucide.createIcons();
    }

    async function loadReportPdfs() {
        try {
            const response = await fetch(contextPath + "/api/v1/reports", { credentials: "same-origin" });
            const payload = await response.json();
            if (response.ok && payload.success) renderReportPdfs(payload.data);
        } catch (error) {
            console.debug("신고자료 목록을 불러오지 못했습니다.", error);
        }
    }

    async function deleteReportPdf(reportId) {
        if (!window.confirm("이 신고자료를 삭제하시겠습니까?")) return;
        const errorBox = document.getElementById("reportPdfError");
        errorBox.classList.add("hidden");
        try {
            const response = await fetch(contextPath + "/api/v1/reports/" + encodeURIComponent(reportId), {
                method: "DELETE",
                credentials: "same-origin"
            });
            const payload = await response.json();
            if (!response.ok || !payload.success) {
                throw new Error(payload.error?.message || "신고자료를 삭제하지 못했습니다.");
            }
            await loadReportPdfs();
        } catch (error) {
            errorBox.textContent = error.message || "신고자료를 삭제하지 못했습니다.";
            errorBox.classList.remove("hidden");
        }
    }

    async function createReportPdf(event) {
        event.preventDefault();
        const reasonInput = document.getElementById("reportPdfReason");
        const reason = reasonInput.value.trim();
        const errorBox = document.getElementById("reportPdfError");
        if (!reason) {
            errorBox.textContent = "신고 사유를 입력해 주세요.";
            errorBox.classList.remove("hidden");
            reasonInput.focus();
            return;
        }

        const button = document.getElementById("createReportPdfButton");
        button.disabled = true;
        button.innerHTML = '<i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i><span>신고자료 생성 중</span>';
        errorBox.classList.add("hidden");
        lucide.createIcons();
        try {
            const response = await fetch(
                contextPath + "/api/v1/verifications/" + encodeURIComponent(resultId) + "/reports",
                {
                    method: "POST",
                    credentials: "same-origin",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        reportReason: reason,
                        sourceUrl: document.getElementById("reportPdfSourceUrl").value.trim(),
                        includeOriginalImage: document.getElementById("reportPdfOriginal").checked,
                        includeHeatmap: document.getElementById("reportPdfHeatmap").checked,
                        includeAiReportDraft: document.getElementById("reportPdfAiDraft").checked
                    })
                }
            );
            const payload = await response.json();
            if (!response.ok || !payload.success || !payload.data) {
                throw new Error(payload.error?.message || "PDF 신고자료를 생성하지 못했습니다.");
            }
            await loadReportPdfs();
            window.location.href = contextPath + payload.data.downloadUrl;
        } catch (error) {
            errorBox.textContent = error.message || "PDF 신고자료를 생성하지 못했습니다.";
            errorBox.classList.remove("hidden");
        } finally {
            button.disabled = false;
            button.innerHTML = '<i data-lucide="file-plus-2" class="h-4 w-4"></i><span>PDF 신고자료 만들기</span>';
            lucide.createIcons();
        }
    }

    document.getElementById("topGlow").classList.add(config.glowClass);
    document.getElementById("verdictGlow").classList.add(config.glowClass);
    document.getElementById("verdictIconWrap").innerHTML = '<i data-lucide="' + config.icon + '" class="h-6 w-6 ' + config.badgeClass + '"></i>';
    document.getElementById("verdictLabel").className = "text-2xl font-bold " + config.badgeClass;
    document.getElementById("verdictLabel").textContent = config.label;
    document.getElementById("scoreValue").textContent = String(confidence);
    document.getElementById("scoreProgress").className = "h-full transition-transform duration-700 ease-out " + config.progressClass;
    document.getElementById("scoreProgress").style.transform = "translateX(-" + (100 - confidence) + "%)";
    document.getElementById("scoreHint").textContent =
        confidence >= 85 ? "Reality Defender 기준으로 강한 조작 의심 신호가 감지된 상태입니다." :
        confidence >= 45 ? "Reality Defender 기준으로 추가 확인이 필요한 신호가 있습니다." :
        "Reality Defender 기준으로 강한 조작 의심 신호는 크지 않습니다.";
    document.getElementById("explanationText").textContent = buildDetailedDescription();
    document.getElementById("apiProviderText").textContent = apiProvider || "Reality Defender";
    document.getElementById("heatmapProviderText").textContent = firstValue(combinedResult, ["imdHeatmapResult.modelName", "heatmapModelName"]) || "IMD / ManTraNet";
    document.getElementById("regionCountText").textContent = anomalyRegions.length + "개";
    document.getElementById("heatmapStatusText").textContent = heatmapUrl ? "표시 가능" : "데이터 없음";

    document.getElementById("baseImage").addEventListener("load", function() {
        const activeMode = document.querySelector(".mode-tab.active")?.id === "regionsTab" ? "regions" : null;
        if (activeMode) renderRegionBoxes(true);
    });

    document.getElementById("heatmapImage").addEventListener("error", function() {
        document.getElementById("heatmapStatusText").textContent = "로딩 실패";
        this.classList.add("hidden");
        showNotice("히트맵 이미지를 불러오지 못했습니다. 파일 경로 또는 응답 필드를 확인해주세요.");
    });

    document.getElementById("shareButton").addEventListener("click", function() {
        const shareUrl = window.location.origin + contextPath + "/detail/" + resultId;
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(shareUrl).then(function() {
                setTemporaryShareState(true);
                setTimeout(function() { setTemporaryShareState(false); }, 2000);
            }).catch(function() {
                legacyCopyToClipboard(shareUrl);
            });
        } else {
            legacyCopyToClipboard(shareUrl);
        }
    });

    document.getElementById("generateAiExplanationButton").addEventListener("click", generateAiExplanation);
    document.getElementById("generateImageReviewButton").addEventListener("click", generateImageReview);
    document.getElementById("reviewRequestForm").addEventListener("submit", submitReviewRequest);
    document.getElementById("reviewRequestReason").addEventListener("input", function() {
        document.getElementById("reviewRequestReasonCount").textContent = this.value.length + " / 1000";
    });
    document.getElementById("reportPdfForm").addEventListener("submit", createReportPdf);
    document.getElementById("reportPdfReason").addEventListener("input", function() {
        document.getElementById("reportPdfReasonCount").textContent = this.value.length + " / 2000";
    });

    renderDetailedAnalysisRows(getDetailedAnalysisRows(combinedResult));
    setViewMode("overlay");
    loadSavedAiExplanation();
    loadSavedImageReview();
    loadSavedReviewRequest();
    loadReportPdfs();
    lucide.createIcons();
</script>
</body>
</html>

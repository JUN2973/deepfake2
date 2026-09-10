<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%--
  체크리스트 기준 주석: 구현(결과 상세): 새벽 분석 결과, 판별 점수, 진위 여부 요약을 표시한다.
--%>
<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>분석 결과 - DeepScan</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
    <style>
        body {
            font-family: "Pretendard", "Noto Sans KR", sans-serif;
            background: #020617;
        }
    </style>
</head>
<body class="min-h-screen bg-slate-950 text-white">
<div class="pointer-events-none fixed inset-0 overflow-hidden">
    <div id="topGlow" class="absolute top-20 left-1/4 h-96 w-96 rounded-full blur-3xl opacity-20"></div>
    <div class="absolute bottom-20 right-1/4 h-96 w-96 rounded-full bg-cyan-500 blur-3xl opacity-10"></div>
</div>

<div class="relative mx-auto max-w-7xl px-4 pb-16 pt-8">
    <div class="mb-8 flex items-center justify-between">
        <button
                type="button"
                onclick="goPage('<%= contextPath %>/')"
                class="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-4 py-2 text-sm text-slate-200 transition hover:bg-white/10"
        >
            <i data-lucide="arrow-left" class="h-4 w-4"></i>
            홈으로
        </button>
        <div class="inline-flex items-center gap-2 text-sm text-slate-400">
            <i data-lucide="shield" class="h-4 w-4 text-sky-400"></i>
            AI 분석 리포트
        </div>
    </div>

    <div class="mb-8 text-center">
        <h1 class="mb-2 text-4xl font-bold md:text-5xl">분석 결과 리포트</h1>
        <p class="text-slate-400">검증 결과를 기반으로 생성한 이미지 진단 요약입니다.</p>
    </div>

    <div class="grid gap-8 lg:grid-cols-2">
        <section class="space-y-4">
            <div class="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/60 backdrop-blur-xl">
                <div class="flex border-b border-white/10">
                    <button
                            id="originalTab"
                            type="button"
                            onclick="setViewMode('original')"
                            class="flex-1 border-b-2 border-sky-500 bg-sky-500/20 px-4 py-3 text-sm font-semibold text-sky-400"
                    >
                        <span class="flex items-center justify-center gap-2">
                            <i data-lucide="image" class="h-4 w-4"></i>
                            원본 보기
                        </span>
                    </button>
                    <button
                            id="analysisTab"
                            type="button"
                            onclick="setViewMode('analysis')"
                            class="flex-1 px-4 py-3 text-sm font-semibold text-slate-400 transition hover:bg-white/5 hover:text-white"
                    >
                        <span class="flex items-center justify-center gap-2">
                            <i data-lucide="scan-search" class="h-4 w-4"></i>
                            분석 안내
                        </span>
                    </button>
                </div>

                <div class="p-6">
                    <div id="originalView">
                        <img src="${result.publicUrl}" alt="${result.originalName}" class="w-full rounded-2xl bg-slate-950 object-contain">
                        <div class="mt-4 rounded-2xl border border-white/10 bg-slate-800/40 p-4 text-sm text-slate-300">
                            <div class="flex items-center gap-2">
                                <i data-lucide="file-text" class="h-4 w-4 text-sky-400"></i>
                                <span class="truncate">${result.originalName}</span>
                            </div>
                        </div>
                    </div>

                    <div id="analysisView" class="hidden rounded-2xl border border-amber-400/20 bg-amber-500/10 p-5">
                        <div class="mb-2 flex items-center gap-2 text-amber-300">
                            <i data-lucide="info" class="h-5 w-5"></i>
                            <span class="font-semibold">분석 영역 데이터 없음</span>
                        </div>
                        <p class="text-sm leading-6 text-amber-100/90">
                            현재 저장된 결과에는 실제 의심 부위 좌표 데이터가 포함되어 있지 않습니다.
                            그래서 결과창에서는 원본 이미지와 판정 정보만 표시하고 있습니다.
                        </p>
                    </div>
                </div>
            </div>
        </section>

        <section class="space-y-6">
            <div class="relative">
                <div id="verdictGlow" class="absolute inset-0 rounded-3xl blur-2xl opacity-25"></div>
                <div id="verdictCard" class="relative rounded-3xl border border-white/10 bg-slate-900/60 p-8 backdrop-blur-xl">
                    <div class="mb-6 flex items-center gap-4">
                        <div id="verdictIconWrap" class="rounded-2xl p-3"></div>
                        <div>
                            <div class="mb-1 text-sm text-slate-400">최종 판정</div>
                            <div id="verdictLabel" class="text-2xl font-bold"></div>
                        </div>
                    </div>

                    <div class="mb-4 text-center">
                        <div class="text-6xl font-bold text-white">
                            <span id="scoreValue">0</span>
                            <span class="text-3xl text-slate-400">/100</span>
                        </div>
                        <div class="mt-2 text-sm text-slate-400">신뢰도 점수</div>
                    </div>

                    <div class="h-3 overflow-hidden rounded-full bg-slate-800">
                        <div id="scoreProgress" class="h-full transition-transform duration-700 ease-out"></div>
                    </div>
                    <div id="scoreHint" class="mt-3 text-xs text-slate-500"></div>
                </div>
            </div>

            <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                <h2 class="mb-3 text-lg font-semibold text-white">분석 설명</h2>
                <p id="explanationText" class="mb-4 leading-7 text-slate-300"></p>
                <div class="rounded-2xl border border-sky-500/20 bg-sky-500/10 p-4 text-sm leading-6 text-sky-100">
                    현재 결과는 저장된 검증 기록 기준으로 표시됩니다. 실제 부위별 오버레이는 별도 좌표 데이터가 있어야 표시할 수 있습니다.
                </div>
            </div>

            <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                <h2 class="mb-4 text-lg font-semibold text-white">기술 정보</h2>
                <div class="space-y-3">
                    <div class="flex items-center justify-between border-b border-white/5 py-2">
                        <span class="text-sm text-slate-400">API 제공사</span>
                        <span class="font-medium text-white" id="apiProviderText">-</span>
                    </div>
                    <div class="flex items-center justify-between border-b border-white/5 py-2">
                        <span class="text-sm text-slate-400">분석 모델</span>
                        <span class="font-medium text-white" id="modelNameText">AI Image Detection</span>
                    </div>
                    <div class="flex items-center justify-between border-b border-white/5 py-2">
                        <span class="text-sm text-slate-400">응답 시간</span>
                        <span class="font-medium text-white" id="responseTimeText">-</span>
                    </div>
                    <div class="flex items-center justify-between py-2">
                        <span class="text-sm text-slate-400">분석 일시</span>
                        <span class="font-medium text-white">${result.regDt}</span>
                    </div>
                </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
                <button
                        type="button"
                        onclick="goPage('<%= contextPath %>/history')"
                        class="flex items-center justify-center gap-2 rounded-2xl border border-white/15 bg-white/5 px-6 py-4 font-semibold text-white transition hover:bg-white/10"
                >
                    <i data-lucide="history" class="h-5 w-5"></i>
                    검증기록
                </button>
                <button
                        id="shareButton"
                        type="button"
                        class="flex items-center justify-center gap-2 rounded-2xl bg-sky-500 px-6 py-4 font-semibold text-white transition hover:bg-sky-400"
                >
                    <i id="shareIcon" data-lucide="share-2" class="h-5 w-5"></i>
                    <span id="shareLabel">결과 공유</span>
                </button>
            </div>
        </section>
    </div>
</div>

<script>
    const contextPath = "<%= contextPath %>";
    const verdict = "${result.verdict}";
    const score = Number("${result.score}");
    const rawResponseText = `${result.apiRaw != null ? result.apiRaw : ""}`;
    const resultId = "${result.id}";
    const confidence = Number.isNaN(score) ? 0 : Math.round(score * 100);

    const statusConfig = {
        SAFE: { label: "안전", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500", text: "정상 이미지일 가능성이 높습니다." },
        AUTHENTIC: { label: "안전", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500", text: "정상 이미지일 가능성이 높습니다." },
        SUSPECT: { label: "의심", glowClass: "bg-yellow-500", badgeClass: "text-yellow-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500", text: "추가 확인이 필요한 의심 신호가 감지되었습니다." },
        SUSPICIOUS: { label: "의심", glowClass: "bg-yellow-500", badgeClass: "text-yellow-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500", text: "추가 확인이 필요한 의심 신호가 감지되었습니다." },
        HIGH_RISK: { label: "위험", glowClass: "bg-red-500", badgeClass: "text-red-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-red-500 to-rose-500", text: "조작 또는 생성 이미지일 가능성이 높습니다." },
        FAKE: { label: "위험", glowClass: "bg-red-500", badgeClass: "text-red-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-red-500 to-rose-500", text: "조작 또는 생성 이미지일 가능성이 높습니다." }
    };

    const fallbackStatus = confidence >= 70 ? statusConfig.HIGH_RISK : confidence >= 35 ? statusConfig.SUSPECT : statusConfig.SAFE;
    const config = statusConfig[verdict] || fallbackStatus;

    let parsedJson = null;
    try {
        parsedJson = rawResponseText ? JSON.parse(rawResponseText) : null;
    } catch (error) {
        parsedJson = null;
    }

    function goPage(path) {
        window.location.href = path;
    }

    function setViewMode(mode) {
        const originalTab = document.getElementById("originalTab");
        const analysisTab = document.getElementById("analysisTab");
        const originalView = document.getElementById("originalView");
        const analysisView = document.getElementById("analysisView");

        if (mode === "original") {
            originalTab.className = "flex-1 border-b-2 border-sky-500 bg-sky-500/20 px-4 py-3 text-sm font-semibold text-sky-400";
            analysisTab.className = "flex-1 px-4 py-3 text-sm font-semibold text-slate-400 transition hover:bg-white/5 hover:text-white";
            originalView.classList.remove("hidden");
            analysisView.classList.add("hidden");
        } else {
            analysisTab.className = "flex-1 border-b-2 border-sky-500 bg-sky-500/20 px-4 py-3 text-sm font-semibold text-sky-400";
            originalTab.className = "flex-1 px-4 py-3 text-sm font-semibold text-slate-400 transition hover:bg-white/5 hover:text-white";
            analysisView.classList.remove("hidden");
            originalView.classList.add("hidden");
        }
        lucide.createIcons();
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
            setTimeout(function () { setTemporaryShareState(false); }, 2000);
        } finally {
            document.body.removeChild(textArea);
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
        confidence >= 90 ? "매우 높은 신뢰도" :
        confidence >= 70 ? "높은 신뢰도" :
        confidence >= 50 ? "중간 수준의 신뢰도" :
        "참고 수준의 신뢰도";
    document.getElementById("explanationText").textContent = parsedJson && parsedJson.explanation ? parsedJson.explanation : config.text;
    document.getElementById("apiProviderText").textContent = "${result.apiProvider}" || "-";
    document.getElementById("modelNameText").textContent = parsedJson && parsedJson.modelName ? parsedJson.modelName : "AI Image Detection";
    document.getElementById("responseTimeText").textContent = parsedJson && parsedJson.responseTime ? parsedJson.responseTime + "ms" : "-";

    document.getElementById("shareButton").addEventListener("click", function () {
        const shareUrl = window.location.origin + contextPath + "/detail/" + resultId;
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(shareUrl).then(function () {
                setTemporaryShareState(true);
                setTimeout(function () { setTemporaryShareState(false); }, 2000);
            }).catch(function () {
                legacyCopyToClipboard(shareUrl);
            });
        } else {
            legacyCopyToClipboard(shareUrl);
        }
    });

    setViewMode("original");
    lucide.createIcons();
</script>
</body>
</html>

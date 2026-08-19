<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  체크리스트 기준 주석: 구현(딥페이크 판별): 분석 진행 상태와 로딩 화면을 구성한다.
--%>
<%
  String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>이미지 분석 중 - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700;800&display=swap" rel="stylesheet">
  <style>
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: #020617;
    }
  </style>
</head>
<body class="min-h-screen bg-slate-950">
  <div class="relative flex min-h-screen items-center justify-center overflow-hidden p-4">
    <div class="absolute inset-0">
      <div class="absolute left-1/4 top-20 h-96 w-96 animate-pulse rounded-full bg-sky-600/30 blur-3xl"></div>
      <div class="absolute bottom-20 right-1/4 h-96 w-96 animate-pulse rounded-full bg-cyan-600/30 blur-3xl"></div>
    </div>

    <section class="relative z-10 max-w-md text-center">
      <div class="mb-8">
        <div class="relative inline-block">
          <div class="absolute inset-0 rounded-full bg-gradient-to-r from-sky-600 to-cyan-600 opacity-60 blur-2xl"></div>
          <i data-lucide="loader-2" class="relative h-24 w-24 animate-spin text-sky-400"></i>
        </div>
      </div>

      <h2 id="title" class="mb-3 text-4xl font-bold text-white">이미지 분석 중...</h2>
      <p id="desc" class="mb-8 text-lg text-slate-400">
        AI가 이미지의 조작 흔적과 생성 여부를 분석하고 있습니다.
      </p>
    </section>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    const query = new URLSearchParams(window.location.search);
    const rawId = query.get("id");
    const verificationId = rawId ? String(rawId) : "";
    const titleEl = document.getElementById("title");
    const descEl = document.getElementById("desc");

    async function pollResult() {
      if (!verificationId) {
        titleEl.textContent = "분석 대상을 찾을 수 없습니다";
        descEl.textContent = "이전 화면으로 돌아가 다시 시도해주세요.";
        return;
      }

      for (let i = 0; i < 30; i += 1) {
        try {
          const response = await fetch(contextPath + "/api/v1/verifications/" + encodeURIComponent(verificationId), {
            credentials: "same-origin"
          });
          const json = await response.json().catch(function () {
            return null;
          });

          if (response.ok && json && json.success && json.data) {
            titleEl.textContent = "분석 완료";
            descEl.textContent = "상세 결과 화면으로 이동하고 있습니다.";
            setTimeout(function () {
              window.location.href = contextPath + "/detail/" + encodeURIComponent(verificationId);
            }, 700);
            return;
          }
        } catch (error) {
        }

        await new Promise(function (resolve) {
          setTimeout(resolve, 700);
        });
      }

      titleEl.textContent = "결과 확인에 실패했습니다";
      descEl.textContent = "잠시 후 다시 시도하거나 검증 기록에서 확인해주세요.";
    }

    lucide.createIcons();
    pollResult();
  </script>
</body>
</html>

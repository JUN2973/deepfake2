<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  체크리스트 기준 주석: 구현(자주하는질문): FAQ 목록과 질문/답변 펼치기 UI를 구성한다.
--%>
<%
  String contextPath = request.getContextPath();
  request.setAttribute("activePage", "faq");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>FAQ - DeepScan</title>
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
<body class="ds-page min-h-screen bg-slate-950 text-white pt-32 pb-12 px-4 relative">
  <div class="absolute inset-0 overflow-hidden">
    <div class="absolute top-20 left-1/4 w-96 h-96 bg-sky-600/30 rounded-full blur-3xl"></div>
    <div class="absolute bottom-20 right-1/4 w-96 h-96 bg-cyan-600/30 rounded-full blur-3xl"></div>
  </div>

  <%@ include file="common/dashboard-nav.jspf" %>

  <div class="max-w-5xl mx-auto relative">
    <div class="text-center mb-12">
      <div class="relative inline-block mb-6">
        <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-xl opacity-75"></div>
        <div class="relative w-20 h-20 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-3xl flex items-center justify-center mx-auto">
          <i data-lucide="help-circle" class="w-10 h-10 text-white"></i>
        </div>
      </div>
      <h1 class="text-5xl md:text-6xl font-bold mb-4">
        <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">자주 묻는 질문</span>
      </h1>
      <p class="text-xl text-slate-400 max-w-2xl mx-auto">서비스 이용에 필요한 내용을 항목별로 정리했습니다.</p>
    </div>

    <div class="mb-8">
      <div class="relative">
        <i data-lucide="search" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
        <input
          id="searchQuery"
          type="text"
          placeholder="궁금한 내용을 입력하세요."
          class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-2xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all backdrop-blur-xl"
        >
      </div>
    </div>

    <div id="categoryFilters" class="flex flex-wrap gap-3 mb-12"></div>
    <div id="faqList" class="space-y-4"></div>

    <div id="emptyState" class="hidden text-center py-16">
      <i data-lucide="help-circle" class="w-16 h-16 text-slate-600 mx-auto mb-4"></i>
      <p class="text-slate-400 text-lg">검색 결과가 없습니다.</p>
    </div>

    <div class="mt-16 relative group">
      <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-20"></div>
      <div class="relative bg-slate-900/50 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
        <div class="text-center mb-8">
          <h2 class="text-3xl font-bold mb-3">답을 찾지 못하셨나요?</h2>
          <p class="text-slate-400">커뮤니티 또는 고객지원 채널을 이용해 주세요.</p>
        </div>
        <div class="grid md:grid-cols-3 gap-6">
          <button
            type="button"
            onclick="goPage('<%= contextPath %>/community')"
            class="p-6 bg-white/5 hover:bg-white/10 rounded-2xl transition-all border border-white/10 hover:border-white/20"
          >
            <i data-lucide="message-circle" class="w-8 h-8 text-sky-400 mx-auto mb-3"></i>
            <h3 class="font-semibold mb-2">커뮤니티</h3>
            <p class="text-sm text-slate-400">사례와 검증 정보 확인</p>
          </button>
          <button type="button" class="p-6 bg-white/5 hover:bg-white/10 rounded-2xl transition-all border border-white/10 hover:border-white/20">
            <i data-lucide="mail" class="w-8 h-8 text-cyan-400 mx-auto mb-3"></i>
            <h3 class="font-semibold mb-2">이메일</h3>
            <p class="text-sm text-slate-400">support@deepscan.com</p>
          </button>
          <button type="button" class="p-6 bg-white/5 hover:bg-white/10 rounded-2xl transition-all border border-white/10 hover:border-white/20">
            <i data-lucide="phone" class="w-8 h-8 text-blue-400 mx-auto mb-3"></i>
            <h3 class="font-semibold mb-2">전화</h3>
            <p class="text-sm text-slate-400">1588-1234</p>
          </button>
        </div>
      </div>
    </div>
  </div>

  <script>
    const faqs = [
      { category: "일반", question: "DeepScan은 무엇인가요?", answer: "업로드한 이미지에서 조작 가능성과 관련 신호를 확인하는 서비스입니다." },
      { category: "일반", question: "서비스는 무료인가요?", answer: "기본적인 이미지 검증 기능은 무료로 이용할 수 있습니다. 추가 기능이나 API 연동은 별도 정책이 적용될 수 있습니다." },
      { category: "사용법", question: "어떤 이미지 형식을 지원하나요?", answer: "JPG, PNG, WebP 등 일반적인 이미지 형식을 지원합니다. 파일 크기는 최대 10MB까지 업로드할 수 있습니다." },
      { category: "사용법", question: "분석에는 얼마나 시간이 걸리나요?", answer: "파일 크기와 서버 상태에 따라 달라집니다. 분석이 끝나면 결과 화면으로 이동합니다." },
      { category: "사용법", question: "업로드한 이미지는 어떻게 처리되나요?", answer: "이미지는 분석과 기록 제공에 필요한 범위에서 처리됩니다. 보관 여부는 서비스 정책을 확인해 주세요." },
      { category: "정확도", question: "탐지 정확도는 어느 정도인가요?", answer: "분석 결과는 참고용입니다. 점수와 근거를 검토하고, 중요한 판단에는 원본과 출처를 확인해 주세요." },
      { category: "정확도", question: "모든 딥페이크를 탐지할 수 있나요?", answer: "새로운 생성 방식이나 화질이 낮은 이미지는 탐지가 어려울 수 있습니다. 결과만으로 진위를 단정하지 마세요." },
      { category: "보안", question: "내 데이터는 안전한가요?", answer: "업로드한 데이터는 분석 기능에 필요한 범위에서 처리됩니다. 민감한 이미지라면 업로드 전에 서비스 정책을 확인해 주세요." },
      { category: "보안", question: "이미지 데이터를 외부와 공유하나요?", answer: "사용자 동의 없이 업로드한 이미지나 분석 결과를 외부에 공유하지 않습니다." },
      { category: "기술", question: "어떤 분석 방식을 사용하나요?", answer: "이미지 패턴, 이상 영역, 파일 메타데이터 등 여러 신호를 확인합니다." },
      { category: "기술", question: "메타데이터도 확인하나요?", answer: "메타데이터가 남아 있으면 EXIF 등 파일 정보를 분석에 참고합니다." },
      { category: "계정", question: "회원가입은 필수인가요?", answer: "기본 기능은 비회원도 일부 이용할 수 있지만, 기록 저장이나 커뮤니티 기능은 로그인 후 사용할 수 있습니다." },
      { category: "계정", question: "계정은 어떻게 관리하나요?", answer: "마이페이지에서 프로필 수정, 비밀번호 변경, 계정 관련 작업을 진행할 수 있습니다." }
    ];

    const allLabel = "전체";
    const categories = [allLabel].concat(Array.from(new Set(faqs.map(function (faq) { return faq.category; }))));
    const filterWrap = document.getElementById("categoryFilters");
    const faqList = document.getElementById("faqList");
    const emptyState = document.getElementById("emptyState");
    const searchInput = document.getElementById("searchQuery");

    let selectedCategory = allLabel;
    let openIndex = null;

    function goPage(path) {
      window.location.href = path;
    }

    function renderFilters() {
      filterWrap.innerHTML = categories.map(function (category) {
        const active = selectedCategory === category;
        const klass = active
          ? "px-6 py-3 rounded-full transition-all font-medium bg-gradient-to-r from-sky-600 to-cyan-600 text-white shadow-lg shadow-sky-500/50"
          : "px-6 py-3 rounded-full transition-all font-medium bg-white/5 text-slate-400 hover:bg-white/10 border border-white/20";

        return '<button type="button" class="faq-filter ' + klass + '" data-category="' + category + '">' + category + "</button>";
      }).join("");

      Array.from(document.querySelectorAll(".faq-filter")).forEach(function (button) {
        button.addEventListener("click", function () {
          selectedCategory = button.dataset.category || allLabel;
          openIndex = null;
          render();
        });
      });
    }

    function filteredFaqs() {
      const query = String(searchInput.value || "").toLowerCase().trim();
      return faqs.filter(function (faq) {
        const matchesCategory = selectedCategory === allLabel || faq.category === selectedCategory;
        const matchesSearch = faq.question.toLowerCase().includes(query) || faq.answer.toLowerCase().includes(query);
        return matchesCategory && matchesSearch;
      });
    }

    function renderFaqs() {
      const list = filteredFaqs();

      faqList.innerHTML = list.map(function (faq, index) {
        const opened = openIndex === index;
        return ''
          + '<div class="faq-item relative group">'
          + '  <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-2xl blur-xl opacity-0 group-hover:opacity-20 transition-opacity"></div>'
          + '  <div class="relative bg-slate-900/50 backdrop-blur-xl rounded-2xl border border-white/10 overflow-hidden">'
          + '    <button type="button" class="faq-toggle w-full px-6 py-5 flex items-center justify-between text-left hover:bg-white/5 transition-all" data-index="' + index + '">'
          + '      <div class="flex items-start gap-4 flex-1">'
          + '        <div class="w-8 h-8 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-lg flex items-center justify-center flex-shrink-0 mt-1"><span class="text-sm font-bold">Q</span></div>'
          + '        <div class="flex-1">'
          + '          <span class="inline-block px-3 py-1 bg-sky-500/20 text-sky-400 rounded-full text-xs font-medium mb-2">' + faq.category + "</span>"
          + '          <p class="text-lg font-semibold">' + faq.question + "</p>"
          + "        </div>"
          + "      </div>"
          + '      <div class="flex-shrink-0 ml-4 transition-transform duration-300' + (opened ? " rotate-180" : "") + '">'
          + '        <i data-lucide="chevron-down" class="w-5 h-5 text-slate-400"></i>'
          + "      </div>"
          + "    </button>"
          + (opened
              ? '    <div class="overflow-hidden"><div class="px-6 pb-5 pl-[4.5rem]"><div class="pt-4 border-t border-white/10"><p class="text-slate-300 leading-relaxed">' + faq.answer + "</p></div></div></div>"
              : "")
          + "  </div>"
          + "</div>";
      }).join("");

      emptyState.classList.toggle("hidden", list.length > 0);

      Array.from(document.querySelectorAll(".faq-toggle")).forEach(function (button) {
        button.addEventListener("click", function () {
          const index = Number(button.dataset.index);
          openIndex = openIndex === index ? null : index;
          renderFaqs();
          lucide.createIcons();
        });
      });
    }

    function render() {
      renderFilters();
      renderFaqs();
      lucide.createIcons();
    }

    searchInput.addEventListener("input", function () {
      openIndex = null;
      renderFaqs();
      lucide.createIcons();
    });

    render();
  </script>
</body>
</html>

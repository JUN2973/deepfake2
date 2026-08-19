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
<body class="min-h-screen bg-slate-950 text-white pt-32 pb-12 px-4 relative">
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
      <p class="text-xl text-slate-400 max-w-2xl mx-auto">DeepScan 이용 중 자주 받는 질문과 답변을 정리했습니다.</p>
    </div>

    <div class="mb-8">
      <div class="relative">
        <i data-lucide="search" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
        <input
          id="searchQuery"
          type="text"
          placeholder="질문을 검색해보세요."
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
          <h2 class="text-3xl font-bold mb-3">추가 문의가 필요하신가요?</h2>
          <p class="text-slate-400">원하시는 답변이 없다면 아래 채널로 문의해 주세요.</p>
        </div>
        <div class="grid md:grid-cols-3 gap-6">
          <button
            type="button"
            onclick="goPage('<%= contextPath %>/community')"
            class="p-6 bg-white/5 hover:bg-white/10 rounded-2xl transition-all border border-white/10 hover:border-white/20"
          >
            <i data-lucide="message-circle" class="w-8 h-8 text-sky-400 mx-auto mb-3"></i>
            <h3 class="font-semibold mb-2">커뮤니티</h3>
            <p class="text-sm text-slate-400">다른 사용자들과 정보 공유</p>
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
      { category: "일반", question: "DeepScan은 무엇인가요?", answer: "DeepScan은 AI 기반 이미지 검증 플랫폼으로 딥페이크와 조작 이미지를 탐지합니다. 업로드한 이미지의 진위 여부를 빠르게 분석해 결과를 제공합니다." },
      { category: "일반", question: "서비스는 무료인가요?", answer: "기본적인 이미지 검증 기능은 무료로 이용할 수 있습니다. 추가 기능이나 API 연동은 별도 정책이 적용될 수 있습니다." },
      { category: "사용법", question: "어떤 이미지 형식을 지원하나요?", answer: "JPG, PNG, WebP 등 일반적인 이미지 형식을 지원합니다. 파일 크기는 최대 10MB까지 업로드할 수 있습니다." },
      { category: "사용법", question: "분석에는 얼마나 시간이 걸리나요?", answer: "대부분의 이미지는 1~2초 내에 분석이 완료됩니다. 파일 크기와 서버 상태에 따라 조금 더 걸릴 수 있습니다." },
      { category: "사용법", question: "업로드한 이미지는 어떻게 처리되나요?", answer: "업로드한 이미지는 분석 후 안전하게 처리되며, 서비스 정책에 따라 필요한 범위에서만 보관됩니다." },
      { category: "정확도", question: "탐지 정확도는 어느 정도인가요?", answer: "DeepScan은 지속적인 모델 개선을 통해 높은 정확도를 유지하고 있습니다. 다만 모든 결과는 참고용으로 보고 추가 확인을 권장합니다." },
      { category: "정확도", question: "모든 딥페이크를 탐지할 수 있나요?", answer: "최신 생성 기법으로 만들어진 고도화된 이미지의 경우 탐지가 어려울 수 있습니다. 따라서 판정 결과와 함께 제공되는 근거를 같이 확인하는 것이 좋습니다." },
      { category: "보안", question: "내 데이터는 안전한가요?", answer: "모든 데이터는 안전한 방식으로 전송 및 처리되며, 개인정보 보호를 최우선으로 고려합니다." },
      { category: "보안", question: "이미지 데이터를 외부와 공유하나요?", answer: "사용자 동의 없이 업로드한 이미지나 분석 결과를 외부에 공유하지 않습니다." },
      { category: "기술", question: "어떤 AI 기술을 사용하나요?", answer: "이미지 패턴 분석, 이상 영역 탐지, 메타데이터 확인 등 복합적인 검증 방식을 활용합니다." },
      { category: "기술", question: "메타데이터도 확인하나요?", answer: "가능한 경우 EXIF 등 이미지 메타데이터를 함께 분석해 생성 및 편집 흔적을 점검합니다." },
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

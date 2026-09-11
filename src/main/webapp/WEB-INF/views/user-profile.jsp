<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  체크리스트 기준 주석: 구현(마이페이지): 회원정보 수정, 탈퇴, 로그아웃 진입 화면을 구성한다.
--%>
<%
  String contextPath = request.getContextPath();
  Object userIdObj = session.getAttribute("USER_ID");
  String userName = (String) session.getAttribute("USER_NAME");
  String userEmail = (String) session.getAttribute("USER_EMAIL");
  if (userIdObj == null) {
    response.sendRedirect(contextPath + "/login");
    return;
  }

  String escapedUserName = userName == null ? "" : userName.replace("\\", "\\\\").replace("\"", "\\\"");
  String escapedUserEmail = userEmail == null ? "" : userEmail.replace("\\", "\\\\").replace("\"", "\\\"");
  request.setAttribute("activePage", "mypage");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>&#47560;&#51060;&#54168;&#51060;&#51648; - DeepScan</title>
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

    .glass-card {
      background: rgba(15, 23, 42, 0.5);
      backdrop-filter: blur(20px);
    }
  </style>
</head>
<body class="ds-page min-h-screen bg-slate-950 text-white relative">
  <div class="absolute inset-0 overflow-hidden">
    <div class="absolute top-1/4 left-1/4 w-96 h-96 bg-sky-600/20 rounded-full blur-3xl"></div>
    <div class="absolute bottom-1/4 right-1/4 w-96 h-96 bg-cyan-600/20 rounded-full blur-3xl"></div>
  </div>

  <%@ include file="common/dashboard-nav.jspf" %>

  <div class="pt-28 pb-12 px-4 relative z-10">
    <div class="max-w-5xl mx-auto">
      <div class="text-center mb-8">
        <div class="relative inline-block mb-4">
          <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-full blur-xl opacity-75"></div>
          <div class="relative inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-full shadow-lg shadow-sky-500/50">
            <i data-lucide="user" class="w-10 h-10 text-white"></i>
          </div>
        </div>
        <h1 class="text-3xl font-bold text-white mb-2">&#47560;&#51060;&#54168;&#51060;&#51648;</h1>
        <p class="text-slate-400"><%= userName %>&#45784;&#51032; &#54532;&#47196;&#54596;</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 glass-card rounded-2xl p-8 border border-white/10">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl font-semibold text-white">&#54532;&#47196;&#54596; &#51221;&#48372;</h2>
            <div id="viewActions">
              <button type="button" onclick="startEdit()" class="flex items-center gap-2 px-4 py-2 text-sky-400 hover:bg-white/10 rounded-xl transition-colors">
                <i data-lucide="edit-2" class="w-4 h-4"></i>
                <span>&#49688;&#51221;</span>
              </button>
            </div>
            <div id="editActions" class="hidden flex gap-2">
              <button type="button" onclick="cancelEdit()" class="flex items-center gap-2 px-4 py-2 text-slate-300 hover:bg-white/10 rounded-xl transition-colors">
                <i data-lucide="x" class="w-4 h-4"></i>
                <span>&#52712;&#49548;</span>
              </button>
              <button type="button" onclick="handleSaveProfile()" class="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-sky-600 to-cyan-600 text-white rounded-xl hover:from-sky-500 hover:to-cyan-500 transition-all shadow-lg shadow-sky-500/50">
                <i data-lucide="save" class="w-4 h-4"></i>
                <span>&#51200;&#51109;</span>
              </button>
            </div>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm text-slate-300 mb-2 font-medium">&#51060;&#47492;</label>
              <div class="relative">
                <i data-lucide="user" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="name" type="text" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white disabled:opacity-50 disabled:text-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl" disabled>
              </div>
            </div>

            <div>
              <label class="block text-sm text-slate-300 mb-2 font-medium">&#51060;&#47700;&#51068;</label>
              <div class="relative">
                <i data-lucide="mail" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input type="email" value="<%= userEmail != null ? userEmail : "" %>" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-slate-400 opacity-50 backdrop-blur-xl" disabled>
              </div>
              <p class="text-xs text-slate-500 mt-1">&#51060;&#47700;&#51068;&#51008; &#48320;&#44221;&#54624; &#49688; &#50630;&#49845;&#45768;&#45796;.</p>
            </div>

            <div>
              <label class="block text-sm text-slate-300 mb-2 font-medium">&#51204;&#54868;&#48264;&#54840;</label>
              <div class="relative">
                <i data-lucide="phone" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="phone" type="tel" maxlength="13" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white disabled:opacity-50 disabled:text-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl" disabled>
              </div>
            </div>

            <div>
              <label class="block text-sm text-slate-300 mb-2 font-medium">
                &#51452;&#49548;
                <span id="zonecodeText" class="ml-2 text-sky-400 text-xs hidden"></span>
              </label>
              <div class="relative">
                <i data-lucide="map-pin" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="address" type="text" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-slate-400 opacity-50 backdrop-blur-xl" disabled>
              </div>
            </div>

            <div id="detailAddressWrap" class="hidden">
              <label class="block text-sm text-slate-300 mb-2 font-medium">&#49345;&#49464; &#51452;&#49548;</label>
              <div class="relative">
                <i data-lucide="map-pin" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="detailAddress" type="text" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white disabled:opacity-50 disabled:text-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl" disabled>
              </div>
            </div>

            <div id="profileErrorBox" class="hidden p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm"></div>
          </div>
        </div>

        <div class="space-y-4">
          <div class="glass-card rounded-2xl p-6 border border-white/10">
            <h3 class="text-lg font-semibold text-white mb-4">&#44228;&#51221; &#44288;&#47532;</h3>
            <div class="space-y-2">
              <button id="passwordChangeButton" type="button" onclick="openPasswordModal()" class="w-full flex items-center justify-between p-3 text-slate-300 hover:bg-white/10 rounded-xl transition-colors">
                <div class="flex items-center gap-3">
                  <i data-lucide="lock" class="w-5 h-5 text-sky-400"></i>
                  <span>&#48708;&#48128;&#48264;&#54840; &#48320;&#44221;</span>
                </div>
                <i data-lucide="chevron-right" class="w-4 h-4 text-slate-500"></i>
              </button>
              <form action="<%= contextPath %>/api/v1/auth/logout" method="post" class="m-0 logout-form">
                <button type="submit" class="w-full flex items-center justify-between p-3 text-slate-300 hover:bg-white/10 rounded-xl transition-colors">
                  <div class="flex items-center gap-3">
                    <i data-lucide="log-out" class="w-5 h-5 text-sky-400"></i>
                    <span>&#47196;&#44536;&#50500;&#50883;</span>
                  </div>
                  <i data-lucide="chevron-right" class="w-4 h-4 text-slate-500"></i>
                </button>
              </form>
              <button type="button" onclick="openDeleteModal()" class="w-full flex items-center justify-between p-3 text-red-400 hover:bg-red-500/10 rounded-xl transition-colors">
                <div class="flex items-center gap-3">
                  <i data-lucide="trash-2" class="w-5 h-5"></i>
                  <span>&#54924;&#50896;&#53448;&#53748;</span>
                </div>
                <i data-lucide="chevron-right" class="w-4 h-4 text-red-400/50"></i>
              </button>
            </div>
          </div>

          <div class="glass-card rounded-2xl p-6 border border-white/10">
            <h3 class="text-lg font-semibold text-white mb-4">&#54876;&#46041; &#53685;&#44228;</h3>
            <div class="space-y-3">
              <div class="flex items-center justify-between p-3 bg-sky-500/10 border border-sky-500/20 rounded-xl">
                <div class="flex items-center gap-3">
                  <i data-lucide="file-text" class="w-5 h-5 text-sky-400"></i>
                  <span class="text-slate-300">&#51089;&#49457;&#54620; &#44544;</span>
                </div>
                <span id="postCount" class="text-sky-400 font-semibold">0&#44060;</span>
              </div>
              <div class="flex items-center justify-between p-3 bg-cyan-500/10 border border-cyan-500/20 rounded-xl">
                <div class="flex items-center gap-3">
                  <i data-lucide="message-square" class="w-5 h-5 text-cyan-400"></i>
                  <span class="text-slate-300">&#51089;&#49457;&#54620; &#45843;&#44544;</span>
                </div>
                <span id="commentCount" class="text-cyan-400 font-semibold">0&#44060;</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="mt-6 glass-card rounded-2xl p-8 border border-white/10">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold text-white">&#45236;&#44032; &#51089;&#49457;&#54620; &#44172;&#49884;&#44544;</h2>
          <button type="button" onclick="goPage('<%= contextPath %>/community')" class="text-sky-400 hover:text-sky-300 text-sm transition-colors">
            &#52964;&#48036;&#45768;&#54000; &#51060;&#46041;
          </button>
        </div>
        <div id="myPostsWrap" class="space-y-3">
          <div class="text-center py-8 text-slate-500">&#51089;&#49457;&#54620; &#44172;&#49884;&#44544;&#51060; &#50630;&#49845;&#45768;&#45796;.</div>
        </div>
      </div>

      <div class="mt-6 glass-card rounded-2xl p-8 border border-white/10">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-semibold text-white">&#48513;&#47560;&#53356;&#54620; &#45684;&#49828;</h2>
          <button type="button" onclick="goPage('<%= contextPath %>/news')" class="text-sky-400 hover:text-sky-300 text-sm transition-colors">
            &#45684;&#49828; &#51060;&#46041;
          </button>
        </div>
        <div id="bookmarkedNewsWrap" class="space-y-3">
          <div class="text-center py-8 text-slate-500">&#48513;&#47560;&#53356;&#54620; &#45684;&#49828;&#44032; &#50630;&#49845;&#45768;&#45796;.</div>
        </div>
      </div>

      <div class="mt-6 glass-card rounded-2xl p-8 border border-white/10">
        <h2 class="text-xl font-semibold text-white mb-6">&#45236;&#44032; &#51089;&#49457;&#54620; &#45843;&#44544;</h2>
        <div id="myCommentsWrap" class="space-y-3">
          <div class="text-center py-8 text-slate-500">&#51089;&#49457;&#54620; &#45843;&#44544;&#51060; &#50630;&#49845;&#45768;&#45796;.</div>
        </div>
      </div>
    </div>
  </div>

  <div id="passwordModal" class="hidden fixed inset-0 z-50 flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" onclick="closePasswordModal()"></div>
    <div class="relative bg-slate-900/90 backdrop-blur-xl border border-white/10 rounded-2xl p-6 max-w-md w-full shadow-2xl">
      <h3 class="text-xl font-semibold text-white mb-4">&#48708;&#48128;&#48264;&#54840; &#48320;&#44221;</h3>
      <div class="space-y-4">
        <div>
          <label class="block text-sm text-slate-300 mb-2 font-medium">&#54788;&#51116; &#48708;&#48128;&#48264;&#54840;</label>
          <input id="currentPassword" type="password" class="w-full px-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
        </div>
        <div>
          <label class="block text-sm text-slate-300 mb-2 font-medium">&#49352; &#48708;&#48128;&#48264;&#54840;</label>
          <input id="newPassword" type="password" class="w-full px-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
        </div>
        <div>
          <label class="block text-sm text-slate-300 mb-2 font-medium">&#49352; &#48708;&#48128;&#48264;&#54840; &#54869;&#51064;</label>
          <input id="confirmPassword" type="password" class="w-full px-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
        </div>
        <div id="passwordErrorBox" class="hidden p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm"></div>
        <div class="flex gap-3 justify-end mt-6">
          <button type="button" onclick="closePasswordModal()" class="px-4 py-2 bg-white/10 text-slate-300 rounded-xl hover:bg-white/20 transition-colors">&#52712;&#49548;</button>
          <button type="button" onclick="handlePasswordChange()" class="px-4 py-2 bg-gradient-to-r from-sky-600 to-cyan-600 text-white rounded-xl hover:from-sky-500 hover:to-cyan-500 transition-all shadow-lg shadow-sky-500/50">&#48320;&#44221;</button>
        </div>
      </div>
    </div>
  </div>

  <div id="deleteModal" class="hidden fixed inset-0 z-50 flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" onclick="closeDeleteModal()"></div>
    <div class="relative bg-slate-900/90 backdrop-blur-xl border border-white/10 rounded-2xl p-6 max-w-sm w-full shadow-2xl">
      <h3 class="text-xl font-semibold text-white mb-3">&#54924;&#50896;&#53448;&#53748;</h3>
      <p class="text-slate-400 mb-6">&#51221;&#47568;&#47196; &#53448;&#53748;&#54616;&#49884;&#44192;&#49845;&#45768;&#44620;? &#47784;&#46304; &#45936;&#51060;&#53552;&#44032; &#49325;&#51228;&#46104;&#47728; &#48373;&#44396;&#54624; &#49688; &#50630;&#49845;&#45768;&#45796;.</p>
      <div class="flex gap-3 justify-end">
        <button type="button" onclick="closeDeleteModal()" class="px-4 py-2 bg-white/10 text-slate-300 rounded-xl hover:bg-white/20 transition-colors">&#52712;&#49548;</button>
        <button type="button" onclick="handleDeleteAccount()" class="px-4 py-2 bg-red-600 text-white rounded-xl hover:bg-red-700 transition-colors shadow-lg shadow-red-500/50">&#53448;&#53748;</button>
      </div>
    </div>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    const sessionUser = {
      id: "<%= String.valueOf(userIdObj) %>",
      name: "<%= escapedUserName %>",
      email: "<%= escapedUserEmail %>"
    };

    let snapshot = null;
    let myPosts = [];
    let myComments = [];
    let socialLogin = false;
    const LEGACY_NEWS_BOOKMARKS_KEY = "newsBookmarks";
    const NEWS_BOOKMARKS_KEY = sessionUser.id ? "newsBookmarks:" + sessionUser.id : "newsBookmarks:guest";

    function goPage(path) {
      window.location.href = path;
    }

    async function performLogout() {
      try {
        await fetch(contextPath + "/api/v1/auth/logout", {
          method: "POST",
          credentials: "same-origin"
        });
      } finally {
        window.location.href = contextPath + "/";
      }
    }

    function getUsers() {
      try {
        return JSON.parse(localStorage.getItem("users") || "[]");
      } catch (error) {
        return [];
      }
    }

    function setBox(id, message) {
      const box = document.getElementById(id);
      box.textContent = message;
      box.classList.remove("hidden");
    }

    function clearBox(id) {
      const box = document.getElementById(id);
      box.textContent = "";
      box.classList.add("hidden");
    }

    function renderProfile(profile) {
      document.getElementById("name").value = profile.name || sessionUser.name || "";
      document.getElementById("phone").value = profile.phone || "";
      document.getElementById("address").value = profile.address || "";
      document.getElementById("detailAddress").value = profile.detailAddress || "";
      socialLogin = !!profile.socialLogin;
      updatePasswordChangeButton();

      document.getElementById("detailAddressWrap").classList.toggle("hidden", !profile.address);

      const zonecodeText = document.getElementById("zonecodeText");
      if (profile.zonecode) {
        zonecodeText.textContent = "[" + profile.zonecode + "]";
        zonecodeText.classList.remove("hidden");
      } else {
        zonecodeText.textContent = "";
        zonecodeText.classList.add("hidden");
      }
    }

    function updatePasswordChangeButton() {
      const button = document.getElementById("passwordChangeButton");
      if (!button) {
        return;
      }
      button.disabled = socialLogin;
      button.title = socialLogin ? "구글 로그인 계정은 비밀번호를 변경할 수 없습니다." : "";
      button.classList.toggle("opacity-50", socialLogin);
      button.classList.toggle("cursor-not-allowed", socialLogin);
      button.classList.toggle("hover:bg-white/10", !socialLogin);
    }

    async function loadProfile() {
      try {
        const response = await fetch(contextPath + "/api/v1/auth/me", {
          credentials: "same-origin"
        });
        const json = await response.json().catch(function () { return null; });
        if (json && json.success && json.data) {
          renderProfile({
            name: json.data.name || sessionUser.name || "",
            phone: json.data.phone || "",
            address: json.data.address || "",
            detailAddress: "",
            zonecode: "",
            socialLogin: !!json.data.socialLogin
          });
          return;
        }
      } catch (error) {
      }

      renderProfile({
        name: sessionUser.name || "",
        phone: "",
        address: "",
        detailAddress: "",
        zonecode: "",
        socialLogin: false
      });
    }

    function setEditMode(isEditing) {
      document.getElementById("name").disabled = !isEditing;
      document.getElementById("phone").disabled = !isEditing;
      document.getElementById("detailAddress").disabled = !isEditing;
      document.getElementById("viewActions").classList.toggle("hidden", isEditing);
      document.getElementById("editActions").classList.toggle("hidden", !isEditing);
    }

    function startEdit() {
      clearBox("profileErrorBox");
      snapshot = {
        name: document.getElementById("name").value,
        phone: document.getElementById("phone").value,
        detailAddress: document.getElementById("detailAddress").value
      };
      setEditMode(true);
    }

    function cancelEdit() {
      if (snapshot) {
        document.getElementById("name").value = snapshot.name;
        document.getElementById("phone").value = snapshot.phone;
        document.getElementById("detailAddress").value = snapshot.detailAddress;
      }
      clearBox("profileErrorBox");
      setEditMode(false);
    }

    function formatPhone(value) {
      const numbers = String(value || "").replace(/[^\d]/g, "");
      if (numbers.length > 7) {
        return numbers.slice(0, 3) + "-" + numbers.slice(3, 7) + "-" + numbers.slice(7, 11);
      }
      if (numbers.length > 3) {
        return numbers.slice(0, 3) + "-" + numbers.slice(3);
      }
      return numbers;
    }

    async function handleSaveProfile() {
      clearBox("profileErrorBox");

      const name = document.getElementById("name").value.trim();
      const phone = document.getElementById("phone").value.trim();
      const address = document.getElementById("address").value.trim();
      const detailAddress = document.getElementById("detailAddress").value.trim();

      if (phone && !/^\d{3}-\d{4}-\d{4}$/.test(phone)) {
        setBox("profileErrorBox", "전화번호 형식을 확인해 주세요. (예: 010-1234-5678)");
        return;
      }

      try {
        const response = await fetch(contextPath + "/api/v1/auth/profile", {
          method: "PUT",
          credentials: "same-origin",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: name,
            phoneNumber: phone,
            address: address,
            detailAddress: detailAddress
          })
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) {
          setBox("profileErrorBox", json && json.error && json.error.message ? json.error.message : "프로필 수정에 실패했습니다.");
          return;
        }

        localStorage.setItem("user", JSON.stringify({
          id: sessionUser.id,
          email: sessionUser.email,
          name: name,
          phone: phone
        }));

        setEditMode(false);
        alert("프로필이 수정되었습니다.");
        window.location.reload();
      } catch (error) {
        setBox("profileErrorBox", "프로필 수정 중 오류가 발생했습니다.");
      }
    }

    function openPasswordModal() {
      if (socialLogin) {
        alert("구글 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        return;
      }
      clearBox("passwordErrorBox");
      document.getElementById("currentPassword").value = "";
      document.getElementById("newPassword").value = "";
      document.getElementById("confirmPassword").value = "";
      document.getElementById("passwordModal").classList.remove("hidden");
    }

    function closePasswordModal() {
      document.getElementById("passwordModal").classList.add("hidden");
      clearBox("passwordErrorBox");
    }

    async function handlePasswordChange() {
      clearBox("passwordErrorBox");
      if (socialLogin) {
        setBox("passwordErrorBox", "구글 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        return;
      }

      const currentPassword = document.getElementById("currentPassword").value;
      const newPassword = document.getElementById("newPassword").value;
      const confirmPassword = document.getElementById("confirmPassword").value;

      if (!currentPassword) {
        setBox("passwordErrorBox", "현재 비밀번호를 입력해 주세요.");
        return;
      }
      if (newPassword.length < 6) {
        setBox("passwordErrorBox", "새 비밀번호는 최소 6자 이상이어야 합니다.");
        return;
      }
      if (newPassword !== confirmPassword) {
        setBox("passwordErrorBox", "새 비밀번호 확인 값이 일치하지 않습니다.");
        return;
      }

      try {
        const response = await fetch(contextPath + "/api/v1/auth/password", {
          method: "PUT",
          credentials: "same-origin",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            currentPassword: currentPassword,
            newPassword: newPassword
          })
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) {
          setBox("passwordErrorBox", json && json.error && json.error.message ? json.error.message : "비밀번호 변경에 실패했습니다.");
          return;
        }

        closePasswordModal();
        alert("비밀번호가 변경되었습니다.");
      } catch (error) {
        setBox("passwordErrorBox", "비밀번호 변경 중 오류가 발생했습니다.");
      }
    }

    function openDeleteModal() {
      document.getElementById("deleteModal").classList.remove("hidden");
    }

    function closeDeleteModal() {
      document.getElementById("deleteModal").classList.add("hidden");
    }

    async function handleDeleteAccount() {
      try {
        const response = await fetch(contextPath + "/api/v1/auth/account", {
          method: "DELETE",
          credentials: "same-origin"
        });
        const json = await response.json().catch(function () { return null; });

        if (!json || !json.success) {
          alert(json && json.error && json.error.message ? json.error.message : "회원탈퇴 처리에 실패했습니다.");
          return;
        }

        const users = getUsers().filter(function (item) {
          return String(item.id) !== String(sessionUser.id);
        });
        localStorage.setItem("users", JSON.stringify(users));
        localStorage.removeItem("user");
        localStorage.removeItem(NEWS_BOOKMARKS_KEY);
        localStorage.removeItem(LEGACY_NEWS_BOOKMARKS_KEY);
        window.location.href = contextPath + "/";
      } catch (error) {
        alert("회원탈퇴 처리 중 오류가 발생했습니다.");
      }
    }

    function formatDate(value) {
      if (!value) {
        return "-";
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return value;
      }
      return date.toLocaleDateString("ko-KR");
    }

    function escapeHtml(value) {
      return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
    }

    function renderMyPosts() {
      const wrap = document.getElementById("myPostsWrap");
      if (!myPosts.length) {
        wrap.innerHTML = '<div class="text-center py-8 text-slate-500">작성한 게시글이 없습니다.</div>';
        return;
      }

      wrap.innerHTML = myPosts.slice(0, 5).map(function (post) {
        return ''
          + '<div onclick="goPage(\'' + contextPath + '/community/' + post.id + '\')" class="p-4 bg-white/5 border border-white/10 rounded-xl hover:border-sky-500/50 hover:bg-white/10 transition-all cursor-pointer">'
          +   '<h3 class="text-white mb-2">' + escapeHtml(post.title) + '</h3>'
          +   '<div class="flex items-center gap-4 text-sm text-slate-400 flex-wrap">'
          +     '<span>' + formatDate(post.createdAt) + '</span>'
          +     '<span>조회 ' + Number(post.views || 0) + '</span>'
          +     '<span>댓글 ' + Number(post.commentCount || 0) + '</span>'
          +   '</div>'
          + '</div>';
      }).join("");
    }

    function renderMyComments() {
      const wrap = document.getElementById("myCommentsWrap");
      if (!myComments.length) {
        wrap.innerHTML = '<div class="text-center py-8 text-slate-500">작성한 댓글이 없습니다.</div>';
        return;
      }

      wrap.innerHTML = myComments.slice(0, 5).map(function (item) {
        return ''
          + '<div onclick="goPage(\'' + contextPath + '/community/' + item.postId + '\')" class="p-4 bg-white/5 border border-white/10 rounded-xl hover:border-sky-500/50 hover:bg-white/10 transition-all cursor-pointer">'
          +   '<p class="text-slate-300 mb-2">' + escapeHtml(item.content) + '</p>'
          +   '<div class="flex items-center gap-4 text-sm text-slate-400 flex-wrap">'
          +     '<span>게시글: ' + escapeHtml(item.postTitle) + '</span>'
          +     '<span>' + formatDate(item.createdAt) + '</span>'
          +   '</div>'
          + '</div>';
      }).join("");
    }

    function getNewsBookmarks() {
      try {
        const parsed = JSON.parse(localStorage.getItem(NEWS_BOOKMARKS_KEY) || "[]");
        if (!Array.isArray(parsed)) {
          return [];
        }
        return parsed.filter(function (item) {
          return item && item.id;
        }).sort(function (a, b) {
          return new Date(b.savedAt || 0).getTime() - new Date(a.savedAt || 0).getTime();
        });
      } catch (error) {
        return [];
      }
    }

    function saveNewsBookmarks(bookmarks) {
      localStorage.setItem(NEWS_BOOKMARKS_KEY, JSON.stringify(bookmarks));
    }

    function syncBookmarkedNewsButtons() {
      document.querySelectorAll("[data-bookmarked-news-button]").forEach(function (button) {
        const icon = button.querySelector("i");
        if (!icon) {
          return;
        }

        button.className = "flex h-11 w-11 items-center justify-center rounded-full border border-sky-400/30 bg-sky-500/18 text-sky-300 transition-all hover:bg-sky-500/28";
        button.setAttribute("aria-label", "북마크 해제");
        icon.setAttribute("data-lucide", "bookmark-check");
        icon.className = "h-5 w-5";
      });

      lucide.createIcons();
    }

    function removeNewsBookmark(newsId, event) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }

      const bookmarks = getNewsBookmarks().filter(function (item) {
        return String(item.id) !== String(newsId);
      });

      saveNewsBookmarks(bookmarks);
      renderBookmarkedNews();
    }

    function resolveBookmarkUrl(item) {
      if (!item || !item.url) {
        return contextPath + "/news";
      }
      if (item.url.startsWith("http://") || item.url.startsWith("https://")) {
        return item.url;
      }
      if (item.url.startsWith("/")) {
        return item.url;
      }
      return contextPath + "/" + item.url.replace(/^\/+/, "");
    }

    function renderBookmarkedNews() {
      const wrap = document.getElementById("bookmarkedNewsWrap");
      const bookmarks = getNewsBookmarks();
      if (!bookmarks.length) {
        wrap.innerHTML = '<div class="text-center py-8 text-slate-500">북마크한 뉴스가 없습니다.</div>';
        return;
      }

      wrap.innerHTML = bookmarks.slice(0, 6).map(function (item) {
        const targetUrl = resolveBookmarkUrl(item);
        return ''
          + '<div onclick="window.location.href=\'' + escapeHtml(targetUrl) + '\'" class="p-4 bg-white/5 border border-white/10 rounded-xl hover:border-sky-500/50 hover:bg-white/10 transition-all cursor-pointer">'
          +   '<div class="flex items-start justify-between gap-3 mb-2">'
          +     '<div class="min-w-0 flex-1">'
          +       '<h3 class="text-white font-medium">' + escapeHtml(item.title || "뉴스") + '</h3>'
          +       '<span class="mt-1 inline-block text-xs text-sky-400 whitespace-nowrap">' + escapeHtml(formatDate(item.savedAt)) + '</span>'
          +     '</div>'
          +     '<button type="button" data-bookmarked-news-button onclick="removeNewsBookmark(\'' + escapeHtml(item.id) + '\', event)" class="flex h-11 w-11 items-center justify-center rounded-full border border-sky-400/30 bg-sky-500/18 text-sky-300 transition-all hover:bg-sky-500/28" aria-label="북마크 해제">'
          +       '<i data-lucide="bookmark-check" class="h-5 w-5"></i>'
          +     '</button>'
          +   '</div>'
          +   '<p class="text-sm text-slate-400 mb-3">' + escapeHtml(item.summary || "") + '</p>'
          +   '<div class="flex items-center gap-4 text-sm text-slate-500 flex-wrap">'
          +     '<span>' + escapeHtml(item.pubDate || "-") + '</span>'
          +     '<span>북마크 날짜 ' + escapeHtml(formatDate(item.savedAt)) + '</span>'
          +   '</div>'
          + '</div>';
      }).join("");

      syncBookmarkedNewsButtons();
    }

    async function loadCommunityActivity() {
      try {
        const response = await fetch(contextPath + "/api/v1/community/posts");
        const json = await response.json().catch(function () { return null; });
        if (json && json.success && Array.isArray(json.data)) {
          myPosts = json.data.filter(function (post) {
            return String(post.userId) === String(sessionUser.id);
          });
        }
      } catch (error) {
        myPosts = [];
      }

      try {
        const response = await fetch(contextPath + "/api/v1/community/posts/my-comments", {
          credentials: "same-origin"
        });
        const json = await response.json().catch(function () { return null; });
        myComments = json && json.success && Array.isArray(json.data) ? json.data : [];
      } catch (error) {
        myComments = [];
      }

      document.getElementById("postCount").textContent = myPosts.length + "\uAC1C";
      document.getElementById("commentCount").textContent = myComments.length + "\uAC1C";
      renderMyPosts();
      renderMyComments();
    }

    document.getElementById("phone").addEventListener("input", function (event) {
      event.target.value = formatPhone(event.target.value);
    });

    document.querySelectorAll(".logout-form").forEach(function (form) {
      form.addEventListener("submit", function (event) {
        event.preventDefault();
        performLogout();
      });
    });

    loadProfile();
    loadCommunityActivity();
    renderBookmarkedNews();
    setEditMode(false);
    lucide.createIcons();
  </script>
</body>
</html>

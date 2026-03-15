@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param SecurityHelper secHelper
@param StaticResourceHelper res
<header class="top-0 left-0 justify-center items-center h-16 w-full bg-slate-200 flex sticky sm:px-4 md:relative sm:justify-between">

    <div class="flex justify-center items-center">
        <button onclick="toggleSidebar()" class="md:hidden p-2 ml-2 rounded-lg hover:bg-slate-300 transition duration-200">
            <span class="material-symbols-outlined text-slate-700">menu</span>
        </button>
        <img class="w-32" src="${res.url("/images/siga-ubs.svg")}" alt="logo-principal">
    </div>
    <div class="hidden sm:flex items-center space-x-4">
        <div class="font-bold text-slate-700 hover:text-blue-500 transition duration-500">
            ${secHelper.username()}
            <p class="text-sm font-light text-slate-900">Manager</p>
        </div>
        <div class="w-10 h-10 rounded-full bg-slate-100 ring-2 ring-slate-200 hover:ring-2 hover:ring-blue-500 transition duration-500">
            <%-- Foto-User --%>
        </div>
    </div>
    <div id="sidebar-backdrop" onclick="toggleSidebar()" class="hidden fixed inset-0 bg-black/50 z-[9998] md:hidden"></div>
    <script>
        function toggleSidebar() {
            var sidebar = document.getElementById('sidebar');
            var backdrop = document.getElementById('sidebar-backdrop');
            sidebar.classList.toggle('hidden');
            sidebar.classList.toggle('flex');
            backdrop.classList.toggle('hidden');
        }
    </script>
</header>

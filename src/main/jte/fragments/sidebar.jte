@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean adminArea = false
<aside id="sidebar" data-sidebar
       class="hidden w-64 flex-col absolute z-[9999] left-0 top-0 justify-between h-full pb-2.5 px-2 bg-slate-900 md:static md:flex overflow-auto transition-all duration-300 ease-in-out">

    <div class="flex flex-col h-screen justify-between flex-wrap overflow-hidden overscroll-contain">
        <div class="flex flex-col w-full h-24 justify-center items-center">

            <div data-sidebar-logo class="flex w-full h-20 gap-x-2 items-center justify-center bg-white rounded-md">
                <img class="max-w-36 transition-transform duration-300" src="${res.url("/images/logo-prefeitura-h.png")}" alt="logo-principal">
            </div>
        </div>

        <%-- Buttons-Section --%>
        <div class="flex flex-col flex-1 space-y-2.5 overflow-auto">
            @if(adminArea != null && adminArea)
            <a href="/admin/tenant-management" class="text-sm bg-slate-950 inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle">domain</span>
                <span data-sidebar-label class="text-nowrap">Tenants</span>
            </a>
            <a href="/admin/admin-user-management" class="text-sm bg-slate-950 inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle">admin_panel_settings</span>
                <span data-sidebar-label class="text-nowrap">Admins</span>
            </a>
            <a href="/admin/session-management" class="text-sm bg-slate-950 inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle">devices</span>
                <span data-sidebar-label class="text-nowrap">Sessões Ativas</span>
            </a>
            @else
            <%-- Dashboard Item --%>
            <a href="/" class="text-sm bg-slate-950 inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle">dashboard</span>
                <span data-sidebar-label class="text-nowrap">Dashboard</span>
            </a>

            <%-- Queue Item --%>
            <a href="/queue-management/v2" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-amber-500">patient_list</span>
                <span data-sidebar-label class="text-nowrap">Filas de Espera</span>
            </a>

            <%-- Contemplation Item — somente ADMIN/SMS --%>
            @if(secHelper.hasAnyRole("ADMIN", "SMS"))
            <a href="/contemplation-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-amber-500">groups</span>
                <span data-sidebar-label class="text-nowrap">Contemplados</span>
            </a>
            @endif

            <%-- MedicalSlot Item — somente ADMIN/SMS --%>
            @if(secHelper.hasAnyRole("ADMIN", "SMS"))
            <a href="/medicalSlot-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-amber-500">note_stack_add</span>
                <span data-sidebar-label class="text-nowrap">Vagas Médicas</span>
            </a>
            @endif

            <%-- Appointment Item --%>
            <a href="/appointment-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-red-600">heart_plus</span>
                <span data-sidebar-label class="text-nowrap">Marcação</span>
            </a>

            <%-- New Patient Item --%>
            <a href="/patient-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-blue-600">personal_injury</span>
                <span data-sidebar-label class="text-nowrap">Novo Paciente</span>
            </a>

            <%-- Search Patient Item --%>
            <a href="/patient-list" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-blue-600">conditions</span>
                <span data-sidebar-label class="text-nowrap">Buscar Pacientes</span>
            </a>

            <%-- UBS Item — somente ADMIN/SMS --%>
            @if(secHelper.hasAnyRole("ADMIN", "SMS"))
            <a href="/basicHealthUnit-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-blue-600">apartment</span>
                <span data-sidebar-label>UBS</span>
            </a>
            @endif

            <%-- Specialty Item — somente ADMIN/SMS --%>
            @if(secHelper.hasAnyRole("ADMIN", "SMS"))
            <a href="/specialty-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle hover:text-green-600">health_and_safety</span>
                <span data-sidebar-label class="text-nowrap">Especialidades</span>
            </a>
            @endif

            <%-- User Item — somente ADMIN/SMS --%>
            @if(secHelper.hasAnyRole("ADMIN", "SMS"))
            <a href="/systemUser-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle">account_circle</span>
                <span data-sidebar-label class="text-nowrap">Usuários</span>
            </a>
            @endif

            <a href="/session-management" class="text-sm inline-flex flex-nowrap items-center p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10">
                <span class="material-symbols-outlined inline-block align-middle">devices</span>
                <span data-sidebar-label class="text-nowrap">Minhas sessões</span>
            </a>

            @endif
        </div>
        <%-- Buttons-Section End --%>
    </div>
    <div class="flex flex-col flex-1 mt-2.5 bg-white bg-opacity-5 rounded-lg">
        <form action="${adminArea != null && adminArea ? "/admin/logout" : "/logout"}" method="post" class="m-0">
            <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
            <button type="submit" class="w-full text-sm inline-flex items-center flex-nowrap p-2.5 gap-x-2 tracking-wide rounded-lg text-white hover:bg-white hover:text-slate-900 overflow-hidden flex-shrink-0 min-h-10 text-left">
                <span class="material-symbols-outlined inline-block align-middle hover:text-red-500">logout</span>
                <span data-sidebar-label class="text-nowrap">Sair</span>
            </button>
        </form>
    </div>
</aside>

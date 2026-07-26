@import br.com.tecsus.sigaubs.dtos.TenantSearchDTO
@import br.com.tecsus.sigaubs.entities.SystemMaintenance
@import br.com.tecsus.sigaubs.entities.Tenant
@import br.com.tecsus.sigaubs.enums.TenantStatus
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@param Tenant tenant
@param TenantSearchDTO searchTenant = null
@param Page<Tenant> tenantsPage
@param TenantStatus[] tenantStatuses
@param SystemMaintenance systemMaintenance
@param String selectedSort = "creationDate"
@param String selectedDirection = "DESC"
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Gestão de Tenants", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res, csrf = csrf, adminArea = true)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res, adminArea = true)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panelGlobalMaintenance" class="peer hidden" checked>
                <label for="panelGlobalMaintenance" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">construction</span>Manutenção Global
                    </div>
                </label>
                <div class="flex justify-center peer-checked:max-h-screen max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          action="/admin/tenant-management/global-maintenance" method="post">
                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="enabled">Status:</label>
                            <label class="inline-flex items-center gap-2 mt-3 text-sm text-slate-700">
                                <input id="enabled" name="enabled" value="true" type="checkbox"
                                       checked="${systemMaintenance != null && systemMaintenance.isEnabled()}"
                                       class="h-4 w-4 rounded border-slate-300 text-blue-500 focus:ring-blue-500">
                                Manutenção ativa
                            </label>
                        </div>

                        <div class="flex flex-col w-full sm:col-span-2">
                            <label class="text-sm" for="message">Mensagem:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="message" name="message"
                                   value="${systemMaintenance != null && systemMaintenance.getMessage() != null ? systemMaintenance.getMessage() : ""}"
                                   placeholder="Mensagem exibida aos tenants">
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="endDate">Previsão de Retorno:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="datetime-local" id="endDate" name="endDate"
                                   value="${systemMaintenance != null && systemMaintenance.getEndDate() != null ? systemMaintenance.getEndDate().toString().substring(0, 16) : ""}">
                        </div>

                        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

                        <div class="col-span-1 sm:col-span-2 lg:col-span-4 text-center mt-2">
                            <button type="submit" class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Salvar</button>
                        </div>
                    </form>
                </div>
            </div>

            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panelTenantForm" class="peer hidden" checked>
                <label for="panelTenantForm" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">domain_add</span>${tenant.getId() == null ? "Cadastrar Tenant" : "Atualizar Tenant"}
                    </div>
                </label>
                <div class="flex justify-center peer-checked:max-h-screen max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          action="${tenant.getId() == null ? "/admin/tenant-management/create" : "/admin/tenant-management/update"}"
                          method="post">

                        @if(tenant.getId() == null)
                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="slug">Slug:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="slug" name="slug" value="${tenant.getSlug() != null ? tenant.getSlug() : ""}"
                                   placeholder="ex: afogados" required>
                        </div>
                        @else
                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="slugView">Slug:</label>
                            <span id="slugView" class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg mt-1">${tenant.getSlug()}</span>
                        </div>
                        @endif

                        <div class="flex flex-col w-full sm:col-span-2">
                            <label class="text-sm" for="name">Nome:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="name" name="name" value="${tenant.getName() != null ? tenant.getName() : ""}"
                                   placeholder="Nome do município ou tenant" required>
                        </div>

                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="domain">Domínio:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="domain" name="domain" value="${tenant.getDomain() != null ? tenant.getDomain() : ""}"
                                   placeholder="Opcional">
                        </div>

                        @if(tenant.getId() != null)
                        <div class="flex flex-col w-full">
                            <label class="text-sm" for="statusView">Status:</label>
                            <span id="statusView" class="px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg mt-1">${tenant.getStatus() != null ? tenant.getStatus().getDescription() : ""}</span>
                        </div>
                        @endif

                        <input type="hidden" id="id" name="id" value="${tenant.getId() != null ? tenant.getId().toString() : ""}"/>
                        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

                        <div class="col-span-1 sm:col-span-2 lg:col-span-4 text-center mt-2">
                            <button type="submit" class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">Salvar</button>
                            @if(tenant.getId() != null)
                            <a href="/admin/tenant-management" class="text-sm inline-block rounded-lg bg-slate-100 py-2.5 px-8 text-slate-700 cursor-pointer hover:bg-slate-200 transition duration-300 ml-2">Novo</a>
                            @endif
                        </div>
                    </form>
                </div>
            </div>

            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panelTenantsTable" class="peer hidden" checked>
                <label for="panelTenantsTable" class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">manage_search</span> Tenants
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <form id="tenantSearchForm" action="/admin/tenant-management" method="get"
                          class="grid grid-flow-row grid-cols-1 sm:grid-cols-4 md:grid-cols-12 gap-2 rounded-lg gap-x-4 py-4 px-4 w-full"
                          hx-get="/admin/tenant-management?pagination=true"
                          hx-target="#tenantsDatatable"
                          hx-swap="outerHTML">
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-3">
                            <label class="text-sm" for="slugSearch">Slug:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="slugSearch" name="slug" value="${searchTenant != null && searchTenant.getSlug() != null ? searchTenant.getSlug() : ""}">
                        </div>
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-4">
                            <label class="text-sm" for="nameSearch">Nome:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="nameSearch" name="name" value="${searchTenant != null && searchTenant.getName() != null ? searchTenant.getName() : ""}">
                        </div>
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-3">
                            <label class="text-sm" for="domainSearch">Domínio:</label>
                            <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                   type="text" id="domainSearch" name="domain" value="${searchTenant != null && searchTenant.getDomain() != null ? searchTenant.getDomain() : ""}">
                        </div>
                        <div class="flex flex-col w-full sm:col-span-2 md:col-span-2">
                            <label class="text-sm" for="statusSearch">Status:</label>
                            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                    id="statusSearch" name="status">
                                <option value="">Todos</option>
                                @for(var status : tenantStatuses)
                                <option value="${status.name()}" selected="${searchTenant != null && status.name().equals(searchTenant.getStatus())}">${status.getDescription()}</option>
                                @endfor
                            </select>
                        </div>
                        <div class="inline-flex col-span-1 sm:col-span-4 md:col-span-12 justify-center sm:justify-start items-end gap-x-2 mt-2">
                            <button type="submit" class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                                <span class="-ml-1 material-symbols-outlined mx-1">search</span>Buscar
                            </button>
                            <a href="/admin/tenant-management" class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-red-500 transition duration-300">
                                <span class="-ml-1 material-symbols-outlined mx-1">close</span>Limpar
                            </a>
                        </div>
                    </form>
                    <div class="pb-4 px-4 rounded-lg">
                        @template.tenantManagement.tenantFragments.tenant_datatable(tenantsPage = tenantsPage, searchTenant = searchTenant, selectedSort = selectedSort, selectedDirection = selectedDirection, csrf = csrf)
                    </div>
                </div>
            </div>

        </div>
    </section>
    @template.fragments.footer()
</main>
<form id="tenantSlugForm" action="/admin/tenant-management/slug" method="post" class="hidden">
    <input type="hidden" id="slugTenantId" name="id">
    <input type="hidden" id="newSlug" name="newSlug">
    <input type="hidden" id="slugConfirmation" name="confirmation">
    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
</form>
<form id="tenantDisableForm" action="/admin/tenant-management/disable" method="post" class="hidden">
    <input type="hidden" id="disableTenantId" name="id">
    <input type="hidden" id="disabledReason" name="disabledReason">
    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
</form>
<form id="tenantMaintenanceForm" action="/admin/tenant-management/maintenance/start" method="post" class="hidden">
    <input type="hidden" id="maintenanceTenantId" name="id">
    <input type="hidden" id="maintenanceMessage" name="maintenanceMessage">
    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
</form>
<script>
    function submitSlugChange(id, currentSlug) {
        const currentSlugSafe = escapeHtml(currentSlug);
        Swal.fire({
            title: 'Alterar slug do tenant',
            icon: 'warning',
            width: '42rem',
            buttonsStyling: false,
            showCancelButton: true,
            confirmButtonText: 'Alterar slug',
            cancelButtonText: 'Cancelar',
            customClass: tenantSwalClasses(),
            html:
                '<div class="text-left text-sm text-slate-700">' +
                    '<div class="mb-4 rounded-lg bg-slate-100 px-4 py-3">' +
                        '<span class="block text-xs font-semibold uppercase tracking-wide text-slate-500">Slug atual</span>' +
                        '<span class="mt-1 inline-flex rounded-lg bg-white px-3 py-1 font-mono text-sm text-slate-900">' + currentSlugSafe + '</span>' +
                    '</div>' +

                    '<div class="mb-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-amber-800">' +
                        '<span class="mb-1 block font-semibold">Atenção antes de alterar</span>' +
                        '<span class="block">A URL do tenant será alterada imediatamente, as sessões ativas serão encerradas e o cache será invalidado. A URL antiga deixará de funcionar.</span>' +
                    '</div>' +

                    '<label for="swalNewSlug" class="mb-1 block font-semibold text-slate-700">Novo slug</label>' +
                    '<input id="swalNewSlug" class="swal2-input !mx-0 !mt-0 !mb-3 !h-auto !w-full rounded-lg border border-stroke px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" placeholder="exemplo-tenant">' +

                    '<label for="swalSlugConfirmation" class="mb-1 block font-semibold text-slate-700">Confirme digitando o novo slug</label>' +
                    '<input id="swalSlugConfirmation" class="swal2-input !mx-0 !mt-0 !mb-0 !h-auto !w-full rounded-lg border border-stroke px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" placeholder="repita o novo slug">' +
                '</div>',
            focusConfirm: false,
            preConfirm: () => {
                const newSlug = document.getElementById('swalNewSlug').value.trim();
                const confirmation = document.getElementById('swalSlugConfirmation').value.trim();
                const slugPattern = /^[a-z0-9]([a-z0-9-]*[a-z0-9])?$/;

                if (!newSlug || !confirmation) {
                    Swal.showValidationMessage('Informe e confirme o novo slug.');
                    return false;
                }
                if (!slugPattern.test(newSlug)) {
                    Swal.showValidationMessage('Use apenas letras minúsculas, números e hífen. O slug deve iniciar e terminar com letra ou número.');
                    return false;
                }
                if (newSlug !== confirmation) {
                    Swal.showValidationMessage('A confirmação precisa ser igual ao novo slug.');
                    return false;
                }
                if (newSlug === currentSlug) {
                    Swal.showValidationMessage('O novo slug precisa ser diferente do slug atual.');
                    return false;
                }
                return { newSlug, confirmation };
            }
        }).then((result) => {
            if (!result.isConfirmed) return;
            document.getElementById('slugTenantId').value = id;
            document.getElementById('newSlug').value = result.value.newSlug;
            document.getElementById('slugConfirmation').value = result.value.confirmation;
            document.getElementById('tenantSlugForm').submit();
        });
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function tenantSwalClasses(confirmButtonClass) {
        return {
            popup: 'rounded-lg',
            title: 'text-slate-800',
            confirmButton: confirmButtonClass || 'text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300',
            cancelButton: 'text-sm inline-block rounded-lg bg-slate-100 py-2.5 px-6 text-slate-700 cursor-pointer hover:bg-slate-200 transition duration-300 ml-2'
        };
    }

    function submitTenantDisable(id) {
        Swal.fire({
            title: 'Desabilitar tenant',
            icon: 'warning',
            width: '40rem',
            buttonsStyling: false,
            showCancelButton: true,
            confirmButtonText: 'Desabilitar',
            cancelButtonText: 'Cancelar',
            customClass: tenantSwalClasses('text-sm inline-block rounded-lg bg-red-600 py-2.5 px-6 text-white cursor-pointer active:bg-red-700 hover:bg-red-500 transition duration-300'),
            html:
                '<div class="text-left text-sm text-slate-700">' +
                    '<div class="mb-4 rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-red-800">' +
                        '<span class="mb-1 block font-semibold">Acesso será bloqueado</span>' +
                        '<span class="block">O tenant será marcado como desabilitado, deixará de responder no subdomínio e as sessões ativas serão encerradas.</span>' +
                    '</div>' +

                    '<label for="swalDisabledReason" class="mb-1 block font-semibold text-slate-700">Motivo da desabilitação</label>' +
                    '<textarea id="swalDisabledReason" class="swal2-textarea !mx-0 !mt-0 !mb-0 !h-28 !w-full rounded-lg border border-stroke px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" placeholder="Opcional"></textarea>' +
                '</div>',
            focusConfirm: false,
            preConfirm: () => {
                return document.getElementById('swalDisabledReason').value.trim();
            }
        }).then((result) => {
            if (!result.isConfirmed) return;
            document.getElementById('disableTenantId').value = id;
            document.getElementById('disabledReason').value = result.value;
            document.getElementById('tenantDisableForm').submit();
        });
    }

    function submitTenantMaintenance(id) {
        Swal.fire({
            title: 'Colocar tenant em manutenção',
            icon: 'warning',
            width: '40rem',
            buttonsStyling: false,
            showCancelButton: true,
            confirmButtonText: 'Ativar manutenção',
            cancelButtonText: 'Cancelar',
            customClass: tenantSwalClasses(),
            html:
                '<div class="text-left text-sm text-slate-700">' +
                    '<div class="mb-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-amber-800">' +
                        '<span class="mb-1 block font-semibold">Acesso temporariamente travado</span>' +
                        '<span class="block">O tenant exibirá a página de manutenção, novas requisições serão bloqueadas e as sessões ativas serão encerradas.</span>' +
                    '</div>' +

                    '<label for="swalMaintenanceMessage" class="mb-1 block font-semibold text-slate-700">Mensagem de manutenção</label>' +
                    '<textarea id="swalMaintenanceMessage" class="swal2-textarea !mx-0 !mt-0 !mb-0 !h-28 !w-full rounded-lg border border-stroke px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-blue-500" placeholder="Opcional"></textarea>' +
                '</div>',
            focusConfirm: false,
            preConfirm: () => {
                return document.getElementById('swalMaintenanceMessage').value.trim();
            }
        }).then((result) => {
            if (!result.isConfirmed) return;
            document.getElementById('maintenanceTenantId').value = id;
            document.getElementById('maintenanceMessage').value = result.value;
            document.getElementById('tenantMaintenanceForm').submit();
        });
    }
</script>
</body>
</html>

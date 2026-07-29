@import br.com.tecsus.sigaubs.dtos.DashboardDTO
@import br.com.tecsus.sigaubs.dtos.UBSDashboardDTO
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param DashboardDTO dashboard = null
@param UBSDashboardDTO ubsDashboard = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "SIGA-UBS", res = res)
<body id="body" class="flex w-screen h-dvh items-center">
@template.fragments.sidebar(secHelper = secHelper, res = res, csrf = csrf)
<main class="flex flex-col h-full w-full overflow-auto">
    @template.fragments.header(secHelper = secHelper, res = res)

    <%-- Dashboard Content --%>
    <div class="flex-1 p-6 overflow-auto">

        <%-- Dashboard para ADMIN/SMS --%>
        @if(dashboard != null)
            <%-- UBS Cards Grid --%>
            <div class="mb-8">
                <h2 class="text-2xl font-bold text-slate-700 mb-4">Unidades Básicas de Saúde</h2>
                <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">

                    !{int ubsIdx = 0;}
                    @for(var ubs : dashboard.ubsSummaries())
                    !{int colorIdx = ubsIdx % 5;}
                    <div data-tooltip-container class="relative">
                        <div class="bg-white rounded-lg shadow-md p-4 cursor-pointer hover:shadow-lg transition duration-300 border-l-4 ${colorIdx == 0 ? "border-blue-500" : colorIdx == 1 ? "border-green-500" : colorIdx == 2 ? "border-purple-500" : colorIdx == 3 ? "border-amber-500" : "border-rose-500"}">
                            <h3 class="font-bold text-lg text-slate-700">${ubs.name()}</h3>
                            <p class="text-sm text-slate-500">Bairro: ${ubs.neighborhood()}</p>
                        </div>

                        <%-- Modal Tooltip --%>
                        <div data-tooltip
                             class="hidden fixed z-50 bg-white p-4 rounded-lg shadow-xl border border-gray-200 w-64">
                            <h4 class="font-bold text-slate-700 mb-2">Informações - ${ubs.name()}</h4>
                            <ul class="text-sm space-y-1">
                                <li class="flex justify-between"><span>Marcações em aberto:</span> <span class="font-medium">${ubs.totalOpenAppointments()}</span></li>
                                <li class="flex justify-between"><span>Contemplados (mês):</span> <span class="font-medium">${ubs.totalContemplated()}</span></li>
                                <li class="flex justify-between"><span>Pacientes cadastrados:</span> <span class="font-medium">${ubs.totalPatients()}</span></li>
                                <li class="flex justify-between"><span>Vagas disponíveis:</span> <span class="font-medium">${ubs.totalAvailableSlots()}</span></li>
                                <li class="flex justify-between"><span>Espera média:</span> <span class="font-medium">${ubs.averageWaitDays()} dias</span></li>
                            </ul>
                        </div>
                    </div>
                    !{ubsIdx++;}
                    @endfor

                </div>
            </div>

            <%-- Charts Section --%>
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                <div class="bg-white p-4 rounded-lg shadow-md">
                    <h3 class="text-lg font-semibold text-slate-700 mb-4">Marcações Diárias (Últimos 7 dias)</h3>
                    <div id="dailyChart" class="h-64 overflow-hidden"></div>
                </div>
                <div class="bg-white p-4 rounded-lg shadow-md">
                    <h3 class="text-lg font-semibold text-slate-700 mb-4">Marcações Mensais (Últimos 6 meses)</h3>
                    <div id="monthlyChart" class="h-64 overflow-hidden"></div>
                </div>
            </div>

            <%-- Donut Charts and Status Section --%>
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div class="bg-white p-4 rounded-lg shadow-md">
                    <h3 class="text-lg font-semibold text-slate-700 mb-4">Perfil por Prioridade</h3>
                    <div id="priorityDonut" class="overflow-hidden pb-4"></div>
                </div>
                <div class="bg-white p-4 rounded-lg shadow-md">
                    <h3 class="text-lg font-semibold text-slate-700 mb-4">Perfil por Procedimento</h3>
                    <div id="procedureDonut" class="overflow-hidden pb-4"></div>
                </div>

                <%-- Contemplation Status --%>
                <div class="bg-white p-4 rounded-lg shadow-md">
                    <h3 class="text-lg font-semibold text-slate-700 mb-4">Status da Rotina de Contemplação</h3>
                    <div class="flex items-center justify-center h-64">
                        <div class="text-center">

                            @if("COMPLETED".equals(dashboard.contemplationStatus().status()))
                            <div>
                                <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                                    </svg>
                                </div>
                                <h4 class="text-lg font-medium text-green-600 mb-1">Executada com sucesso</h4>
                                <p class="text-sm text-slate-500">Última execução: ${dashboard.contemplationStatus().endTime()}</p>
                            </div>
                            @elseif("RUNNING".equals(dashboard.contemplationStatus().status()))
                            <div>
                                <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-blue-600 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                    </svg>
                                </div>
                                <h4 class="text-lg font-medium text-blue-600 mb-1">Em execução...</h4>
                                <p class="text-sm text-slate-500">Iniciada em: ${dashboard.contemplationStatus().startTime()}</p>
                            </div>
                            @elseif("FAILED".equals(dashboard.contemplationStatus().status()))
                            <div>
                                <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </div>
                                <h4 class="text-lg font-medium text-red-600 mb-1">Falha na execução</h4>
                                <p class="text-sm text-slate-500">Última tentativa: ${dashboard.contemplationStatus().endTime()}</p>
                            </div>
                            @elseif("INTERRUPTED".equals(dashboard.contemplationStatus().status()))
                            <div>
                                <div class="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v3m0 4h.01M10.3 3.7L2.7 17a2 2 0 001.7 3h15.2a2 2 0 001.7-3L13.7 3.7a2 2 0 00-3.4 0z" />
                                    </svg>
                                </div>
                                <h4 class="text-lg font-medium text-amber-600 mb-1">Execução interrompida</h4>
                                <p class="text-sm text-slate-500">Lease expirada em: ${dashboard.contemplationStatus().endTime()}</p>
                            </div>
                            @else
                            <div>
                                <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                </div>
                                <h4 class="text-lg font-medium text-gray-500 mb-1">Aguardando execução</h4>
                                <p class="text-sm text-slate-500">Nenhuma execução registrada</p>
                            </div>
                            @endif

                            <p class="text-sm text-slate-500 mt-2">
                                Total contemplados hoje: ${dashboard.contemplationStatus().totalContemplatedToday()} pacientes
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            <%-- Bottlenecks and Slot Occupancy Section --%>
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-8">
                <div class="bg-white p-4 rounded-lg shadow-md">
                    <h3 class="text-lg font-semibold text-slate-700 mb-4">Top 10 Maiores Filas de Espera</h3>
                    <div id="bottleneckChart" class="overflow-y-auto" style="max-height: 420px;"></div>
                </div>
                <div class="bg-white p-4 rounded-lg shadow-md">
                    <h3 class="text-lg font-semibold text-slate-700 mb-4">Ocupação de Vagas por UBS (Mês Atual)</h3>
                    <div id="occupancyChart" class="overflow-y-auto" style="max-height: 420px;"></div>
                </div>
            </div>
            <script src="${res.url("/js/apexcharts-3.54.1.min.js")}" defer></script>
            <script src="${res.url("/js/dashboard.js")}" defer></script>
        @endif

        <%-- Dashboard para demais roles --%>
        @if(ubsDashboard != null)

            <%-- Busca de Paciente --%>
            <div class="mb-6">
                <h2 class="text-2xl font-bold text-slate-700 mb-4">Buscar Paciente</h2>
                <div class="bg-white rounded-lg shadow-md p-4">
                    <div class="flex flex-col w-full sm:w-2/3 relative">
                        <label class="text-sm" for="patientSearch">Nome, Cartão SUS ou CPF:</label>
                        <input type="search" id="patientSearch" name="name"
                               class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                               placeholder="Digite um nome, Cartão SUS ou CPF" hx-get="/patient-list/search"
                               hx-trigger="input changed delay:500ms, patientSearch" hx-target="#dropdown"
                               hx-swap="outerHTML" />
                        <div class="absolute top-full w-full z-50">
                            <div id="dropdown" class="bg-white border border-gray-300 rounded shadow-lg hidden"></div>
                        </div>
                    </div>
                </div>
            </div>

            <%-- Cards da UBS do usuário --%>
            <div class="mb-6">
                <h2 class="text-2xl font-bold text-slate-700 mb-4">${ubsDashboard.ubsName()}</h2>
                <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div class="bg-white rounded-lg shadow-md p-4 border-l-4 border-blue-500">
                        <p class="text-sm text-slate-500">Marcações em aberto</p>
                        <p class="text-3xl font-bold text-slate-700 mt-1">${ubsDashboard.totalOpenAppointments()}</p>
                    </div>
                    <div class="bg-white rounded-lg shadow-md p-4 border-l-4 border-green-500">
                        <p class="text-sm text-slate-500">Contemplados (mês)</p>
                        <p class="text-3xl font-bold text-slate-700 mt-1">${ubsDashboard.totalContemplatedThisMonth()}</p>
                    </div>
                    <div class="bg-white rounded-lg shadow-md p-4 border-l-4 border-purple-500">
                        <p class="text-sm text-slate-500">Pacientes Cadastrados</p>
                        <p class="text-3xl font-bold text-slate-700 mt-1">${ubsDashboard.totalPatients()}</p>
                    </div>
                </div>
            </div>

            <%-- Tabela: Contemplados do Mês --%>
            <div class="bg-white rounded-lg shadow-md p-4">
                <h3 class="text-lg font-semibold text-slate-700 mb-4">Contemplados do Mês</h3>
                <div class="overflow-x-auto">
                    <table class="min-w-full bg-white">
                        <thead>
                            <tr class="bg-slate-200">
                                <th class="text-sm rounded-l-lg py-2 px-4">N.</th>
                                <th class="text-sm pl-4">Paciente</th>
                                <th class="text-sm pl-4">Especialidade</th>
                                <th class="text-sm pl-4">Marcação</th>
                                <th class="text-sm pl-4">Procedimento</th>
                                <th class="text-sm rounded-r-lg py-2 px-4">Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            @if(ubsDashboard.contemplatedThisMonth().isEmpty())
                            <tr>
                                <td colspan="6" class="py-4 text-center text-sm text-slate-400">Nenhum paciente contemplado neste mês.</td>
                            </tr>
                            @else
                            !{int rowIdx = 0;}
                            @for(var row : ubsDashboard.contemplatedThisMonth())
                            !{rowIdx++;}
                            <tr class="overflow-hidden border-slate-200 text-slate-600 hover:bg-slate-50">
                                <td class="border-b text-sm py-2 px-4">${rowIdx}</td>
                                <td class="border-b text-sm pl-4">${row.patientName()}</td>
                                <td class="border-b text-sm pl-4">${row.specialty()}</td>
                                <td class="border-b text-sm pl-4">${row.procedureType().getDescription()}</td>
                                <td class="border-b text-sm pl-4">${row.medicalProcedureDescription()}</td>
                                <td class="border-b text-sm py-2 px-4">${row.appointmentStatus().getDescription()}</td>
                            </tr>
                            @endfor
                            @endif
                        </tbody>
                    </table>
                </div>
            </div>
        @endif

        <%-- Aviso se usuário não está vinculado a nenhuma UBS --%>
        @if(dashboard == null && ubsDashboard == null)
        <div class="bg-amber-50 border border-amber-200 rounded-lg p-6 text-amber-700">
            Seu usuário não está vinculado a nenhuma UBS. Contate o administrador.
        </div>
        @endif

    </div>

    @template.fragments.footer()
</main>
</body>
</html>

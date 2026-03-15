@import br.com.tecsus.sigaubs.dtos.DashboardDTO
@import br.com.tecsus.sigaubs.dtos.UBSDashboardDTO
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param DashboardDTO dashboard = null
@param UBSDashboardDTO ubsDashboard = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "SIGA-UBS", res = res)
<body id="body" class="flex w-screen h-dvh items-center">
@template.fragments.sidebar(secHelper = secHelper, res = res)
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
                    <div x-data="{ showModal: false, tooltipX: 0, tooltipY: 0 }" @mouseover="showModal = true"
                         @mouseleave="showModal = false"
                         @mousemove="tooltipX = ($event.clientX + 270 > window.innerWidth) ? $event.clientX - 270 : $event.clientX + 10; tooltipY = ($event.clientY + 230 > window.innerHeight) ? $event.clientY - 230 : $event.clientY + 10"
                         class="relative">
                        <div class="bg-white rounded-lg shadow-md p-4 cursor-pointer hover:shadow-lg transition duration-300 border-l-4 ${colorIdx == 0 ? "border-blue-500" : colorIdx == 1 ? "border-green-500" : colorIdx == 2 ? "border-purple-500" : colorIdx == 3 ? "border-amber-500" : "border-rose-500"}">
                            <h3 class="font-bold text-lg text-slate-700">${ubs.name()}</h3>
                            <p class="text-sm text-slate-500">Bairro: ${ubs.neighborhood()}</p>
                        </div>

                        <%-- Modal Tooltip --%>
                        <div x-show="showModal" x-cloak
                             class="fixed z-50 bg-white p-4 rounded-lg shadow-xl border border-gray-200 w-64"
                             :style="'top: ' + tooltipY + 'px; left: ' + tooltipX + 'px'">
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

                            @if("DONE".equals(dashboard.contemplationStatus().status()))
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

            <%-- ApexCharts Script (apenas para ADMIN/SMS) --%>
            <script src="https://cdn.jsdelivr.net/npm/apexcharts"></script>
            <script>
                document.addEventListener('DOMContentLoaded', function () {
                    var dailyLabels = [
                        @for(var d : dashboard.dailyAppointments())
                        "${d.day()}",
                        @endfor
                    ];
                    var dailyData = [
                        @for(var d : dashboard.dailyAppointments())
                        ${d.total()},
                        @endfor
                    ];

                    var monthlyOpenLabels = [
                        @for(var m : dashboard.monthlyOpenAppointments())
                        "${m.month()}",
                        @endfor
                    ];
                    var monthlyOpenData = [
                        @for(var m : dashboard.monthlyOpenAppointments())
                        ${m.total()},
                        @endfor
                    ];

                    var monthlyContLabels = [
                        @for(var m : dashboard.monthlyContemplations())
                        "${m.month()}",
                        @endfor
                    ];
                    var monthlyContData = [
                        @for(var m : dashboard.monthlyContemplations())
                        ${m.total()},
                        @endfor
                    ];

                    var pieLabels = [
                        @for(var p : dashboard.priorityDistribution())
                        "${p.priority()}",
                        @endfor
                    ];
                    var pieData = [
                        @for(var p : dashboard.priorityDistribution())
                        ${p.total()},
                        @endfor
                    ];

                    var procLabels = [
                        @for(var p : dashboard.procedureTypeDistribution())
                        "${p.procedureType()}",
                        @endfor
                    ];
                    var procData = [
                        @for(var p : dashboard.procedureTypeDistribution())
                        ${p.total()},
                        @endfor
                    ];

                    var bottleneckLabels = [
                        @for(var b : dashboard.topBottlenecks())
                        "${b.specialty()} - ${b.procedure()}",
                        @endfor
                    ];
                    var bottleneckData = [
                        @for(var b : dashboard.topBottlenecks())
                        ${b.total()},
                        @endfor
                    ];

                    var ubsNames = [
                        @for(var s : dashboard.slotOccupancy())
                        "${s.ubsName()}",
                        @endfor
                    ];
                    var totalSlotsData = [
                        @for(var s : dashboard.slotOccupancy())
                        ${s.totalSlots()},
                        @endfor
                    ];
                    var consumedSlotsData = [
                        @for(var s : dashboard.slotOccupancy())
                        ${s.consumedSlots()},
                        @endfor
                    ];

                    var monthlyLabels = monthlyOpenLabels.length >= monthlyContLabels.length ? monthlyOpenLabels : monthlyContLabels;

                    var dailyOptions = {
                        series: [{ name: 'Marcações', data: dailyData }],
                        chart: { height: '100%', type: 'line', zoom: { enabled: false }, toolbar: { show: false } },
                        dataLabels: { enabled: false },
                        stroke: { curve: 'smooth', width: 3, colors: ['#3B82F6'] },
                        xaxis: { categories: dailyLabels },
                        tooltip: { y: { formatter: function (val) { return val + " marcações" } } },
                        colors: ['#3B82F6'],
                        noData: { text: 'Sem dados para o período', align: 'center', verticalAlign: 'middle' }
                    };
                    new ApexCharts(document.querySelector("#dailyChart"), dailyOptions).render();

                    var monthlyOptions = {
                        series: [{ name: 'Marcações em Aberto', data: monthlyOpenData }, { name: 'Contemplados', data: monthlyContData }],
                        chart: { height: '100%', type: 'line', zoom: { enabled: false }, toolbar: { show: false } },
                        dataLabels: { enabled: false },
                        stroke: { curve: 'smooth', width: 3 },
                        colors: ['#EF4444', '#10B981'],
                        xaxis: { categories: monthlyLabels },
                        tooltip: { y: { formatter: function (val) { return val + " pacientes" } } },
                        legend: { position: 'top' },
                        noData: { text: 'Sem dados para o período', align: 'center', verticalAlign: 'middle' }
                    };
                    new ApexCharts(document.querySelector("#monthlyChart"), monthlyOptions).render();

                    var priorityColorMap = { 'Urgência': '#EF4444', 'Retorno': '#3B82F6', 'Prioritário': '#F59E0B', 'Eletivo': '#10B981', 'Administrativo': '#8B5CF6' };
                    var priorityColors = pieLabels.map(function (label) { return priorityColorMap[label] || '#6B7280'; });
                    var priorityOptions = {
                        series: pieData, chart: { type: 'donut', height: 300 }, labels: pieLabels, colors: priorityColors,
                        plotOptions: { pie: { donut: { labels: { show: true, total: { show: true, label: 'Total em Aberto', fontSize: '14px', fontWeight: 600 } } } } },
                        dataLabels: { enabled: true, formatter: function (val, opts) { return opts.w.config.series[opts.seriesIndex] + ' (' + val.toFixed(0) + '%)'; } },
                        responsive: [{ breakpoint: 480, options: { chart: { height: 250 } } }],
                        legend: { position: 'bottom' },
                        noData: { text: 'Sem dados', align: 'center', verticalAlign: 'middle' }
                    };
                    new ApexCharts(document.querySelector("#priorityDonut"), priorityOptions).render();

                    var procColorMap = { 'Consulta': '#3B82F6', 'Exame': '#F59E0B', 'Cirurgia': '#EF4444' };
                    var procColors = procLabels.map(function (label) { return procColorMap[label] || '#6B7280'; });
                    var procedureOptions = {
                        series: procData, chart: { type: 'donut', height: 300 }, labels: procLabels, colors: procColors,
                        plotOptions: { pie: { donut: { labels: { show: true, total: { show: true, label: 'Total', fontSize: '14px', fontWeight: 600 } } } } },
                        dataLabels: { enabled: true, formatter: function (val, opts) { return opts.w.config.series[opts.seriesIndex] + ' (' + val.toFixed(0) + '%)'; } },
                        responsive: [{ breakpoint: 480, options: { chart: { height: 250 } } }],
                        legend: { position: 'bottom' },
                        noData: { text: 'Sem dados', align: 'center', verticalAlign: 'middle' }
                    };
                    new ApexCharts(document.querySelector("#procedureDonut"), procedureOptions).render();

                    var bottleneckHeight = Math.max(300, bottleneckLabels.length * 40);
                    var bottleneckOptions = {
                        series: [{ name: 'Na fila', data: bottleneckData }],
                        chart: { type: 'bar', height: bottleneckHeight, toolbar: { show: false } },
                        plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '60%' } },
                        xaxis: { categories: bottleneckLabels },
                        colors: ['#EF4444'],
                        dataLabels: { enabled: true, style: { fontSize: '12px' } },
                        tooltip: { y: { formatter: function (val) { return val + ' marcações'; } } },
                        noData: { text: 'Sem gargalos identificados', align: 'center', verticalAlign: 'middle' }
                    };
                    new ApexCharts(document.querySelector("#bottleneckChart"), bottleneckOptions).render();

                    var availableData = totalSlotsData.map(function (total, i) { return total - consumedSlotsData[i]; });
                    var occupancyHeight = Math.max(300, ubsNames.length * 45);
                    var occupancyOptions = {
                        series: [{ name: 'Consumidas', data: consumedSlotsData }, { name: 'Disponíveis', data: availableData }],
                        chart: { type: 'bar', height: occupancyHeight, stacked: true, toolbar: { show: false } },
                        plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '55%' } },
                        xaxis: { categories: ubsNames },
                        colors: ['#3B82F6', '#D1FAE5'],
                        dataLabels: { enabled: true, style: { fontSize: '11px' } },
                        legend: { position: 'top' },
                        tooltip: { y: { formatter: function (val) { return val + ' vagas'; } } },
                        noData: { text: 'Sem vagas cadastradas neste mês', align: 'center', verticalAlign: 'middle' }
                    };
                    new ApexCharts(document.querySelector("#occupancyChart"), occupancyOptions).render();
                });
            </script>
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

            <script>
                $(document).click(function (event) {
                    if (!$(event.target).closest('#dropdown').length && !$(event.target).closest('#patientSearch').length) {
                        $('#dropdown').hide();
                    }
                });
                $('#patientSearch').on('input', function () { $('#dropdown').show(); });
            </script>
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

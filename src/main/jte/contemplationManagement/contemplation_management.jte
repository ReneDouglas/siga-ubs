@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.entities.Contemplation
@import br.com.tecsus.sigaubs.entities.Specialty
@import br.com.tecsus.sigaubs.enums.AppointmentStatus
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@import java.util.List
@param List<BasicHealthUnit> basicHealthUnits
@param List<Specialty> specialties
@param Page<Contemplation> consultasPage
@param Page<Contemplation> examesPage
@param Page<Contemplation> cirurgiasPage
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Long selectedUBS = null
@param Long selectedSpecialty = null
@param String selectedMonth = null
@param String selectedStatus = null
@param String hide = "hidden"
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Gestão de Contemplados", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <%-- Panel 1 - Pacientes Contemplados --%>
            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">groups</span>Pacientes Contemplados
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-[2000px] max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <div class="py-4 px-4">
                        <form class="grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-2 gap-x-4 w-full items-end"
                              action="/contemplation-management/search" method="get">
                            <div class="flex flex-col w-full">
                                <label class="text-sm" for="ubs">UBS:</label>
                                <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="ubs" name="basicHealthUnit">
                                    <option value="">Selecione</option>
                                    @for(var ubs : basicHealthUnits)
                                    <option value="${ubs.getId()}" selected="${selectedUBS != null && selectedUBS.equals(ubs.getId())}">${ubs.getName()}</option>
                                    @endfor
                                </select>
                            </div>
                            <div class="flex flex-col w-full">
                                <label class="text-sm" for="specialty">Especialidade:</label>
                                <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="specialty" name="specialty">
                                    <option value="">Selecione</option>
                                    @for(var specialty : specialties)
                                    <option value="${specialty.getId()}" selected="${selectedSpecialty != null && selectedSpecialty.equals(specialty.getId())}">${specialty.getTitle()}</option>
                                    @endfor
                                </select>
                            </div>
                            <div class="flex flex-col w-full">
                                <label class="text-sm" for="month">Mês de Referência:</label>
                                <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                       type="month" id="month" name="referenceMonth">
                                <input type="hidden" id="selectedMonth" value="${selectedMonth != null ? selectedMonth : ""}">
                            </div>
                            <div class="flex flex-col w-full">
                                <label class="text-sm" for="status">Status:</label>
                                <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                        id="status" name="status">
                                    <option value="">Todos</option>
                                    @for(var apptStatus : AppointmentStatus.getContemplationValues())
                                    <option value="${apptStatus.getDescription()}" selected="${selectedStatus != null && selectedStatus.equals(apptStatus.getDescription())}">${apptStatus.getDescription()}</option>
                                    @endfor
                                </select>
                            </div>
                            <div class="flex items-end">
                                <button type="submit"
                                        class="inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                                    <span class="-ml-1 material-symbols-outlined mx-1">search</span>Pesquisar
                                </button>
                            </div>
                        </form>
                        <%-- Abas --%>
                        <div class="flex space-x-1 border-b border-slate-300 mt-4 mb-4">
                            <button class="tab-btn w-1/3 py-2 text-sm text-center bg-slate-300 hover:bg-slate-300 rounded-t-lg focus:outline-none transition duration-200"
                                    onclick="showTab('consultas', this)">
                                Consultas
                            </button>
                            <button class="tab-btn w-1/3 py-2 text-sm text-center bg-slate-200 hover:bg-slate-300 rounded-t-lg focus:outline-none transition duration-200"
                                    onclick="showTab('exames', this)">
                                Exames
                            </button>
                            <button class="tab-btn w-1/3 py-2 text-sm text-center bg-slate-200 hover:bg-slate-300 rounded-t-lg focus:outline-none transition duration-200"
                                    onclick="showTab('cirurgias', this)">
                                Cirurgias
                            </button>
                        </div>
                        <%-- Conteúdo das abas --%>
                        <div id="contentPanel">
                            @template.contemplationManagement.contemplationFragments.consultas_datatable(consultasPage = consultasPage, selectedUBS = selectedUBS, selectedSpecialty = selectedSpecialty, selectedMonth = selectedMonth, selectedStatus = selectedStatus, hide = "")
                            @template.contemplationManagement.contemplationFragments.exames_datatable(examesPage = examesPage, selectedUBS = selectedUBS, selectedSpecialty = selectedSpecialty, selectedMonth = selectedMonth, selectedStatus = selectedStatus, hide = hide)
                            @template.contemplationManagement.contemplationFragments.cirurgias_datatable(cirurgiasPage = cirurgiasPage, selectedUBS = selectedUBS, selectedSpecialty = selectedSpecialty, selectedMonth = selectedMonth, selectedStatus = selectedStatus, hide = hide)
                        </div>
                    </div>
                </div>
            </div>

            <%-- Panel 2 - Histórico de vagas --%>
            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panel2" class="peer hidden" checked>
                <label for="panel2"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">history</span>Histórico de vagas
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-[2000px] max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <div class="p-4 rounded-lg">
                    </div>
                </div>
            </div>

        </div>
    </section>
    <div id="patientAppointment-modal"></div>
</main>
</body>
<script>

    window.addEventListener("DOMContentLoaded", function () {
        const selectedMonth = document.getElementById("selectedMonth");
        const monthControl = document.querySelector('input[type="month"]');
        if (selectedMonth.value !== "") {
            monthControl.value = selectedMonth.value;
        } else {
            const date = new Date();
            const month = ("0" + (date.getMonth() + 1)).slice(-2);
            const year = date.getFullYear();
            monthControl.value = year + "-" + month;
        }
    });

    function showTab(tabId, clickedBtn) {
        document.querySelectorAll('.tab-content').forEach(function (content) {
            content.classList.add('hidden');
        });
        document.getElementById(tabId).classList.remove('hidden');

        clickedBtn.closest('.flex').querySelectorAll('.tab-btn').forEach(function (btn) {
            btn.classList.remove('bg-slate-300');
            btn.classList.add('bg-slate-200');
        });
        clickedBtn.classList.remove('bg-slate-200');
        clickedBtn.classList.add('bg-slate-300');
    }

    function showModalTab(tabId, clickedBtn) {
        document.getElementById('patient-details').classList.add('hidden');
        document.getElementById('appointment-details').classList.add('hidden');
        document.getElementById('status-details').classList.add('hidden');
        document.getElementById(tabId).classList.remove('hidden');

        clickedBtn.closest('.flex').querySelectorAll('.modal-tab-btn').forEach(function (btn) {
            btn.classList.remove('bg-slate-300');
            btn.classList.add('bg-slate-200');
        });
        clickedBtn.classList.remove('bg-slate-200');
        clickedBtn.classList.add('bg-slate-300');
    }

    function closeModal() {
        let modal = document.getElementById("patientAppointment-modal");
        modal.classList.add("opacity-0");
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }

    function handleCancel() {
        Swal.fire({
            title: "Cancelar contemplação",
            text: "Tem certeza que deseja cancelar?",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            cancelButtonColor: "#d33",
            confirmButtonText: "Sim",
            cancelButtonText: "Cancelar"
        }).then(function (result) {
            if (result.isConfirmed) {
                Swal.fire({
                    title: "Digite o motivo do cancelamento",
                    input: "textarea",
                    inputPlaceholder: "Motivo...",
                    showCancelButton: true,
                    confirmButtonText: "Enviar",
                    cancelButtonText: "Cancelar",
                    inputAttributes: {
                        maxlength: "255"
                    },
                    inputValidator: (value) => {
                        if (!value) {
                            return "Você precisa digitar um motivo!";
                        }
                    }
                }).then(function (textResult) {
                    if (textResult.isConfirmed && textResult.value) {
                        document.getElementById("reason").value = textResult.value;
                        document.getElementById("cancelForm").submit();
                    }
                });
            }
        });
    }

    function handleConfirmation() {
        Swal.fire({
            title: "Confirmar contemplação",
            text: "Tem certeza que deseja continuar?",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            cancelButtonColor: "#d33",
            confirmButtonText: "Sim",
            cancelButtonText: "Cancelar"
        }).then(function (result) {
            if (result.isConfirmed) {
                document.getElementById("confirmForm").submit();
            }
        });
    }
</script>
</html>

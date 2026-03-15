@import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO
@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.entities.MedicalProcedure
@import br.com.tecsus.sigaubs.entities.MedicalSlot
@import br.com.tecsus.sigaubs.entities.Specialty
@import br.com.tecsus.sigaubs.enums.ProcedureType
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import org.springframework.data.domain.Page
@import java.util.List
@param List<BasicHealthUnit> basicHealthUnits
@param List<Specialty> specialties
@param Page<MedicalSlot> medicalSlotsPage
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Gestão de Vagas", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <%-- Panel 1 - Cadastrar Vagas --%>
            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">note_stack_add</span>Cadastrar Vagas Disponíveis
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <div class="py-4 px-4">
                        <div class="flex flex-col lg:flex-row justify-normal">
                            <div class="p-0.5 w-full lg:w-1/3">
                                <form id="availableMedicalSlotAddRow"
                                      hx-post="/medicalSlot-management/slots/add"
                                      hx-target="#availableMedicalSlotsFormTable"
                                      hx-on::after-request="if(event.detail.elt.tagName != 'SELECT') clearFormAndSetCurrentMonth()"
                                      hx-swap="outerHTML"
                                      class="space-y-4 bg-slate-50 p-4 rounded-lg">

                                    <div class="flex flex-col w-full">
                                        <label class="text-sm" for="ubsSlots">UBS:</label>
                                        <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                                id="ubsSlots" name="basicHealthUnit.id" required>
                                            <option value="">Selecione</option>
                                            @for(var ubs : basicHealthUnits)
                                            <option value="${ubs.getId()}">${ubs.getName()}</option>
                                            @endfor
                                        </select>
                                    </div>

                                    <div class="flex flex-col w-full">
                                        <label class="text-sm" for="totalSlots">Vagas:</label>
                                        <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                               type="number" min="1" step="1" id="totalSlots" name="totalSlots" required>
                                    </div>

                                    <div class="flex flex-col w-full">
                                        <label class="text-sm" for="specialty">Especialidade:</label>
                                        <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                                id="specialty" name="specialty" required>
                                            <option value="">Selecione</option>
                                            @for(var specialty : specialties)
                                            <option value="${specialty.getId()}">${specialty.getTitle()}</option>
                                            @endfor
                                        </select>
                                    </div>

                                    <div class="flex flex-col w-full">
                                        <label class="text-sm" for="procedureType">Marcação:</label>
                                        <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                                id="procedureType" name="procedureType"
                                                hx-get="/medicalSlot-management/procedures"
                                                hx-target="#selectMedicalProcedure"
                                                hx-include="[name='specialty']"
                                                hx-vals='{"slot":true}'
                                                hx-swap="outerHTML"
                                                disabled required>
                                            <option value="">Selecione</option>
                                            @for(var type : ProcedureType.values())
                                            <option value="${type.toString()}">${type.getDescription()}</option>
                                            @endfor
                                        </select>
                                    </div>

                                    @template.medicalSlotManagement.medicalSlotFragments.medicalProcedures(procedures = java.util.List.of())
                                    <div class="flex flex-col w-full">
                                        <label class="text-sm" for="referenceMonth">Mês de Referência:</label>
                                        <input class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                                               type="month" id="referenceMonth" name="referenceMonth" required>
                                    </div>
                                    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                                </form>
                            </div>
                            <div class="p-1 flex items-center justify-center lg:justify-start py-4 lg:py-1">
                                <button id="addMedicalSlot" form="availableMedicalSlotAddRow"
                                        class="inline-flex p-2 rounded-lg bg-slate-900 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                                    <span class="material-symbols-outlined transform rotate-90 lg:rotate-0">double_arrow</span>
                                </button>
                            </div>
                            <div class="p-2 bg-slate-50 overflow-auto max-h-[500px] rounded-lg w-full lg:flex-1">
                                @template.medicalSlotManagement.medicalSlotFragments.available_slots_form_table(availableMedicalSlotsForm = new AvailableMedicalSlotsFormDTO(), csrf = csrf)
                            </div>
                        </div>
                        <div class="flex justify-start mt-4">
                            <button id="saveButton" type="submit" form="availableMedicalSlotsFormTable"
                                    class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                                Salvar
                            </button>
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
                        @template.medicalSlotManagement.medicalSlotFragments.medicalSlot_datatable(medicalSlotsPage = medicalSlotsPage)
                    </div>
                </div>
            </div>

        </div>
    </section>
</main>
</body>
<script>

    document.getElementById('saveButton').addEventListener('click', function(event) {
        event.preventDefault();

        Swal.fire({
            title: 'Confirmação',
            text: "Você deseja salvar as alterações?",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Sim',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                document.getElementById('availableMedicalSlotsFormTable').submit();
            }
        });
    });

    window.addEventListener("DOMContentLoaded", function () {
        const monthControl = document.querySelector('input[type="month"]');
        if (monthControl) {
            const date = new Date();
            const month = ("0" + (date.getMonth() + 1)).slice(-2);
            const year = date.getFullYear();
            monthControl.value = year + "-" + month;
        }
    });

    function clearFormAndSetCurrentMonth() {
        document.getElementById('availableMedicalSlotAddRow').reset();
        const currentDate = new Date();
        const year = currentDate.getFullYear();
        const month = String(currentDate.getMonth() + 1).padStart(2, '0');
        document.getElementById('referenceMonth').value = year + "-" + month;
    }

    $(document).ready(function () {
        $('#specialty').change(function () {
            if ($(this).val()) {
                $('#procedureType').prop('disabled', false).val(null);
                $('#medicalProcedure').prop('disabled', true).empty().append('<option value="">Selecione</option>');
            } else {
                $('#procedureType').prop('disabled', true).val(null);
            }
        });
    });
</script>
</html>

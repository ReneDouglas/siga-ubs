@import br.com.tecsus.sigaubs.dtos.ProcedureDTO
@import br.com.tecsus.sigaubs.dtos.SpecialtyDTO
@import br.com.tecsus.sigaubs.entities.Specialty
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import java.util.List
@param SpecialtyDTO specialtyDTO
@param List<Specialty> specialties
@param List<ProcedureDTO> procedures
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Gestão de Especialidades", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <%-- Panel 1 - Cadastrar Especialidades --%>
            <div class="w-full bg-white rounded-lg mt-4">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">person_add</span>Cadastrar Especialidades
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-300 ease-in-out">
                    @template.specialtyManagement.specialtyFragments.specialty_form(specialtyDTO = specialtyDTO, procedures = procedures, csrf = csrf)
                </div>
            </div>

            <%-- Panel 2 - Especialidades --%>
            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panel2" class="peer hidden" checked>
                <label for="panel2"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">person_search</span> Especialidades
                    </div>
                </label>
                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    <div class="mt-4">
                        <div class="pb-4 px-4 rounded-lg" id="datatable-panel">
                            @template.specialtyManagement.specialtyFragments.specialty_info(specialties = specialties)
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </section>
</main>
</body>
<script>
    $(document).ready(function () {
        var procedures = [];
        $('#addProcedureBtn').click(function () {
            var procedureName = $('#procedure').val();
            var procedureType = $('#procedureType option:selected').text();

            if (procedureName && procedureType !== "Selecione") {
                $('#procedureTable tbody').append('<tr><td class="border-b text-sm py-2 pl-4 text-nowrap">' + procedureName + '</td><td class="border-b text-sm py-2 pl-4">' + procedureType + '</td><td class="border-b text-sm py-2 pl-4"></td></tr>');

                procedures.push({description: procedureName, procedureType: procedureType});

                $('#procedure').val('').prop('disabled', false);
                $('#procedureType').prop('selectedIndex', 0);
            } else {
                alert("Por favor, preencha ambos os campos antes de adicionar um procedimento.");
            }
        });

        $('form').submit(function () {
            $('#proceduresJson').val(JSON.stringify(procedures));
        });

        $('#procedureType').change(function () {
            var selectedValue = $('#procedureType option:selected').val();
            if (selectedValue === "Consulta") {
                $('#procedure').val('-').prop('disabled', true);
            } else {
                $('#procedure').prop('disabled', false);
            }
        });
    });
</script>
</html>

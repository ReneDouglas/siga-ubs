@import br.com.tecsus.sigaubs.entities.Patient
@import br.com.tecsus.sigaubs.entities.BasicHealthUnit
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import java.util.List
@param Patient patient
@param SecurityHelper secHelper
@param CsrfHelper csrf
@param StaticResourceHelper res
@param Boolean error = null
@param String message = null
@param List<BasicHealthUnit> basicHealthUnits = null
@param BasicHealthUnit systemUserUBS = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Cadastro de Paciente", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res)

<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res)
    @template.fragments.loading()
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">

            <%-- Panel 1 --%>
            <div class="w-full bg-white rounded-lg my-4">
                <input type="checkbox" id="panel1" class="peer hidden" checked>
                <label for="panel1"
                       class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">personal_injury</span>Dados do Paciente
                    </div>
                </label>

                <div class="flex flex-col justify-center peer-checked:max-h-full max-h-0 overflow-hidden overscroll-contain transition-max-height duration-500 ease-in-out">
                    @template.patientManagement.patientFragments.patient_form(patient = patient, csrf = csrf, secHelper = secHelper, res = res, error = error, message = message, basicHealthUnits = basicHealthUnits, systemUserUBS = systemUserUBS)
                </div>
            </div>

        </div>
    </section>
</main>
</body>
<script src="${res.url("/js/cpfmask.js")}"></script>
<script src="${res.url("/js/phonemask.js")}"></script>
</html>

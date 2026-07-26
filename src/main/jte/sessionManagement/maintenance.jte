@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@import java.time.LocalDateTime
@param StaticResourceHelper res
@param String maintenanceMessage = null
@param LocalDateTime maintenanceEndDate = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Sistema em Manutenção", res = res)
<body class="flex flex-col h-dvh w-full items-center bg-slate-900">
    <main class="flex flex-col h-full w-full justify-center items-center p-4">
        <div class="flex flex-col bg-white shadow-sm rounded-2xl w-full max-w-xl p-8">
            <div class="flex flex-col justify-center items-center mb-6">
                <img class="w-24" src="${res.url("/images/logo-v2.svg")}" alt="logo-principal">
                <img class="w-32 mt-2" src="${res.url("/images/siga-ubs.svg")}" alt="nome-logo-principal">
            </div>
            <div class="flex flex-col items-center text-center">
                <span class="material-symbols-outlined text-5xl text-amber-500 mb-2">construction</span>
                <h1 class="text-xl font-semibold text-slate-800">Sistema em manutenção</h1>
                <p class="text-sm text-slate-600 mt-3">
                    ${maintenanceMessage != null && !maintenanceMessage.isBlank() ? maintenanceMessage : "O acesso está temporariamente indisponível."}
                </p>
                @if(maintenanceEndDate != null)
                <p class="text-sm text-slate-500 mt-2">
                    Previsão de retorno: ${TemplateUtils.formatDateTime(maintenanceEndDate)}
                </p>
                @endif
            </div>
        </div>
    </main>

    <footer class="flex items-center justify-center h-12 sm:h-16 w-full bg-slate-950 text-white">
        <div class="text-center px-4 overflow-hidden whitespace-nowrap overflow-ellipsis">
            <span class="text-xs">&copy; 2024 Halotec. Todos os direitos reservados.</span>
        </div>
    </footer>
</body>
</html>

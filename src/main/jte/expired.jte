@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param StaticResourceHelper res
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Sessão Expirada", res = res)
<body class="flex flex-col h-dvh w-full items-center bg-slate-900">
    <main class="flex flex-col h-full w-full justify-center items-center p-4">
        <div class="flex flex-col bg-white shadow-sm rounded-2xl w-full sm:max-w-sm md:max-w-md p-8 items-center text-center gap-4">

            <%-- Logos --%>
            <img class="w-20" src="${res.url("/images/logo-v2.svg")}" alt="logo">
            <img class="w-28" src="${res.url("/images/siga-ubs.svg")}" alt="siga-ubs">

            <%-- Ícone --%>
            <span class="material-symbols-outlined text-6xl text-amber-500 mt-2">timer_off</span>

            <%-- Título e descrição --%>
            <h1 class="text-2xl font-bold text-slate-800">Sessão Expirada</h1>
            <p class="text-sm text-slate-500">
                Sua sessão foi encerrada. Faça login novamente para continuar.
            </p>

            <%-- Botão para login --%>
            <a href="/login"
               class="w-full py-2 px-4 rounded-lg bg-slate-900 text-sm text-white text-center cursor-pointer hover:bg-blue-500 transition duration-300">
                Ir para o Login
            </a>
        </div>
    </main>
    @template.fragments.footer()
</body>
</html>

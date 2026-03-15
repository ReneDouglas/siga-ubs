@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param StaticResourceHelper res
@param Integer status = null
@param String error = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Erro", res = res)
<body class="flex flex-col h-dvh w-full items-center bg-slate-900">
    <main class="flex flex-col h-full w-full justify-center items-center p-4">
        <div class="flex flex-col bg-white shadow-sm rounded-2xl w-full sm:max-w-sm md:max-w-md p-8 items-center text-center gap-4">

            <%-- Logos --%>
            <img class="w-20" src="${res.url("/images/logo-v2.svg")}" alt="logo">
            <img class="w-28" src="${res.url("/images/siga-ubs.svg")}" alt="siga-ubs">

            <%-- Ícone --%>
            <span class="material-symbols-outlined text-6xl text-red-500 mt-2">error</span>

            <%-- Código de status --%>
            <h1 class="text-5xl font-bold text-slate-800">${status != null ? status : 500}</h1>

            <%-- Título e mensagem --%>
            <h2 class="text-lg font-semibold text-slate-700">Ocorreu um erro</h2>
            <p class="text-sm text-red-500">${error != null ? error : "Mensagem de erro"}</p>

            <%-- Botão para página inicial --%>
            <a href="/"
               class="w-full py-2 px-4 rounded-lg bg-slate-900 text-sm text-white text-center cursor-pointer hover:bg-blue-500 transition duration-300">
                Voltar para a página inicial
            </a>
        </div>
    </main>
    @template.fragments.footer()
</body>
</html>

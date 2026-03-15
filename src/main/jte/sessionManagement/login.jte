@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param CsrfHelper csrf
@param StaticResourceHelper res
@param Boolean loginError = false
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Login", res = res)
<body class="flex flex-col h-dvh w-full items-center bg-slate-900">
    <%-- Container Principal --%>
    <main class="flex flex-col h-full w-full justify-center items-center p-4">

        <div class="flex flex-col sm:flex-row bg-white shadow-sm rounded-2xl w-full sm:max-w-xl md:max-w-2xl">
            <%-- Seção Logo --%>
            <section class="flex flex-col justify-center items-center bg-slate-200 rounded-2xl sm:rounded-l-2xl p-6 sm:w-48 sm:flex-1">
                <%-- Logo para telas pequenas --%>
                <img class="max-w-52 sm:hidden" src="${res.url("/images/logo-prefeitura-h.png")}" alt="logo-principal">
                <%-- Logo para telas grandes --%>
                <img class="max-w-52 hidden sm:block" src="${res.url("/images/logo-prefeitura-v.png")}" alt="logo-principal">
            </section>

            <%-- Seção do Formulário --%>
            <section class="flex flex-col justify-center items-center p-6 sm:flex-1">
                <div class="flex flex-col justify-center items-center mb-6">
                    <a href="/"><img class="w-24" src="${res.url("/images/logo-v2.svg")}" alt="logo-principal"></a>
                    <a href="/"><img class="w-32 mt-2" src="${res.url("/images/siga-ubs.svg")}" alt="nome-logo-principal"></a>
                </div>
                <form class="flex flex-col items-center space-y-4 w-full" action="/login" method="post">
                    <input id="username" name="username" type="text" placeholder="Nome de Usuário"
                           class="text-sm block w-full rounded-lg border-2 border-stroke py-2 pl-2 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:drop-shadow-lg focus:border-white focus:ring-blue-500 transition transition-300">

                    <input id="password" name="password" type="password" placeholder="Senha"
                           class="text-sm block w-full rounded-lg border-2 border-stroke py-2 pl-2 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:drop-shadow-lg focus:border-white focus:ring-blue-500 transition transition-300">

                    <input type="submit" value="Entrar"
                           class="w-full py-2 px-4 rounded-lg bg-slate-900 text-sm text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition transition-300">

                    <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>

                    <div class="text-nowrap text-sm">
                        <span class="text-xs text-gray-500">Esqueceu a Senha? <a href="/" class="text-blue-500 hover:text-slate-900 transition transition-300">Recuperar</a></span>
                    </div>
                </form>
            </section>
        </div>

        <%-- Card de Erro --%>
        @if(loginError != null && loginError)
        <div class="inline-flex justify-center items-center bg-red-200 sm:text-md text-xs border-t-2 hover:bg-red-100 border-red-400 w-64 sm:w-96 h-10 rounded-lg p-2 absolute left-1/2 bottom-16 sm:bottom-20 transform -translate-x-1/2 transition-all duration-300">
            <span class="material-symbols-outlined inline-block align-middle text-red-400 mr-2">report</span>
            <span class="error text-red-500">Usuário ou senha incorretos.</span>
        </div>
        @endif
    </main>

    <footer class="flex items-center justify-center h-12 sm:h-16 w-full bg-slate-950 text-white">
        <div class="text-center px-4 overflow-hidden whitespace-nowrap overflow-ellipsis">
            <a href="/" class="text-xs hover:text-blue-500 transition transition-300">
                © 2024 Halotec. Todos os direitos reservados.
            </a>
        </div>
    </footer>
</body>
</html>

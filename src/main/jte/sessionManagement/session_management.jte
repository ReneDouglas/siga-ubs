@import br.com.tecsus.sigaubs.dtos.SessionSummaryDTO
@import br.com.tecsus.sigaubs.security.CsrfHelper
@import br.com.tecsus.sigaubs.security.SecurityHelper
@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@import br.com.tecsus.sigaubs.utils.TemplateUtils
@import java.util.List
@param List<SessionSummaryDTO> sessions
@param String targetName
@param String revokeBaseUrl
@param String backUrl
@param Boolean adminArea = false
@param Boolean ownSessions = false
@param Boolean globalSessions = false
@param SecurityHelper secHelper
@param StaticResourceHelper res
@param CsrfHelper csrf
@param Boolean error = null
@param String message = null
<!DOCTYPE html>
<html lang="pt-br">
@template.fragments.head(title = "Sessões", res = res)
<body class="flex w-full flex-1 h-dvh justify-between items-center bg-slate-300">
@template.fragments.sidebar(secHelper = secHelper, res = res, csrf = csrf, adminArea = adminArea)
<main class="flex flex-col h-full w-full overflow-hidden">
    @template.fragments.header(secHelper = secHelper, res = res, adminArea = adminArea)
    @template.fragments.dialogs(error = error, message = message)

    <section class="flex flex-col flex-1 overflow-auto">
        <div class="flex flex-col mx-4 justify-start items-center">
            <div class="w-full bg-white rounded-lg my-4">
                <div class="text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg border-2 border-slate-600">
                    <div class="flex items-center">
                        <span class="material-symbols-outlined mr-4">devices</span>
                        ${globalSessions != null && globalSessions
                            ? "Sessões Ativas"
                            : ownSessions != null && ownSessions
                                ? "Minhas sessões"
                                : "Sessões de " + targetName}
                    </div>
                </div>

                <div class="p-4">
                    <p class="text-sm text-slate-600 mb-4">
                        ${globalSessions != null && globalSessions
                            ? "Confira e encerre os acessos ativos na plataforma."
                            : ownSessions != null && ownSessions
                                ? "Confira onde sua conta está conectada e encerre acessos que não reconhece."
                                : "Encerre acessos ativos desta conta quando necessário."}
                    </p>

                    @if(sessions.isEmpty())
                    <div class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                        Nenhuma sessão ativa encontrada.
                    </div>
                    @else
                    <div class="overflow-x-auto">
                        <table class="min-w-full bg-white">
                            <thead class="font-light text-sm text-slate-600 text-left tracking-wide">
                            <tr class="bg-slate-200">
                                @if(globalSessions != null && globalSessions)
                                <th class="text-sm rounded-l-lg py-2 px-4">Titular</th>
                                <th class="text-sm py-2 px-4">Cliente</th>
                                @else
                                <th class="text-sm rounded-l-lg py-2 px-4">Cliente</th>
                                @endif
                                <th class="text-sm py-2 px-4">Localidade no login</th>
                                <th class="text-sm py-2 px-4">Criada em</th>
                                <th class="text-sm py-2 px-4">Último acesso</th>
                                <th class="text-sm py-2 px-4">Expiração por inatividade</th>
                                <th class="text-sm rounded-r-lg py-2 px-4">Ações</th>
                            </tr>
                            </thead>
                            <tbody>
                            @for(var session : sessions)
                            <tr class="overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200">
                                @if(globalSessions != null && globalSessions)
                                <td class="border-b text-sm py-2 px-4">${session.ownerDescription()}</td>
                                @endif
                                <td class="border-b text-sm py-2 px-4">
                                    ${session.clientDescription()}
                                    @if(session.current())
                                    <span class="ml-2 inline-block px-2 py-1 rounded-xl text-xs bg-green-100 text-green-700">Atual</span>
                                    @endif
                                </td>
                                <td class="border-b text-sm py-2 px-4 text-nowrap">
                                    ${session.locationDescription()}
                                </td>
                                <td class="border-b text-sm py-2 px-4 text-nowrap">${TemplateUtils.formatInstant(session.createdAt())}</td>
                                <td class="border-b text-sm py-2 px-4 text-nowrap">${TemplateUtils.formatInstant(session.lastAccessedAt())}</td>
                                <td class="border-b text-sm py-2 px-4">${session.maximumIdleTime().toMinutes()} minutos</td>
                                <td class="border-b text-sm py-2 px-4">
                                    <form action="${revokeBaseUrl}/${session.managementId()}/revoke" method="post" class="inline-flex">
                                        <input type="hidden" name="${csrf.parameterName()}" value="${csrf.token()}"/>
                                        <button type="submit"
                                                class="relative group inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300">
                                            <span class="material-symbols-outlined hover:text-red-500 transition duration-300">logout</span>
                                            <span class="absolute right-full top-1/2 transform -translate-y-1/2 -translate-x-1 hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1 whitespace-nowrap">
                                                ${session.current() ? "Encerrar sessão atual" : "Encerrar sessão"}
                                            </span>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                            @endfor
                            </tbody>
                        </table>
                    </div>
                    @endif

                    <div class="mt-4">
                        <a href="${backUrl}"
                           class="text-sm inline-block rounded-lg bg-slate-100 py-2.5 px-6 text-slate-700 hover:bg-slate-200 transition duration-300">
                            Voltar
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </section>
    @template.fragments.footer()
</main>
</body>
</html>

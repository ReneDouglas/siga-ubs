@param Boolean error = null
@param String message = null
@if(error != null)
    @if(!error)
        <%-- Alerta de Sucesso --%>
        <div role="alert"
             class="fixed z-50 top-4 right-4 left-4 sm:left-auto sm:w-96 shadow-lg shadow-gray-700 transition-all duration-300 transform translate-y-0">
            <div class="bg-green-500 text-white font-bold rounded-t px-4 py-3 flex items-center justify-between">
                <span class="flex items-center gap-2">
                    <span class="material-symbols-outlined">check_circle</span>
                    Sucesso
                </span>
                <button class="text-green-100 hover:text-white transition-colors focus:outline-none"
                        onclick="this.closest('[role=alert]').style.display='none'">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>
            <div class="border border-t-0 border-green-400 rounded-b bg-green-50 px-4 py-4 text-green-800 shadow-inner">
                <p>${message}</p>
            </div>
        </div>
    @else
        <%-- Alerta de Erro --%>
        <div role="alert"
             class="fixed z-50 top-4 right-4 left-4 sm:left-auto sm:w-96 shadow-lg shadow-gray-700 transition-all duration-300 transform translate-y-0">
            <div class="bg-red-500 text-white font-bold rounded-t px-4 py-3 flex items-center justify-between">
                <span class="flex items-center gap-2">
                    <span class="material-symbols-outlined">error</span>
                    Erro
                </span>
                <button class="text-red-100 hover:text-white transition-colors focus:outline-none"
                        onclick="this.closest('[role=alert]').style.display='none'">
                    <span class="material-symbols-outlined">close</span>
                </button>
            </div>
            <div class="border border-t-0 border-red-400 rounded-b bg-red-50 px-4 py-4 text-red-800 shadow-inner">
                <p>${message}</p>
            </div>
        </div>
    @endif
@endif

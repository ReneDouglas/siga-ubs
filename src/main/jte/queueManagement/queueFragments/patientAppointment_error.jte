@param String message
<div id="patientAppointment-modal" tabindex="-1" aria-hidden="false"
    class="flex opacity-100 overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 z-30 justify-center items-center w-full md:inset-0 h-[calc(100%-1rem)] max-h-full">

    <div class="fixed inset-0 bg-black opacity-50"></div>

    <div class="relative p-4 w-full max-w-lg max-h-full">
        <div class="relative bg-white rounded-lg shadow-2xl">
            <div class="flex items-center justify-between p-4 md:p-5 bg-slate-900 rounded-t border-2 border-slate-600">
                <h3 class="text-xl font-semibold text-slate-200">
                    Detalhes do paciente
                </h3>
                <button type="button" onclick="closeModal()"
                    class="text-slate-400 bg-transparent hover:bg-slate-700 hover:text-white rounded-lg text-sm w-8 h-8 ms-auto inline-flex justify-center items-center">
                    <span class="material-symbols-outlined text-base">close</span>
                    <span class="sr-only">Fechar modal</span>
                </button>
            </div>
            <div class="p-6">
                <div class="border border-red-300 bg-red-100 text-red-700 rounded-lg px-4 py-3 text-sm flex items-start gap-2">
                    <span class="material-symbols-outlined text-base">error</span>
                    <span>${message}</span>
                </div>
                <div class="mt-4 flex justify-end">
                    <button type="button" onclick="closeModal()"
                        class="text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300">
                        Fechar
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

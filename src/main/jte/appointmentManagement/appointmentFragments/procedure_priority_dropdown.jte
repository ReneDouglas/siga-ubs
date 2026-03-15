@import br.com.tecsus.sigaubs.entities.MedicalProcedure
@import br.com.tecsus.sigaubs.enums.Priorities
@import java.util.List
@param List<MedicalProcedure> procedures
@param List<Priorities> priorities
@param Boolean isConsultation = false
<div id="selectProcedureAndPriority" class="flex flex-col w-full col-span-2 sm:col-span-2 lg:col-span-2">
    <div class="grid grid-cols-2 gap-2 gap-x-4">
        <div class="flex flex-col w-full">
            <label class="text-sm" for="medicalProcedure">Procedimento:</label>
            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                    id="medicalProcedure" name="medicalProcedure.id" required>
                <option value="">Selecione</option>
                @for(var procedure : procedures)
                <option value="${procedure.getId()}" selected="${isConsultation}">${procedure.getDescription()}</option>
                @endfor
            </select>
        </div>
        <div class="flex flex-col w-full">
            <label class="text-sm" for="priority">Prioridade:</label>
            <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
                    id="priority" name="priority" required>
                <option value="">Selecione</option>
                @for(var priority : priorities)
                <option value="${priority.name()}">${priority.getDescription()}</option>
                @endfor
            </select>
        </div>
    </div>
</div>

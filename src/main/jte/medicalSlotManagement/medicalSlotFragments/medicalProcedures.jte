@import br.com.tecsus.sigaubs.entities.MedicalProcedure
@import java.util.List
@param List<MedicalProcedure> procedures
@param Boolean isConsultation = false
<div id="selectMedicalProcedure" class="flex flex-col w-full">
    <label class="text-sm" for="medicalProcedure">Procedimento:</label>
    <select class="text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200"
            id="medicalProcedure" name="medicalProcedure.id"
            disabled="${procedures.isEmpty()}" required>
        <option value="">Selecione</option>
        @for(var procedure : procedures)
        <option value="${procedure.getId()}" selected="${isConsultation}">${procedure.getDescription()}</option>
        @endfor
    </select>
</div>

(function () {
    "use strict";

    const values = (items, field) => (items || []).map((item) => item[field]);
    const chart = (selector, options) => {
        const element = document.querySelector(selector);
        if (element) new ApexCharts(element, options).render();
    };

    document.addEventListener("DOMContentLoaded", async () => {
        if (!document.querySelector("#dailyChart") || typeof ApexCharts === "undefined") return;
        let data;
        try {
            const response = await fetch("/dashboard/data", {headers: {"Accept": "application/json"}});
            if (!response.ok) throw new Error("dashboard");
            data = await response.json();
        } catch (_) {
            document.querySelectorAll("#dailyChart, #monthlyChart, #priorityDonut, #procedureDonut, #bottleneckChart, #occupancyChart")
                .forEach((element) => element.textContent = "Não foi possível carregar os indicadores.");
            return;
        }

        const dailyLabels = values(data.dailyAppointments, "day");
        const dailyValues = values(data.dailyAppointments, "total");
        const monthlyOpenLabels = values(data.monthlyOpenAppointments, "month");
        const monthlyOpen = values(data.monthlyOpenAppointments, "total");
        const monthlyContemplations = values(data.monthlyContemplations, "total");
        const monthlyLabels = monthlyOpenLabels.length >= (data.monthlyContemplations || []).length
            ? monthlyOpenLabels : values(data.monthlyContemplations, "month");

        chart("#dailyChart", {
            series: [{name: "Marcações", data: dailyValues}],
            chart: {height: 256, type: "line", zoom: {enabled: false}, toolbar: {show: false}},
            dataLabels: {enabled: false}, stroke: {curve: "smooth", width: 3},
            xaxis: {categories: dailyLabels}, colors: ["#3B82F6"],
            noData: {text: "Sem dados para o período"}
        });
        chart("#monthlyChart", {
            series: [
                {name: "Marcações em Aberto", data: monthlyOpen},
                {name: "Contemplados", data: monthlyContemplations}
            ],
            chart: {height: 256, type: "line", zoom: {enabled: false}, toolbar: {show: false}},
            dataLabels: {enabled: false}, stroke: {curve: "smooth", width: 3},
            colors: ["#EF4444", "#10B981"], xaxis: {categories: monthlyLabels},
            legend: {position: "top"}, noData: {text: "Sem dados para o período"}
        });

        const priorities = values(data.priorityDistribution, "priority");
        const priorityColorMap = {"Urgência": "#EF4444", "Retorno": "#3B82F6", "Prioritário": "#F59E0B", "Eletivo": "#10B981", "Administrativo": "#8B5CF6"};
        chart("#priorityDonut", {
            series: values(data.priorityDistribution, "total"), labels: priorities,
            chart: {type: "donut", height: 300},
            colors: priorities.map((label) => priorityColorMap[label] || "#6B7280"),
            legend: {position: "bottom"}, noData: {text: "Sem dados"}
        });

        const procedures = values(data.procedureTypeDistribution, "procedureType");
        const procedureColorMap = {"Consulta": "#3B82F6", "Exame": "#F59E0B", "Cirurgia": "#EF4444"};
        chart("#procedureDonut", {
            series: values(data.procedureTypeDistribution, "total"), labels: procedures,
            chart: {type: "donut", height: 300},
            colors: procedures.map((label) => procedureColorMap[label] || "#6B7280"),
            legend: {position: "bottom"}, noData: {text: "Sem dados"}
        });

        const bottleneckLabels = (data.topBottlenecks || []).map((item) => `${item.specialty} - ${item.procedure}`);
        chart("#bottleneckChart", {
            series: [{name: "Na fila", data: values(data.topBottlenecks, "total")}],
            chart: {type: "bar", height: Math.max(300, bottleneckLabels.length * 40), toolbar: {show: false}},
            plotOptions: {bar: {horizontal: true, borderRadius: 4, barHeight: "60%"}},
            xaxis: {categories: bottleneckLabels}, colors: ["#EF4444"],
            noData: {text: "Sem gargalos identificados"}
        });

        const totalSlots = values(data.slotOccupancy, "totalSlots");
        const consumed = values(data.slotOccupancy, "consumedSlots");
        chart("#occupancyChart", {
            series: [
                {name: "Consumidas", data: consumed},
                {name: "Disponíveis", data: totalSlots.map((total, index) => total - consumed[index])}
            ],
            chart: {type: "bar", height: Math.max(300, (data.slotOccupancy || []).length * 45), stacked: true, toolbar: {show: false}},
            plotOptions: {bar: {horizontal: true, borderRadius: 4, barHeight: "55%"}},
            xaxis: {categories: values(data.slotOccupancy, "ubsName")},
            colors: ["#3B82F6", "#D1FAE5"], legend: {position: "top"},
            noData: {text: "Sem vagas cadastradas neste mês"}
        });
    });
})();

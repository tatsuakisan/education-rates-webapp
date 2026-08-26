let allData = [];
let ratesChart;
let studentsChart;
let gradPctChart;

const startYearEl = document.getElementById("startYear");
const endYearEl = document.getElementById("endYear");
const applyBtn = document.getElementById("applyFilter");
const tableBody = document.querySelector("#ratesTable tbody");

const avgAttendanceEl = document.getElementById("avgAttendance");
const avgGraduationEl = document.getElementById("avgGraduation");
const avgGradPctEl    = document.getElementById("avgGradPct");

// Dark mode toggle
const themeToggleBtn = document.getElementById("themeToggle");
const savedTheme = localStorage.getItem("theme");
if (savedTheme === "dark") {
  document.body.classList.add("dark");
  themeToggleBtn.textContent = "☀️ Light Mode";
}

themeToggleBtn.addEventListener("click", () => {
  document.body.classList.toggle("dark");
  const isDark = document.body.classList.contains("dark");
  themeToggleBtn.textContent = isDark ? "☀️ Light Mode" : "🌙 Dark Mode";
  localStorage.setItem("theme", isDark ? "dark" : "light");
  // Re-render charts with new theme colors
  const data = filteredData();
  if (data.length) {
    renderRatesChart(data);
    renderGradPctChart(data);
    renderStudentsChart(data);
  }
});

async function loadData() {
  const res = await fetch("/api/rates");
  allData = await res.json();
  initYearFilters(allData);
  render(filteredData());
}

function initYearFilters(data) {
  startYearEl.innerHTML = "";
  endYearEl.innerHTML = "";

  const years = data.map(d => d.year);
  const min = Math.min(...years);
  const max = Math.max(...years);

  for (let y = min; y <= max; y++) {
    startYearEl.add(new Option(y, y));
    endYearEl.add(new Option(y, y));
  }

  startYearEl.value = min;
  endYearEl.value = max;
}

function filteredData() {
  const start = Number(startYearEl.value);
  const end = Number(endYearEl.value);
  return allData.filter(d => d.year >= start && d.year <= end);
}

function gradPct(d) {
  if (!d.population || d.population === 0) return 0;
  return (d.graduationRate / d.population) * 100;
}

function privateGraduates(d) {
  return Math.max(0, Math.round(d.graduationRate) - d.publicSchoolCount);
}

function renderStats(data) {
  const avgPopulation =
    data.reduce((sum, item) => sum + item.population, 0) / data.length;
  const avgGraduates =
    data.reduce((sum, item) => sum + item.graduationRate, 0) / data.length;
  const avgPct =
    data.reduce((sum, item) => sum + gradPct(item), 0) / data.length;

  avgAttendanceEl.textContent = Math.round(avgPopulation).toLocaleString();
  avgGraduationEl.textContent = Math.round(avgGraduates).toLocaleString();
  avgGradPctEl.textContent    = `${avgPct.toFixed(1)}%`;
}

function renderTable(data) {
  tableBody.innerHTML = "";
  data.forEach(d => {
    const pct = gradPct(d);
    const privGrad = privateGraduates(d);
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${d.year}</td>
      <td>${Math.round(d.population).toLocaleString()}</td>
      <td>${Math.round(d.graduationRate).toLocaleString()}</td>
      <td>${pct.toFixed(1)}%</td>
      <td>${d.publicSchoolCount.toLocaleString()}</td>
      <td>${privGrad.toLocaleString()}</td>
    `;
    tableBody.appendChild(row);
  });
}

function chartDefaults() {
  const isDark = document.body.classList.contains("dark");
  return {
    gridColor: isDark ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)",
    tickColor: isDark ? "#9ca3af" : "#6b7280",
    legendColor: isDark ? "#f9fafb" : "#1f2937"
  };
}

function renderRatesChart(data) {
  const labels     = data.map(d => d.year);
  const population = data.map(d => d.population);
  const graduates  = data.map(d => d.graduationRate);
  const { gridColor, tickColor, legendColor } = chartDefaults();

  const ctx = document.getElementById("ratesChart");
  if (ratesChart) ratesChart.destroy();

  ratesChart = new Chart(ctx, {
    type: "line",
    data: {
      labels,
      datasets: [
        {
          label: "Population (17yr)",
          data: population,
          borderColor: "#2563eb",
          backgroundColor: "rgba(37, 99, 235, 0.15)",
          borderWidth: 2,
          tension: 0.25
        },
        {
          label: "Number of Graduates",
          data: graduates,
          borderColor: "#059669",
          backgroundColor: "rgba(5, 150, 105, 0.15)",
          borderWidth: 2,
          tension: 0.25
        }
      ]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { position: "top", labels: { color: legendColor } }
      },
      scales: {
        x: { ticks: { color: tickColor }, grid: { color: gridColor } },
        y: { beginAtZero: true, ticks: { color: tickColor }, grid: { color: gridColor }, title: { display: true, text: "People", color: tickColor } }
      }
    }
  });
}

function renderGradPctChart(data) {
  const labels = data.map(d => d.year);
  const pcts   = data.map(d => gradPct(d));
  const { gridColor, tickColor, legendColor } = chartDefaults();

  const ctx = document.getElementById("gradPctChart");
  if (gradPctChart) gradPctChart.destroy();

  gradPctChart = new Chart(ctx, {
    type: "line",
    data: {
      labels,
      datasets: [
        {
          label: "Graduation %",
          data: pcts,
          borderColor: "#7c3aed",
          backgroundColor: "rgba(124, 58, 237, 0.15)",
          borderWidth: 2,
          tension: 0.25
        }
      ]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { position: "top", labels: { color: legendColor } }
      },
      scales: {
        x: { ticks: { color: tickColor }, grid: { color: gridColor } },
        y: { beginAtZero: true, ticks: { color: tickColor, callback: value => `${value.toFixed(1)}%` }, grid: { color: gridColor }, title: { display: true, text: "Graduation %", color: tickColor } }
      }
    }
  });
}

function renderStudentsChart(data) {
  const labels          = data.map(d => d.year);
  const publicStudents  = data.map(d => d.publicSchoolCount);
  const privateStudents = data.map(d => privateGraduates(d));
  const { gridColor, tickColor, legendColor } = chartDefaults();

  const ctx = document.getElementById("studentsChart");
  if (studentsChart) studentsChart.destroy();

  studentsChart = new Chart(ctx, {
    type: "bar",
    data: {
      labels,
      datasets: [
        {
          label: "Public School Graduates",
          data: publicStudents,
          backgroundColor: "#2563eb"
        },
        {
          label: "Private School Graduates",
          data: privateStudents,
          backgroundColor: "#f59e0b"
        }
      ]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: true, labels: { color: legendColor } }
      },
      scales: {
        x: { ticks: { color: tickColor }, grid: { color: gridColor } },
        y: { beginAtZero: true, ticks: { color: tickColor }, grid: { color: gridColor }, title: { display: true, text: "Graduates", color: tickColor } }
      }
    }
  });
}

function render(data) {
  if (!data.length) {
    tableBody.innerHTML = "";
    avgAttendanceEl.textContent = "--";
    avgGraduationEl.textContent = "--";
    avgGradPctEl.textContent    = "--";
    if (ratesChart)   ratesChart.destroy();
    if (gradPctChart) gradPctChart.destroy();
    if (studentsChart) studentsChart.destroy();
    return;
  }

  renderStats(data);
  renderRatesChart(data);
  renderGradPctChart(data);
  renderStudentsChart(data);
  renderTable(data);
}

applyBtn.addEventListener("click", () => {
  if (Number(startYearEl.value) > Number(endYearEl.value)) {
    alert("Start year cannot be greater than end year.");
    return;
  }
  render(filteredData());
});

loadData();
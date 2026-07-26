# Repository Guidelines

## Project Structure & Module Organization

SIGA-UBS is a Java 21 Spring Boot application with server-rendered JTE views and Tailwind CSS assets. Java code lives under `src/main/java/br/com/tecsus/sigaubs`, organized by responsibility: `controllers`, `services`, `repositories`, `entities`, `dtos`, `enums`, `security`, `jobs`, and `utils`. JTE templates are in `src/main/jte`, grouped by feature modules such as `patientManagement`, `queueManagement`, and `medicalSlotManagement`; shared layout fragments are in `src/main/jte/fragments`. Static CSS, JS, and images are in `src/main/resources/static`. Application profiles, SQL schema, and seed data are in `src/main/resources`.

## Build, Test, and Development Commands

- `./mvnw spring-boot:run` starts the Spring Boot app locally.
- `./mvnw clean package` builds the application JAR.
- `./mvnw test` runs the Maven test suite.
- `npm run watch:postcss` rebuilds Tailwind CSS during development.
- `npm run build:postcss` creates the minified CSS output in `target/classes/static/css/style.build.tailwind.css`.

During UI work, run the Spring Boot app and `npm run watch:postcss` together so template and CSS changes are visible.

## Coding Style & Naming Conventions

Follow the existing Spring MVC layered style: controllers handle web flow, services hold business logic and transactions, repositories isolate persistence, and entities model JPA state. Use Java package names in lowercase and class names by role, for example `PatientController`, `AppointmentService`, and `MedicalSlotRepository`. Keep feature templates under their matching JTE module and name reusable partials with clear suffixes such as `_datatable.jte`, `_form.jte`, or `_info.jte`. Code, comments, and commits are primarily in Brazilian Portuguese; keep that convention unless integrating with external APIs.

## Frontend Design & Component Guidelines

The frontend is a server-rendered administrative interface built with JTE templates, Tailwind utility classes, HTMX partial updates, Alpine.js for small local state, jQuery for existing inline interactions, SweetAlert2 confirmations, ApexCharts on the dashboard, and Material Symbols for icons. Tailwind has no custom theme tokens today; the visual system is encoded directly in repeated utility class compositions inside the JTE files. Global CSS is intentionally small: `styles.css` imports Manrope from Google Fonts and applies it globally, and `style.tailwind.css` only adds Tailwind layers plus a minimum height rule for read-only `span.bg-slate-100` values inside flex-column containers.

The active visual language is a restrained admin UI based on slate neutrals with blue as the primary action accent. Common colors are `bg-slate-900`/`text-slate-200` for dark section headers, sidebar, modal headers, and primary buttons; `bg-slate-300` or `bg-slate-100` for page backgrounds; `bg-white` for panels, cards, modals, and tables; `bg-slate-200` for table headers and inactive tabs; `bg-slate-300` for selected tabs; `text-slate-600`/`text-slate-700` for body and label text; `focus:ring-blue-500` and `hover:bg-blue-500` for primary focus/action feedback. Semantic accents are used sparingly: green for success/confirmation, red for destructive actions/errors, amber for warnings or waiting states, purple/rose/green/amber/blue as dashboard card border accents.

Most authenticated pages share the same layout skeleton: `body` as a full-height flex container with `h-dvh`, the shared dark collapsible sidebar, and a `main` column containing the shared header, optional loading/dialog fragments, a scrollable `section`, and feature panels centered inside `div.flex.flex-col.mx-4.justify-start.items-center`. Use this structure for new internal screens. The sidebar is `bg-slate-900`, width-collapsible via Alpine (`w-64`/`w-16`), and uses `Material Symbols` plus text labels. `main.js` highlights the current sidebar item with `bg-blue-500`; preserve matching `href` values when adding navigation entries. Login, error, and expired-session pages intentionally use a standalone centered card over `bg-slate-900`, with logos and footer, not the authenticated sidebar layout.

CRUD and workflow screens are organized as accordion-like panels implemented with a hidden checkbox plus Tailwind `peer`/`peer-checked` classes. The standard panel container is `w-full bg-white rounded-lg mt-4` or `my-4`; the header label is dark slate with roughly `text-slate-200 tracking-wide block bg-slate-900 py-4 px-4 rounded-lg cursor-pointer border-2 border-slate-600 hover:bg-slate-800 hover:border-blue-500`, and usually includes a Material Symbol with `mr-4`. Panel bodies use `peer-checked:max-h-full` or `peer-checked:max-h-[2000px]`, `max-h-0 overflow-hidden`, and `transition-max-height duration-500 ease-in-out`. New panels should copy the active v2/slate pattern rather than the older gray/indigo queue v1 templates.

Forms use responsive Tailwind grids, usually `grid grid-flow-row grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2 gap-x-4 py-4 px-4 w-full`, with wider modules using `lg:grid-cols-5`, `lg:grid-cols-6`, or a dense 12-column grid when needed. Field wrappers are generally `flex flex-col w-full`, with explicit `col-span-*` only where the content needs more horizontal space. Labels are `text-sm`; inputs, selects, and textareas consistently use `text-sm w-full rounded-lg border border-stroke py-2 pl-4 mt-1 outline-none focus:ring-2 hover:ring-gray-400 hover:ring-2 hover:border-white focus:border-white focus:ring-blue-500 transition duration-200`. Disabled fields typically add `bg-slate-100 cursor-not-allowed`.

Buttons are compact and utilitarian. Primary actions use `text-sm inline-block rounded-lg bg-slate-900 py-2.5 px-6/px-8 text-white cursor-pointer active:bg-blue-400 hover:bg-blue-500 transition duration-300`. Search and clear actions often use `inline-flex text-sm items-center rounded-lg bg-slate-900 p-2 text-white` with a Material Symbol (`search`, `close`). Destructive buttons keep the same base but hover red (`hover:bg-red-500`) or use explicit `bg-red-600 hover:bg-red-500`; confirm/success actions use `bg-green-600 hover:bg-green-500`. Icon-only table actions are `inline-flex p-2 rounded-lg hover:bg-slate-200 cursor-pointer transition duration-300`, with icon hover colors indicating intent (`hover:text-blue-500` for edit/details, `hover:text-red-400`/`hover:text-red-500` for remove/cancel).

Tables are dense, horizontally scrollable, and should be wrapped in `overflow-x-auto` or `overflow-auto`. Active datatables use `table class="min-w-full bg-white"` or `table-auto w-full text-base`; headers use `thead class="font-light text-sm text-slate-600 text-left tracking-wide"` and `tr.bg-slate-200`, with rounded first/last header cells (`rounded-l-lg`, `rounded-r-lg`). Body rows use `overflow-hidden border-slate-200 text-slate-600 w-full hover:bg-slate-50 active:bg-slate-200`; cells use `border-b text-sm` with `py-2 px-4` or `pl-4`. Pagination is usually a `flex flex-1 mt-4 justify-between items-center bg-white` footer, page-size select, and Material Symbol arrow buttons with `bg-slate-100 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed`. Prefer HTMX `hx-get`, `hx-target`, `hx-include`, and `hx-swap="outerHTML"` for pagination and table refreshes when nearby code already uses HTMX.

Read-only detail sections use small grids and label/value pairs. The standard value style is `px-4 py-2 text-sm text-slate-700 bg-slate-100 rounded-lg`; labels are `font-semibold text-sm text-slate-700` or muted `text-sm text-slate-500`. Keep these blocks compact and scannable. Avoid replacing them with large marketing-style cards.

Tabs appear in contemplation and modal flows. The active tab uses `bg-slate-300`, inactive tabs use `bg-slate-200`, and tabs are usually `w-1/3 py-2 text-sm text-center rounded-t-lg hover:bg-slate-300 focus:outline-none transition duration-200`. Existing tab switching is handled by small inline JavaScript functions that toggle `hidden` and swap `bg-slate-200`/`bg-slate-300`; keep the behavior consistent unless refactoring the whole interaction.

Modals use fixed full-screen overlays and white content with dark slate headers. The active patient/appointment modal uses a darkened backdrop, `relative bg-white rounded-lg shadow-2xl`, header `bg-slate-900 rounded-t border-2 border-slate-600`, and a compact icon close button. Smaller observation modals follow the same header pattern in newer screens, though some older fragments still have gray/dark-mode remnants. Tooltips are usually implemented with `relative group` wrappers and an absolutely positioned `hidden group-hover:block bg-slate-800 text-white text-sm rounded px-2 py-1 whitespace-nowrap` label.

Feedback is split between server-rendered alerts and SweetAlert2 dialogs. Shared dialogs render fixed top-right success/error alerts with green/red headers and Material Symbols. Some HTMX fragments render inline alerts with `bg-emerald-100 text-emerald-700 border-emerald-300` or `bg-red-100 text-red-700 border-red-300`. Destructive or sensitive confirmations, such as saving medical slots, confirmation/cancelation of contemplation, and password validation, use `Swal.fire` with Portuguese labels.

Dashboard screens are more card/chart oriented but still follow the same slate/white language. Summary cards use `bg-white rounded-lg shadow-md p-4 border-l-4` with colored left borders. Chart panels use `bg-white p-4 rounded-lg shadow-md`, titles `text-lg font-semibold text-slate-700`, and ApexCharts colors mapped to existing semantic colors (`#3B82F6`, `#10B981`, `#EF4444`, `#F59E0B`, `#8B5CF6`). Do not introduce a separate dashboard palette.

Autocomplete/dropdown fragments are white bordered absolute panels under their inputs: `absolute bg-white border border-gray-300 w-full mt-1 rounded shadow-lg z-10`, with list items `p-2 hover:bg-gray-200` or `hover:bg-slate-100 cursor-pointer`. Existing search boxes usually close dropdowns with jQuery document-click handlers and update partials with HTMX delayed input triggers.

When changing frontend code, prefer preserving the current server-rendered JTE plus Tailwind utility style over introducing a component framework or large custom CSS layer. If repeated compositions are extracted later, do it deliberately and across a real family of components. Avoid copying the deprecated `queue_management.jte` and `queue_tabs_*` v1 visual style (`bg-gray-700`, indigo buttons, uppercase table headers); active work should follow `queue_management_v2.jte` and the other current management screens.

## Testing Guidelines

Place Java tests under `src/test/java` using JUnit through `spring-boot-starter-test`; mirror the main package structure and name classes `*Test` or `*Tests`. There is no explicit coverage threshold configured. Prefer focused service and repository tests for business rules, especially queue ordering, appointment status changes, and contemplation scheduling. Use `./mvnw test -Dtest=ClassName` for a single class.

## Commit & Pull Request Guidelines

Recent history uses concise Portuguese conventional-style messages such as `fix: corrigir ...` and `refactor: renomear ...`. Keep commits scoped and use prefixes like `fix:`, `feat:`, `refactor:`, or `chore:`. Pull requests should describe the change, list test commands run, link related issues when available, and include screenshots or short recordings for visible JTE/Tailwind UI changes.

## Security & Configuration Tips

Do not commit real credentials or certificates beyond existing development fixtures. Prefer profile-specific properties in `src/main/resources/application-*.properties`, and keep local database secrets in environment variables when possible.

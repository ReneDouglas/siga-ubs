@import br.com.tecsus.sigaubs.security.StaticResourceHelper
@param String title
@param StaticResourceHelper res
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
    <link href="${res.url("/css/styles.css")}" rel="stylesheet"/>
    <link href="${res.url("/css/style.build.tailwind.css")}" rel="stylesheet"/>
    <link href="${res.url("/css/sweetalert2.min.css")}" rel="stylesheet">
    <script src="${res.url("/js/jquery-3.7.1.min.js")}"></script>
    <script src="${res.url("/js/htmx.min.js")}"></script>
    <script src="${res.url("/js/main.js")}"></script>
    <script src="${res.url("/js/sweetalert2.all.min.js")}"></script>
    <script src="https://cdn.jsdelivr.net/npm/alpinejs@3.12.0/dist/cdn.min.js" defer></script>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,1,1" />
    <title>${title}</title>
</head>

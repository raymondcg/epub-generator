<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title>${bookName?html}</title>
		<link rel="stylesheet" href="css/style.css" type="text/css" />
    </head>
    <body>
        <#list chapters as chapter>
        <section class="body-rw Chapter-rw" epub:type="bodymatter chapter">
            <header>
                <p id="chapter_${chapter.number}" class="chapter">${chapter.number}: ${chapter.title?html}</p>
            </header>
		    <p>${chapter.body?html?replace("\n", "</p>\n\t\t<p>")}</p>
        </section>
        </#list>
    </body>
</html>

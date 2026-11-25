<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" xml:lang="en" unique-identifier="pub-id" prefix="cc: http://creativecommons.org/ns#">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
        <dc:title id="title">${title?html}</dc:title>
        <meta refines="#title" property="title-type">main</meta>
        <dc:creator id="creator">${author}</dc:creator>
        <meta refines="#creator" property="file-as">${author}</meta>
        <meta refines="#creator" property="role" scheme="marc:relators">aut</meta>
        <dc:identifier id="pub-id">gitlab.com.webnerdbot.${bookId}</dc:identifier>
        <dc:language>en-US</dc:language>
        <meta property="dcterms:modified">${modifieddate}</meta>
        <dc:publisher>MySelf</dc:publisher>
		<dc:subject>${subject}</dc:subject>
		<dc:date>${crawldate}</dc:date>
		<dc:description>${description}</dc:description>
    </metadata>

    <manifest>
        <item id="toc" properties="nav" href="toc.xhtml" media-type="application/xhtml+xml"/>
        <item id="copyright" href="copyright.xhtml" media-type="application/xhtml+xml"/>
        <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml"/>
        <item id="cover-image" properties="cover-image" href="images/${coverFileName?html}" media-type="${coverMediaType?html}"/>
        <item id="style" href="css/style.css" media-type="text/css"/>
        <item id="xpreface_001" href="preface_001.xhtml" media-type="application/xhtml+xml"/>
        <item id="content" href="content.xhtml" media-type="application/xhtml+xml"/>
    </manifest>
    
    <spine>
        <itemref idref="cover" linear="no"/>
        <itemref idref="toc" linear="yes"/>
        <itemref idref="xpreface_001" linear="yes"/>
        <itemref idref="content" linear="yes"/>
        <itemref idref="copyright" linear="yes"/>
    </spine>
    
</package>
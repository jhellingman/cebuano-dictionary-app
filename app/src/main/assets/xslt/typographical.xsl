<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">

    <xsl:output 
        method="html" 
        indent="yes"
        encoding="ISO-8859-1"/>

    <xsl:preserve-space elements="*"/>

    <xsl:key name="id" match="*[@id]" use="@id"/>

    <xsl:param name="fontSize" select="'20'"/>
    <xsl:param name="expandAbbreviations" select="'false'"/>
    <xsl:param name="useMetric" select="'false'"/>
    <xsl:param name="useNightMode" select="'false'"/>

    <xsl:template match="dictionary">
        <html>
            <head>
                <title>Dictionary of Cebuano Visayan</title>

                <style type="text/css">

                    body { font-size: <xsl:value-of select="$fontSize"/>pt; }
                    .form { font-size: 110%; }
                    .eg { font-size: 90%; color: #606060; }
                    .eg i { font-style: italic }
                    .pos { font-size: 110%; color: red; }
                    .num { font-weight: bold; color: blue; }
                    .itype { }
                    .bio { font-style: italic; font-weight: bold; }
                    .tr {  }
                    .xr {  }
                    .gramGrp {  }
                    .exp { color: #606060; }
                    .rm { font-style: normal; font-weight: normal; }

                    <xsl:if test="$useNightMode = 'true'">
                        body { background-color: #272727; color: #FFFFFF;}
                        .pos { color: #ff6666; }
                        .num { color: #00ccff; }
                        a { color: #00ccff; }
                        .exp, .eg { color: #c0c0c0; }
                    </xsl:if>

                </style>
            </head>
            <body>
                <xsl:apply-templates/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="entry">
        <span class="entry">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="hom[pos='v']">
        <span class="hom verb">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="hom[pos='n']">
        <span class="hom noun">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="hom[pos='a']">
        <span class="hom adjective">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="hom">
        <span class="hom">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="sense">
        <span class="sense">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="form">
        <b class="form">
            <xsl:apply-templates/>
        </b>
    </xsl:template>

    <xsl:template match="w">
        <span id="{@id}">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="number">
        <b class="num">
            <xsl:apply-templates/>
        </b>
    </xsl:template>

    <xsl:template match="gramGrp">
        <span class="gramGrp">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="pos">
        <i class="pos">
            <xsl:apply-templates/>
        </i>
    </xsl:template>

    <xsl:template match="pos[parent::xr]">
        <i><xsl:apply-templates/></i>
    </xsl:template>

    <xsl:template match="number[parent::xr]">
        <b><xsl:apply-templates/></b>
    </xsl:template>

    <xsl:template match="number[parent::form]">
        <sub><xsl:apply-templates/></sub>
    </xsl:template>

    <xsl:template match="number[parent::hw]">
        <sub><xsl:apply-templates/></sub>
    </xsl:template>

    <xsl:template match="bio">
        <span class="bio">
            <a>
                <xsl:attribute name="href">https://www.google.com/search?q=<xsl:value-of select="translate(., ' ', '+')"/></xsl:attribute>
                <xsl:apply-templates/>
            </a>
        </span>
    </xsl:template>

    <xsl:template match="note">
        <span class="note">[* <xsl:apply-templates/> *]</span>
    </xsl:template>

    <xsl:template match="tr">
        <span class="tr">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="xr">
        <span class="xr">
            <xsl:choose>
                <xsl:when test="@target">
                    <a class="search">
                        <xsl:attribute name="href">
                            <!-- Remove x and q encoding present in files. -->
                            <xsl:text>search:</xsl:text><xsl:value-of select="substring-after(translate(@target, 'xq1234567890', ''), '#')"/>
                        </xsl:attribute>
                        <xsl:apply-templates />
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
            </xsl:choose>
        </span>
    </xsl:template>

    <xsl:template match="eg">
        <span class="eg">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="q">
        <i><xsl:apply-templates/></i>
    </xsl:template>

    <xsl:template match="hw">
        <b><xsl:apply-templates/></b>
    </xsl:template>

    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="pb"/>

    <xsl:template match="itype">
        <span class="itype">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="bx | formx">
        <b>
            <xsl:apply-templates/>
        </b>
    </xsl:template>

    <xsl:template match="r">
        <span class="rm">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <xsl:template match="ix">
        <i>
            <xsl:apply-templates/>
        </i>
    </xsl:template>

    <xsl:template match="abbr">
        <xsl:choose>
            <xsl:when test="$expandAbbreviations = 'true'">
                <xsl:value-of select="@expan" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="measure">
        <xsl:choose>
            <xsl:when test="$useMetric = 'true'">
                <xsl:value-of select="@reg" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- Discard unwanted stuff -->
    <xsl:template match="TEI.2|text|body|trans|div1|sc|corr|head|foreign|back|divGen|sic">
        <xsl:apply-templates/>
    </xsl:template>

</xsl:stylesheet>

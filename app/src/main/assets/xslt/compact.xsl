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
                    .rm { font-style: normal; font-weight: normal; }

                    <xsl:if test="$useNightMode = 'true'">
                        body { background-color: #272727; color: #FFFFFF; }
                        a { color: #00ccff; }
                    </xsl:if>
                </style>
            </head>
            <body>
                <xsl:apply-templates/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="form | number | hw | bx | formx | b">
        <b><xsl:apply-templates/></b>
    </xsl:template>

    <xsl:template match="pos | q | ix | i">
        <i><xsl:apply-templates/></i>
    </xsl:template>

    <xsl:template match="number[parent::form] | number[parent::hw] | sub">
        <sub><xsl:apply-templates/></sub>
    </xsl:template>

    <xsl:template match="bio">
        <b><i><xsl:apply-templates/></i></b>
    </xsl:template>

    <xsl:template match="r">
        <span class="rm">
            <xsl:apply-templates/>
        </span>
    </xsl:template>

    <!--  Drop examples and verb codes in the compact presentation. -->
    <xsl:template match="eg | itype | pb"/>

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
    
</xsl:stylesheet>

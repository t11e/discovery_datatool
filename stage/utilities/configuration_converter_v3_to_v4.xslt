<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://transparensee.com/schema/datatool-config-4"
  xmlns:c3="http://transparensee.com/schema/datatool-config-3"
  xmlns:c4="http://transparensee.com/schema/datatool-config-4"
  exclude-result-prefixes="c3 c4 xsi"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="
     http://transparensee.com/schema/datatool-config-3
     http://transparensee.com/schema/datatool-config-3.xsd
     http://transparensee.com/schema/datatool-config-4
     http://transparensee.com/schema/datatool-config-4.xsd
     ">

  <xsl:output
    method="xml"
    version="1.0"
    encoding="utf-8"
    omit-xml-declaration="no"
    cdata-section-elements="query retrieveSql updateSql createSql"
    indent="yes"/>

  <xsl:template match="c3:config">
    <config
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation=
        "http://transparensee.com/schema/datatool-config-4
         http://transparensee.com/schema/datatool-config-4.xsd">
      <xsl:apply-templates/>
    </config>
  </xsl:template>

  <xsl:template match="c3:sqlPublisher">
     <sqlPublisher>
        <xsl:apply-templates select="@*"/>
        <xsl:if test="./c3:action[@filter='snapshot']">
          <snapshot>
            <xsl:apply-templates select="./c3:action[(@filter='snapshot' or @filter='any') and @type='create']"/>
            <xsl:apply-templates select="./c3:action[(@filter='snapshot' or @filter='any') and @type='delete']"/>
          </snapshot>
        </xsl:if>
        <xsl:if test="./c3:action[@filter='delta']">
          <delta>
            <xsl:apply-templates select="./c3:action[(@filter='delta' or @filter='any') and @type='create']"/>
            <xsl:apply-templates select="./c3:action[(@filter='delta' or @filter='any') and @type='delete']"/>
          </delta>
        </xsl:if>
     </sqlPublisher>
  </xsl:template>

  <xsl:template match="c3:action[@type='create']">
    <set-item>
       <xsl:apply-templates select="@idColumn"/>
       <xsl:apply-templates select="@jsonColumns"/>
       <xsl:apply-templates select="child::node()"/>
    </set-item>
  </xsl:template>

  <xsl:template match="c3:action[@type='delete']">
    <remove-item>
       <xsl:apply-templates select="@idColumn"/>
       <xsl:apply-templates select="child::node()"/>
    </remove-item>
  </xsl:template>

  <xsl:template match="c3:*">
    <xsl:element name="{local-name()}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="@*|node()">
     <xsl:copy>
        <xsl:apply-templates select="@*|node()"/>
     </xsl:copy>
  </xsl:template>

</xsl:stylesheet>

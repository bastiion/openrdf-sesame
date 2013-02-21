<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE rdf:RDF [
   <!ENTITY xsd  "http://www.w3.org/2001/XMLSchema#" >
 ]>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$repository-create.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<script src="../../scripts/create.js" type="text/javascript"></script>
		<script src="../../scripts/create-federate.js" type="text/javascript">
		</script>
		<form action="create" method="post">
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$repository-type.label" />
						</th>
						<td>
							<select id="type" name="type">
								<option value="federate">Federation Store</option>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-id.label" />
						</th>
						<td>
							<input type="text" id="id" name="Local repository ID" size="16"
								value="fed" />
						</td>
						<td>
							<span id="recurse-message" class="ERROR" style="display: none;">
								Federation ID
								may not match an existing ID.
							</span>
						</td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-title.label" />
						</th>
						<td>
							<input type="text" id="title" name="Repository title" size="48"
								value="Federation" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$federation-members.label" />
						</th>
						<td>
							<xsl:apply-templates select="*" />
						</td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$distinct.label" />
						</th>
						<td>
							<input type="checkbox" name="distinct" value="true" />
						</td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$read-only.label" />
						</th>
						<td>
							<input type="checkbox" name="readonly" value="true"
								checked="true" />
						</td>
					</tr>
					<tr>
						<td></td>
						<td>
							<input type="button" value="{$cancel.label}" style="float:right"
								href="repositories" onclick="document.location.href=this.getAttribute('href')" />
							<input id="create" type="submit" value="{$create.label}" />
							<span class="error" id="create-feedback">Select at least two federation
								members.
							</span>
						</td>
					</tr>
				</tbody>
			</table>
		</form>
	</xsl:template>

	<xsl:template match="sparql:binding[@name='id']">
		<input type="checkbox" class="memberID" name="memberID" value="{sparql:literal}" />
		<xsl:value-of select="sparql:literal" />
		<br />
	</xsl:template>
	<xsl:template match="sparql:binding[@name='description']" />
	<xsl:template match="sparql:binding[@name='location']" />

</xsl:stylesheet>

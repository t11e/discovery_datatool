Use the configuration converter stylesheet to convert an XML configuration file from schema version 3 to 4.

Since there are some structural changes to the XML configuration file, this helps ease the transition.
Note that the Data Tool will continue to be backwards compatible with the earlier versions of the XML schema.

Here is an example of how you would use this stylesheet with xsltproc and xmllint to convert IntegrationTest-3.xml:

$ xsltproc configuration_converter_v3_to_v4.xslt IntegrationTest-3.xml | xmllint --format - > IntegrationTest-4.xml


package com.t11e.discovery.datatool;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ChangesetController
{
  @RequestMapping("/ws/publisher")
  public void publish(
    final HttpServletResponse response)
    throws XMLStreamException, IOException
  {
    response.setContentType("text/xml; charset=utf-8");
    final XMLStreamWriter writer =
      StaxUtil.newOutputFactory().createXMLStreamWriter(response.getOutputStream());
    writer.writeStartDocument();
    writer.writeStartElement("changeset");
    writer.writeEndElement();
    writer.writeEndDocument();
    writer.flush();
  }
}
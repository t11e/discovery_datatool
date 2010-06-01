package com.t11e.discovery.datatool;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpUtil
{
  /**
   * Returns a HTTP request's response output stream. If the request supports
   * compression, the appropriate content encoding is set, and the returned
   * stream will transparently compress the output.
   */
  public static OutputStream getCompressedResponseStream(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    // Careful here not to get the stream before setting headers
    OutputStream stream = null;
    final Set<String> acceptedEncodings = getAcceptEncodingNames(request);
    if (acceptedEncodings.contains("gzip"))
    {
      response.setHeader("Content-Encoding", "gzip");
      stream = new GZIPOutputStream(response.getOutputStream());
    }
    if (stream == null)
    {
      stream = response.getOutputStream();
    }
    return stream;
  }

  /**
   * Returns accepted encoding names.
   */
  public static Set<String> getAcceptEncodingNames(final HttpServletRequest request)
  {
    final Set<String> result = new HashSet<String>();
    final String acceptEncodingHeader = request.getHeader("Accept-Encoding");
    if (acceptEncodingHeader != null)
    {
      for (final StringTokenizer tk = new StringTokenizer(acceptEncodingHeader, ", ",
        false); tk.hasMoreTokens();)
      {
        String token = tk.nextToken();
        final int i = token.indexOf(";");
        if (i >= 0)
        {
          token = token.substring(0, i).trim();
        }
        result.add(token);
      }
    }
    return result;
  }
}

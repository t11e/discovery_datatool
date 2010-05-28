package com.t11e.discovery.datatool;

import static com.t11e.discovery.datatool.CollectionsFactory.makeList;
import static com.t11e.discovery.datatool.CollectionsFactory.makeMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.EOFException;
import java.util.Date;

import org.junit.Test;

public class JsonUtilTest
{
  @Test
  public void testEncodeNull()
  {
    assertEquals("null", JsonUtil.encode(null));
  }

  @Test
  public void testEncodeTrue()
  {
    assertEquals("true", JsonUtil.encode(Boolean.TRUE));
  }

  @Test
  public void testEncodeFalse()
  {
    assertEquals("false", JsonUtil.encode(Boolean.FALSE));
  }

  @Test
  public void testEncodeInteger()
  {
    assertEquals("0", JsonUtil.encode(new Integer(0)));
    assertEquals("1", JsonUtil.encode(new Integer(1)));
    assertEquals("-1", JsonUtil.encode(new Integer(-1)));
    assertEquals("2", JsonUtil.encode(new Integer(2)));
    assertEquals("-2147483647",
      JsonUtil.encode(new Integer(Integer.MIN_VALUE + 1)));
    assertEquals("-2147483648",
      JsonUtil.encode(new Integer(Integer.MIN_VALUE)));
    assertEquals("2147483646",
      JsonUtil.encode(new Integer(Integer.MAX_VALUE - 1)));
    assertEquals("2147483647",
      JsonUtil.encode(new Integer(Integer.MAX_VALUE)));
  }

  @Test
  public void testEncodeLong()
  {
    assertEquals("0", JsonUtil.encode(new Long(0)));
    assertEquals("1", JsonUtil.encode(new Long(1)));
    assertEquals("-1", JsonUtil.encode(new Long(-1)));
    assertEquals("2", JsonUtil.encode(new Long(2)));
    assertEquals("-9223372036854775807",
      JsonUtil.encode(new Long(Long.MIN_VALUE + 1)));
    assertEquals("-9223372036854775808",
      JsonUtil.encode(new Long(Long.MIN_VALUE)));
    assertEquals("9223372036854775806",
      JsonUtil.encode(new Long(Long.MAX_VALUE - 1)));
    assertEquals("9223372036854775807",
      JsonUtil.encode(new Long(Long.MAX_VALUE)));
  }

  @Test
  public void testEncodeDouble()
  {
    assertEquals("0.0", JsonUtil.encode(new Double(0)));
    assertEquals("1.0", JsonUtil.encode(new Double(1)));
    assertEquals("-1.0", JsonUtil.encode(new Double(-1)));
    assertEquals("2.0", JsonUtil.encode(new Double(2)));
    assertEquals("3.141592653589793", // TODO Missing 23846
      JsonUtil.encode(new Double(Math.PI)));
    assertEquals("2.718281828459045", // TODO Missing 2354
      JsonUtil.encode(new Double(Math.E)));
  }

  // TODO Verify all doubles can be passed around without loss of
  // precision.
  //@Test
  public void testEncodeDoubleExtremes()
  {
    assertEquals("4.9E-324",
      JsonUtil.encode(new Double(Double.MIN_VALUE)));
    assertEquals("1.7976931348623157E308",
      JsonUtil.encode(new Double(Double.MAX_VALUE)));
    assertEquals("-4.9E-324",
      JsonUtil.encode(new Double(-Double.MIN_VALUE)));
    assertEquals("-1.7976931348623157E308",
      JsonUtil.encode(new Double(-Double.MAX_VALUE)));
  }

  @Test
  public void testEncodeString()
  {
    assertEquals("\"\"", JsonUtil.encode(""));
    assertEquals("\"foo\"", JsonUtil.encode("foo"));
    assertEquals("\"foo bar\"", JsonUtil.encode("foo bar"));
    assertEquals("\" Foo Bar \"", JsonUtil.encode(" Foo Bar "));
    assertEquals("\"'\"", JsonUtil.encode("'"));
    assertEquals("\"\\\"\"", JsonUtil.encode("\""));
    assertEquals("\"\\\\\"", JsonUtil.encode("\\"));
    assertEquals("\"/\"", JsonUtil.encode("/"));
    assertEquals("\"\\b\"", JsonUtil.encode("\b"));
    assertEquals("\"\\f\"", JsonUtil.encode("\f"));
    assertEquals("\"\\n\"", JsonUtil.encode("\n"));
    assertEquals("\"\\r\"", JsonUtil.encode("\r"));
    assertEquals("\"\\t\"", JsonUtil.encode("\t"));
    assertEquals("\"\\u000F\"", JsonUtil.encode("\u000F"));
    assertEquals("\"\\u0019\"", JsonUtil.encode("\u0019"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testEncodeCollection()
  {
    assertEquals("[]", JsonUtil.encode(makeList()));
    assertEquals("[\"a\"]", JsonUtil.encode(makeList("a")));
    assertEquals("[\"a\",\"b\"]", JsonUtil.encode(makeList("a", "b")));
    assertEquals("[\"a\",true,1]", JsonUtil.encode(
      makeList("a", Boolean.TRUE, new Integer(1))));
    assertEquals("[[\"a\"]]", JsonUtil.encode(
      makeList(makeList("a"))));
    assertEquals("[[\"a\"],[\"b\"]]", JsonUtil.encode(
      makeList(makeList("a"),makeList("b"))));
  }

  @Test
  public void testEncodeArray()
  {
    assertEquals("[]", JsonUtil.encode(new String[0]));
    assertEquals("[\"a\"]", JsonUtil.encode(new String[] {"a"}));
    assertEquals("[\"a\",\"b\"]", JsonUtil.encode(new String[] {"a", "b"}));
    assertEquals("[\"a\",true,1]", JsonUtil.encode(
      new Object[] {"a", Boolean.TRUE, new Integer(1)}));
    assertEquals("[[\"a\"]]", JsonUtil.encode(
      new String[][] {{"a"}}));
    assertEquals("[[\"a\"],[\"b\"]]", JsonUtil.encode(
      new String[][] {{"a"}, {"b"}}));
  }

  @Test
  public void testEncodeMap()
  {
    assertEquals("{}", JsonUtil.encode(makeMap()));
    assertEquals("{\"a\":\"b\"}", JsonUtil.encode(makeMap("a", "b")));
    assertEquals("{\"a\":1,\"b\":true}", JsonUtil.encode(
      makeMap(
        "a", new Integer(1),
        "b", Boolean.TRUE)));
    assertEquals("{\"a\":{\"b\":\"c\"}}", JsonUtil.encode(
      makeMap("a", makeMap("b", "c"))));
    assertEquals("{\"a\":{\"b\":\"c\"},\"d\":{\"e\":\"f\"}}", JsonUtil.encode(
      makeMap(
        "a", makeMap("b", "c"),
        "d", makeMap("e", "f"))));
  }

  @Test
  public void testEncodeDate()
  {
    try
    {
      JsonUtil.encode(new Date());
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e)
    {
      // Good
    }
  }

  @Test
  public void testDecodeNull()
  {
    assertNull(JsonUtil.decode("null"));
  }

  @Test
  public void testDecodeEmpty()
  {
    try
    {
      JsonUtil.decode("");
      fail("Should throw EOFException");
    }
    catch(final RuntimeException e)
    {
      // success expected
      assertEquals(
        "Should throw EOFException",
        EOFException.class.getName(), e.getCause().getClass().getName());
    }
  }

  @Test
  public void testDecodeTrue()
  {
    assertEquals(Boolean.TRUE, JsonUtil.decode("true"));
  }

  @Test
  public void testDecodeFalse()
  {
    assertEquals(Boolean.FALSE, JsonUtil.decode("false"));
  }

  @Test
  public void testDecodeInteger()
  {
    assertEquals(new Integer(0), JsonUtil.decode("0"));
    assertEquals(new Integer(1), JsonUtil.decode("1"));
    assertEquals(new Integer(-1), JsonUtil.decode("-1"));
    assertEquals(new Integer(2), JsonUtil.decode("2"));
  }

  @Test
  public void testDecodeDouble()
  {
    assertEquals(new Double(0), JsonUtil.decode("0.0"));
    assertEquals(new Double(1), JsonUtil.decode("1.0"));
    assertEquals(new Double(-1), JsonUtil.decode("-1.0"));
    assertEquals(new Double(2), JsonUtil.decode("2.0"));
    assertEquals(new Double(Math.PI),
      JsonUtil.decode("3.14159265358979323846"));
    assertEquals(new Double(Math.E),
      JsonUtil.decode("2.7182818284590452354"));
    assertEquals(new Double(Double.MAX_VALUE),
      JsonUtil.decode("1.7976931348623157e+308"));
    assertEquals(new Double(Double.MIN_VALUE),
      JsonUtil.decode("4.9e-324"));
    assertEquals(new Double(-Double.MAX_VALUE),
      JsonUtil.decode("-1.7976931348623157e+308"));
    assertEquals(new Double(-Double.MIN_VALUE),
      JsonUtil.decode("-4.9e-324"));
  }

  @Test
  public void testDecodeLong()
  {
    assertEquals(new Long(Long.MIN_VALUE + 1),
      JsonUtil.decode("-9223372036854775807"));
    assertEquals(new Long(Long.MIN_VALUE),
      JsonUtil.decode("-9223372036854775808"));
    assertEquals(new Long(Long.MAX_VALUE - 1),
      JsonUtil.decode("9223372036854775806"));
    assertEquals(new Long(Long.MAX_VALUE),
      JsonUtil.decode("9223372036854775807"));
  }

  @Test
  public void testDecodeString()
  {
    assertEquals("", JsonUtil.decode("\"\""));
    assertEquals("foo", JsonUtil.decode("\"foo\""));
    assertEquals("foo bar", JsonUtil.decode("\"foo bar\""));
    assertEquals(" Foo Bar ", JsonUtil.decode("\" Foo Bar \""));
    assertEquals("'", JsonUtil.decode("\"'\""));
    assertEquals("\"", JsonUtil.decode("\"\\\"\""));
    assertEquals("\\", JsonUtil.decode("\"\\\\\""));
    assertEquals("/", JsonUtil.decode("\"/\""));
    assertEquals("\b", JsonUtil.decode("\"\\b\""));
    assertEquals("\f", JsonUtil.decode("\"\\f\""));
    assertEquals("\n", JsonUtil.decode("\"\\n\""));
    assertEquals("\r", JsonUtil.decode("\"\\r\""));
    assertEquals("\t", JsonUtil.decode("\"\\t\""));
    assertEquals("\u000F", JsonUtil.decode("\"\\u000F\""));
    assertEquals("\u000F", JsonUtil.decode("\"\\u000f\""));
    assertEquals("\u0019", JsonUtil.decode("\"\\u0019\""));
    assertEquals("\u1234", JsonUtil.decode("\"\\u1234\""));
    assertEquals("\u4321", JsonUtil.decode("\"\\u4321\""));
    assertEquals("\uabcd", JsonUtil.decode("\"\\uabcd\""));
    assertEquals("\udcba", JsonUtil.decode("\"\\udcba\""));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDecodeCollection()
  {
    assertEquals(makeList(), JsonUtil.decode("[]"));
    assertEquals(makeList("a"), JsonUtil.decode("[\"a\"]"));
    assertEquals(makeList("a", "b"), JsonUtil.decode("[\"a\",\"b\"]"));
    assertEquals(makeList("a", Boolean.TRUE, new Integer(1)),
      JsonUtil.decode("[\"a\",true,1]"));
    assertEquals(makeList(makeList("a")),
      JsonUtil.decode("[[\"a\"]]"));
    assertEquals(makeList(makeList("a"),makeList("b")),
      JsonUtil.decode("[[\"a\"],[\"b\"]]"));
  }

  @Test
  public void testDecodeMap()
  {
    assertEquals(makeMap(), JsonUtil.decode("{}"));
    assertEquals(makeMap("a", "b"), JsonUtil.decode("{\"a\":\"b\"}"));
    assertEquals(
      makeMap(
        "a", new Integer(1),
        "b", Boolean.TRUE),
      JsonUtil.decode("{\"a\":1,\"b\":true}"));
    assertEquals(
      makeMap("a", makeMap("b", "c")),
      JsonUtil.decode("{\"a\":{\"b\":\"c\"}}"));
    assertEquals(
      makeMap(
        "a", makeMap("b", "c"),
        "d", makeMap("e", "f")),
      JsonUtil.decode("{\"a\":{\"b\":\"c\"},\"d\":{\"e\":\"f\"}}"));
  }

  @Test
  public void testDecodeDate()
  {
    // Dates are no longer special, this should come back as a string.
    assertEquals("20071225T08:30:00",
      JsonUtil.decode("\"20071225T08:30:00\""));
  }

  @Test
  public void testDecodeLeadingWhitespace()
  {
    assertNull(JsonUtil.decode(" null"));
    assertEquals(Boolean.TRUE, JsonUtil.decode(" true"));
    assertEquals(Boolean.FALSE, JsonUtil.decode(" false"));
    assertEquals(new Integer(0), JsonUtil.decode(" 0"));
    assertEquals(new Double(2.4), JsonUtil.decode(" 2.4"));
    assertEquals("", JsonUtil.decode(" \"\""));
    assertEquals(makeList(), JsonUtil.decode(" []"));
    assertEquals(makeMap(), JsonUtil.decode(" {}"));
  }
}

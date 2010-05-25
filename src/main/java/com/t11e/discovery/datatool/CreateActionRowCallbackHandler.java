package com.t11e.discovery.datatool;

public class CreateActionRowCallbackHandler
  //extends AbstractJdbcProcessor
{
//  private String m_columns;
//  private String m_idPrefix;
//  private String m_idSuffix;
//  // Set<String>
//  private Set m_jsonColumns;
//  private boolean m_lowerCaseColumnNames;
//
//  public CreateActionRowCallbackHandler(final String table, final String idColumn,
//    final String[] lastModifiedColumns, final String columns, final String[] jsonColumns,
//    final boolean lowerCaseColumnNames)
//  {
//    super(table, idColumn, lastModifiedColumns);
//    m_columns = columns;
//    m_jsonColumns = new HashSet();
//    if (jsonColumns != null)
//    {
//      for (final String jsonColumn : jsonColumns)
//      {
//        if (jsonColumn != null)
//        {
//          m_jsonColumns.add(jsonColumn.toLowerCase());
//        }
//      }
//    }
//    m_lowerCaseColumnNames = lowerCaseColumnNames;
//  }
//
//  protected PreparedStatement createStatement(final Connection connection, final Date startDate,
//    final Date endDate, final boolean isSnapshot)
//    throws SQLException
//  {
//    return createStatementWithDateConstraint(connection,
//      "SELECT " + m_columns + " FROM " + m_table,
//      startDate, endDate);
//  }
//
//  protected void processRows(final ResultSet rs, final IItemChangesetListener listener,
//    final IProgressMonitor progress)
//    throws SQLException
//  {
//    m_logger.info("Reading updated records from " + m_table);
//    final Map properties = new HashMap();
//    int count = 0;
//    final ResultSetMetaData md = rs.getMetaData();
//    final IColumnProcessor[] columnProcessors = getColumnProcessors(md);
//    while (rs.next())
//    {
//      final String id;
//      {
//        final StringBuffer buffer = new StringBuffer();
//        if (m_idPrefix != null)
//        {
//          buffer.append(m_idPrefix);
//        }
//        buffer.append(rs.getString(m_idColumn));
//        if (m_idSuffix != null)
//        {
//          buffer.append(m_idSuffix);
//        }
//        id = buffer.toString();
//      }
//      for (int column = 1; column < columnProcessors.length; column++)
//      {
//        final IColumnProcessor columnProcessor = columnProcessors[column];
//        if (columnProcessor != null)
//        {
//          String name = md.getColumnName(column);
//          if (m_lowerCaseColumnNames)
//          {
//            name = name.toLowerCase();
//          }
//          final Object value = columnProcessor.processColumn(rs, column);
//          if (value != null)
//          {
//            properties.put(name, value);
//          }
//        }
//      }
//      listener.onSetItem(new ObjectId(id), properties);
//      properties.clear();
//      count++;
//      if (count % 100 == 0 && count > 0)
//      {
//        progress.worked(100);
//      }
//    }
//    if (count % 100 != 0 && count > 0)
//    {
//      progress.worked(count % 100);
//    }
//    m_logger.info("Read " + count + " updated records from " + m_table);
//  }
//
//  public void setIdPrefix(final String prefix)
//  {
//    m_idPrefix = prefix;
//  }
//
//  public void setIdSuffix(final String suffix)
//  {
//    m_idSuffix = suffix;
//  }
//
//  private IColumnProcessor[] getColumnProcessors(final ResultSetMetaData md)
//    throws SQLException
//  {
//    final IColumnProcessor[] output = new IColumnProcessor[md.getColumnCount() + 1];
//    for (int column = 1; column < output.length; column++)
//    {
//      output[column] = getColumnProcessor(md, column);
//    }
//    return output;
//  }
//
//  private IColumnProcessor getColumnProcessor(final ResultSetMetaData md, final int column)
//    throws SQLException
//  {
//    IColumnProcessor output;
//    switch (md.getColumnType(column))
//    {
//      case java.sql.Types.BIT:
//      case java.sql.Types.BOOLEAN:
//        output = BooleanColumnProcessor.INSTANCE;
//        break;
//      case java.sql.Types.TINYINT:
//      case java.sql.Types.SMALLINT:
//      case java.sql.Types.INTEGER:
//      case java.sql.Types.BIGINT:
//      case java.sql.Types.FLOAT:
//      case java.sql.Types.REAL:
//      case java.sql.Types.DOUBLE:
//      case java.sql.Types.NUMERIC:
//      case java.sql.Types.DECIMAL:
//        output = StringColumnProcessor.INSTANCE;
//        break;
//      case java.sql.Types.CHAR:
//      case java.sql.Types.VARCHAR:
//      case java.sql.Types.LONGVARCHAR:
//      case java.sql.Types.CLOB:
//      {
//        final String columnName = md.getColumnName(column);
//        if (columnName != null && m_jsonColumns.contains(columnName.toLowerCase()))
//        {
//          output = JsonColumnProcessor.INSTANCE;
//        }
//        else
//        {
//          output = StringColumnProcessor.INSTANCE;
//        }
//        break;
//      }
//      case java.sql.Types.DATE:
//        output = DateColumnProcessor.INSTANCE;
//        break;
//      case java.sql.Types.TIME:
//        output = TimeColumnProcessor.INSTANCE;
//        break;
//      case java.sql.Types.TIMESTAMP:
//        output = TimestampColumnProcessor.INSTANCE;
//        break;
//      case java.sql.Types.BINARY:
//      case java.sql.Types.VARBINARY:
//      case java.sql.Types.LONGVARBINARY:
//      case java.sql.Types.NULL:
//      case java.sql.Types.OTHER:
//      case java.sql.Types.JAVA_OBJECT:
//      case java.sql.Types.DISTINCT:
//      case java.sql.Types.STRUCT:
//      case java.sql.Types.ARRAY:
//      case java.sql.Types.BLOB:
//      case java.sql.Types.REF:
//      case java.sql.Types.DATALINK:
//      default:
//        output = null;
//        break;
//    }
//    return output;
//  }
}

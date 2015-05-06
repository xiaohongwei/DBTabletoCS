

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class JsonData_table
{
  private static String localString;
  private static String dBIp;
  private static String dBName = "Y3_Dict";
  private static String outPath = "F:/JYWorkspace/JYGame/src/code/model";

  static
  {
    localString = "192.168.0.12";
    dBIp = localString;
  }

  private static Connection getMySQLConnection()
    throws Exception
  {
    Class.forName("com.mysql.jdbc.Driver");
    Connection conn = DriverManager.getConnection("jdbc:mysql://" + dBIp + ":3306/" + dBName, "root", "root");
    return conn;
  }

  private static String getClassColumnName(String name)
  {
    String string = name;
    int ix = name.indexOf("_");
    if (ix >= 0)
    {
      String[] strs = name.split("_");
      String className = "";
      String[] as;
      int j = (as = strs).length;
      for (int i = 0; i < j; i++)
      {
        String s = as[i];
        className = className + s;
      }

      string = className;
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }

  private static Hashtable getTableInfo() throws Exception
  {
    Hashtable tableMap = new Hashtable();
    String sql = "SELECT table_name ,TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES  WHERE table_schema = '" + dBName + "'";
    Statement statement = getMySQLConnection().createStatement();
    for (ResultSet rs = statement.executeQuery(sql); rs.next(); tableMap.put(rs.getString("table_name"), rs.getString("TABLE_COMMENT")));
    return tableMap;
  }

  private static void getASBean(String tableName, String tableCommon) throws Exception
  {
    StringBuffer sBuffer = new StringBuffer();
    String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT  FROM INFORMATION_SCHEMA.COLUMNS  WHERE table_name = '" + tableName + "' " + " AND table_schema = '" + dBName + "' ";
   


    sBuffer.append("\t{\n");
    Statement statement = getMySQLConnection().createStatement();

    String query = "CREATE TABLE " + tableName +"(";
    
    
    for (ResultSet rs = statement.executeQuery(sql); rs.next(); )
    {
      String colName = rs.getString("COLUMN_NAME");
      //String colType = rs.getString("DATA_TYPE");
      //String colComm = rs.getString("COLUMN_COMMENT");

      query+=(""+colName+" varchar(255) default NULL," );
      
      //System.out.println(colName);

    }

    query = query.substring(0, query.length()-1);
    query+=(");");
    //sBuffer.append("}\n");

    System.out.println(query);
    //getFile(sBuffer.toString(), getClassColumnName(tableName), "as");
  }


  public static void main(String[] args) throws Exception
  {
    try
    {
      Hashtable tableMap = getTableInfo();
      Iterator tableNames = tableMap.keySet().iterator();

      while (tableNames.hasNext())
      {
        String table = (String)tableNames.next();
        if (table.indexOf("Log_") != -1)
          continue;
        getASBean(table, (String)tableMap.get(table));
       // System.out.println("生成表:" + table);
      }

      System.out.println("全部生成完成!");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
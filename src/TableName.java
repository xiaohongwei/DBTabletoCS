

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

public class TableName
{
  private static String dBIp;
  private static String dBName = "";
  private static String outPath = "";

  private static Connection getMySQLConnection()
    throws Exception
  {
    Class.forName("com.mysql.jdbc.Driver");
    Connection conn = DriverManager.getConnection("jdbc:mysql://" + dBIp + ":3306/" + dBName, "root", "root");
    return conn;
  }

  private static Hashtable getTableInfo()
    throws Exception
  {
    Hashtable tableMap = new Hashtable();
    String sql = "SELECT table_name ,TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES  WHERE table_schema = '" + dBName + "'";
    Statement statement = getMySQLConnection().createStatement();
    for (ResultSet rs = statement.executeQuery(sql); rs.next(); tableMap.put(rs.getString("table_name"), rs.getString("TABLE_COMMENT")));
    return tableMap;
  }



  public static void main(String[] args)
    throws Exception
  {
    Properties prop = new Properties();

    String confiFile = System.getProperty("user.dir") + "/dictData.properties";
    System.out.println("设置文件：" + confiFile);
    InputStream in = new BufferedInputStream(new FileInputStream(confiFile));
    try {
      prop.load(in);
      dBIp = prop.getProperty("dBIp").trim();
      dBName = prop.getProperty("dBName").trim();
      outPath = prop.getProperty("outPath").trim();

      System.out.println("生成路径：" + outPath);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try
    {
      Hashtable tableMap = getTableInfo();
      Iterator tableNames = tableMap.keySet().iterator();

      while (tableNames.hasNext())
      {
        String table = (String)tableNames.next();
        if (table.indexOf("Log_") != -1)
          continue;
        
        try
        {
          //System.out.println("生成表:" + table);
          ///System.out.println("name_Class.Add(\""+table.replaceAll("_", "")+"\",typeof("+table.replaceAll("_", "")+"));");
          System.out.println("name_tableName.Add(\""+table.replaceAll("_", "")+"\",\""+table+"\");");
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }

      System.out.println("全部生成完成!");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

}
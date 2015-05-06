

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
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;

public class JsonData
{
  private static String dBIp = "";
  private static String dBName = "";
  private static String outPath = "";
  private static JSONArray jsonArr = new JSONArray();
  
  private static Properties prop = new Properties();

  public static void main(String[] args)throws Exception
  {
    String confiFile = System.getProperty("user.dir") + "/jsonData.properties";
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

    crtJosn(1);
    jsonArr = new JSONArray();
    crtJosn(2);
  }
  
  
  
  
  public static void crtJosn(int type)
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
        
        String no_tables = prop.getProperty("no_tables");
        if (no_tables.indexOf( ","+table.replaceAll("_", "")+",")!=-1 )
            continue;
        
        try
        {
          if ((isUseTable(table)==true && type==1)  || (isUseTable(table)==false && type==2))
          {
	          System.out.println("生成表:" + table);
	          getJson(table, (String)tableMap.get(table));
          }
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }

      }

      geneFile(jsonArr.toString(), outPath, "dictData"+type+".dict");

      System.out.println("全部生成完成!");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    File file = new File(outPath + "dictData"+type+".dat");
    if ((file.isFile()) && (file.exists())) {
      file.delete();
    }
    System.out.println("删除文件：" + outPath + "dictData"+type+".dat");
  }
  
  
  
  
  public static boolean isUseTable(String name)
  {
    String[] str = prop.getProperty("tables").split(",");
    for (int i = 0; i < str.length; i++)
    {
      if (str[i].replaceAll("_", "").equals(name.replaceAll("_", ""))) {
        return true;
      }
    }
    return false;
  }

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

  private static void getJson(String tableName, String tableCommon)
    throws Exception
  {
    String sqlstString = "select * from " + tableName;
    Statement statement2 = getMySQLConnection().createStatement();
    ResultSet rs2 = statement2.executeQuery(sqlstString);
    ResultSetMetaData data = rs2.getMetaData();

    while (rs2.next())
    {
      Map mp = new HashMap();

      for (int i = 1; i <= data.getColumnCount(); i++)
      {
        String columnName = data.getColumnName(i);

        String columnClassName = data.getColumnClassName(i);

        Object value = rs2.getString(columnName);
        if (columnClassName == "java.lang.Long")
        {
          if (value == null) {
            value = "0";
          }
          value = Long.parseLong(value+"");
        }
        else if (columnClassName == "java.lang.Double")
        {
          if (value == null) {
            value = "0";
          }
          value = Double.parseDouble(value+"");
        }
        else if (columnClassName == "java.lang.Integer")
        {
          if (value == null) {
            value = "0";
          }
          value = Integer.parseInt(value+"");
        }

        if (columnName.equals("className"))
          mp.put(columnName, tableName.replaceAll("_", ""));
        else {
          mp.put(columnName, value);
        }

      }

      jsonArr.put(mp);
    }
  }

  private static void geneFile(String content, String filePath, String fileName)
    throws Exception
  {
    BufferedWriter bw = null;
    try
    {
      createDir(filePath);
      bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(createFile(filePath + fileName)), "utf-8"));
      bw.write(content);
      bw.flush();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    bw.close();
  }

  private static void createDir(String path)
  {
    File dict = new File(path);
    if (!dict.exists())
      dict.mkdirs();
  }

  private static File createFile(String filePath)
  {
    File file = new File(filePath);
    if (!file.exists())
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    return file;
  }
}


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class Inst
{
  private static String localString;
  private static String dBIp;
  private static String dBName = "Y2_T_Inst";
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

  private static Hashtable getTableInfo()
    throws Exception
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
    if (tableName.contains("Dict"))
      sBuffer.append("package code.model.dictData\n");
    else if (tableName.contains("Inst")) {
      sBuffer.append("package code.model.instData\n");
    }
    sBuffer.append("{\n\n");
    sBuffer.append("\timport code.model.DataObj;\n\n");
    sBuffer.append("\t/** " + tableCommon + " */\n");
    sBuffer.append("\tpublic class " + getClassColumnName(tableName) + " extends DataObj\n");
    sBuffer.append("\t{\n");
    Statement statement = getMySQLConnection().createStatement();

    for (ResultSet rs = statement.executeQuery(sql); rs.next(); )
    {
      String colName = rs.getString("COLUMN_NAME");
      String colType = rs.getString("DATA_TYPE");
      String colComm = rs.getString("COLUMN_COMMENT");
      if ((colName.equals("id")) || (colName.equals("className")))
        continue;
      sBuffer.append("\t\t/** " + colComm + " */\n");
      sBuffer.append("\t\tpublic var " + colName + ":" + getCSType(colType) + " = " + getCSInitValue(colType) + ";\n\n");
    }

    sBuffer.append("\t\tpublic function " + getClassColumnName(tableName) + "(id:String){\n");
    sBuffer.append("\t\t\tsuper(id);\n");
    sBuffer.append("\t\t\tthis.className=\"" + getClassColumnName(tableName) + "\";\n");
    sBuffer.append("\t\t}\n");
    sBuffer.append("\t}\n");
    sBuffer.append("}\n");

    getFile(sBuffer.toString(), getClassColumnName(tableName), "as");
  }

  private static void getFile(String content, String fileName, String type) throws Exception
  {
    fileName = fileName + ".as";
    if (fileName.contains("Dict"))
    {
      String filePath = outPath + "/dictData/";
      geneFile(content, filePath, fileName);
    }
    else if (fileName.contains("Inst"))
    {
      String filePath = outPath + "/instData/";
      geneFile(content, filePath, fileName);
    }
    else
    {
      String filePath = outPath + "/constData/";
      geneFile(content, filePath, fileName);
    }
  }

  private static void geneFile(String content, String filePath, String fileName) throws Exception
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

  private static String getCSType(String type)
  {
    String dataType = "";
    if (type.equals("int")) {
      dataType = "int";
    }
    else if (type.equals("bigint")) {
      dataType = "Number";
    }
    else if (type.equals("varchar")) {
      dataType = "String";
    }
    else if (type.equals("float")) {
      dataType = "Number";
    }
    else if (type.equals("double")) {
      dataType = "Number";
    }
    else if (type.equals("smallint")) {
      dataType = "int";
    }
    else if (type.equals("decimal")) {
      dataType = "Number";
    }
    else if (type.equals("numeric")) {
      dataType = "Number";
    }
    else if (type.equals("timestamp")) {
      dataType = "Date";
    }
    else if (type.equals("datetime")) {
      dataType = "Date";
    }
    else if (type.equals("blob")) {
      dataType = "String";
    }
    else if (type.equals("text"))
      dataType = "String";
    else
      dataType = "Object";
    return dataType;
  }

  private static String getCSInitValue(String type)
  {
    String dataType = "";
    if (type.equals("int")) {
      dataType = "0";
    }
    else if (type.equals("bigint")) {
      dataType = "0";
    }
    else if (type.equals("varchar")) {
      dataType = "null";
    }
    else if (type.equals("float")) {
      dataType = "0";
    }
    else if (type.equals("double")) {
      dataType = "0";
    }
    else if (type.equals("smallint")) {
      dataType = "0";
    }
    else if (type.equals("decimal")) {
      dataType = "0";
    }
    else if (type.equals("numeric")) {
      dataType = "0";
    }
    else if (type.equals("timestamp")) {
      dataType = "null";
    }
    else if (type.equals("datetime")) {
      dataType = "null";
    }
    else if (type.equals("blob")) {
      dataType = "null";
    }
    else if (type.equals("text"))
      dataType = "null";
    else
      dataType = "null";
    return dataType;
  }

  private static void delAll(File f)
    throws IOException
  {
    if (!f.exists())
      return;
    boolean rslt = true;
    if (!(rslt = f.delete()))
    {
      File[] subs = f.listFiles();
      for (int i = 0; i <= subs.length - 1; i++)
      {
        if (subs[i].isDirectory())
          delAll(subs[i]);
        rslt = subs[i].delete();
      }

      rslt = f.delete();
      if (!rslt)
        throw new IOException("无法删除:" + f.getName());
    }
  }

  public static void main(String[] args)
    throws Exception
  {
    try
    {
      delAll(new File(outPath + "/instData"));

      Hashtable tableMap = getTableInfo();
      Iterator tableNames = tableMap.keySet().iterator();

      while (tableNames.hasNext())
      {
        String table = (String)tableNames.next();
        if (table.indexOf("Log_") != -1)
          continue;
        getASBean(table, (String)tableMap.get(table));
        System.out.println("生成表:" + table);
      }

      System.out.println("全部生成完成!");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
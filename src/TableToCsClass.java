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

public class TableToCsClass
{
	/** 数据库IP地址*/
	private static String dBIp;
	/** 数据库名称*/
	private static String dBName = "";
	/** 用户名*/
	private static String uname = "";
	/** 密码*/
	private static String pwd = "";
	/** 输出路径*/
	private static String outPath = "";
	/** Cs文件名称类型*/
	private static String outCsType = "";
	/** 不生成的表*/
	private static String no_tables = "";

	/**
	 * 运行入口
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		String confiFile = System.getProperty("user.dir") + "/dictData.properties";
		
		Properties prop = new Properties();
		InputStream in = new BufferedInputStream(new FileInputStream(confiFile));
		prop.load(in);

		dBIp = prop.getProperty("dBIp").trim();
		dBName = prop.getProperty("dBName").trim();
		uname = prop.getProperty("uname").trim();
		pwd = prop.getProperty("pwd").trim();
		outPath = prop.getProperty("outPath").trim();
		outCsType = prop.getProperty("outCsType").trim();
		no_tables = prop.getProperty("no_tables").trim();

		System.out.println("设置文件：" + confiFile);
		System.out.println("生成路径：" + outPath);

		delAll(new File(outPath + "/dictData"));
		delAll(new File(outPath + "/constData"));
		delAll(new File(outPath + "/instData"));

		Hashtable<?, ?> tableMap = getTableInfo();
		Iterator<?> tableNames = tableMap.keySet().iterator();

		while (tableNames.hasNext())
		{
			String table = (String) tableNames.next();
			if (table.indexOf("Log_") != -1) // 忽略日志
				continue;

			if (!isNotUseTable(table))
				continue;

			System.out.println("生成表:" + table);
			try
			{
				getASBean(table, (String) tableMap.get(table));
				getStatic_file(table, (String) tableMap.get(table));
			} catch (Exception e){ e.printStackTrace();}
			
		}

		System.out.println("全部生成完成!");
		Runtime.getRuntime().exec("cmd.exe /k explorer.exe "+outPath.replaceAll("\\/", "\\\\"));
	}

	/**
	 * 获取连接
	 * @return
	 * @throws Exception
	 */
	private static Connection getMySQLConnection() throws Exception
	{
		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection("jdbc:mysql://" + dBIp + ":3306/" + dBName, uname, pwd);
		return conn;
	}

	/**
	 * 文件名
	 * @param name
	 * @return
	 */
	private static String getClassColumnName(String name)
	{
		// 1为文件名跟表名一样  2标准命名规则（DictUserInfo）
		if(outCsType.equals("1"))
			return name;
		else
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
	}

	/**
	 * 获取表名
	 * @return
	 * @throws Exception
	 */
	private static Hashtable<String, String> getTableInfo() throws Exception
	{
		Hashtable<String, String> tableMap = new Hashtable<String, String>();
		String sql = "SELECT table_name ,TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES  WHERE table_schema = '" + dBName + "'";
		Statement statement = getMySQLConnection().createStatement();
		ResultSet rs = statement.executeQuery(sql);

		while (rs.next())
		{
			tableMap.put(rs.getString("table_name"), rs.getString("TABLE_COMMENT"));
		}
		return tableMap;
	}

	
	/**
	 * 生成静态类
	 * @param tableName
	 * @param tableCommon
	 * @throws Exception
	 */
	private static void getStatic_file(String tableName, String tableCommon) throws Exception
	{
		StringBuffer sBuffer = new StringBuffer();
		String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT  FROM INFORMATION_SCHEMA.COLUMNS  WHERE table_name = '" + tableName + "' " + " AND table_schema = '" + dBName + "' ";
		String newTableName = "";
		if (!tableName.contains("_"))
			return;
		String[] arr = tableName.split("_");
		StringBuilder sb = new StringBuilder("Static_");

		for (int i = 1; i < arr.length; i++)
		{
			sb.append(arr[i]);
		}

		newTableName = sb.toString();

		Statement statement = getMySQLConnection().createStatement();
		for (ResultSet rs = statement.executeQuery(sql); rs.next();)
		{
			String colName = rs.getString("COLUMN_NAME");
			if ((tableName.equals("Dict_Cn")) || (tableName.equals("Dict_GameConfig")) || (!colName.equals("sname")))
				continue;
			sBuffer.append("\tpublic  class " + newTableName + "{\n\n");
			String sqlstString = "select * from " + tableName;
			Statement statement2 = getMySQLConnection().createStatement();
			for (ResultSet rs2 = statement2.executeQuery(sqlstString); rs2.next();)
			{
				String id = rs2.getString("id");
				String name = rs2.getString("name");
				String sname = rs2.getString("sname");

				if (tableName.equals("Dict_Constant"))
				{
					id = rs2.getString("value");
				}

				if ((name == null) || (sname == null) || (name.equals("")) || (sname.equals("")))
					throw new Exception("coulmn is not null when creating StaticClass:" + newTableName);

				sBuffer.append("\t\t/** " + name + " */\n");
				sBuffer.append("\t\tpublic  const int " + sname + " = " + id + ";\n\n");
			}

			sBuffer.append("\t}\n");
			getFile(sBuffer.toString(), newTableName, "staticAs");
		}
	}

	/**
	 * 生成类文件
	 * @param tableName
	 * @param tableCommon
	 * @throws Exception
	 */
	private static void getASBean(String tableName, String tableCommon) throws Exception
	{
		StringBuffer sBuffer = new StringBuffer();
		String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT  FROM INFORMATION_SCHEMA.COLUMNS  WHERE table_name = '" + tableName + "' " + " AND table_schema = '" + dBName + "' ";

		sBuffer.append("using System;\n[Serializable]\n\tpublic class " + getClassColumnName(tableName) + ":DataObj\n");
		sBuffer.append("\t{\n");
		Statement statement = getMySQLConnection().createStatement();
		for (ResultSet rs = statement.executeQuery(sql); rs.next();)
		{
			String colName = rs.getString("COLUMN_NAME");
			String colType = rs.getString("DATA_TYPE");
			String colComm = rs.getString("COLUMN_COMMENT");
			if ((colName.equals("id")) || (colName.equals("className")))
				continue;
			sBuffer.append("\t\t/** " + colComm + " */\n");

			sBuffer.append("\t\tpublic " + getCSType(colType) + " " + colName + " {get;set;} " + "\n\n");
		}

		sBuffer.append("\t\tpublic " + getClassColumnName(tableName) + "(){\n");
		sBuffer.append("\t}\n");
		sBuffer.append("}\n");
		getFile(sBuffer.toString(), getClassColumnName(tableName), "cs");
	}

	/**
	 * 获得文件名
	 * @param content
	 * @param fileName
	 * @param type
	 * @throws Exception
	 */
	private static void getFile(String content, String fileName, String type) throws Exception
	{
		fileName = fileName + ".cs";
		if (fileName.contains("Dict"))
		{
			String filePath = outPath + "/dictData/";
			geneFile(content, filePath, fileName);
		} else if (fileName.contains("Inst"))
		{
			String filePath = outPath + "/instData/";
			geneFile(content, filePath, fileName);
		} else
		{
			String filePath = outPath + "/constData/";
			geneFile(content, filePath, fileName);
		}
	}

	/**
	 * 写入文件
	 * @param content
	 * @param filePath
	 * @param fileName
	 * @throws Exception
	 */
	private static void geneFile(String content, String filePath, String fileName) throws Exception
	{
		createDir(filePath);
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(createFile(filePath + fileName)), "utf-8"));
		bw.write(content);
		bw.flush();
		bw.close();
	}

	/**
	 * 创建目录
	 * @param path
	 */
	private static void createDir(String path)
	{
		File dict = new File(path);
		if (!dict.exists())
			dict.mkdirs();
	}

	/**
	 * 创建文件
	 * @param filePath
	 * @return
	 */
	private static File createFile(String filePath)
	{
		File file = new File(filePath);
		if (!file.exists())
			try
			{
				file.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		return file;
	}

	/**
	 * 类型转换
	 * @param type
	 * @return
	 */
	private static String getCSType(String type)
	{
		String dataType = "";
		if (type.equals("int"))
		{
			dataType = "int";
		} else if (type.equals("bigint"))
		{
			dataType = "int";
		} else if (type.equals("varchar"))
		{
			dataType = "string";
		} else if (type.equals("float"))
		{
			dataType = "float";
		} else if (type.equals("double"))
		{
			dataType = "float";
		} else if (type.equals("smallint"))
		{
			dataType = "int";
		} else if (type.equals("decimal"))
		{
			dataType = "Number";
		} else if (type.equals("numeric"))
		{
			dataType = "Number";
		} else if (type.equals("timestamp"))
		{
			dataType = "Date";
		} else if (type.equals("datetime"))
		{
			dataType = "Date";
		} else if (type.equals("blob"))
		{
			dataType = "string";
		} else if (type.equals("text"))
			dataType = "string";
		return dataType;
	}

	/**
	 * 删除文件
	 * @param f
	 * @throws IOException
	 */
	private static void delAll(File f) throws IOException
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

	/**
	 * 不生成的表
	 * @param name
	 * @return
	 */
	public static boolean isNotUseTable(String name)
	{
		String[] str = no_tables.split(",");
		for (int i = 0; i < str.length; i++)
		{
			if (str[i].equals(name))
			{
				return false;
			}
		}
		return true;
	}
}
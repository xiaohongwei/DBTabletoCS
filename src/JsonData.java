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

/**
 * 该工具是把数据库的数据生成 josn数据格式的文件。<br/>
 * 注：className 是做一个对应标示,对应的是那张表。一般用于转换为对象时使用
 * 
 * @author benbear
 * @version 1.0
 */
public class JsonData
{
	/** 数据库IP地址 */
	private static String dBIp;
	/** 数据库名称 */
	private static String dBName = "";
	/** 用户名 */
	private static String uname = "";
	/** 密码 */
	private static String pwd = "";
	/** 输出路径 */
	private static String outPath = "";
	/** Cs文件名称类型 */
	private static String outTableNameType = "";
	/** Cs文件名称类型 */
	private static String outJsonType = "";
	/** 不生成的表 */
	private static String tables = "";
	/** josn数据*/
	private static JSONArray jsonArr = new JSONArray();

	/**
	 * 程序入口
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		String confiFile = System.getProperty("user.dir") + "/jsonData.properties";

		Properties prop = new Properties();
		InputStream in = new BufferedInputStream(new FileInputStream(confiFile));
		prop.load(in);

		dBIp = prop.getProperty("dBIp").trim();
		dBName = prop.getProperty("dBName").trim();
		uname = prop.getProperty("uname").trim();
		pwd = prop.getProperty("pwd").trim();
		outPath = prop.getProperty("outPath").trim();
		outTableNameType = prop.getProperty("outTableNameType").trim();
		outJsonType = prop.getProperty("outJsonType").trim();
		tables = prop.getProperty("tables").trim();

		System.out.println("设置文件：" + confiFile);
		System.out.println("生成路径：" + outPath);

		crtJosn();
		
		Runtime.getRuntime().exec("cmd.exe /k explorer.exe "+outPath.replaceAll("\\/", "\\\\"));
	}

	/**
	 * 生成json数据
	 * @param type
	 */
	public static void crtJosn()
	{
		try
		{
			Hashtable<String, String> tableMap = getTableInfo();
			Iterator<?> tableNames = tableMap.keySet().iterator();

			while (tableNames.hasNext())
			{
				String table = (String) tableNames.next();
				
				if(isUseTable(table))
				{
					System.out.println("生成表:" + table);
					getJson(table,getClassColumnName(table));
				}
			}

			geneFile(jsonArr.toString(), outPath, "jsonData.dict");

			System.out.println("全部生成完成!");
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 表是否生成
	 * @param name 表名
	 * @return
	 */
	public static boolean isUseTable(String name)
	{
		// 类型 1生成tables表 2不生成tables表
		if (outJsonType.equals("1"))
		{
			String[] str = tables.split(",");
			for (int i = 0; i < str.length; i++)
			{
				if (str[i].equals(name))
					return true;
			}
			return false;
		} else
		{
			String[] str = tables.split(",");
			for (int i = 0; i < str.length; i++)
			{
				if (str[i].equals(name))
					return false;
			}
			return true;
		}
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
	 * 获取json
	 * @param tableName 表名
	 * @param className 类型(对象名称)
	 * @throws Exception
	 */
	private static void getJson(String tableName, String className) throws Exception
	{
		String sqlstString = "select * from " + tableName;
		Statement statement2 = getMySQLConnection().createStatement();
		ResultSet rs2 = statement2.executeQuery(sqlstString);
		ResultSetMetaData data = rs2.getMetaData();

		while (rs2.next())
		{
			Map<String, Object> mp = new HashMap<String, Object>();

			for (int i = 1; i <= data.getColumnCount(); i++)
			{
				String columnName = data.getColumnName(i);

				String columnClassName = data.getColumnClassName(i);

				Object value = rs2.getString(columnName);
				if (columnClassName == "java.lang.Long")
				{
					if (value == null)
					{
						value = "0";
					}
					value = Long.parseLong(value + "");
				} else if (columnClassName == "java.lang.Double")
				{
					if (value == null)
					{
						value = "0";
					}
					value = Double.parseDouble(value + "");
				} else if (columnClassName == "java.lang.Integer")
				{
					if (value == null)
					{
						value = "0";
					}
					value = Integer.parseInt(value + "");
				}

				mp.put(columnName, value);
			}
			
			mp.put("className", className);

			jsonArr.put(mp);
		}
	}

	/**
	 * 写入文件
	 * @param content 内容
	 * @param filePath 路径
	 * @param fileName 文件名
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
	 * @param path 路径
	 */
	private static void createDir(String path)
	{
		File dict = new File(path);
		if (!dict.exists())
			dict.mkdirs();
	}

	/**
	 * 创建文件
	 * @param filePath 文件路径
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
	 * 文件名
	 * @param name
	 * @return
	 */
	private static String getClassColumnName(String name)
	{
		// 1为文件名跟表名一样  2标准命名规则（DictUserInfo）
		if(outTableNameType.equals("1"))
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
}
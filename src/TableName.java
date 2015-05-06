
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
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


	public static void main(String[] args) throws Exception
	{
		Properties prop = new Properties();

		String confiFile = System.getProperty("user.dir") + "/dictData.properties";
		System.out.println("设置文件：" + confiFile);
		InputStream in = new BufferedInputStream(new FileInputStream(confiFile));

		prop.load(in);
		dBIp = prop.getProperty("dBIp").trim();
		dBName = prop.getProperty("dBName").trim();
		outPath = prop.getProperty("outPath").trim();

		System.out.println("生成路径：" + outPath);

		Hashtable<String, String> tableMap = getTableInfo();
		Iterator<String> tableNames = tableMap.keySet().iterator();

		while (tableNames.hasNext())
		{
			String table = (String) tableNames.next();
			System.out.println("name_tableName.Add(\"" + table.replaceAll("_", "") + "\",\"" + table + "\");");
		}

		System.out.println("全部生成完成!");
	}
	
	private static Connection getMySQLConnection() throws Exception
	{
		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection("jdbc:mysql://" + dBIp + ":3306/" + dBName, "root", "root");
		return conn;
	}

	private static Hashtable<String, String> getTableInfo() throws Exception
	{
		Hashtable<String, String> tableMap = new Hashtable<String, String>();
		String sql = "SELECT table_name ,TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES  WHERE table_schema = '" + dBName + "'";
		Statement statement = getMySQLConnection().createStatement();
		for (ResultSet rs = statement.executeQuery(sql); rs.next(); tableMap.put(rs.getString("table_name"), rs.getString("TABLE_COMMENT")));
		return tableMap;
	}

}
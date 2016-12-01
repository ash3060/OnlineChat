import java.util.*;
/**
 * 用户信息类
 */
public class User{
	private String name;
	private String pwd;
	private String ip;
	public TreeSet <String> friends;
	private String nickname;
	private String gender;
	private String age;
	private String loc;
	public Map<String, String> chatRecord = new HashMap<String, String>();
	public User(String name, String pwd,String ip,String gender,String age,String loc,String nickname) {
		this.name = name;
		this.ip = ip;
		this.pwd = pwd;
		this.gender = gender;
		this.age = age;
		this.loc = loc;
		this.nickname = nickname;
		friends = new TreeSet<String>();
	}
	/**
	 * 设置地区
	 * @param loc 地区
	 */
	public void setLoc(String loc)
	{
		this.loc = loc;
	}
	/**
	 * 获得地区
	 * @return 用户所在地
	 */
	public String getLoc()
	{
		return loc;
	}
	/**
	 * 设置年龄
	 * @param age 年龄
	 */
	public void setAge(String age)
	{
		this.age = age;
	}
	/**
	 * 获得年龄
	 * @return 用户年龄
	 */
	public String getAge()
	{
		return age;
	}
	/**
	 * 设置性别
	 * @param gender 性别
	 */
	public void setGender(String gender)
	{
		this.gender = gender;
	}
	/**
	 * 获得性别
	 * @return 用户性别
	 */
	public String getGender()
	{
		return gender;
	}
	/**
	 * 获得朋友集合
	 * @return 用户的朋友集合
	 */
	public TreeSet <String> getFriend()
	{
		return this.friends;
	}
	/**
	 * 加好友
	 * @param friend 即将加入好友的用户
	 */
	public void addFriend(String friend)
	{
		this.friends.add(friend);
	}
	/**
	 * 删好友
	 * @param friend 即将删除好友的用户
	 */
	public void delFriend(String friend)
	{
		if (this.friends.contains(friend))
		{
			this.friends.remove(friend);
		}
	}
	/**
	 * 获得用户名
	 * @return 用户用户名(ID)
	 */
	public String getName() {
		return name;
	}
	/**
	 * 获得IP
	 * @return 用户IP
	 */
	public String getIp() {
		return ip;
	}
	/**
	 * 获得密码
	 * @return 用户密码
	 */
	public String getPwd()
	{
		return pwd;
	}
	/**
	 * 设置密码
	 * @param pwd 新密码
	 */
	public void setPwd(String pwd)
	{
		this.pwd = pwd;
	}
	/**
	 * 获得昵称
	 * @return 用户昵称
	 */
	public String getNickname()
	{
		return nickname;
	}
	/**
	 * 设置昵称
	 * @param nickname 新昵称
	 */
	public void setNickname(String nickname)
	{
		this.nickname = nickname;
	}
}

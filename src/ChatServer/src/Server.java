//服务器端
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
public class Server {

	private JFrame frame;
	private JTextArea contentArea;
	private JTextField txt_message;
	private JButton btn_start;
	private JButton btn_stop;
	private JButton btn_show;
	private JPanel northPanel;
	private JPanel southPanel;
	private JScrollPane rightPanel;
	private JScrollPane leftPanel;
	private JSplitPane centerSplit;
	private JList userList;
	private DefaultListModel listModel;

	final int port = 6999;
	final int fileport = 2000;
	private ServerSocket serverSocket;
	private ServerSocket fileserverSocket;
	private ServerThread serverThread;
	private ArrayList<ClientThread> clients;
	private HashSet<User> registeredUser;
	private HashMap<String,String>friendReqOff;
	private boolean isStart = false;

	/**
	 * 主方法
	 * @param args
	 */
	public static void main(String[] args) {
		new Server();
	}

	/**
	 * 查看在线用户状态
	 */
	public void show() {
		if (!isStart) {
			JOptionPane.showMessageDialog(frame, "服务器还未启动,不能发送消息！", "错误",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (clients.size() == 0) {
			JOptionPane.showMessageDialog(frame, "没有用户在线,不能发送消息！", "错误",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (User ii : registeredUser)
		{
			contentArea.append("usrname:" + ii.getName() + " nickname:" + ii.getNickname() + 
					" pwd:" + ii.getPwd() + " gender:" + ii.getGender() + " age:" + ii.getAge() +
					" location:" + ii.getLoc() + " friends:" + ii.getFriend() + "\n");
		}
		contentArea.append("OffLine Buffer:\n");
		for (String item : friendReqOff.keySet())
		{
			contentArea.append(item + ": "+ friendReqOff.get(item) + "\n");
		}
	}

	/**
	 * Server构造方法
	 */
	public Server() {
		frame = new JFrame("服务器");
		// 更改JFrame的图标：
		//frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("qq.png")));
		contentArea = new JTextArea();
		contentArea.setEditable(false);
		contentArea.setForeground(Color.blue);
		txt_message = new JTextField();
		btn_start = new JButton("启动");
		btn_stop = new JButton("停止");
		btn_show = new JButton("查看");
		btn_stop.setEnabled(false);
		listModel = new DefaultListModel();
		userList = new JList(listModel);

		southPanel = new JPanel(new BorderLayout());
		southPanel.setBorder(new TitledBorder("写消息"));
		southPanel.add(txt_message, "Center");
		southPanel.add(btn_show, "East");
		leftPanel = new JScrollPane(userList);
		leftPanel.setBorder(new TitledBorder("在线用户"));

		rightPanel = new JScrollPane(contentArea);
		rightPanel.setBorder(new TitledBorder("消息显示区"));

		centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
				rightPanel);
		centerSplit.setDividerLocation(100);
		northPanel = new JPanel();
		northPanel.setLayout(new GridLayout(1, 6));
		northPanel.add(btn_start);
		northPanel.add(btn_stop);
		northPanel.setBorder(new TitledBorder("配置信息"));

		frame.setLayout(new BorderLayout());
		frame.add(northPanel, "North");
		frame.add(centerSplit, "Center");
		frame.add(southPanel, "South");
		frame.setSize(600, 400);
		int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
		int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
		frame.setLocation((screen_width - frame.getWidth()) / 2,
				(screen_height - frame.getHeight()) / 2);
		frame.setVisible(true);

		/**
		 * 关闭窗口时事件
		 */
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isStart) {
					closeServer();// 关闭服务器
				}
				System.exit(0);// 退出程序
			}
		});

		/**
		 * 单击查看按钮时事件
		 */
		btn_show.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				show();
			}
		});

		/**
		 *  单击启动服务器按钮时事件
		 */
		btn_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isStart ) {
					JOptionPane.showMessageDialog(frame, "服务器已处于启动状态，不要重复启动！",
							"错误", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					serverStart(port, fileport);
					contentArea.append("消息服务器已成功启动！端口：" + port + "\n");
					contentArea.append("文件服务器已成功启动！端口：" + fileport + "\n");
					//JOptionPane.showMessageDialog(frame, "服务器成功启动!");
					btn_start.setEnabled(false);
					btn_stop.setEnabled(true);
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"错误", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		/**
		 *  单击停止服务器按钮时事件
		 */
		btn_stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isStart) {
					JOptionPane.showMessageDialog(frame, "服务器还未启动，无需停止！", "错误",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					closeServer();
					btn_start.setEnabled(true);
					btn_stop.setEnabled(false);
					contentArea.append("服务器成功停止!\n");
					JOptionPane.showMessageDialog(frame, "服务器成功停止！");
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, "停止服务器发生异常！", "错误",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	/**
	 *  启动服务器
	 * @param port 消息端口
	 * @param fileport 文件端口
	 * @throws java.net.BindException
	 */
	public void serverStart(int port, int fileport) throws java.net.BindException {
		try {
			clients = new ArrayList<ClientThread>();
			registeredUser = new HashSet<User>();
			friendReqOff = new HashMap<String,String>();
			serverSocket = new ServerSocket(port);
			fileserverSocket = new ServerSocket(fileport);
			serverThread = new ServerThread(serverSocket, fileserverSocket);
			serverThread.start();
			isStart = true;
		} catch (BindException e) {
			isStart = false;
			throw new BindException("端口号已被占用，请换一个！");
		} catch (Exception e1) {
			e1.printStackTrace();
			isStart = false;
			throw new BindException("启动服务器异常！");
		}
	}

	/**
	 *  关闭服务器
	 */
	@SuppressWarnings("deprecation")
	public void closeServer() {
		try {
			if (serverThread != null)
				serverThread.stop();// 停止服务器线程

			for (int i = clients.size() - 1; i >= 0; i--) {
				// 给所有在线用户发送关闭命令
				clients.get(i).getWriter().println("CLOSE");
				clients.get(i).getWriter().flush();
				// 释放资源
				clients.get(i).stop();// 停止此条为客户端服务的线程
				clients.get(i).reader.close();
				clients.get(i).writer.close();
				clients.get(i).socket.close();
				clients.remove(i);
			}
			if (serverSocket != null) {
				serverSocket.close();// 关闭服务器端连接
			}
			if (fileserverSocket != null) {
				fileserverSocket.close();// 关闭服务器端连接
			}
			listModel.removeAllElements();// 清空用户列表
			isStart = false;
		} catch (IOException e) {
			e.printStackTrace();
			isStart = true;
		}
	}

	/**
	 *  服务器线程
	 */
	class ServerThread extends Thread {
		private ServerSocket serverSocket;
		private ServerSocket fileserverSocket;

		/**
		 *  服务器线程的构造方法
		 * @param serverSocket
		 * @param fileserverSocket
		 */
		public ServerThread(ServerSocket serverSocket, ServerSocket fileserverSocket) {
			this.serverSocket = serverSocket;
			this.fileserverSocket = fileserverSocket;
		}

		public void run() {
			while (true) {// 不停的等待客户端的链接
				try {
					Socket socket = serverSocket.accept();
					Socket filesocket = fileserverSocket.accept();
					ClientThread client = new ClientThread(socket, filesocket);
					client.start();// 开启对此客户端服务的线程
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 *  为一个客户端服务的线程
	 */
	class ClientThread extends Thread {
		private Socket socket;
		private Socket filesocket;
		private BufferedReader reader;
		private PrintWriter writer;
		private DataInputStream din;
		private DataOutputStream dout;
		private User user;
		
		public BufferedReader getReader() {
			return reader;
		}

		public PrintWriter getWriter() {
			return writer;
		}
		
		public DataOutputStream getDout() {
			return dout;
		}
		public DataInputStream getDin() {
			return din;
		}

		public User getUser() {
			return user;
		}

		/**
		 *  客户端线程的构造方法
		 * @param socket
		 * @param filesocket
		 */
		public ClientThread(Socket socket, Socket filesocket) {
			try {
				boolean userexist = false;
				boolean useronline = false;
				this.socket = socket;
				this.filesocket = filesocket;
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream());
				din = new DataInputStream(new BufferedInputStream(filesocket.getInputStream()));
				dout = new DataOutputStream(new BufferedOutputStream(filesocket.getOutputStream()));
				// 接收客户端的基本用户信息
				String inf = reader.readLine();
				System.out.println(inf);
				contentArea.append("readline: " + inf);
				StringTokenizer st = new StringTokenizer(inf, "@");
				String command = st.nextToken();
				String usrname = st.nextToken();
				String usrpwd = st.nextToken();
				String usrIp = st.nextToken();
				String usrgender = "男";
				String usrage = "18";
				String usrloc = "北京";
				if (st.hasMoreTokens())
				{
					usrgender = st.nextToken();
					usrage = st.nextToken();
					usrloc = st.nextToken();
				}			
				user = new User(usrname, usrpwd, usrIp,usrgender,usrage,usrloc,usrname);
				
				
				if (command.equals("REGISTER"))
				{
					for (User item : registeredUser)
					{
						if (item.getName().equals(usrname))
						{
							this.getWriter().println("ALREADYREGISTERED");
							this.getWriter().flush();
							return;
						}
					}
					User tmp_user = user;
					registeredUser.add(tmp_user);
					this.getWriter().println("REGISTERSUCCESS");
					this.getWriter().flush();
					// 反馈连接成功信息
					writer.flush();
					// 反馈当前在线用户信息
					if (clients.size() > 0) {
						String temp = "";
						for (int i = clients.size() - 1; i >= 0; i--) {
							temp += (clients.get(i).getUser().getName() + "/" + clients
									.get(i).getUser().getIp())
									+ "@";
						}
						writer.println("USERLIST@" + clients.size() + "@" + temp);
						writer.flush();
					}
					// 向所有在线用户发送该用户上线命令
					for (int i = clients.size() - 1; i >= 0; i--) {
						clients.get(i).getWriter().println(
								"ADD@" + user.getName() + user.getIp());
						clients.get(i).getWriter().flush();
					}
					clients.add(this);
					listModel.addElement(this.getUser().getName());// 更新在线列表
					
				}
				
				else if (command.equals("CONNECT"))
				{
					for(ClientThread ct : clients) //already online
					{
						if(ct.getUser().getName().equals(user.getName()))
						{
							useronline = true;
							if(ct.getUser().getPwd().equals(usrpwd))
							{
							this.getWriter().println("ALREADYONLINE");
							this.getWriter().flush();	
							return;
							//this.destroy();
							}
							else
							{
								this.getWriter().println("PWDERROR");
								this.getWriter().flush();	
								return;
							}
						}
					}
					if (!useronline)
					{
						for (User item : registeredUser)
						{
							if (item.getName().equals(user.getName()))
							{
								user = item;
								userexist = true;
								if(item.getPwd().equals(usrpwd))
								{
									this.getWriter().print("CONNECTSUCCESS@"+user.getNickname()+"@"+user.getGender()+"@"+user.getAge()+"@"+user.getLoc());
									System.out.println("CONNECTSUCCESS@"+user.getNickname());
									for (String ii : user.friends)
									{
										this.getWriter().print("@" + ii);
										System.out.print("@" + ii);
									}
									this.getWriter().println();
									for (String jj : this.getUser().chatRecord.keySet())
									{
										if (!this.getUser().chatRecord.get(jj).equals(""))
										{
											String tmp[] = this.getUser().chatRecord.get(jj).split("\n");
											for (String ii : tmp)
											{
												this.getWriter().println("DOWNLOADRECORD@" + usrname +
														     			"@" + jj + "@" + ii);
											}
										}
									}
									this.getWriter().flush();
									// 反馈连接成功信息
									writer.flush();
									// 反馈当前在线用户信息
									if (clients.size() > 0) {
										String temp = "";
										for (int i = clients.size() - 1; i >= 0; i--) {
											temp += (clients.get(i).getUser().getName() + "/" + clients
													.get(i).getUser().getIp())
													+ "@";
										}
										writer.println("USERLIST@" + clients.size() + "@" + temp);
										writer.flush();
									}
									// 向所有在线用户发送该用户上线命令
									for (int i = clients.size() - 1; i >= 0; i--) {
										clients.get(i).getWriter().println(
												"ADD@" + user.getName() + user.getIp());
										clients.get(i).getWriter().flush();
									}
									clients.add(this);
									listModel.addElement(this.getUser().getName());// 更新在线列表
									contentArea.append(this.getUser().getName()
											+ this.getUser().getIp() + "上线!\n");
									if (friendReqOff.keySet().contains(usrname))
									{
										
										this.getWriter().print(friendReqOff.get(usrname));					
										this.getWriter().flush();
										friendReqOff.put(usrname, "");
									}				
								}
								else
								{
									this.getWriter().println("PWDERROR");
									this.getWriter().flush();	
									return;
								}
								
							}
							
						}
					}
					//未注册
					if(useronline == false && userexist == false)
					{		
						this.getWriter().println("NOTREGISTERED");
						this.getWriter().flush();	
					}
				}
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings("deprecation")
		/**
		 *  不断接收客户端的消息，进行处理
		 */
		public void run() {
			String message = null;
			while (true) {
				try {
					message = reader.readLine();// 接收客户端消息
					//System.out.println(message);
					contentArea.append("readline: " + message + "\n");
					if (message.equals("CLOSE"))// 下线命令
					{
						contentArea.append(this.getUser().getName()
								+ this.getUser().getIp() + "下线!\n");
						// 断开连接释放资源
						reader.close();
						writer.close();
						socket.close();

						// 向所有在线用户发送该用户的下线命令
						for (int i = clients.size() - 1; i >= 0; i--) {
							clients.get(i).getWriter().println(
									"DELETE@" + user.getName());
							clients.get(i).getWriter().flush();
						}

						listModel.removeElement(user.getName());// 更新在线列表

						// 删除此条客户端服务线程
						for (int i = clients.size() - 1; i >= 0; i--) {
							if (clients.get(i).getUser() == user) {
								ClientThread temp = clients.get(i);
								clients.remove(i);// 删除此用户的服务线程
								temp.stop();// 停止这条服务线程
								return;
							}
						}
					}
					else {
						dispatcherMessage(message);// 转发消息
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 *  处理消息
		 * @param message
		 */
		public void dispatcherMessage(String message) {
			StringTokenizer stringTokenizer = new StringTokenizer(message, "@");
			String source = stringTokenizer.nextToken();
			/**
			 * 好友请求
			 */
			if (source.equals("FRIENDREQUEST"))
			{
				boolean user_exist = false;
				boolean register_exist = false;
				String asker = stringTokenizer.nextToken();
				String receiver = stringTokenizer.nextToken();
				for (int i=clients.size()-1; i>=0; i--)
				{
					if (clients.get(i).getUser().getName().equals(receiver))
					{
						user_exist = true;
						ClientThread tmp = clients.get(i);
						tmp.getWriter().println("FRIENDREQUEST" + "@" + asker + "@" + receiver);
						tmp.getWriter().flush();
					}
				}
				if (!user_exist)
				{
					for(User item : registeredUser)
					{
						if(item.getName().equals(receiver))
						{
							register_exist = true;
							if (friendReqOff.containsKey(receiver))
							{
								String tmp = friendReqOff.get(receiver);
								tmp += "FRIENDREQUEST" + "@" + asker + "@" + receiver +"\n";
								friendReqOff.put(receiver, tmp);	
							}
							else
							{
								friendReqOff.put(receiver, "FRIENDREQUEST" + 
										"@" + asker + "@" + receiver +"\n");
							}
						}
					}
					if(!register_exist)
					{
						this.getWriter().println("USERNOTFOUND");
						this.getWriter().flush();
					}
				}
				
			}
			/**
			 * 接受好友请求
			 */
			else if (source.equals("FRIENDACCEPT"))
			{
				boolean user_exist = false;
				boolean register_exist = false;
				String accepter = stringTokenizer.nextToken();
				String receiver = stringTokenizer.nextToken();
				for (int i=clients.size()-1; i>=0; i--)
				{
					if (clients.get(i).getUser().getName().equals(receiver))
					{
						user_exist = true;
						ClientThread tmp = clients.get(i);
						tmp.getUser().addFriend(accepter);	
						tmp.getWriter().println("FRIENDACCEPT" + "@" + accepter + "@" + receiver);
						tmp.getWriter().flush();
					}
					if (clients.get(i).getUser().getName().equals(accepter))
					{
						ClientThread tmp = clients.get(i);
						tmp.getUser().addFriend(receiver);
					}
				}
				if (!user_exist)
				{
					for(User item : registeredUser)
					{
						if(item.getName().equals(receiver))
						{
							register_exist = true;
							item.addFriend(accepter);
							if (friendReqOff.containsKey(receiver))
							{
								String tmp = friendReqOff.get(receiver);
								tmp += "FRIENDACCEPT" + "@" + accepter + "@" + receiver +"\n";
								friendReqOff.put(receiver, tmp);	
							}
							else
							{
								friendReqOff.put(receiver, "FRIENDACCEPT" + 
										"@" + accepter + "@" + receiver +"\n");
							}
							
							/*change1設一個陣列記錄好友邀請，每次上線時檢查，或直接輸出為txt檔*/
						}
						if (item.getName().equals(accepter))
						{
							item.addFriend(receiver);
						}
					}
				}
			}
			/**
			 * 拒绝好友请求
			 */
			else if (source.equals("FRIENDREFUSE"))
			{
				boolean user_exist = false;
				boolean register_exist = false;
				String refuser = stringTokenizer.nextToken();
				String receiver = stringTokenizer.nextToken();
				for (int i = clients.size()-1; i>=0; i--)
				{
					if (clients.get(i).getUser().getName().equals(receiver))
					{
						user_exist = true;
						ClientThread tmp = clients.get(i);
						tmp.getWriter().println("FRIENDREFUSE" + "@" + refuser + "@" + receiver);
						tmp.getWriter().flush();
					}
				}
				if (!user_exist)
				{
					for(User item : registeredUser)
					{
						if(item.getName().equals(receiver))
						{
							register_exist = true;
							if (friendReqOff.containsKey(receiver))
							{
								String tmp = friendReqOff.get(receiver);
								tmp += "FRIENDREFUSE" + "@" + refuser + "@" + receiver +"\n";
								friendReqOff.put(receiver, tmp);	
							}
							else
							{
								friendReqOff.put(receiver, "FRIENDREQUEST" + 
										"@" + refuser + "@" + receiver +"\n");
							}							
						}
					}
				}
			}
			/**
			 * 查看好友资料
			 */
			else if(source.equals("VIEWPROFILE"))
			{
				String username = stringTokenizer.nextToken();
				String friend = stringTokenizer.nextToken();
				for(User item : registeredUser)
				{
					if(item.getName().equals(friend))
					{
						File fafile = new File(friend + ".jpg");
						long filelength = fafile.length();
						this.getWriter().println("PROFILE"+"@"+username+"@"+friend+"@"+
						item.getNickname()+"@"+item.getGender()+"@"+item.getAge()+"@"+item.getLoc()+"@"+
								filelength);
						this.getWriter().flush();
						
						//transport avatar
						try {
							FileInputStream fin = new FileInputStream(fafile);
							long read = 0;
							byte[] b = new byte[1024];
							int l = -1;
							while(read < filelength)
							{
								l = fin.read(b);
								dout.write(b, 0, l);
								read += l;
								dout.flush();
							}							
							fin.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}			
						break;
					}
				}
				
			}
			/**
			 * 删除好友
			 */
			else if (source.equals("DELETEFRIEND"))
			{
				String usrname = stringTokenizer.nextToken();
				String friendd = stringTokenizer.nextToken();
				for (User item : registeredUser)
				{
					if (item.getName().equals(usrname))
					{
						item.delFriend(friendd);
					}
					else if (item.getName().equals(friendd))
					{
						item.delFriend(usrname);
						boolean online = false;
						for (int i = 0; i<clients.size(); i++)
						{
							if (clients.get(i).getUser().getName().equals(friendd))  //friend online
							{
								online = true;
								clients.get(i).getWriter().println("DELETEFRIEND" + "@" + friendd + "@" + usrname);
								clients.get(i).getWriter().flush();
								break;
							}
						}
						if (!online)
						{
							if (friendReqOff.containsKey(friendd))
							{
								String tmp = friendReqOff.get(friendd);
								if (tmp == null) tmp = "";
								tmp += "DELETEFRIEND" + friendd + "@" + usrname +"\n";
								friendReqOff.put(friendd, tmp);	
							}
							else
							{
								friendReqOff.put(friendd, "DELETEFRIEND" + friendd + "@" + usrname +"\n");
							}	
						}
						break;
					}
				}
			}
			/**
			 * 修改密码
			 */
			else if(source.equals("CHANGEPWD"))
			{
				stringTokenizer.nextToken();
				String pwd = stringTokenizer.nextToken();
				this.getUser().setPwd(pwd);
			}
			/**
			 * 修改昵称
			 */
			else if(source.equals("NICKNAME"))
			{
				stringTokenizer.nextToken();
				String nickname = stringTokenizer.nextToken();
				this.getUser().setNickname(nickname);
			}
			/**
			 * 修改年龄
			 */
			else if (source.equals("CHANGEAGE"))
			{
				stringTokenizer.nextToken();
				String age = stringTokenizer.nextToken();
				this.getUser().setAge(age);
			}
			/**
			 * 修改性别
			 */
			else if (source.equals("CHANGEGENDER"))
			{
				stringTokenizer.nextToken();
				String gender = stringTokenizer.nextToken();
				this.getUser().setGender(gender);
			}
			/**
			 * 修改所在地
			 */
			else if (source.equals("CHANGELOCATION"))
			{
				String username = stringTokenizer.nextToken();
				String loc = stringTokenizer.nextToken();
				this.getUser().setLoc(loc);
			}
			/**
			 * 发送文件
			 */
			else if(source.equals("SENDFILE"))
			{
				String sender = stringTokenizer.nextToken();
				String receiver = stringTokenizer.nextToken();
				String filename = stringTokenizer.nextToken();
				long filelength = new Long(stringTokenizer.nextToken());
				for (int i = clients.size()-1; i>=0; i--)
				{
					if (clients.get(i).getUser().getName().equals(receiver))
					{
						ClientThread tmp = clients.get(i);
						tmp.getWriter().println("RECEIVEFILE" + "@" + sender + "@" + receiver + 
													"@" + filename + "@" + filelength);	
						tmp.getWriter().flush();
					}
				}
			}
			/**
			 * 接受文件
			 */
			else if (source.equals("FILEACCEPT"))
			{
				String receiver = stringTokenizer.nextToken();
				String sender = stringTokenizer.nextToken();
				String filename = stringTokenizer.nextToken();
				long filelength = new Long(stringTokenizer.nextToken());
				for (int i = clients.size()-1; i>=0; i--)
				{
					if (clients.get(i).getUser().getName().equals(sender))
					{
						ClientThread tmp = clients.get(i);
						this.getWriter().println("STARTFILE@" + sender + "@" + receiver + "@"
													+ filename + "@" + filelength);
						this.getWriter().flush();
						try {
							long read = 0;
							byte[] b = new byte[1024];
							int l = -1;
							while(read < filelength)
							{
								l = tmp.din.read(b);
								dout.write(b, 0, l);
								read += l;
								dout.flush();
							}				
							//din.close();
							//dout.close();

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						this.getWriter().flush();
					}
				}
				
			}
			/**
			 * 拒绝文件
			 */
			else if (source.equals("FILEREJECT"))
			{
				String receiver = stringTokenizer.nextToken();
				String sender = stringTokenizer.nextToken();
				for (int i = clients.size()-1; i>=0; i--)
				{
					if (clients.get(i).getUser().getName().equals(sender))
					{
						clients.get(i).getWriter().println("FILEREJECT@" + sender + "@" + receiver);
						clients.get(i).getWriter().flush();
						break;
					}
				}
				
			}
			/**
			 * 向用户发送其好友列表
			 */
			else if(source.equals("FRIENDLISTREQUEST"))
			{
				String username = stringTokenizer.nextToken();
				for (int i = clients.size()-1; i>=0; i--)
				{
					if (clients.get(i).getUser().getName().equals(username))
					{
						ClientThread tmp = clients.get(i);
						tmp.getWriter().print("FRIENDLIST" + "@" + username);
						for(String item : tmp.getUser().friends)
						{
							tmp.getWriter().print("@" + item);
						}
						tmp.getWriter().println();
						tmp.getWriter().flush();
					}
				}
			}
			/**
			 * 清楚云端聊天记录
			 */
			else if (source.equals("CLEARRECORD"))
			{
				for (String item: this.getUser().chatRecord.keySet())
				{
					this.getUser().chatRecord.put(item, "");
				}
			}
			/**
			 * 用户上载聊天记录
			 */
			else if (source.equals("UPLOADRECORD"))
			{
				String uploader = stringTokenizer.nextToken();
				String friendd = stringTokenizer.nextToken();
				//String line = stringTokenizer.
				String s = "";
				if (this.getUser().chatRecord.keySet().contains(friendd))
				{
					s = this.getUser().chatRecord.get(friendd);
				}
				
				s += stringTokenizer.nextToken() + "\n";
				this.getUser().chatRecord.put(friendd, s);
			}
			/**
			 * 用户上传头像
			 */
			else if (source.equals("UPLOADAVATAR"))
			{
				String usrname = stringTokenizer.nextToken();
				long filelength = new Long(stringTokenizer.nextToken());	
				try {
					File avatarfile = new File(usrname + ".jpg");
					if (avatarfile.exists())
					{
						avatarfile.delete();
					}
					avatarfile.createNewFile();
					FileOutputStream fout = new FileOutputStream(avatarfile);
					long read = 0;
					byte[] b = new byte[1024];
					int l = -1;
					while(read < filelength)
					{
						l = din.read(b);
						fout.write(b, 0, l);
						read += l;
						fout.flush();
					}							
					fout.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			/**
			 * 向用户发送头像
			 */
			else if (source.equals("DOWNLOADAVATAR"))
			{
				String usrname = stringTokenizer.nextToken();
				File avatarfile = new File(usrname + ".jpg");
				if (!avatarfile.exists())
				{
					JOptionPane.showMessageDialog(frame, usrname+"头像未找到！", null, 
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				long filelength = avatarfile.length();
				this.getWriter().println("DOWNLOADAVATAR@" + usrname + "@" + filelength);
				this.getWriter().flush();
				try {
					FileInputStream fin = new FileInputStream(avatarfile);
					long read = 0;
					byte[] b = new byte[1024];
					int l = -1;
					while(read < filelength)
					{
						l = fin.read(b);
						dout.write(b, 0, l);
						read += l;
						dout.flush();
					}							
					fin.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			/**
			 * 发消息
			 */
			else 
			{
				boolean user_exist = false;
				boolean register_exist = false;
				String receiver = stringTokenizer.nextToken();
				String content = stringTokenizer.nextToken();
				message = source + "@" + receiver + "@" + content;
				contentArea.append(message + "\n");
				if (receiver.equals("ALL")) {// 群发
					for (int i = clients.size() - 1; i >= 0; i--) {
						clients.get(i).getWriter().println(message + "(多人发送)");
						clients.get(i).getWriter().flush();
					}
				}
				else
				{	
					for (int i = clients.size() - 1; i >= 0; i--) {
						if (clients.get(i).getUser().getName().equals(receiver))
						{
							user_exist = true;
							ClientThread tmp = clients.get(i);
							tmp.getWriter().println(message);
							tmp.getWriter().flush();	
						}				
					}
					if (!user_exist)
					{
						for (User item : registeredUser)
						{
							if (item.getName().equals(receiver))
							{
								register_exist = true;
								if (friendReqOff.containsKey(receiver))
								{
									String tmp = friendReqOff.get(receiver);
									tmp += message +"\n";
									friendReqOff.put(receiver, tmp);	
								}
								else
								{
									friendReqOff.put(receiver, message +"\n");
								}
								
							}
						}
					}
					if (!user_exist && !register_exist)
					{
						this.getWriter().println("USERNOTFOUND");
						this.getWriter().flush();
						System.out.println("SERVER: USERNOTFOUND");
					}
				}
			}
			
		}
	}
}




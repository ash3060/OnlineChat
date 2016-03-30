//客户机端
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

	int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
	int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;

	private JFileChooser sendfc;
	private JFileChooser receivefc;
	private File filesrc;
	private File filedes;


	private JFrame profileFrame;
	private JPopupMenu listMenu;
	private JMenuItem viewprofile;
	private JMenuItem deletefriend;
	private JTextArea info;

	private JFrame regFrame;

	private JPanel dialogPanel;
	private JFrame logFrame;
	private JTextField txt_usrname;

	private String myname;
	private String nickname;
	private String pwd;
	private String gender;
	private String age;
	private String location;
	private ImageIcon avatar;
	private File avatarfile;
	private JLabel lb_fa = new JLabel();

	private String message_des;

	private JFrame frame;
	private JList userList;
	private JList friendList;
	private JTextArea textArea;
	//private JPanel chatArea;
	private JTextField textField;
	private JTextField txt_addfriend;
	private JButton btn_addfriend;
	private JButton btn_start;
	private JButton btn_register;
	private JButton btn_stop;
	private JButton btn_send;
	private JButton btn_senfile;
	//private JButton btn_sendpic;
	private JButton btn_deleterecord;
	private JPanel northPanel;
	private JPanel southPanel;

	private JPanel profilePanel;
	private JLabel lb_usrname = new JLabel("用户名");
	private JLabel lb_nickname = new JLabel("昵称");
	private JTextField txt_nickname;
	private JLabel lb_pwd = new JLabel("密码");
	private JPasswordField txt_profile_pwd;
	private JLabel lb_gender = new JLabel("性别");
	private JComboBox box_gender = new JComboBox();
	private JLabel lb_age = new JLabel("年龄");
	private JComboBox box_age = new JComboBox();
	private JLabel lb_location = new JLabel("地区");
	private JComboBox box_location = new JComboBox();
	//ch1
	private JLabel lb_avatar = new JLabel();
	private JButton btn_browse = new JButton();
	//ch1

	private JButton btn_modify;
	private JButton btn_save;


	private JScrollPane txtScroll;
	private JScrollPane userScroll;
	private JScrollPane friendsScroll;
	private JSplitPane centerSplit;

	private DefaultListModel listModel;
	private DefaultListModel friends;
	private boolean isConnected = false;

	final String hostIp = "192.168.1.101";
	final int port = 6999;
	final int fileport = 2000;
	private Socket socket;
	private Socket filesocket;
	private PrintWriter writer;
	private BufferedReader reader;
	private DataInputStream din;
	private DataOutputStream dout;

	private MessageThread messageThread;// 负责接收消息的线程
	private Map<String, String> chatRecord = new HashMap<String, String>();

	/** 
	 * 主方法
	 * @param args
	 */
	public static void main(String[] args) {
		new Client();
	}

	/**
	 *  发送消息
	 */
	public void send() {
		if (!isConnected) {
			JOptionPane.showMessageDialog(frame, "还没有连接服务器，无法发送消息！", "错误",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String message = textField.getText().trim();
		if (message == null || message.equals("")) {
			JOptionPane.showMessageDialog(frame, "消息不能为空！", "错误",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		sendMessage(myname + "@" + message_des + "@" + message);
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd,HH:mm:ss"); //24小时机制
		String s = chatRecord.get(message_des);
		if (s == null) s="";
		s += format.format(date) + "," + myname + "说:\n";
		s += message + "\n";
		chatRecord.put(message_des, s);
		textArea.setText(s);
		textField.setText(null);
	}

	/**
	 *  构造方法
	 */
	public Client() {
		box_gender = new JComboBox();
		box_gender.addItem("男");
		box_gender.addItem("女");
		box_gender.setSelectedItem("男");
		box_age = new JComboBox();
		for (int i=1; i<=100; i++)
		{
			box_age.addItem(i);
		}
		box_age.setSelectedItem(18);
		String str_location[] = new String[]{"北京", "上海", "江苏", "浙江", "广东", "山东", "黑龙江", "四川", "重庆"};
		box_location = new JComboBox(str_location);

		avatarfile = new File("avatarfile.jpg");
		avatar = new ImageIcon("default.jpg");
		avatar.setImage(avatar.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT));

		dialogPanel = new JPanel();
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setBackground(new Color(255, 210, 200));
		textField = new JTextField();
		message_des = "ALL";

		txt_addfriend = new JTextField("");
		btn_addfriend = new JButton("好友申请");
		btn_register = new JButton("注册");
		btn_start = new JButton("连接");
		btn_stop = new JButton("下线");
		btn_send = new JButton("发送");
		btn_senfile = new JButton("发送文件");
		//btn_sendpic = new JButton("发送图片");
		btn_deleterecord = new JButton("删除记录");
		listModel = new DefaultListModel();
		friends = new DefaultListModel();
		userList = new JList(listModel);
		friendList = new JList(friends);
		friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


		northPanel = new JPanel();
		northPanel.setLayout(new GridLayout(1, 4));
		northPanel.add(new JLabel("搜索用户"));
		northPanel.add(txt_addfriend);
		northPanel.add(btn_addfriend);
		northPanel.add(btn_stop);

		txtScroll = new JScrollPane(textArea);
		txtScroll.setBorder(new TitledBorder("消息显示区"));
		friendsScroll = new JScrollPane(friendList);
		friendsScroll.setBorder(new TitledBorder("好友"));
		userScroll = new JScrollPane(userList);
		userScroll.setBorder(new TitledBorder("在线用户"));

		profilePanel = new JPanel();
		profilePanel.setLayout(new GridLayout(8, 1));
		//profilePanel.setLayout(new FlowLayout());
		txt_nickname = new JTextField(10);
		txt_nickname.setEditable(false);
		txt_profile_pwd = new JPasswordField(10);
		txt_profile_pwd.setEchoChar('*');
		//ch
		btn_browse = new JButton("浏览");
		//ch
		txt_profile_pwd.setEditable(false);
		box_gender.setEditable(false);
		box_gender.setEnabled(false);
		box_age.setEditable(false);
		box_age.setEnabled(false);
		box_location.setEditable(false);
		box_location.setEnabled(false);
		btn_modify = new JButton("修改");
		btn_save = new JButton("保存");
		profilePanel.add(lb_avatar);
		profilePanel.add(btn_browse);
		profilePanel.add(lb_nickname);
		profilePanel.add(txt_nickname);
		profilePanel.add(new JLabel("密码"));
		profilePanel.add(txt_profile_pwd);
		profilePanel.add(lb_gender);
		profilePanel.add(box_gender);
		profilePanel.add(lb_age);
		profilePanel.add(box_age);
		profilePanel.add(lb_location);
		profilePanel.add(box_location);

		profilePanel.add(btn_modify);
		profilePanel.add(btn_save);

		southPanel = new JPanel(new GridLayout());
		southPanel.add(textField);
		southPanel.add(btn_send);
		southPanel.add(btn_senfile);
		//southPanel.add(btn_sendpic);
		southPanel.add(btn_deleterecord);
		southPanel.setBorder(new TitledBorder("写消息"));

		centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScroll,  friendsScroll);
		centerSplit.setDividerLocation(100);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new GridLayout());
		centerPanel.add(centerSplit);
		centerPanel.add(txtScroll);

		frame = new JFrame("用户");
		frame.setLayout(new GridLayout());
		dialogPanel.setLayout(new BorderLayout());
		dialogPanel.add(northPanel, "North");
		dialogPanel.add(centerPanel, "Center");
		dialogPanel.add(southPanel, "South");
		dialogPanel.setSize(500, 400);

		JSplitPane mySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dialogPanel, profilePanel);
		mySplit.setDividerLocation(600);

		frame.add(mySplit);

		frame.setSize(750, 400);
		frame.setLocation((screen_width - frame.getWidth()) / 2,
				(screen_height - frame.getHeight()) / 2);

		logFrame = new JFrame("登陆");
		logFrame.setSize(300, 450);

		logFrame.setLocation((screen_width - logFrame.getWidth()) / 2,
				(screen_height - logFrame.getHeight()) / 2);
		logFrame.setLayout(null);
		JLabel logTitle = new JLabel("登陆");
		Font f=new Font("Arial",Font.BOLD+Font.ITALIC,40);
		logTitle.setFont(f);
		logTitle.setBounds(110, 160, 100, 40);
		JPanel p1 = new JPanel();
		JPanel p2 = new JPanel();
		JLabel lb_request1 = new JLabel("用户名和密码都必须由英文和数字组成，");
		JLabel lb_request2 = new JLabel("不超过10位，区分大小写。");
		lb_request1.setBounds(40, 300, 300, 30);
		lb_request2.setBounds(40, 320, 300, 30);
		txt_usrname = new JTextField(10);
		final JPasswordField txt_pwd = new JPasswordField(10);
		txt_pwd.setEchoChar('*');
		p1.setLayout(new BorderLayout());
		p2.setLayout(new BorderLayout());
		p1.add(lb_usrname, "West");
		p1.add(txt_usrname, "East");
		p1.setBounds(60, 230, 180, 30);
		p2.add(lb_pwd, "West");
		p2.add(txt_pwd, "East");
		p2.setBounds(60, 265, 180, 30);
		JPanel p_btn = new JPanel();
		p_btn.setLayout(new GridLayout(1, 2));
		p_btn.add(btn_start);
		p_btn.add(btn_register);
		p_btn.setBounds(60, 360, 180, 30);


		logFrame.add(p1);
		logFrame.add(p2);
		logFrame.add(logTitle);
		logFrame.add(lb_request1);
		logFrame.add(lb_request2);
		logFrame.add(p_btn);
		logFrame.setResizable(false);
		logFrame.setVisible(true);

		regFrame = new JFrame();

		//右键弹出好友菜单
		profileFrame = new JFrame();
		profileFrame.setSize(300, 300);
		profileFrame.setLayout(new FlowLayout());
		profileFrame.setLocation((screen_width - profileFrame.getWidth()) / 2,
				(screen_height - profileFrame.getHeight()) / 2);
		listMenu = new JPopupMenu();
		viewprofile = new JMenuItem("查看资料");
		deletefriend = new JMenuItem("删除好友");
		listMenu.add(viewprofile);
		listMenu.add(deletefriend);
		friendList.add(listMenu);
		info = new JTextArea();
		profileFrame.add(info);

		//******************************************

		/**
		 * 单击注册按钮事件
		 */
		btn_register.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				avatarfile = new File("/Users/TsaonYih/Documents/eclipse_workspace/ChatClient/src/default.jpg");

				logFrame.setVisible(false);

				regFrame.setSize(300, 450);

				regFrame.setLocation((screen_width - logFrame.getWidth()) / 2,
						(screen_height - logFrame.getHeight()) / 2);
				regFrame.setLayout(null);
				JLabel regTitle = new JLabel("注册");
				Font f=new Font("Arial",Font.BOLD+Font.ITALIC,40);
				regTitle.setFont(f);
				regTitle.setBounds(110, 20, 100, 40);
				JPanel p1 = new JPanel();
				JPanel p2 = new JPanel();
				JPanel p3 = new JPanel();
				JPanel p4 = new JPanel();
				JPanel p5 = new JPanel();
				//ch
				JPanel p6 = new JPanel();
				//ch

				final JComboBox reg_box_gender = new JComboBox();
				reg_box_gender.addItem("男");
				reg_box_gender.addItem("女");
				reg_box_gender.setSelectedItem("男");
				final JComboBox reg_box_age = new JComboBox();
				for (int i=1; i<=100; i++)
				{
					reg_box_age.addItem(i);
				}
				reg_box_age.setSelectedItem(18);
				//ch



				final JLabel reg_lb_view = new JLabel(avatar);
				reg_lb_view.setSize(50, 50);
				final JButton reg_btn_browse = new JButton("浏览");

				String str_location[] = new String[]{"北京", "上海", "江苏", "浙江", "广东", "山东", "黑龙江", "四川", "重庆"};
				final JComboBox reg_box_location = new JComboBox(str_location);
				reg_box_gender.setEnabled(true);
				reg_box_age.setEnabled(true);
				reg_box_location.setEnabled(true);
				//ch
				reg_btn_browse.setEnabled(true);
				//ch


				JLabel lb_request1 = new JLabel("用户名和密码都必须由英文和数字组成，");
				JLabel lb_request2 = new JLabel("不超过10位，区分大小写。");
				lb_request1.setBounds(40, 150, 300, 30);
				lb_request2.setBounds(40, 170, 300, 30);
				final JTextField reg_usrname = new JTextField(10);
				final JPasswordField reg_pwd = new JPasswordField(10);
				reg_pwd.setEchoChar('*');
				p1.setLayout(new BorderLayout());
				p2.setLayout(new BorderLayout());
				p3.setLayout(new BorderLayout());
				p4.setLayout(new BorderLayout());
				p5.setLayout(new BorderLayout());
				p6.setLayout(new FlowLayout());

				p1.add(new JLabel("用户名"), "West");
				p1.add(reg_usrname, "East");
				p1.setBounds(60, 90, 180, 30);
				p2.add(new JLabel("密码"), "West");
				p2.add(reg_pwd, "East");
				p2.setBounds(60, 125, 180, 30);
				p3.add(new JLabel("性别"), "West");
				p3.add(reg_box_gender, "East");
				p3.setBounds(60, 205, 180, 30);
				p4.add(new JLabel("年龄"), "West");
				p4.add(reg_box_age, "East");
				p4.setBounds(60, 240, 180, 30);
				p5.add(new JLabel("地区"), "West");
				p5.add(reg_box_location, "East");
				p5.setBounds(60, 275, 180, 30);
				//ch
				p6.add(new JLabel("头像"),"West");
				p6.add(reg_lb_view,"Center");
				p6.add(reg_btn_browse, "East");
				p6.setBounds(60, 320, 180, 70);

				//ch


				JPanel p_btn = new JPanel();
				p_btn.setLayout(new GridLayout(1, 2));
				JButton btn_ok = new JButton("确定");
				JButton btn_cancel = new JButton("取消");
				p_btn.add(btn_ok);
				p_btn.add(btn_cancel);
				p_btn.setBounds(60, 390, 180, 30);
				regFrame.add(p1);
				regFrame.add(p2);
				regFrame.add(p3);
				regFrame.add(p4);
				regFrame.add(p5);
				//ch
				regFrame.add(p6);
				//ch

				regFrame.add(regTitle);
				regFrame.add(lb_request1);
				regFrame.add(lb_request2);
				regFrame.add(p_btn);
				regFrame.setResizable(false);
				regFrame.setVisible(true);

				/**
				 * 单击确定按钮事件
				 */
				btn_ok.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						myname = reg_usrname.getText().trim();
						pwd = reg_pwd.getText();
						if (myname.equals("") || pwd.equals("") || hostIp.equals("")) {
							JOptionPane.showMessageDialog(frame, "用户名密码不能为空！",
									"错误", JOptionPane.ERROR_MESSAGE);
							return;
						}
						Pattern p = Pattern.compile("[0-9a-zA-Z]+");
						Matcher m = p.matcher(myname);
						if (!m.matches())
						{
							JOptionPane.showMessageDialog(frame, "用户名不合要求！",
									"错误", JOptionPane.ERROR_MESSAGE);
							return;
						}
						m = p.matcher(pwd);
						if (!m.matches())
						{
							JOptionPane.showMessageDialog(frame, "密码不合要求！",
									"错误", JOptionPane.ERROR_MESSAGE);
							return;
						}
						gender = (String)reg_box_gender.getSelectedItem();
						age = ((Integer)reg_box_age.getSelectedItem()).toString();
						location = (String)reg_box_location.getSelectedItem();
						boolean flag = registerServer(port, fileport, hostIp, myname, pwd,
										gender, age, location);
						if (flag == false) {
							JOptionPane.showMessageDialog(frame, "连接失败！",
									null, JOptionPane.ERROR_MESSAGE);
							return;
						}
						//开始上传头像
						long filelength = avatarfile.length();
						sendMessage("UPLOADAVATAR" + "@" + myname + "@" + filelength);
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
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						frame.setTitle(myname);
					}
				});

				/** 
				 * 单击取消按钮事件
				 */
				btn_cancel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						regFrame.setVisible(false);
						logFrame.setVisible(true);
					}
				});

				/**
				 * 单击浏览(图片)按钮事件
				 */
				reg_btn_browse.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e){
						JFileChooser fc = new JFileChooser();
						FileNameExtensionFilter filter = new FileNameExtensionFilter(
						        "JPG & GIF & BMP & PNG Images", "jpg", "gif","bmp","png");
						fc.setFileFilter(filter);
						fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
						int selected = fc.showOpenDialog(regFrame);
						if (selected == JFileChooser.APPROVE_OPTION) {
							try
							{
								avatarfile = fc.getSelectedFile();
								if(avatarfile.exists())
								{
									 BufferedImage mavatar = ImageIO.read(avatarfile);
									 avatar = new ImageIcon(mavatar);
									 avatar.setImage(avatar.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT));
									 reg_lb_view.setIcon(avatar);
								}
							}catch(IOException ioe)
							{
								ioe.printStackTrace();
							}
						}
					}
				});

			}
		});

		/**
		 * 单击浏览(图片)按钮事件(修改资料处)
		 */
		btn_browse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
				        "JPG & GIF & BMP & PNG Images", "jpg", "gif","bmp","png");
				fc.setFileFilter(filter);
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int selected = fc.showOpenDialog(regFrame);
				if (selected == JFileChooser.APPROVE_OPTION) {
					try
					{
						avatarfile = fc.getSelectedFile();
						if(avatarfile.exists())
						{
							 BufferedImage mavatar = ImageIO.read(avatarfile);
							 avatar = new ImageIcon(mavatar);
							 avatar.setImage(avatar.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT));
							 lb_avatar.setSize(50, 50);
							 lb_avatar.setIcon(avatar);
							 long filelength = avatarfile.length();
							 sendMessage("UPLOADAVATAR" + "@" + myname + "@" + filelength);
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
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							frame.setTitle(myname);
						}
					}catch(IOException ioe)
					{
						ioe.printStackTrace();
					}
				}
			}
		});

		/**
		 * 单击修改(资料)按钮事件
		 */
		btn_modify.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txt_nickname.setEditable(true);
				txt_profile_pwd.setEditable(true);
				box_gender.setEditable(true);
				box_gender.setEnabled(true);
				box_age.setEditable(true);
				box_age.setEnabled(true);
				box_location.setEditable(true);
				box_location.setEnabled(true);
			}
		});

		/**
		 * 单击保存按钮事件
		 */
		btn_save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String tmp_nickname = txt_nickname.getText().trim();
				String tmp_pwd = txt_profile_pwd.getText();
				String tmp_gender = (String)box_gender.getSelectedItem();
				String tmp_age = ((Integer)box_age.getSelectedItem()).toString();
				String tmp_location = (String)box_location.getSelectedItem();

				if (!tmp_nickname.equals("") && tmp_nickname != null)
				{
					nickname = tmp_nickname;
					sendMessage("NICKNAME" + "@" + myname + "@" + nickname);
				}
				else
				{
					txt_nickname.setText(nickname);
				}
				if (!tmp_pwd.equals("") && tmp_pwd != null)
				{
					pwd = tmp_pwd;
					sendMessage("CHANGEPWD" + "@" + myname + "@" + pwd);
				}
				if (!tmp_gender.equals(gender))
				{
					gender = tmp_gender;
					sendMessage("CHANGEGENDER" + "@" + myname + "@" + gender);
				}
				if (!tmp_age.equals(age))
				{
					age = tmp_age;
					sendMessage("CHANGEAGE" + "@" + myname + "@" + age);
				}
				if (!tmp_gender.equals(location))
				{
					location = tmp_location;
					sendMessage("CHANGELOCATION" + "@" + myname + "@" + location);
				}

				txt_profile_pwd.setText("");

				txt_nickname.setEditable(false);
				txt_profile_pwd.setEditable(false);
				box_gender.setEditable(false);
				box_gender.setEnabled(false);
				box_age.setEditable(false);
				box_age.setEnabled(false);
				box_location.setEditable(false);
				box_location.setEnabled(false);
			}
		});

		/**
		 * 好友列表选中事件
		 */
		friendList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
				String selectedFriend = (String)friendList.getSelectedValue();
				System.out.println(selectedFriend);
				message_des = selectedFriend;
				textArea.setText(chatRecord.get(selectedFriend));
			}
		});

		/**
		 * 好友列表右键事件，弹出菜单
		 */
		friendList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == 3 && //right button
						friendList.getSelectedIndex() >=0)
				{
					 listMenu.show(friendList,e.getX(),e.getY());

				}
			}
		});

		/**
		 * 菜单项【查看资料】选中事件
		 */
		viewprofile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String friend = (String)friendList.getSelectedValue();
				sendMessage("VIEWPROFILE" + "@" + myname + "@" + friend);
			}
		});

		/**
		 * 菜单项【删除好友】选中事件
		 */
		deletefriend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String friend = (String)friendList.getSelectedValue();
				friends.removeElement(friend);
				sendMessage("DELETEFRIEND" + "@" + myname + "@" + friend);
			}
		});

		/**
		 * 写消息的文本框中按回车键时事件
		 */
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});

		/**
		 * 单击发送按钮时事件
		 */
		btn_send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});

		/**
		 * 单击发送文件事件
		 */
		btn_senfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				sendfc = new JFileChooser();
				sendfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				int selected = sendfc.showOpenDialog(frame.getContentPane());
				if (selected == JFileChooser.APPROVE_OPTION){
					if (!sendfc.getSelectedFile().exists())
					{
						JOptionPane.showMessageDialog(frame,"No such file!","Error", JOptionPane.ERROR_MESSAGE);
					}
					else
					{
						filesrc = sendfc.getSelectedFile();
						long filelength = filesrc.length();
						sendMessage("SENDFILE@" + myname + "@" + message_des +
									"@" + filesrc.getName() + "@" + filelength);
						//textField.setText(filesrc.getAbsolutePath());

						try {
							FileInputStream fin = new FileInputStream(filesrc);
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
							//dout.close();
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

					}
				}

			}
		});

		/**
		 * 单击删除记录事件
		 */
		btn_deleterecord.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chatRecord.put(message_des, "");
				textArea.setText(chatRecord.get(message_des));
			}
		});

		/**
		 * 单击加好友按钮事件
		 */
		btn_addfriend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String tmp = txt_addfriend.getText().trim();
				if (tmp.equals("") || tmp==null)
				{
					JOptionPane.showMessageDialog(frame, "请输入搜索用户名!",
							null, JOptionPane.ERROR_MESSAGE);
					return;
				}
				else if (tmp.equals(myname))
				{
					JOptionPane.showMessageDialog(frame, "不要放弃治疗!",
							null, JOptionPane.ERROR_MESSAGE);
					return;
				}
				else if (friends.contains(tmp))
				{
					JOptionPane.showMessageDialog(frame, "该用户已经是您的好友！!",
							null, JOptionPane.ERROR_MESSAGE);
					return;
				}
				sendMessage("FRIENDREQUEST" + "@" + myname +
						"@" + tmp);
				txt_addfriend.setText("");
			}
		});

		/**
		 * 单击连接按钮时事件
		 */
		btn_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isConnected) {
					JOptionPane.showMessageDialog(frame, "已处于连接上状态，不要重复连接!",
							"错误", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					myname = txt_usrname.getText().trim();
					pwd = txt_pwd.getText();
					if (myname.equals("") || pwd.equals("") || hostIp.equals("")) {
						throw new Exception("用户名、密码不能为空!");
					}
					Pattern p = Pattern.compile("[0-9a-zA-Z]+");
					Matcher m = p.matcher(myname);
					if (!m.matches())
					{
						JOptionPane.showMessageDialog(frame, "用户名不合要求！",
								"错误", JOptionPane.ERROR_MESSAGE);
						return;
					}
					m = p.matcher(pwd);
					if (!m.matches())
					{
						JOptionPane.showMessageDialog(frame, "密码不合要求！",
								"错误", JOptionPane.ERROR_MESSAGE);
						return;
					}
					boolean flag = connectServer(port, fileport, hostIp, myname, pwd);
					if (flag == false) {
						throw new Exception("与服务器连接失败!");
					}
					frame.setTitle(myname);
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"错误", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		/**
		 * 单击下线按钮时事件
		 */
		btn_stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isConnected) {
					JOptionPane.showMessageDialog(frame, "已处于断开状态，不要重复断开!",
							"错误", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					boolean flag = closeConnection();// 断开连接
					if (flag == false) {
						throw new Exception("断开连接发生异常！");
					}
					JOptionPane.showMessageDialog(frame, "成功下线!");
					System.exit(0);// 退出程序
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"错误", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		/**
		 * 关闭窗口时事件
		 */
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isConnected) {
					closeConnection();// 关闭连接
				}
				System.exit(0);// 退出程序
			}
		});
		logFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isConnected) {
					closeConnection();// 关闭连接
				}
				System.exit(0);// 退出程序
			}
		});
		regFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isConnected) {
					closeConnection();// 关闭连接
				}
				System.exit(0);// 退出程序
			}
		});
	}

	/**
	 * 上线连接服务器
	 * @param port 消息端口
	 * @param fileport 文件端口
	 * @param hostIp IP
	 * @param name 用户名
	 * @param pwd 密码
	 * @return 连接是否成功
	 */
	public boolean connectServer(int port, int fileport, String hostIp, String name, String pwd) {
		// 连接服务器
		try {
			socket = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接
			filesocket = new Socket(hostIp, fileport);
			writer = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			din = new DataInputStream(new BufferedInputStream(filesocket.getInputStream()));
			dout = new DataOutputStream(new BufferedOutputStream(filesocket.getOutputStream()));

			// 发送客户端用户基本信息(用户名和ip地址)
			sendMessage("CONNECT" + "@" + name + "@" + pwd + "@" + socket.getLocalAddress().toString() );
			// 开启接收消息的线程
			messageThread = new MessageThread(reader, textArea);
			messageThread.start();
			//isConnected = true;// 已经连接上了
			return true;
		} catch (Exception e) {
			textArea.append("与端口号为：" + port + "    IP地址为：" + hostIp
					+ "   的服务器连接失败!" + "\n");
			isConnected = false;// 未连接上
			return false;
		}
	}

	/**
	 * 注册连接服务器
	 * @param port 消息端口
	 * @param fileport 文件端口
	 * @param hostIp IP
	 * @param name 用户名
	 * @param pwd 密码
	 * @param gender 性别
	 * @param age 年龄
	 * @param location 地区
	 * @return 连接是否成功
	 */
	public boolean registerServer(int port, int fileport, String hostIp, String name, String pwd,
									String gender, String age, String location) {
		// 连接服务器
		try {
			socket = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接
			filesocket = new Socket(hostIp, fileport);
			writer = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			din = new DataInputStream(new BufferedInputStream(filesocket.getInputStream()));
			dout = new DataOutputStream(new BufferedOutputStream(filesocket.getOutputStream()));

			// 发送客户端用户基本信息(用户名和ip地址)
			sendMessage("REGISTER" + "@" + name + "@" + pwd + "@" + socket.getLocalAddress().toString() +
					"@" + gender + "@" + age + "@" + location);
			// 开启接收消息的线程
			messageThread = new MessageThread(reader, textArea);
			messageThread.start();
			//isConnected = true;// 已经连接上了
			return true;
		} catch (Exception e) {
			textArea.append("与端口号为：" + port + "    IP地址为：" + hostIp
					+ "   的服务器连接失败!" + "\n");
			isConnected = false;// 未连接上
			return false;
		}
	}

	/**
	 * 发送消息
	 * @param message
	 */
	public void sendMessage(String message) {
		writer.println(message);
		System.out.println(message);
		writer.flush();
	}

	/**
	 * 客户端主动关闭连接
	 */
	@SuppressWarnings("deprecation")
	public synchronized boolean closeConnection() {
		try {
			//上传聊天记录
			sendMessage("CLEARRECORD");
			for (String item : chatRecord.keySet())  //item friend
			{
				if (!chatRecord.get(item).equals(""))
				{
					String[] tmp = chatRecord.get(item).split("\n");
					for (String i : tmp)  // i 1 line
					{
						sendMessage("UPLOADRECORD@" + myname + "@" + item + "@" + i);
					}
				}
			}
			listModel.removeAllElements();
			sendMessage("CLOSE");// 发送断开连接命令给服务器
			messageThread.stop();// 停止接受消息线程
			// 释放资源
			if (reader != null) {
				reader.close();
			}
			if (writer != null) {
				writer.close();
			}
			if (din != null) {
				din.close();
			}
			if (dout != null) {
				dout.close();
			}
			if (socket != null) {
				socket.close();
			}
			if (filesocket != null)
			{
				filesocket.close();
			}
			isConnected = false;
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
			isConnected = true;
			return false;
		}
	}

	/**
	 *  不断接收消息的线程
	 */
	class MessageThread extends Thread {
		private BufferedReader reader;
		private JTextArea textArea;

		/**
		 *  接收消息线程的构造方法
		 * @param reader 输入流
		 * @param textArea 消息显示区
		 */
		public MessageThread(BufferedReader reader, JTextArea textArea) {
			this.reader = reader;
			this.textArea = textArea;
		}

		/**
		 *  被动的关闭连接
		 * @throws Exception
		 */
		public synchronized void closeCon() throws Exception {
			// 清空用户列表
			listModel.removeAllElements();
			friends.removeAllElements();
			// 被动的关闭连接释放资源
			if (reader != null) {
				reader.close();
			}
			if (writer != null) {
				writer.close();
			}
			if (din != null) {
				din.close();
			}
			if (dout != null) {
				dout.close();
			}
			if (socket != null) {
				socket.close();
			}
			if (filesocket != null) {
				filesocket.close();
			}
			isConnected = false;// 修改状态为断开
		}

		public void run() {
			String message = "";
			while (true) {
				try {
					message = reader.readLine();
					System.out.println(message);
					StringTokenizer stringTokenizer = new StringTokenizer(
							message, "/@");
					String command = stringTokenizer.nextToken();// 命令
					/**
					 *  服务器已关闭命令
					 */
					if (command.equals("CLOSE"))
					{
						textArea.append("服务器已关闭!\n");
						closeCon();// 被动的关闭连接
						return;// 结束线程
					}
					/**
					 *  有用户上线更新在线列表
					 */
					else if (command.equals("ADD")) {
						String username = "";
						String userIp = "";
						if ((username = stringTokenizer.nextToken()) != null
								&& (userIp = stringTokenizer.nextToken()) != null) {
							//User user = new User(username, userIp);
							//onLineUsers.put(username, user);
							listModel.addElement(username);
						}
					}
					/**
					 *  有用户下线更新在线列表
					 */
					else if (command.equals("DELETE")) {
						String username = stringTokenizer.nextToken();
						//User user = (User) onLineUsers.get(username);
						//onLineUsers.remove(user);
						listModel.removeElement(username);
					}
					/**
					 *  加载在线用户列表
					 */
					else if (command.equals("USERLIST")) {
						int size = Integer
								.parseInt(stringTokenizer.nextToken());
						String username = null;
						String userIp = null;
						for (int i = 0; i < size; i++) {
							username = stringTokenizer.nextToken();
							userIp = stringTokenizer.nextToken();
							System.out.println(username + "  " + userIp);
							listModel.addElement(username);
						}
					}
					/**
					 * 收到好友申请
					 */
					else if (command.equals("FRIENDREQUEST")) 
					{
						String asker = stringTokenizer.nextToken();
						int res = JOptionPane.showConfirmDialog(frame, "来自"+asker+"的好友申请",null,
								JOptionPane.YES_NO_OPTION);
						if (res == 0) //yes
						{
							String s = "你和" + asker + "已经是好友了，现在开始对话吧！\n";
							chatRecord.put(asker, s);
							message_des = asker;
							//friendList.setSelectedValue(asker, true);
							sendMessage("FRIENDACCEPT" + "@" + myname + "@" + asker);
							sendMessage("FRIENDLISTREQUEST" + "@" + myname);
						}
						else
						{
							sendMessage("FRIENDREFUSE" + "@" + myname + "@" + asker);
						}
					}
					/**
					 * 好友申请被通过
					 */
					else if (command.equals("FRIENDACCEPT"))  
					{
						String accepter = stringTokenizer.nextToken();
						String s = "你和" + accepter + "已经是好友了，现在开始对话吧！\n";
						chatRecord.put(accepter, s);
						message_des = accepter;
						JOptionPane.showMessageDialog(frame, accepter + "已经接受了你的好友请求", null,
								JOptionPane.INFORMATION_MESSAGE);
						sendMessage("FRIENDLISTREQUEST" + "@" + myname);
					}
					/**
					 * 好友申请被拒绝
					 */
					else if (command.equals("FRIENDREFUSE")) 
					{
						String username = stringTokenizer.nextToken();
						JOptionPane.showMessageDialog(frame, username + "已经拒绝了你的好友申请", null,
								JOptionPane.INFORMATION_MESSAGE);
					}
					/**
					 * 搜索好友不存在
					 */
					else if (command.equals("USERNOTFOUND")) 
					{
						JOptionPane.showMessageDialog(frame, "无此用户！","错误",
								JOptionPane.ERROR_MESSAGE);
					}
					/**
					 * 被删好友
					 */
					else if (command.equals("DELETEFRIEND"))
					{
						stringTokenizer.nextToken();
						String usrname = stringTokenizer.nextToken();
						JOptionPane.showMessageDialog(frame, usrname + "同你解除好友关系！", null,
								JOptionPane.INFORMATION_MESSAGE);
						sendMessage("FRIENDLISTREQUEST" + "@" + myname);

					}
					/**
					 * 上线成功
					 */
					else if (command.equals("CONNECTSUCCESS")) 
					{
						nickname = stringTokenizer.nextToken();
						gender = stringTokenizer.nextToken();
						age = stringTokenizer.nextToken();
						Integer tmp_age = new Integer(age);
						location = stringTokenizer.nextToken();
						friends.removeAllElements();
						String friendd;
						while (stringTokenizer.hasMoreTokens())
						{
							friendd = stringTokenizer.nextToken();
							System.out.println(friendd);
							friends.addElement(friendd);
						}
						for (String item : chatRecord.keySet())
						{
							chatRecord.put(item, "");
						}
						JOptionPane.showMessageDialog(frame, "登陆成功！",null,
								JOptionPane.INFORMATION_MESSAGE);
						isConnected = true;
						logFrame.setVisible(false);
						regFrame.setVisible(false);
						txt_nickname.setText(nickname);
						box_gender.setSelectedItem(gender);
						box_age.setSelectedItem(tmp_age);
						box_location.setSelectedItem(location);
						frame.setVisible(true);
						sendMessage("DOWNLOADAVATAR" + "@" + myname);
					}
					/**
					 * 用户未注册
					 */
					else if (command.equals("NOTREGISTERED"))
					{
						int ans = JOptionPane.showConfirmDialog(frame, "未注册，是否注册？", null,
								JOptionPane.YES_NO_OPTION);
						if (ans == 0) //yes
						{
							btn_register.doClick();
						}
					}
					/**
					 * 注册成功
					 */
					else if (command.equals("REGISTERSUCCESS"))  //注册成功
					{
						JOptionPane.showMessageDialog(frame, "新用户注册成功！", null,
								JOptionPane.INFORMATION_MESSAGE);
						isConnected = true;
						logFrame.setVisible(false);
						regFrame.setVisible(false);
						frame.setVisible(true);
						nickname = myname;
						txt_nickname.setText(nickname);
						box_gender.setSelectedItem(gender);
						Integer tmp_age = new Integer(age);
						box_age.setSelectedItem(tmp_age);
						box_location.setSelectedItem(location);
						lb_avatar.setSize(50, 50);
						lb_avatar.setIcon(avatar);
					}
					/**
					 * 已注册过
					 */
					else if (command.equals("ALREADYREGISTERED"))
					{
						JOptionPane.showMessageDialog(frame, "该用户已注册！", null,
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					/**
					 * 已在线
					 */
					else if (command.equals("ALREADYONLINE"))  //已经在线
					{
						JOptionPane.showMessageDialog(frame, "该用户已在线！", null,
								JOptionPane.ERROR_MESSAGE);
						isConnected = false;
					}
					/**
					 * 密码错误
					 */
					else if (command.equals("PWDERROR"))
					{
						JOptionPane.showMessageDialog(frame, "密码错误！", null,
								JOptionPane.ERROR_MESSAGE);
					}
					/**
					 * 更新好友列表
					 */
					else if (command.equals("FRIENDLIST"))
					{
						friends.removeAllElements();
						stringTokenizer.nextToken();
						while (stringTokenizer.hasMoreTokens())
						{
							friends.addElement(stringTokenizer.nextToken());
						}
						friendList.setSelectedValue(message_des, true);
					}
					/**
					 * 查看好友资料
					 */
					else if (command.equals("PROFILE"))
					{
						stringTokenizer.nextToken();
						String usrname = stringTokenizer.nextToken();;
						String usrnickname = stringTokenizer.nextToken();
						String usrgender = stringTokenizer.nextToken();
						String usrage = stringTokenizer.nextToken();
						String usrlocation = stringTokenizer.nextToken();
						long filelength = new Long(stringTokenizer.nextToken());
						info.setEditable(false);
						info.setEnabled(false);
						info.setText("用户名: " + usrname + "\n");
						info.append("昵称: " + usrnickname + "\n");
						info.append("性别: " + usrgender + "\n");
						info.append("年龄: " + usrage + "\n");
						info.append("地区: " + usrlocation + "\n");
						profileFrame.add(info);

						File fafile = new File(usrname + ".jpg");
						if (fafile.exists()) fafile.delete();
						fafile.createNewFile();
						try {
							FileOutputStream fout = new FileOutputStream(fafile);
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
							BufferedImage favatar = ImageIO.read(fafile);
							ImageIcon icon_fa = new ImageIcon(favatar);
							icon_fa.setImage(icon_fa.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT));
							lb_fa.setSize(50, 50);
							lb_fa.setIcon(icon_fa);
							profileFrame.add(lb_fa);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						profileFrame.setVisible(true);

					}
					/**
					 * 下载聊天记录
					 */
					else if (command.equals("DOWNLOADRECORD"))
					{
						stringTokenizer.nextToken();
						String friendd = stringTokenizer.nextToken();
						String s = "";
						if (chatRecord.keySet().contains(friendd))
						{
							s = chatRecord.get(friendd);
							if (s == null)
							{
								s = "";
							}
						}
						s += stringTokenizer.nextToken() + "\n";
						chatRecord.put(friendd, s);
						textArea.setText(s);
						friendList.setSelectedValue(friendd, true);
					}
					/**
					 * 下载头像
					 */
					else if (command.equals("DOWNLOADAVATAR"))
					{
						stringTokenizer.nextToken();
						long filelength = new Long(stringTokenizer.nextToken());
						try {
							FileOutputStream fout = new FileOutputStream(avatarfile);
							if (!avatarfile.exists())
							{
								avatarfile.createNewFile();
							}
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
							BufferedImage mavatar = ImageIO.read(avatarfile);
							avatar = new ImageIcon(mavatar);
							avatar.setImage(avatar.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT));
							lb_avatar.setSize(50, 50);
							lb_avatar.setIcon(avatar);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					/**
					 * 收到文件请求
					 */
					else if (command.equals("RECEIVEFILE"))
					{
						String sender = stringTokenizer.nextToken();
						String receiver = stringTokenizer.nextToken();
						String filename = stringTokenizer.nextToken();
						long filelength = new Long(stringTokenizer.nextToken());
						int res =
						JOptionPane.showConfirmDialog(frame,  "来自"+sender+"的文件:" + filename, null,
								JOptionPane.YES_NO_OPTION);
						if (res == 0) //yes
						{
							sendMessage("FILEACCEPT" + "@" + myname + "@" + sender + "@" +
										filename + "@" + filelength);

						}
						else
						{
							sendMessage("FILEREJECT" + "@" + myname + "@" + sender);
						}

					}
					/**
					 * 文件被拒绝
					 */
					else if (command.equals("FILEREJECT"))
					{
						stringTokenizer.nextToken();
						String receiver = stringTokenizer.nextToken();
						JOptionPane.showMessageDialog(frame, receiver +"拒绝了你的文件！",null,
								JOptionPane.INFORMATION_MESSAGE);
					}
					/**
					 * 开始接收文件
					 */
					else if (command.equals("STARTFILE"))
					{
						String sender = stringTokenizer.nextToken();
						String receiver = stringTokenizer.nextToken();
						String filename = stringTokenizer.nextToken();
						long filelength = new Long(stringTokenizer.nextToken());
						receivefc = new JFileChooser();
						receivefc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						int selected = receivefc.showOpenDialog(frame.getContentPane());
						if (selected == JFileChooser.APPROVE_OPTION){
							if (!receivefc.getSelectedFile().exists())
							{
								JOptionPane.showMessageDialog(frame,"No such directory!","Error",
										JOptionPane.ERROR_MESSAGE);
							}
							else
							{
								filedes = receivefc.getSelectedFile();
								//textField.setText(filesrc.getAbsolutePath());
								File target = new File(filedes.getAbsolutePath() + File.separator + filename);
								FileOutputStream fout = new FileOutputStream(target);
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
								//din.close();
								fout.close();
								JOptionPane.showMessageDialog(frame, "文件接收成功!", null,
										JOptionPane.INFORMATION_MESSAGE);
								String s = chatRecord.get(sender);
								if (s == null) s = "";
								Date date = new Date();
								SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd,HH:mm:ss"); //24小时机制
								s += format.format(date) + "," + myname + "接收了来自"+sender+"的文件:" + filename + "\n";
								chatRecord.put(sender, s);
								textArea.setText(s);
							}
						}
					}
					/**
					 * 普通消息
					 */
					else {
						StringTokenizer st = new StringTokenizer(message, "@");
						String source = st.nextToken();
						String des = st.nextToken();
						String m = st.nextToken();
						Date date = new Date();
						SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd,HH:mm:ss"); //24小时机制
						String s = chatRecord.get(source);
						if (s == null) s = "";
						s += format.format(date) + "," + source + "说:\n";
						s += m + "\n";
						chatRecord.put(source, s);
						friendList.setSelectedValue(source, true);
						textArea.setText(s);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}


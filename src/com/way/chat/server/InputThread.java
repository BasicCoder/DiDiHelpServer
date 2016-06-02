package com.way.chat.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.way.chat.common.bean.SeekInfoEntity;
import com.way.chat.common.bean.TextMessage;
import com.way.chat.common.bean.User;
import com.way.chat.common.tran.bean.TranObject;
import com.way.chat.common.tran.bean.TranObjectType;
import com.way.chat.common.util.MyDate;
import com.way.chat.dao.UserDao;
import com.way.chat.dao.impl.UserDaoFactory;



/**
 * ����Ϣ�̺߳ʹ�����
 * 
 * @author way
 * 
 */
public class InputThread extends Thread {
	private Socket socket;// socket����
	private OutputThread out;// ���ݽ�����д��Ϣ�̣߳���Ϊ����Ҫ���û��ظ���Ϣ��
	private OutputThreadMap map;// д��Ϣ�̻߳�����
	private ObjectInputStream ois;// ����������
	private boolean isStart = true;// �Ƿ�ѭ������Ϣ

	public InputThread(Socket socket, OutputThread out, OutputThreadMap map) {
		this.socket = socket;
		this.out = out;
		this.map = map;
		try {
			ois = new ObjectInputStream(socket.getInputStream());// ʵ��������������
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void setStart(boolean isStart) {// �ṩ�ӿڸ��ⲿ�رն���Ϣ�߳�
		this.isStart = isStart;
	}

	@Override
	public void run() {
		try {
			while (isStart) {
				// ��ȡ��Ϣ
				System.out.println("0000");
				readMessage();
			}
			if (ois != null)
				ois.close();
			if (socket != null)
				socket.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * ����Ϣ�Լ�������Ϣ���׳��쳣
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readMessage() throws IOException, ClassNotFoundException {
		System.out.println("xxxx");
//		if(ois.available() <= 0 ){
//			return;
//		}
		Object readObject = ois.readObject();// �����ж�ȡ����
		if(readObject == null)
			return;
		System.out.println("1111");
		UserDao dao = UserDaoFactory.getInstance();// ͨ��daoģʽ�����̨
		if (readObject != null && readObject instanceof TranObject) {
			
			TranObject read_tranObject = (TranObject) readObject;// ת���ɴ������
			
			System.out.println("2222");
			
			switch (read_tranObject.getType()) {
			case REGISTER:// ����û���ע��
				System.out.println("3333");
				User registerUser = (User) read_tranObject.getObject();
				int registerResult = dao.register(registerUser);
				System.out.println(MyDate.getDateCN() + " ���û�ע��:"
						+ registerResult);
				// ���û��ظ���Ϣ
				TranObject<User> register2TranObject = new TranObject<User>(
						TranObjectType.REGISTER);
				User register2user = new User();
				register2user.setId(registerResult);
				register2TranObject.setObject(register2user);
				out.setMessage(register2TranObject);
				break;
			case LOGIN:
				User loginUser = (User) read_tranObject.getObject();
				ArrayList<User> userlist = dao.login(loginUser);
				
				System.out.println("UserList: " + Boolean.toString(userlist.isEmpty()));
				
				TranObject<ArrayList<User>> login2Object = new TranObject<ArrayList<User>>(TranObjectType.LOGIN);
				     
				if (userlist != null) {// �����¼�ɹ�
					TranObject<User> onObject = new TranObject<User>(
							TranObjectType.LOGIN);
					User login2User = new User();
					login2User.setId(loginUser.getId());
					onObject.setObject(login2User);
					System.out.println("LoginHaveOutputThread: " + map.size());
					for (OutputThread onOut : map.getAll()) {
						onOut.setMessage(onObject);// �㲥һ���û�����
					}
					map.add(loginUser.getId(), out);// �ȹ㲥���ٰѶ�Ӧ�û�id��д�̴߳���map�У��Ա�ת����Ϣʱ����
					login2Object.setObject(userlist);// �Ѻ����б����ظ��Ķ�����
					
					System.out.println("Have");
				} else {
					login2Object.setObject(null);
					System.out.println("None");
				}
				out.setMessage(login2Object);// ͬʱ�ѵ�¼��Ϣ�ظ����û�
				
				System.out.println(MyDate.getDateCN() + " �û���"
						+ loginUser.getId() + " ������");
				break;
			case SEEKINFO:
				System.out.println("SendSeekInfoList");
				ArrayList<SeekInfoEntity> seeklist = dao.findAllSeekInfo(); // �õ���ǰ����������Ϣ��
				System.out.println("SeekInfoList: " + Boolean.toString(seeklist.isEmpty()));
				TranObject<ArrayList<SeekInfoEntity>> seekInfoObject = new TranObject<ArrayList<SeekInfoEntity>>(TranObjectType.SEEKINFO);
				if(seeklist != null){
					seekInfoObject.setObject(seeklist);
					out.setMessage(seekInfoObject); // �ѵ�ǰ������Ϣ�ظ����û�
				}
				break;
			case PUBLISHINFO:
				System.out.println("SendSeekInfoEntity");
				SeekInfoEntity seekinfo = (SeekInfoEntity) read_tranObject.getObject();
				int publishResult = dao.publishSeekInfo(seekinfo);
				TranObject<SeekInfoEntity> onObject = new TranObject<SeekInfoEntity>(TranObjectType.PUBLISHINFO);
				if(seekinfo != null){
					onObject.setObject(seekinfo);
					for (OutputThread onOut : map.getAll()) {
						onOut.setMessage(onObject);// �㲥һ���û�����
					}
				}
				break;
			case LOGOUT:// ������˳����������ݿ�����״̬��ͬʱȺ���������������û�
				User logoutUser = (User) read_tranObject.getObject();
				int offId = logoutUser.getId();
				System.out
						.println(MyDate.getDateCN() + " �û���" + offId + " ������");
				dao.logout(offId);
				isStart = false;// �����Լ��Ķ�ѭ��
				map.remove(offId);// �ӻ�����߳����Ƴ�
				out.setMessage(null);// ��Ҫ����һ������Ϣȥ����д�߳�
				out.setStart(false);// �ٽ���д�߳�ѭ��

				TranObject<User> offObject = new TranObject<User>(
						TranObjectType.LOGOUT);
				User logout2User = new User();
				logout2User.setId(logoutUser.getId());
				offObject.setObject(logout2User);
				for (OutputThread offOut : map.getAll()) {// �㲥�û�������Ϣ
					offOut.setMessage(offObject);
				}
				break;
			case MESSAGE:// �����ת����Ϣ�������Ⱥ����
				// ��ȡ��Ϣ��Ҫת���Ķ���id��Ȼ���ȡ����ĸö����д�߳�
				int id2 = read_tranObject.getToUser();
				OutputThread toOut = map.getById(id2);
				if (toOut != null) {// ����û�����
					toOut.setMessage(read_tranObject);
				} else {// ���Ϊ�գ�˵���û��Ѿ�����,�ظ��û�
					TextMessage text = new TextMessage();
					text.setMessage("�ף��Է�������Ŷ��������Ϣ����ʱ�����ڷ�����");
					TranObject<TextMessage> offText = new TranObject<TextMessage>(
							TranObjectType.MESSAGE);
					offText.setObject(text);
					offText.setFromUser(0);
					out.setMessage(offText);
				}
				break;
			case REFRESH:
				List<User> refreshList = dao.refresh(read_tranObject
						.getFromUser());
				TranObject<List<User>> refreshO = new TranObject<List<User>>(
						TranObjectType.REFRESH);
				refreshO.setObject(refreshList);
				out.setMessage(refreshO);
				break;
			default:
				break;
			}
		}
	}
}

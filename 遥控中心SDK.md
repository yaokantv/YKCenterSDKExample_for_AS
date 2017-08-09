### 一: 下载YKCenterSDKExample_for_AS 项目

### 二: SDK集成 , 采用AnroidStudio开发工具集成
- 2-1: 把libs所有文件拷贝到自己项目的libs目录中，把.so文件拷贝到jniLibs/armeabi。
 
-   2-2：在AndroidManifest.xml项目配置文件中增加以下权限
-   
		 <!-- Required  一些系统要求的权限，如访问网络等 -->
		 <uses-permission android:name="android.permission.RECEIVE_USER_PRESENT" />
		 <uses-permission android:name="android.permission.WAKE_LOCK" />
		 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
		 <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
		 <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
		 <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
		 <uses-permission android:name="android.permission.INTERNET" />
		 <uses-permission android:name="android.permission.READ_LOGS" />
		 <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
		 <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
		 <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
		 <uses-permission android:name="android.permission.WRITE_SETTINGS" />
		 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
		 <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />

-   2-3：修改遥看appkey (遥看公司合作后得到的APPID)
    
    ```
    <meta-data
    android:name="yk_key"
    android:value="yk" />
    <!-- yk_key需要向我公司申请 格式为：ykxxxxxx 注意保留前面的yk -->
    ```

		     
### 三：开放的接口，具体参数可以查阅Demo代码
-   3-1：在调用启动应用的第一个Activity或者 Application 中调用初始化SDK
		 
    ```
    //初始化SDK
    YkanSDKManager.init(Context, InitYkanListner);
    //设置Log信息是否打印
    YkanSDKManager.getInstance().setLogger(true);
    
    /**
     * 初始化回调接口
     */
    public interface InitYkanListener {
    /**
     * 初始化成功标示
     */
    int INIT_SUCCESS = 1;
    /**
     * 初始化失败标示
     */
    int INIT_FAIL = 0;

    /**
     * 开始初始化
     */
    void onInitStart();

    String E_NOT_NETWORK = "没有连接网络";

    String E_OTHER_ERROR_MSG = "其他错误信息：";

    /**
     * 初始化完成回调函数
     *
     * @param status:初始化成功或失败标示
     * @param errorMsg:初始化错误信息
     */
    void onInitFinish(int status, String errorMsg);
    }
    ```

  
-   3-2：配置入网：使用DeviceConfig类对象
		  
    ```
    public DeviceConfig(Context ctx,IDeviceConfigListener iConfig)
    参数说明
    ctx：上下文
    IdeviceConfigListener：配置是否成功回调接口，具体参考Demo中是使用
    
    DeviceConfig 类中方法
    1.启动配置入网方法：
	public void startAirlink(String ssid, String pwd)；
	参数说明：
	ssid ：wifi 设备的ssid
	pwd ：wifi 密码
	
	2．回调方法
	public void didSetDeviceOnboarding(GizWifiErrorCode result, String mac, String did, String productKey);
	参数说明
	GizWifiErrorCode：状态对象
	mac：设备mac地址
	did: 设备ID
	productKey:产品Key
    ```

		   

-    3-3：下载数据
-    
    ```
    所有从云端访问的数据封装在YkanIRInterface接口中
    在访问之前需要设置设备ID
    // 在下载数据之前需要设置设备ID，用哪个设备去下载
    YkanSDKManager.getInstance().setDeviceId(deviceId);
    ```
    ```
   1：获取设备类型方法
   /**
   * @param mac：设备Mac地址
   * @param listener:http请求回调方法
   */
	public void getDeviceType(String mac, YKanHttpListener listener);
	
	 2：获取设备类型下面的所有品牌
	/**
	 * @param mac:设备Mac地址
	 * @param type：设备型号
	 * @param listener:http请求回调方法
	 */
	public void getBrandsByType(String mac, int type, YKanHttpListener listener);


	 3：根据品牌id和设备类型获取匹配的遥控器对象集合
	/**
	 * @param mac:设备Mac地址
	 * @param bid:品牌ID
	 * @param type:设备类型ID
	 * @param version:码库版本
	 * @param listener:http请求回调方法
	 */
	public void getRemoteMatched(String mac, int bid, int type, int version, YKanHttpListener listener);


	 4：根据遥控ID获取遥控器相关详细信息
	/**
	 * @param mac:设备Mac地址
	 * @param rcID:遥控器ID
	 * @param listener:http请求回调方法
	 */
	public void getRemoteDetails(String mac, String rcID, YKanHttpListener listener);


	5：一键匹配接口
	/**
	 * @param mac:设备Mac地址
	 * @param bid:品牌ID
	 * @param tid:设备ID
	 * @param src:采集原始码
	 * @param listener:http请求回调方法
	 */
	public void getFastMatched(String mac, int bid, int tid, String src, YKanHttpListener listener);

	6：http请求回调
	public interface YKanHttpListener {
	    /**
	     * http请求成功之后的回调方法
	     * @param baseResult:所有结果都会转成BaseResult父类，使用时请强转成对应子类。
	     */
	    void onSuccess(BaseResult baseResult);
	
	    /**
	     * http请求失败之后的回调方法
	     * @param ykError
	     */
	    void onFail(YKError ykError);
	}
	

    ```

- 3-4：与设备通信对应的类

    ```
    DeviceController／IdeviceControllerListener／LearnCodeListener
    
    DeviceController(设备通信主要的操作类)构造方法说明
    DeviceController(Context ctx,GizWifiDevice device, IDeviceControllerListener listener)
    参数说明
    ctx  :上下文
    GizWifiDevice：当前使用的遥控中心
    IdeviceControllerListener：需要回调监听
    1：发送数据，用于发送命令给遥控中心
    /**
     * 发送单条数据
     * @param value：红外数据
     * @return
     */
    public boolean sendCMD(String value)；
	/**
	 * 发送多条数据
	 * @param listRCD：红外码集合
	 * @param intervalTime：两个码之间的发送间隔
	 */
	public void sendCMD(List<String> listRCD,int intervalTime)；
    
	2：发送学习命令：
	在学习之前需要初始化,把监听接口传入。
	DeviceController. initLearn(LearnCodeListener)；
	
	public interface LearnCodeListener {
    /**
     * 接收数据
     */
    void didReceiveData(DeviceDataStatus dataStatus, String data);
	}

   /**
	 * 发送学习命令
	 */
	 public void startLearn()；
	 
	3：发送停止学习命令：
	/**
	* 停止学习命令(学习过程中不要调用，学习成功之后遥控中心自身会关闭学习状态)
	*/
	public void learnStop()；

	4: 回调监听方法
	/**
	 * dataStatus:学习成功还是其它状态
     * data，返回数据
	 */
	public void didReceiveData(DeviceDataStatus dataStatus,String data);

   5：设备状态信息回调
  /**
	 * 状态返回
	 * @param device
	 * @param netStatus
	 */
	public void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus);

	6：用户获取硬件信息回调
	/** 用于设备硬件信息 */
	public void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device,java.util.concurrent.ConcurrentHashMap<String, String> hardwareInfo) ;

  7：用于修改硬件信息回调
	/** 用于修改设备信息 */
	public void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device);
    ```


- 	3-5: 用户以及遥控中心设备管理DeviceManager（单例模式）
    ```
    1：用户注册：
	public void  registerUser(String userName, String password, String code, GizUserAccountType userType)  
	注：目前只支持GizUserAccountType.GizUserNormal类型注册

	2：用户登录
		a.匿名登录
		public void  userLoginAnonymous()
		
		b.正常登录（用注册后的账号登录）
		public void  userLogin(String userName, String password)
	
	3 判断是否需要登录
	public boolean isneedLogin（）
	注：返回值为true时则必须调用登录接口，false时则可以为不用调用。
	
	4获取遥控中心设备列表
	public synchronized  List<GizWifiDevice>  getCanUseGizWifiDevice（）
	
	5绑定设备
	public void  bindRemoteDevice (String mac)
	
	6解除某个设备的绑定
	public void  unbindDevice (String did)
	
	7回调接口GizWifiCallBack
		a注册回调
		public  void  registerUserCb(GizWifiErrorCode result, String uid, String token)
		
		b登录回调
		public  void  userLoginCb(GizWifiErrorCode result, String uid, String token)	
		
		c刷新设备列表回调
		public void discoveredrCb(GizWifiErrorCode result,List<GizWifiDevice> deviceList)
    ```


- 3-6 空调数据再封装

    ```
    接口AirEvent，两个实现类AirV1Command（V1版本数据），AirV3Command（V3版本数据）
	V1只有开关，模式，温度功能
	V3数据有开关，温度，模式，风量，左右扫风，上下扫风功能

	a 判断当前空调的状态(这里指打开和关闭两种状态)
	public boolean isOff()
	注：返回值为true时则表示当前空调为关闭状态，按模式温度等其他按键不起作用，false时则当前空调为打开状态，按模式温度等其他按键起作用。
	
	b 判断该模式下温度能否变化（只有制冷制热两种模式状态下才有温度的改变）
	public boolean modeHasTemp ()
	注：返回值为true时则按温度键起作用表示当前模式下，false时则按温度键不起作用
	
	c获取码的方法
	public KeyCode getNextValueByCatogery (AirConCatogery catogery)
	public KeyCode getForwardValueByCatogery(AirConCatogery catogery)
	注：AirConCatogery为枚举类
	Power（对应开关键）,Speed（对应风量键）, Mode（对应模式键）
	Temp（对应温度键）,WindUp（对应上下扫风键）,WindLeft（对应左右扫风键）
	
	温度加必须调用getNextValueByCatogery方法
	温度减必须调用getForwardValueByCatogery方法
	其他键两个方法都可以调用。
    ```


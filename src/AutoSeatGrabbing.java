import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson2.JSONObject;


public class AutoSeatGrabbing {

    private static final String BASE_URL = "https://libseat.ecnu.edu.cn/api.php";
    private static final String SKALIB_URL = "http://www.skalibrary.com"; // skalibrary
    private static final String SCHOOL = "ecnu";
    private static final String SCHOOL_NAME = "华东师范大学";
    private static final String OPENID = ""; // openId，固定，用于绑定用户
    private static final String PASSWORD = ""; // 用户密码
    private static final String USERNAME = ""; // 学号
    private static final String EMAIL = ""; // 接收预约结果通知的邮箱，建议使用qq邮箱，然后在微信上绑定qq邮箱，这样每次预约之后能够在微信上收到预约结果
    private static HttpURLConnection con = null;
    private static BufferedReader bufferedReader = null;
    private static InputStream inputStream = null;
    private static StringBuffer stringBuffer = null;
    private static SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");
    private static final String TYPE = "1";
    private static final String SEAT_ID = ""; // 默认座位id，可以修改
    private static final String[] SEATS_ID = {"", ""}; // 默认座位id数组，可以修改

    /**
     * 主函数，使用crontab设置定时任务的时候，会默认执行这个函数
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // 预约SEAT_ID单个座位，如果失败，不会继续尝试
        autoGrabSeat(SEAT_ID);

        // 尝试SEATS_ID数组中的所有座位，直到某个座位预约成功或者全部预约失败
        bookOneSeat();
    }

    /**
     * 预约座位，包括登录-->获取segment参数-->开始预约三个主要流程
     * @throws Exception
     */
    public static boolean autoGrabSeat(String seatId) throws Exception {
        // 获取明天日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        // 下面这行代码最后日期增加1，意思是今天预约明天的座位
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        String tomorrow = formatter.format(calendar.getTime());
        String content = "<div>位置id：" + seatId + "</div><div>预约日期：" + tomorrow + "</div>";

        /**
         * 解除当前用户绑定
         * 如果已经在公众号上面绑定了，可以不用调用removeUser()
         */
//        Boolean hasLogOut = removeUser();
        /**
         * 登录以获取access_token
         * 如果没有预先绑定的话，会登录失败，这时候可以先去公众号上面绑定一下
         */
        HashMap<String, String> map = login();
        if (map.get("status") == "0") {
            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：初始登录失败</div>";
            SendEmailUtil.sendEmail(EMAIL, content);
            return false;
        }
        /**
         * 绑定用户
         * 如果已经在公众号上面绑定过了，就不需要执行下面的addUser()
         */
//        Boolean addUser = addUser(map.get("name"), map.get("card"), map.get("deptName"), map.get("gender"), map.get("roleName"));
//        if (!addUser) {
//            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：绑定失败，请检查学号或密码</div>";
//            SendEmailUtil.sendEmail(EMAIL, content);
//            return false;
//        }
        /**
         * 通过getViableTime()获取segement参数
         */
        HashMap<String, String> map1 = getViableTime("40", tomorrow);
        if (map1.get("status") == "0") {
            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：获取segement参数的过程中失败</div>";
            SendEmailUtil.sendEmail(EMAIL, content);
            return false;
        }
        /**
         * 开始抢座
         */
        HashMap<String, String> res = grabSeat(map.get("accessToken"), TYPE, map1.get("segment"), seatId);
        if (res.get("status") == "1") {
            content = "预约结果：预约成功！" + content;
        } else {
            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：" + res.get("msg") + "</div>";
            SendEmailUtil.sendEmail(EMAIL, content);
            return false;
        }
        SendEmailUtil.sendEmail(EMAIL, "<div>预约结果：预约成功！</div>" + content + "<div><b>莫等闲，白了少年头，空悲切！</b></div>");
        return true;
    }

    // 用户登录
    public static HashMap<String, String> login() throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        JSONObject obj = requestPost(BASE_URL + "/login", "from=mobile&password=" + PASSWORD + "&username=" + USERNAME);
        if (obj.getInteger("status") == 1) {
            JSONObject infoObj = obj.getJSONObject("data").getJSONObject("list");
            map.replace("status", "1");
            map.put("name", infoObj.getString("name"));
            map.put("card", infoObj.getString("card"));
            map.put("deptName", infoObj.getString("deptName"));
            map.put("gender", infoObj.getString("gender"));
            map.put("roleName", infoObj.getString("roleName"));
            map.put("accessToken", obj.getJSONObject("data").getJSONObject("_hash_").getString("access_token"));
        }
        System.out.println("高校");
        return map;
    }

    // skalibrary绑定用户
    public static Boolean addUser(String name, String card, String deptName, String gender, String roleName) throws IOException {
        String paramsStr = "openid=" + OPENID + "&username=" + USERNAME + "&password=" + PASSWORD + "&name=" + name + "&card=" + card + "&deptName=" + deptName + "&gender=" + gender + "&roleName=" + roleName + "&school=" + SCHOOL + "&schoolName=" + SCHOOL_NAME;
        JSONObject obj = requestPost(SKALIB_URL + "/addUser", paramsStr);
        return obj.getBoolean("status");
    }

    // skalibrary解除绑定
    public static Boolean removeUser() throws IOException {
        JSONObject obj = requestPost(SKALIB_URL + "/removeUser", "openid=" + OPENID);
        return obj.getBoolean("status");
    }

    // 预约
    // type : 操作类型 : 1 : 预约座位
    // setId : 座位id : 6056
    public static HashMap<String, String> grabSeat(String accessToken, String type, String segment, String seatId) throws IOException {
        String paramStr = "access_token=" + accessToken + "&userid=" + USERNAME + "&type=" + type + "&id=" + seatId + "&segment=" + segment;
        JSONObject obj = requestPost(BASE_URL + "/spaces/" + seatId + "/book", paramStr);
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        if (obj.getInteger("status") == 1 && obj.getString("msg").indexOf("预约成功") != -1) {
            map.replace("status", "1");
        }
        map.put("msg", obj.getString("msg"));
        return map;
    }

    // 获取图书馆区域信息
    public static String getAreaInfo() throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/areas?tree=1");
        if (obj.getInteger("status") == 0) {
            String areaId = obj.getJSONObject("data").getJSONArray("list").getJSONObject(0).getJSONArray("_child").getJSONObject(0).getJSONArray("_child").getJSONObject(4).getString("id");
            return areaId;
        }
        return "error";
    }

    // 获取可预约日期
    // areaCode: 图书馆区域代码(从getAreaInfo()中获取) : 40 : 中北一楼B区
    public static void getViableDate(String areaCode) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/space_days/" + areaCode);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    // 获取可预约时间段
    // date : 预约日期 : 2023-03-05
    public static HashMap<String, String> getViableTime(String areaCode, String date) throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        JSONObject obj = requestGet(BASE_URL + "/space_time_buckets?area=" + areaCode + "&day=" + date);
        if (obj.getInteger("status") == 1) {
            map.replace("status", "1");
            String segment = obj.getJSONObject("data").getJSONArray("list").getJSONObject(0).getString("id");
            String spaceId = obj.getJSONObject("data").getJSONArray("list").getJSONObject(0).getString("spaceId");
            map.put("segment", segment);
            map.put("spaceId", spaceId);
        }
        return map;
    }

    // 获取座位预约信息
    // endTime : 结束时间 : 23:50
    // segment : 当天预定id(每天增加1)，需要从getViableTime()中获取 : 142576
    public static void getSeatInfo(String areaCode, String date, String endTime, String segment, String startTime) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/spaces_old?area=" + areaCode + "&day=" + date + "&endTime=" + endTime + "&segment=" + segment + "&startTime=" + startTime);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    // 获取空间信息
    // spaceId : 空间id(从getViableTime()中获取)
    public static void getAreaInfo(String spaceId) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/spaces/" + spaceId);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    /**
     * 获取预约历史
     * 虽然这里只能查最近10条，但是通过分析，完全可以加入page和count两个参数，从而获取到更多的历史记录
     * page是当前查询的页数，count是每一页的数量
     * 在返回的数据中，有一个allpage，这个allpage是按照每页10个数据来算的，所以一共有allpage*10
     * 有了这些信息，就能分次把所有数据拿到了（但是亲测，每次获取100条数据要花比较长的时间，我用postman测试花了将近30s）
     * @param accessToken
     * @param userid
     * @throws IOException
     */
    public static void getBookHistory(String accessToken, String userid) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/profile/books?access_token=" + accessToken + "&userid=" + userid);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    /**
     * 查询在馆状态
     * 可以直接输入学号查询，这样就意味着我们可以随意的查别人的学号
     * @throws IOException
     */
    public static void getBookStatus() throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/wechat_lib_access_check?id=" + USERNAME);
        switch (obj.getInteger("status")) {
            case -2:
                System.out.println("没有入馆");
                break;
            case 6:
                System.out.println("正在使用，双向门禁使用中");
                break;
            case 7:
                System.out.println("临时离开，双向门禁临时离开");
                break;
            default:
                break;
        }
    }

    /**
     * 设置临时离开
     * 仅当处于使用状态时，使用临时离开才有用，不然会提示已经临时离开了
     * @param accessToken
     * @param bookId
     * @return
     * @throws IOException
     */
    public static Boolean leave(String accessToken, String bookId) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/profile/books/" + bookId + "?_method=leave&access_token=" + accessToken + "&userid=" + USERNAME);
        if (obj.getInteger("status") == 1) {
            return true;
        } else {
            return false;
        }
    }

    // 对多个座位进行检查，如果某个座位预约失败，则自动预约下一个座位
    public static Boolean bookOneSeat() throws Exception {
        for (int i=0;i<SEATS_ID.length;i++) {
            if (!autoGrabSeat(SEATS_ID[i])) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    // 请求配置
    public static JSONObject requestConfig(String oriUrl, String method, String paramsStr) throws IOException {
        URL url = new URL(oriUrl);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; OXF-AN10 Build/HUAWEIOXF-AN10; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/107.0.5304.141 Mobile Safari/537.36 XWEB/5015 MMWEBSDK/20221206 MMWEBID/838 MicroMessenger/8.0.32.2300(0x2800205D) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setUseCaches(false);
        if (method.toUpperCase() == "POST") {
            OutputStream outputStream = con.getOutputStream();
            outputStream.write(paramsStr.getBytes());
        }
        int resCode = con.getResponseCode();
        JSONObject res;
        if (resCode == HttpURLConnection.HTTP_OK) {
            inputStream = con.getInputStream();
            stringBuffer = new StringBuffer();
            String line;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            res = JSONObject.parseObject(stringBuffer.toString());
        } else {
            res = new JSONObject();
            res.put("status", 0);
        }
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    // GET
    public static JSONObject requestGet(String oriUrl) throws IOException {
        return requestConfig(oriUrl, "GET", null);
    }

    // POST
    public static JSONObject requestPost(String oriUrl, String paramsStr) throws IOException {
        return requestConfig(oriUrl, "POST", paramsStr);
    }

    // 从盛卡恩官网获取所有学校列表
    public static void getSchoolList() throws IOException {
        String pathName = "schools.txt";
        int num = 1;
        File file = new File("schools.txt");
        FileOutputStream fos = new FileOutputStream(file, true);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        while (num <= 16) {
            URL url = new URL("http://skalibrary.cn/index.do?case&pageNumber=" + num);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            int resCode = con.getResponseCode();
            String res;
            if (resCode == HttpURLConnection.HTTP_OK) {
                inputStream = con.getInputStream();
                //将响应流转换成字符串
                stringBuffer = new StringBuffer();
                String line;
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }
                res = stringBuffer.toString();
                String reg = "(?<=<h2>).*?(?=</h2>)";
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(res);
                while (matcher.find()){
                    osw.write(matcher.group() + "\r\n");
                }
            }
            num++;
        }

        if (bufferedReader != null) {
            try {
                bufferedReader.close();
                osw.close(); // 最后记得关闭文件
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

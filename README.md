# 【支持多所高校图书馆】盛卡恩空间（图书馆座位）预约系统自动预约脚本



## 目前已发现几个好玩的东西，感兴趣的可以去试试

- 可以根据学号查询其他同学的在馆状态
- 获取所有预约历史，形成全面的图书馆座位使用报告





# :school: 支持多所高校的空间预约!

- 支持盛卡恩空间预约系统，根据[官网](http://skalibrary.cn/)介绍，全国余90所高校的使用了该系统
- 该系统包括空间预约和通道闸机两个部分，有的学校两个都用了，有的学校只用了通道闸机。官网给出的案例介绍了90余所高校的使用情况，可以在`schools.txt`中查看。
- 根据官网介绍，支持高校列表(在项目根目录下的schools.txt中也可查到，从盛卡恩官网下载到的数据，不保证实际使用效果)：
复旦大学研讨间
  复旦大学座位预约
  河南大学研讨间系统
  华东师范大学座位预约系统
  六盘水师范学院座位预约系统
  太原理工大学研讨间系统
  西南交通大学座位预约系统
  新疆农业大学座位预约系统
- 你可以看看自己图书馆预约系统是不是下面这个样子的。是的话，这个脚本可以通用。

![xitongwaiguan](https://raw.githubusercontent.com/quarkape/ecnu-lib-auto-book/main/imgs/01.jpg)

![](https://raw.githubusercontent.com/quarkape/ecnu-lib-auto-book/main/imgs/02.png)



# 使用教程

## 准备工作

1. （如果你是华东师范大学的学生，直接看第3点）在`AutoSeatGrabbing.java`中将`BASE_URL`修改为你们学校的API地址，具体可以抓一下包。
2. 在`AutoSeatGrabbing.java`中将`SCHOOL`、`SCHOOL_NAME`修改为你自己所在高校的英文简写、中文全称。
3. 在`AutoSeatGrabbing.java`中将`PASSOWRD`、`USERNAME`修改为你的密码和学号。
4. 在`AutoSeatGrabbing.java`中将`EMAIL`修改为你想要接收预约结果通知的邮箱，推荐填写自己的qq邮箱，然后用微信绑定qq邮箱，这样可以直接在微信上收到qq邮件提醒。
5. 在`AutoSeatGrabbing.java`中将`SEAT_ID`修改为想要预约的座位的ID，将`SEATS_ID`修改为你想要预约的座位的ID的数组（提供多个选择，防止只预约一个座位的时候预约失败）。座位ID获取方式请看文末。
6. `AutoSeatGrabbing.java`中的`OPEN_ID`不是必须的，通常只用于绑定用户和解绑用户。华东师范大学可以在【华东师范大学图书馆】公众号--读者服务--座位预约上面进行绑定。
7. 在`SendEmailUtil.java`中将`myEmailPassword`修改为你的qq邮箱SMTP授权码。获取授权码请看[QQ邮箱授权码](https://blog.csdn.net/github_2011/article/details/82664560)。
8. 在`SendEmailUtil.java`中将`myEmailAccount`设置为发送邮件的邮箱，还是推荐使用第4点中的qq邮箱（可以给自己发邮件）。

## 函数调用

1. 每个函数的作用请看`AutoSeatGrabbing.java`中的注释。

2. `autoGrabSeat`可以针对单个座位进行预约，如果预约失败（一般情况下是不能预约或者别人已经预约了），不会重复预约，也没必要重复预约，因为大概率是被其他人预约了。

3. `bookOneSeat`可以依次尝试对`SEATS_ID`数组中的座位进行预约。如果预约失败，会继续尝试数组中下一个座位，直到预约成功一个座位或者全部预约失败。推荐使用这个方式预约。

4. `main()`函数是主函数，如果你使用crontab为`AutoSeatGrabbing.java`设置定时任务的话，会执行这个函数。所以这个函数里面要么放`autoGrabSeat`，要么放`bookOneSeat`。你也可以自己再写一些函数。

5. `autoGrabSeat`函数默认在今天预约明天的座位，这个配置是华东师范大学的配置（可以预约今明两天的座位），你可以根据你们学校的实际情况进行修改。如只支持今天预约当天的作为的话，请把该函数里面的这一句`calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1)`改为`calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) )`，也就是不加一。

   

## 定时任务

请自行搜索windows和linux系统（crontab）设置定时任务的具体方式。

linux系统可以用crontab-->[crontab定时执行java程序(简单易懂)](https://blog.csdn.net/weixin_44422604/article/details/107026556)

windows系统可以使用定时执行脚本-->[windows设置定时执行脚本](https://www.cnblogs.com/sui776265233/p/13602893.html)



# 适用场景

每所学校适用场景不一。

- 对于华东师范大学的同学来说，可以将定时任务设置在早上7点，然后7点自动预约第二天早上的图书馆。这样能够保证每天都预约到座位。
- 可以继续开发，连续对某个座位id段内的座位进行预约，预约成功即可。适用于对某些固定座位依赖不高的同学使用。



# 提示

- 仅限学生交流学习，使用时请遵守学校各项管理规定、规范制度



# 获取座位ID

每一个座位对应一个id。

1. 如果你是华东师范大学的学生并且想要预约中北校区图书馆的座位，可以在项目中`areas`目录下找到华东师范大学中北校区图书馆不同楼层的座位信息。

   查阅方式如下：

   ```json
   {
       "id": 3327,
       "no": "001",
       "name": "001",
       "area": 8,
       "category": 12,
       "point_x": 21.04167,
       "point_x2": null,
       "point_x3": null,
       "point_x4": null,
       "point_y": 75,
       "point_y2": null,
       "point_y3": null,
       "point_y4": null,
       "width": 2.03125,
       "height": 3.173077,
       "status": 1,
       "status_name": "空闲",
       "area_name": "一楼A区",
       "area_levels": 1,
       "area_type": 1,
       "area_color": null
   }
   ```

   

这是某个区域中某个座位的详细信息。其中`id`就是座位id，`no`就是实际的座位号，`area_name`就是区域名称。所以上面这个座位是：**一楼A区01号座位**。将**3327**填入**SEAT_ID**中就可以预约这个座位。其他座位类似。

2. 如果你不是华东师范大学的学生，你可以先执行`getAreaInfo()`函数，获取区域信息；然后根据区域id，执行`getViableTime()`函数，获取`segement`参数；最后执行`getSeatInfo()`函数，获取该区域下某个座位的id号，此时可以参考第1点去查座位id号。
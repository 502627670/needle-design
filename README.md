needle-design
绣花针微框架

#### 介绍
前后端分离项目，快速后台应用开发框架；前端使用vue-element-admin，后端使用java + springboot + jpa + mysql；基于springboot的项目，无需编写页面代码就可以快速完成开发，并且代码无侵入性；自带基于springboot的权限框架，支持自定义扩展。

1、本框架基于前后端分离技术，前端使用vue-element-admin，后端使用java + springboot + jpa + mysql

2、本框架可以快速生成基于模型定义的页面视图，而不需要做页面开发，使用方法如下：

（1）任意springboot项目，引入本项目的jar依赖包。

（2）如果要使用本项目的自动构建页面视图功能，那么只需要继承org.needleframe.AbstractContextService抽象类；重写defModules方法可以定义领域模型，系统根据定义的领域模型自动生成增、删、改、查页面视图；重写defMenus方法可以构建管理端的页面菜单，程序运行后自动显示页面菜单项，例如：

// 定义会员模型
mf.build(MemberUser.class)
// 定义搜索字段
.filters("nickname").op(Op.RLIKE)

// 定义列表视图要显示的列
.showList("avatar", "nickname", "gender", "registerTime", "lastLoginTime")

// 字段定义为图片字段，并且存储为绝对路径
.prop("avatar").absoluteImage().end()

// 定义昵称字段
.prop("nickname")

// 字段值从页面传入后台要做Base64编码后再保存
.encoder(v -> Base64Utils.encode(v.toString()))

// 字段值从数据库查出后要做Base64解码操作再在页面显示
.decoder(v -> Base64Utils.decode(v.toString()))
// 退出字段定义
.end()
.prop("registerTime")
// 定义字段类型为日期时间型
.dateTime()
.encoder(new DateEncoder("yyyy-MM-dd HH:mm:ss"))
.decoder(new DateDecoder("yyyy-MM-dd HH:mm:ss")).end()
// 字段隐藏不显示（查询、新增、修改和详情页面都隐藏）
.prop("nameMobile").hide().end()

 // 定义子表 
.addChild(Cart.class).addCRUD()             
     // 定义子表的外键，并在子表视图页面上显示父表的昵称
    .fk("user").show("nickname").end()                                  
    .fk("goods").show("name").end()
    .fk("product").show("goodsName").end()
     // 退出子表定义
    .endChild(); 
    
// 定义菜单和路径
mf.build("订单列表").uri("/order").icon("el-icon-s-grid")   
    // 定义子菜单和路径
.addItem(Order.class).name("订单列表").uri("/order").icon("el-icon-s-shop");  
3、通过以上方式可以定义出一套基本的应用系统，并且可以保证对原有项目的无侵入性，如果有复杂页面，则可以做页面定制化，可以下载needle-design-ui项目进行页面定制化，最后打包成静态文件后复制到目标项目的webapp目录下即可。

4、本项目已经包含了基于spring security的通用权限框架，可以控制菜单项、操作项（比如增、删、改、查、下载、上传）以及数据权限控制。系统自动生成的页面、菜单和操作功能会自动加入权限管理， 也允许通过编码的方式增加自定义操作并进行授权控制。系统运行后会自动创建“权限管理”菜单项，包含可用户、角色、用户组以及应用设置。

5、权限管理模块的应用设置，可以更改项目页面左上视图的标题名和logo，随心所欲定制化。

线上演示地址：
http://47.107.173.194/booking-admin/
用户：guest
密码：123456

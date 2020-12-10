### 数据模板说明

若要替换数据模板请放到data/json目录下，并且修改数据模板名称为相应的名字。

![](https://main.qcloudimg.com/raw/ab54c7effca62dc96e8e3dca2861e55c.jpg)

SDK会根据数据模板对用户构造上报的数据进行校验在调试终端中输出，包括以下几种情况：

1. 属性名称、事件名称等在数据模板中存在
2. 数据类型符合数据模板定义（取值范围，值类型等）
3. 如果上报数据存在多个错误，将只会提示一个

![](https://main.qcloudimg.com/raw/d925b1d06108d7024eb0041cebc983fa.png)
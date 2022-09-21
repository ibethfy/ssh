最近在做物理网项目，需要SSH连接测试设备，所以就做了一个ssh连接的服务，主要基于反向ssh和websocket实现。
项目分为几个模块：
ssh-client: 设备端运行的客户端程序，主要作用是用于接收服务端发起的反向连接请求，接收消息后，执行反向连接服务器的命令。
ssh-server: 服务端程序，主要提供了webssh的前后端功能，反向ssh连接设备的能力。
ssh-mqtt: mqtt的消息处理模块。
ssh-common: 通用模块。

准备工作：
1、安装emqx，并分配test用户权限。
2、windows测试ssh，需要安装openssh服务端程序。

使用步骤：
1、设备需要有唯一的设备ID，程序中默认为1000.
2、将ssh-client的resources目录下的sshpass脚本拷贝到设备系统的/usr/bin目录下，并赋予相应执行权限。
注：sshpass是自己编译的ssh调用程序，对ssh进行了封装，可以实现在命令行不用手动输入ssh命令。如果不使用，可以通过openssl证书免密登录的方式，直接调用ssh命令实现。
3、修改客户端的ssh连接配置，这里主要是通过客户端程序执行ssh命令，连接客户端程序所在的系统，并在系统上执行反向ssh的命令。修改mqtt连接配置。
4、修改服务端ssh连接的配置，执行反向ssh需要一个专用跳板机（也可以是本地），配置跳板机ssh信息，用于反向ssh端口映射的创建。修改mqtt连接配置。
5、各模块的maven都install一下，然后将ssh-client-1.0.0.jar拷贝到设备系统，通过“java -jar ssh-client-1.0.0.jar”启动客户端程序。
6、修改webssh.html中需要连接的设备ssh信息，并启动服务端。（这里可以自己做一个界面输入）。
7、启动服务端。
8、访问http://localhost:8888/ssh，查看是否连接设备成功。


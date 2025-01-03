在同 VPC 网络下访问，使用 SASL_PLAINTEXT 协议接入，接入点可以在  **集群详情** 页面查看。

## 环境准备

1. [安装 .NET](https://dotnet.microsoft.com/zh-cn/download)
2. 在项目目录运行如下命令下载Confluent.Kafka依赖。

```shell
dotnet add package -v 1.5.2 Confluent.Kafka
```

## 集群准备

### 1. 购买专享版消息服务for Kafka集群

开通消息服务 for Kafka服务后，在控制台页面点击『创建集群』，即可进行购买。

![.jpeg](https://bce.bdstatic.com/doc/bce-doc/Kafka/%E5%9B%BE%E5%83%8F%2021_8eb7111.jpeg)

### 2. 为购买的集群创建主题

在控制台页面点击集群名称，进入集群详情页面。

在左侧的边栏中点击『主题管理』，进入主题管理页面。

![.jpeg](https://bce.bdstatic.com/doc/bce-doc/Kafka/%E5%9B%BE%E5%83%8F%2022_10525b3.jpeg)

在主题管理页面点击『创建主题』，进行主题的创建。

## 使用步骤：

### 步骤一：获取集群接入点

具体请参考：[接入点查看](Kafka/专享版/操作指南/集群管理/查看集群接入点.md)。

### 步骤二：编写测试代码

- 需要关注并自行修改的参数
- 认证机制支持PLAIN、SCRAM-SHA-512两种机制，根据集群所使用的认证方式进行选择

| 参数名              | 含义                       |
|------------------| -------------------------- |
| BootstrapServers | 接入点信息                 |
| topic            | 主题名称                   |
| SaslUsername     | 用户管理中创建用户的用户名 |
| SaslPassword     | 用户管理中创建用户的密码   |
| groupId          | 消费组id                   |

#### 生产者代码示例

创建KafkaProducerDemo.cs文件，具体代码示例如下：

```cs
using System;
using Confluent.Kafka;

class Producer
{
    public static void Main(string[] args)
    {
        var conf = new ProducerConfig {
            // 接入点
            BootstrapServers = "接入点",
            // SASL机制
            SaslMechanism = SaslMechanism.ScramSha512,
            // 接入协议
            SecurityProtocol = SecurityProtocol.SaslPlaintext,
            // 用户名
            SaslUsername = "用户名",
            // 密码
            SaslPassword = "密码",
            };

        Action<DeliveryReport<Null, string>> handler = r =>
            Console.WriteLine(!r.Error.IsError
                ? $"Delivered message to {r.TopicPartitionOffset}"
                : $"Delivery Error: {r.Error.Reason}");

        // 指定主题
        string topic ="主题名称";

        using (var p = new ProducerBuilder<Null, string>(conf).Build())
        {
            for (int i=0; i<100; ++i)
            {
                p.Produce(topic, new Message<Null, string> { Value = i.ToString() }, handler);
            }
            p.Flush(TimeSpan.FromSeconds(10));
        }
    }
}
```

#### 消费者代码示例

创建KafkaConsumerDemo.cs文件，具体代码示例如下：

```cs
using System;
using System.Threading;
using Confluent.Kafka;

class Consumer
{
    public static void Main(string[] args)
    {
        var conf = new ConsumerConfig {
            // 消费组ID
            GroupId = "消费组ID",
            // 接入点
            BootstrapServers = "接入点",
            // SASL机制
            SaslMechanism = SaslMechanism.ScramSha512,
            // 接入协议
            SecurityProtocol = SecurityProtocol.SaslPlaintext,
            // 用户名
            SaslUsername = "用户名",
            // 密码
            SaslPassword = "密码",
            AutoOffsetReset = AutoOffsetReset.Earliest
        };

        // 指定主题
        string topic = "主题名称";

        using (var c = new ConsumerBuilder<Ignore, string>(conf).Build())
        {
            c.Subscribe(topic);

            CancellationTokenSource cts = new CancellationTokenSource();
            Console.CancelKeyPress += (_, e) => {
                e.Cancel = true;
                cts.Cancel();
            };

            try
            {
                while (true)
                {
                    try
                    {
                        var cr = c.Consume(cts.Token);
                        Console.WriteLine($"Consumed message '{cr.Value}' at: '{cr.TopicPartitionOffset}'.");
                    }
                    catch (ConsumeException e)
                    {
                        Console.WriteLine($"Error occurred: {e.Error.Reason}");
                    }
                }
            }
            catch (OperationCanceledException)
            {
                c.Close();
            }
        }
    }
}
```

### 步骤三：编译并运行

运行上述两个代码文件。

```bash
# 启动消费者
dotnet run KafkaProducerDemo.cs
# 启动生产者
dotnet run KafkaConsumerDemo.cs
```

### 步骤四：查看集群监控

查看消息是否发送成功或消费成功有两种方式：

1. 查看程序输出日志。
2. 在专享版消息服务 for Kafka控制台查看集群监控，获取集群生产、消息情况。

推荐使用第二种方式，下面介绍如何查看集群监控。

（1）在专享版消息服务 for Kafka的控制台页面找到需要连接的集群，点击集群名称进入『集群详情』页面。

![.jpeg](https://bce.bdstatic.com/doc/bce-doc/Kafka/%E5%9B%BE%E5%83%8F%2023_e22b5e2.jpeg)

（2）页面跳转后，进入左侧边中的『集群详情』页面。

![.jpeg](https://bce.bdstatic.com/doc/bce-doc/Kafka/%E5%9B%BE%E5%83%8F%2024_0a0eee3.jpeg)

（3）点击左侧边栏中的『集群监控』，进入『集群监控』页面。

![.jpeg](https://bce.bdstatic.com/doc/bce-doc/Kafka/%E5%9B%BE%E5%83%8F%2025_279e3b3.jpeg)

（4）通过查看『集群监控』页面，提供的不同纬度的监控信息（集群监控、节点监控、主题监控、消费组监控），即可获知集群的生产和消费情况。

集群监控的具体使用请参考：[集群监控](Kafka/专享版/操作指南/监控报警/集群监控.md)

![.jpeg](https://bce.bdstatic.com/doc/bce-doc/Kafka/%E5%9B%BE%E5%83%8F%2026_94a8e6f.jpeg)
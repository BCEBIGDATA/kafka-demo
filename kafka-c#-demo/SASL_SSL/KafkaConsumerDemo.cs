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
            // SSL证书文件路径
            SslCaLocation = "kafka-key/ca.pem",
            // SASL机制
            SaslMechanism = SaslMechanism.ScramSha512,
            // 接入协议
            SecurityProtocol = SecurityProtocol.SaslSsl,
            // 用户名
            SaslUsername = "用户名",
            // 密码
            SaslPassword = "密码",
            SslEndpointIdentificationAlgorithm = SslEndpointIdentificationAlgorithm.None,
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